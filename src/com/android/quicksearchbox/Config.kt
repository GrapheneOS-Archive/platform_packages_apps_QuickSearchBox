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

import android.app.AlarmManager
import android.content.Context
import android.net.Uri
import android.os.Process
import java.util.HashSet

/**
 * Provides values for configurable parameters in all of QSB.
 *
 * All the methods in this class return fixed default values. Subclasses may
 * make these values server-side settable.
 *
 */
class Config(context: Context) {
    private val mContext: Context
    private val mDefaultCorpora: HashSet<String>? = null
    private val mHiddenCorpora: HashSet<String>? = null
    private val mDefaultCorporaSuggestUris: HashSet<String>? = null
    protected val context: Context
        protected get() = mContext

    /**
     * Releases any resources used by the configuration object.
     *
     * Default implementation does nothing.
     */
    fun close() {}
    private fun loadResourceStringSet(res: Int): HashSet<String> {
        val set: HashSet<String> = HashSet<String>()
        val items: Array<String> = mContext.getResources().getStringArray(res)
        for (item in items) {
            set.add(item)
        }
        return set
    }// Get the list of default corpora from a resource, which allows vendor overlays.

    /**
     * The number of suggestions visible above the onscreen keyboard.
     */
    val numSuggestionsAboveKeyboard: Int
        get() =// Get the list of default corpora from a resource, which allows vendor overlays.
            mContext.getResources().getInteger(R.integer.num_suggestions_above_keyboard)

    /**
     * The maximum number of suggestions to promote.
     */
    val maxPromotedSuggestions: Int
        get() = mContext.getResources().getInteger(R.integer.max_promoted_suggestions)
    val maxPromotedResults: Int
        get() = mContext.getResources().getInteger(R.integer.max_promoted_results)

    /**
     * The maximum number of shortcuts to show for the web source in All mode.
     */
    val maxShortcutsPerWebSource: Int
        get() = mContext.getResources().getInteger(R.integer.max_shortcuts_per_web_source)

    /**
     * The maximum number of shortcuts to show for each non-web source in All mode.
     */
    val maxShortcutsPerNonWebSource: Int
        get() = mContext.getResources().getInteger(R.integer.max_shortcuts_per_non_web_source)

    /**
     * Gets the maximum number of shortcuts that will be shown from the given source.
     */
    fun getMaxShortcuts(sourceName: String?): Int {
        return maxShortcutsPerNonWebSource
    }

    fun allowVoiceSearchHints(): Boolean {
        return true
    }

    fun showSuggestionsForZeroQuery(): Boolean {
        // Get the list of default corpora from a resource, which allows vendor overlays.
        return mContext.getResources().getBoolean(R.bool.show_zero_query_suggestions)
    }

    fun showShortcutsForZeroQuery(): Boolean {
        // Get the list of default corpora from a resource, which allows vendor overlays.
        return mContext.getResources().getBoolean(R.bool.show_zero_query_shortcuts)
    }

    fun showScrollingSuggestions(): Boolean {
        return mContext.getResources().getBoolean(R.bool.show_scrolling_suggestions)
    }

    fun showScrollingResults(): Boolean {
        return mContext.getResources().getBoolean(R.bool.show_scrolling_results)
    }

    fun getHelpUrl(activity: String?): Uri? {
        return null
    }

    companion object {
        private const val TAG = "QSB.Config"
        private const val DBG = false
        protected const val SECOND_MILLIS = 1000L
        protected const val MINUTE_MILLIS = 60L * SECOND_MILLIS
        protected const val DAY_MILLIS = 86400000L

        /**
         * The number of promoted sources.
         */
        val numPromotedSources = 3
            get() = Companion.field

        /**
         * The number of results to ask each source for.
         */
        val maxResultsPerSource = 50
            get() = Companion.field

        /**
         * The timeout for querying each source, in milliseconds.
         */
        val sourceTimeoutMillis: Long = 10000
            get() = Companion.field

        /**
         * The priority of query threads.
         *
         * @return A thread priority, as defined in [Process].
         */
        val queryThreadPriority: Int =
            Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE
            get() = Companion.field

        /**
         * The maximum age of log data used for shortcuts.
         */
        val maxStatAgeMillis = 30 * DAY_MILLIS
            get() = Companion.field

        /**
         * The minimum number of clicks needed to rank a source.
         */
        val minClicksForSourceRanking = 3
            get() = Companion.field
        val numWebCorpusThreads = 2
            get() = Companion.field

        /**
         * How often query latency should be logged.
         *
         * @return An integer in the range 0-1000. 0 means that no latency events
         * should be logged. 1000 means that all latency events should be logged.
         */
        val latencyLogFrequency = 1000
            get() = Companion.field

        /**
         * The delay in milliseconds before suggestions are updated while typing.
         * If a new character is typed before this timeout expires, the timeout is reset.
         */
        val typingUpdateSuggestionsDelayMillis: Long = 100
            get() = Companion.field
        private const val PUBLISH_RESULT_DELAY_MILLIS: Long = 200

        /**
         * The period of time for which after installing voice search we should consider showing voice
         * search hints.
         *
         * @return The period in milliseconds.
         */
        val voiceSearchHintActivePeriod = 7L * DAY_MILLIS
            get() = Companion.field

        /**
         * The time interval at which we should consider whether or not to show some voice search hints.
         *
         * @return The period in milliseconds.
         */
        val voiceSearchHintUpdatePeriod: Long = AlarmManager.INTERVAL_FIFTEEN_MINUTES
            get() = Companion.field

        /**
         * The time interval at which, on average, voice search hints are displayed.
         *
         * @return The period in milliseconds.
         */
        val voiceSearchHintShowPeriod: Long = AlarmManager.INTERVAL_HOUR * 2
            get() = Companion.field

        /**
         * The period that we change voice search hints at while they're being displayed.
         *
         * @return The period in milliseconds.
         */
        val voiceSearchHintChangePeriod = 2L * MINUTE_MILLIS
            get() = Companion.field

        /**
         * The amount of time for which voice search hints are displayed in one go.
         *
         * @return The period in milliseconds.
         */
        val voiceSearchHintVisibleTime = 6L * MINUTE_MILLIS
            get() = Companion.field
        val httpConnectTimeout = 4000
            get() = Companion.field
        val httpReadTimeout = 4000
            get() = Companion.field
        val userAgent = "Android/1.0"
            get() = Companion.field
    }

    /**
     * Creates a new config that uses hard-coded default values.
     */
    init {
        mContext = context
    }
}