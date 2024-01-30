/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Sampled
@Composable
fun DrawTextSample() {
    val textMeasurer = rememberTextMeasurer()

    Canvas(Modifier.fillMaxSize()) {
        drawText(textMeasurer, "Hello, World!")
    }
}

@Sampled
@Composable
fun DrawTextStyledSample() {
    val textMeasurer = rememberTextMeasurer()

    Canvas(Modifier.fillMaxSize()) {
        drawText(
            textMeasurer = textMeasurer,
            text = "Hello, World!",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        )
    }
}

@Sampled
@Composable
fun DrawTextAnnotatedStringSample() {
    val textMeasurer = rememberTextMeasurer()

    Canvas(Modifier.fillMaxSize()) {
        drawText(
            textMeasurer = textMeasurer,
            text = buildAnnotatedString {
                withStyle(ParagraphStyle(textAlign = TextAlign.Start)) {
                    append("Hello")
                }
                withStyle(ParagraphStyle(textAlign = TextAlign.End)) {
                    append("World")
                }
            }
        )
    }
}

/**
 * This sample demonstrates how to use layout phase to improve performance when drawing text on
 * DrawScope. We can use [layout] Modifier or [Layout] composable to calculate the text layout
 * during layout phase and then cache the result in a Snapshot aware state to draw it during draw
 * phase.
 */
@Sampled
@Composable
fun DrawTextMeasureInLayoutSample() {
    val textMeasurer = rememberTextMeasurer()
    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // TextLayout can be done any time prior to its use in draw, including in a
                // background thread.
                // In this sample, text layout is measured in layout modifier. This way the layout
                // call can be restarted when async font loading completes due to the fact that
                // `.measure` call is executed in `.layout`.
                textLayoutResult = textMeasurer.measure(
                    text = "Hello, World!",
                    style = TextStyle(fontSize = 24.sp)
                )
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }) {
        // This happens during draw phase.
        textLayoutResult?.let { drawText(it) }
    }
}

/**
 * This sample demonstrates how to use [drawWithCache] modifier to improve performance when drawing
 * text on DrawScope. We can use [drawWithCache] to calculate the text layout once in
 * [CacheDrawScope] and then repeatedly use the same [TextLayoutResult] in the draw phase.
 *
 * This approach improves performance when the text itself does not change but its draw attributes
 * do change over time, such as during a color animation.
 */
@Sampled
@Composable
fun DrawTextDrawWithCacheSample() {
    // We can disable implicit caching since we will cache in DrawWithCache
    val textMeasurer = rememberTextMeasurer(cacheSize = 0)
    // Apply the current text style from theme, otherwise TextStyle.Default will be used.
    val materialTextStyle = LocalTextStyle.current

    // Animate color repeatedly
    val infiniteTransition = rememberInfiniteTransition()
    val color by infiniteTransition.animateColor(
        initialValue = Color.Red,
        targetValue = Color.Blue,
        animationSpec = infiniteRepeatable(tween(1000))
    )

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                // Text layout will be measured just once until the size of the drawing area or
                // materialTextStyle changes.
                val textLayoutResult = textMeasurer.measure(
                    text = "Hello, World!",
                    style = materialTextStyle,
                    constraints = Constraints.fixed(
                        width = (size.width / 2).roundToInt(),
                        height = (size.height / 2).roundToInt()
                    ),
                    overflow = TextOverflow.Ellipsis
                )
                // color changes will only invalidate draw phase
                onDrawWithContent {
                    drawContent()
                    drawText(
                        textLayoutResult,
                        color = color,
                        topLeft = Offset(
                            (size.width - textLayoutResult.size.width) / 2,
                            (size.height - textLayoutResult.size.height) / 2,
                        )
                    )
                }
            })
}
