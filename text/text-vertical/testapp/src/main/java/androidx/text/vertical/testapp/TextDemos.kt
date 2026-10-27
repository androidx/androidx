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

package androidx.text.vertical.testapp

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.text.vertical.compose.toVerticalTextStyle
import java.io.File

/** Shows a long vertical text that the user can zoom and pan. */
@Composable
fun LongVerticalTextDemo() {
    StyleColorsToggle { enabled ->
        ZoomableVerticalText(styleColorsEnabled = enabled) { LongText(it.toVerticalTextStyle()) }
    }
}

/** Shows a vertical text with ruby, emphasis, and other spans. */
@Composable
fun MultiStyleVerticalTextDemo() {
    StyleColorsToggle { enabled ->
        ZoomableVerticalText(styleColorsEnabled = enabled) { ComplexText(it.toVerticalTextStyle()) }
    }
}

/** Shows a vertical text that draws each span style in its own color. */
@Composable
fun StyleColorsDemo() {
    StyleColorsToggle(initiallyEnabled = true) { enabled ->
        ZoomableVerticalText(styleColorsEnabled = enabled) {
            StyleColorsText(it.toVerticalTextStyle())
        }
    }
}

/** Shows a long horizontal text that the user can zoom and pan. */
@Composable
fun LongHorizontalTextDemo() {
    StyleColorsToggle { enabled ->
        ZoomableVerticalText(isVertical = false, styleColorsEnabled = enabled) {
            LongHorizontalText(it)
        }
    }
}

/** Shows a horizontal text with ruby, emphasis, and other spans. */
@Composable
fun MultiStyleHorizontalTextDemo() {
    StyleColorsToggle { enabled ->
        ZoomableVerticalText(isVertical = false, styleColorsEnabled = enabled) {
            ComplexHorizontalText(it)
        }
    }
}

/** Shows [MultiStyleHorizontalTextDemo] with the system CJK font as the primary typeface. */
@Composable
fun CjkFontHorizontalTextDemo() {
    val cjkFontFamily = rememberCjkFontFamily()
    StyleColorsToggle { enabled ->
        ZoomableVerticalText(
            isVertical = false,
            styleColorsEnabled = enabled,
            fontFamily = cjkFontFamily,
        ) {
            ComplexHorizontalText(it)
        }
    }
}

/**
 * Loads the system CJK font as a [FontFamily].
 *
 * Its top and bottom bounds are much larger than its ascent and descent, unlike the Latin default
 * font when it falls back to CJK per glyph. Falls back to [FontFamily.SansSerif] when the file is
 * absent.
 */
@Composable
private fun rememberCjkFontFamily(): FontFamily = remember {
    File("/system/fonts/NotoSansCJK-Regular.ttc")
        .takeIf { it.exists() }
        ?.let {
            FontFamily(Typeface.createFromFile(it))
        } ?: FontFamily.SansSerif
}
