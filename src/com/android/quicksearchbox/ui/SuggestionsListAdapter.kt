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

package com.android.quicksearchbox.ui

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListAdapter
import com.android.quicksearchbox.SuggestionCursor
import com.android.quicksearchbox.SuggestionPosition

/** Uses a [Suggestions] object to back a [SuggestionsView]. */
class SuggestionsListAdapter(viewFactory: SuggestionViewFactory?) :
  SuggestionsAdapterBase<ListAdapter?>(viewFactory!!) {
  private val mAdapter: SuggestionsListAdapter.Adapter

  @get:Override
  override val isEmpty: Boolean
    get() = mAdapter.getCount() == 0

  @Override
  override fun getSuggestion(suggestionId: Long): SuggestionPosition? {
    return SuggestionPosition(getCurrentSuggestions(), suggestionId.toInt())
  }

  @get:Override
  override val listAdapter: BaseAdapter
    get() = mAdapter

  @Override
  public override fun notifyDataSetChanged() {
    mAdapter.notifyDataSetChanged()
  }

  @Override
  public override fun notifyDataSetInvalidated() {
    mAdapter.notifyDataSetInvalidated()
  }

  internal inner class Adapter : BaseAdapter() {
    @Override
    fun getCount(): Int {
      val s: SuggestionCursor = getCurrentSuggestions()
      return if (s == null) 0 else s.getCount()
    }

    @Override
    fun getItem(position: Int): Object? {
      return getSuggestion(position)
    }

    @Override
    fun getItemId(position: Int): Long {
      return position.toLong()
    }

    @Override
    fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
      return this@SuggestionsListAdapter.getView(
        getCurrentSuggestions(),
        position,
        position.toLong(),
        convertView,
        parent
      )
    }

    @Override
    fun getItemViewType(position: Int): Int {
      return getSuggestionViewType(getCurrentSuggestions(), position)
    }

    @Override
    fun getViewTypeCount(): Int {
      return getSuggestionViewTypeCount()
    }
  }

  init {
    mAdapter = SuggestionsListAdapter.Adapter()
  }
}
