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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.internal.DraggableAnchors
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.draggableAnchors
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [Material Design modal bottom sheet](https://m3.material.io/components/bottom-sheets/overview)
 *
 * Modal bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons. Like dialogs, modal bottom sheets appear in front of app content, disabling all other
 * app functionality when they appear, and remaining on screen until confirmed, dismissed, or a
 * required action has been taken.
 *
 * ![Bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.ModalBottomSheetSample
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet
 *   animates to [Hidden].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param sheetState The state of the bottom sheet.
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param sheetGesturesEnabled Whether the bottom sheet can be interacted with by gestures.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The color used for the background of this bottom sheet
 * @param contentColor The preferred color for content inside this bottom sheet. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param scrimColor Color of the scrim that obscures content when the bottom sheet is open.
 * @param dragHandle Optional visual marker to swipe the bottom sheet.
 * @param contentWindowInsets callback which provides window insets to be passed to the bottom sheet
 *   content via [Modifier.windowInsetsPadding]. [ModalBottomSheet] will pre-emptively consume top
 *   insets based on it's current offset. This keeps content outside of the expected window insets
 *   at any position.
 * @param properties [ModalBottomSheetProperties] for further customization of this modal bottom
 *   sheet's window behavior.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
@ExperimentalMaterial3Api
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    // TODO Load the motionScheme tokens from the component tokens file
    val anchoredDraggableMotion: FiniteAnimationSpec<Float> =
        MotionSchemeKeyTokens.DefaultSpatial.value()
    val showMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    val hideMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()

    SideEffect {
        sheetState.showMotionSpec = showMotion
        sheetState.hideMotionSpec = hideMotion
        sheetState.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }
    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        if (sheetState.anchoredDraggableState.confirmValueChange(Hidden)) {
            scope
                .launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    }
                }
        }
    }
    val settleToDismiss: (velocity: Float) -> Unit = {
        scope
            .launch { sheetState.settle(it) }
            .invokeOnCompletion { if (!sheetState.isVisible) onDismissRequest() }
    }

    val predictiveBackProgress = remember { Animatable(initialValue = 0f) }

    ModalBottomSheetDialog(
        properties = properties,
        contentColor = contentColor,
        onDismissRequest = {
            if (sheetState.currentValue == Expanded && sheetState.hasPartiallyExpandedState) {
                // Smoothly animate away predictive back transformations since we are not fully
                // dismissing. We don't need to do this in the else below because we want to
                // preserve the predictive back transformations (scale) during the hide animation.
                scope.launch { predictiveBackProgress.animateTo(0f) }
                scope.launch { sheetState.partialExpand() }
            } else { // Is expanded without collapsed state or is collapsed.
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
            }
        },
        predictiveBackProgress = predictiveBackProgress,
    ) {
        Box(modifier = Modifier.fillMaxSize().imePadding().semantics { isTraversalGroup = true }) {
            Scrim(
                color = scrimColor,
                onDismissRequest = animateToDismiss,
                visible = sheetState.targetValue != Hidden,
                dismissEnabled = properties.shouldDismissOnClickOutside,
            )
            ModalBottomSheetContent(
                predictiveBackProgress,
                scope,
                animateToDismiss,
                settleToDismiss,
                modifier,
                sheetState,
                sheetMaxWidth,
                sheetGesturesEnabled,
                shape,
                containerColor,
                contentColor,
                tonalElevation,
                dragHandle,
                contentWindowInsets,
                content,
            )
        }
    }
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }
}

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Maintained for Binary compatibility. Use overload with sheetGesturesEnabled param.",
)
@Composable
@ExperimentalMaterial3Api
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit,
) =
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        sheetGesturesEnabled = true,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
        content = content,
    )

