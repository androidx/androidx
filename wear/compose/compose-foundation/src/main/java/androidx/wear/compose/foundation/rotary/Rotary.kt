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
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import kotlin.math.abs
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
 * An abstract class for handling scroll events
 */
internal abstract class RotaryHandler {

    // Threshold for detection of a new gesture
    private val gestureThresholdTime = 200L
    protected var previousScrollEventTime = -1L

    /**
     * Handles scrolling events
     * @param coroutineScope A scope for performing async actions
     * @param event A scrollable event from rotary input, containing scrollable delta and timestamp
     */
    abstract suspend fun handleScrollEvent(
        coroutineScope: CoroutineScope,
        event: UnifiedRotaryEvent,
    )

    protected fun isNewScrollEvent(timestamp: Long): Boolean {
        val timeDelta = timestamp - previousScrollEventTime
        return previousScrollEventTime == -1L || timeDelta > gestureThresholdTime
    }
}

/**
 * A rotary event object which contains all necessary information for handling rotary
 * event with haptics.
 */
internal data class UnifiedRotaryEvent(
    val timestamp: Long,
    val deviceId: Int,
    val orientation: Orientation,
    val deltaInPixels: Float
)

/**
 * This class does a smooth animation when the scroll by N pixels is done.
 * This animation works well on Rsb(high-res) and Bezel(low-res) devices.
 */
internal class RotaryScrollBehavior(
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
 * A modifier which handles rotary events.
 * It accepts ScrollHandler as the input - a class that handles the main scroll logic.
 */
internal fun Modifier.rotaryHandler(
    rotaryScrollHandler: RotaryHandler,
    reverseDirection: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit = debugInspectorInfo {
        name = "rotaryHandler"
        properties["rotaryScrollHandler"] = rotaryScrollHandler
        properties["reverseDirection"] = reverseDirection
    }
): Modifier = this then RotaryHandlerElement(
    rotaryScrollHandler,
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
internal class RotaryFlingBehavior(
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
 * A scroll handler for scrolling without snapping and with or without fling.
 * A list is scrolled by the number of pixels received from the rotary device.
 *
 * For a high-res input it has a filtering for events which are coming
 * with an opposite sign (this might happen to devices with rsb,
 * especially at the end of the scroll )
 *
 * This scroll handler supports fling. It can be set with [RotaryFlingBehavior].
 */
internal class RotaryScrollHandler(
    private val isLowRes: Boolean,
    private val rotaryHaptics: RotaryHapticHandler,
    private val rotaryFlingBehaviorFactory: () -> RotaryFlingBehavior?,
    private val scrollBehaviorFactory: () -> RotaryScrollBehavior,
) : RotaryHandler() {
    private var rotaryScrollDistance = 0f

    private var rotaryFlingBehavior: RotaryFlingBehavior? = rotaryFlingBehaviorFactory()
    private var scrollBehavior: RotaryScrollBehavior = scrollBehaviorFactory()

    override suspend fun handleScrollEvent(
        coroutineScope: CoroutineScope,
        event: UnifiedRotaryEvent,
    ) {
        val time = event.timestamp
        debugLog { "RotaryScrollHandler: handleScrollEvent" }

        if (isNewScrollEvent(time)) {
            debugLog { "New scroll event" }
            resetScrolling()
            resetFlingTracking(time)
        } else {
            // Due to the physics of high-res Rotary side button, some events might come
            // with an opposite axis value - either at the start or at the end of the motion.
            // We don't want to use these values for fling calculations.
            if (isLowRes || !isOppositeValueAfterScroll(event.deltaInPixels)) {
                rotaryFlingBehavior?.observeEvent(time, event.deltaInPixels)
            } else {
                debugLog { "Opposite value after scroll :${event.deltaInPixels}" }
            }
        }

        rotaryScrollDistance += event.deltaInPixels
        debugLog { "Rotary scroll distance: $rotaryScrollDistance" }

        rotaryHaptics.handleScrollHaptic(event)

        previousScrollEventTime = time
        scrollBehavior.scrollToTarget(coroutineScope, rotaryScrollDistance)

        rotaryFlingBehavior?.performFlingIfRequired(
            coroutineScope,
            beforeFling = {
                debugLog { "Calling beforeFling section" }
                resetScrolling()
            },
            edgeReached = { velocity ->
                rotaryHaptics.handleLimitHaptic(event, velocity > 0f)
            }
        )
    }

    private fun resetScrolling() {
        scrollBehavior.cancelScrollIfActive()
        scrollBehavior = scrollBehaviorFactory()
        rotaryScrollDistance = 0f
    }

    private fun resetFlingTracking(timestamp: Long) {
        rotaryFlingBehavior?.cancelFlingIfActive()
        rotaryFlingBehavior = rotaryFlingBehaviorFactory()
        rotaryFlingBehavior?.startFlingTracking(timestamp)
    }

    private fun isOppositeValueAfterScroll(delta: Float): Boolean =
        rotaryScrollDistance * delta < 0f &&
            (abs(delta) < abs(rotaryScrollDistance))
}

private data class RotaryHandlerElement(
    private val rotaryScrollHandler: RotaryHandler,
    private val reverseDirection: Boolean,
    private val inspectorInfo: InspectorInfo.() -> Unit
) : ModifierNodeElement<RotaryInputNode>() {
    override fun create(): RotaryInputNode = RotaryInputNode(
        rotaryScrollHandler,
        reverseDirection,
    )

    override fun update(node: RotaryInputNode) {
        debugLog { "Update launched!" }
        node.rotaryScrollHandler = rotaryScrollHandler
        node.reverseDirection = reverseDirection
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RotaryHandlerElement

        if (rotaryScrollHandler != other.rotaryScrollHandler) return false
        if (reverseDirection != other.reverseDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rotaryScrollHandler.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        return result
    }
}

private class RotaryInputNode(
    var rotaryScrollHandler: RotaryHandler,
    var reverseDirection: Boolean,
) : RotaryInputModifierNode, Modifier.Node() {

    val channel = Channel<UnifiedRotaryEvent>(capacity = Channel.CONFLATED)
    val flow = channel.receiveAsFlow()

    override fun onAttach() {
        coroutineScope.launch {
            flow
                .collectLatest {
                    debugLog {
                        "Scroll event received: " +
                            "delta:${it.deltaInPixels}, timestamp:${it.timestamp}"
                    }
                    rotaryScrollHandler.handleScrollEvent(this, it)
                }
        }
    }

    override fun onRotaryScrollEvent(event: RotaryScrollEvent): Boolean = false

    override fun onPreRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
        debugLog { "onPreRotaryScrollEvent" }

        val (orientation: Orientation, deltaInPixels: Float) =
            if (event.verticalScrollPixels != 0.0f)
                Pair(Orientation.Vertical, event.verticalScrollPixels)
            else
                Pair(Orientation.Horizontal, event.horizontalScrollPixels)

        channel.trySend(
            UnifiedRotaryEvent(
                timestamp = event.uptimeMillis,
                deviceId = event.inputDeviceId,
                orientation = orientation,
                deltaInPixels = deltaInPixels * if (reverseDirection) -1f else 1f
            )
        )
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
