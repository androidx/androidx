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
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * Internal class that dispatches initialization events.
 */
@Suppress("DEPRECATION")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
open class ReportFragment() : android.app.Fragment() {
    private var processListener: ActivityInitializationListener? = null

    private fun dispatchCreate(listener: ActivityInitializationListener?) {
        listener?.onCreate()
    }

    private fun dispatchStart(listener: ActivityInitializationListener?) {
        listener?.onStart()
    }

    private fun dispatchResume(listener: ActivityInitializationListener?) {
        listener?.onResume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dispatchCreate(processListener)
        dispatch(Lifecycle.Event.ON_CREATE)
    }

    override fun onStart() {
        super.onStart()
        dispatchStart(processListener)
        dispatch(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        dispatchResume(processListener)
        dispatch(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        super.onPause()
        dispatch(Lifecycle.Event.ON_PAUSE)
    }

    override fun onStop() {
        super.onStop()
        dispatch(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        dispatch(Lifecycle.Event.ON_DESTROY)
        // just want to be sure that we won't leak reference to an activity
        processListener = null
    }

    private fun dispatch(event: Lifecycle.Event) {
        if (Build.VERSION.SDK_INT < 29) {
            // Only dispatch events from ReportFragment on API levels prior
            // to API 29. On API 29+, this is handled by the ActivityLifecycleCallbacks
            // added in ReportFragment.injectIfNeededIn
            dispatch(activity, event)
        }
    }

    fun setProcessListener(processListener: ActivityInitializationListener?) {
        this.processListener = processListener
    }

    interface ActivityInitializationListener {
        fun onCreate()
        fun onStart()
        fun onResume()
    }

    // this class isn't inlined only because we need to add a proguard rule for it (b/142778206)
    // In addition to that registerIn method allows to avoid class verification failure,
    // because registerActivityLifecycleCallbacks is available only since api 29.
    @RequiresApi(29)
    internal class LifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(
            activity: Activity,
            bundle: Bundle?
        ) {}

        override fun onActivityPostCreated(
            activity: Activity,
            savedInstanceState: Bundle?
        ) {
            dispatch(activity, Lifecycle.Event.ON_CREATE)
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityPostStarted(activity: Activity) {
            dispatch(activity, Lifecycle.Event.ON_START)
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPostResumed(activity: Activity) {
            dispatch(activity, Lifecycle.Event.ON_RESUME)
        }

        override fun onActivityPrePaused(activity: Activity) {
            dispatch(activity, Lifecycle.Event.ON_PAUSE)
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityPreStopped(activity: Activity) {
            dispatch(activity, Lifecycle.Event.ON_STOP)
        }

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(
            activity: Activity,
            bundle: Bundle
        ) {}

        override fun onActivityPreDestroyed(activity: Activity) {
            dispatch(activity, Lifecycle.Event.ON_DESTROY)
        }

        override fun onActivityDestroyed(activity: Activity) {}

        companion object {
            @JvmStatic
            fun registerIn(activity: Activity) {
                activity.registerActivityLifecycleCallbacks(LifecycleCallbacks())
            }
        }
    }

    companion object {
        private const val REPORT_FRAGMENT_TAG =
            "androidx.lifecycle.LifecycleDispatcher.report_fragment_tag"

        @JvmStatic
        fun injectIfNeededIn(activity: Activity) {
            if (Build.VERSION.SDK_INT >= 29) {
                // On API 29+, we can register for the correct Lifecycle callbacks directly
                LifecycleCallbacks.registerIn(activity)
            }
            // Prior to API 29 and to maintain compatibility with older versions of
            // ProcessLifecycleOwner (which may not be updated when lifecycle-runtime is updated and
            // need to support activities that don't extend from FragmentActivity from support lib),
            // use a framework fragment to get the correct timing of Lifecycle events
            val manager = activity.fragmentManager
            if (manager.findFragmentByTag(REPORT_FRAGMENT_TAG) == null) {
                manager.beginTransaction().add(ReportFragment(), REPORT_FRAGMENT_TAG).commit()
                // Hopefully, we are the first to make a transaction.
                manager.executePendingTransactions()
            }
        }

        @JvmStatic
        internal fun dispatch(activity: Activity, event: Lifecycle.Event) {
            if (activity is LifecycleRegistryOwner) {
                activity.lifecycle.handleLifecycleEvent(event)
                return
            }
            if (activity is LifecycleOwner) {
                val lifecycle = (activity as LifecycleOwner).lifecycle
                if (lifecycle is LifecycleRegistry) {
                    lifecycle.handleLifecycleEvent(event)
                }
            }
        }

        @JvmStatic
        @get:JvmName("get")
        val Activity.reportFragment: ReportFragment
            get() {
                return this.fragmentManager.findFragmentByTag(
                    REPORT_FRAGMENT_TAG
                ) as ReportFragment
            }
    }
}
