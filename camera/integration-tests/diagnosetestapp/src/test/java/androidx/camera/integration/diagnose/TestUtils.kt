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

package androidx.camera.integration.diagnose

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import java.io.BufferedReader

const val JPEG_ENCODE_ERROR_TOLERANCE = 3

/**
 * Asserting bitmap color and dimension is as expected
 */
fun assertBitmapColorAndSize(bitmap: Bitmap, color: Int, width: Int, height: Int) {
    for (x in 0 until bitmap.width) {
        for (y in 0 until bitmap.height) {
            val pixelColor = bitmap.getPixel(x, y)
            // compare the RGB of the pixel to the given color
            for (shift in 16 until 0 step 8) {
                val pixelRgb = (pixelColor shr shift) and 0xFF
                val rgb = (color shr shift) and 0xFF
                val rgbDifference = kotlin.math.abs(pixelRgb.minus(rgb))
                assertThat(rgbDifference).isLessThan(JPEG_ENCODE_ERROR_TOLERANCE)
            }
        }
    }
    assertThat(bitmap.width).isEqualTo(width)
    assertThat(bitmap.height).isEqualTo(height)
}

/**
 * Reading and returning complete String from buffer
 */
fun readText(br: BufferedReader): String {
    var lines = StringBuilder()
    while (br.ready()) {
        lines.append(br.readLine())
    }
    return lines.toString()
}
