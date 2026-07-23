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
import android.content.ComponentName
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertNotNull
import kotlin.test.fail
import org.junit.Test

/**
 * Abstract parameterized base test class for smoke testing XR Activities. Other libraries can
 * subclass this to automatically inherit these smoke tests.
 */
public abstract class TestAppSmokeTest(public val activityClass: Class<out Activity>) {

    @Test
    @XrDeviceTest
    @Suppress("BanThreadSleep")
    public fun activity_loadsAndShowsUi() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation = instrumentation.uiAutomation
        val screenshotBefore = uiAutomation.takeScreenshot()
        val packageName = instrumentation.targetContext.packageName
        val componentName = ComponentName(packageName, activityClass.name)
        val monitor = instrumentation.addMonitor(activityClass.name, null, false)
        val command = "am start -f 0x10008000 -n ${componentName.flattenToString()}"
        uiAutomation.executeShellCommand(command).checkError()

        val activity = monitor.waitForActivityWithTimeout(20000)
        assertNotNull(
            activity,
            "Failed to launch Activity ${activityClass.name} within 20 seconds timeout.",
        )

        // Wait for the main thread to be idle
        instrumentation.waitForIdleSync()

        var screenshotAfter = uiAutomation.takeScreenshot()

        // Verify that the screenshot changed (allow up to 8 seconds for slow emulation)
        if (screenshotBefore != null) {
            var attempts = 0
            while (
                attempts < 40 &&
                    (screenshotAfter == null || screenshotBefore.sameAs(screenshotAfter))
            ) {
                // waitForIdleSync may return too quickly on its own, using Thread.sleep to give the
                // compositor some time to update the UI. This only happens in cases where the UI
                // has not immediately rendered, so we wait some time for the screen to update
                Thread.sleep(200)
                instrumentation.waitForIdleSync()
                screenshotAfter = uiAutomation.takeScreenshot()
                attempts++
            }
            assertNotNull(
                screenshotAfter,
                "Failed to take screenshot after launching Activity ${activityClass.name}.",
            )
            if (screenshotBefore.sameAs(screenshotAfter)) {
                fail(
                    "The screenshot did not change after launching Activity ${activityClass.name}. " +
                        "The screen UI might not have rendered correctly."
                )
            }
        }

        // Finish the Activity to close it
        instrumentation.runOnMainSync { activity.finish() }

        // Wait for the main thread to be idle again
        instrumentation.waitForIdleSync()
    }
}
