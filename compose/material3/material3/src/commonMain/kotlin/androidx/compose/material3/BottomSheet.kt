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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.internal.PredictiveBack
import androidx.compose.material3.internal.PredictiveBackHandler
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.draggableAnchors
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * [Material Design bottom sheet](https://m3.material.io/components/bottom-sheets/overview)
 *
 * Modal bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons.
 *
 * This component provides the visual surface and gesture behavior for a bottom sheet. Crucially, it
 * renders **directly in the composition hierarchy** (the main UI tree), unlike [ModalBottomSheet]
 * which launches a separate [androidx.compose.ui.window.Dialog] window.
 *
 * Because this component exists in the main UI tree:
 * - It is drawn at the Z-index determined by its placement in the layout (e.g. inside a [Box]).
 * - It does not automatically provide a scrim or block interaction with the rest of the screen.
 * - It shares the same lifecycle and input handling as its parent composables.
 *
 * Use this component when building custom sheet experiences where a
 * [androidx.compose.ui.window.Dialog] window is not desired, or when a custom
 * [androidx.compose.ui.window.Dialog] is needed.
 *
 * For a modal bottom sheet that handles the Dialog window, scrim, and focus management
 * automatically, use [ModalBottomSheet].
 *
 * For a persistent bottom sheet that is structurally integrated into a screen layout, use
 * [BottomSheetScaffold].
 *
 * The following sample shows how the component can be used alongside your UI.
 *
 * @sample androidx.compose.material3.samples.ManualModalBottomSheetSample
 * @param modifier The modifier to be applied to the bottom sheet.
 * @param state The state object managing the sheet's value and offsets.
 * @param onDismissRequest Optional callback invoked when the sheet is swiped to [Hidden].
 * @param maxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param gesturesEnabled Whether gestures are enabled.
 * @param backHandlerEnabled Whether dismissing via back press and predictive back behavior is
 *   enabled
 * @param dragHandle Optional visual marker to indicate the sheet is draggable.
 * @param contentWindowInsets Window insets to be applied to the content.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The background color of the bottom sheet.
 * @param contentColor The preferred content color.
 * @param tonalElevation The tonal elevation of the bottom sheet.
 * @param shadowElevation The shadow elevation of the bottom sheet.
 * @param content The content of the sheet.
 */
@Composable
@ExperimentalMaterial3Api
fun BottomSheet(
    modifier: Modifier = Modifier,
    state: SheetState = rememberBottomSheetState(initialValue = Hidden),
    onDismissRequest: () -> Unit = {},
    maxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    gesturesEnabled: Boolean = true,
    backHandlerEnabled: Boolean = true,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = {
        BottomSheetDefaults.standardWindowInsets
    },
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    shadowElevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val showMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val hideMotion = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val anchoredDraggableMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    SideEffect {
        state.showMotionSpec = showMotion
        state.hideMotionSpec = hideMotion
        state.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }

    val predictiveBackProgress = remember { Animatable(initialValue = 0f) }
    val scope = rememberCoroutineScope()
    val settleToDismiss: () -> Unit = {
        if (state.currentValue == Expanded && state.hasPartiallyExpandedState) {
            scope.launch { state.partialExpand() }
            scope.launch { predictiveBackProgress.animateTo(0f) }
        } else {
            scope
                .launch { state.hide() }
                .invokeOnCompletion { if (!state.isVisible) onDismissRequest() }
        }
    }

    PredictiveBackHandler(enabled = backHandlerEnabled && state.isVisible) { progress ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress.snapTo(PredictiveBack.transform(backEvent.progress))
            }
            settleToDismiss()
        } catch (e: CancellationException) {
            predictiveBackProgress.animateTo(0f)
        }
    }
    BottomSheetImpl(
        predictiveBackProgress = predictiveBackProgress.value,
        modifier = modifier,
        state = state,
        onDismissRequest = onDismissRequest,
        maxWidth = maxWidth,
        gesturesEnabled = gesturesEnabled,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

/** Refactored content implementation to enable predictive back testing. */
@Composable
@ExperimentalMaterial3Api
internal fun BottomSheetImpl(
    predictiveBackProgress: Float,
    modifier: Modifier = Modifier,
    state: SheetState = rememberBottomSheetState(initialValue = Hidden),
    onDismissRequest: () -> Unit = {},
    maxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    gesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    shadowElevation: Dp = 0.dp,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = {
        BottomSheetDefaults.standardWindowInsets
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    val bottomSheetPaneTitle = getString(string = Strings.BottomSheetPaneTitle)
    val viewConfiguration = LocalViewConfiguration.current
    val spatialFlingSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val density = LocalDensity.current

    val anchoredDraggableFlingBehavior =
        AnchoredDraggableDefaults.flingBehavior(
            state = state.anchoredDraggableState,
            positionalThreshold = { _ -> state.positionalThreshold.invoke() },
            animationSpec = spatialFlingSpec,
        )

    val modalBottomSheetFlingBehavior =
        remember(anchoredDraggableFlingBehavior, state, viewConfiguration, density) {
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    val maxSystemVelocity = viewConfiguration.maximumFlingVelocity
                    var safeVelocity =
                        initialVelocity.coerceIn(-maxSystemVelocity, maxSystemVelocity)

                    if (
                        safeVelocity > 0f &&
                            state.anchoredDraggableState.anchors.hasPositionFor(Hidden)
                    ) {
                        val hiddenAnchor = state.anchoredDraggableState.anchors.positionOf(Hidden)
                        val currentOffset = state.requireOffset()
                        val distanceToFloor = max(0f, hiddenAnchor - currentOffset)

                        val dampeningZone =
                            with(density) { BottomSheetDefaults.BoundaryDampeningZone.toPx() }
                        if (distanceToFloor < dampeningZone) {
                            val factor = distanceToFloor / dampeningZone
                            safeVelocity *= (factor * factor)

                            // Ensure previously valid velocities (above velocityThresholdPx) shrink
                            // at most to velocityThresholdPx to maintain a valid fling.
                            val velocityThresholdPx =
                                with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                            if (initialVelocity >= velocityThresholdPx) {
                                safeVelocity = max(safeVelocity, velocityThresholdPx)
                            }
                        }
                    }

                    var remainingVelocity = 0f
                    try {
                        remainingVelocity =
                            with(anchoredDraggableFlingBehavior) { performFling(safeVelocity) }
                    } finally {
                        if (!state.isVisible) onDismissRequest()
                    }
                    return remainingVelocity
                }
            }
        }

    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        if (state.confirmValueChange(Hidden)) {
            scope
                .launch { state.hide() }
                .invokeOnCompletion {
                    if (!state.isVisible) {
                        onDismissRequest()
                    }
                }
        }
    }

    Surface(
        modifier =
            modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .then(
                    if (gesturesEnabled)
                        Modifier.nestedScroll(
                            remember(state) {
                                ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                    sheetState = state,
                                    orientation = Orientation.Vertical,
                                    flingBehavior = modalBottomSheetFlingBehavior,
                                )
                            }
                        )
                    else Modifier
                )
                .draggableAnchors(state.anchoredDraggableState, Orientation.Vertical) {
                    sheetSize,
                    constraints ->
                    val fullHeight = constraints.maxHeight.toFloat()
                    val newAnchors = DraggableAnchors {
                        Hidden at fullHeight
                        if (sheetSize.height > (fullHeight / 2) && !state.skipPartiallyExpanded) {
                            PartiallyExpanded at fullHeight / 2f
                        }
                        if (sheetSize.height != 0) {
                            Expanded at max(0f, fullHeight - sheetSize.height)
                        }
                    }
                    val newTarget =
                        when (state.targetValue) {
                            Hidden -> Hidden
                            PartiallyExpanded -> {
                                when {
                                    newAnchors.hasPositionFor(PartiallyExpanded) ->
                                        PartiallyExpanded
                                    newAnchors.hasPositionFor(Expanded) -> Expanded
                                    else -> Hidden
                                }
                            }

                            Expanded -> {
                                if (newAnchors.hasPositionFor(Expanded)) Expanded else Hidden
                            }
                        }
                    return@draggableAnchors newAnchors to newTarget
                }
                .anchoredDraggable(
                    state = state.anchoredDraggableState,
                    orientation = Orientation.Vertical,
                    enabled = gesturesEnabled && state.currentValue != Hidden,
                    flingBehavior = modalBottomSheetFlingBehavior,
                )
                .semantics {
                    paneTitle = bottomSheetPaneTitle
                    traversalIndex = 0f
                }
                .sheetPredictiveBackScaling(state, predictiveBackProgress)
                // Scale up the Surface vertically in case the sheet's offset overflows below
                // the min anchor. This is done to avoid showing a gap when the sheet opens and
                // bounces when it's applied with a bouncy motion. Note that the content inside
                // the Surface is scaled back down to maintain its aspect ratio (see below).
                .verticalScaleUp(state),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(contentWindowInsets())
                .contentPredictiveBackScaling(predictiveBackProgress)
                // Scale the content down in case the sheet offset overflows below the min
                // anchor. The wrapping Surface is scaled up, so this is done to maintain
                // the content's aspect ratio.
                .verticalScaleDown(state)
        ) {
            if (dragHandle != null) {
                val collapseActionLabel = getString(Strings.BottomSheetPartialExpandDescription)
                val dismissActionLabel = getString(Strings.BottomSheetDismissDescription)
                val expandActionLabel = getString(Strings.BottomSheetExpandDescription)
                DragHandleWithTooltip(
                    modifier =
                        Modifier.clickable {
                                when (state.currentValue) {
                                    Expanded -> animateToDismiss()
                                    PartiallyExpanded -> scope.launch { state.expand() }
                                    else -> scope.launch { state.show() }
                                }
                            }
                            .semantics(mergeDescendants = true) {
                                // Provides semantics to interact with the bottomsheet based on
                                // its current value.
                                if (gesturesEnabled) {
                                    with(state) {
                                        dismiss(dismissActionLabel) {
                                            animateToDismiss()
                                            true
                                        }
                                        if (currentValue == PartiallyExpanded) {
                                            expand(expandActionLabel) {
                                                if (confirmValueChange(Expanded)) {
                                                    scope.launch { state.expand() }
                                                }
                                                true
                                            }
                                        } else if (hasPartiallyExpandedState) {
                                            collapse(collapseActionLabel) {
                                                if (confirmValueChange(PartiallyExpanded)) {
                                                    scope.launch { partialExpand() }
                                                }
                                                true
                                            }
                                        }
                                    }
                                }
                            },
                    content = dragHandle,
                )
            }
            content()
        }
    }
}

