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

package com.android.quicksearchbox.util

/**
 * This class represents the matrix used in the Levenshtein distance algorithm, together with the
 * algorithm itself which operates on the matrix.
 *
 * We also track of the individual operations applied to transform the source string into the target
 * string so we can trace the path taken through the matrix afterwards, in order to perform the
 * formatting as required.
 */
class LevenshteinDistance(
  source: Array<LevenshteinDistance.Token>,
  target: Array<LevenshteinDistance.Token>
) {
  private val mSource: Array<LevenshteinDistance.Token>
  private val mTarget: Array<LevenshteinDistance.Token>
  private val mEditTypeTable: Array<IntArray>
  private val mDistanceTable: Array<IntArray>

  /**
   * Implementation of Levenshtein distance algorithm.
   *
   * @return The Levenshtein distance.
   */
  fun calculate(): Int {
    val src = mSource
    val trg = mTarget
    val sourceLen = src.size
    val targetLen = trg.size
    val distTab = mDistanceTable
    val editTab = mEditTypeTable
    for (s in 1..sourceLen) {
      val sourceToken = src[s - 1]
      for (t in 1..targetLen) {
        val targetToken = trg[t - 1]
        val cost = if (sourceToken.prefixOf(targetToken)) 0 else 1
        var distance = distTab[s - 1][t] + 1
        var type: Int = LevenshteinDistance.Companion.EDIT_DELETE
        var d = distTab[s][t - 1]
        if (d + 1 < distance) {
          distance = d + 1
          type = LevenshteinDistance.Companion.EDIT_INSERT
        }
        d = distTab[s - 1][t - 1]
        if (d + cost < distance) {
          distance = d + cost
          type =
            if (cost == 0) LevenshteinDistance.Companion.EDIT_UNCHANGED
            else LevenshteinDistance.Companion.EDIT_REPLACE
        }
        distTab[s][t] = distance
        editTab[s][t] = type
      }
    }
    return distTab[sourceLen][targetLen]
  }

  /**
   * Gets the list of operations which were applied to each target token; [.calculate] must have
   * been called on this object before using this method.
   * @return A list of [EditOperation]s indicating the origin of each token in the target string.
   * The position of the token indicates the position in the source string of the token that was
   * unchanged/replaced, or the position in the source after which a target token was inserted.
   */
  val targetOperations: Array<EditOperation?>
    get() {
      val trgLen = mTarget.size
      val ops = arrayOfNulls<EditOperation>(trgLen)
      var targetPos = trgLen
      var sourcePos = mSource.size
      val editTab = mEditTypeTable
      while (targetPos > 0) {
        val editType = editTab[sourcePos][targetPos]
        when (editType) {
          LevenshteinDistance.Companion.EDIT_DELETE -> sourcePos--
          LevenshteinDistance.Companion.EDIT_INSERT -> {
            targetPos--
            ops[targetPos] = EditOperation(editType, sourcePos)
          }
          LevenshteinDistance.Companion.EDIT_UNCHANGED,
          LevenshteinDistance.Companion.EDIT_REPLACE -> {
            targetPos--
            sourcePos--
            ops[targetPos] = EditOperation(editType, sourcePos)
          }
        }
      }
      return ops
    }

  class EditOperation(val type: Int, val position: Int)
  class Token(private val mContainer: CharArray, val mStart: Int, val mEnd: Int) : CharSequence {
    fun length(): Int {
      return mEnd - mStart
    }

    @Override
    override fun toString(): String {
      // used in tests only.
      return subSequence(0, length())
    }

    fun prefixOf(that: LevenshteinDistance.Token): Boolean {
      val len = length()
      if (len > that.length()) return false
      val thisStart = mStart
      val thatStart: Int = that.mStart
      val thisContainer = mContainer
      val thatContainer: CharArray = that.mContainer
      for (i in 0 until len) {
        if (thisContainer[thisStart + i] != thatContainer[thatStart + i]) {
          return false
        }
      }
      return true
    }

    fun charAt(index: Int): Char {
      return mContainer[index + mStart]
    }

    override fun subSequence(start: Int, end: Int): String {
      return String(mContainer, mStart + start, length())
    }
  }

  companion object {
    const val EDIT_DELETE = 0
    const val EDIT_INSERT = 1
    const val EDIT_REPLACE = 2
    const val EDIT_UNCHANGED = 3
  }

  init {
    val sourceSize = source.size
    val targetSize = target.size
    val editTab = Array(sourceSize + 1) { IntArray(targetSize + 1) }
    val distTab = Array(sourceSize + 1) { IntArray(targetSize + 1) }
    editTab[0][0] = LevenshteinDistance.Companion.EDIT_UNCHANGED
    distTab[0][0] = 0
    for (i in 1..sourceSize) {
      editTab[i][0] = LevenshteinDistance.Companion.EDIT_DELETE
      distTab[i][0] = i
    }
    for (i in 1..targetSize) {
      editTab[0][i] = LevenshteinDistance.Companion.EDIT_INSERT
      distTab[0][i] = i
    }
    mEditTypeTable = editTab
    mDistanceTable = distTab
    mSource = source
    mTarget = target
  }
}