@Composable
@ExperimentalMaterial3Api
internal fun BoxScope.ModalBottomSheetContent(
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    scope: CoroutineScope,
    animateToDismiss: () -> Unit,
    settleToDismiss: (velocity: Float) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit,
) {
    val bottomSheetPaneTitle = getString(string = Strings.BottomSheetPaneTitle)

    Surface(
        modifier =
            modifier
                .align(Alignment.TopCenter)
                .widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .then(
                    if (sheetGesturesEnabled)
                        Modifier.nestedScroll(
                            remember(sheetState) {
                                ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                    sheetState = sheetState,
                                    orientation = Orientation.Vertical,
                                    onFling = settleToDismiss,
                                )
                            }
                        )
                    else Modifier
                )
                .draggableAnchors(sheetState.anchoredDraggableState, Orientation.Vertical) {
                    sheetSize,
                    constraints ->
                    val fullHeight = constraints.maxHeight.toFloat()
                    val newAnchors = DraggableAnchors {
                        Hidden at fullHeight
                        if (
                            sheetSize.height > (fullHeight / 2) && !sheetState.skipPartiallyExpanded
                        ) {
                            PartiallyExpanded at fullHeight / 2f
                        }
                        if (sheetSize.height != 0) {
                            Expanded at max(0f, fullHeight - sheetSize.height)
                        }
                    }
                    val newTarget =
                        when (sheetState.anchoredDraggableState.targetValue) {
                            Hidden -> Hidden
                            PartiallyExpanded -> {
                                val hasPartiallyExpandedState =
                                    newAnchors.hasAnchorFor(PartiallyExpanded)
                                val newTarget =
                                    if (hasPartiallyExpandedState) PartiallyExpanded
                                    else if (newAnchors.hasAnchorFor(Expanded)) Expanded else Hidden
                                newTarget
                            }
                            Expanded -> {
                                if (newAnchors.hasAnchorFor(Expanded)) Expanded else Hidden
                            }
                        }
                    return@draggableAnchors newAnchors to newTarget
                }
                .draggable(
                    state = sheetState.anchoredDraggableState.draggableState,
                    orientation = Orientation.Vertical,
                    enabled = sheetGesturesEnabled && sheetState.isVisible,
                    startDragImmediately = sheetState.anchoredDraggableState.isAnimationRunning,
                    onDragStopped = { settleToDismiss(it) },
                )
                .semantics {
                    paneTitle = bottomSheetPaneTitle
                    traversalIndex = 0f
                }
                .consumeWindowInsets(WindowInsets(top = sheetState.offset.toInt().coerceAtLeast(0)))
                .graphicsLayer {
                    val sheetOffset = sheetState.anchoredDraggableState.offset
                    val sheetHeight = size.height
                    if (!sheetOffset.isNaN() && !sheetHeight.isNaN() && sheetHeight != 0f) {
                        val progress = predictiveBackProgress.value
                        scaleX = calculatePredictiveBackScaleX(progress)
                        scaleY = calculatePredictiveBackScaleY(progress)
                        transformOrigin =
                            TransformOrigin(0.5f, (sheetOffset + sheetHeight) / sheetHeight)
                    }
                }
                // Scale up the Surface vertically in case the sheet's offset overflows below the
                // min anchor. This is done to avoid showing a gap when the sheet opens and bounces
                // when it's applied with a bouncy motion. Note that the content inside the Surface
                // is scaled back down to maintain its aspect ratio (see below).
                .verticalScaleUp(sheetState),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(contentWindowInsets())
                .graphicsLayer {
                    val progress = predictiveBackProgress.value
                    val predictiveBackScaleX = calculatePredictiveBackScaleX(progress)
                    val predictiveBackScaleY = calculatePredictiveBackScaleY(progress)

                    // Preserve the original aspect ratio and alignment of the child content.
                    scaleY =
                        if (predictiveBackScaleY != 0f) predictiveBackScaleX / predictiveBackScaleY
                        else 1f
                    transformOrigin = PredictiveBackChildTransformOrigin
                }
                // Scale the content down in case the sheet offset overflows below the min anchor.
                // The wrapping Surface is scaled up, so this is done to maintain the content's
                // aspect ratio.
                .verticalScaleDown(sheetState)
        ) {
            if (dragHandle != null) {
                val collapseActionLabel = getString(Strings.BottomSheetPartialExpandDescription)
                val dismissActionLabel = getString(Strings.BottomSheetDismissDescription)
                val expandActionLabel = getString(Strings.BottomSheetExpandDescription)
                DragHandleWithTooltip {
                    Box(
                        modifier =
                            Modifier.clickable {
                                    when (sheetState.currentValue) {
                                        Expanded -> animateToDismiss()
                                        PartiallyExpanded -> scope.launch { sheetState.expand() }
                                        else -> scope.launch { sheetState.show() }
                                    }
                                }
                                .semantics(mergeDescendants = true) {
                                    // Provides semantics to interact with the bottomsheet based on
                                    // its current value.
                                    if (sheetGesturesEnabled) {
                                        with(sheetState) {
                                            dismiss(dismissActionLabel) {
                                                animateToDismiss()
                                                true
                                            }
                                            if (currentValue == PartiallyExpanded) {
                                                expand(expandActionLabel) {
                                                    if (
                                                        anchoredDraggableState.confirmValueChange(
                                                            Expanded
                                                        )
                                                    ) {
                                                        scope.launch { sheetState.expand() }
                                                    }
                                                    true
                                                }
                                            } else if (hasPartiallyExpandedState) {
                                                collapse(collapseActionLabel) {
                                                    if (
                                                        anchoredDraggableState.confirmValueChange(
                                                            PartiallyExpanded
                                                        )
                                                    ) {
                                                        scope.launch { partialExpand() }
                                                    }
                                                    true
                                                }
                                            }
                                        }
                                    }
                                }
                    ) {
                        dragHandle()
                    }
                }
            }
            content()
        }
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleX(progress: Float): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleXDistance.toPx(), width), progress) / width
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(progress: Float): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleYDistance.toPx(), height), progress) / height
    }
}

