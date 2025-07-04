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

package androidx.wear.compose.foundation.rotary

import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.inverseLerp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * A modifier which connects rotary events with scrollable containers such as Column, LazyList and
 * others. [ScalingLazyColumn] has a build-in rotary support, and accepts [RotaryScrollableBehavior]
 * directly as a parameter.
 *
 * This modifier handles rotary input devices, used for scrolling. These devices can be categorized
 * as high-resolution or low-resolution based on their precision.
 * - High-res devices: Offer finer control and can detect smaller rotations. This allows for more
 *   precise adjustments during scrolling. One example of a high-res device is the crown (also known
 *   as rotating side button), located on the side of the watch.
 * - Low-res devices: Have less granular control, registering larger rotations at a time. Scrolling
 *   behavior is adapted to compensate for these larger jumps. Examples include physical or virtual
 *   bezels, positioned around the screen.
 *
 * This modifier supports rotary scrolling and snapping. The behaviour is configured by the provided
 * [RotaryScrollableBehavior]: either provide [RotaryScrollableDefaults.behavior] for scrolling
 * with/without fling or pass [RotaryScrollableDefaults.snapBehavior] when snap is required.
 *
 * The default scroll direction of this modifier is aligned with the scroll direction of the
 * `Modifier.verticalScroll` and `Modifier.horizontalScroll`, (please be aware that
 * `Modifier.scrollable` has the opposite direction by default).
 *
 * To keep the scroll direction aligned, `reverseDirection` flag should have the same value as the
 * `reverseScrolling` parameter in `Modifier.verticalScroll` and `Modifier.horizontalScroll`, and
 * the opposite value to the `reverseDirection` parameter used in `Modifier.scrollable`. When used
 * for horizontal scrolling, RTL/LTR orientations should be taken into account, as these can affect
 * the expected scroll behavior. It's recommended to use `ScrollableDefaults.reverseDirection` for
 * handling LTR/RTL layouts for horizontal scrolling.
 *
 * This overload provides the access to [OverscrollEffect] that defines the behaviour of the rotary
 * over scrolling logic. Use [androidx.compose.foundation.rememberOverscrollEffect] to create an
 * instance of the current provided overscroll implementation.
 *
 * Example of scrolling with fling:
 *
 * @sample androidx.wear.compose.foundation.samples.RotaryScrollSample
 *
 * Example of scrolling with snap:
 *
 * @sample androidx.wear.compose.foundation.samples.RotarySnapSample
 *
 * Example of scrolling with overscroll:
 *
 * @sample androidx.wear.compose.foundation.samples.RotaryScrollWithOverscrollSample
 * @param behavior Specified [RotaryScrollableBehavior] for rotary handling with snap or fling.
 * @param focusRequester Used to request the focus for rotary input. Each composable with this
 *   modifier should have a separate focusRequester, and only one of them at a time can be active.
 *   We recommend using [requestFocusOnHierarchyActive] and passing this focusRequester to it to
 *   handle requesting focus, as this will guarantee the proper behavior.
 * @param reverseDirection Reverses the direction of the rotary scroll. This direction should be
 *   aligned with the general touch scroll direction - and should be reversed if, for example, it
 *   was reversed in `.verticalScroll` or `.horizontalScroll` modifiers. If used with a
 *   `.scrollable` modifier - the scroll direction should be the opposite to the one specified
 *   there. When used for horizontal scrolling, RTL/LTR orientations should be taken into account,
 *   as these can affect the expected scroll behavior. It's recommended to use
 *   `ScrollableDefaults.reverseDirection` for handling LTR/RTL layouts for horizontal scrolling.
 * @param overscrollEffect effect to which the deltas will be fed when the scrollable have some
 *   scrolling delta left. Pass `null` for no overscroll. If you pass an effect you should also
 *   apply [androidx.compose.foundation.overscroll] modifier.
 */
public fun Modifier.rotaryScrollable(
    behavior: RotaryScrollableBehavior,
    focusRequester: FocusRequester,
    reverseDirection: Boolean = false,
    overscrollEffect: OverscrollEffect? = null,
): Modifier =
    rotaryHandler(
            behavior = behavior,
            overscrollEffect = overscrollEffect,
            reverseDirection = reverseDirection,
        )
        .focusRequester(focusRequester)
        .focusTargetWithSemantics()

/**
 * A modifier which connects rotary events with scrollable containers such as Column, LazyList and
 * others. [ScalingLazyColumn] has a build-in rotary support, and accepts [RotaryScrollableBehavior]
 * directly as a parameter.
 *
 * This modifier handles rotary input devices, used for scrolling. These devices can be categorized
 * as high-resolution or low-resolution based on their precision.
 * - High-res devices: Offer finer control and can detect smaller rotations. This allows for more
 *   precise adjustments during scrolling. One example of a high-res device is the crown (also known
 *   as rotating side button), located on the side of the watch.
 * - Low-res devices: Have less granular control, registering larger rotations at a time. Scrolling
 *   behavior is adapted to compensate for these larger jumps. Examples include physical or virtual
 *   bezels, positioned around the screen.
 *
 * This modifier supports rotary scrolling and snapping. The behaviour is configured by the provided
 * [RotaryScrollableBehavior]: either provide [RotaryScrollableDefaults.behavior] for scrolling
 * with/without fling or pass [RotaryScrollableDefaults.snapBehavior] when snap is required.
 *
 * Example of scrolling with fling:
 *
 * @sample androidx.wear.compose.foundation.samples.RotaryScrollSample
 *
 * Example of scrolling with snap:
 *
 * @sample androidx.wear.compose.foundation.samples.RotarySnapSample
 * @param behavior Specified [RotaryScrollableBehavior] for rotary handling with snap or fling.
 * @param focusRequester Used to request the focus for rotary input. Each composable with this
 *   modifier should have a separate focusRequester, and only one of them at a time can be active.
 *   We recommend using [requestFocusOnHierarchyActive] and passing this focusRequester to it to
 *   handle requesting focus, as this will guarantee the proper behavior.
 * @param reverseDirection Reverse the direction of scrolling if required for consistency with the
 *   scrollable state passed via [behavior].
 */
@Deprecated(
    "Deprecated, use another overload with overscroll parameter instead",
    level = DeprecationLevel.HIDDEN,
)
public fun Modifier.rotaryScrollable(
    behavior: RotaryScrollableBehavior,
    focusRequester: FocusRequester,
    reverseDirection: Boolean = false,
): Modifier =
    rotaryScrollable(
        behavior = behavior,
        overscrollEffect = null,
        focusRequester = focusRequester,
        reverseDirection = reverseDirection,
    )

/**
 * An interface for handling scroll events. Has implementations for handling scroll with/without
 * fling [FlingRotaryScrollableBehavior] and for handling snap [LowResSnapRotaryScrollableBehavior],
 * [HighResSnapRotaryScrollableBehavior] (see [Modifier.rotaryScrollable] for descriptions of
 * low-res and high-res devices). Note: It's not recommended to extend this interface directly.
 * Please use the provided implementations instead.
 */
public interface RotaryScrollableBehavior {

    /**
     * Executes a scrolling operation based on rotary input.
     *
     * @param timestampMillis The time in milliseconds at which this even occurred
     * @param delta The amount to scroll, in pixels
     * @param inputDeviceId The id for the input device that this event came from
     * @param orientation Orientation of the scrolling
     */
    public suspend fun CoroutineScope.performScroll(
        timestampMillis: Long,
        delta: Float,
        inputDeviceId: Int,
        orientation: Orientation,
    )
}

