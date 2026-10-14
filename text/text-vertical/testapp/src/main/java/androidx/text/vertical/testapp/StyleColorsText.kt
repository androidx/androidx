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

import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.text.vertical.RubySpan
import androidx.text.vertical.compose.VerticalText
import androidx.text.vertical.compose.VerticalTextStyle
import androidx.text.vertical.compose.buildVerticalText

@Composable
fun StyleColorsText(style: VerticalTextStyle, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val text =
        remember(density) {
            buildVerticalText(density) {
                withStyle(textColor = Color(0xFFB71C1C), backgroundColor = Color(0xFFC8E6C9)) {
                    withRuby(
                        SpannableString("わがはい").apply {
                            setSpan(
                                ForegroundColorSpan(android.graphics.Color.BLUE),
                                0,
                                2,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or
                                    (1 shl Spanned.SPAN_PRIORITY_SHIFT),
                            )
                        }
                    ) {
                        text("吾輩")
                    }
                }
                text("は猫である。")
                withRuby("なまえ") { text("名前") }
                text("はまだ無い。")
            }
        }
    val strokeText = remember { makeStrokeText() }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        VerticalText(text, style = style, overflow = TextOverflow.Visible)
        // Keep the plain string after strokeText. This shows that StrokeSpan does not change the
        // background of the next text.
        VerticalText(strokeText, style = style, overflow = TextOverflow.Visible)
        VerticalText("プレーン文字列", style = style, overflow = TextOverflow.Visible)
    }
}

/**
 * Makes a text with platform spans. [StrokeSpan] covers all of the text. A [BackgroundColorSpan]
 * and a [RubySpan] cover the first word.
 *
 * The ruby text is shorter than its base text. This leaves space above and below the ruby text,
 * which gets the background color of the base text.
 */
private fun makeStrokeText(): CharSequence =
    SpannableString("名前はまだ無い。").apply {
        setSpan(StrokeSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(
            BackgroundColorSpan(Color(0xFFBBDEFB).toArgb()),
            0,
            2,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        setSpan(RubySpan("なまえ"), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

/** Draws the text as an outline. */
private class StrokeSpan : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint) {
        tp.style = Paint.Style.STROKE
        tp.strokeWidth = tp.textSize / 24
    }
}
