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

package androidx.graphics.surface

import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert

@SdkSuppress(minSdkVersion = 29)
internal class SurfaceControlUtils {
    companion object {
        fun validateOutput(block: (bitmap: Bitmap) -> Boolean) {
            var sleepDurationMillis = 1000L
            var success = false
            for (i in 0..3) {
                val bitmap = getScreenshot(InstrumentationRegistry.getInstrumentation())
                success = block(bitmap)
                if (!success) {
                    SystemClock.sleep(sleepDurationMillis)
                    sleepDurationMillis *= 2
                } else {
                    break
                }
            }
            Assert.assertTrue(success)
        }

        fun getScreenshot(instrumentation: Instrumentation): Bitmap {
            val uiAutomation = instrumentation.uiAutomation
            val screenshot = uiAutomation.takeScreenshot()
            return screenshot
        }
    }
}