/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ReportFragment.Companion.reportFragment

/**
 * Class that provides lifecycle for the whole application process.
 *
 * You can consider this LifecycleOwner as the composite of all of your Activities, except that
 * [Lifecycle.Event.ON_CREATE] will be dispatched once and [Lifecycle.Event.ON_DESTROY]
 * will never be dispatched. Other lifecycle events will be dispatched with following rules:
 * ProcessLifecycleOwner will dispatch [Lifecycle.Event.ON_START],
 * [Lifecycle.Event.ON_RESUME] events, as a first activity moves through these events.
 * [Lifecycle.Event.ON_PAUSE], [Lifecycle.Event.ON_STOP], events will be dispatched with
 * a **delay** after a last activity
 * passed through them. This delay is long enough to guarantee that ProcessLifecycleOwner
 * won't send any events if activities are destroyed and recreated due to a
 * configuration change.
 *
 * It is useful for use cases where you would like to react on your app coming to the foreground or
 * going to the background and you don't need a milliseconds accuracy in receiving lifecycle
 * events.
 */
class ProcessLifecycleOwner private constructor() : LifecycleOwner {
    // ground truth counters
    private var startedCounter = 0
    private var resumedCounter = 0
    private var pauseSent = true
    private var stopSent = true
    private var handler: Handler? = null
    private val registry = LifecycleRegistry(this)
    private val delayedPauseRunnable = Runnable {
        dispatchPauseIfNeeded()
        dispatchStopIfNeeded()
    }
    private val initializationListener: ReportFragment.ActivityInitializationListener =
        object : ReportFragment.ActivityInitializationListener {
            override fun onCreate() {}

            override fun onStart() {
                activityStarted()
            }

            override fun onResume() {
                activityResumed()
            }
        }

    companion object {
        @VisibleForTesting
        internal const val TIMEOUT_MS: Long = 700 // mls
        private val newInstance = ProcessLifecycleOwner()

        /**
         * The LifecycleOwner for the whole application process. Note that if your application
         * has multiple processes, this provider does not know about other processes.
         *
         * @return [LifecycleOwner] for the whole application.
         */
        @JvmStatic
        fun get(): LifecycleOwner {
            return newInstance
        }

        @JvmStatic
        internal fun init(context: Context) {
            newInstance.attach(context)
        }
    }

    internal fun activityStarted() {
        startedCounter++
        if (startedCounter == 1 && stopSent) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            stopSent = false
        }
    }

    internal fun activityResumed() {
        resumedCounter++
        if (resumedCounter == 1) {
            if (pauseSent) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                pauseSent = false
            } else {
                handler!!.removeCallbacks(delayedPauseRunnable)
            }
        }
    }

    internal fun activityPaused() {
        resumedCounter--
        if (resumedCounter == 0) {
            handler!!.postDelayed(delayedPauseRunnable, TIMEOUT_MS)
        }
    }

    internal fun activityStopped() {
        startedCounter--
        dispatchStopIfNeeded()
    }

    internal fun dispatchPauseIfNeeded() {
        if (resumedCounter == 0) {
            pauseSent = true
            registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
    }

    internal fun dispatchStopIfNeeded() {
        if (startedCounter == 0 && pauseSent) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            stopSent = true
        }
    }

    @Suppress("DEPRECATION")
    internal fun attach(context: Context) {
        handler = Handler()
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(object : EmptyActivityLifecycleCallbacks() {
            @RequiresApi(29)
            override fun onActivityPreCreated(
                activity: Activity,
                savedInstanceState: Bundle?
            ) {
                // We need the ProcessLifecycleOwner to get ON_START and ON_RESUME precisely
                // before the first activity gets its LifecycleOwner started/resumed.
                // The activity's LifecycleOwner gets started/resumed via an activity registered
                // callback added in onCreate(). By adding our own activity registered callback in
                // onActivityPreCreated(), we get our callbacks first while still having the
                // right relative order compared to the Activity's onStart()/onResume() callbacks.
                Api29Impl.registerActivityLifecycleCallbacks(activity,
                    object : EmptyActivityLifecycleCallbacks() {
                        override fun onActivityPostStarted(activity: Activity) {
                            activityStarted()
                        }

                        override fun onActivityPostResumed(activity: Activity) {
                            activityResumed()
                        }
                    })
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Only use ReportFragment pre API 29 - after that, we can use the
                // onActivityPostStarted and onActivityPostResumed callbacks registered in
                // onActivityPreCreated()
                if (Build.VERSION.SDK_INT < 29) {
                    activity.reportFragment.setProcessListener(initializationListener)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                activityPaused()
            }

            override fun onActivityStopped(activity: Activity) {
                activityStopped()
            }
        })
    }

    override val lifecycle: Lifecycle
        get() = registry

    @RequiresApi(29)
    internal object Api29Impl {
        @DoNotInline
        @JvmStatic
        fun registerActivityLifecycleCallbacks(
            activity: Activity,
            callback: Application.ActivityLifecycleCallbacks
        ) {
            activity.registerActivityLifecycleCallbacks(callback)
        }
    }
}
