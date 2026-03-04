/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.material3.tokens.SheetBottomTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.jvm.JvmName
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
 * which launches a separate [Dialog] window.
 *
 * Because this component exists in the main UI tree:
 * - It is drawn at the Z-index determined by its placement in the layout (e.g. inside a [Box]).
 * - It does not automatically provide a scrim or block interaction with the rest of the screen.
 * - It shares the same lifecycle and input handling as its parent composables.
 *
 * Use this component when building custom sheet experiences where a [Dialog] window is not desired,
 * or when a custom [Dialog] is needed.
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
    state: SheetState = rememberModalBottomSheetState(),
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
    state: SheetState = rememberModalBottomSheetState(),
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
    val spatialFlingSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val viewConfiguration = LocalViewConfiguration.current

    val anchoredDraggableFlingBehavior =
        AnchoredDraggableDefaults.flingBehavior(
            state = state.anchoredDraggableState,
            positionalThreshold = { _ -> state.positionalThreshold.invoke() },
            animationSpec = spatialFlingSpec,
        )
    val modalBottomSheetFlingBehavior =
        remember(anchoredDraggableFlingBehavior) {
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    // We clamp this to the device's physical maximum (usually ~8,000 px/s)
                    // to prevent the math from breaking bounds. This prevents potential overshoot
                    // bugs caused by migrating the sheet from stiff tween animations.
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

                        // Apply a friction zone to the bottom 400 pixels
                        val dampeningZone = 400f
                        if (distanceToFloor < dampeningZone) {
                            val factor = distanceToFloor / dampeningZone
                            safeVelocity *= factor
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
                        when (state.anchoredDraggableState.targetValue) {
                            Hidden -> Hidden
                            PartiallyExpanded -> {
                                val hasPartiallyExpandedState =
                                    newAnchors.hasPositionFor(PartiallyExpanded)
                                val newTarget =
                                    if (hasPartiallyExpandedState) PartiallyExpanded
                                    else if (newAnchors.hasPositionFor(Expanded)) Expanded
                                    else Hidden
                                newTarget
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

/**
 * State of a sheet composable, such as [ModalBottomSheet]
 *
 * Contains states relating to its swipe position as well as animations between state values.
 *
 * @param skipPartiallyExpanded Whether the partially expanded state, if the sheet is large enough,
 *   should be skipped. If true, the sheet will always expand to the [Expanded] state and move to
 *   the [Hidden] state if available when hiding the sheet, either programmatically or by user
 *   interaction.
 * @param positionalThreshold The positional threshold, in px, to be used when calculating the
 *   target state while a drag is in progress and when settling after the drag ends. This is the
 *   distance from the start of a transition. It will be, depending on the direction of the
 *   interaction, added or subtracted from/to the origin offset. It should always be a positive
 *   value.
 * @param velocityThreshold The velocity threshold (in px per second) that the end velocity has to
 *   exceed in order to animate to the next state, even if the [positionalThreshold] has not been
 *   reached.
 * @param initialValue The initial value of the state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param skipHiddenState Whether the hidden state should be skipped. If true, the sheet will always
 *   expand to the [Expanded] state and move to the [PartiallyExpanded] if available, either
 *   programmatically or by user interaction.
 */
@Stable
@ExperimentalMaterial3Api
class SheetState(
    internal val skipPartiallyExpanded: Boolean,
    internal val positionalThreshold: () -> Float,
    internal val velocityThreshold: () -> Float,
    initialValue: SheetValue = Hidden,
    internal val confirmValueChange: (SheetValue) -> Boolean = { true },
    internal val skipHiddenState: Boolean = false,
) {

    init {
        if (skipPartiallyExpanded) {
            require(initialValue != PartiallyExpanded) {
                "The initial value must not be set to PartiallyExpanded if skipPartiallyExpanded " +
                    "is set to true."
            }
        }
        if (skipHiddenState) {
            require(initialValue != Hidden) {
                "The initial value must not be set to Hidden if skipHiddenState is set to true."
            }
        }
    }

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the state the bottom sheet is
     * currently in. If a swipe or an animation is in progress, this corresponds the state the sheet
     * was in before the swipe or animation started.
     */
    val currentValue: SheetValue
        // Note: Current Value is mapping to the newly introduced settled value for roughly
        // analogous behavior to internal fork. anchoredDraggableState.currentValue now maps to the
        // value the touch target is closest to, regardless of release/settling.
        get() = anchoredDraggableState.settledValue

    // TODO(b/477969920): Remove forked targetValue logic when foundation dependencies are updated.
    /**
     * The target value of the bottom sheet state.
     *
     * If a swipe is in progress, this is the value that the sheet would animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: SheetValue by derivedStateOf {
        // AnchoredDraggableState does not expose the dragTarget, but isAnimationRunning returns
        // whether AnchoredDraggableState.dragTarget is null. If it's not, we can use the
        // targetValue; otherwise we apply the calculation fix.
        if (isAnimationRunning) {
            anchoredDraggableState.targetValue
        } else {
            calculateTargetValueWithFix(offset)
        }
    }

    private fun calculateTargetValueWithFix(currentOffset: Float): SheetValue {
        return if (!currentOffset.isNaN()) {
            // DraggableAnchors allows multiple anchors with the same offsets. If the offset is
            // already equal to the currentValue's offset, this anchor gets priority.
            val currentValueOffset = anchoredDraggableState.anchors.positionOf(currentValue)
            if (currentValueOffset.isNaN() || currentOffset == currentValueOffset) {
                currentValue
            } else {
                anchoredDraggableState.anchors.closestAnchor(currentOffset) ?: currentValue
            }
        } else currentValue
    }

    /** Whether the modal bottom sheet is visible. */
    val isVisible: Boolean
        get() = anchoredDraggableState.currentValue != Hidden

    /**
     * Whether an expanding or collapsing sheet animation is currently in progress.
     *
     * See [expand], [partialExpand], [show] or [hide] for more information.
     */
    val isAnimationRunning: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    /**
     * Require the current offset (in pixels) of the bottom sheet.
     *
     * The offset will be initialized during the first measurement phase of the provided sheet
     * content.
     *
     * These are the phases: Composition { -> Effects } -> Layout { Measurement -> Placement } ->
     * Drawing
     *
     * During the first composition, an [IllegalStateException] is thrown. In subsequent
     * compositions, the offset will be derived from the anchors of the previous pass. Always prefer
     * accessing the offset from a LaunchedEffect as it will be scheduled to be executed the next
     * frame, after layout.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /** Whether the sheet has an expanded state defined. */
    val hasExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasPositionFor(Expanded)

    /** Whether the modal bottom sheet has a partially expanded state defined. */
    val hasPartiallyExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasPositionFor(PartiallyExpanded)

    /**
     * If [confirmValueChange] returns true, fully expand the bottom sheet with animation and
     * suspend until it is fully expanded or animation has been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun expand() {
        if (confirmValueChange(Expanded)) animateTo(Expanded, showMotionSpec)
    }

    /**
     * If [confirmValueChange] returns true, animate the bottom sheet and suspend until it is
     * partially expanded or animation has been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     * @throws [IllegalStateException] if [skipPartiallyExpanded] is set to true
     */
    suspend fun partialExpand() {
        check(!skipPartiallyExpanded) {
            "Attempted to animate to partial expanded when skipPartiallyExpanded was enabled. Set" +
                " skipPartiallyExpanded to false to use this function."
        }
        if (confirmValueChange(PartiallyExpanded)) animateTo(PartiallyExpanded, hideMotionSpec)
    }

    /**
     * If [confirmValueChange] returns true, expand the bottom sheet with animation and suspend
     * until it is [PartiallyExpanded] if defined, else [Expanded].
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun show() {
        val targetValue =
            when {
                hasPartiallyExpandedState -> PartiallyExpanded
                else -> Expanded
            }
        if (confirmValueChange(targetValue)) animateTo(targetValue, showMotionSpec)
    }

    /**
     * If [confirmValueChange] returns true, hide the bottom sheet with animation and suspend until
     * it is fully hidden or animation has been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun hide() {
        check(!skipHiddenState) {
            "Attempted to animate to hidden when skipHiddenState was enabled. Set skipHiddenState" +
                " to false to use this function."
        }
        if (confirmValueChange(Hidden)) animateTo(Hidden, hideMotionSpec)
    }

    /**
     * Animate to a [targetValue]. If the [targetValue] is not in the set of anchors, the
     * [currentValue] will be updated to the [targetValue] without updating the offset.
     *
     * @param targetValue The target value of the animation
     * @param animationSpec an [AnimationSpec]
     * @throws CancellationException if the interaction interrupted by another interaction like a
     *   gesture interaction or another programmatic interaction like a [animateTo] or [snapTo]
     *   call.
     */
    internal suspend fun animateTo(
        targetValue: SheetValue,
        animationSpec: FiniteAnimationSpec<Float>,
    ) = anchoredDraggableState.animateTo(targetValue, animationSpec)

    /**
     * Snap to a [targetValue] without any animation.
     *
     * @param targetValue The target value of the animation
     * @throws CancellationException if the interaction interrupted by another interaction like a
     *   gesture interaction or another programmatic interaction like a [animateTo] or [snapTo]
     *   call.
     */
    internal suspend fun snapTo(targetValue: SheetValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    internal var anchoredDraggableMotionSpec: AnimationSpec<Float> = BottomSheetAnimationSpec

    @Suppress("Deprecation")
    internal var anchoredDraggableState: AnchoredDraggableState<SheetValue> =
        AnchoredDraggableState(initialValue = initialValue, confirmValueChange = confirmValueChange)

    /**
     * Calculate the new offset for a [delta] to ensure it is coerced in the bounds
     *
     * @param delta The delta to be added to the [offset]
     * @return The coerced offset
     */
    internal fun newOffsetForDelta(delta: Float) =
        ((if (offset.isNaN()) 0f else offset) + delta).coerceIn(
            anchoredDraggableState.anchors.minPosition(),
            anchoredDraggableState.anchors.maxPosition(),
        )

    internal suspend fun anchoredDrag(flingBehavior: FlingBehavior, initialVelocity: Float): Float {
        var consumedVelocity = 0f
        anchoredDraggableState.anchoredDrag {
            val scrollScope =
                object : ScrollScope {
                    override fun scrollBy(pixels: Float): Float {
                        val newOffset = newOffsetForDelta(pixels)
                        val consumed = newOffset - offset
                        dragTo(newOffset)
                        return consumed
                    }
                }
            consumedVelocity = with(flingBehavior) { scrollScope.performFling(initialVelocity) }
        }
        return consumedVelocity
    }

    internal val offset: Float
        get() = anchoredDraggableState.offset

    internal var showMotionSpec: FiniteAnimationSpec<Float> = snap()

    internal var hideMotionSpec: FiniteAnimationSpec<Float> = snap()

    companion object {
        /** The default [Saver] implementation for [SheetState]. */
        fun Saver(
            skipPartiallyExpanded: Boolean,
            positionalThreshold: () -> Float,
            velocityThreshold: () -> Float,
            confirmValueChange: (SheetValue) -> Boolean,
            skipHiddenState: Boolean,
        ) =
            Saver<SheetState, SheetValue>(
                save = { it.currentValue },
                restore = { savedValue ->
                    val newValue =
                        if (skipPartiallyExpanded && savedValue == PartiallyExpanded) {
                            Expanded
                        } else {
                            savedValue
                        }
                    SheetState(
                        skipPartiallyExpanded,
                        positionalThreshold,
                        velocityThreshold,
                        newValue,
                        confirmValueChange,
                        skipHiddenState,
                    )
                },
            )

        @Deprecated(
            level = DeprecationLevel.HIDDEN,
            message = "Maintained for binary compatibility.",
        )
        fun Saver(
            skipPartiallyExpanded: Boolean,
            confirmValueChange: (SheetValue) -> Boolean,
            density: Density,
            skipHiddenState: Boolean,
        ) =
            Saver(
                skipPartiallyExpanded = skipPartiallyExpanded,
                confirmValueChange = confirmValueChange,
                skipHiddenState = skipHiddenState,
                positionalThreshold = {
                    with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                },
                velocityThreshold = {
                    with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                },
            )
    }

    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Maintained for binary compatibility.")
    constructor(
        skipPartiallyExpanded: Boolean,
        density: Density,
        initialValue: SheetValue = Hidden,
        confirmValueChange: (SheetValue) -> Boolean = { true },
        skipHiddenState: Boolean = false,
    ) : this(
        skipPartiallyExpanded = skipPartiallyExpanded,
        positionalThreshold = { with(density) { BottomSheetDefaults.PositionalThreshold.toPx() } },
        velocityThreshold = { with(density) { BottomSheetDefaults.VelocityThreshold.toPx() } },
        initialValue = initialValue,
        confirmValueChange = confirmValueChange,
        skipHiddenState = skipHiddenState,
    )
}

/** Possible values of [SheetState]. */
@ExperimentalMaterial3Api
enum class SheetValue {
    /** The sheet is not visible. */
    Hidden,

    /** The sheet is visible at full height. */
    Expanded,

    /** The sheet is partially visible. */
    PartiallyExpanded,
}

/** Contains the default values used by [ModalBottomSheet] and [BottomSheetScaffold]. */
@Stable
@ExperimentalMaterial3Api
object BottomSheetDefaults {
    /** The default shape for bottom sheets in a [Hidden] state. */
    val HiddenShape: Shape
        @Composable get() = SheetBottomTokens.DockedMinimizedContainerShape.value

    /** The default shape for a bottom sheets in [PartiallyExpanded] and [Expanded] states. */
    val ExpandedShape: Shape
        @Composable get() = SheetBottomTokens.DockedContainerShape.value

    /** The default container color for a bottom sheet. */
    val ContainerColor: Color
        @Composable get() = SheetBottomTokens.DockedContainerColor.value

    /** The default elevation for a bottom sheet. */
    val Elevation = SheetBottomTokens.DockedModalContainerElevation

    /** The default color of the scrim overlay for background content. */
    val ScrimColor: Color
        @Composable get() = ScrimTokens.ContainerColor.value.copy(ScrimTokens.ContainerOpacity)

    /** The default peek height used by [BottomSheetScaffold]. */
    val SheetPeekHeight = 56.dp

    /** The default max width used by [ModalBottomSheet] and [BottomSheetScaffold] */
    val SheetMaxWidth = 640.dp

    /** Default insets to be used and consumed by the [ModalBottomSheet]'s content. */
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Renamed as modalWindowInsets.",
        replaceWith = ReplaceWith("modalWindowInsets"),
    )
    val windowInsets: WindowInsets
        @Composable
        get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Top)

    /** Default insets to be used and consumed by the [BottomSheet]'s content. */
    val standardWindowInsets: WindowInsets
        @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)

    /** Default insets to be used and consumed by the [ModalBottomSheet]'s content. */
    val modalWindowInsets: WindowInsets
        @Composable
        get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Top)

    internal val PositionalThreshold = 56.dp

    internal val VelocityThreshold = 125.dp

    /** The optional visual marker placed on top of a bottom sheet to indicate it may be dragged. */
    @Composable
    fun DragHandle(
        modifier: Modifier = Modifier,
        width: Dp = SheetBottomTokens.DockedDragHandleWidth,
        height: Dp = SheetBottomTokens.DockedDragHandleHeight,
        shape: Shape = MaterialTheme.shapes.extraLarge,
        color: Color = SheetBottomTokens.DockedDragHandleColor.value,
    ) {
        val dragHandleDescription = getString(Strings.BottomSheetDragHandleDescription)
        Surface(
            modifier =
                modifier.padding(vertical = DragHandleVerticalPadding).semantics {
                    contentDescription = dragHandleDescription
                },
            color = color,
            shape = shape,
        ) {
            Box(Modifier.size(width = width, height = height))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DragHandleWithTooltip(modifier: Modifier, content: @Composable (() -> Unit)) {
    val dragHandleDescription = getString(Strings.BottomSheetDragHandleDescription)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TooltipBox(
            modifier = modifier,
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(dragHandleDescription) } },
            state = rememberTooltipState(),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    sheetState: SheetState,
    orientation: Orientation,
    flingBehavior: FlingBehavior,
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                sheetState.anchoredDraggableState.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            return if (source == NestedScrollSource.UserInput) {
                sheetState.anchoredDraggableState.dispatchRawDelta(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = available.toFloat()
            val currentOffset = sheetState.requireOffset()
            val minAnchor = sheetState.anchoredDraggableState.anchors.minPosition()
            return if (toFling < 0 && currentOffset > minAnchor) {
                sheetState.anchoredDrag(flingBehavior, toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val toFling = available.toFloat()
            val consumedByAnchoredDraggableFling = sheetState.anchoredDrag(flingBehavior, toFling)
            return Velocity(consumed.x, consumedByAnchoredDraggableFling)
        }

        private fun Float.toOffset(): Offset =
            Offset(
                x = if (orientation == Orientation.Horizontal) this else 0f,
                y = if (orientation == Orientation.Vertical) this else 0f,
            )

        @JvmName("velocityToFloat")
        private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

        @JvmName("offsetToFloat")
        private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
    }

@Composable
@ExperimentalMaterial3Api
internal fun rememberSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = Hidden,
    skipHiddenState: Boolean = false,
    positionalThreshold: Dp = BottomSheetDefaults.PositionalThreshold,
    velocityThreshold: Dp = BottomSheetDefaults.VelocityThreshold,
): SheetState {
    val density = LocalDensity.current
    val positionalThresholdToPx = { with(density) { positionalThreshold.toPx() } }
    val velocityThresholdToPx = { with(density) { velocityThreshold.toPx() } }
    return rememberSaveable(
        skipPartiallyExpanded,
        confirmValueChange,
        skipHiddenState,
        saver =
            SheetState.Saver(
                skipPartiallyExpanded = skipPartiallyExpanded,
                positionalThreshold = positionalThresholdToPx,
                velocityThreshold = velocityThresholdToPx,
                confirmValueChange = confirmValueChange,
                skipHiddenState = skipHiddenState,
            ),
    ) {
        SheetState(
            skipPartiallyExpanded,
            positionalThresholdToPx,
            velocityThresholdToPx,
            initialValue,
            confirmValueChange,
            skipHiddenState,
        )
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

/**
 * A [Modifier] that scales up the drawing layer on the Y axis in case the [SheetState]'s
 * anchoredDraggableState offset overflows below the min anchor coordinates. The scaling will ensure
 * that there is no visible gap between the sheet and the edge of the screen in case the sheet
 * bounces when it opens due to a more expressive motion setting.
 *
 * A [verticalScaleDown] should be applied to the content of the sheet to maintain the content
 * aspect ratio as the container scales up.
 *
 * @param state a [SheetState]
 * @see verticalScaleDown
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.verticalScaleUp(state: SheetState) = graphicsLayer {
    val offset = state.anchoredDraggableState.offset
    val anchor = state.anchoredDraggableState.anchors.minPosition()
    val overflow = if (offset < anchor) anchor - offset else 0f
    scaleY = if (overflow > 0f) (size.height + overflow) / size.height else 1f
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
}

/**
 * A [Modifier] that scales down the drawing layer on the Y axis in case the [SheetState]'s
 * anchoredDraggableState offset overflows below the min anchor coordinates. This modifier should be
 * applied to the content inside a component that was scaled up with a [verticalScaleUp] modifier.
 * It will ensure that the content maintains its aspect ratio as the container scales up.
 *
 * @param state a [SheetState]
 * @see verticalScaleUp
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.verticalScaleDown(state: SheetState) = graphicsLayer {
    val offset = state.anchoredDraggableState.offset
    val anchor = state.anchoredDraggableState.anchors.minPosition()
    val overflow = if (offset < anchor) anchor - offset else 0f
    scaleY = if (overflow > 0f) 1 / ((size.height + overflow) / size.height) else 1f
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
}

/** A function that provides the default animation spec used by [SheetState]. */
internal val BottomSheetAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 300, easing = FastOutSlowInEasing)

private val DragHandleVerticalPadding = 22.dp
private val PredictiveBackMaxScaleXDistance = 48.dp
private val PredictiveBackMaxScaleYDistance = 24.dp
internal val PredictiveBackChildTransformOrigin = TransformOrigin(0.5f, 0f)
