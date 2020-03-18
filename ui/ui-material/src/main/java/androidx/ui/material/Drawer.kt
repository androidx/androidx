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

import androidx.animation.AnimatedFloat
import androidx.animation.PhysicsBuilder
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.DensityAmbient
import androidx.ui.core.DrawClipToBounds
import androidx.ui.core.Layout
import androidx.ui.core.WithConstraints
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Stack
import androidx.ui.material.internal.StateDraggable
import androidx.ui.unit.IntPx
import androidx.ui.unit.Px
import androidx.ui.unit.dp
import androidx.ui.unit.min
import androidx.ui.unit.px
import androidx.ui.unit.toRect
import androidx.ui.util.lerp

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
 * See [ModalDrawerLayout] and [BottomDrawerLayout] for more mobile friendly options.
 *
 * @sample androidx.ui.material.samples.StaticDrawerSample
 *
 * @param drawerContent composable that represents content inside the drawer
 */
@Composable
fun StaticDrawer(
    drawerContent: @Composable() () -> Unit
) {
    Box(LayoutWidth(StaticDrawerWidth) + LayoutHeight.Fill, children = drawerContent)
}

/**
 * Navigation drawers provide access to destinations in your app.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim.
 * They are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * See [StaticDrawer] for always visible drawer, suitable for tablet or desktop.
 * See [BottomDrawerLayout] for a layout that introduces a bottom drawer, suitable when
 * using bottom navigation.
 *
 * @sample androidx.ui.material.samples.ModalDrawerSample
 *
 * @param drawerState state of the drawer
 * @param onStateChange lambda to be invoked when the drawer requests to change its state,
 * e.g. when the drawer is being swiped to the new state or when the scrim is clicked
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerContent composable that represents content inside the drawer
 * @param bodyContent content of the rest of the UI
 *
 * @throws IllegalStateException when parent has [Px.Infinity] width
 */
