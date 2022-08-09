/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.os.Handler
import com.android.quicksearchbox.util.Consumer
import com.android.quicksearchbox.util.NamedTaskExecutor

/**
 * Suggestions provider implementation.
 *
 * The provider will only handle a single query at a time. If a new query comes
 * in, the old one is cancelled.
 */
class SuggestionsProviderImpl(
    private val mConfig: Config,
    private val mQueryExecutor: NamedTaskExecutor,
    publishThread: Handler,
    logger: Logger?
) : SuggestionsProvider {
    private val mPublishThread: Handler
    private val mLogger: Logger?
    @Override
    override fun close() {
    }

    @Override
    override fun getSuggestions(query: String, sourceToQuery: Source): Suggestions {
        if (SuggestionsProviderImpl.Companion.DBG) Log.d(
            SuggestionsProviderImpl.Companion.TAG,
            "getSuggestions($query)"
        )
        val suggestions = Suggestions(query, sourceToQuery)
        Log.i(
            SuggestionsProviderImpl.Companion.TAG,
            "chars:" + query.length().toString() + ",source:" + sourceToQuery
        )
        val receiver: Consumer<SourceResult>
        if (shouldDisplayResults(query)) {
            receiver = SuggestionsProviderImpl.SuggestionCursorReceiver(suggestions)
        } else {
            receiver = NoOpConsumer<SourceResult>()
            suggestions.done()
        }
        val maxResults: Int = mConfig.getMaxResultsPerSource()
        QueryTask.startQuery(
            query, maxResults, sourceToQuery, mQueryExecutor,
            mPublishThread, receiver
        )
        return suggestions
    }

    private fun shouldDisplayResults(query: String): Boolean {
        return if (query.length() === 0 && !mConfig.showSuggestionsForZeroQuery()) {
            // Note that even though we don't display such results, it's
            // useful to run the query itself because it warms up the network
            // connection.
            false
        } else true
    }

    private inner class SuggestionCursorReceiver(private val mSuggestions: Suggestions) :
        Consumer<SourceResult?> {
        @Override
        override fun consume(cursor: SourceResult): Boolean {
            if (SuggestionsProviderImpl.Companion.DBG) {
                Log.d(
                    SuggestionsProviderImpl.Companion.TAG,
                    "SuggestionCursorReceiver.consume(" + cursor + ") corpus=" +
                            cursor.getSource() + " count = " + cursor.getCount()
                )
            }
            // publish immediately
            if (SuggestionsProviderImpl.Companion.DBG) Log.d(
                SuggestionsProviderImpl.Companion.TAG,
                "Publishing results"
            )
            mSuggestions.addResults(cursor)
            if (cursor != null && mLogger != null) {
                mLogger.logLatency(cursor)
            }
            return true
        }
    }

    companion object {
        private const val DBG = false
        private const val TAG = "QSB.SuggestionsProviderImpl"
    }

    init {
        mPublishThread = publishThread
        mLogger = logger
    }
}