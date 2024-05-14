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
package androidx.testutils.lifecycle

import android.app.Activity
import android.app.Instrumentation.ActivityMonitor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert

/** Utility methods for testing LifecycleOwners */
public object LifecycleOwnerUtils {

    private const val TIMEOUT_MS: Long = 5000

    private val DO_NOTHING = Runnable {}

    /**
     * Waits until the given [LifecycleOwner] has the specified
     * [androidx.lifecycle.Lifecycle.State]. If the owner has not hit that state within a suitable
     * time period, it asserts that the current state equals the given state.
     */
    @JvmStatic
    @Throws(Throwable::class)
    public fun waitUntilState(owner: LifecycleOwner, state: Lifecycle.State) {
        if (owner.lifecycle.currentState == state) {
            return
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val latch = CountDownLatch(1)

        instrumentation.runOnMainSync {
            if (owner.lifecycle.currentState == state) {
                latch.countDown()
                return@runOnMainSync
            }

            val observer =
                object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (source.lifecycle.currentState == state) {
                            source.lifecycle.removeObserver(this)
                            latch.countDown()
                        }
                    }
                }

            owner.lifecycle.addObserver(observer)
        }

        val isCountZero = latch.await(15, TimeUnit.SECONDS)
        MatcherAssert.assertThat(
            "Expected state $state never happened to $owner. " +
                "Current state: ${owner.lifecycle.currentState}",
            isCountZero,
            CoreMatchers.`is`(true)
        )

        // wait for another loop to ensure all observers are called
        instrumentation.runOnMainSync(DO_NOTHING)
    }

    /**
     * Waits until the given the current [Activity] has been recreated, and the new instance is
     * resumed.
     */
    @Throws(Throwable::class)
    public fun <T> waitForRecreation(
        @Suppress("deprecation") activityRule: androidx.test.rule.ActivityTestRule<T>
    ): T where T : Activity, T : LifecycleOwner {
        return waitForRecreation(activityRule.activity)
    }

    /**
     * Waits until the given the given [Activity] has been recreated, and the new instance is
     * resumed.
     */
    @Throws(Throwable::class)
    public fun <T> waitForRecreation(activity: T): T where T : Activity, T : LifecycleOwner {
        return waitForRecreation(activity, null)
    }

    /**
     * Waits until the given [Activity] and [LifecycleOwner] has been recreated, and the new
     * instance is resumed.
     */
    @Throws(Throwable::class)
    public fun <T> waitForRecreation(activity: T, actionOnUiThread: Runnable?): T where
    T : Activity,
    T : LifecycleOwner {
        val monitor = ActivityMonitor(activity::class.qualifiedName, null, false)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.addMonitor(monitor)

        if (actionOnUiThread != null) {
            instrumentation.runOnMainSync(actionOnUiThread)
        }

        // Wait for the old activity to be destroyed. This helps avoid flakiness on test devices
        // (ex. API 26) where the system takes a long time to go from STOPPED to DESTROYED.
        waitUntilState(activity, Lifecycle.State.DESTROYED)

        var recreatedActivity: T

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate
        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        try {
            synchronized(monitor) {
                do {
                    // The documentation says "Block until an Activity is created
                    // that matches this monitor." This statement is true, but there are some other
                    // true statements like: "Block until an Activity is destroyed" or
                    // "Block until an Activity is resumed"...
                    // this call will release synchronization monitor's monitor
                    @Suppress("UNCHECKED_CAST")
                    recreatedActivity =
                        monitor.waitForActivityWithTimeout(TIMEOUT_MS) as? T
                            ?: throw RuntimeException("Timeout. Activity was not recreated.")
                } while (recreatedActivity == activity)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }

        // Finally wait for the recreated Activity to be resumed
        waitUntilState(recreatedActivity, Lifecycle.State.RESUMED)

        return recreatedActivity
    }
}
