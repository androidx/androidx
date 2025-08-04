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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.internal.AnchoredDraggableState
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.animateTo
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.snapTo
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.material3.tokens.SheetBottomTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmName
import kotlinx.coroutines.CancellationException

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
    positionalThreshold: () -> Float,
    velocityThreshold: () -> Float,
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
        get() = anchoredDraggableState.currentValue

    /**
     * The target value of the bottom sheet state.
     *
     * If a swipe is in progress, this is the value that the sheet would animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: SheetValue
        get() = anchoredDraggableState.targetValue

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
        get() = anchoredDraggableState.anchors.hasAnchorFor(Expanded)

    /** Whether the modal bottom sheet has a partially expanded state defined. */
    val hasPartiallyExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasAnchorFor(PartiallyExpanded)

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
     * @param velocity an initial velocity for the animation
     * @throws CancellationException if the interaction interrupted by another interaction like a
     *   gesture interaction or another programmatic interaction like a [animateTo] or [snapTo]
     *   call.
     */
    internal suspend fun animateTo(
        targetValue: SheetValue,
        animationSpec: FiniteAnimationSpec<Float>,
        velocity: Float = anchoredDraggableState.lastVelocity,
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                var prev = if (offset.isNaN()) 0f else offset
                animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                    // Our onDrag coerces the value within the bounds, but an animation may
                    // overshoot, for example a spring animation or an overshooting interpolator
                    // We respect the user's intention and allow the overshoot, but still use
                    // DraggableState's drag for its mutex.
                    dragTo(value, velocity)
                    prev = value
                }
            }
        }
    }

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

    /**
     * Find the closest anchor taking into account the velocity and settle at it with an animation.
     */
    internal suspend fun settle(velocity: Float) {
        anchoredDraggableState.settle(velocity)
    }

    internal var anchoredDraggableMotionSpec: AnimationSpec<Float> = BottomSheetAnimationSpec

    internal var anchoredDraggableState =
        AnchoredDraggableState(
            initialValue = initialValue,
            animationSpec = { anchoredDraggableMotionSpec },
            confirmValueChange = confirmValueChange,
            positionalThreshold = { positionalThreshold() },
            velocityThreshold = velocityThreshold,
        )

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
                    SheetState(
                        skipPartiallyExpanded,
                        positionalThreshold,
                        velocityThreshold,
                        savedValue,
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
    val windowInsets: WindowInsets
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
internal fun ColumnScope.DragHandleWithTooltip(content: @Composable (() -> Unit)) {
    val dragHandleDescription = getString(Strings.BottomSheetDragHandleDescription)
    // We need outer box for alignment because TooltipBox's modifier is only applied to its anchor.
    Box(Modifier.align(CenterHorizontally)) {
        TooltipBox(
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
    onFling: (velocity: Float) -> Unit,
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
            val minAnchor = sheetState.anchoredDraggableState.anchors.minAnchor()
            return if (toFling < 0 && currentOffset > minAnchor) {
                onFling(toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            onFling(available.toFloat())
            return available
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

private val DragHandleVerticalPadding = 22.dp

/** A function that provides the default animation spec used by [SheetState]. */
private val BottomSheetAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 300, easing = FastOutSlowInEasing)
