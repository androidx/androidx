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

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.theme

import androidx.compose.integration.macrobenchmark.target.R
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class SquadTypography(
    val regular: Regular = Regular(),
    val medium: Medium = Medium(),
    val mediumItalic: MediumItalic = MediumItalic(),
    val italic: Italic = Italic(),
    val bold: Bold = Bold(),
) {
    private companion object {
        val regularFont: FontFamily = FontFamily(Font(R.font.rubik_regular))
        val mediumFont: FontFamily = FontFamily(Font(R.font.rubik_medium))
        val mediumItalicFont: FontFamily = FontFamily(Font(R.font.rubik_medium_italic))
        val boldFont: FontFamily = FontFamily(Font(R.font.rubik_bold))
        val italicFont: FontFamily = FontFamily(Font(R.font.rubik_italic))
    }

    data class Regular(
        val s20: TextStyle =
            TextStyle(
                fontSize = 20.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s19: TextStyle =
            TextStyle(
                fontSize = 19.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s18: TextStyle =
            TextStyle(
                fontSize = 18.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s17: TextStyle =
            TextStyle(
                fontSize = 17.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s16: TextStyle =
            TextStyle(
                fontSize = 16.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s15: TextStyle =
            TextStyle(
                fontSize = 15.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s14: TextStyle =
            TextStyle(
                fontSize = 14.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s13: TextStyle =
            TextStyle(
                fontSize = 13.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s12: TextStyle =
            TextStyle(
                fontSize = 12.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s11: TextStyle =
            TextStyle(
                fontSize = 11.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s10: TextStyle =
            TextStyle(
                fontSize = 10.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s9: TextStyle =
            TextStyle(
                fontSize = 9.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
        val s8: TextStyle =
            TextStyle(
                fontSize = 8.sp,
                fontFamily = regularFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal,
            ),
    )

    data class Medium(
        val s20: TextStyle =
            TextStyle(
                fontSize = 20.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s19: TextStyle =
            TextStyle(
                fontSize = 19.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s18: TextStyle =
            TextStyle(
                fontSize = 18.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s17: TextStyle =
            TextStyle(
                fontSize = 17.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s16: TextStyle =
            TextStyle(
                fontSize = 16.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s15: TextStyle =
            TextStyle(
                fontSize = 15.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s14: TextStyle =
            TextStyle(
                fontSize = 14.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s13: TextStyle =
            TextStyle(
                fontSize = 13.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s12: TextStyle =
            TextStyle(
                fontSize = 12.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s11: TextStyle =
            TextStyle(
                fontSize = 11.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s10: TextStyle =
            TextStyle(
                fontSize = 10.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s9: TextStyle =
            TextStyle(
                fontSize = 9.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
        val s8: TextStyle =
            TextStyle(
                fontSize = 8.sp,
                fontFamily = mediumFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
            ),
    )

    data class MediumItalic(
        val s20: TextStyle =
            TextStyle(
                fontSize = 20.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s19: TextStyle =
            TextStyle(
                fontSize = 19.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s18: TextStyle =
            TextStyle(
                fontSize = 18.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s17: TextStyle =
            TextStyle(
                fontSize = 17.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s16: TextStyle =
            TextStyle(
                fontSize = 16.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s15: TextStyle =
            TextStyle(
                fontSize = 15.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s14: TextStyle =
            TextStyle(
                fontSize = 14.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s13: TextStyle =
            TextStyle(
                fontSize = 13.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s12: TextStyle =
            TextStyle(
                fontSize = 12.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s11: TextStyle =
            TextStyle(
                fontSize = 11.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s10: TextStyle =
            TextStyle(
                fontSize = 10.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s9: TextStyle =
            TextStyle(
                fontSize = 9.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
        val s8: TextStyle =
            TextStyle(
                fontSize = 8.sp,
                fontFamily = mediumItalicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
    )

    data class Italic(
        val s20: TextStyle =
            TextStyle(
                fontSize = 20.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s19: TextStyle =
            TextStyle(
                fontSize = 19.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s18: TextStyle =
            TextStyle(
                fontSize = 18.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s17: TextStyle =
            TextStyle(
                fontSize = 17.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s16: TextStyle =
            TextStyle(
                fontSize = 16.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s15: TextStyle =
            TextStyle(
                fontSize = 15.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s14: TextStyle =
            TextStyle(
                fontSize = 14.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s13: TextStyle =
            TextStyle(
                fontSize = 13.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s12: TextStyle =
            TextStyle(
                fontSize = 12.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s11: TextStyle =
            TextStyle(
                fontSize = 11.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s10: TextStyle =
            TextStyle(
                fontSize = 10.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s9: TextStyle =
            TextStyle(
                fontSize = 9.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
        val s8: TextStyle =
            TextStyle(
                fontSize = 8.sp,
                fontFamily = italicFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
    )

    data class Bold(
        val s20: TextStyle =
            TextStyle(
                fontSize = 20.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s19: TextStyle =
            TextStyle(
                fontSize = 19.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s18: TextStyle =
            TextStyle(
                fontSize = 18.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s17: TextStyle =
            TextStyle(
                fontSize = 17.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s16: TextStyle =
            TextStyle(
                fontSize = 16.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s15: TextStyle =
            TextStyle(
                fontSize = 15.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s14: TextStyle =
            TextStyle(
                fontSize = 14.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s13: TextStyle =
            TextStyle(
                fontSize = 13.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s12: TextStyle =
            TextStyle(
                fontSize = 12.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s11: TextStyle =
            TextStyle(
                fontSize = 11.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s10: TextStyle =
            TextStyle(
                fontSize = 10.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s9: TextStyle =
            TextStyle(
                fontSize = 9.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
        val s8: TextStyle =
            TextStyle(
                fontSize = 8.sp,
                fontFamily = boldFont,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Black,
            ),
    )
}

val LocalSquadTypography = staticCompositionLocalOf<SquadTypography> { SquadTypography() }
