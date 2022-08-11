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

import android.app.Activity

/**
 * The main activity for Quick Search Box. Shows the search UI.
 *
 */
class SearchActivity : Activity() {
    private var mTraceStartUp = false

    // Measures time from for last onCreate()/onNewIntent() call.
    private var mStartLatencyTracker: LatencyTracker? = null

    // Measures time spent inside onCreate()
    private var mOnCreateTracker: LatencyTracker? = null
    private var mOnCreateLatency = 0

    // Whether QSB is starting. True between the calls to onCreate()/onNewIntent() and onResume().
    private var mStarting = false

    // True if the user has taken some action, e.g. launching a search, voice search,
    // or suggestions, since QSB was last started.
    private var mTookAction = false
    private var mSearchActivityView: SearchActivityView? = null
    protected var searchSource: Source? = null
        private set
    private var mAppSearchData: Bundle? = null
    private val mHandler: Handler = Handler()
    private val mUpdateSuggestionsTask: Runnable = object : Runnable() {
        @Override
        fun run() {
            updateSuggestions()
        }
    }
    private val mShowInputMethodTask: Runnable = object : Runnable() {
        @Override
        fun run() {
            mSearchActivityView.showInputMethodForQuery()
        }
    }
    private var mDestroyListener: SearchActivity.OnDestroyListener? = null

    /** Called when the activity is first created.  */
    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        mTraceStartUp = getIntent().hasExtra(SearchActivity.Companion.INTENT_EXTRA_TRACE_START_UP)
        if (mTraceStartUp) {
            val traceFile: String = File(getDir("traces", 0), "qsb-start.trace").getAbsolutePath()
            Log.i(SearchActivity.Companion.TAG, "Writing start-up trace to $traceFile")
            Debug.startMethodTracing(traceFile)
        }
        recordStartTime()
        if (SearchActivity.Companion.DBG) Log.d(SearchActivity.Companion.TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        // This forces the HTTP request to check the users domain to be
        // sent as early as possible.
        QsbApplication[this].getSearchBaseUrlHelper()
        searchSource = QsbApplication[this].getGoogleSource()
        mSearchActivityView = setupContentView()
        if (config.showScrollingResults()) {
            mSearchActivityView.setMaxPromotedResults(config.getMaxPromotedResults())
        } else {
            mSearchActivityView.limitResultsToViewHeight()
        }
        mSearchActivityView.setSearchClickListener(object : SearchClickListener {
            @Override
            override fun onSearchClicked(method: Int): Boolean {
                return this@SearchActivity.onSearchClicked(method)
            }
        })
        mSearchActivityView.setQueryListener(object : QueryListener {
            @Override
            override fun onQueryChanged() {
                updateSuggestionsBuffered()
            }
        })
        mSearchActivityView.setSuggestionClickListener(SearchActivity.ClickHandler())
        mSearchActivityView.setVoiceSearchButtonClickListener(object : OnClickListener() {
            @Override
            fun onClick(view: View?) {
                onVoiceSearchClicked()
            }
        })
        val finishOnClick: View.OnClickListener = object : OnClickListener() {
            @Override
            fun onClick(v: View?) {
                finish()
            }
        }
        mSearchActivityView.setExitClickListener(finishOnClick)

        // First get setup from intent
        val intent: Intent = getIntent()
        setupFromIntent(intent)
        // Then restore any saved instance state
        restoreInstanceState(savedInstanceState)

        // Do this at the end, to avoid updating the list view when setSource()
        // is called.
        mSearchActivityView.start()
        recordOnCreateDone()
    }

    protected fun setupContentView(): SearchActivityView {
        setContentView(R.layout.search_activity)
        return findViewById(R.id.search_activity_view) as SearchActivityView
    }

    protected val searchActivityView: SearchActivityView?
        protected get() = mSearchActivityView

