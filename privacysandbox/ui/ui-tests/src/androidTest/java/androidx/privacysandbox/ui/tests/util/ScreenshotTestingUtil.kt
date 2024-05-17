/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.tests.util

import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.view.Window
import androidx.annotation.RequiresApi
import org.junit.Assert.assertNotNull

/** Utility class for operations related to screenshot testing. */
class ScreenshotTestingUtil {

    companion object {

        // This logic is similar to the one used in BitmapPixelChecker class under cts tests
        // If session is created from another process we should make changes to the test to
        // make this logic work.
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun verifyColorOfScreenshot(
            instrumentationRegistry: Instrumentation,
            window: Window,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            requiredColor: Int
        ): Boolean {
            val screenshot = instrumentationRegistry.uiAutomation.takeScreenshot(window)
            assertNotNull("Failed to generate a screenshot", screenshot)

            val swBitmap = screenshot!!.copy(Bitmap.Config.ARGB_8888, false)
            screenshot.recycle()
            val bounds = Rect(left, top, right, bottom)
            val rectangleArea = (bottom - top) * (right - left)
            val numMatchingPixels = getNumMatchingPixels(swBitmap, bounds, requiredColor)

            swBitmap.recycle()

            return numMatchingPixels == rectangleArea
        }

        // This logic is from the method named AreSame in AlmostPerfectMatcher class under
        // platform_testing. Depending on the hardware used, the colors in pixel look similar
        // to assigned color to the naked eye but their value can be slightly different from
        // the assigned value. This method takes care to verify whether both the assigned and
        // the actual value of the color are almost same.
        // ref
        // R. F. Witzel, R. W. Burnham, and J. W. Onley. Threshold and suprathreshold perceptual
        // color differences. J. Optical Society of America, 63:615{625, 1973. 14
        // TODO(b/339201299): Replace with original implementation
        private fun areAlmostSameColors(referenceColor: Int, testColor: Int): Boolean {
            val green = Color.green(referenceColor) - Color.green(testColor)
            val blue = Color.blue(referenceColor) - Color.blue(testColor)
            val red = Color.red(referenceColor) - Color.red(testColor)
            val redMean = (Color.red(referenceColor) + Color.red(testColor)) / 2
            val redScalar = if (redMean < 128) 2 else 3
            val blueScalar = if (redMean < 128) 3 else 2
            val greenScalar = 4
            val correction =
                (redScalar * red * red) + (greenScalar * green * green) + (blueScalar * blue * blue)
            val thresholdSq = 3 * 3
            // 1.5 no difference
            // 3.0 observable by experienced human observer
            // 6.0 minimal difference
            // 12.0 perceivable difference
            return correction <= thresholdSq
        }

        private fun getNumMatchingPixels(bitmap: Bitmap, bounds: Rect, requiredColor: Int): Int {
            var numMatchingPixels = 0

            for (x in bounds.left until bounds.right) {
                for (y in bounds.top until bounds.bottom) {
                    val color = bitmap.getPixel(x, y)
                    if (areAlmostSameColors(color, requiredColor)) {
                        numMatchingPixels++
                    }
                }
            }

            return numMatchingPixels
        }
    }
}