/**
 * A provider which connects scrollableState to a rotary input for snapping scroll actions.
 *
 * This interface defines the essential properties and methods required for a scrollable to be
 * controlled by rotary input and perform a snap action.
 */
public interface RotarySnapLayoutInfoProvider {

    /**
     * The average size in pixels of an item within the scrollable. This is used to estimate
     * scrolling distances for snapping when responding to rotary input.
     */
    public val averageItemSize: Float

    /** The index of the item that is closest to the center. */
    public val currentItemIndex: Int

    /**
     * The offset in pixels of the currently centered item from its centered position. This value
     * can be positive or negative.
     */
    public val currentItemOffset: Float

    /** The total number of items within the scrollable */
    public val totalItemCount: Int
}

/** Defaults for rotaryScrollable modifier */
public object RotaryScrollableDefaults {

    /**
     * Implementation of [RotaryScrollableBehavior] to define scrolling behaviour with or without
     * fling - used with the [rotaryScrollable] modifier when snapping is not required.
     *
     * If fling is not required, set [flingBehavior] = null. In that case, flinging will not happen
     * and the scrollable content will stop scrolling immediately after the user stops interacting
     * with rotary input.
     *
     * @param scrollableState Scrollable state which will be scrolled while receiving rotary events.
     * @param flingBehavior Optional rotary fling behavior, pass null to turn off fling if
     *   necessary.
     * @param hapticFeedbackEnabled Controls whether haptic feedback is given during rotary
     *   scrolling (true by default). It's recommended to keep the default value of true for premium
     *   scrolling experience.
     */
    @Composable
    public fun behavior(
        scrollableState: ScrollableState,
        flingBehavior: FlingBehavior? = ScrollableDefaults.flingBehavior(),
        hapticFeedbackEnabled: Boolean = true,
    ): RotaryScrollableBehavior {
        val isLowRes = isLowResInput()
        val viewConfiguration = ViewConfiguration.get(LocalContext.current)
        val rotaryHaptics: RotaryHapticHandler =
            rememberRotaryHapticHandler(scrollableState, hapticFeedbackEnabled)

        return flingBehavior(
            scrollableState,
            rotaryHaptics,
            flingBehavior,
            isLowRes,
            viewConfiguration,
        )
    }

    /**
     * Implementation of [RotaryScrollableBehavior] to define scrolling behaviour with snap - used
     * with the [rotaryScrollable] modifier when snapping is required.
     *
     * @param scrollableState Scrollable state which will be scrolled while receiving rotary events.
     * @param layoutInfoProvider A connection between scrollable entities and rotary events.
     * @param snapOffset An optional offset to be applied when snapping the item. Defines the
     *   distance from the center of the scrollable to the center of the snapped item.
     * @param hapticFeedbackEnabled Controls whether haptic feedback is given during rotary
     *   scrolling (true by default). It's recommended to keep the default value of true for premium
     *   scrolling experience.
     */
    @Composable
    public fun snapBehavior(
        scrollableState: ScrollableState,
        layoutInfoProvider: RotarySnapLayoutInfoProvider,
        snapOffset: Dp = 0.dp,
        hapticFeedbackEnabled: Boolean = true,
    ): RotaryScrollableBehavior =
        snapBehavior(
            scrollableState = scrollableState,
            layoutInfoProvider = layoutInfoProvider,
            snapSensitivity = RotarySnapSensitivity.DEFAULT,
            snapOffset = snapOffset,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )

    /**
     * Implementation of [RotaryScrollableBehavior] to define scrolling behaviour with snap for
     * [ScalingLazyColumn] - used with the [rotaryScrollable] modifier when snapping is required.
     *
     * @param scrollableState [ScalingLazyListState] to which rotary scroll will be connected.
     * @param snapOffset An optional offset to be applied when snapping the item. Defines the
     *   distance from the center of the scrollable to the center of the snapped item.
     * @param hapticFeedbackEnabled Controls whether haptic feedback is given during rotary
     *   scrolling (true by default). It's recommended to keep the default value of true for premium
     *   scrolling experience.
     */
    @Composable
    public fun snapBehavior(
        scrollableState: ScalingLazyListState,
        snapOffset: Dp = 0.dp,
        hapticFeedbackEnabled: Boolean = true,
    ): RotaryScrollableBehavior =
        snapBehavior(
            scrollableState = scrollableState,
            layoutInfoProvider =
                remember(scrollableState) {
                    ScalingLazyColumnRotarySnapLayoutInfoProvider(scrollableState)
                },
            snapOffset = snapOffset,
            snapSensitivity = RotarySnapSensitivity.DEFAULT,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )

    /**
     * Implementation of [RotaryScrollableBehavior] to define scrolling behaviour with snap for
     * [HorizontalPager] and [VerticalPager].
     *
     * @param pagerState [PagerState] to which rotary scroll will be connected.
     * @param snapOffset An optional offset to be applied when snapping the item. Defines the
     *   distance from the center of the scrollable to the center of the snapped item.
     * @param hapticFeedbackEnabled Controls whether haptic feedback is given during rotary
     *   scrolling (true by default). It's recommended to keep the default value of true for premium
     *   scrolling experience.
     */
    @Composable
    public fun snapBehavior(
        pagerState: PagerState,
        snapOffset: Dp = 0.dp,
        hapticFeedbackEnabled: Boolean = true,
    ): RotaryScrollableBehavior {
        return snapBehavior(
            scrollableState = pagerState,
            layoutInfoProvider =
                remember(pagerState) { PagerRotarySnapLayoutInfoProvider(pagerState) },
            snapSensitivity = RotarySnapSensitivity.HIGH,
            snapOffset = snapOffset,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )
    }

    @Composable
    private fun snapBehavior(
        scrollableState: ScrollableState,
        layoutInfoProvider: RotarySnapLayoutInfoProvider,
        snapSensitivity: RotarySnapSensitivity,
        snapOffset: Dp,
        hapticFeedbackEnabled: Boolean,
    ): RotaryScrollableBehavior {
        val isLowRes = isLowResInput()
        val snapOffsetPx = with(LocalDensity.current) { snapOffset.roundToPx() }
        val rotaryHaptics: RotaryHapticHandler =
            rememberRotaryHapticHandler(scrollableState, hapticFeedbackEnabled)

        return remember(scrollableState, layoutInfoProvider, rotaryHaptics, snapOffset, isLowRes) {
            snapBehavior(
                scrollableState,
                layoutInfoProvider,
                rotaryHaptics,
                snapSensitivity,
                snapOffsetPx,
                isLowRes,
            )
        }
    }

    /** Returns whether the input is Low-res (a bezel) or high-res (a crown/rsb). */
    @Composable
    private fun isLowResInput(): Boolean =
        LocalContext.current.packageManager.hasSystemFeature(
            "android.hardware.rotaryencoder.lowres"
        )

    // These values represent the timeframe for a fling event. A bigger value is assigned
    // to low-res input due to the lower frequency of low-res rotary events.
    internal const val LowResFlingTimeframe: Long = 100L
    internal const val HighResFlingTimeframe: Long = 30L
}

