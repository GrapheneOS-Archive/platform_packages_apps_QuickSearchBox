/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quicksearchbox.google

import android.app.SearchManager
import com.android.quicksearchbox.SourceResult

/**
 * A suggestion provider which provides content from Genie, a service that offers
 * a superset of the content provided by Google Suggest.
 */
class GoogleSuggestionProvider : ContentProvider() {
    private var mUriMatcher: UriMatcher? = null
    private var mSource: GoogleSource? = null
    @Override
    fun onCreate(): Boolean {
        mSource = QsbApplication.get(getContext()).getGoogleSource()
        mUriMatcher = buildUriMatcher(getContext())
        return true
    }

    /**
     * This will always return [SearchManager.SUGGEST_MIME_TYPE] as this
     * provider is purely to provide suggestions.
     */
    @Override
    fun getType(uri: Uri?): String {
        return SearchManager.SUGGEST_MIME_TYPE
    }

    private fun emptyIfNull(
        result: SourceResult?,
        source: GoogleSource?,
        query: String
    ): SourceResult {
        return result ?: CursorBackedSourceResult(source, query)
    }

    @Override
    fun query(
        uri: Uri, projection: Array<String?>?, selection: String?,
        selectionArgs: Array<String?>?, sortOrder: String?
    ): Cursor {
        if (GoogleSuggestionProvider.Companion.DBG) Log.d(
            GoogleSuggestionProvider.Companion.TAG,
            "query uri=$uri"
        )
        val match: Int = mUriMatcher.match(uri)
        return if (match == GoogleSuggestionProvider.Companion.SEARCH_SUGGEST) {
            val query = getQuery(uri)
            SuggestionCursorBackedCursor(
                emptyIfNull(mSource!!.queryExternal(query), mSource, query)
            )
        } else if (match == GoogleSuggestionProvider.Companion.SEARCH_SHORTCUT) {
            val shortcutId = getQuery(uri)
            val extraData: String =
                uri.getQueryParameter(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA)
            SuggestionCursorBackedCursor(mSource!!.refreshShortcut(shortcutId, extraData))
        } else {
            throw IllegalArgumentException("Unknown URI $uri")
        }
    }

    /**
     * Gets the search text from a uri.
     */
    private fun getQuery(uri: Uri): String {
        return if (uri.getPathSegments().size() > 1) {
            uri.getLastPathSegment()
        } else {
            ""
        }
    }

    @Override
    fun insert(uri: Uri?, values: ContentValues?): Uri {
        throw UnsupportedOperationException()
    }

    @Override
    fun update(
        uri: Uri?, values: ContentValues?, selection: String?,
        selectionArgs: Array<String?>?
    ): Int {
        throw UnsupportedOperationException()
    }

    @Override
    fun delete(uri: Uri?, selection: String?, selectionArgs: Array<String?>?): Int {
        throw UnsupportedOperationException()
    }

    private fun buildUriMatcher(context: Context): UriMatcher {
        val authority = getAuthority(context)
        val matcher = UriMatcher(UriMatcher.NO_MATCH)
        matcher.addURI(
            authority, SearchManager.SUGGEST_URI_PATH_QUERY,
            GoogleSuggestionProvider.Companion.SEARCH_SUGGEST
        )
        matcher.addURI(
            authority, SearchManager.SUGGEST_URI_PATH_QUERY.toString() + "/*",
            GoogleSuggestionProvider.Companion.SEARCH_SUGGEST
        )
        matcher.addURI(
            authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT,
            GoogleSuggestionProvider.Companion.SEARCH_SHORTCUT
        )
        matcher.addURI(
            authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT.toString() + "/*",
            GoogleSuggestionProvider.Companion.SEARCH_SHORTCUT
        )
        return matcher
    }

    protected fun getAuthority(context: Context): String {
        return context.getPackageName().toString() + ".google"
    }

    companion object {
        private const val DBG = false
        private const val TAG = "QSB.GoogleSuggestionProvider"

        // UriMatcher constants
        private const val SEARCH_SUGGEST = 0
        private const val SEARCH_SHORTCUT = 1
    }
}