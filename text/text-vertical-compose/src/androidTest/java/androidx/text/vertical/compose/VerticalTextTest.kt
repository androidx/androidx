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

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.text.vertical.TextOrientation
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
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
}
