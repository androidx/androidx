/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.border
import androidx.compose.foundation.demos.collection.buildColorList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

private const val alpha = 0.4f

private val Red = Color(0xffE13C56)
private val Orange = Color(0xffE16D3C)
private val Yellow = Color(0xffE0AE04)
private val Green = Color(0xff78AA04)
private val Blue = Color(0xff4A7DCF)
private val Indigo = Color(0xff3F0FB7)
private val Purple = Color(0xff7B4397)

// red is used for the selection container color
private val Rainbow =
    buildColorList(initialCapacity = 6) {
        add(Orange)
        add(Yellow)
        add(Green)
        add(Blue)
        add(Indigo)
        add(Purple)
    }

@Composable
fun MinTouchTargetTextSelection() {
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 8.dp)) {
        Text(
            buildAnnotatedString {
                append("The ")
                appendWithColor(Red, "solid red")
                append(" rectangle borders the ")
                appendCode("SelectionContainer")
                append(". The inner ")
                appendRainbowText("solid rainbow")
                append(" border is the bounds of each individual ")
                appendCode("Text")
                append(" with the matching text color. The outer ")
                appendRainbowText("faded rainbow", alpha)
                append(" and ")
                appendWithColor(Red.copy(alpha), "faded red")
                append(" borders are the minimum touch target space for the associated ")
                appendCode("Text")
                append(" or ")
                appendCode("SelectionContainer")
                append(
                    """
                    |. We expect that touch selection gestures in the touch target space,
                    | but not directly on the
                    | """
                        .trimMargin()
                        .replace("\n", "")
                )
                appendCode("Text")
                append(
                    """
                    |, will still start a selection and not crash. The below slider adjusts
                    | the minimum touch target size between 0 and 100 dp.
                    |"""
                        .trimMargin()
                        .replace("\n", "")
                )
            },
        )
        var minTouchSideLength by remember { mutableFloatStateOf(48f) }
        Slider(
            value = minTouchSideLength,
            onValueChange = { minTouchSideLength = it },
            valueRange = 0f..100f
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val length = minTouchSideLength.dp
            OverrideMinimumTouchTarget(DpSize(length, length)) { MinTouchTargetInTextSelection() }
        }
    }
}

@Composable
private fun OverrideMinimumTouchTarget(size: DpSize, content: @Composable () -> Unit) {
    val viewConfiguration = LocalViewConfiguration.current
    val viewConfigurationOverride = DelegatedViewConfiguration(viewConfiguration, size)
    CompositionLocalProvider(LocalViewConfiguration provides viewConfigurationOverride, content)
}

@Composable
private fun MinTouchTargetInTextSelection() {
    val minimumTouchTarget = LocalViewConfiguration.current.minimumTouchTargetSize
    SelectionContainer(
        Modifier.border(1.dp, Red)
            .padding(1.dp)
            .drawMinTouchTargetBorderBehind(Red.copy(alpha), minimumTouchTarget)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Rainbow.forEachIndexed { index, color ->
                val fadedColor = color.copy(alpha)
                Text(
                    text = "Text",
                    style = LocalTextStyle.current.merge(color = color),
                    modifier =
                        Modifier
                            // offset the texts horizontally, else the borders will heavily overlap
                            .padding(start = (index * 6).dp)
                            .border(1.dp, color)
                            // Padding between text and border so they aren't touching
                            .padding(1.dp)
                            .drawMinTouchTargetBorderBehind(fadedColor, minimumTouchTarget)
                )
            }
        }
    }
}

/** Draw a 1 dp unfilled rect around the minimum touch target. */
private fun Modifier.drawMinTouchTargetBorderBehind(
    color: Color,
    minimumTouchTarget: DpSize
): Modifier = drawBehind {
    val minTouchTargetCoercedSize =
        Size(
            width = size.width.coerceAtLeast(minimumTouchTarget.width.toPx()),
            height = size.height.coerceAtLeast(minimumTouchTarget.height.toPx())
        )
    val topLeft =
        Offset(
            x = (size.width - minTouchTargetCoercedSize.width) / 2,
            y = (size.height - minTouchTargetCoercedSize.height) / 2
        )
    drawRect(color, topLeft, minTouchTargetCoercedSize, style = Stroke(1.dp.toPx()))
}

private fun AnnotatedString.Builder.appendRainbowText(text: String, alpha: Float = 1f) {
    val size = Rainbow.size
    text.forEachIndexed { index, char ->
        val color = Rainbow[index % size].copy(alpha)
        withStyle(SpanStyle(color = color)) { append(char) }
    }
}

private class DelegatedViewConfiguration(
    delegate: ViewConfiguration,
    minimumTouchTargetSizeOverride: DpSize,
) : ViewConfiguration by delegate {
    override val minimumTouchTargetSize: DpSize = minimumTouchTargetSizeOverride
}
