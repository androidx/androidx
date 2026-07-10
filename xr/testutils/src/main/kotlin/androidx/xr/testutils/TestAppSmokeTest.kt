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
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Abstract parameterized base test class for smoke testing XR Activities. Other libraries can
 * subclass this to automatically inherit these smoke tests.
 */
public abstract class TestAppSmokeTest(public val activityClass: Class<out Activity>) {

    @Test
    @XrDeviceTest
    public fun activity_loadsAndShowsUi() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation = instrumentation.uiAutomation
        val screenshotBefore = uiAutomation.takeScreenshot()
        val packageName = instrumentation.targetContext.packageName
        val componentName = ComponentName(packageName, activityClass.name)
        val monitor = instrumentation.addMonitor(activityClass.name, null, false)
        val command = "am start -f 0x10008000 -n ${componentName.flattenToString()}"
        uiAutomation.executeShellCommand(command)

        val activity = monitor.waitForActivityWithTimeout(20000)
        assertThat(activity).isNotNull()

        // Wait for the main thread to be idle
        instrumentation.waitForIdleSync()

        var screenshotAfter = uiAutomation.takeScreenshot()

        // Verify that the screenshot changed (allow up to 5 seconds for slow emulation
        // environments)
        if (screenshotBefore != null) {
            var attempts = 0
            while (
                attempts < 10 &&
                    (screenshotAfter == null || screenshotBefore.sameAs(screenshotAfter))
            ) {
                instrumentation.waitForIdleSync()
                screenshotAfter = uiAutomation.takeScreenshot()
                attempts++
            }
            assertThat(screenshotAfter).isNotNull()
            assertThat(screenshotBefore.sameAs(screenshotAfter)).isFalse()
        }

        // Finish the Activity to close it
        instrumentation.runOnMainSync { activity.finish() }

        // Wait for the main thread to be idle again
        instrumentation.waitForIdleSync()
    }
}
