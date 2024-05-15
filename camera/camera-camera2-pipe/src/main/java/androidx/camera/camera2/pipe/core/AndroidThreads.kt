/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.core

import android.os.Process
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import kotlinx.atomicfu.atomic

internal object AndroidThreads {

    /**
     * Pulled from kNiceValues in AOSP.
     *
     * This is a map of Java thread priorities which range from 1 to 10 (10 being highest priority)
     * Android thread priorities, which range from about 20 to -20 (-20 being highest priority).
     *
     * For this thread priority mapping:
     * - 1 is mapped to 19 (lowest)
     * - 5 is mapped to 0 (default)
     * - 10 is mapped to -8 (urgent display)
     */
    private val NICE_VALUES =
        intArrayOf(
            Process.THREAD_PRIORITY_LOWEST, // 1 (Thread.MIN_PRIORITY)
            Process.THREAD_PRIORITY_BACKGROUND + 6,
            Process.THREAD_PRIORITY_BACKGROUND + 3,
            Process.THREAD_PRIORITY_BACKGROUND,
            Process.THREAD_PRIORITY_DEFAULT, // 5 (Thread.NORM_PRIORITY)
            Process.THREAD_PRIORITY_DEFAULT - 2,
            Process.THREAD_PRIORITY_DEFAULT - 4,
            Process.THREAD_PRIORITY_URGENT_DISPLAY + 3,
            Process.THREAD_PRIORITY_URGENT_DISPLAY + 2,
            Process.THREAD_PRIORITY_URGENT_DISPLAY // 10 (Thread.MAX_PRIORITY)
        )

    val factory: ThreadFactory = Executors.defaultThreadFactory()

    /** Wraps `delegate` such that the threads created by it are set to `priority`. */
    fun ThreadFactory.withAndroidPriority(androidPriority: Int): ThreadFactory {
        return ThreadFactory { runnable ->
            val javaPriority = androidToJavaPriority(androidPriority)
            val thread: Thread =
                this.newThread {
                    // Set the Android thread priority once the thread actually starts running.
                    Process.setThreadPriority(androidPriority)
                    runnable.run()
                }

            // Setting java priority internally sets the android priority, but not vice versa.
            // By setting the java priority here, we ensure that the priority is set to the same or
            // higher priority when the thread starts so that it is scheduled quickly. When the
            // runnable executes, the Android priority, which is more fine grained, is set before
            // the wrapped runnable executes.
            thread.priority = javaPriority
            thread
        }
    }

    /**
     * Create a [ThreadFactory] that will track the number of threads that have been created and
     * assigns a name based on the prefix and the number of threads that have been created by this
     * factory.
     */
    fun ThreadFactory.withPrefix(namePrefix: String): ThreadFactory {
        val counter = atomic(0)
        return ThreadFactory { runnable ->
            val thread: Thread = this.newThread(runnable)
            thread.name = namePrefix + counter.incrementAndGet().toString().padStart(2, '0')
            thread
        }
    }

    /** Create a new fixed size thread pool using [Executors.newFixedThreadPool]. */
    fun ThreadFactory.asFixedSizeThreadPool(threads: Int): ExecutorService {
        require(threads > 0) { "Threads ($threads) must be > 0" }
        return Executors.newFixedThreadPool(threads, this)
    }

    /** Create a new scheduled thread pool using [Executors.newScheduledThreadPool]. */
    fun ThreadFactory.asScheduledThreadPool(threads: Int): ScheduledExecutorService {
        require(threads > 0) { "Threads ($threads) must be > 0" }
        return Executors.newScheduledThreadPool(threads, this)
    }

    /** Create a new cached thread pool using [Executors.newCachedThreadPool]. */
    fun ThreadFactory.asCachedThreadPool(): ExecutorService = Executors.newCachedThreadPool(this)

    private fun androidToJavaPriority(androidPriority: Int): Int {
        // Err on the side of increased priority.
        for (i in NICE_VALUES.indices) {
            if (androidPriority >= NICE_VALUES[i]) {
                return i + 1
            }
        }
        return Thread.MAX_PRIORITY
    }
}
