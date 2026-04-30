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

package androidx.compose.ui.text.googlefonts.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

@Sampled
@Composable
fun GoogleFontSample() {
    val fontFamily =
        FontFamily(
            Font(
                googleFont = GoogleFont("Lobster Two"),
                weight = FontWeight.W600,
                style = FontStyle.Italic,
            )
        )

    Text("Hello World", style = TextStyle(fontFamily = fontFamily))
}

@Sampled
@Composable
fun GoogleFontWithVariationSettingsSample() {
    val fontVariationSettings =
        FontVariation.Settings(
            FontVariation.grade(0),
            FontVariation.weight(900),
            FontVariation.slant(0f),
            FontVariation.width(100f),
        )

    val fontFamily =
        FontFamily(
            Font(
                googleFont = GoogleFont("Google Sans Flex"),
                variationSettings = fontVariationSettings,
            )
        )

    Text("Hello World", style = TextStyle(fontFamily = fontFamily))
}
