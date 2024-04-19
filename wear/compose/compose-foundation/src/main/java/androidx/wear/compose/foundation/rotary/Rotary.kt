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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.inverseLerp
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
 * A modifier which connects rotary events with scrollable containers such as Column,
 * LazyList and others. [ScalingLazyColumn] has a build-in rotary support, and accepts
 * [rotaryBehavior] parameter directly.
 *
 * This modifier supports rotary scrolling and snapping.
 * The behaviour is configured by the provided [RotaryBehavior]:
 * either provide [RotaryDefaults.scrollBehavior] for scrolling with/without fling
 * or pass [RotaryDefaults.snapBehavior] when snap is required.
 *
 * Example of scrolling with fling:
 * @sample androidx.wear.compose.foundation.samples.RotaryScrollSample
 *
 * Example of scrolling with snap:
 * @sample androidx.wear.compose.foundation.samples.RotarySnapSample
 *
 * @param rotaryBehavior Specified [RotaryBehavior] for rotary handling with snap or fling.
 * @param focusRequester Used to request the focus for rotary input. Each composable with this
 * modifier should have a separate focusRequester, and only one of them at a time can be active.
 * @param reverseDirection Reverse the direction of scrolling if required for consistency
 * with the scrollable state passed via [rotaryBehavior].
 */
fun Modifier.rotary(
    rotaryBehavior: RotaryBehavior,
    focusRequester: FocusRequester,
    reverseDirection: Boolean = false
): Modifier =
    rotaryHandler(
        rotaryBehavior = rotaryBehavior,
        reverseDirection = reverseDirection,
    )
        .focusRequester(focusRequester)
        .focusable()

/**
 * An interface for handling scroll events. Has implementations for handling scroll
 * with/without fling [RotaryScrollBehavior] and for handling snap [LowResRotarySnapBehavior],
 * [HighResRotarySnapBehavior].
 */
interface RotaryBehavior {

    /**
     * Handles scrolling events.
     *
     * @param timestamp The time in milliseconds at which this even occurred
     * @param deltaInPixels The amount to scroll (in pixels)
     * @param deviceId The id for the input device that this event came from
     * @param orientation Orientation of the scrolling
     */
    suspend fun CoroutineScope.handleScrollEvent(
        timestamp: Long,
        deltaInPixels: Float,
        deviceId: Int,
        orientation: Orientation
    )
}

/**
 * An adapter which connects scrollableState to a rotary input for snapping scroll actions.
 *
 * This interface defines the essential properties and methods required for a scrollable
 * to be controlled by rotary input and perform a snap action.
 *
 */
interface RotaryScrollableAdapter {

    /**
     * The scrollable state used for performing scroll actions in response to rotary events.
     */
    val scrollableState: ScrollableState

    /**
     * Calculates the average size of an item within the scrollable. This is used to
     * estimate scrolling distances for snapping when responding to rotary input.
     *
     * @return The average item size in pixels.
     */
    fun averageItemSize(): Float

    /**
     * Returns the index of the item that is closest to the center.
     */
    fun currentItemIndex(): Int

    /**
     * Returns the offset of the currently centered item from its centered position.
     * This value can be positive or negative.
     *
     * @return The offset of the current item in pixels.
     */
    fun currentItemOffset(): Float

    /**
     * The total number of items within the scrollable in [scrollableState]
     */
    fun totalItemsCount(): Int
}

/**
 * Defaults for rotary modifiers
 */
object RotaryDefaults {

    /**
     * Implementation of [RotaryBehavior] to define scrolling behaviour with or without fling -
     * used with the [rotary] modifier when snapping is not required.
     *
     * If fling is not required, set [flingBehavior] = null. In that case,
     * flinging will not happen and the scrollable content will
     * stop scrolling immediately after the user stops interacting with rotary input.
     *
     * @param scrollableState Scrollable state which will be scrolled
     * while receiving rotary events.
     * @param flingBehavior Optional rotary fling behavior, pass null to
     * turn off fling if necessary.
     * @param hapticFeedbackEnabled Controls whether haptic feedback is given during rotary
     * scrolling (true by default). It's recommended to keep the default value of true
     * for premium scrolling experience.
     */
    @Composable
    fun scrollBehavior(
        scrollableState: ScrollableState,
        flingBehavior: FlingBehavior? = ScrollableDefaults.flingBehavior(),
        hapticFeedbackEnabled: Boolean = true
    ): RotaryBehavior {
        val isLowRes = isLowResInput()
        val viewConfiguration = ViewConfiguration.get(LocalContext.current)
        val rotaryHaptics: RotaryHapticHandler =
            rememberRotaryHapticHandler(scrollableState, hapticFeedbackEnabled)

        return flingBehavior(
            scrollableState,
            rotaryHaptics,
            flingBehavior,
            isLowRes,
            viewConfiguration
        )
    }

