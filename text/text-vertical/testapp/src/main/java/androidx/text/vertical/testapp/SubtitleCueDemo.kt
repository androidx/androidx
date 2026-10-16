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

import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.text.vertical.EmphasisStyle
import androidx.text.vertical.FontShearSpan
import androidx.text.vertical.compose.HorizontalText
import androidx.text.vertical.compose.VerticalText
import androidx.text.vertical.compose.buildVerticalText
import androidx.text.vertical.compose.toVerticalTextStyle

/** The test tag of the subtitle cue [VerticalText]. */
internal const val SUBTITLE_CUE_TEXT_TAG = "subtitle_cue_text"

/** The maximum height constraint applied to the subtitle cue [VerticalText]. */
internal val SUBTITLE_CUE_MAX_HEIGHT = 280.dp

/** The test tag of the subtitle cue [HorizontalText]. */
internal const val SUBTITLE_CUE_HORIZONTAL_TAG = "subtitle_cue_horizontal_text"

/** The maximum width constraint applied to the subtitle cue [HorizontalText]. */
internal val SUBTITLE_CUE_MAX_WIDTH = 280.dp

/**
 * Displays vertical and horizontal subtitle cues inside one viewport.
 *
 * The top-end box uses [Modifier.heightIn] with [SUBTITLE_CUE_MAX_HEIGHT] so that [VerticalText]
 * wraps its height to the measured vertical column height. The bottom-center box uses
 * [Modifier.widthIn] with [SUBTITLE_CUE_MAX_WIDTH] so that [HorizontalText] wraps its width to the
 * widest measured line width.
 *
 * @param styleColorsEnabled whether the sample uses high-contrast style colors
 * @param modifier the [Modifier] applied to the outer container
 */
@Composable
fun SubtitleCueDemo(styleColorsEnabled: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val cueText = remember(density) { buildSubtitleCueText(density) }
    val cueStyle =
        remember(styleColorsEnabled) {
            TextStyle(
                color = if (styleColorsEnabled) Color(0xFF1A237E) else Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                background = if (styleColorsEnabled) Color(0xFFFFF59D) else Color(0xCC000000),
                localeList = LocaleList(Locale("ja")),
            )
        }
    val verticalCueStyle = remember(cueStyle) { cueStyle.toVerticalTextStyle() }

    Box(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Box(
            modifier =
                Modifier.align(Alignment.TopEnd).border(1.dp, Color(0xFF90CAF9)).padding(8.dp)
        ) {
            VerticalText(
                text = cueText,
                modifier =
                    Modifier.heightIn(max = SUBTITLE_CUE_MAX_HEIGHT).testTag(SUBTITLE_CUE_TEXT_TAG),
                style = verticalCueStyle,
            )
        }
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter).border(1.dp, Color(0xFF90CAF9)).padding(8.dp)
        ) {
            HorizontalText(
                text = cueText,
                modifier =
                    Modifier.widthIn(max = SUBTITLE_CUE_MAX_WIDTH)
                        .testTag(SUBTITLE_CUE_HORIZONTAL_TAG),
                style = cueStyle,
            )
        }
    }
}

private fun buildSubtitleCueText(density: Density) =
    buildVerticalText(density) {
        text("第")
        combineUpright("01")
        text("話\n")
        withRuby("たてが") { text("縦書") }
        text("き")
        text(
            SpannableString("字幕").apply {
                setSpan(FontShearSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )
        text("\n")
        withEmphasis(style = EmphasisStyle.Sesame) { text("傍点") }
        text("・")
        text(
            SpannableString("傍線").apply {
                setSpan(UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )
        text("\n")
        text(
            SpannableString("取消線").apply {
                setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )
    }