@Composable
fun ModalDrawerLayout(
    drawerState: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    gesturesEnabled: Boolean = true,
    drawerContent: @Composable() () -> Unit,
    bodyContent: @Composable() () -> Unit
) {
    Box(LayoutSize.Fill) {
        WithConstraints { pxConstraints, _ ->
            // TODO : think about Infinite max bounds case
            if (!pxConstraints.hasBoundedWidth) {
                throw IllegalStateException("Drawer shouldn't have infinite width")
            }
            val constraints = with(DensityAmbient.current) {
                DpConstraints(pxConstraints)
            }
            val minValue = -pxConstraints.maxWidth.value.toFloat()
            val maxValue = 0f

            val anchors = listOf(minValue to DrawerState.Closed, maxValue to DrawerState.Opened)
            StateDraggable(
                state = drawerState,
                onStateChange = onStateChange,
                anchorsToState = anchors,
                animationBuilder = AnimationBuilder,
                dragDirection = DragDirection.Horizontal,
                minValue = minValue,
                maxValue = maxValue,
                enabled = gesturesEnabled
            ) { model ->
                Stack {
                    bodyContent()
                    Scrim(drawerState, onStateChange, fraction = {
                        calculateFraction(minValue, maxValue, model.value)
                    })
                    DrawerContent(model, constraints, drawerContent)
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
 * See [StaticDrawer] for always visible drawer, suitable for tablet or desktop
 * See [ModalDrawerLayout] for a layout that introduces a classic from-the-side drawer.
 *
 * @sample androidx.ui.material.samples.BottomDrawerSample
 *
 * @param drawerState state of the drawer
 * @param onStateChange lambda to be invoked when the drawer requests to change its state,
 * e.g. when the drawer is being swiped to the new state or when the scrim is clicked
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerContent composable that represents content inside the drawer
 * @param bodyContent content of the rest of the UI
 *
 * @throws IllegalStateException when parent has [Px.Infinity] height
 */
@Composable
fun BottomDrawerLayout(
    drawerState: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    gesturesEnabled: Boolean = true,
    drawerContent: @Composable() () -> Unit,
    bodyContent: @Composable() () -> Unit
) {
    Box(LayoutSize.Fill) {
        WithConstraints { pxConstraints, _ ->
            // TODO : think about Infinite max bounds case
            if (!pxConstraints.hasBoundedHeight) {
                throw IllegalStateException("Drawer shouldn't have infinite height")
            }
            val constraints = with(DensityAmbient.current) {
                DpConstraints(pxConstraints)
            }
            val minValue = 0f
            val maxValue = pxConstraints.maxHeight.value.toFloat()

            // TODO: add proper landscape support
            val isLandscape = constraints.maxWidth > constraints.maxHeight
            val openedValue = if (isLandscape) minValue else lerp(
                minValue,
                maxValue,
                BottomDrawerOpenFraction
            )
            val anchors =
                if (isLandscape) {
                    listOf(maxValue to DrawerState.Closed, minValue to DrawerState.Opened)
                } else {
                    listOf(
                        maxValue to DrawerState.Closed,
                        openedValue to DrawerState.Opened,
                        minValue to DrawerState.Opened
                    )
                }
            StateDraggable(
                state = drawerState,
                onStateChange = onStateChange,
                anchorsToState = anchors,
                animationBuilder = AnimationBuilder,
                dragDirection = DragDirection.Vertical,
                minValue = minValue,
                maxValue = maxValue,
                enabled = gesturesEnabled
            ) { model ->
                Stack {
                    bodyContent()
                    Scrim(drawerState, onStateChange, fraction = {
                        // as we scroll "from height to 0" , need to reverse fraction
                        1 - calculateFraction(openedValue, maxValue, model.value)
                    })
                    BottomDrawerContent(model, constraints, drawerContent)
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    xOffset: AnimatedFloat,
    constraints: DpConstraints,
    children: @Composable() () -> Unit
) {
    WithOffset(xOffset = xOffset) {
        Box(
            LayoutSize.Constrain(
                constraints.minWidth,
                constraints.minHeight,
                constraints.maxWidth,
                constraints.maxHeight
            ),
            paddingEnd = VerticalDrawerPadding
        ) {
            // remove Container when we will support multiply children
            Surface {
                Box(LayoutSize.Fill, children = children)
            }
        }
    }
}

@Composable
private fun BottomDrawerContent(
    yOffset: AnimatedFloat,
    constraints: DpConstraints,
    children: @Composable() () -> Unit
) {
    WithOffset(yOffset = yOffset) {
        Box(
            LayoutSize.Constrain(
                constraints.minWidth,
                constraints.minHeight,
                constraints.maxWidth,
                constraints.maxHeight
            )
        ) {
            // remove Container when we will support multiply children
            Surface {
                Box(LayoutSize.Fill, children = children)
            }
        }
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun Scrim(
    state: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    fraction: () -> Float
) {
    // TODO: use enabled = false here when it will be available
    val scrimContent = @Composable {
        val paint = remember { Paint().apply { style = PaintingStyle.fill } }
        val color = MaterialTheme.colors().onSurface
        Canvas(LayoutSize.Fill) {
            val scrimAlpha = fraction() * ScrimDefaultOpacity
            paint.color = color.copy(alpha = scrimAlpha)
            drawRect(size.toRect(), paint)
        }
    }
    if (state == DrawerState.Opened) {
        Clickable(onClick = { onStateChange(DrawerState.Closed) }, children = scrimContent)
    } else {
        scrimContent()
    }
}

// TODO: consider make pretty and move to public
@Composable
private fun WithOffset(
    xOffset: AnimatedFloat? = null,
    yOffset: AnimatedFloat? = null,
    child: @Composable() () -> Unit
) {
    Layout(children = {
        Box(modifier = DrawClipToBounds, children = child)
    }) { measurables, constraints, _ ->
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
            val offX = xOffset?.value?.px ?: 0.px
            val offY = yOffset?.value?.px ?: 0.px
            placeable?.place(offX, offY)
        }
    }
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

internal val BottomDrawerOpenFraction = 0.5f
