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

import com.android.common.Search

/**
 * Search widget provider.
 *
 */
class SearchWidgetProvider : BroadcastReceiver() {
    @Override
    fun onReceive(context: Context?, intent: Intent) {
        if (SearchWidgetProvider.Companion.DBG) Log.d(
            SearchWidgetProvider.Companion.TAG,
            "onReceive(" + intent.toUri(0).toString() + ")"
        )
        val action: String = intent.getAction()
        if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)) {
            // nothing needs doing
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            SearchWidgetProvider.Companion.updateSearchWidgets(context)
        } else {
            if (SearchWidgetProvider.Companion.DBG) Log.d(
                SearchWidgetProvider.Companion.TAG,
                "Unhandled intent action=$action"
            )
        }
    }

    private class SearchWidgetState(private val mAppWidgetId: Int) {
        private var mQueryTextViewIntent: Intent? = null
        private var mVoiceSearchIntent: Intent? = null
        fun setQueryTextViewIntent(queryTextViewIntent: Intent?) {
            mQueryTextViewIntent = queryTextViewIntent
        }

        fun setVoiceSearchIntent(voiceSearchIntent: Intent?) {
            mVoiceSearchIntent = voiceSearchIntent
        }

        fun updateWidget(context: Context, appWidgetMgr: AppWidgetManager) {
            if (SearchWidgetProvider.Companion.DBG) Log.d(
                SearchWidgetProvider.Companion.TAG,
                "Updating appwidget $mAppWidgetId"
            )
            val views = RemoteViews(context.getPackageName(), R.layout.search_widget)
            setOnClickActivityIntent(
                context, views, R.id.search_widget_text,
                mQueryTextViewIntent
            )
            // Voice Search button
            if (mVoiceSearchIntent != null) {
                setOnClickActivityIntent(
                    context, views, R.id.search_widget_voice_btn,
                    mVoiceSearchIntent
                )
                views.setViewVisibility(R.id.search_widget_voice_btn, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.search_widget_voice_btn, View.GONE)
            }
            appWidgetMgr.updateAppWidget(mAppWidgetId, views)
        }

        private fun setOnClickActivityIntent(
            context: Context, views: RemoteViews, viewId: Int,
            intent: Intent?
        ) {
            intent.setPackage(context.getPackageName())
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            views.setOnClickPendingIntent(viewId, pendingIntent)
        }
    }

    companion object {
        private const val DBG = false
        private const val TAG = "QSB.SearchWidgetProvider"

        /**
         * The [Search.SOURCE] value used when starting searches from the search widget.
         */
        private const val WIDGET_SEARCH_SOURCE = "launcher-widget"
        private fun getSearchWidgetStates(context: Context): Array<SearchWidgetProvider.SearchWidgetState?> {
            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(
                SearchWidgetProvider.Companion.myComponentName(context)
            )
            val states: Array<SearchWidgetProvider.SearchWidgetState?> =
                arrayOfNulls<SearchWidgetProvider.SearchWidgetState>(appWidgetIds.size)
            for (i in appWidgetIds.indices) {
                states[i] =
                    SearchWidgetProvider.Companion.getSearchWidgetState(context, appWidgetIds[i])
            }
            return states
        }

        /**
         * Updates all search widgets.
         */
        @JvmStatic
        fun updateSearchWidgets(context: Context?) {
            if (SearchWidgetProvider.Companion.DBG) Log.d(
                SearchWidgetProvider.Companion.TAG,
                "updateSearchWidgets"
            )
            val states: Array<SearchWidgetProvider.SearchWidgetState> =
                SearchWidgetProvider.Companion.getSearchWidgetStates(context)
            for (state in states) {
                state.updateWidget(context, AppWidgetManager.getInstance(context))
            }
        }

        /**
         * Gets the component name of this search widget provider.
         */
        private fun myComponentName(context: Context): ComponentName {
            val pkg: String = context.getPackageName()
            val cls = "$pkg.SearchWidgetProvider"
            return ComponentName(pkg, cls)
        }

        private fun createQsbActivityIntent(
            context: Context, action: String,
            widgetAppData: Bundle
        ): Intent {
            val qsbIntent = Intent(action)
            qsbIntent.setPackage(context.getPackageName())
            qsbIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            qsbIntent.putExtra(SearchManager.APP_DATA, widgetAppData)
            return qsbIntent
        }

        private fun getSearchWidgetState(
            context: Context,
            appWidgetId: Int
        ): SearchWidgetProvider.SearchWidgetState {
            if (SearchWidgetProvider.Companion.DBG) Log.d(
                SearchWidgetProvider.Companion.TAG,
                "Creating appwidget state $appWidgetId"
            )
            val state: SearchWidgetProvider.SearchWidgetState =
                SearchWidgetProvider.SearchWidgetState(appWidgetId)
            val widgetAppData = Bundle()
            widgetAppData.putString(
                Search.SOURCE,
                SearchWidgetProvider.Companion.WIDGET_SEARCH_SOURCE
            )

            // Text field click
            val qsbIntent: Intent = SearchWidgetProvider.Companion.createQsbActivityIntent(
                context,
                SearchManager.INTENT_ACTION_GLOBAL_SEARCH,
                widgetAppData
            )
            state.setQueryTextViewIntent(qsbIntent)

            // Voice search button
            val voiceSearchIntent: Intent =
                SearchWidgetProvider.Companion.getVoiceSearchIntent(context, widgetAppData)
            state.setVoiceSearchIntent(voiceSearchIntent)
            return state
        }

        private fun getVoiceSearchIntent(context: Context, widgetAppData: Bundle): Intent? {
            val voiceSearch: VoiceSearch = QsbApplication[context].getVoiceSearch()
            return voiceSearch.createVoiceWebSearchIntent(widgetAppData)
        }
    }
}