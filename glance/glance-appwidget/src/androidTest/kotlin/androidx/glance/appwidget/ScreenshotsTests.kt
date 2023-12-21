/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.annotation.FloatRange
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher

fun screenshotRule() = AndroidXScreenshotTestRule("glance/glance-appwidget")

fun AndroidXScreenshotTestRule.checkScreenshot(
    rootView: View,
    expectedGolden: String,
    @FloatRange(from = 0.0, to = 1.0) threshold: Double = 0.98
) {
    val bmp = Bitmap.createBitmap(
        rootView.measuredWidth,
        rootView.measuredHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    rootView.draw(canvas)

    assertBitmapAgainstGolden(bmp, expectedGolden, MSSIMMatcher(threshold))
}