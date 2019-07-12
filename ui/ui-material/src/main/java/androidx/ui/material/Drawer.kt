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

package androidx.ui.material

import androidx.animation.PhysicsBuilder
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.onCommit
import androidx.compose.unaryPlus
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.Px
import androidx.ui.core.RepaintBoundary
import androidx.ui.core.WithConstraints
import androidx.ui.core.dp
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.core.min
import androidx.ui.core.withDensity
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.gestures.AnchorsFlingConfig
import androidx.ui.foundation.gestures.AnimatedDraggable
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.layout.Alignment
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Stack
import androidx.ui.lerp
import androidx.ui.material.surface.Surface
import kotlin.math.max

/**
 * Possible states of the drawer
 */
enum class DrawerState {
    /**
     * Constant to indicate the state of the drawer when it's closed
     */
    Closed,
    /**
     * Constant to indicate the state of the drawer when it's opened
     */
    Opened,
    // Expanded
}

/**
 * Navigation drawers provide access to destinations in your app.
 *
 * Standard navigation drawers allow interaction with both screen content and the drawer
 * at the same time. They can be used on tablet and desktop,
 * but they are not suitable for mobile due to limited screen size.
 *
 * @see [ModalDrawer] and [BottomDrawer] for more mobile friendly options
 *
 * @sample androidx.ui.material.samples.StaticDrawerSample
 *
 * @param drawerContent composable that represents content inside the drawer
 */
@Composable
fun StaticDrawer(
    drawerContent: @Composable() () -> Unit
) {
    Container(width = StaticDrawerWidth, expanded = true) {
        drawerContent()
    }
}

/**
 * Navigation drawers provide access to destinations in your app.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim.
 * They are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * @see [StaticDrawer] for always visible drawer, suitable for tables or desktop
 * @see [BottomDrawer] for drawer that is recommended when you have bottom navigation
 *
 * @sample androidx.ui.material.samples.ModalDrawerSample
 *
 * @param drawerState state of the drawer
 * @param onStateChange lambda to be invoked when the drawer requests to change its state,
 * e.g. when the drawer is being swiped to the new state or when the scrim is clicked
 * @param drawerContent composable that represents content inside the drawer
 *
 * @throws IllegalStateException when parent has [Px.Infinity] width
 */
@Composable
fun ModalDrawer(
    drawerState: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    drawerContent: @Composable() () -> Unit
) {
    Container(expanded = true) {
        WithConstraints { pxConstraints ->
            // TODO : think about Infinite max bounds case
            if (!pxConstraints.hasBoundedWidth) {
                throw IllegalStateException("Drawer shouldn't have infinite width")
            }
            val constraints = +withDensity {
                DpConstraints(pxConstraints)
            }
            val minValue = -pxConstraints.maxWidth.value.toFloat()
            val maxValue = 0f
            val valueByState = if (drawerState == DrawerState.Opened) maxValue else minValue

            val flingConfig = AnchorsFlingConfig(
                listOf(minValue, maxValue),
                onAnimationFinished = { finalValue, cancelled ->
                    if (!cancelled) {
                        onStateChange(
                            if (finalValue <= minValue) DrawerState.Closed else DrawerState.Opened
                        )
                    }
                },
                animationBuilder = AnimationBuilder
            )

            AnimatedDraggable(
                dragDirection = DragDirection.Horizontal,
                startValue = valueByState,
                minValue = minValue,
                maxValue = maxValue,
                flingConfig = flingConfig
            ) { animatedValue ->
                +onCommit(valueByState) {
                    animatedValue.animateTo(valueByState, AnimationBuilder)
                }
                val fraction = calculateFraction(minValue, maxValue, animatedValue.value)
                val scrimAlpha = fraction * ScrimDefaultOpacity
                val dpOffset = +withDensity {
                    animatedValue.value.toDp()
                }

                Stack {
                    aligned(Alignment.TopLeft) {
                        Scrim(drawerState, onStateChange, scrimAlpha)
                        DrawerContent(dpOffset, constraints, drawerContent)
                    }
                }
            }
        }
    }
}

/**
 * Navigation drawers provide access to destinations in your app.
 *
 * Bottom navigation drawers are modal drawers that are anchored
 * to the bottom of the screen instead of the left or right edge.
 * They are only used with bottom app bars.
 *
 * These drawers open upon tapping the navigation menu icon in the bottom app bar.
 * They are only for use on mobile.
 *
 * @see [StaticDrawer] for always visible drawer, suitable for tables or desktop
 * @see [ModalDrawer] for classic "from the side" drawer
 *
 * @sample androidx.ui.material.samples.BottomDrawerSample
 *
 * @param drawerState state of the drawer
 * @param onStateChange lambda to be invoked when the drawer requests to change its state,
 * e.g. when the drawer is being swiped to the new state or when the scrim is clicked
 * @param drawerContent composable that represents content inside the drawer
 *
 * @throws IllegalStateException when parent has [Px.Infinity] height
 */
