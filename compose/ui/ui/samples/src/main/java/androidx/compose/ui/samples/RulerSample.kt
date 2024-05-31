/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

val SafeBottomRuler = HorizontalRuler()

@Sampled
@Composable
fun RulerProducerUsage(content: @Composable BoxScope.() -> Unit) {
    val safeInsets = WindowInsets.safeContent

    Box(
        Modifier.fillMaxSize().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(
                width = placeable.width,
                height = placeable.height,
                rulers = {
                    val height = coordinates.size.height
                    SafeBottomRuler provides (height - safeInsets.getBottom(this)).toFloat()
                }
            ) {
                placeable.place(0, 0)
            }
        },
        content = content
    )
}

@Sampled
@Composable
fun RulerConsumerUsage(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.layout { measurable, constraints ->
            if (!constraints.hasBoundedHeight || !constraints.hasBoundedWidth) {
                // Can't use the ruler. We don't know our size
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            } else {
                // Use the entire space available
                layout(constraints.maxWidth, constraints.maxHeight) {
                    // Child is measured to fit above the IME
                    val imePosition = SafeBottomRuler.current(-1f)
                    val maxHeight: Int
                    if (imePosition <= 0 || imePosition >= constraints.maxHeight) {
                        // IME ruler is outside the bounds of this layout
                        maxHeight = constraints.maxHeight
                    } else {
                        maxHeight = imePosition.roundToInt()
                    }
                    val minHeight = constraints.minHeight.coerceAtMost(maxHeight)
                    val childConstraints =
                        constraints.copy(minHeight = minHeight, maxHeight = maxHeight)
                    val placeable = measurable.measure(childConstraints)
                    placeable.place(0, 0)
                }
            }
        },
        content = content
    )
}