    /**
     * Implementation of [RotaryBehavior] to define scrolling behaviour with snap -
     * used with the [rotary] modifier when snapping is required.
     *
     * @param rotaryScrollableAdapter A connection between scrollable entities and rotary events.
     * @param snapOffset An optional offset to be applied when snapping the item.
     * After snapping, defines the offset to the center of the item.
     * @param hapticFeedbackEnabled Controls whether haptic feedback is given during
     * rotary scrolling (true by default). It's recommended to keep the default value of true
     * for premium scrolling experience.
     */
    @Composable
    fun snapBehavior(
        rotaryScrollableAdapter: RotaryScrollableAdapter,
        snapOffset: Int = SnapOffset,
        hapticFeedbackEnabled: Boolean = true
    ): RotaryBehavior {
        val isLowRes = isLowResInput()
        val rotaryHaptics: RotaryHapticHandler =
            rememberRotaryHapticHandler(
                rotaryScrollableAdapter.scrollableState,
                hapticFeedbackEnabled
            )

        return remember(rotaryScrollableAdapter, rotaryHaptics, snapOffset, isLowRes) {
            snapBehavior(
                rotaryScrollableAdapter,
                rotaryHaptics,
                snapOffset,
                ThresholdDivider,
                ResistanceFactor,
                isLowRes
            )
        }
    }

    /**
     * Implementation of [RotaryBehavior] to define scrolling behaviour with snap for
     * [ScalingLazyColumn] - used with the [rotary] modifier when snapping is required.
     *
     * @param state [ScalingLazyListState] to which rotary scroll will be connected.
     * @param snapOffset An optional offset to be applied when snapping the item.
     * After snapping, defines the offset to the center of the item.
     * @param hapticFeedbackEnabled Controls whether haptic feedback is given during
     * rotary scrolling (true by default). It's recommended to keep the default value of true
     * for premium scrolling experience.
     */
    @Composable
    fun snapBehavior(
        state: ScalingLazyListState,
        snapOffset: Int = SnapOffset,
        hapticFeedbackEnabled: Boolean = true
    ): RotaryBehavior = snapBehavior(
        rotaryScrollableAdapter = remember(state) {
            ScalingLazyColumnRotaryScrollableAdapter(state)
        },
        snapOffset = snapOffset,
        hapticFeedbackEnabled = hapticFeedbackEnabled
    )

    /**
     * Returns whether the input is Low-res (a bezel) or high-res (a crown/rsb).
     */
    @Composable
    private fun isLowResInput(): Boolean = LocalContext.current.packageManager
        .hasSystemFeature("android.hardware.rotaryencoder.lowres")

    private const val SnapOffset: Int = 0
    private const val ThresholdDivider: Float = 1.5f
    private const val ResistanceFactor: Float = 3f

    // These values represent the timeframe for a fling event. A bigger value is assigned
    // to low-res input due to the lower frequency of low-res rotary events.
    internal const val LowResFlingTimeframe: Long = 100L
    internal const val HighResFlingTimeframe: Long = 30L
}

/**
 * An implementation of rotary scroll adapter for ScalingLazyColumn
 */
internal class ScalingLazyColumnRotaryScrollableAdapter(
    override val scrollableState: ScalingLazyListState
) : RotaryScrollableAdapter {

    /**
     * Calculates the average item height by averaging the height of visible items.
     */
    override fun averageItemSize(): Float {
        val visibleItems = scrollableState.layoutInfo.visibleItemsInfo
        return (visibleItems.fastSumBy { it.unadjustedSize } / visibleItems.size).toFloat()
    }

    /**
     * Current (centered) item index
     */
    override fun currentItemIndex(): Int = scrollableState.centerItemIndex

    /**
     * The offset from the item center.
     */
    override fun currentItemOffset(): Float = scrollableState.centerItemScrollOffset.toFloat()

    /**
     * The total count of items in ScalingLazyColumn
     */
    override fun totalItemsCount(): Int = scrollableState.layoutInfo.totalItemsCount
}

/**
 * Handles scroll with fling.
 *
 * @return A scroll with fling implementation of [RotaryBehavior] which is suitable
 * for both low-res and high-res inputs.
 *
 * @param scrollableState Scrollable state which will be scrolled while receiving rotary events
 * @param flingBehavior Logic describing Fling behavior. If null - fling will not happen
 * @param isLowRes Whether the input is Low-res (a bezel) or high-res(a crown/rsb)
 * @param viewConfiguration [ViewConfiguration] for accessing default fling values
 */
