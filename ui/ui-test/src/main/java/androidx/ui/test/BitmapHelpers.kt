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

package androidx.ui.test

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.ui.core.IntPxPosition
import androidx.ui.core.IntPxSize
import androidx.ui.core.ipx
import androidx.ui.graphics.Color

/**
 * Captures the underlying component's surface into bitmap.
 *
 * This has currently several limitations. Currently we assume that the component is hosted in
 * Activity's window. Also if there is another window covering part of the component if won't occur
 * in the bitmap as this is taken from the component's window surface.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun SemanticsNodeInteraction.captureToBitmap(): Bitmap {
    return semanticsTreeInteraction.captureNodeToBitmap(semanticsTreeNode)
}

/**
 * A helper function to run asserts on [Bitmap].
 *
 * @param expectedSize The expected size of the bitmap. Leave null to skip the check.
 * @param expectedColorProvider Returns the expected color for the provided pixel position.
 * The returned color is then asserted as the expected one on the given bitmap.
 *
 * @throws AssertionError if size or colors don't match.
 */
fun Bitmap.assertPixels(
    expectedSize: IntPxSize? = null,
    expectedColorProvider: (pos: IntPxPosition) -> Color?
) {
    if (expectedSize != null) {
        if (width != expectedSize.width.value || height != expectedSize.height.value) {
            throw AssertionError("Bitmap size is wrong! Expected '$expectedSize' but got " +
                    "'$width x $height'")
        }
    }

    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pxPos = IntPxPosition(x.ipx, y.ipx)
            val color = Color(pixels[width * y + x])
            val expectedClr = expectedColorProvider(pxPos)
            if (expectedClr != null && expectedClr != color) {
                throw AssertionError("Comparison failed for $pxPos: expected $expectedClr $ " +
                        "but received $color")
            }
        }
    }
}