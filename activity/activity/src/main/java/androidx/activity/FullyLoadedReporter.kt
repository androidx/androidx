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
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Looper
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi

/**
 * Manages when to call [Activity.reportFullyDrawn]. Different parts of the UI may
 * individually indicate when they are ready for interaction. [Activity.reportFullyDrawn]
 * will only be called when all parts are ready. At least one [addReporter] or
 * [reportWhenComplete] must be used before [Activity.reportFullyDrawn] will be called.
 *
 * For example, to use coroutines:
 * ```
 * val fullyDrawnReporter = FullyLoadedReporter.findFullyLoadedReporter(context)
 * launch {
 *     fullyDrawnReporter.reportWhenComplete {
 *         dataLoadedMutex.lock()
 *         dataLoadedMutex.unlock()
 *     }
 * }
 * ```
 * Or it can be manually controlled:
 * ```
 * // On worker thread:
 * FullyLoadedReporter.findFullyLoadedReporter(context).addReporter()
 * // Do the loading
 * FullyLoadedReporter.findFullyLoadedReporter(context).removeReporter()
 * ```
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
class FullyLoadedReporter private constructor(val activity: Activity) {
    private val lock = Any()

    private val decorView = activity.window.decorView

    @GuardedBy("lock")
    private var reporterCount = 0

    @GuardedBy("lock")
    private var reportPosted = false

    @GuardedBy("lock")
    private var reportedFullyDrawn = false

    /**
     * Returns `true` if [Activity.reportFullyDrawn] has been called or `false` otherwise.
     */
    val hasReported: Boolean
        get() {
            return synchronized(lock) { reportedFullyDrawn }
        }

    @GuardedBy("lock")
    private val onReportCallbacks = mutableListOf<() -> Unit>()

    private val reportOnAnimation: Runnable = Runnable {
        synchronized(lock) {
            reportPosted = false
            if (reporterCount == 0) {
                reportedFullyDrawn = true
                activity.reportFullyDrawn()
                onReportCallbacks.forEach { it() }
                onReportCallbacks.clear()
            }
        }
    }

    /**
     * Adds a lock to prevent calling [Activity.reportFullyDrawn].
     */
    fun addReporter() {
        synchronized(lock) {
            if (!reportedFullyDrawn) {
                reporterCount++
            }
        }
    }

    /**
     * Removes a lock added in [addReporter]. When all locks have been removed,
     * [Activity.reportFullyDrawn] will be called on the next animation frame.
     */
    fun removeReporter() {
        synchronized(lock) {
            if (!reportedFullyDrawn) {
                check(reporterCount > 0) {
                    "removeReporter() called when all reporters have already been removed."
                }
                reporterCount--
                postWhenReportersAreDone()
            }
        }
    }

    /**
     * Registers [callback] to be called when [Activity.reportFullyDrawn] is called. If
     * it has already been called, then [callback] will be called immediately.
     */
    fun addOnReportLoadedListener(callback: () -> Unit) {
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
     * Removes a previously registered [callback] so that it won't be called when
     * [Activity.reportFullyDrawn] is called.
     */
    fun removeOnReportLoadedListener(callback: () -> Unit) {
        synchronized(lock) {
            onReportCallbacks -= callback
        }
    }

    /**
     * Posts a request to report that the Activity is fully drawn on the next animation frame.
     * On the next animation frame, it will check again that there are no other reporters
     * that have yet to complete.
     */
    private fun postWhenReportersAreDone() {
        if (!reportPosted && reporterCount == 0) {
            reportPosted = true
            decorView.postOnAnimation(reportOnAnimation)
            // Invalidate just in case no drawing was already scheduled
            if (Looper.myLooper() != activity.mainLooper) {
                decorView.postInvalidate()
            } else {
                decorView.invalidate()
            }
        }
    }

    companion object {
        /**
         * Finds the [FullyLoadedReporter] associated with the activity, creating one if
         * necessary. `null` will be returned if [context] is not associated with
         * an [Activity].
         */
        @JvmStatic
        @MainThread
        fun findFullyLoadedReporter(context: Context): FullyLoadedReporter? {
            val activity = unwrapContext(context) ?: return null
            val decorView = activity.window.decorView
            return decorView.getTag(R.id.report_loaded) as? FullyLoadedReporter
                ?: FullyLoadedReporter(activity).also {
                    decorView.setTag(R.id.report_loaded, it)
                }
        }

        @JvmStatic
        internal fun unwrapContext(wrappedContext: Context): Activity? {
            var context: Context? = wrappedContext
            while (true) {
                when (context) {
                    is Activity -> return context
                    is ContextWrapper -> context = context.baseContext
                    else -> return null
                }
            }
        }
    }
}

/**
 * Tells the [FullyLoadedReporter] to wait until [reporter] has completed
 * before calling [Activity.reportFullyDrawn].
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
suspend inline fun FullyLoadedReporter.reportWhenComplete(
    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
    reporter: suspend () -> Unit
) {
    addReporter()
    if (hasReported) {
        return
    }
    try {
        reporter()
    } finally {
        removeReporter()
    }
}