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

package androidx.compose.material

import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.state
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.WithConstraints
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.foundation.Box
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.DpConstraints
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offsetPx
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.compose.material.internal.fixedThresholds
import androidx.compose.material.internal.stateDraggable
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Possible states of the [ModalDrawerLayout]
 */
enum class DrawerState {
    /**
     * Constant to indicate the state of the drawer when it's closed
     */
    Closed,
    /**
     * Constant to indicate the state of the drawer when it's opened
     */
    Opened
}

/**
 * Possible states of the [BottomDrawerLayout]
 */
enum class BottomDrawerState {
    /**
     * Constant to indicate the state of the bottom drawer when it's closed
     */
    Closed,
    /**
     * Constant to indicate the state of the bottom drawer when it's opened
     */
    Opened,
    /**
     * Constant to indicate the state of the bottom drawer when it's fully expanded
     */
    Expanded
}

/**
 * Navigation drawers provide access to destinations in your app.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim.
 * They are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * See [BottomDrawerLayout] for a layout that introduces a bottom drawer, suitable when
 * using bottom navigation.
 *
 * @sample androidx.compose.material.samples.ModalDrawerSample
 *
 * @param drawerState state of the drawer
 * @param onStateChange lambda to be invoked when the drawer requests to change its state,
 * e.g. when the drawer is being swiped to the new state or when the scrim is clicked
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerShape shape of the drawer sheet
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow below the
 * drawer sheet
 * @param drawerContent composable that represents content inside the drawer
 * @param bodyContent content of the rest of the UI
 *
 * @throws IllegalStateException when parent has [Float.POSITIVE_INFINITY] width
 */