/** An implementation of rotary scroll adapter for ScalingLazyColumn */
internal class ScalingLazyColumnRotarySnapLayoutInfoProvider(
    private val scrollableState: ScalingLazyListState
) : RotarySnapLayoutInfoProvider {

    /** Calculates the average item height by averaging the height of visible items. */
    override val averageItemSize: Float
        get() {
            val visibleItems = scrollableState.layoutInfo.visibleItemsInfo
            return if (visibleItems.isNotEmpty()) {
                (visibleItems.fastSumBy { it.unadjustedSize } / visibleItems.size).toFloat()
            } else 0f
        }

    /** Current (centered) item index */
    override val currentItemIndex: Int
        get() = scrollableState.centerItemIndex

    /** The offset from the item center. */
    override val currentItemOffset: Float
        get() = scrollableState.centerItemScrollOffset.toFloat()

    /** The total count of items in ScalingLazyColumn */
    override val totalItemCount: Int
        get() = scrollableState.layoutInfo.totalItemsCount
}

/** An implementation of rotary scroll adapter for Pager */
internal class PagerRotarySnapLayoutInfoProvider(private val state: PagerState) :
    RotarySnapLayoutInfoProvider {

    /** Calculates the average item height by just taking the pageSize. */
    override val averageItemSize: Float
        get() = state.pagerState.layoutInfo.pageSize.toFloat()

    /** Current page */
    override val currentItemIndex: Int
        get() = state.pagerState.currentPage

    /** The offset from the page center. */
    override val currentItemOffset: Float
        get() = state.pagerState.currentPageOffsetFraction * averageItemSize

    /** The total count of items in Pager */
    override val totalItemCount: Int
        get() = state.pagerState.pageCount
}

internal class RotaryScrollLogic(
    val overscrollEffect: OverscrollEffect?,
    val nestedScrollDispatcher: NestedScrollDispatcher?,
    val reverseDirection: Boolean,
) {

    var orientation = Vertical

    fun ScrollScope.scroll(delta: Float, scrollableState: ScrollableState): Offset =
        if (overscrollEffect != null && scrollableState.shouldDispatchOverscroll) {
            overscrollEffect.applyToScroll(delta.toOffset().reverseIfNeeded(), UserInput) { offset
                ->
                performScroll(offset, nestedScrollDispatcher)
            }
        } else {
            performScroll(delta.toOffset().reverseIfNeeded(), nestedScrollDispatcher)
        }

    suspend fun ScrollScope.fling(
        velocity: Float,
        flingBehavior: FlingBehavior?,
        scrollableState: ScrollableState,
    ): Float {
        var consumedVelocity = Velocity.Zero
        if (overscrollEffect != null && scrollableState.shouldDispatchOverscroll) {
            overscrollEffect.applyToFling(velocity.reverseIfNeeded().toVelocity()) { velocityToApply
                ->
                consumedVelocity =
                    performFling(velocityToApply, nestedScrollDispatcher, flingBehavior)
                consumedVelocity
            }
        } else {
            consumedVelocity =
                performFling(
                    velocity.reverseIfNeeded().toVelocity(),
                    nestedScrollDispatcher,
                    flingBehavior,
                )
        }
        return consumedVelocity.toFloat().reverseIfNeeded()
    }

    private fun ScrollScope.performScroll(
        delta: Offset,
        nestedScrollDispatcher: NestedScrollDispatcher?,
    ): Offset {
        if (nestedScrollDispatcher != null) {
            val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(delta, UserInput)
            val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

            val singleAxisDeltaForSelfScroll =
                scrollAvailableAfterPreScroll.toFloat().reverseIfNeeded().toFloat()

            val consumedBySelfScroll =
                scrollBy(singleAxisDeltaForSelfScroll).reverseIfNeeded().toOffset()

            val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll

            val consumedByPostScroll =
                nestedScrollDispatcher.dispatchPostScroll(
                    consumedBySelfScroll,
                    deltaAvailableAfterScroll,
                    UserInput,
                )

            debugLog {
                "performScroll: Scrollable delta: $delta, " +
                    "consumedByPreScroll: $consumedByPreScroll, " +
                    "scrollAvailableAfterPreScroll: $scrollAvailableAfterPreScroll, " +
                    "scrollBy: $singleAxisDeltaForSelfScroll, " +
                    "consumedBySelfScroll: $consumedBySelfScroll, " +
                    "deltaAvailableAfterScroll: $deltaAvailableAfterScroll"
            }
            return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
        } else {
            return scrollBy(delta.toFloat()).toOffset()
        }
    }

    private suspend fun ScrollScope.performFling(
        velocity: Velocity,
        nestedScrollDispatcher: NestedScrollDispatcher?,
        flingBehavior: FlingBehavior?,
    ): Velocity {
        val afterPreFling =
            velocity - (nestedScrollDispatcher?.dispatchPreFling(velocity) ?: Velocity.Zero)
        val afterFling =
            (if (flingBehavior != null) {
                    with(flingBehavior) {
                        performFling(afterPreFling.toFloat().reverseIfNeeded()).reverseIfNeeded()
                    }
                } else afterPreFling.toFloat().reverseIfNeeded())
                .toVelocity()

        val consumedByPostFling =
            nestedScrollDispatcher?.dispatchPostFling((afterPreFling - afterFling), afterFling)
                ?: Velocity.Zero
        val remainingVelocity = afterFling - consumedByPostFling

        debugLog {
            "performFling: Flinging with velocity: $velocity, " +
                "Velocity after dispatchPreFling: $afterPreFling, " +
                "Velocity after performFling: $afterFling, " +
                "remainingVelocity: $remainingVelocity"
        }
        // Returns consumed velocity
        return velocity - remainingVelocity
    }

    // TODO(b/397650406): Implement a more efficient way to reverse the scroll direction
    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this else -this

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this else -this

    fun Float.toOffset(): Offset =
        when {
            orientation == Horizontal -> Offset(this, 0f)
            else -> Offset(0f, this)
        }

    fun Offset.singleAxisOffset(): Offset =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Offset.toFloat(): Float = if (orientation == Horizontal) this.x else this.y

    fun Float.toVelocity(): Velocity =
        when {
            orientation == Horizontal -> Velocity(this, 0f)
            else -> Velocity(0f, this)
        }

    private fun Velocity.toFloat(): Float = if (orientation == Horizontal) this.x else this.y

    private fun Velocity.singleAxisVelocity(): Velocity =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)
}

/**
 * Handles scroll with fling.
 *
 * @param scrollableState Scrollable state which will be scrolled while receiving rotary events
 * @param flingBehavior Logic describing Fling behavior. If null - fling will not happen
 * @param isLowRes Whether the input is Low-res (a bezel) or high-res(a crown/rsb)
 * @param viewConfiguration [ViewConfiguration] for accessing default fling values
 * @return A scroll with fling implementation of [RotaryScrollableBehavior] which is suitable for
 *   both low-res and high-res inputs (see [Modifier.rotaryScrollable] for descriptions of low-res
 *   and high-res devices).
 */
