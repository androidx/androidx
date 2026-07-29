/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.testutils

import android.app.Activity
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.ComponentName
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.getValue
import kotlin.test.assertNotNull
import kotlin.test.fail

abstract class TestAppTest(val activityClass: Class<out Activity>) {
    protected val instrumentation: Instrumentation by lazy {
        InstrumentationRegistry.getInstrumentation()
    }
    protected val uiAutomation: UiAutomation by lazy { instrumentation.uiAutomation }

    protected fun startActivity(): Activity {
        val monitor = instrumentation.addMonitor(activityClass.name, null, false)
        val packageName = instrumentation.targetContext.packageName
        val componentName = ComponentName(packageName, activityClass.name)
        val command = "am start -f 0x10008000 -n ${componentName.flattenToString()}"
        uiAutomation.executeShellCommand(command).checkError()
        val activity =
            assertNotNull(
                monitor.waitForActivityWithTimeout(20000),
                "Failed to launch Activity ${activityClass.name} within 20 seconds timeout.",
            )

        instrumentation.removeMonitor(monitor)

        // Wait for the main thread to be idle
        instrumentation.waitForIdleSync()

        return activity
    }

    protected fun takeScreenshotWithTimeout(timeoutMillis: Long = 8000L): Bitmap {
        val startTime = System.currentTimeMillis()
        var screenshot = uiAutomation.takeScreenshot()
        while (screenshot == null && (System.currentTimeMillis() - startTime) < timeoutMillis) {
            CountDownLatch(1).await(200, TimeUnit.MILLISECONDS)
            instrumentation.waitForIdleSync()
            screenshot = uiAutomation.takeScreenshot()
        }
        return assertNotNull(
            screenshot,
            "Failed to take screenshot for Activity ${activityClass.name} within ${timeoutMillis}ms.",
        )
    }

    protected fun assertScreenshotChanged(screenshotBefore: Bitmap, timeoutMillis: Long = 8000L) {
        val startTime = System.currentTimeMillis()
        var screenshotAfter = takeScreenshotWithTimeout(timeoutMillis)
        while (
            screenshotBefore.sameAs(screenshotAfter) &&
                (System.currentTimeMillis() - startTime) < timeoutMillis
        ) {
            // waitForIdleSync may return too quickly on its own, using a CountDownLatch to give the
            // compositor some time to update the UI. This only happens in cases where the UI
            // has not immediately rendered, so we wait some time for the screen to update
            CountDownLatch(1).await(200, TimeUnit.MILLISECONDS)
            instrumentation.waitForIdleSync()
            screenshotAfter = takeScreenshotWithTimeout(timeoutMillis)
        }
        if (screenshotBefore.sameAs(screenshotAfter)) {
            fail(
                "The screenshot did not change after launching Activity ${activityClass.name}. " +
                    "The screen UI might not have rendered correctly."
            )
        }
    }

    protected fun assertGarbageCollected(weakRef: WeakReference<*>, maxAttempts: Int = 20) {
        var attempts = 0
        while ((weakRef.get() != null) && (attempts < maxAttempts)) {
            Runtime.getRuntime().gc()
            System.runFinalization()
            CountDownLatch(1).await(100, TimeUnit.MILLISECONDS)
            attempts++
        }

        weakRef.get()?.let { fail("Activity ${it.toString()} was not garbage collected.") }
    }
}
