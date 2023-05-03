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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.PI
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathTest {

    @Test
    fun testAddArcPath() {
        val width = 100
        val height = 100
        val image = ImageBitmap(width, height)
        val canvas = Canvas(image)
        val path1 = Path().apply {
            addArcRad(
                Rect(Offset.Zero, Size(width.toFloat(), height.toFloat())),
                0.0f,
                PI.toFloat() / 2
            )
        }

        val arcColor = Color.Cyan
        val arcPaint = Paint().apply { color = arcColor }
        canvas.drawPath(path1, arcPaint)

        val path2 = Path().apply {
            arcToRad(
                Rect(Offset(0.0f, 0.0f), Size(width.toFloat(), height.toFloat())),
                PI.toFloat(),
                PI.toFloat() / 2,
                false
            )
            close()
        }

        canvas.drawPath(path2, arcPaint)

        val pixelmap = image.toPixelMap()
        val x = (50.0 * Math.cos(PI / 4)).toInt()
        assertEquals(
            arcColor,
            pixelmap[
                width / 2 + x - 1,
                height / 2 + x - 1
            ]
        )

        assertEquals(
            arcColor,
            pixelmap[
                width / 2 - x,
                height / 2 - x
            ]
        )
    }

    @Test
    fun testRewindPath() {
        val androidPath = TestAndroidPath()
        val path = androidPath.asComposePath().apply {
            addRect(Rect(0f, 0f, 100f, 200f))
        }
        assertFalse(path.isEmpty)

        path.rewind()

        assertTrue(path.isEmpty)
        // Reset should not be invoked as the rewind method is implemented to call into the
        // corresponding rewind call in the framework and not call the default fallback
        assertEquals(0, androidPath.resetCount)
    }

    @Test
    fun testPathTransform() {
        val width = 100
        val height = 100
        val image = ImageBitmap(width, height)
        val canvas = Canvas(image)

        val path = Path().apply {
            addRect(Rect(0f, 0f, 50f, 50f))
            transform(
                Matrix().apply { translate(50f, 50f) }
            )
        }

        val paint = Paint().apply { color = Color.Black }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.Red
        canvas.drawPath(path, paint)

        image.toPixelMap().apply {
            assertEquals(Color.Black, this[width / 2 - 3, height / 2 - 3])
            assertEquals(Color.Black, this[width / 2, height / 2 - 3])
            assertEquals(Color.Black, this[width / 2 - 3, height / 2])

            assertEquals(Color.Red, this[width / 2 + 2, height / 2 + 2])
            assertEquals(Color.Red, this[width - 2, height / 2 + 2])
            assertEquals(Color.Red, this[width - 2, height - 2])
            assertEquals(Color.Red, this[width / 2 + 2, height - 2])
        }
    }

    class TestAndroidPath : android.graphics.Path() {

        var resetCount = 0

        override fun reset() {
            resetCount++
            super.reset()
        }
    }
}