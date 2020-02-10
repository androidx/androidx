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

package androidx.ui.material.studies.rally

import androidx.compose.Composable
import androidx.ui.graphics.Color
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Typography
import androidx.ui.material.lightColorPalette
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.fontFamily
import androidx.ui.text.font.FontWeight
import androidx.ui.unit.em
import androidx.ui.unit.sp

val rallyGreen = Color(0xFF1EB980)
val rallyDarkGreen = Color(0xFF045D56)
val rallyOrange = Color(0xFFFF6859)
val rallyYellow = Color(0xFFFFCF44)
val rallyPurple = Color(0xFFB15DFF)
val rallyBlue = Color(0xFF72DEFF)

@Composable
fun RallyTheme(children: @Composable() () -> Unit) {
    val colors = lightColorPalette(
        primary = rallyGreen,
        surface = Color(0xFF26282F),
        onSurface = Color.White,
        background = Color(0xFF26282F),
        onBackground = Color.White
    )
    // TODO: Bundle Roboto Condensed and Eczar font files.
    val typography = Typography(
        h1 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W100,
            fontSize = 96.sp),
        h2 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W100,
            fontSize = 60.sp),
        h3 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W500,
            fontSize = 48.sp),
        h4 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W700,
            fontSize = 34.sp),
        h5 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W700,
            fontSize = 24.sp),
        h6 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W700,
            fontSize = 20.sp),
        subtitle1 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W700,
            fontSize = 16.sp),
        subtitle2 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W500,
            fontSize = 14.sp),
        body1 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W700,
            fontSize = 16.sp),
        body2 = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W200,
            fontSize = 14.sp),
        button = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W800,
            fontSize = 14.sp),
        caption = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W500,
            fontSize = 12.sp),
        overline = TextStyle(fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W500,
            fontSize = 10.sp)

    )
    MaterialTheme(colors = colors, typography = typography, children = children)
}

@Composable
fun RallyDialogThemeOverlay(children: @Composable() () -> Unit) {
    val dialogColors = lightColorPalette(
        primary = Color.White,
        surface = Color(0xFF1E1E1E),
        onSurface = Color.White
    )
    val currentTypography = MaterialTheme.typography()
    val dialogTypography = currentTypography.copy(
        body1 = currentTypography.body1.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp
        ),
        button = currentTypography.button.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.em
        )
    )
    MaterialTheme(colors = dialogColors, typography = dialogTypography, children = children)
}