private fun flingBehavior(
    scrollableState: ScrollableState,
    rotaryHaptics: RotaryHapticHandler,
    flingBehavior: FlingBehavior? = null,
    isLowRes: Boolean,
    viewConfiguration: ViewConfiguration,
): RotaryScrollableBehavior {

    fun rotaryFlingHandler(inputDeviceId: Int, initialTimestamp: Long) =
        flingBehavior?.run {
            RotaryFlingHandler(
                scrollableState = scrollableState,
                flingBehavior = flingBehavior,
                flingTimeframe =
                    if (isLowRes) RotaryScrollableDefaults.LowResFlingTimeframe
                    else RotaryScrollableDefaults.HighResFlingTimeframe,
                viewConfiguration = viewConfiguration,
                inputDeviceId = inputDeviceId,
                initialTimestamp = initialTimestamp,
            )
        }

    fun scrollHandler() = RotaryScrollHandler(scrollableState)

    return FlingRotaryScrollableBehavior(
        isLowRes,
        rotaryHaptics,
        rotaryFlingHandlerFactory = { inputDeviceId: Int, initialTimestamp: Long ->
            rotaryFlingHandler(inputDeviceId, initialTimestamp)
        },
        scrollHandlerFactory = { scrollHandler() },
    )
}

/**
 * Handles scroll with snap.
 *
 * @param layoutInfoProvider Implementation of [RotarySnapLayoutInfoProvider], which connects
 *   scrollableState to a rotary input for snapping scroll actions.
 * @param rotaryHaptics Implementation of [RotaryHapticHandler] which handles haptics for rotary
 *   usage
 * @param snapOffset An offset to be applied when snapping the item. After the snap the snapped
 *   items offset will be [snapOffset]. In pixels.
 * @param snapSensitivity Sensitivity of the rotary snap.
 * @param isLowRes Whether the input is Low-res (a bezel) or high-res(a crown/rsb)
 * @return A snap implementation of [RotaryScrollableBehavior] which is either suitable for low-res
 *   or high-res input (see [Modifier.rotaryScrollable] for descriptions of low-res and high-res
 *   devices).
 */
private fun snapBehavior(
    scrollableState: ScrollableState,
    layoutInfoProvider: RotarySnapLayoutInfoProvider,
    rotaryHaptics: RotaryHapticHandler,
    snapSensitivity: RotarySnapSensitivity,
    snapOffset: Int,
    isLowRes: Boolean,
): RotaryScrollableBehavior {
    return if (isLowRes) {
        LowResSnapRotaryScrollableBehavior(
            rotaryHaptics = rotaryHaptics,
            snapHandlerFactory = {
                RotarySnapHandler(scrollableState, layoutInfoProvider, snapOffset)
            },
        )
    } else {
        HighResSnapRotaryScrollableBehavior(
            rotaryHaptics = rotaryHaptics,
            scrollDistanceDivider = snapSensitivity.resistanceFactor,
            thresholdHandlerFactory = {
                ThresholdHandler(
                    minThresholdDivider = snapSensitivity.minThresholdDivider,
                    maxThresholdDivider = snapSensitivity.maxThresholdDivider,
                    averageItemSize = { layoutInfoProvider.averageItemSize },
                )
            },
            snapHandlerFactory = {
                RotarySnapHandler(scrollableState, layoutInfoProvider, snapOffset)
            },
            scrollHandlerFactory = { RotaryScrollHandler(scrollableState) },
        )
    }
}

/**
 * An abstract base class for handling scroll events. Has implementations for handling scroll
 * with/without fling [FlingRotaryScrollableBehavior] and for handling snap
 * [LowResSnapRotaryScrollableBehavior], [HighResSnapRotaryScrollableBehavior] (see
 * [Modifier.rotaryScrollable] for descriptions of low-res and high-res devices ).
 */
internal abstract class BaseRotaryScrollableBehavior : RotaryScrollableBehavior {

    // Threshold for detection of a new gesture
    private val gestureThresholdTime = 200L
    protected var previousScrollEventTime = -1L

    protected fun isNewScrollEvent(timestamp: Long): Boolean {
        val timeDelta = timestamp - previousScrollEventTime
        return previousScrollEventTime == -1L || timeDelta > gestureThresholdTime
    }

    internal var scrollLogic: RotaryScrollLogic = RotaryScrollLogic(null, null, false)
}

/**
 * This class does a smooth animation when the scroll by N pixels is done. This animation works well
 * on Rsb(high-res) and Bezel(low-res) devices.
 */
internal class RotaryScrollHandler(private val scrollableState: ScrollableState) {
    private var sequentialAnimation = false
    private var scrollAnimation = AnimationState(0f)
    private var prevPosition = 0f
    private var scrollJob: Job = CompletableDeferred<Unit>()

    /** Produces scroll to [targetValue] */
    fun scrollToTarget(
        coroutineScope: CoroutineScope,
        targetValue: Float,
        scrollLogic: RotaryScrollLogic,
    ) {
        cancelScrollIfActive()

        scrollJob = coroutineScope.async { scrollTo(targetValue, scrollLogic) }
    }

    fun cancelScrollIfActive() {
        if (scrollJob.isActive) scrollJob.cancel()
    }

    private suspend fun scrollTo(targetValue: Float, scrollLogic: RotaryScrollLogic) {
        scrollableState.scroll(MutatePriority.Default) {
            debugLog { "ScrollAnimation value before start: ${scrollAnimation.value}" }

            val animationSpec =
                if (scrollableState.atTheEdge) spring(visibilityThreshold = 0.3f) else spring()

            scrollAnimation.animateTo(
                targetValue,
                animationSpec = animationSpec,
                sequentialAnimation = sequentialAnimation,
            ) {
                val delta = value - prevPosition
                with(scrollLogic) { scroll(delta, scrollableState) }

                prevPosition = value
                sequentialAnimation = value != this.targetValue
            }
            // After the scroll ends we need to call a fling with 0f velocity for proper overscroll
            // and nested scroll support.
            with(scrollLogic) { fling(0f, null, scrollableState) }
        }
    }
}