private fun flingBehavior(
    scrollableState: ScrollableState,
    rotaryHaptics: RotaryHapticHandler,
    flingBehavior: FlingBehavior? = null,
    isLowRes: Boolean,
    viewConfiguration: ViewConfiguration
): RotaryBehavior {

    fun rotaryFlingHandler() = flingBehavior?.run {
        RotaryFlingHandler(
            scrollableState,
            flingBehavior,
            viewConfiguration,
            flingTimeframe = if (isLowRes) RotaryDefaults.LowResFlingTimeframe
            else RotaryDefaults.HighResFlingTimeframe
        )
    }

    fun scrollHandler() = RotaryScrollHandler(scrollableState)

    return RotaryScrollBehavior(
        isLowRes,
        rotaryHaptics,
        rotaryFlingHandlerFactory = { rotaryFlingHandler() },
        scrollHandlerFactory = { scrollHandler() }
    )
}

/**
 * Handles scroll with snap.
 *
 * @return A snap implementation of [RotaryBehavior] which is either suitable for low-res or
 * high-res input.
 *
 * @param rotaryScrollableAdapter Implementation of [RotaryScrollableAdapter], which connects
 * scrollableState to a rotary input for snapping scroll actions.
 * @param rotaryHaptics Implementation of [RotaryHapticHandler] which handles haptics
 * for rotary usage
 * @param snapOffset An offset to be applied when snapping the item. After the snap the
 * snapped items offset will be [snapOffset].
 * @param maxThresholdDivider Factor to divide item size when calculating threshold.
 * @param scrollDistanceDivider A value which is used to slow down or
 * speed up the scroll before snap happens. The higher the value the slower the scroll.
 * @param isLowRes Whether the input is Low-res (a bezel) or high-res(a crown/rsb)
 */
private fun snapBehavior(
    rotaryScrollableAdapter: RotaryScrollableAdapter,
    rotaryHaptics: RotaryHapticHandler,
    snapOffset: Int,
    maxThresholdDivider: Float,
    scrollDistanceDivider: Float,
    isLowRes: Boolean
): RotaryBehavior {
    return if (isLowRes) {
        LowResRotarySnapBehavior(
            rotaryHaptics = rotaryHaptics,
            snapHandlerFactory = {
                RotarySnapHandler(
                    rotaryScrollableAdapter,
                    snapOffset,
                )
            }
        )
    } else {
        HighResRotarySnapBehavior(
            rotaryHaptics = rotaryHaptics,
            scrollDistanceDivider = scrollDistanceDivider,
            thresholdHandlerFactory = {
                ThresholdHandler(
                    maxThresholdDivider,
                    averageItemSize = { rotaryScrollableAdapter.averageItemSize() }
                )
            },
            snapHandlerFactory = {
                RotarySnapHandler(
                    rotaryScrollableAdapter,
                    snapOffset,
                )
            },
            scrollHandlerFactory = {
                RotaryScrollHandler(rotaryScrollableAdapter.scrollableState)
            }
        )
    }
}

/**
 * An abstract base class for handling scroll events. Has implementations for handling scroll
 * with/without fling [RotaryScrollBehavior] and for handling snap [LowResRotarySnapBehavior],
 * [HighResRotarySnapBehavior].
 */
internal abstract class BaseRotaryBehavior : RotaryBehavior {

    // Threshold for detection of a new gesture
    private val gestureThresholdTime = 200L
    protected var previousScrollEventTime = -1L

    protected fun isNewScrollEvent(timestamp: Long): Boolean {
        val timeDelta = timestamp - previousScrollEventTime
        return previousScrollEventTime == -1L || timeDelta > gestureThresholdTime
    }
}

/**
 * This class does a smooth animation when the scroll by N pixels is done.
 * This animation works well on Rsb(high-res) and Bezel(low-res) devices.
 */
internal class RotaryScrollHandler(
    private val scrollableState: ScrollableState
) {
    private var sequentialAnimation = false
    private var scrollAnimation = AnimationState(0f)
    private var prevPosition = 0f
    private var scrollJob: Job = CompletableDeferred<Unit>()

    /**
     * Produces scroll to [targetValue]
     */
    fun scrollToTarget(coroutineScope: CoroutineScope, targetValue: Float) {
        cancelScrollIfActive()

        scrollJob = coroutineScope.async {
            scrollTo(targetValue)
        }
    }

    fun cancelScrollIfActive() {
        if (scrollJob.isActive) scrollJob.cancel()
    }

    private suspend fun scrollTo(targetValue: Float) {
        scrollableState.scroll(MutatePriority.UserInput) {
            debugLog { "ScrollAnimation value before start: ${scrollAnimation.value}" }

            scrollAnimation.animateTo(
                targetValue,
                animationSpec = spring(),
                sequentialAnimation = sequentialAnimation
            ) {
                val delta = value - prevPosition
                debugLog { "Animated by $delta, value: $value" }
                scrollBy(delta)
                prevPosition = value
                sequentialAnimation = value != this.targetValue
            }
        }
    }
}