@Composable
fun ModalDrawerLayout(
    drawerState: DrawerState,
    onStateChange: (DrawerState) -> Unit,
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerConstants.DefaultElevation,
    drawerContent: @Composable () -> Unit,
    bodyContent: @Composable () -> Unit
) {
    WithConstraints(Modifier.fillMaxSize()) {
        // TODO : think about Infinite max bounds case
        if (!constraints.hasBoundedWidth) {
            throw IllegalStateException("Drawer shouldn't have infinite width")
        }
        val dpConstraints = with(DensityAmbient.current) {
            DpConstraints(constraints)
        }
        val minValue = -constraints.maxWidth.toFloat()
        val maxValue = 0f

        val anchors = listOf(minValue to DrawerState.Closed, maxValue to DrawerState.Opened)
        val drawerPosition = state { maxValue }
        val isRtl = LayoutDirectionAmbient.current == LayoutDirection.Rtl
        Stack(Modifier.stateDraggable(
            state = drawerState,
            onStateChange = onStateChange,
            anchorsToState = anchors,
            animationSpec = AnimationSpec,
            orientation = Orientation.Horizontal,
            reverseDirection = isRtl,
            minValue = minValue,
            maxValue = maxValue,
            enabled = gesturesEnabled,
            onNewValue = { drawerPosition.value = it }
        )) {
            Stack {
                bodyContent()
            }
            Scrim(
                opened = drawerState == DrawerState.Opened,
                onClose = { onStateChange(DrawerState.Closed) },
                fraction = { calculateFraction(minValue, maxValue, drawerPosition.value) }
            )
            DrawerContent(
                drawerPosition, dpConstraints, drawerShape, drawerElevation, drawerContent
            )
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
 * See [ModalDrawerLayout] for a layout that introduces a classic from-the-side drawer.
 *
 * @sample androidx.compose.material.samples.BottomDrawerSample
 *
 * @param drawerState state of the drawer
 * @param onStateChange lambda to be invoked when the drawer requests to change its state,
 * e.g. when the drawer is being swiped to the new state or when the scrim is clicked
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerShape shape of the drawer sheet
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow below the
 * drawer sheet
 * @param drawerContent composable that represents content inside the drawer
 * @param bodyContent content of the rest of the UI
 *
 * @throws IllegalStateException when parent has [Float.POSITIVE_INFINITY] height
 */
@Composable
fun BottomDrawerLayout(
    drawerState: BottomDrawerState,
    onStateChange: (BottomDrawerState) -> Unit,
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerConstants.DefaultElevation,
    drawerContent: @Composable () -> Unit,
    bodyContent: @Composable () -> Unit
) {
    WithConstraints(Modifier.fillMaxSize()) {
        // TODO : think about Infinite max bounds case
        if (!constraints.hasBoundedHeight) {
            throw IllegalStateException("Drawer shouldn't have infinite height")
        }
        val dpConstraints = with(DensityAmbient.current) {
            DpConstraints(constraints)
        }
        val minValue = 0f
        val maxValue = constraints.maxHeight.toFloat()

        // TODO: add proper landscape support
        val isLandscape = constraints.maxWidth > constraints.maxHeight
        val openedValue = if (isLandscape) minValue else lerp(
            minValue,
            maxValue,
            BottomDrawerOpenFraction
        )
        val anchors =
            if (isLandscape) {
                listOf(
                    maxValue to BottomDrawerState.Closed,
                    minValue to BottomDrawerState.Opened
                )
            } else {
                listOf(
                    maxValue to BottomDrawerState.Closed,
                    openedValue to BottomDrawerState.Opened,
                    minValue to BottomDrawerState.Expanded
                )
            }
        val drawerPosition = state { maxValue }
        val offset = with(DensityAmbient.current) { BottomDrawerThreshold.toPx() }
        Stack(
            Modifier.stateDraggable(
                state = drawerState,
                onStateChange = onStateChange,
                anchorsToState = anchors,
                thresholds = fixedThresholds(offset),
                animationSpec = AnimationSpec,
                orientation = Orientation.Vertical,
                minValue = minValue,
                maxValue = maxValue,
                enabled = gesturesEnabled,
                onNewValue = { drawerPosition.value = it }
            )
        ) {
            Stack {
                bodyContent()
            }
            Scrim(
                opened = drawerState == BottomDrawerState.Opened,
                onClose = { onStateChange(BottomDrawerState.Closed) },
                fraction = {
                    // as we scroll "from height to 0" , need to reverse fraction
                    1 - calculateFraction(openedValue, maxValue, drawerPosition.value)
                }
            )
            BottomDrawerContent(
                drawerPosition, dpConstraints, drawerShape, drawerElevation, drawerContent
            )
        }
    }
}

/**
 * Object to hold default values for [ModalDrawerLayout] and [BottomDrawerLayout]
 */
object DrawerConstants {

    /**
     * Default Elevation for drawer sheet as specified in material specs
     */
    val DefaultElevation = 16.dp
}

@Composable
private fun DrawerContent(
    xOffset: State<Float>,
    constraints: DpConstraints,
    shape: Shape,
    elevation: Dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier =
        Modifier
            .preferredSizeIn(constraints)
            .offsetPx(x = xOffset)
            .padding(end = VerticalDrawerPadding),
        shape = shape,
        elevation = elevation
    ) {
        Box(Modifier.fillMaxSize(), children = content)
    }
}

@Composable
private fun BottomDrawerContent(
    yOffset: State<Float>,
    constraints: DpConstraints,
    shape: Shape,
    elevation: Dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .preferredSizeIn(constraints)
            .offsetPx(y = yOffset),
        shape = shape,
        elevation = elevation
    ) {
        Box(Modifier.fillMaxSize(), children = content)
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun Scrim(
    opened: Boolean,
    onClose: () -> Unit,
    fraction: () -> Float
) {
    val color = MaterialTheme.colors.onSurface
    val dismissDrawer = if (opened) {
        Modifier.tapGestureFilter { onClose() }
    } else {
        Modifier
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .then(dismissDrawer)
    ) {
        drawRect(color, alpha = fraction() * ScrimDefaultOpacity)
    }
}

private const val ScrimDefaultOpacity = 0.32f
private val VerticalDrawerPadding = 56.dp

private const val DrawerStiffness = 1000f

private val AnimationSpec = SpringSpec<Float>(stiffness = DrawerStiffness)

internal const val BottomDrawerOpenFraction = 0.5f
internal val BottomDrawerThreshold = 56.dp
