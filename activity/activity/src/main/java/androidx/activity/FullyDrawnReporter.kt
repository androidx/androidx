/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.activity

import android.app.Activity
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/**
 * Manages when to call [Activity.reportFullyDrawn]. Different parts of the UI may individually
 * indicate when they are ready for interaction. [Activity.reportFullyDrawn] will only be called by
 * this class when all parts are ready. At least one [addReporter] or [reportWhenComplete] must be
 * used before [Activity.reportFullyDrawn] will be called by this class.
 *
 * For example, to use coroutines:
 * ```
 * val fullyDrawnReporter = componentActivity.fullyDrawnReporter
 * launch {
 *     fullyDrawnReporter.reportWhenComplete {
 *         dataLoadedMutex.lock()
 *         dataLoadedMutex.unlock()
 *     }
 * }
 * ```
 *
 * Or it can be manually controlled:
 * ```
 * // On the UI thread:
 * fullyDrawnReporter.addReporter()
 *
 * // Do the loading on worker thread:
 * fullyDrawnReporter.removeReporter()
 * ```
 *
 * @param executor The [Executor] on which to call [reportFullyDrawn].
 * @param reportFullyDrawn Will be called when all reporters have been removed.
 */
class FullyDrawnReporter(private val executor: Executor, private val reportFullyDrawn: () -> Unit) {
    private val lock = Any()

    @GuardedBy("lock") private var reporterCount = 0

    @GuardedBy("lock") private var reportPosted = false

    @GuardedBy("lock") private var reportedFullyDrawn = false

    /**
     * Returns `true` after [reportFullyDrawn] has been called or if backed by a [ComponentActivity]
     * and [ComponentActivity.reportFullyDrawn] has been called.
     */
    val isFullyDrawnReported: Boolean
        get() {
            return synchronized(lock) { reportedFullyDrawn }
        }

    @GuardedBy("lock") private val onReportCallbacks = mutableListOf<() -> Unit>()

    private val reportRunnable: Runnable = Runnable {
        synchronized(lock) {
            reportPosted = false
            if (reporterCount == 0 && !reportedFullyDrawn) {
                reportFullyDrawn()
                fullyDrawnReported()
            }
        }
    }

    /** Adds a lock to prevent calling [reportFullyDrawn]. */
    fun addReporter() {
        synchronized(lock) {
            if (!reportedFullyDrawn) {
                reporterCount++
            }
        }
    }

    /**
     * Removes a lock added in [addReporter]. When all locks have been removed, [reportFullyDrawn]
     * will be called on the next animation frame.
     */
    fun removeReporter() {
        synchronized(lock) {
            if (!reportedFullyDrawn && reporterCount > 0) {
                reporterCount--
                postWhenReportersAreDone()
            }
        }
    }

    /**
     * Registers [callback] to be called when [reportFullyDrawn] is called by this class. If it has
     * already been called, then [callback] will be called immediately.
     *
     * Once [callback] has been called, it will be removed and [removeOnReportDrawnListener] does
     * not need to be called to remove it.
     */
    fun addOnReportDrawnListener(callback: () -> Unit) {
        val callImmediately =
            synchronized(lock) {
                if (reportedFullyDrawn) {
                    true
                } else {
                    onReportCallbacks += callback
                    false
                }
            }
        if (callImmediately) {
            callback()
        }
    }

    /**
     * Removes a previously registered [callback] so that it won't be called when [reportFullyDrawn]
     * is called by this class.
     */
    fun removeOnReportDrawnListener(callback: () -> Unit) {
        synchronized(lock) { onReportCallbacks -= callback }
    }

    /**
     * Must be called when when [reportFullyDrawn] is called to indicate that
     * [Activity.reportFullyDrawn] has been called. This method should also be called if
     * [Activity.reportFullyDrawn] has been called outside of this class.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun fullyDrawnReported() {
        synchronized(lock) {
            reportedFullyDrawn = true
            onReportCallbacks.forEach { it() }
            onReportCallbacks.clear()
        }
    }

    /**
     * Posts a request to report that the Activity is fully drawn on the next animation frame. On
     * the next animation frame, it will check again that there are no other reporters that have yet
     * to complete.
     */
    private fun postWhenReportersAreDone() {
        if (!reportPosted && reporterCount == 0) {
            reportPosted = true
            executor.execute(reportRunnable)
        }
    }
}

/**
 * Tells the [FullyDrawnReporter] to wait until [reporter] has completed before calling
 * [Activity.reportFullyDrawn].
 */
suspend inline fun FullyDrawnReporter.reportWhenComplete(
    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE") reporter: suspend () -> Unit
) {
    addReporter()
    if (isFullyDrawnReported) {
        return
    }
    try {
        reporter()
    } finally {
        removeReporter()
    }
}
