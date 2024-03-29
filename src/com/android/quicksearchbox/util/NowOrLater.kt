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
 * Interface for an object that may be constructed asynchronously. In cases when the object is ready
 * (on constructible) immediately, it provides synchronous access to it. Otherwise, the object can
 * be sent to a [Consumer] later.
 */
interface NowOrLater<C> {
  /** Indicates if the object is ready (or constructible synchronously). */
  fun haveNow(): Boolean

  /**
   * Gets the object now. Should only be called if [.haveNow] returns `true`, otherwise an
   * [IllegalStateException] will be thrown.
   */
  val now: C

  /**
   * Request the object asynchronously. This can be called even if the object is ready now, in which
   * case the callback may be made in context. The thread on which the consumer is called back
   * depends on the implementation.
   */
  fun getLater(consumer: Consumer<in C>?)
}