@Composable
fun BottomDrawer(
    drawerState: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    drawerContent: @Composable() () -> Unit
) {
    Container(expanded = true) {
        WithConstraints { pxConstraints ->
            // TODO : think about Infinite max bounds case
            if (!pxConstraints.hasBoundedHeight) {
                throw IllegalStateException("Drawer shouldn't have infinite height")
            }
            val constraints = +withDensity {
                DpConstraints(pxConstraints)
            }
            val minValue = 0f
            val maxValue = pxConstraints.maxHeight.value.toFloat()

            // TODO: add proper landscape support
            val isLandscape = constraints.maxWidth > constraints.maxHeight
            val openedValue = if (isLandscape) maxValue else lerp(
                minValue,
                maxValue,
                BottomDrawerOpenFraction
            )
            val valueByState = if (drawerState == DrawerState.Opened) openedValue else maxValue
            val anchors = listOf(minValue, maxValue, openedValue)

            val onAnimationFinished = { finalValue: Float, cancelled: Boolean ->
                if (!cancelled) {
                    onStateChange(
                        if (finalValue >= maxValue) DrawerState.Closed else DrawerState.Opened
                    )
                }
            }

            AnimatedDraggable(
                dragDirection = DragDirection.Vertical,
                startValue = valueByState,
                minValue = minValue,
                maxValue = maxValue,
                flingConfig = AnchorsFlingConfig(anchors, onAnimationFinished, AnimationBuilder)
            ) { animatedValue ->
                +onCommit(valueByState) {
                    animatedValue.animateTo(valueByState, AnimationBuilder)
                }
                // as we scroll "from height to 0" backwards, (1 - fraction) will reverse it
                val fractionToOpened =
                    1 - max(0f, calculateFraction(openedValue, maxValue, animatedValue.value))
                val scrimAlpha = fractionToOpened * ScrimDefaultOpacity
                val dpOffset = +withDensity {
                    animatedValue.value.toDp()
                }
                Stack {
                    aligned(Alignment.TopLeft) {
                        Scrim(drawerState, onStateChange, scrimAlpha)
                        BottomDrawerContent(dpOffset, constraints, drawerContent)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    xOffset: Dp,
    constraints: DpConstraints,
    children: @Composable() () -> Unit
) {
    WithOffset(xOffset = xOffset) {
        Container(
            constraints = constraints,
            padding = EdgeInsets(right = VerticalDrawerPadding)
        ) {
            // remove Container when we will support multiply children
            Surface { Container(expanded = true) { children() } }
        }
    }
}

@Composable
private fun BottomDrawerContent(
    yOffset: Dp,
    constraints: DpConstraints,
    children: @Composable() () -> Unit
) {
    WithOffset(yOffset = yOffset) {
        Container(constraints = constraints) {
            // remove Container when we will support multiply children
            Surface { Container(expanded = true) { children() } }
        }
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) = (pos - a) / (b - a)

@Composable
private fun Scrim(state: DrawerState, onStateChange: (DrawerState) -> Unit, opacity: Float) {
    // TODO: use enabled = false here when it will be available
    if (state == DrawerState.Opened) {
        Clickable(onClick = { onStateChange(DrawerState.Closed) }) {
            ColoredRect(+themeColor { onSurface.copy(alpha = opacity) })
        }
    } else {
        ColoredRect(+themeColor { onSurface.copy(alpha = opacity) })
    }
}

// TODO: consider make pretty and move to public
@Composable
private fun WithOffset(
    xOffset: Dp = 0.dp,
    yOffset: Dp = 0.dp,
    child: @Composable() () -> Unit
) {
    Layout(children = {
        RepaintBoundary {
            child()
        }
    }, layoutBlock = { measurables, constraints ->
        if (measurables.size > 1) {
            throw IllegalStateException("Only one child is allowed")
        }
        val childMeasurable = measurables.firstOrNull()
        val placeable = childMeasurable?.measure(constraints)
        val width: IntPx
        val height: IntPx
        if (placeable == null) {
            width = constraints.minWidth
            height = constraints.minHeight
        } else {
            width = min(placeable.width, constraints.maxWidth)
            height = min(placeable.height, constraints.maxHeight)
        }
        layout(width, height) {
            placeable?.place(xOffset.toIntPx(), yOffset.toIntPx())
        }
    })
}

private val ScrimDefaultOpacity = 0.32f
private val VerticalDrawerPadding = 56.dp

// drawer children specs
private val StaticDrawerWidth = 256.dp
private val DrawerStiffness = 1000f

private val AnimationBuilder =
    PhysicsBuilder<Float>().apply {
        stiffness = DrawerStiffness
    }

private val BottomDrawerOpenFraction = 0.5f