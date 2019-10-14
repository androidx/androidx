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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.sp
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.material.MaterialColors
import androidx.ui.material.MaterialTheme
import androidx.ui.material.MaterialTypography
import androidx.ui.material.themeColor
import androidx.ui.material.themeTextStyle
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontWeight

@Sampled
@Composable
fun MaterialThemeSample() {
    val colors = MaterialColors(
        primary = Color(0xFF1EB980.toInt()),
        surface = Color(0xFF26282F.toInt()),
        onSurface = Color.White
    )
    val typography = MaterialTypography(
        h1 = TextStyle(fontFamily = FontFamily("RobotoCondensed"),
            fontWeight = FontWeight.W100,
            fontSize = 96.sp),
        h2 = TextStyle(fontFamily = FontFamily("RobotoCondensed"),
            fontWeight = FontWeight.W100,
            fontSize = 60.sp)

    )
    MaterialTheme(colors = colors, typography = typography) {
        // Your app content goes here
    }
}

@Sampled
@Composable
fun ThemeColorSample() {
    ColoredRect(color = +themeColor { primary })
}

@Sampled
@Composable
fun ThemeTextStyleSample() {
    Text(text = "Styled text", style = +themeTextStyle { h1 })
}