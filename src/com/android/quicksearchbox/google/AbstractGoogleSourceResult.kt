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
package com.android.quicksearchbox.google

import com.android.quicksearchbox.R

abstract class AbstractGoogleSourceResult(source: Source, userQuery: String) : SourceResult {
    private val mSource: Source
    val userQuery: String
    var position = 0
        private set
    abstract val count: Int
    abstract val suggestionQuery: String
    val source: Source
        get() = mSource

    fun close() {}
    fun moveTo(pos: Int) {
        position = pos
    }

    fun moveToNext(): Boolean {
        val size = count
        if (position >= size) {
            // Already past the end
            return false
        }
        position++
        return position < size
    }

    fun registerDataSetObserver(observer: DataSetObserver?) {}
    fun unregisterDataSetObserver(observer: DataSetObserver?) {}
    val suggestionText1: String
        get() = suggestionQuery
    val suggestionSource: Source
        get() = mSource
    val isSuggestionShortcut: Boolean
        get() = false
    val shortcutId: String?
        get() = null
    val suggestionFormat: String?
        get() = null
    val suggestionIcon1: String
        get() = String.valueOf(R.drawable.magnifying_glass)
    val suggestionIcon2: String?
        get() = null
    val suggestionIntentAction: String
        get() = mSource.defaultIntentAction
    val suggestionIntentComponent: ComponentName
        get() = mSource.intentComponent
    val suggestionIntentDataString: String?
        get() = null
    val suggestionIntentExtraData: String?
        get() = null
    val suggestionLogType: String?
        get() = null
    val suggestionText2: String?
        get() = null
    val suggestionText2Url: String?
        get() = null
    val isSpinnerWhileRefreshing: Boolean
        get() = false
    val isWebSearchSuggestion: Boolean
        get() = true
    val isHistorySuggestion: Boolean
        get() = false
    val extras: SuggestionExtras?
        get() = null
    val extraColumns: Collection<String>?
        get() = null

    init {
        mSource = source
        this.userQuery = userQuery
    }
}