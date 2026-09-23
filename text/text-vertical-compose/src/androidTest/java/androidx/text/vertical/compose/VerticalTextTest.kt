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

package androidx.text.vertical.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.SpannableString
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.VerticalTextLayout
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.ceil
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class VerticalTextTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun exposesSemantics_correctly() = runComposeUiTest {
        val text = SpannableString("Hello Vertical")
        val style = VerticalTextStyle(fontSize = 30.sp)

        setContent { VerticalText(text = text, style = style) }

        // Modern Compose testing relies on finding nodes by their semantic text
        onNodeWithText("Hello Vertical").assertExists().assertIsDisplayed()
    }
}

@MediumTest
@RunWith(AndroidJUnit4::class)
class VerticalTextLayoutCacheTest {

    @Test
    fun cache_returnsSameInstance_whenInputsAreSame() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, density)
        val layout2 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, density)

        assertSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenDensityChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT

        val layout1 =
            cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, Density(1f))
        val layout2 =
            cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, Density(2f))

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenStyleChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style1 = VerticalTextStyle(fontSize = 12.sp)
        val style2 = VerticalTextStyle(fontSize = 14.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style1, typeface, density)
        val layout2 = cache.getLayout(text, 100, TextOrientation.Mixed, style2, typeface, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenTextChanges() {
        val cache = VerticalTextLayoutCache()
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout("Hello", 100, TextOrientation.Mixed, style, typeface, density)
        val layout2 = cache.getLayout("World", 100, TextOrientation.Mixed, style, typeface, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenHeightChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, density)
        val layout2 = cache.getLayout(text, 200, TextOrientation.Mixed, style, typeface, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenOrientationChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, density)
        val layout2 = cache.getLayout(text, 100, TextOrientation.Upright, style, typeface, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenTypefaceChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface1 = Typeface.DEFAULT
        val typeface2 = Typeface.SERIF
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface1, density)
        val layout2 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface2, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_clearsBackground_whenStyleBackgroundBecomesUnspecified() {
        val cache = VerticalTextLayoutCache()
        val text = SpannableString("Hello")
        val withBg = VerticalTextStyle(fontSize = 24.sp, background = Color.Yellow)
        val withoutBg = VerticalTextStyle(fontSize = 24.sp, background = Color.Unspecified)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)
        val layoutHeight = 200

        // Assert on layout1 before the second getLayout call, because VerticalTextLayoutCache
        // reuses a single cachedPaint instance that layout1 references directly.
        val layout1 =
            cache.getLayout(text, layoutHeight, TextOrientation.Mixed, withBg, typeface, density)
        assertWithMessage("layout1 must draw the yellow background")
            .that(layout1.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isGreaterThan(0)

        val layout2 =
            cache.getLayout(text, layoutHeight, TextOrientation.Mixed, withoutBg, typeface, density)
        assertWithMessage("layout2 must not draw a yellow background")
            .that(layout2.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isEqualTo(0)
        assertWithMessage("layout2 must draw black text")
            .that(layout2.countColorPixels(Color.Black.toArgb(), layoutHeight))
            .isGreaterThan(0)
    }
}

/** Draws this layout into a new bitmap and returns the number of pixels that are [targetColor]. */
private fun VerticalTextLayout.countColorPixels(targetColor: Int, height: Int): Int {
    val bitmapWidth = ceil(width).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
    try {
        draw(Canvas(bitmap), bitmapWidth.toFloat(), 0f)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { it == targetColor }
    } finally {
        bitmap.recycle()
    }
}