/**
 * A helper class for snapping with rotary.
 */
internal class RotarySnapHandler(
    private val rotaryScrollableAdapter: RotaryScrollableAdapter,
    private val snapOffset: Int,
) {
    private var snapTarget: Int = rotaryScrollableAdapter.currentItemIndex()
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
     * If [sequentialSnap] is true, items are summed up together.
     * For example, if [updateSnapTarget] is called with
     * [moveForElements] = 2, 3, 5 -> then the snapping will happen to current + 10 items
     *
     * If [sequentialSnap] is false, then [moveForElements] are not summed up together.
     */
    fun updateSnapTarget(moveForElements: Int, sequentialSnap: Boolean) {
        this.sequentialSnap = sequentialSnap
        if (sequentialSnap) {
            snapTarget += moveForElements
        } else {
            snapTarget = rotaryScrollableAdapter.currentItemIndex() + moveForElements
        }
        snapTargetUpdated = true
        snapTarget = snapTarget
            .coerceIn(0 until rotaryScrollableAdapter.totalItemsCount())
    }

    /**
     * Performs snapping to the closest item.
     */
    suspend fun snapToClosestItem() {
        // Perform the snapping animation
        rotaryScrollableAdapter.scrollableState.scroll(MutatePriority.UserInput) {
            debugLog { "snap to the closest item" }
            var prevPosition = 0f

            // Create and execute the snap animation
            AnimationState(0f).animateTo(
                targetValue = -rotaryScrollableAdapter.currentItemOffset(),
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
            ) {
                val animDelta = value - prevPosition
                scrollBy(animDelta)
                prevPosition = value
            }
            // Update the snap target to ensure consistency
            snapTarget = rotaryScrollableAdapter.currentItemIndex()
        }
    }

    /**
     * Returns true if top edge was reached
     */
    fun topEdgeReached(): Boolean = snapTarget <= 0

    /**
     * Returns true if bottom edge was reached
     */
    fun bottomEdgeReached(): Boolean =
        snapTarget >= rotaryScrollableAdapter.totalItemsCount() - 1

    /**
     * Performs snapping to the specified in [updateSnapTarget] element
     */
    suspend fun snapToTargetItem() {
        if (!sequentialSnap) anim = AnimationState(0f)

        rotaryScrollableAdapter.scrollableState.scroll(MutatePriority.UserInput) {
            // If snapTargetUpdated is true -means the target was updated so we
            // need to do snap animation again
            while (snapTargetUpdated) {
                snapTargetUpdated = false
                var latestCenterItem: Int
                var continueFirstScroll = true
                debugLog { "snapTarget $snapTarget" }

                // First part of animation. Performing it until the target element centered.
                while (continueFirstScroll) {
                    latestCenterItem = rotaryScrollableAdapter.currentItemIndex()
                    expectedDistance = expectedDistanceTo(snapTarget, snapOffset)
                    debugLog {
                        "expectedDistance = $expectedDistance, " +
                            "scrollableState.centerItemScrollOffset " +
                            "${rotaryScrollableAdapter.currentItemOffset()}"
                    }

                    continueFirstScroll = false
                    var prevPosition = anim.value
                    anim.animateTo(
                        prevPosition + expectedDistance,
                        animationSpec = spring(
                            stiffness = defaultStiffness,
                            visibilityThreshold = 0.1f
                        ),
                        sequentialAnimation = (anim.velocity != 0f)
                    ) {
                        // Exit animation if snap target was updated
                        if (snapTargetUpdated) cancelAnimation()

                        val animDelta = value - prevPosition
                        debugLog {
                            "First animation, value:$value, velocity:$velocity, " +
                                "animDelta:$animDelta"
                        }
                        scrollBy(animDelta)
                        prevPosition = value

                        if (latestCenterItem != rotaryScrollableAdapter.currentItemIndex()) {
                            continueFirstScroll = true
                            cancelAnimation()
                            return@animateTo
                        }

                        debugLog {
                            "centerItemIndex =  ${rotaryScrollableAdapter.currentItemIndex()}"
                        }
                        if (rotaryScrollableAdapter.currentItemIndex() == snapTarget) {
                            debugLog { "Target is near the centre. Cancelling first animation" }
                            debugLog {
                                "scrollableState.centerItemScrollOffset " +
                                    "${rotaryScrollableAdapter.currentItemOffset()}"
                            }
                            expectedDistance = -rotaryScrollableAdapter.currentItemOffset()
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
                    animationSpec = SpringSpec(
                        stiffness = defaultStiffness,
                        visibilityThreshold = 0.1f
                    ),
                    sequentialAnimation = (anim.velocity != 0f)
                ) {
                    // Exit animation if snap target was updated
                    if (snapTargetUpdated) cancelAnimation()

                    val animDelta = value - prevPosition
                    debugLog { "Final animation. velocity:$velocity, animDelta:$animDelta" }
                    scrollBy(animDelta)
                    prevPosition = value
                }
            }
        }
    }

    private fun expectedDistanceTo(index: Int, targetScrollOffset: Int): Float {
        val averageSize = rotaryScrollableAdapter.averageItemSize()
        val indexesDiff = index - rotaryScrollableAdapter.currentItemIndex()
        debugLog { "Average size $averageSize" }
        return (averageSize * indexesDiff) +
            targetScrollOffset - rotaryScrollableAdapter.currentItemOffset()
    }
}

/**
 * A modifier which handles rotary events.
 * It accepts [RotaryBehavior] as the input - a class that handles the main scroll logic.
 */
internal fun Modifier.rotaryHandler(
    rotaryBehavior: RotaryBehavior,
    reverseDirection: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit = debugInspectorInfo {
        name = "rotaryHandler"
        properties["rotaryBehavior"] = rotaryBehavior
        properties["reverseDirection"] = reverseDirection
    }
): Modifier = this then RotaryHandlerElement(
    rotaryBehavior,
    reverseDirection,
    inspectorInfo
)

/**
 * Class responsible for Fling behaviour with rotary.
 * It tracks rotary events and produces fling when necessary.
 * @param flingTimeframe represents a time interval (in milliseconds) used to determine
 * whether a rotary input should trigger a fling. If no new events come during this interval,
 * then the fling is triggered.
 */
internal class RotaryFlingHandler(
    private val scrollableState: ScrollableState,
    private val flingBehavior: FlingBehavior,
    viewConfiguration: ViewConfiguration,
    private val flingTimeframe: Long
) {
    private var flingJob: Job = CompletableDeferred<Unit>()

    // A time range during which the fling is valid.
    // For simplicity it's twice as long as [flingTimeframe]
    private val timeRangeToFling = flingTimeframe * 2

    //  A default fling factor for making fling slower
    private val flingScaleFactor = 0.7f

    private var previousVelocity = 0f

    private val rotaryVelocityTracker = RotaryVelocityTracker()

    private val minFlingSpeed = viewConfiguration.scaledMinimumFlingVelocity.toFloat()
    private val maxFlingSpeed = viewConfiguration.scaledMaximumFlingVelocity.toFloat()
    private var latestEventTimestamp: Long = 0

    private var flingVelocity: Float = 0f
    private var flingTimestamp: Long = 0

    /**
     * Starts a new fling tracking session
     * with specified timestamp
     */
    fun startFlingTracking(timestamp: Long) {
        rotaryVelocityTracker.start(timestamp)
        latestEventTimestamp = timestamp
        previousVelocity = 0f
    }

    fun cancelFlingIfActive() {
        if (flingJob.isActive) flingJob.cancel()
    }

    /**
     * Observing new event within a fling tracking session with new timestamp and delta
     */
    fun observeEvent(timestamp: Long, delta: Float) {
        rotaryVelocityTracker.move(timestamp, delta)
        latestEventTimestamp = timestamp
    }

    fun performFlingIfRequired(
        coroutineScope: CoroutineScope,
        beforeFling: () -> Unit,
        edgeReached: (velocity: Float) -> Unit
    ) {
        cancelFlingIfActive()

        flingJob = coroutineScope.async {
            trackFling(beforeFling, edgeReached)
        }
    }

    /**
     * Performing fling if necessary and calling [beforeFling] lambda before it is triggered.
     * [edgeReached] is called when the scroll reaches the end of the list and can't scroll further
     */
    private suspend fun trackFling(
        beforeFling: () -> Unit,
        edgeReached: (velocity: Float) -> Unit
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
        if (latestEventTimestamp - flingTimestamp < timeRangeToFling &&
            abs(flingVelocity) > minFlingSpeed
        ) {
            // Call beforeFling because a fling will be performed
            beforeFling()
            val velocity = flingVelocity.coerceIn(-maxFlingSpeed, maxFlingSpeed)
            scrollableState.scroll(MutatePriority.UserInput) {
                with(flingBehavior) {
                    debugLog { "Flinging with velocity $velocity" }
                    val remainedVelocity = performFling(velocity)
                    debugLog { "-- Velocity after fling: $remainedVelocity" }
                    if (remainedVelocity != 0.0f) {
                        edgeReached(remainedVelocity)
                    }
                }
            }
        }
    }
}

/**
 * A scroll behavior for scrolling without snapping and with or without fling.
 * A list is scrolled by the number of pixels received from the rotary device.
 *
 * For a high-res input it has a filtering for events which are coming
 * with an opposite sign (this might happen to devices with rsb,
 * especially at the end of the scroll )
 *
 * This scroll behavior supports fling. It can be set with [RotaryFlingHandler].
 */
internal class RotaryScrollBehavior(
    private val isLowRes: Boolean,
    private val rotaryHaptics: RotaryHapticHandler,
    private val rotaryFlingHandlerFactory: () -> RotaryFlingHandler?,
    private val scrollHandlerFactory: () -> RotaryScrollHandler,
) : BaseRotaryBehavior() {
    private var rotaryScrollDistance = 0f

    private var rotaryFlingHandler: RotaryFlingHandler? = rotaryFlingHandlerFactory()
    private var scrollHandler: RotaryScrollHandler = scrollHandlerFactory()

    override suspend fun CoroutineScope.handleScrollEvent(
        timestamp: Long,
        deltaInPixels: Float,
        deviceId: Int,
        orientation: Orientation
    ) {
        debugLog { "RotaryScrollHandler: handleScrollEvent" }

        if (isNewScrollEvent(timestamp)) {
            debugLog { "New scroll event" }
            resetScrolling()
            resetFlingTracking(timestamp)
        } else {
            // Due to the physics of high-res Rotary side button, some events might come
            // with an opposite axis value - either at the start or at the end of the motion.
            // We don't want to use these values for fling calculations.
            if (isLowRes || !isOppositeValueAfterScroll(deltaInPixels)) {
                rotaryFlingHandler?.observeEvent(timestamp, deltaInPixels)
            } else {
                debugLog { "Opposite value after scroll :$deltaInPixels" }
            }
        }

        rotaryScrollDistance += deltaInPixels
        debugLog { "Rotary scroll distance: $rotaryScrollDistance" }

        rotaryHaptics.handleScrollHaptic(timestamp, deltaInPixels)

        previousScrollEventTime = timestamp
        scrollHandler.scrollToTarget(this, rotaryScrollDistance)

        rotaryFlingHandler?.performFlingIfRequired(
            this,
            beforeFling = {
                debugLog { "Calling beforeFling section" }
                resetScrolling()
            },
            edgeReached = { velocity ->
                rotaryHaptics.handleLimitHaptic(velocity > 0f)
            }
        )
    }

    private fun resetScrolling() {
        scrollHandler.cancelScrollIfActive()
        scrollHandler = scrollHandlerFactory()
        rotaryScrollDistance = 0f
    }

    private fun resetFlingTracking(timestamp: Long) {
        rotaryFlingHandler?.cancelFlingIfActive()
        rotaryFlingHandler = rotaryFlingHandlerFactory()
        rotaryFlingHandler?.startFlingTracking(timestamp)
    }

    private fun isOppositeValueAfterScroll(delta: Float): Boolean =
        rotaryScrollDistance * delta < 0f &&
            (abs(delta) < abs(rotaryScrollDistance))
}

/**
 * A scroll behavior for RSB(high-res) input with snapping and without fling.
 *
 * Threshold for snapping is set dynamically in ThresholdBehavior, which depends
 * on the scroll speed and the average size of the items.
 *
 * This scroll handler doesn't support fling.
 */
internal class HighResRotarySnapBehavior(
    private val rotaryHaptics: RotaryHapticHandler,
    private val scrollDistanceDivider: Float,
    private val thresholdHandlerFactory: () -> ThresholdHandler,
    private val snapHandlerFactory: () -> RotarySnapHandler,
    private val scrollHandlerFactory: () -> RotaryScrollHandler
) : BaseRotaryBehavior() {
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

    override suspend fun CoroutineScope.handleScrollEvent(
        timestamp: Long,
        deltaInPixels: Float,
        deviceId: Int,
        orientation: Orientation
    ) {
        debugLog { "HighResSnapHandler: handleScrollEvent" }

        if (isNewScrollEvent(timestamp)) {
            debugLog { "New scroll event" }
            resetScrolling()
            resetSnapping()
            resetThresholdTracking(timestamp)
        }

        if (!isOppositeValueAfterScroll(deltaInPixels)) {
            thresholdHandler.updateTracking(timestamp, deltaInPixels)
        } else {
            debugLog { "Opposite value after scroll :$deltaInPixels" }
        }

        val snapThreshold = thresholdHandler.calculateSnapThreshold()
        debugLog { "snapThreshold: $snapThreshold" }

        if (!snapJob.isActive) {
            val proximityFactor = calculateProximityFactor(snapThreshold)
            rotaryScrollDistance += deltaInPixels * proximityFactor
        }
        debugLog { "Rotary scroll distance: $rotaryScrollDistance" }

        accumulatedSnapDelta += deltaInPixels
        debugLog { "Accumulated snap delta: $accumulatedSnapDelta" }

        previousScrollEventTime = timestamp

        if (abs(accumulatedSnapDelta) > snapThreshold) {
            resetScrolling()

            // We limit a number of handled snap items per event to [maxSnapsPerEvent],
            // as otherwise the snap might happen too quickly
            val snapDistanceInItems = (accumulatedSnapDelta / snapThreshold).toInt()
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
                rotaryHaptics.handleSnapHaptic(timestamp, deltaInPixels)
            }

            snapHandler.updateSnapTarget(snapDistanceInItems, sequentialSnap)
            if (!snapJob.isActive) {
                snapJob.cancel()
                snapJob = with(this) {
                    async {
                        debugLog { "Snap started" }
                        try {
                            snapHandler.snapToTargetItem()
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

                scrollHandler.scrollToTarget(this, distanceWithDivider)
                delay(snapDelay)

                resetScrolling()
                accumulatedSnapDelta = 0f
                snapHandler.updateSnapTarget(0, false)

                snapJob.cancel()
                snapJob = with(this) {
                    async {
                        snapHandler.snapToClosestItem()
                    }
                }
            }
        }
    }

    /**
     * Calculates a value based on the rotaryScrollDistance and size of snapThreshold.
     * The closer rotaryScrollDistance to snapThreshold, the lower the value.
     */
    private fun calculateProximityFactor(snapThreshold: Float): Float =
        1 - scrollProximityEasing
            .transform(rotaryScrollDistance.absoluteValue / snapThreshold)

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
        rotaryScrollDistance * delta < 0f &&
            (abs(delta) < abs(rotaryScrollDistance))
}

/**
 * A scroll behavior for Bezel(low-res) input with snapping and without fling
 *
 * This scroll behavior doesn't support fling.
 */
internal class LowResRotarySnapBehavior(
    private val rotaryHaptics: RotaryHapticHandler,
    private val snapHandlerFactory: () -> RotarySnapHandler
) : BaseRotaryBehavior() {

    private var snapJob: Job = CompletableDeferred<Unit>()

    private var accumulatedSnapDelta = 0f

    private var snapHandler = snapHandlerFactory()

    override suspend fun CoroutineScope.handleScrollEvent(
        timestamp: Long,
        deltaInPixels: Float,
        deviceId: Int,
        orientation: Orientation
    ) {
        debugLog { "LowResSnapHandler: handleScrollEvent" }

        if (isNewScrollEvent(timestamp)) {
            debugLog { "New scroll event" }
            resetSnapping()
        }

        accumulatedSnapDelta += deltaInPixels

        debugLog { "Accumulated snap delta: $accumulatedSnapDelta" }

        previousScrollEventTime = timestamp

        if (abs(accumulatedSnapDelta) > 1f) {

            val snapDistanceInItems = sign(accumulatedSnapDelta).toInt()
            rotaryHaptics.handleSnapHaptic(timestamp, deltaInPixels)
            val sequentialSnap = snapJob.isActive
            debugLog {
                "Snap threshold reached: snapDistanceInItems:$snapDistanceInItems, " +
                    "sequentialSnap: $sequentialSnap, " +
                    "Accumulated snap delta: $accumulatedSnapDelta"
            }

            snapHandler.updateSnapTarget(snapDistanceInItems, sequentialSnap)
            if (!snapJob.isActive) {
                snapJob.cancel()
                snapJob = with(this) {
                    async {
                        debugLog { "Snap started" }
                        try {
                            snapHandler.snapToTargetItem()
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
 *  This class is responsible for determining the dynamic 'snapping' threshold.
 *  The threshold dictates how much rotary input is required to trigger a snapping action.
 *
 *  The threshold is calculated dynamically based on the user's scroll input velocity.
 *  Faster scrolling results in a lower threshold, making snapping easier to achieve.
 *  An exponential smoothing is also applied to the velocity readings to reduce noise
 *  and provide more consistent threshold calculations.
 */
internal class ThresholdHandler(
    // Factor to divide item size when calculating threshold.
    // Depending on the speed, it dynamically varies in range 1..maxThresholdDivider
    private val maxThresholdDivider: Float,
    // Min velocity for threshold calculation
    private val minVelocity: Float = 300f,
    // Max velocity for threshold calculation
    private val maxVelocity: Float = 3000f,
    // Smoothing factor for velocity readings
    private val smoothingConstant: Float = 0.4f,
    private val averageItemSize: () -> Float
) {
    private val thresholdDividerEasing: Easing = CubicBezierEasing(0.5f, 0.0f, 0.5f, 1.0f)

    private val rotaryVelocityTracker = RotaryVelocityTracker()

    private var smoothedVelocity = 0f

    /**
     *  Resets tracking state in preparation for a new scroll event.
     *  Initiates the velocity tracker and resets smoothed velocity.
     */
    fun startThresholdTracking(time: Long) {
        rotaryVelocityTracker.start(time)
        smoothedVelocity = 0f
    }

    /**
     * Updates the velocity tracker with the latest rotary input data.
     */
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
                inverseLerp(
                    minVelocity,
                    maxVelocity,
                    smoothedVelocity
                )
            )
        // Calculate the final threshold size by dividing the average item size by a dynamically
        // adjusted threshold divider.
        return averageItemSize() / lerp(
            1f,
            maxThresholdDivider,
            thresholdDividerFraction
        )
    }

    /**
     *  Applies exponential smoothing to the tracked velocity to reduce noise
     *  and provide more consistent threshold calculations.
     */
    private fun applySmoothing() {
        if (rotaryVelocityTracker.velocity != 0.0f) {
            // smooth the velocity
            smoothedVelocity = exponentialSmoothing(
                currentVelocity = rotaryVelocityTracker.velocity.absoluteValue,
                prevVelocity = smoothedVelocity,
                smoothingConstant = smoothingConstant
            )
        }
        debugLog { "rotaryVelocityTracker velocity: ${rotaryVelocityTracker.velocity}" }
        debugLog { "SmoothedVelocity: $smoothedVelocity" }
    }

    private fun exponentialSmoothing(
        currentVelocity: Float,
        prevVelocity: Float,
        smoothingConstant: Float
    ): Float =
        smoothingConstant * currentVelocity + (1 - smoothingConstant) * prevVelocity
}

private data class RotaryHandlerElement(
    private val rotaryBehavior: RotaryBehavior,
    private val reverseDirection: Boolean,
    private val inspectorInfo: InspectorInfo.() -> Unit
) : ModifierNodeElement<RotaryInputNode>() {
    override fun create(): RotaryInputNode = RotaryInputNode(
        rotaryBehavior,
        reverseDirection,
    )

    override fun update(node: RotaryInputNode) {
        debugLog { "Update launched!" }
        node.rotaryBehavior = rotaryBehavior
        node.reverseDirection = reverseDirection
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RotaryHandlerElement

        if (rotaryBehavior != other.rotaryBehavior) return false
        if (reverseDirection != other.reverseDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rotaryBehavior.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        return result
    }
}

private class RotaryInputNode(
    var rotaryBehavior: RotaryBehavior,
    var reverseDirection: Boolean,
) : RotaryInputModifierNode, Modifier.Node() {

    val channel = Channel<RotaryScrollEvent>(capacity = Channel.CONFLATED)
    val flow = channel.receiveAsFlow()

    override fun onAttach() {
        coroutineScope.launch {
            flow
                .collectLatest { event ->
                    val (orientation: Orientation, deltaInPixels: Float) =
                        if (event.verticalScrollPixels != 0.0f)
                            Pair(Orientation.Vertical, event.verticalScrollPixels)
                        else
                            Pair(Orientation.Horizontal, event.horizontalScrollPixels)
                    debugLog {
                        "Scroll event received: " +
                            "delta:$deltaInPixels, timestamp:${event.uptimeMillis}"
                    }
                    with(rotaryBehavior) {
                        handleScrollEvent(
                            timestamp = event.uptimeMillis,
                            deltaInPixels = deltaInPixels * if (reverseDirection) -1f else 1f,
                            deviceId = event.inputDeviceId,
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
}

/**
 * Debug logging that can be enabled.
 */
private const val DEBUG = false

private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("RotaryScroll: ${generateMsg()}")
    }
}
