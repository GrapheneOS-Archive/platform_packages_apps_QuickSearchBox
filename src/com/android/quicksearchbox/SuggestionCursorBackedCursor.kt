/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.quicksearchbox

import android.app.SearchManager
import android.database.AbstractCursor
import android.database.CursorIndexOutOfBoundsException
import java.util.ArrayList
import java.util.Arrays

class SuggestionCursorBackedCursor(private val mCursor: SuggestionCursor) :
    AbstractCursor() {
    private var mExtraColumns: ArrayList<String>? = null
    @Override
    fun close() {
        super.close()
        mCursor.close()
    }

    @get:Override
    val columnNames: Array<String>
        get() {
            val extraColumns: Collection<String> = mCursor.getExtraColumns()
            return if (extraColumns != null) {
                val allColumns: ArrayList<String> = ArrayList<String>(
                    COLUMNS.size +
                            extraColumns.size()
                )
                mExtraColumns = ArrayList<String>(extraColumns)
                allColumns.addAll(Arrays.asList(COLUMNS))
                allColumns.addAll(mExtraColumns)
                allColumns.toArray(arrayOfNulls<String>(allColumns.size()))
            } else {
                COLUMNS
            }
        }

    @get:Override
    val count: Int
        get() = mCursor.getCount()

    private fun get(): Suggestion {
        mCursor.moveTo(getPosition())
        return mCursor
    }

    private fun getExtra(columnIdx: Int): String? {
        val extraColumn = columnIdx - COLUMNS.size
        val extras: SuggestionExtras = get().getExtras()
        return if (extras != null) {
            extras.getExtra(mExtraColumns.get(extraColumn))
        } else {
            null
        }
    }

    @Override
    fun getInt(column: Int): Int {
        return if (column == COLUMN_INDEX_ID) {
            getPosition()
        } else {
            try {
                Integer.valueOf(getString(column))
            } catch (e: NumberFormatException) {
                0
            }
        }
    }

    @Override
    fun getString(column: Int): String? {
        return if (column < COLUMNS.size) {
            when (column) {
                COLUMN_INDEX_ID -> String.valueOf(getPosition())
                COLUMN_INDEX_TEXT1 -> get().getSuggestionText1()
                COLUMN_INDEX_TEXT2 -> get().getSuggestionText2()
                COLUMN_INDEX_TEXT2_URL -> get().getSuggestionText2Url()
                COLUMN_INDEX_ICON1 -> get().getSuggestionIcon1()
                COLUMN_INDEX_ICON2 -> get().getSuggestionIcon2()
                COLUMN_INDEX_INTENT_ACTION -> get().getSuggestionIntentAction()
                COLUMN_INDEX_INTENT_DATA -> get().getSuggestionIntentDataString()
                COLUMN_INDEX_INTENT_EXTRA_DATA -> get().getSuggestionIntentExtraData()
                COLUMN_INDEX_QUERY -> get().getSuggestionQuery()
                COLUMN_INDEX_FORMAT -> get().getSuggestionFormat()
                COLUMN_INDEX_SHORTCUT_ID -> get().getShortcutId()
                COLUMN_INDEX_SPINNER_WHILE_REFRESHING -> String.valueOf(get().isSpinnerWhileRefreshing())
                else -> throw CursorIndexOutOfBoundsException(
                    "Requested column " + column
                            + " of " + COLUMNS.size
                )
            }
        } else {
            getExtra(column)
        }
    }

    @Override
    fun getLong(column: Int): Long {
        return try {
            Long.valueOf(getString(column))
        } catch (e: NumberFormatException) {
            0
        }
    }

    @Override
    fun isNull(column: Int): Boolean {
        return getString(column) == null
    }

    @Override
    fun getShort(column: Int): Short {
        return try {
            Short.valueOf(getString(column))
        } catch (e: NumberFormatException) {
            0
        }
    }

    @Override
    fun getDouble(column: Int): Double {
        return try {
            Double.valueOf(getString(column))
        } catch (e: NumberFormatException) {
            0
        }
    }

    @Override
    fun getFloat(column: Int): Float {
        return try {
            Float.valueOf(getString(column))
        } catch (e: NumberFormatException) {
            0
        }
    }

    companion object {
        // This array also used in CursorBackedSuggestionExtras to avoid duplication.
        val COLUMNS = arrayOf(
            "_id",  // 0, This will contain the row number. CursorAdapter, used by SuggestionsAdapter,
            // used by SearchDialog, expects an _id column.
            SearchManager.SUGGEST_COLUMN_TEXT_1,  // 1
            SearchManager.SUGGEST_COLUMN_TEXT_2,  // 2
            SearchManager.SUGGEST_COLUMN_TEXT_2_URL,  // 3
            SearchManager.SUGGEST_COLUMN_ICON_1,  // 4
            SearchManager.SUGGEST_COLUMN_ICON_2,  // 5
            SearchManager.SUGGEST_COLUMN_INTENT_ACTION,  // 6
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,  // 7
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,  // 8
            SearchManager.SUGGEST_COLUMN_QUERY,  // 9
            SearchManager.SUGGEST_COLUMN_FORMAT,  // 10
            SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,  // 11
            SearchManager.SUGGEST_COLUMN_SPINNER_WHILE_REFRESHING
        )
        private const val COLUMN_INDEX_ID = 0
        private const val COLUMN_INDEX_TEXT1 = 1
        private const val COLUMN_INDEX_TEXT2 = 2
        private const val COLUMN_INDEX_TEXT2_URL = 3
        private const val COLUMN_INDEX_ICON1 = 4
        private const val COLUMN_INDEX_ICON2 = 5
        private const val COLUMN_INDEX_INTENT_ACTION = 6
        private const val COLUMN_INDEX_INTENT_DATA = 7
        private const val COLUMN_INDEX_INTENT_EXTRA_DATA = 8
        private const val COLUMN_INDEX_QUERY = 9
        private const val COLUMN_INDEX_FORMAT = 10
        private const val COLUMN_INDEX_SHORTCUT_ID = 11
        private const val COLUMN_INDEX_SPINNER_WHILE_REFRESHING = 12
    }
}