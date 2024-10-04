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

package androidx.compose.integration.hero.jetsnack.implementation.compose.theme

import androidx.compose.integration.hero.jetsnack.implementation.R
import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Karla = FontFamily(Font(R.font.karla_regular, FontWeight.Normal))

val Typography =
    Typography(
        h1 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 96.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 117.sp,
                letterSpacing = (-1.5).sp
            ),
        h2 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 60.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 73.sp,
                letterSpacing = (-0.5).sp
            ),
        h3 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 48.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 59.sp
            ),
        h4 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 37.sp
            ),
        h5 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 29.sp
            ),
        h6 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp
            ),
        subtitle1 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            ),
        subtitle2 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp
            ),
        body1 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp,
                letterSpacing = 0.15.sp
            ),
        body2 =
            TextStyle(
                fontFamily = Karla,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp
            ),
        button =
            TextStyle(
                fontFamily = Karla,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                letterSpacing = 1.25.sp
            ),
        caption =
            TextStyle(
                fontFamily = Karla,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp
            ),
        overline =
            TextStyle(
                fontFamily = Karla,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                letterSpacing = 1.sp
            )
    )
