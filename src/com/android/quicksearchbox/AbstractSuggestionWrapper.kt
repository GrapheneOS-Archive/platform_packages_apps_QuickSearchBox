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

import android.content.ComponentName

/**
 * A Suggestion that delegates all calls to other suggestions.
 */
abstract class AbstractSuggestionWrapper : Suggestion {
    /**
     * Gets the current suggestion.
     */
    protected abstract fun current(): Suggestion
    override val shortcutId: String
        get() = current().getShortcutId()
    override val suggestionFormat: String
        get() = current().getSuggestionFormat()
    override val suggestionIcon1: String
        get() = current().getSuggestionIcon1()
    override val suggestionIcon2: String
        get() = current().getSuggestionIcon2()
    override val suggestionIntentAction: String
        get() = current().getSuggestionIntentAction()
    override val suggestionIntentComponent: ComponentName
        get() = current().getSuggestionIntentComponent()
    override val suggestionIntentDataString: String
        get() = current().getSuggestionIntentDataString()
    override val suggestionIntentExtraData: String
        get() = current().getSuggestionIntentExtraData()
    override val suggestionLogType: String
        get() = current().getSuggestionLogType()
    override val suggestionQuery: String
        get() = current().getSuggestionQuery()
    override val suggestionSource: Source
        get() = current().getSuggestionSource()
    override val suggestionText1: String
        get() = current().getSuggestionText1()
    override val suggestionText2: String
        get() = current().getSuggestionText2()
    override val suggestionText2Url: String
        get() = current().getSuggestionText2Url()
    override val isSpinnerWhileRefreshing: Boolean
        get() = current().isSpinnerWhileRefreshing()
    override val isSuggestionShortcut: Boolean
        get() = current().isSuggestionShortcut()
    override val isWebSearchSuggestion: Boolean
        get() = current().isWebSearchSuggestion()
    override val isHistorySuggestion: Boolean
        get() = current().isHistorySuggestion()
    override val extras: SuggestionExtras
        get() = current().getExtras()
}