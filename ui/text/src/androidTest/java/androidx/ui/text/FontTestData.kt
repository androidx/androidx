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

package androidx.ui.text

import androidx.ui.text.font.Font
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight

class FontTestData {
    companion object {
        // This sample font provides the following features:
        // 1. The width of most of visible characters equals to font size.
        // 2. The LTR/RTL characters are rendered as ▶/◀.
        // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
        val BASIC_MEASURE_FONT = Font(
            name = "sample_font.ttf",
            weight = FontWeight.normal,
            style = FontStyle.Normal
        )

        // The kern_font provides the following features:
        // 1. Characters from A to Z are rendered as ▲ while a to z are rendered as ▼.
        // 2. When kerning is off, the width of each character is equal to font size.
        // 3. When kerning is on, it will reduce the space between two characters by 0.4 * width.
        val BASIC_KERN_FONT = Font(
            name = "kern_font.ttf",
            weight = FontWeight.normal,
            style = FontStyle.Normal
        )

        val FONT_100_REGULAR = Font(
            name = "test_100_regular.ttf",
            weight = FontWeight.w100,
            style = FontStyle.Normal
        )

        val FONT_100_ITALIC = Font(
            name = "test_100_italic.ttf",
            weight = FontWeight.w100,
            style = FontStyle.Italic
        )

        val FONT_200_REGULAR = Font(
            name = "test_200_regular.ttf",
            weight = FontWeight.w200,
            style = FontStyle.Normal
        )

        val FONT_200_ITALIC = Font(
            name = "test_200_italic.ttf",
            weight = FontWeight.w200,
            style = FontStyle.Italic
        )

        val FONT_300_REGULAR = Font(
            name = "test_300_regular.ttf",
            weight = FontWeight.w300,
            style = FontStyle.Normal
        )

        val FONT_300_ITALIC = Font(
            name = "test_300_italic.ttf",
            weight = FontWeight.w300,
            style = FontStyle.Italic
        )

        val FONT_400_REGULAR = Font(
            name = "test_400_regular.ttf",
            weight = FontWeight.w400,
            style = FontStyle.Normal
        )

        val FONT_400_ITALIC = Font(
            name = "test_400_italic.ttf",
            weight = FontWeight.w400,
            style = FontStyle.Italic
        )

        val FONT_500_REGULAR = Font(
            name = "test_500_regular.ttf",
            weight = FontWeight.w500,
            style = FontStyle.Normal
        )

        val FONT_500_ITALIC = Font(
            name = "test_500_italic.ttf",
            weight = FontWeight.w500,
            style = FontStyle.Italic
        )

        val FONT_600_REGULAR = Font(
            name = "test_600_regular.ttf",
            weight = FontWeight.w600,
            style = FontStyle.Normal
        )

        val FONT_600_ITALIC = Font(
            name = "test_600_italic.ttf",
            weight = FontWeight.w600,
            style = FontStyle.Italic
        )

        val FONT_700_REGULAR = Font(
            name = "test_700_regular.ttf",
            weight = FontWeight.w700,
            style = FontStyle.Normal
        )

        val FONT_700_ITALIC = Font(
            name = "test_700_italic.ttf",
            weight = FontWeight.w700,
            style = FontStyle.Italic
        )

        val FONT_800_REGULAR = Font(
            name = "test_800_regular.ttf",
            weight = FontWeight.w800,
            style = FontStyle.Normal
        )

        val FONT_800_ITALIC = Font(
            name = "test_800_italic.ttf",
            weight = FontWeight.w800,
            style = FontStyle.Italic
        )

        val FONT_900_REGULAR = Font(
            name = "test_900_regular.ttf",
            weight = FontWeight.w900,
            style = FontStyle.Normal
        )

        val FONT_900_ITALIC = Font(
            name = "test_900_italic.ttf",
            weight = FontWeight.w900,
            style = FontStyle.Italic
        )
    }
}