    @Override
    protected fun onNewIntent(intent: Intent) {
        if (SearchActivity.Companion.DBG) Log.d(SearchActivity.Companion.TAG, "onNewIntent()")
        recordStartTime()
        setIntent(intent)
        setupFromIntent(intent)
    }

    private fun recordStartTime() {
        mStartLatencyTracker = LatencyTracker()
        mOnCreateTracker = LatencyTracker()
        mStarting = true
        mTookAction = false
    }

    private fun recordOnCreateDone() {
        mOnCreateLatency = mOnCreateTracker.getLatency()
    }

    protected fun restoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        val query: String =
            savedInstanceState.getString(SearchActivity.Companion.INSTANCE_KEY_QUERY)
        setQuery(query, false)
    }

    @Override
    protected fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // We don't save appSearchData, since we always get the value
        // from the intent and the user can't change it.
        outState.putString(SearchActivity.Companion.INSTANCE_KEY_QUERY, query)
    }

    private fun setupFromIntent(intent: Intent) {
        if (SearchActivity.Companion.DBG) Log.d(
            SearchActivity.Companion.TAG,
            "setupFromIntent(" + intent.toUri(0).toString() + ")"
        )
        val corpusName = getCorpusNameFromUri(intent.getData())
        val query: String = intent.getStringExtra(SearchManager.QUERY)
        val appSearchData: Bundle = intent.getBundleExtra(SearchManager.APP_DATA)
        val selectAll: Boolean = intent.getBooleanExtra(SearchManager.EXTRA_SELECT_QUERY, false)
        setQuery(query, selectAll)
        mAppSearchData = appSearchData
    }

    private fun getCorpusNameFromUri(uri: Uri?): String? {
        if (uri == null) return null
        return if (!SearchActivity.Companion.SCHEME_CORPUS.equals(uri.getScheme())) null else uri.getAuthority()
    }

    private val qsbApplication: QsbApplication
        private get() = QsbApplication[this]
    private val config: Config
        private get() = qsbApplication.getConfig()
    protected val settings: SearchSettings
        protected get() = qsbApplication.getSettings()
    private val suggestionsProvider: SuggestionsProvider
        private get() = qsbApplication.getSuggestionsProvider()
    private val logger: Logger
        private get() = qsbApplication.getLogger()

    @VisibleForTesting
    fun setOnDestroyListener(l: SearchActivity.OnDestroyListener?) {
        mDestroyListener = l
    }

    @Override
    protected fun onDestroy() {
        if (SearchActivity.Companion.DBG) Log.d(SearchActivity.Companion.TAG, "onDestroy()")
        mSearchActivityView.destroy()
        super.onDestroy()
        if (mDestroyListener != null) {
            mDestroyListener.onDestroyed()
        }
    }

    @Override
    protected fun onStop() {
        if (SearchActivity.Companion.DBG) Log.d(SearchActivity.Companion.TAG, "onStop()")
        if (!mTookAction) {
            // TODO: This gets logged when starting other activities, e.g. by opening the search
            // settings, or clicking a notification in the status bar.
            // TODO we should log both sets of suggestions in 2-pane mode
            logger.logExit(currentSuggestions, query.length())
        }
        // Close all open suggestion cursors. The query will be redone in onResume()
        // if we come back to this activity.
        mSearchActivityView.clearSuggestions()
        mSearchActivityView.onStop()
        super.onStop()
    }

    @Override
    protected fun onPause() {
        if (SearchActivity.Companion.DBG) Log.d(SearchActivity.Companion.TAG, "onPause()")
        mSearchActivityView.onPause()
        super.onPause()
    }

    @Override
    protected fun onRestart() {
        if (SearchActivity.Companion.DBG) Log.d(SearchActivity.Companion.TAG, "onRestart()")
        super.onRestart()
    }

    @Override
    protected fun onResume() {
        if (SearchActivity.Companion.DBG) Log.d(SearchActivity.Companion.TAG, "onResume()")
        super.onResume()
        updateSuggestionsBuffered()
        mSearchActivityView.onResume()
        if (mTraceStartUp) Debug.stopMethodTracing()
    }

    @Override
    fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Since the menu items are dynamic, we recreate the menu every time.
        menu.clear()
        createMenuItems(menu, true)
        return true
    }

    fun createMenuItems(menu: Menu?, showDisabled: Boolean) {
        qsbApplication.getHelp()
            .addHelpMenuItem(menu, SearchActivity.Companion.ACTIVITY_HELP_CONTEXT)
    }

    @Override
    fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Launch the IME after a bit
            mHandler.postDelayed(mShowInputMethodTask, 0)
        }
    }

    protected val query: String
        protected get() = mSearchActivityView.getQuery()

    protected fun setQuery(query: String?, selectAll: Boolean) {
        mSearchActivityView.setQuery(query, selectAll)
    }

    /**
     * @return true if a search was performed as a result of this click, false otherwise.
     */
    protected fun onSearchClicked(method: Int): Boolean {
        val query: String = CharMatcher.whitespace().trimAndCollapseFrom(query, ' ')
        if (SearchActivity.Companion.DBG) Log.d(
            SearchActivity.Companion.TAG,
            "Search clicked, query=$query"
        )

        // Don't do empty queries
        if (TextUtils.getTrimmedLength(query) === 0) return false
        mTookAction = true

        // Log search start
        logger.logSearch(method, query.length())

        // Start search
        startSearch(searchSource, query)
        return true
    }

    protected fun startSearch(searchSource: Source?, query: String?) {
        val intent: Intent? = searchSource!!.createSearchIntent(query, mAppSearchData)
        launchIntent(intent)
    }

    protected fun onVoiceSearchClicked() {
        if (SearchActivity.Companion.DBG) Log.d(
            SearchActivity.Companion.TAG,
            "Voice Search clicked"
        )
        mTookAction = true

        // Log voice search start
        logger.logVoiceSearch()

        // Start voice search
        val intent: Intent? = searchSource!!.createVoiceSearchIntent(mAppSearchData)
        launchIntent(intent)
    }

    protected val currentSuggestions: SuggestionCursor?
        protected get() {
            val suggestions: Suggestions = mSearchActivityView.getSuggestions()
                ?: return null
            return suggestions.getResult()
        }

    protected fun getCurrentSuggestions(
        adapter: SuggestionsAdapter<*>,
        id: Long
    ): SuggestionPosition? {
        val pos: SuggestionPosition = adapter.getSuggestion(id) ?: return null
        val suggestions: SuggestionCursor = pos.getCursor()
        val position: Int = pos.getPosition()
        if (suggestions == null) {
            return null
        }
        val count: Int = suggestions.getCount()
        if (position < 0 || position >= count) {
            Log.w(
                SearchActivity.Companion.TAG,
                "Invalid suggestion position $position, count = $count"
            )
            return null
        }
        suggestions.moveTo(position)
        return pos
    }

    protected fun launchIntent(intent: Intent?) {
        if (SearchActivity.Companion.DBG) Log.d(
            SearchActivity.Companion.TAG,
            "launchIntent $intent"
        )
        if (intent == null) {
            return
        }
        try {
            startActivity(intent)
        } catch (ex: RuntimeException) {
            // Since the intents for suggestions specified by suggestion providers,
            // guard against them not being handled, not allowed, etc.
            Log.e(SearchActivity.Companion.TAG, "Failed to start " + intent.toUri(0), ex)
        }
    }

    private fun launchSuggestion(adapter: SuggestionsAdapter<*>, id: Long): Boolean {
        val suggestion = getCurrentSuggestions(adapter, id) ?: return false
        if (SearchActivity.Companion.DBG) Log.d(
            SearchActivity.Companion.TAG,
            "Launching suggestion $id"
        )
        mTookAction = true

        // Log suggestion click
        logger.logSuggestionClick(
            id, suggestion.getCursor(),
            Logger.SUGGESTION_CLICK_TYPE_LAUNCH
        )

        // Launch intent
        launchSuggestion(suggestion.getCursor(), suggestion.getPosition())
        return true
    }

    protected fun launchSuggestion(suggestions: SuggestionCursor, position: Int) {
        suggestions.moveTo(position)
        val intent: Intent = SuggestionUtils.getSuggestionIntent(suggestions, mAppSearchData)
        launchIntent(intent)
    }

    protected fun refineSuggestion(adapter: SuggestionsAdapter<*>, id: Long) {
        if (SearchActivity.Companion.DBG) Log.d(
            SearchActivity.Companion.TAG,
            "query refine clicked, pos $id"
        )
        val suggestion = getCurrentSuggestions(adapter, id) ?: return
        val query: String = suggestion.getSuggestionQuery()
        if (TextUtils.isEmpty(query)) {
            return
        }

        // Log refine click
        logger.logSuggestionClick(
            id, suggestion.getCursor(),
            Logger.SUGGESTION_CLICK_TYPE_REFINE
        )

        // Put query + space in query text view
        val queryWithSpace = "$query "
        setQuery(queryWithSpace, false)
        updateSuggestions()
        mSearchActivityView.focusQueryTextView()
    }

    private fun updateSuggestionsBuffered() {
        if (SearchActivity.Companion.DBG) Log.d(
            SearchActivity.Companion.TAG,
            "updateSuggestionsBuffered()"
        )
        mHandler.removeCallbacks(mUpdateSuggestionsTask)
        val delay: Long = config.getTypingUpdateSuggestionsDelayMillis()
        mHandler.postDelayed(mUpdateSuggestionsTask, delay)
    }

    private fun gotSuggestions(suggestions: Suggestions) {
        if (mStarting) {
            mStarting = false
            val source: String = getIntent().getStringExtra(Search.SOURCE)
            val latency: Int = mStartLatencyTracker.getLatency()
            logger.logStart(mOnCreateLatency, latency, source)
            qsbApplication.onStartupComplete()
        }
    }

    fun updateSuggestions() {
        if (SearchActivity.Companion.DBG) Log.d(SearchActivity.Companion.TAG, "updateSuggestions()")
        val query: String = CharMatcher.whitespace().trimLeadingFrom(query)
        updateSuggestions(query, searchSource)
    }

    protected fun updateSuggestions(query: String, source: Source?) {
        if (SearchActivity.Companion.DBG) Log.d(
            SearchActivity.Companion.TAG,
            "updateSuggestions(\"$query\",$source)"
        )
        val suggestions = suggestionsProvider.getSuggestions(
            query, source!!
        )

        // Log start latency if this is the first suggestions update
        gotSuggestions(suggestions)
        showSuggestions(suggestions)
    }

    protected fun showSuggestions(suggestions: Suggestions?) {
        mSearchActivityView.setSuggestions(suggestions)
    }

    private inner class ClickHandler : SuggestionClickListener {
        @Override
        override fun onSuggestionClicked(adapter: SuggestionsAdapter<*>, id: Long) {
            launchSuggestion(adapter, id)
        }

        @Override
        override fun onSuggestionQueryRefineClicked(adapter: SuggestionsAdapter<*>, id: Long) {
            refineSuggestion(adapter, id)
        }
    }

    interface OnDestroyListener {
        fun onDestroyed()
    }

    companion object {
        private const val DBG = false
        private const val TAG = "QSB.SearchActivity"
        private const val SCHEME_CORPUS = "qsb.corpus"
        private const val INTENT_EXTRA_TRACE_START_UP = "trace_start_up"

        // Keys for the saved instance state.
        private const val INSTANCE_KEY_QUERY = "query"
        private const val ACTIVITY_HELP_CONTEXT = "search"
    }
}