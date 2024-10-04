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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material.Text
import kotlin.math.max
import kotlin.math.roundToInt

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
                    Modifier.transformedHeight { originalHeight, scrollProgression ->
                            if (scrollProgression.topOffsetFraction < 0f)
                                (originalHeight * scrollProgression.bottomOffsetFraction /
                                        (scrollProgression.bottomOffsetFraction -
                                            scrollProgression.topOffsetFraction))
                                    .roundToInt()
                            else originalHeight
                        }
                        .graphicsLayer {
                            val itemProgression = scrollProgress ?: return@graphicsLayer
                            rotationY =
                                -180f +
                                    (itemProgression.topOffsetFraction +
                                        itemProgression.bottomOffsetFraction) * 180f
                            val scale =
                                (itemProgression.bottomOffsetFraction -
                                    max(itemProgression.topOffsetFraction, 0f)) /
                                    (itemProgression.bottomOffsetFraction -
                                        itemProgression.topOffsetFraction)
                            scaleY = scale
                            translationY = size.height * (scale - 1f) / 2f
                        }
                        .drawBehind {
                            val colorProgress =
                                scrollProgress?.let {
                                    (it.topOffsetFraction + it.bottomOffsetFraction) / 2f
                                } ?: 0f
                            drawCircle(rainbowColor(colorProgress))
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
