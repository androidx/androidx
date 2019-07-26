/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.util.Log
import android.widget.TextView
import androidx.annotation.AnyThread
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Simple opaque activity used to reduce benchmark interference from other windows.
 *
 * For example, sources of potential interference:
 * - live wallpaper rendering
 * - homescreen widget updates
 * - hotword detection
 * - status bar repaints
 * - running in background (some cores may be foreground-app only)
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class IsolationActivity : android.app.Activity() {
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.isolation_activity)

        // disable launch animation
        overridePendingTransition(0, 0)

        if (firstInit) {
            if (!CpuInfo.locked && isSustainedPerformanceModeSupported()) {
                sustainedPerformanceModeInUse = true
                application.registerActivityLifecycleCallbacks(sustainedPerfCallbacks)

                // trigger the one missed lifecycle event, from registering the callbacks late
                sustainedPerfCallbacks.onActivityCreated(this, savedInstanceState)

                // Keep at least one core busy. Together with a single threaded benchmark, this makes
                // the process get multi-threaded setSustainedPerformanceMode.
                //
                // We want to keep to the relatively lower clocks of the multi-threaded benchmark mode
                // to avoid any benchmarks running at higher clocks than any others.
                //
                // Note, thread names have 15 char max in Systrace
                thread(name = "BenchSpinThread") {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST)
                    while (true) {}
                }
            }
            firstInit = false
        }

        val old = singleton.getAndSet(this)
        if (old != null && !old.destroyed && !old.isFinishing) {
            throw IllegalStateException("Only one IsolationActivity should exist")
        }

        findViewById<TextView>(R.id.clock_state).text = when {
            CpuInfo.locked -> "Locked Clocks"
            sustainedPerformanceModeInUse -> "Sustained Performance Mode"
            else -> ""
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
    }

    override fun onPause() {
        super.onPause()
        resumed = false
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyed = true
    }

    /** finish is ignored! we defer until [actuallyFinish] is called. */
    override fun finish() {
    }

    fun actuallyFinish() {
        // disable close animation
        overridePendingTransition(0, 0)
        super.finish()
    }

    companion object {
        private const val TAG = "Benchmark"
        internal val singleton = AtomicReference<IsolationActivity>()
        private var firstInit = true
        internal var sustainedPerformanceModeInUse = false
            private set
        internal var resumed = false
            private set

        @WorkerThread
        fun launchSingleton() {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                Log.d(TAG, "launching Benchmark IsolationActivity")
                setClassName(
                    InstrumentationRegistry.getInstrumentation().targetContext.packageName,
                    IsolationActivity::class.java.name
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            InstrumentationRegistry.getInstrumentation().startActivitySync(intent)
        }

        @AnyThread
        fun finishSingleton() {
            Log.d(TAG, "Benchmark runner being destroyed, tearing down activities")
            singleton.getAndSet(null)?.apply {
                runOnUiThread {
                    actuallyFinish()
                }
            }
        }

        internal fun isSustainedPerformanceModeSupported(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isSustainedPerformanceModeSupported
            } else {
                false
            }

        private val sustainedPerfCallbacks = object : Application.ActivityLifecycleCallbacks {
            @SuppressLint("NewApi") // window API guarded by [isSustainedPerformanceModeSupported]
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                activity.window.setSustainedPerformanceMode(true)
            }
            override fun onActivityDestroyed(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
        }
    }
}