internal fun GraphicsLayerScope.calculateSheetPredictiveBackScaleX(progress: Float): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleXDistance.toPx(), width), progress) / width
    }
}

internal fun GraphicsLayerScope.calculateSheetPredictiveBackScaleY(progress: Float): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleYDistance.toPx(), height), progress) / height
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.sheetPredictiveBackScaling(
    sheetState: SheetState,
    predictiveBackProgress: Float,
) = graphicsLayer {
    val sheetOffset = sheetState.anchoredDraggableState.offset
    val sheetHeight = size.height
    if (!sheetOffset.isNaN() && !sheetHeight.isNaN() && sheetHeight != 0f) {
        scaleX = calculateSheetPredictiveBackScaleX(predictiveBackProgress)
        scaleY = calculateSheetPredictiveBackScaleY(predictiveBackProgress)
        transformOrigin = TransformOrigin(0.5f, (sheetOffset + sheetHeight) / sheetHeight)
    }
}

internal fun Modifier.contentPredictiveBackScaling(predictiveBackProgress: Float) = graphicsLayer {
    val predictiveBackScaleX = calculateSheetPredictiveBackScaleX(predictiveBackProgress)
    val predictiveBackScaleY = calculateSheetPredictiveBackScaleY(predictiveBackProgress)
    scaleY = if (predictiveBackScaleY != 0f) predictiveBackScaleX / predictiveBackScaleY else 1f
    transformOrigin = PredictiveBackChildTransformOrigin
}

private val PredictiveBackMaxScaleXDistance = 48.dp
private val PredictiveBackMaxScaleYDistance = 24.dp
internal val PredictiveBackChildTransformOrigin = TransformOrigin(0.5f, 0f)
