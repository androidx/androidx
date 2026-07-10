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

package androidx.compose.ui.graphics

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidPathMeasureTest {

    // Documenting accurate workaround for API35+ b/429020208
    // lock in the specific required path behavior with this test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun potentialWorkaround_writeParts() {
        // 1. Create a simple horizontal line path
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(20f, 0f)

        // 2. Create the PathMeasure and set the path
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, false)

        val length = pathMeasure.length
        assertEquals(20f, length)

        // 4. Create the destination path and apply the segments by writing to target path
        val tmpPath = Path()
        val targetPath = Path()
        pathMeasure.getSegment(10f, length, tmpPath, true)
        targetPath.addPath(tmpPath)
        tmpPath.reset() // do this to work correctly on all API levels
        pathMeasure.getSegment(0f, 5f, tmpPath, true)
        targetPath.addPath(tmpPath)

        assertEquals(
            """
            #####.....##########
            #####.....##########
            """
                .trimIndent() + "\n",
            targetPath.toAsciiArt(20, 2),
        )
    }
}

/**
 * Converts a [Path] to an ASCII art representation. This is a very inefficient way to debug but can
 * be useful for visual verification.
 */
private fun Path.toAsciiArt(width: Int, height: Int): String {
    val bitmap =
        android.graphics.Bitmap.createBitmap(
            width,
            height,
            android.graphics.Bitmap.Config.ARGB_8888,
        )
    val canvas = android.graphics.Canvas(bitmap)
    val paint =
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f * height // fill height
        }
    canvas.drawPath(this.asAndroidPath(), paint)

    val out = StringBuilder()
    for (y in 0 until height) {
        val row = StringBuilder()
        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, y)
            // Check if the pixel is not transparent (i.e., the path drew something here)
            if (android.graphics.Color.alpha(pixel) > 0) {
                row.append("#") // Represents a drawn pixel
            } else {
                row.append(".") // Represents an empty pixel
            }
        }
        out.append("$row\n")
    }
    bitmap.recycle()
    return out.toString()
}