/** A helper class for snapping with rotary. */
internal class RotarySnapHandler(
    private val scrollableState: ScrollableState,
    private val layoutInfoProvider: RotarySnapLayoutInfoProvider,
    private val snapOffset: Int,
) {
    private var snapTarget: Int = layoutInfoProvider.currentItemIndex
    private var sequentialSnap: Boolean = false

    private var anim = AnimationState(0f)
    private var expectedDistance = 0f

    private val defaultStiffness = 200f
    private var snapTargetUpdated = true

    /**
     * Updating snapping target. This method should be called before [snapToTargetItem].
     *
     * Snapping is done for current + [moveForElements] items.
     *
     * If [sequentialSnap] is true, items are summed up together. For example, if [updateSnapTarget]
     * is called with [moveForElements] = 2, 3, 5 -> then the snapping will happen to current + 10
     * items
     *
     * If [sequentialSnap] is false, then [moveForElements] are not summed up together.
     */
    fun updateSnapTarget(moveForElements: Int, sequentialSnap: Boolean) {
        this.sequentialSnap = sequentialSnap
        if (sequentialSnap) {
            snapTarget += moveForElements
        } else {
            snapTarget = layoutInfoProvider.currentItemIndex + moveForElements
        }
        snapTargetUpdated = true
        snapTarget =
            snapTarget.coerceIn(0..(layoutInfoProvider.totalItemCount - 1).coerceAtLeast(0))
        debugLog { "Snap target updated to $snapTarget, moveForElements: $moveForElements" }
    }

    /** Performs snapping to the closest item. */
    suspend fun snapToClosestItem(scrollLogic: RotaryScrollLogic) {
        // Perform the snapping animation
        scrollableState.scroll(MutatePriority.Default) {
            debugLog {
                "snap to the closest item, ceneredItem: ${layoutInfoProvider.currentItemIndex}, currentItemOffset: ${layoutInfoProvider.currentItemOffset}"
            }
            var prevPosition = 0f

            // Create and execute the snap animation
            AnimationState(0f).animateTo(
                targetValue = -layoutInfoProvider.currentItemOffset,
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
            ) {
                val animDelta = value - prevPosition
                with(scrollLogic) { scroll(animDelta, scrollableState) }
                prevPosition = value
            }
            // Update the snap target to ensure consistency
            snapTarget = layoutInfoProvider.currentItemIndex
        }
    }

    /** Returns true if top edge was reached */
    fun topEdgeReached(): Boolean = snapTarget <= 0

    /** Returns true if bottom edge was reached */
    fun bottomEdgeReached(): Boolean = snapTarget >= layoutInfoProvider.totalItemCount - 1

    /** Performs snapping to the specified in [updateSnapTarget] element */
    suspend fun snapToTargetItem(scrollLogic: RotaryScrollLogic) {
        if (!sequentialSnap) anim = AnimationState(0f)

        scrollableState.scroll(MutatePriority.Default) {
            // If snapTargetUpdated is true -means the target was updated so we
            // need to do snap animation again
            while (snapTargetUpdated) {
                snapTargetUpdated = false
                var latestCenterItem: Int
                var continueFirstScroll = true
                debugLog { "snapTarget $snapTarget" }

                // First part of animation. Performing it until the target element centered.
                while (continueFirstScroll) {
                    latestCenterItem = layoutInfoProvider.currentItemIndex
                    expectedDistance = expectedDistanceTo(snapTarget, snapOffset)
                    debugLog {
                        "expectedDistance = $expectedDistance, " +
                            "scrollableState.centerItemScrollOffset " +
                            "${layoutInfoProvider.currentItemOffset}"
                    }

                    continueFirstScroll = false
                    var prevPosition = anim.value
                    anim.animateTo(
                        prevPosition + expectedDistance,
                        animationSpec =
                            spring(stiffness = defaultStiffness, visibilityThreshold = 0.1f),
                        sequentialAnimation = (anim.velocity != 0f),
                    ) {
                        // Exit animation if snap target was updated
                        if (snapTargetUpdated) cancelAnimation()

                        val animDelta = value - prevPosition
                        debugLog {
                            "First animation, value:$value, velocity:$velocity, " +
                                "animDelta:$animDelta"
                        }
                        with(scrollLogic) { scroll(animDelta, scrollableState) }
                        prevPosition = value

                        if (latestCenterItem != layoutInfoProvider.currentItemIndex) {
                            continueFirstScroll = true
                            cancelAnimation()
                            return@animateTo
                        }

                        debugLog {
                            "centerItemIndex = ${layoutInfoProvider.currentItemIndex} " +
                                "currentItemOffset = ${layoutInfoProvider.currentItemOffset}"
                        }
                        if (layoutInfoProvider.currentItemIndex == snapTarget) {
                            debugLog { "Target is near the centre. Cancelling first animation" }
                            expectedDistance = -layoutInfoProvider.currentItemOffset
                            continueFirstScroll = false
                            cancelAnimation()
                            return@animateTo
                        }
                    }
                }
                // Exit animation if snap target was updated
                if (snapTargetUpdated) continue

                // Second part of Animation - animating to the centre of target element.
                var prevPosition = anim.value
                anim.animateTo(
                    prevPosition + expectedDistance,
                    animationSpec =
                        SpringSpec(stiffness = defaultStiffness, visibilityThreshold = 0.1f),
                    sequentialAnimation = (anim.velocity != 0f),
                ) {
                    // Exit animation if snap target was updated
                    if (snapTargetUpdated) cancelAnimation()

                    val animDelta = value - prevPosition
                    debugLog { "Final animation. velocity:$velocity, animDelta:$animDelta" }
                    with(scrollLogic) { scroll(animDelta, scrollableState) }
                    prevPosition = value
                }
            }
        }
    }

    private fun expectedDistanceTo(index: Int, targetScrollOffset: Int): Float {
        val averageSize = layoutInfoProvider.averageItemSize
        val indexesDiff = index - layoutInfoProvider.currentItemIndex
        debugLog { "Average size $averageSize" }
        return (averageSize * indexesDiff) + targetScrollOffset -
            layoutInfoProvider.currentItemOffset
    }
}

/**
 * A modifier which handles rotary events. It accepts [RotaryScrollableBehavior] as the input - a
 * class that handles the main scroll logic.
 */
internal fun Modifier.rotaryHandler(
    behavior: RotaryScrollableBehavior,
    overscrollEffect: OverscrollEffect?,
    reverseDirection: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit = debugInspectorInfo {
        name = "rotaryHandler"
        properties["behavior"] = behavior
        properties["overscrollEffect"] = overscrollEffect
        properties["reverseDirection"] = reverseDirection
    },
): Modifier =
    this then RotaryHandlerElement(behavior, overscrollEffect, reverseDirection, inspectorInfo)

/**
 * Class responsible for Fling behaviour with rotary. It tracks rotary events and produces fling
 * when necessary.
 *
 * @param scrollableState The [ScrollableState] used to perform the fling.
 * @param flingBehavior The [FlingBehavior] used to control the fling animation.
 * @param flingTimeframe Represents a time interval (in milliseconds) used to determine whether a
 *   rotary input should trigger a fling. If no new events come during this interval, then the fling
 *   is triggered.
 * @param viewConfiguration The [ViewConfiguration] used to obtain fling velocity thresholds.
 * @param inputDeviceId The ID of the input device generating the rotary events.
 * @param initialTimestamp The initial timestamp of the fling tracking session.
 */
