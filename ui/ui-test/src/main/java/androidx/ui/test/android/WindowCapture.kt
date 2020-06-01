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

package androidx.ui.test.android

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.ui.geometry.Rect
import kotlin.math.roundToInt

internal fun captureRegionToBitmap(
    captureRect: Rect
): Bitmap {
    // TODO: This could go to some Android specific extensions.
    val srcRect = android.graphics.Rect(
        captureRect.left.roundToInt(),
        captureRect.top.roundToInt(),
        captureRect.right.roundToInt(),
        captureRect.bottom.roundToInt()
    )

    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.waitForIdle()

    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val srcBitmap = automation.takeScreenshot()

    return Bitmap.createBitmap(srcBitmap, captureRect.left.roundToInt(),
        captureRect.top.roundToInt(), srcRect.width(), srcRect.height())
}