/**
 * Properties used to customize the behavior of a [ModalBottomSheet].
 *
 * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing the
 *   back button. If true, pressing the back button will call onDismissRequest.
 * @param shouldDismissOnClickOutside Whether the modal bottom sheet can be dismissed by clicking on
 *   the scrim.
 */
@Immutable
@ExperimentalMaterial3Api
expect class ModalBottomSheetProperties(
    shouldDismissOnBackPress: Boolean = true,
    shouldDismissOnClickOutside: Boolean = true,
) {
    val shouldDismissOnBackPress: Boolean
    val shouldDismissOnClickOutside: Boolean

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "Replaced with additional shouldDismissOnClickOutside param constructor.",
    )
    constructor(shouldDismissOnBackPress: Boolean)
}

/** Default values for [ModalBottomSheet] */
@Immutable
@ExperimentalMaterial3Api
expect object ModalBottomSheetDefaults {

    /** Properties used to customize the behavior of a [ModalBottomSheet]. */
    val properties: ModalBottomSheetProperties
}

/**
 * Create and [remember] a [SheetState] for [ModalBottomSheet].
 *
 * @param skipPartiallyExpanded Whether the partially expanded state, if the sheet is tall enough,
 *   should be skipped. If true, the sheet will always expand to the [Expanded] state and move to
 *   the [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
) =
    rememberSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = confirmValueChange,
        initialValue = Hidden,
    )

@Composable
private fun Scrim(
    color: Color,
    onDismissRequest: () -> Unit,
    visible: Boolean,
    dismissEnabled: Boolean,
) {
    // TODO Load the motionScheme tokens from the component tokens file
    if (color.isSpecified) {
        val alpha by
            animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
            )
        val closeSheet = getString(Strings.CloseSheet)
        val dismissSheet =
            if (dismissEnabled) {
                Modifier.pointerInput(onDismissRequest) { detectTapGestures { onDismissRequest() } }
                    .semantics(mergeDescendants = true) {
                        traversalIndex = 1f
                        contentDescription = closeSheet
                        onClick {
                            onDismissRequest()
                            true
                        }
                    }
            } else {
                Modifier
            }
        Canvas(Modifier.fillMaxSize().then(dismissSheet)) {
            drawRect(color = color, alpha = alpha.coerceIn(0f, 1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal expect fun ModalBottomSheetDialog(
    onDismissRequest: () -> Unit,
    contentColor: Color,
    properties: ModalBottomSheetProperties,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit,
)

private val PredictiveBackMaxScaleXDistance = 48.dp
private val PredictiveBackMaxScaleYDistance = 24.dp
private val PredictiveBackChildTransformOrigin = TransformOrigin(0.5f, 0f)
