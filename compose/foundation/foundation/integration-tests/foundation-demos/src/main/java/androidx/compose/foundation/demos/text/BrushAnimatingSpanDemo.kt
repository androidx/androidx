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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BrushAnimatingSpanDemo() {
    val animatable = remember { Animatable(0f) }
    val animatable2 = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    var preWordCount by remember { mutableIntStateOf(5) }
    var highlightWordCount by remember { mutableIntStateOf(15) }
    var postWordCount by remember { mutableIntStateOf(5) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {

            Text("Pre Word Count")
            Slider(
                value = preWordCount.toFloat(),
                onValueChange = { preWordCount = it.roundToInt() },
                valueRange = 0f..16f,
                steps = 16
            )

            Text("Highlight Word Count")
            Slider(
                value = highlightWordCount.toFloat(),
                onValueChange = { highlightWordCount = it.roundToInt() },
                valueRange = 0f..20f,
                steps = 16
            )

            Text("Post Word Count")
            Slider(
                value = postWordCount.toFloat(),
                onValueChange = { postWordCount = it.roundToInt() },
                valueRange = 0f..16f,
                steps = 16
            )

            BrushAnimatingSpanText(
                preWordCount = preWordCount,
                highlightWordCount = highlightWordCount,
                postWordCount = postWordCount,
                animatable = animatable,
                animatable2 = animatable2
            )
            Button(onClick = {
                coroutineScope.launch {
                    animatable.snapTo(0f)
                    animatable2.snapTo(0f)
                    launch {
                        animatable2.animateTo(
                            1f,
                            tween(highlightWordCount * 100, easing = LinearEasing)
                        )
                    }
                    delay(300)
                    launch {
                        animatable.animateTo(
                            1f,
                            tween(highlightWordCount * 100, easing = LinearEasing)
                        )
                    }
                }
            }) {
                Text("Go")
            }
            Button(onClick = {
                coroutineScope.launch {
                    animatable.snapTo(0f)
                    animatable2.snapTo(0f)
                }
            }) {
                Text("Reset")
            }
        }
    }
}

@Composable
fun BrushAnimatingSpanText(
    preWordCount: Int,
    highlightWordCount: Int,
    postWordCount: Int,
    animatable: Animatable<Float, AnimationVector1D>,
    animatable2: Animatable<Float, AnimationVector1D>
) {
    val (text, start, end) = remember(preWordCount, highlightWordCount, postWordCount) {
        var start = 0
        var end = 0
        val text = buildAnnotatedString {
            append(loremIpsum(wordCount = preWordCount))
            start = length
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(loremIpsum(wordCount = highlightWordCount))
            }
            end = length
            append(loremIpsum(wordCount = postWordCount))
        }
        Triple(text, start, end)
    }

    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val finalText = remember(text, start, end, textLayoutResult.value?.layoutInput?.text?.text) {
        calculateAnnotatedString(
            text,
            start,
            end,
            textLayoutResult.value,
            animatable,
            animatable2
        )
    }

    Text(
        text = finalText,
        fontSize = 20.sp,
        onTextLayout = {
            textLayoutResult.value = it
        })
}

fun calculateAnnotatedString(
    text: AnnotatedString,
    start: Int,
    end: Int,
    textLayoutResult: TextLayoutResult?,
    animatable: Animatable<Float, AnimationVector1D>,
    animatable2: Animatable<Float, AnimationVector1D>
): AnnotatedString {
    textLayoutResult ?: return text
    val startLine = textLayoutResult.getLineForOffset(start)
    val endLine = textLayoutResult.getLineForOffset(end)
    val lines = mutableListOf<Segment>()
    val firstLineLeft = textLayoutResult.getBoundingBox(start).left
    val lastLineRight = textLayoutResult.getBoundingBox(end).right

    if (startLine == endLine) {
        lines += Segment(
            leftPosition = firstLineLeft,
            rightPosition = lastLineRight,
            leftOffset = start,
            rightOffset = end
        )
    } else {
        for (i in (startLine..endLine)) {
            lines += when (i) {
                startLine -> {
                    Segment(
                        leftPosition = firstLineLeft,
                        rightPosition = textLayoutResult.getLineRight(i),
                        leftOffset = start,
                        rightOffset = textLayoutResult.getLineEnd(i)
                    )
                }

                endLine -> {
                    Segment(
                        leftPosition = textLayoutResult.getLineLeft(i),
                        rightPosition = lastLineRight,
                        leftOffset = textLayoutResult.getLineStart(i),
                        rightOffset = end
                    )
                }

                else -> {
                    Segment(
                        leftPosition = textLayoutResult.getLineLeft(i),
                        rightPosition = textLayoutResult.getLineRight(i),
                        leftOffset = textLayoutResult.getLineStart(i),
                        rightOffset = textLayoutResult.getLineEnd(i)
                    )
                }
            }
        }
    }

    val brushSpans = (lines).mapIndexed { index, segment ->
        AnnotatedString.Range(
            item = SpanStyle(
                brush = object : ShaderBrush() {
                    override fun createShader(size: Size): Shader {
                        val animationValue = animatable.value
                        val animationValue2 = animatable2.value
                        return LinearGradientShader(
                            from = Offset(
                                x = segment.leftPosition - lines.allLeftWidth(index),
                                y = 0f
                            ),
                            to = Offset(
                                x = segment.rightPosition + lines.allRightWidth(index),
                                y = 0f
                            ),
                            colors = listOf(
                                Color.Blue,
                                Color.Blue,
                                Color.Gray,
                                Color.Black,
                                Color.Black
                            ),
                            colorStops = listOf(
                                0f,
                                animationValue,
                                (animationValue + animationValue2) / 2,
                                animationValue2,
                                1f
                            )
                        )
                    }
                }
            ),
            start = segment.leftOffset,
            end = segment.rightOffset
        )
    }
    return AnnotatedString(
        text.text,
        paragraphStyles = text.paragraphStyles,
        spanStyles = text.spanStyles + brushSpans
    )
}

data class Segment(
    val leftPosition: Float,
    val rightPosition: Float,
    val leftOffset: Int,
    val rightOffset: Int,
)

fun List<Segment>.allLeftWidth(index: Int): Float {
    var totalWidth = 0f
    for (i in 0 until index) {
        totalWidth += this[index].rightPosition - this[index].leftPosition
    }
    return totalWidth
}

fun List<Segment>.allRightWidth(index: Int): Float {
    var totalWidth = 0f
    for (i in index + 1 until this.size) {
        totalWidth += this[index].rightPosition - this[index].leftPosition
    }
    return totalWidth
}
