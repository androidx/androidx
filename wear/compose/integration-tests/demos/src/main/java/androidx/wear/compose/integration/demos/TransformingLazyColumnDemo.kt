/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material.Text
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TransformingLazyColumnLettersDemo() {
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
                        .padding(20.dp),
            )
        }
    }
}