internal class RotaryFlingHandler(
    private val scrollableState: ScrollableState,
    private val flingBehavior: FlingBehavior,
    private val flingTimeframe: Long,
    viewConfiguration: ViewConfiguration,
    inputDeviceId: Int,
    initialTimestamp: Long,
) {
    private var flingJob: Job = CompletableDeferred<Unit>()

    // A time range during which the fling is valid.
    // For simplicity it's twice as long as [flingTimeframe]
    private val timeRangeToFling = flingTimeframe * 2

    //  A default fling factor for making fling slower
    private val flingScaleFactor = 0.7f

    private var previousVelocity = 0f

    private val rotaryVelocityTracker = RotaryVelocityTracker()

    private val minFlingSpeed: Float
    private val maxFlingSpeed: Float
    private var latestEventTimestamp: Long = 0

    private var flingVelocity: Float = 0f
    private var flingTimestamp: Long = 0

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            minFlingSpeed =
                viewConfiguration
                    .getScaledMinimumFlingVelocity(inputDeviceId, AxisScroll, RotaryInputSource)
                    .toFloat()
            maxFlingSpeed =
                viewConfiguration
                    .getScaledMaximumFlingVelocity(inputDeviceId, AxisScroll, RotaryInputSource)
                    .toFloat()
        } else {
            minFlingSpeed = viewConfiguration.scaledMinimumFlingVelocity.toFloat()
            maxFlingSpeed = viewConfiguration.scaledMaximumFlingVelocity.toFloat()
        }

        startFlingTracking(initialTimestamp)
    }

    fun cancelFlingIfActive() {
        if (flingJob.isActive) flingJob.cancel()
    }

    /** Observing new event within a fling tracking session with new timestamp and delta */
    fun observeEvent(timestamp: Long, delta: Float) {
        rotaryVelocityTracker.move(timestamp, delta)
        latestEventTimestamp = timestamp
    }

    fun performFlingIfRequired(
        coroutineScope: CoroutineScope,
        beforeFling: () -> Unit,
        scrollLogic: RotaryScrollLogic,
        edgeReached: (velocity: Float) -> Unit,
    ) {
        cancelFlingIfActive()

        flingJob = coroutineScope.launch { trackFling(beforeFling, scrollLogic, edgeReached) }
    }

    /** Starts a new fling tracking session with specified timestamp */
    private fun startFlingTracking(timestamp: Long) {
        rotaryVelocityTracker.start(timestamp)
        latestEventTimestamp = timestamp
        previousVelocity = 0f
    }

    /**
     * Performing fling if necessary and calling [beforeFling] lambda before it is triggered.
     * [edgeReached] is called when the scroll reaches the end of the list and can't scroll further
     */
    private suspend fun trackFling(
        beforeFling: () -> Unit,
        scrollLogic: RotaryScrollLogic,
        edgeReached: (velocity: Float) -> Unit,
    ) {
        val currentVelocity = rotaryVelocityTracker.velocity
        debugLog { "currentVelocity: $currentVelocity" }

        if (abs(currentVelocity) >= abs(previousVelocity)) {
            flingTimestamp = latestEventTimestamp
            flingVelocity = currentVelocity * flingScaleFactor
        }
        previousVelocity = currentVelocity

        // Waiting for a fixed amount of time before checking the fling
        delay(flingTimeframe)

        // For making a fling 2 criteria should be met:
        // 1) no more than
        // `timeRangeToFling` ms should pass between last fling detection
        // and the time of last motion event
        // 2) flingVelocity should exceed the minFlingSpeed
        debugLog {
            "Check fling:  flingVelocity: $flingVelocity " +
                "minFlingSpeed: $minFlingSpeed, maxFlingSpeed: $maxFlingSpeed"
        }
        if (
            latestEventTimestamp - flingTimestamp < timeRangeToFling &&
                abs(flingVelocity) > minFlingSpeed
        ) {
            scrollableState.scroll(MutatePriority.Default) {
                // Call beforeFling because a fling will be performed
                beforeFling()
                val coercedVelocity = flingVelocity.coerceIn(-maxFlingSpeed, maxFlingSpeed)
                with(scrollLogic) {
                    val consumedVelocity = fling(coercedVelocity, flingBehavior, scrollableState)
                    debugLog {
                        "After fling: original velocity: ${coercedVelocity}, consumed velocity: $consumedVelocity"
                    }
                    val remainingVelocity = coercedVelocity - consumedVelocity
                    if (remainingVelocity != 0.0f) {
                        edgeReached(remainingVelocity)
                    }
                }
            }
        }
    }
}

/**
 * A scroll behavior for scrolling without snapping and with or without fling. A list is scrolled by
 * the number of pixels received from the rotary device.
 *
 * For a high-res input it has a filtering for events which are coming with an opposite sign (this
 * might happen to devices with rsb, especially at the end of the scroll ) - see
 * [Modifier.rotaryScrollable] for descriptions of low-res and high-res devices.
 *
 * This scroll behavior supports fling. It can be set with [RotaryFlingHandler].
 */
internal class FlingRotaryScrollableBehavior(
    private val isLowRes: Boolean,
    private val rotaryHaptics: RotaryHapticHandler,
    private val rotaryFlingHandlerFactory:
        (inputDeviceId: Int, initialTimestamp: Long) -> RotaryFlingHandler?,
    private val scrollHandlerFactory: () -> RotaryScrollHandler,
) : BaseRotaryScrollableBehavior() {
    private var rotaryScrollDistance = 0f

    private var rotaryFlingHandler: RotaryFlingHandler? = null
    private var scrollHandler: RotaryScrollHandler = scrollHandlerFactory()

    override suspend fun CoroutineScope.performScroll(
        timestampMillis: Long,
        delta: Float,
        inputDeviceId: Int,
        orientation: Orientation,
    ) {
        debugLog { "FlingRotaryScrollableBehavior: performScroll" }

        if (isNewScrollEvent(timestampMillis)) {
            debugLog { "New scroll event" }
            resetScrolling()
            resetFlingTracking(timestampMillis, inputDeviceId)
        } else {
            // Due to the physics of high-res Rotary side button, some events might come
            // with an opposite axis value - either at the start or at the end of the motion.
            // We don't want to use these values for fling calculations.
            if (isLowRes || !isOppositeValueAfterScroll(delta)) {
                rotaryFlingHandler?.observeEvent(timestampMillis, delta)
            } else {
                debugLog { "Opposite value after scroll :$delta" }
            }
        }

        rotaryScrollDistance += delta
        debugLog { "Rotary scroll distance: $rotaryScrollDistance" }

        rotaryHaptics.handleScrollHaptic(timestampMillis, delta, inputDeviceId, AxisScroll)

        previousScrollEventTime = timestampMillis
        scrollHandler.scrollToTarget(this, rotaryScrollDistance, scrollLogic)

        rotaryFlingHandler?.performFlingIfRequired(
            this,
            beforeFling = {
                debugLog { "Calling beforeFling section" }
                resetScrolling()
            },
            scrollLogic = scrollLogic,
            edgeReached = { velocity ->
                debugLog { "Edge reached, velocity: $velocity" }
                rotaryHaptics.handleLimitHaptic(velocity > 0f, inputDeviceId, AxisScroll)
            },
        )
    }

    private fun resetScrolling() {
        scrollHandler.cancelScrollIfActive()
        scrollHandler = scrollHandlerFactory()
        rotaryScrollDistance = 0f
    }

    private fun resetFlingTracking(timestamp: Long, inputDeviceId: Int) {
        rotaryFlingHandler?.cancelFlingIfActive()
        rotaryFlingHandler = rotaryFlingHandlerFactory(inputDeviceId, timestamp)
    }

    private fun isOppositeValueAfterScroll(delta: Float): Boolean =
        rotaryScrollDistance * delta < 0f && (abs(delta) < abs(rotaryScrollDistance))
}

/**
 * A scroll behavior for RSB(high-res) input with snapping and without fling (see
 * [Modifier.rotaryScrollable] for descriptions of low-res and high-res devices ).
 *
 * Threshold for snapping is set dynamically in ThresholdBehavior, which depends on the scroll speed
 * and the average size of the items.
 *
 * This scroll handler doesn't support fling.
 */
