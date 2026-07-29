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
import org.junit.Test

/**
 * Abstract parameterized base test class for smoke testing XR Activities. Other libraries can
 * subclass this to automatically inherit these smoke tests.
 */
abstract class TestAppSmokeTest(activityClass: Class<out Activity>) : TestAppTest(activityClass) {

    @Test
    @XrDeviceTest
    fun activity_loadsAndShowsUi() {
        val screenshotBefore = takeScreenshotWithTimeout()
        val activity = startActivity()

        assertScreenshotChanged(screenshotBefore)

        // Finish the Activity to close it
        instrumentation.runOnMainSync { activity.finish() }

        // Wait for the main thread to be idle again
        instrumentation.waitForIdleSync()
    }
}
