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

import android.util.Log

/**
 * Executor that uses a single thread and an unbounded work queue.
 */
class SingleThreadNamedTaskExecutor(threadFactory: ThreadFactory) : NamedTaskExecutor {
    private val mQueue: LinkedBlockingQueue<NamedTask>
    private val mWorker: Thread

    @Volatile
    private var mClosed = false
    override fun cancelPendingTasks() {
        if (SingleThreadNamedTaskExecutor.Companion.DBG) Log.d(
            SingleThreadNamedTaskExecutor.Companion.TAG,
            "Cancelling " + mQueue.size().toString() + " tasks: " + mWorker.getName()
        )
        if (mClosed) {
            throw IllegalStateException("cancelPendingTasks() after close()")
        }
        mQueue.clear()
    }

    override fun close() {
        mClosed = true
        mWorker.interrupt()
        mQueue.clear()
    }

    override fun execute(task: NamedTask?) {
        if (mClosed) {
            throw IllegalStateException("execute() after close()")
        }
        mQueue.add(task)
    }

    private inner class Worker : Runnable {
        fun run() {
            try {
                loop()
            } finally {
                if (!mClosed) Log.w(
                    SingleThreadNamedTaskExecutor.Companion.TAG,
                    "Worker exited before close"
                )
            }
        }

        private fun loop() {
            val currentThread: Thread = Thread.currentThread()
            val threadName: String = currentThread.getName()
            while (!mClosed) {
                var task: NamedTask
                task = try {
                    mQueue.take()
                } catch (ex: InterruptedException) {
                    continue
                }
                currentThread.setName(threadName + " " + task.getName())
                try {
                    if (SingleThreadNamedTaskExecutor.Companion.DBG) Log.d(
                        SingleThreadNamedTaskExecutor.Companion.TAG,
                        "Running task " + task.getName()
                    )
                    task.run()
                    if (SingleThreadNamedTaskExecutor.Companion.DBG) Log.d(
                        SingleThreadNamedTaskExecutor.Companion.TAG,
                        "Task " + task.getName() + " complete"
                    )
                } catch (ex: RuntimeException) {
                    Log.e(
                        SingleThreadNamedTaskExecutor.Companion.TAG,
                        "Task " + task.getName() + " failed",
                        ex
                    )
                }
            }
        }
    }

    companion object {
        private const val DBG = false
        private const val TAG = "QSB.SingleThreadNamedTaskExecutor"
        @JvmStatic
        fun factory(threadFactory: ThreadFactory?): Factory<NamedTaskExecutor> {
            return object : Factory<NamedTaskExecutor?> {
                override fun create(): NamedTaskExecutor {
                    return SingleThreadNamedTaskExecutor(threadFactory)
                }
            }
        }
    }

    init {
        mQueue = LinkedBlockingQueue<NamedTask>()
        mWorker = threadFactory.newThread(SingleThreadNamedTaskExecutor.Worker())
        mWorker.start()
    }
}