internal class HighResSnapRotaryScrollableBehavior(
    private val rotaryHaptics: RotaryHapticHandler,
    private val scrollDistanceDivider: Float,
    private val thresholdHandlerFactory: () -> ThresholdHandler,
    private val snapHandlerFactory: () -> RotarySnapHandler,
    private val scrollHandlerFactory: () -> RotaryScrollHandler,
) : BaseRotaryScrollableBehavior() {
    private val snapDelay = 100L

    // This parameter limits number of snaps which can happen during single event.
    private val maxSnapsPerEvent = 2

    private var snapJob: Job = CompletableDeferred<Unit>()

    private var accumulatedSnapDelta = 0f
    private var rotaryScrollDistance = 0f

    private var snapHandler = snapHandlerFactory()
    private var scrollHandler = scrollHandlerFactory()
    private var thresholdHandler = thresholdHandlerFactory()

    private val scrollProximityEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.5f, 1.0f)

    override suspend fun CoroutineScope.performScroll(
        timestampMillis: Long,
        delta: Float,
        inputDeviceId: Int,
        orientation: Orientation,
    ) {
        debugLog { "HighResSnapRotaryScrollableBehavior: performScroll" }

        if (isNewScrollEvent(timestampMillis)) {
            debugLog { "New scroll event" }
            resetScrolling()
            resetSnapping()
            resetThresholdTracking(timestampMillis)
        }

        if (!isOppositeValueAfterScroll(delta)) {
            thresholdHandler.updateTracking(timestampMillis, delta)
        } else {
            debugLog { "Opposite value after scroll :$delta" }
        }

        val snapThreshold = thresholdHandler.calculateSnapThreshold()
        debugLog { "snapThreshold: $snapThreshold" }

        if (!snapJob.isActive) {
            val proximityFactor = calculateProximityFactor(snapThreshold)
            rotaryScrollDistance += delta * proximityFactor
        }
        debugLog { "Rotary scroll distance: $rotaryScrollDistance" }

        accumulatedSnapDelta += delta
        debugLog { "Accumulated snap delta: $accumulatedSnapDelta" }

        previousScrollEventTime = timestampMillis

        if (abs(accumulatedSnapDelta) > snapThreshold) {
            resetScrolling()

            // We limit a number of handled snap items per event to [maxSnapsPerEvent],
            // as otherwise the snap might happen too quickly
            val snapDistanceInItems =
                (accumulatedSnapDelta / snapThreshold)
                    .toInt()
                    .coerceIn(-maxSnapsPerEvent..maxSnapsPerEvent)
            accumulatedSnapDelta -= snapThreshold * snapDistanceInItems
            //
            val sequentialSnap = snapJob.isActive

            debugLog {
                "Snap threshold reached: snapDistanceInItems:$snapDistanceInItems, " +
                    "sequentialSnap: $sequentialSnap, " +
                    "Accumulated snap delta: $accumulatedSnapDelta"
            }
            if (edgeNotReached(snapDistanceInItems)) {
                rotaryHaptics.handleSnapHaptic(timestampMillis, delta, inputDeviceId, AxisScroll)
            }

            snapHandler.updateSnapTarget(snapDistanceInItems, sequentialSnap)
            if (!snapJob.isActive) {
                snapJob.cancel()
                snapJob =
                    with(this) {
                        async {
                            debugLog { "Snap started" }
                            try {
                                snapHandler.snapToTargetItem(scrollLogic)
                            } finally {
                                debugLog { "Snap called finally" }
                            }
                        }
                    }
            }
            rotaryScrollDistance = 0f
        } else {
            if (!snapJob.isActive) {
                val distanceWithDivider = rotaryScrollDistance / scrollDistanceDivider
                debugLog { "Scrolling for $distanceWithDivider px" }

                scrollHandler.scrollToTarget(this, distanceWithDivider, scrollLogic)
                delay(snapDelay)

                resetScrolling()
                accumulatedSnapDelta = 0f
                snapHandler.updateSnapTarget(0, false)

                snapJob.cancel()
                snapJob = with(this) { async { snapHandler.snapToClosestItem(scrollLogic) } }
            }
        }
    }

    /**
     * Calculates a value based on the rotaryScrollDistance and size of snapThreshold. The closer
     * rotaryScrollDistance to snapThreshold, the lower the value.
     */
    private fun calculateProximityFactor(snapThreshold: Float): Float =
        1 - scrollProximityEasing.transform(rotaryScrollDistance.absoluteValue / snapThreshold)

    private fun edgeNotReached(snapDistanceInItems: Int): Boolean =
        (!snapHandler.topEdgeReached() && snapDistanceInItems < 0) ||
            (!snapHandler.bottomEdgeReached() && snapDistanceInItems > 0)

    private fun resetScrolling() {
        scrollHandler.cancelScrollIfActive()
        scrollHandler = scrollHandlerFactory()
        rotaryScrollDistance = 0f
    }

    private fun resetSnapping() {
        snapJob.cancel()
        snapHandler = snapHandlerFactory()
        accumulatedSnapDelta = 0f
    }

    private fun resetThresholdTracking(time: Long) {
        thresholdHandler = thresholdHandlerFactory()
        thresholdHandler.startThresholdTracking(time)
    }

    private fun isOppositeValueAfterScroll(delta: Float): Boolean =
        rotaryScrollDistance * delta < 0f && (abs(delta) < abs(rotaryScrollDistance))
}

/**
 * A scroll behavior for Bezel(low-res) input with snapping and without fling (see
 * [Modifier.rotaryScrollable] for descriptions of low-res and high-res devices ).
 *
 * This scroll behavior doesn't support fling.
 */
internal class LowResSnapRotaryScrollableBehavior(
    private val rotaryHaptics: RotaryHapticHandler,
    private val snapHandlerFactory: () -> RotarySnapHandler,
) : BaseRotaryScrollableBehavior() {

    private var snapJob: Job = CompletableDeferred<Unit>()

    private var accumulatedSnapDelta = 0f

    private var snapHandler = snapHandlerFactory()

    override suspend fun CoroutineScope.performScroll(
        timestampMillis: Long,
        delta: Float,
        inputDeviceId: Int,
        orientation: Orientation,
    ) {
        debugLog { "LowResSnapRotaryScrollableBehavior: performScroll" }

        if (isNewScrollEvent(timestampMillis)) {
            debugLog { "New scroll event" }
            resetSnapping()
        }
        accumulatedSnapDelta += delta

        debugLog { "Accumulated snap delta: $accumulatedSnapDelta" }

        previousScrollEventTime = timestampMillis

        if (abs(accumulatedSnapDelta) > 1f) {

            val snapDistanceInItems = sign(accumulatedSnapDelta).toInt()
            rotaryHaptics.handleSnapHaptic(timestampMillis, delta, inputDeviceId, AxisScroll)
            val sequentialSnap = snapJob.isActive
            debugLog {
                "Snap threshold reached: snapDistanceInItems:$snapDistanceInItems, " +
                    "sequentialSnap: $sequentialSnap, " +
                    "Accumulated snap delta: $accumulatedSnapDelta"
            }

            snapHandler.updateSnapTarget(snapDistanceInItems, sequentialSnap)
            if (!snapJob.isActive) {
                snapJob.cancel()
                snapJob =
                    with(this) {
                        async {
                            debugLog { "Snap started" }
                            try {
                                snapHandler.snapToTargetItem(scrollLogic)
                            } finally {
                                debugLog { "Snap called finally" }
                            }
                        }
                    }
            }
            accumulatedSnapDelta = 0f
        }
    }

    private fun resetSnapping() {
        snapJob.cancel()
        snapHandler = snapHandlerFactory()
        accumulatedSnapDelta = 0f
    }
}

