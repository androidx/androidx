/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.SkiaFontLoader
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.text.platform.aliases
import com.google.common.truth.Truth
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesktopFontTest {
    @get:Rule
    val rule = createComposeRule()

    private val fontLoader = SkiaFontLoader()

    private val fontListFontFamily by lazy {
        FontFamily(
            Font(
                "font/sample_font.ttf"
            ),
            Font(
                "font/test_400_italic.ttf",
                style = FontStyle.Italic
            )
        )
    }

    private val loadedTypeface by lazy {
        val bytes = Thread
            .currentThread()
            .contextClassLoader
            .getResourceAsStream("font/sample_font.ttf")!!
            .readAllBytes()
        FontMgr.default.makeFromData(Data.makeFromBytes(bytes))
            ?: error("loadedTypeface failed: FontMgr.default.makeFromData returned null")
    }

    private val loadedFontFamily by lazy {
        FontFamily(Typeface(loadedTypeface))
    }

    @Test
    fun ensureRegistered() {
        Truth.assertThat(fontLoader.loadPlatformTypes(FontFamily.Cursive).aliases)
            .isEqualTo(FontFamily.Cursive.aliases)

        Truth.assertThat(fontLoader.loadPlatformTypes(FontFamily.Default).aliases)
            .isEqualTo(FontFamily.SansSerif.aliases)

        Truth.assertThat(fontLoader.loadPlatformTypes(loadedFontFamily).aliases)
            .isEqualTo(listOf("Sample Font"))
    }
}