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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.BackgroundAnnotation
import androidx.compose.ui.text.BackgroundAnnotation.SpreadValues
import androidx.compose.ui.text.DrawAnnotation
import androidx.compose.ui.text.DrawAnnotationScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withDrawAnnotation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun DrawAnnotationSample() {
    val cachedPath = remember { Path() }
    val highlight = remember {
        DrawAnnotation(
            drawBehind = {
                textLayoutResult.fillPathForRange(
                    start = start,
                    end = end,
                    target = cachedPath,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                drawPath(cachedPath, color = Color.Yellow.copy(alpha = 0.4f))
            }
        )
    }
    BasicText(
        buildAnnotatedString {
            append("Compose ")
            withDrawAnnotation(highlight) { append("custom draw annotation") }
        }
    )
}

@Sampled
@Composable
fun DrawAnnotationSubclassSample() {
    data class HighlightPathDrawAnnotation(val color: Color, val cornerRadius: Dp) :
        DrawAnnotation() {
        // Reuse a single Path instance to avoid allocating a Path on each draw pass.
        // TextLayoutResult caches the underlying range bounding boxes internally, so the same
        // annotation instance can be shared across multiple ranges.
        private val cachedPath = Path()

        override fun DrawAnnotationScope.drawBehind() {
            textLayoutResult.fillPathForRange(
                start = start,
                end = end,
                target = cachedPath,
                cornerRadius = CornerRadius(cornerRadius.toPx()),
            )
            drawPath(cachedPath, color = color)
        }
    }

    val highlight = remember {
        HighlightPathDrawAnnotation(
            color = Color.Yellow.copy(alpha = 0.4f),
            cornerRadius = 4.dp,
        )
    }
    BasicText(
        buildAnnotatedString {
            withDrawAnnotation(highlight) { append("First range") }
            append(" and ")
            withDrawAnnotation(highlight) { append("second range") }
        }
    )
}

@Sampled
@Composable
fun BackgroundAnnotationSample() {
    val text = remember {
        buildAnnotatedString {
            append("Use ")
            withDrawAnnotation(
                BackgroundAnnotation(
                    color = Color.Cyan.copy(alpha = 0.2f),
                    cornerRadius = 6.dp,
                    spread = SpreadValues(horizontal = 4.dp, vertical = 2.dp),
                    border = BackgroundAnnotation.Border(width = 1.dp, color = Color.Blue),
                )
            ) {
                append("BackgroundAnnotation")
            }
            append(" to draw rounded backgrounds and borders.")
        }
    }
    BasicText(text = text)
}

@Sampled
@Composable
fun BackgroundAnnotationSpreadSample() {
    val text = remember {
        buildAnnotatedString {
            append("Use positive horizontal and negative vertical ")
            withDrawAnnotation(
                BackgroundAnnotation(
                    color = Color.Cyan.copy(alpha = 0.25f),
                    cornerRadius = 6.dp,
                    // Positive horizontal spread adds side space around the glyphs;
                    // negative vertical spread shrinks from the line top/bottom so wrapped pills
                    // on consecutive lines stay visually separated instead of touching.
                    spread = SpreadValues(horizontal = 4.dp, vertical = (-2).dp),
                    mergeLines = false,
                    border = BackgroundAnnotation.Border(width = 1.dp, color = Color.Blue),
                )
            ) {
                append("SpreadValues on multi-line per-line pills")
            }
            append(" so adjacent lines do not touch.")
        }
    }
    BasicText(text = text, style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp))
}

@Sampled
@Composable
fun BackgroundAnnotationColorProducerSample() {
    val transition = rememberInfiniteTransition(label = "highlight")
    val animatedColor by
        transition.animateColor(
            initialValue = Color.Cyan.copy(alpha = 0.2f),
            targetValue = Color.Cyan.copy(alpha = 0.6f),
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "highlightColor",
        )
    val text = remember {
        buildAnnotatedString {
            append("Animate ")
            withDrawAnnotation(
                BackgroundAnnotation(
                    colorProducer = { animatedColor },
                    cornerRadius = 6.dp,
                    spread = SpreadValues(horizontal = 4.dp, vertical = 2.dp),
                )
            ) {
                append("draw-phase color")
            }
            append(" without recomposing or re-measuring text.")
        }
    }
    BasicText(text = text)
}

@Sampled
@Composable
fun BackgroundAnnotationBrushSample() {
    val gradientBrush = remember {
        Brush.horizontalGradient(
            colors =
                listOf(
                    Color.Cyan.copy(alpha = 0.3f),
                    Color.Magenta.copy(alpha = 0.3f),
                    Color.Red.copy(alpha = 0.3f),
                )
        )
    }
    val borderGradientBrush = remember {
        Brush.horizontalGradient(colors = listOf(Color.Blue, Color.Magenta))
    }
    val text = remember {
        buildAnnotatedString {
            append("Use a ")
            withDrawAnnotation(
                BackgroundAnnotation(
                    brush = gradientBrush,
                    cornerRadius = 6.dp,
                    spread = SpreadValues(horizontal = 4.dp, vertical = 2.dp),
                    border = BackgroundAnnotation.Border(width = 1.dp, brush = borderGradientBrush),
                )
            ) {
                append("gradient Brush background")
            }
            append(" with a matching gradient border.")
        }
    }
    BasicText(text = text)
}

@Sampled
@Composable
fun TextLayoutResultGetBoundingBoxesForRangeSample() {
    val textMeasurer = rememberTextMeasurer()
    val highlightRange = TextRange(10, 25)
    var boundingBoxes by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Canvas(
        Modifier.size(200.dp, 80.dp).layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val result =
                textMeasurer.measure(
                    text = "Highlight a range of text",
                    style = TextStyle(fontSize = 18.sp),
                    constraints = Constraints(maxWidth = placeable.width),
                )
            textLayoutResult = result
            boundingBoxes =
                result.getBoundingBoxesForRange(
                    start = highlightRange.start,
                    end = highlightRange.end,
                )
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    ) {
        textLayoutResult?.let { result ->
            for (i in boundingBoxes.indices) {
                val box = boundingBoxes[i]
                drawRect(
                    color = Color.Yellow,
                    topLeft = box.topLeft,
                    size = box.size,
                )
            }
            drawText(result)
        }
    }
}

@Sampled
@Composable
fun TextLayoutResultFillPathForRangeSample() {
    val textMeasurer = rememberTextMeasurer()
    val highlightRange = TextRange(12, 36)
    val cachedPath = remember { Path() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Canvas(
        Modifier.size(200.dp, 100.dp).layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val result =
                textMeasurer.measure(
                    text = "Highlight a multi-line range of text with a merged rounded path",
                    style = TextStyle(fontSize = 18.sp),
                    constraints = Constraints(maxWidth = placeable.width),
                )
            textLayoutResult = result
            result.fillPathForRange(
                start = highlightRange.start,
                end = highlightRange.end,
                target = cachedPath,
                cornerRadius = CornerRadius(6.dp.toPx()),
                mergeLines = true,
            )
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    ) {
        textLayoutResult?.let { result ->
            drawPath(path = cachedPath, color = Color.Yellow)
            drawText(result)
        }
    }
}
