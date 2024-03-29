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

import android.database.sqlite.SQLiteDatabase

/** Abstract helper base class for SQLite write transactions. */
abstract class SQLiteTransaction {
  /**
   * Executes the statements that form the transaction.
   *
   * @param db A writable database.
   * @return `true` if the transaction should be committed.
   */
  protected abstract fun performTransaction(db: SQLiteDatabase?): Boolean

  /**
   * Runs the transaction against the database. The results are committed if [.performTransaction]
   * completes normally and returns `true`.
   */
  fun run(db: SQLiteDatabase) {
    db.beginTransaction()
    try {
      if (performTransaction(db)) {
        db.setTransactionSuccessful()
      }
    } finally {
      db.endTransaction()
    }
  }
}
