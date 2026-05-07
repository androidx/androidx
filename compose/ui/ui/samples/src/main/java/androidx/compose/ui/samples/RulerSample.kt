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
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
                },
            ) {
                placeable.place(0, 0)
            }
        },
        content = content,
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
        content = content,
    )
}

@Sampled
fun DerivedVerticalRulerUsage() {
    class PaddedRulers(val ruler: VerticalRuler, val padding: Dp) {
        val left =
            VerticalRuler.derived { defaultValue ->
                val rulerValue = ruler.current(Float.NaN)
                if (rulerValue.isNaN()) {
                    defaultValue
                } else {
                    rulerValue + padding.toPx()
                }
            }
        val right =
            VerticalRuler.derived { defaultValue ->
                val rulerValue = ruler.current(Float.NaN)
                if (rulerValue.isNaN()) {
                    defaultValue
                } else {
                    rulerValue - padding.toPx()
                }
            }
    }
}

@Sampled
fun DerivedHorizontalRulerUsage() {
    class PaddedRulers(val ruler: HorizontalRuler, val padding: Dp) {
        val top =
            HorizontalRuler.derived { defaultValue ->
                val rulerValue = ruler.current(Float.NaN)
                if (rulerValue.isNaN()) {
                    defaultValue
                } else {
                    rulerValue + padding.toPx()
                }
            }
        val bottom =
            HorizontalRuler.derived { defaultValue ->
                val rulerValue = ruler.current(Float.NaN)
                if (rulerValue.isNaN()) {
                    defaultValue
                } else {
                    rulerValue - padding.toPx()
                }
            }
    }
}

// This is an example of how to provide ruler values where it may be somewhat expensive
// to provide always and only makes sense to provide when the developer requests it.
@Sampled
fun ProvideRulerOnlyWhenUsed() {
    // Position of the left side of the screen
    val ScreenLeftRuler = VerticalRuler()
    // Position of the top of the screen
    val ScreenTopRuler = HorizontalRuler()
    // Position of the right side of the screen
    val ScreenRightRuler = VerticalRuler()
    // Position of the bottom of the screen
    val ScreenBottomRuler = HorizontalRuler()

    class ScreenRulerNode :
        Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(
                placeable.width,
                placeable.height,
                isRulerProvided = { ruler ->
                    ruler === ScreenLeftRuler ||
                        ruler === ScreenTopRuler ||
                        ruler === ScreenRightRuler ||
                        ruler === ScreenBottomRuler
                },
                rulerProvider = {
                    // Because these rulers are updated when the View or Window moves, this may be
                    // expensive if the ComposeView is in a RecyclerView, for example. If no
                    // content has requested a screen position ruler, this won't be called and
                    // the calculation can be avoided.
                    val positionOnScreen = coordinates.positionOnScreen()

                    // Once we've calculated the position on screen, we may as well provide
                    // all values since it doesn't cost much to provide everything. The developer
                    // is likely to request multiple values if they request one.
                    ScreenLeftRuler provides -positionOnScreen.x
                    ScreenTopRuler provides -positionOnScreen.y

                    // We have to find the screen size from the DisplayMetrics
                    val displayMetrics = currentValueOf(LocalContext).resources.displayMetrics
                    val screenHeight = displayMetrics.heightPixels
                    val screenWidth = displayMetrics.widthPixels
                    ScreenRightRuler provides screenWidth - positionOnScreen.x
                    ScreenBottomRuler provides screenHeight - positionOnScreen.y
                },
            ) {
                placeable.place(0, 0)
            }
        }
    }
}
