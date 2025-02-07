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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.LocalTransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material.Text
import kotlin.math.max
import kotlin.math.roundToInt

@Sampled
@Preview
@Composable
fun TransformingLazyColumnAnimateItemSample() {
    val state = rememberTransformingLazyColumnState()

    var list by remember { mutableStateOf(listOf("1", "2", "3")) }

    var next by remember { mutableIntStateOf(4) }

    Box(Modifier.fillMaxSize()) {
        TransformingLazyColumn(
            state = state,
            contentPadding = PaddingValues(5.dp),
            modifier = Modifier.background(Color.Black)
        ) {
            items(list.size, key = { list[it] }) {
                Text(
                    "Item ${list[it]}",
                    Modifier.animateItem().clickable {
                        list = list.filter { elem -> elem != list[it] }
                    }
                )
            }
        }
        Text(
            "+",
            Modifier.align(Alignment.CenterStart).padding(horizontal = 5.dp).clickable {
                if (list.size < 25) list = list + "${next++}"
            }
        )
        Text(
            "S",
            Modifier.align(Alignment.CenterEnd).padding(horizontal = 5.dp).clickable {
                list = list.shuffled()
            }
        )
    }
}

@Preview
@Sampled
@Composable
fun TransformingLazyColumnLettersSample() {
    val alphabet = ('A'..'Z').map { it.toString() }

    fun rainbowColor(progress: Float): Color {
        val hue = progress * 360f
        val saturation = 1f
        val value = 1f

        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    TransformingLazyColumn {
        items(count = alphabet.size) { index ->
            Text(
                alphabet[index],
                modifier =
                    Modifier.transformedHeight { measuredHeight, scrollProgress ->
                            if (scrollProgress.topOffsetFraction < 0f)
                                (measuredHeight * scrollProgress.bottomOffsetFraction /
                                        (scrollProgress.bottomOffsetFraction -
                                            scrollProgress.topOffsetFraction))
                                    .roundToInt()
                            else measuredHeight
                        }
                        .graphicsLayer {
                            with(scrollProgress) {
                                if (isUnspecified) {
                                    return@graphicsLayer
                                }
                                rotationY =
                                    -180f + (topOffsetFraction + bottomOffsetFraction) * 180f
                                val scale =
                                    (bottomOffsetFraction - max(topOffsetFraction, 0f)) /
                                        (bottomOffsetFraction - topOffsetFraction)
                                scaleY = scale
                                translationY = size.height * (scale - 1f) / 2f
                            }
                        }
                        .drawBehind {
                            with(scrollProgress) {
                                if (isUnspecified) {
                                    return@drawBehind
                                }
                                val colorProgress = (topOffsetFraction + bottomOffsetFraction) / 2f
                                drawCircle(rainbowColor(colorProgress))
                            }
                        }
                        .padding(20.dp)
            )
        }
    }
}

@Preview
@Composable
fun TransformingLazyColumnRectangularBoxesSample() {
    TransformingLazyColumn {
        items(count = 10) {
            Text(
                "Item $it",
                modifier =
                    Modifier.background(Color.Gray).padding(10.dp).transformedHeight {
                        originalHeight,
                        _ ->
                        originalHeight / 2
                    }
            )
        }
    }
}

@Preview
@Sampled
@Composable
fun TransformingLazyColumnImplicitSample() {
    fun rainbowColor(progress: Float): Color {
        val hue = progress * 360f
        val saturation = 1f
        val value = 1f

        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    // Create a Box that automatically transform when in a TransformingLazyColumn
    val implicitTransformingBox: @Composable (String) -> Unit = { text ->
        val itemScope = LocalTransformingLazyColumnItemScope.current
        Box(
            Modifier.height(30.dp)
                .then(
                    itemScope?.let {
                        with(it) {
                            Modifier.graphicsLayer {
                                    with(scrollProgress) {
                                        if (isUnspecified) {
                                            return@graphicsLayer
                                        }
                                        scaleX =
                                            (bottomOffsetFraction - max(topOffsetFraction, 0f)) /
                                                (bottomOffsetFraction - topOffsetFraction)
                                    }
                                }
                                .drawBehind {
                                    with(scrollProgress) {
                                        if (isUnspecified) {
                                            return@drawBehind
                                        }
                                        val colorProgress =
                                            (topOffsetFraction + bottomOffsetFraction) / 2f
                                        drawRect(rainbowColor(colorProgress))
                                    }
                                }
                        }
                    } ?: Modifier.background(Color.Green)
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicText(text)
        }
    }
    TransformingLazyColumn {
        items(count = 10) { implicitTransformingBox("Before #$it") }
        // The middle 3 boxes will not auto-transform
        items(count = 3) { TransformExclusion { implicitTransformingBox("Middle #$it") } }
        items(count = 10) { implicitTransformingBox("After #$it") }
    }
}

@Sampled
@Composable
fun UsingListAnchorItemPositionInCompositionSample() {
    val columnState = rememberTransformingLazyColumnState()
    val isAtCenter by remember {
        derivedStateOf {
            columnState.anchorItemIndex == 0 && columnState.anchorItemScrollOffset == 0
        }
    }

    @Composable
    fun ScrollToTopButton(@Suppress("UNUSED_PARAMETER") columnState: TransformingLazyColumnState) {}

    if (!isAtCenter) {
        ScrollToTopButton(columnState)
    }
}