/**
 * This class is responsible for determining the dynamic 'snapping' threshold. The threshold
 * dictates how much rotary input is required to trigger a snapping action.
 *
 * The threshold is calculated dynamically based on the user's scroll input velocity. Faster
 * scrolling results in a lower threshold, making snapping easier to achieve. An exponential
 * smoothing is also applied to the velocity readings to reduce noise and provide more consistent
 * threshold calculations.
 */
internal class ThresholdHandler(
    // Factor to divide item size when calculating threshold.
    // Threshold is divided by a linear interpolation value between minThresholdDivider and
    // maxThresholdDivider, based on the scrolling speed.
    private val minThresholdDivider: Float,
    private val maxThresholdDivider: Float,
    // Min velocity for threshold calculation
    private val minVelocity: Float = 300f,
    // Max velocity for threshold calculation
    private val maxVelocity: Float = 3000f,
    // Smoothing factor for velocity readings
    private val smoothingConstant: Float = 0.4f,
    private val averageItemSize: () -> Float,
) {
    private val thresholdDividerEasing: Easing = CubicBezierEasing(0.5f, 0.0f, 0.5f, 1.0f)

    private val rotaryVelocityTracker = RotaryVelocityTracker()

    private var smoothedVelocity = 0f

    /**
     * Resets tracking state in preparation for a new scroll event. Initiates the velocity tracker
     * and resets smoothed velocity.
     */
    fun startThresholdTracking(time: Long) {
        rotaryVelocityTracker.start(time)
        smoothedVelocity = 0f
    }

    /** Updates the velocity tracker with the latest rotary input data. */
    fun updateTracking(timestamp: Long, delta: Float) {
        rotaryVelocityTracker.move(timestamp, delta)
        applySmoothing()
    }

    /**
     * Calculates the dynamic snapping threshold based on the current smoothed velocity.
     *
     * @return The threshold, in pixels, required to trigger a snapping action.
     */
    fun calculateSnapThreshold(): Float {
        // Calculate a divider fraction based on the smoothedVelocity within the defined range.
        val thresholdDividerFraction =
            thresholdDividerEasing.transform(
                inverseLerp(minVelocity, maxVelocity, smoothedVelocity)
            )
        // Calculate the final threshold size by dividing the average item size by a dynamically
        // adjusted threshold divider.
        return averageItemSize() /
            lerp(minThresholdDivider, maxThresholdDivider, thresholdDividerFraction)
    }

    /**
     * Applies exponential smoothing to the tracked velocity to reduce noise and provide more
     * consistent threshold calculations.
     */
    private fun applySmoothing() {
        if (rotaryVelocityTracker.velocity != 0.0f) {
            // smooth the velocity
            smoothedVelocity =
                exponentialSmoothing(
                    currentVelocity = rotaryVelocityTracker.velocity.absoluteValue,
                    prevVelocity = smoothedVelocity,
                    smoothingConstant = smoothingConstant,
                )
        }
        debugLog { "rotaryVelocityTracker velocity: ${rotaryVelocityTracker.velocity}" }
        debugLog { "SmoothedVelocity: $smoothedVelocity" }
    }

    private fun exponentialSmoothing(
        currentVelocity: Float,
        prevVelocity: Float,
        smoothingConstant: Float,
    ): Float = smoothingConstant * currentVelocity + (1 - smoothingConstant) * prevVelocity
}

private class RotaryHandlerElement(
    private val behavior: RotaryScrollableBehavior,
    private val overscrollEffect: OverscrollEffect?,
    private val reverseDirection: Boolean,
    private val inspectorInfo: InspectorInfo.() -> Unit,
) : ModifierNodeElement<RotaryInputNode>() {
    override fun create(): RotaryInputNode =
        RotaryInputNode(behavior, overscrollEffect, reverseDirection)

    override fun update(node: RotaryInputNode) {
        debugLog { "Update launched!" }
        node.behavior = behavior
        node.overscrollEffect = overscrollEffect
        node.reverseDirection = reverseDirection
        node.updateScrollLogic()
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RotaryHandlerElement

        if (behavior != other.behavior) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (reverseDirection != other.reverseDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = behavior.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        return result
    }
}

private class RotaryInputNode(
    var behavior: RotaryScrollableBehavior,
    var overscrollEffect: OverscrollEffect?,
    var reverseDirection: Boolean,
) : RotaryInputModifierNode, DelegatingNode() {

    val channel = Channel<RotaryScrollEvent>(capacity = Channel.CONFLATED)
    val flow = channel.receiveAsFlow()
    val nestedScrollDispatcher = NestedScrollDispatcher()
    val nestedScrollConnection = object : NestedScrollConnection {}

    val scrollLogic = RotaryScrollLogic(overscrollEffect, nestedScrollDispatcher, reverseDirection)

    init {
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))
    }

    override fun onAttach() {
        updateScrollLogic()
        coroutineScope.launch {
            flow.collectLatest { event ->
                val (orientation: Orientation, deltaInPixels: Float) =
                    if (event.verticalScrollPixels != 0.0f)
                        Pair(Vertical, event.verticalScrollPixels)
                    else Pair(Horizontal, event.horizontalScrollPixels)
                debugLog {
                    "Scroll event received: " +
                        "delta:$deltaInPixels, timestamp:${event.uptimeMillis}"
                }
                scrollLogic.orientation = orientation
                with(behavior) {
                    performScroll(
                        timestampMillis = event.uptimeMillis,
                        // TODO(b/397650406): Implement a more efficient way to reverse the scroll
                        // direction
                        delta = deltaInPixels * if (reverseDirection) -1f else 1f,
                        inputDeviceId = event.inputDeviceId,
                        orientation = orientation,
                    )
                }
            }
        }
    }

    override fun onRotaryScrollEvent(event: RotaryScrollEvent): Boolean = false

    override fun onPreRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
        debugLog { "onPreRotaryScrollEvent" }
        channel.trySend(event)
        return true
    }

    internal fun updateScrollLogic() {
        (behavior as? BaseRotaryScrollableBehavior)?.let { it.scrollLogic = scrollLogic }
    }
}

/**
 * Enum class representing the sensitivity of the rotary scroll.
 *
 * It defines two types of parameters that influence scroll behavior:
 * - min/max thresholdDivider : these parameters reduce the scroll threshold based on the speed of
 *   rotary input, making the UI more responsive to both slow, deliberate rotations and fast flicks
 *   of the rotary.
 * - resistanceFactor : Used to dampen the visual scroll effect. This allows the UI to scroll less
 *   than the actual input from the rotary device, providing a more controlled scrolling experience.
 */
internal enum class RotarySnapSensitivity(
    val minThresholdDivider: Float,
    val maxThresholdDivider: Float,
    val resistanceFactor: Float,
) {
    // Default sensitivity
    DEFAULT(1f, 1.5f, 3f),

    // Used for full-screen pagers
    HIGH(5f, 7.5f, 5f),
}

private val ScrollableState.shouldDispatchOverscroll
    get() = canScrollForward || canScrollBackward

private val ScrollableState.atTheEdge
    get() = !canScrollForward || !canScrollBackward

private const val AxisScroll = MotionEvent.AXIS_SCROLL

private const val RotaryInputSource = InputDevice.SOURCE_ROTARY_ENCODER

/** Debug logging that can be enabled. */
private const val DEBUG = false

private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("RotaryScroll: ${generateMsg()}")
    }
}
