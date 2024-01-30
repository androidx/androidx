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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class MouseWheelScrollNode(
    private val scrollingLogic: ScrollingLogic,
    private var _enabled: Boolean,
) : DelegatingNode(), CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private lateinit var mouseWheelScrollConfig: ScrollConfig
    private lateinit var physics: ScrollPhysics

    override fun onAttach() {
        mouseWheelScrollConfig = platformScrollConfig()
        physics = if (mouseWheelScrollConfig.isSmoothScrollingEnabled) {
            AnimatedMouseWheelScrollPhysics(
                mouseWheelScrollConfig,
                scrollingLogic,
                density = { currentValueOf(LocalDensity) }
            )
        } else {
            RawMouseWheelScrollPhysics(mouseWheelScrollConfig, scrollingLogic)
        }
        coroutineScope.launch {
            physics.launch()
        }
    }

    // TODO(https://youtrack.jetbrains.com/issue/COMPOSE-731/Scrollable-doesnt-react-on-density-changes)
    //  it isn't called, because LocalDensity is staticCompositionLocalOf
    override fun onObservedReadsChanged() {
        physics.mouseWheelScrollConfig = mouseWheelScrollConfig
        physics.scrollingLogic = scrollingLogic
    }

    private val pointerInputNode = delegate(SuspendingPointerInputModifierNode {
        if (_enabled) {
            mouseWheelInput()
        }
    })

    var enabled
        get() = _enabled
        set(value) {
            if (_enabled != value) {
                _enabled = value
                pointerInputNode.resetPointerInputHandler()
            }
        }

    private suspend fun PointerInputScope.mouseWheelInput() = awaitPointerEventScope {
        while (true) {
            val event = awaitScrollEvent()
            if (!event.isConsumed) {
                val consumed = with(physics) { onMouseWheel(event) }
                if (consumed) {
                    event.consume()
                }
            }
        }
    }

    private suspend fun AwaitPointerEventScope.awaitScrollEvent(): PointerEvent {
        var event: PointerEvent
        do {
            event = awaitPointerEvent()
        } while (event.type != PointerEventType.Scroll)
        return event
    }

    private inline val PointerEvent.isConsumed: Boolean get() = changes.fastAny { it.isConsumed }
    private inline fun PointerEvent.consume() = changes.fastForEach { it.consume() }
}

private abstract class ScrollPhysics {
    abstract var mouseWheelScrollConfig: ScrollConfig
    abstract var scrollingLogic: ScrollingLogic

    open suspend fun launch() = Unit
    abstract fun PointerInputScope.onMouseWheel(pointerEvent: PointerEvent): Boolean
}

private class RawMouseWheelScrollPhysics(
    override var mouseWheelScrollConfig: ScrollConfig,
    override var scrollingLogic: ScrollingLogic,
) : ScrollPhysics() {
    override fun PointerInputScope.onMouseWheel(pointerEvent: PointerEvent): Boolean {
        val delta = with(mouseWheelScrollConfig) {
            calculateMouseWheelScroll(pointerEvent, size)
        }
        return scrollingLogic.dispatchRawDelta(delta) != Offset.Zero
    }
}

private class AnimatedMouseWheelScrollPhysics(
    override var mouseWheelScrollConfig: ScrollConfig,
    override var scrollingLogic: ScrollingLogic,
    val density: () -> Density,
) : ScrollPhysics() {
    private var isAnimationRunning = false
    private val channel = Channel<Float>(capacity = Channel.UNLIMITED)

    override suspend fun launch() {
        while (coroutineContext.isActive) {
            val event = channel.receive()
            isAnimationRunning = true
            try {
                scrollingLogic.animatedDispatchScroll(event, speed = 1f * density().density) {
                    // Sum delta from all pending events to avoid multiple animation restarts.
                    channel.sumOrNull()
                }
            } finally {
                isAnimationRunning = false
            }
        }
    }

    override fun PointerInputScope.onMouseWheel(pointerEvent: PointerEvent): Boolean {
        val scrollDelta = with(mouseWheelScrollConfig) {
            calculateMouseWheelScroll(pointerEvent, size)
        }
        return if (mouseWheelScrollConfig.isPreciseWheelScroll(pointerEvent)) {
            // In case of high-resolution wheel, such as a freely rotating wheel with no notches
            // or trackpads, delta should apply directly without any delays.
            scrollingLogic.dispatchRawDelta(scrollDelta) != Offset.Zero

            /*
             * TODO Set isScrollInProgress to true in case of touchpad.
             *  Dispatching raw delta doesn't cause a progress indication even with wrapping in
             *  `scrollableState.scroll` block, since it applies the change within single frame.
             *  Touchpads emit just multiple mouse wheel events, so detecting start and end of this
             *  "gesture" is not straight forward.
             *  Ideally it should be resolved by catching real touches from input device instead of
             *  introducing a timeout (after each event before resetting progress flag).
             */
        } else with(scrollingLogic) {
            val delta = scrollDelta.reverseIfNeeded().toFloat()
            if (isAnimationRunning) {
                channel.trySend(delta).isSuccess
            } else {
                // Try to apply small delta immediately to conditionally consume
                // an input event and to avoid useless animation.
                tryToScrollBySmallDelta(delta, threshold = 4.dp.toPx()) {
                    channel.trySend(it).isSuccess
                }
            }
        }
    }

    private fun Channel<Float>.sumOrNull(): Float? {
        val elements = untilNull { tryReceive().getOrNull() }.toList()
        return if (elements.isEmpty()) null else elements.sum()
    }

    private fun <E> untilNull(builderAction: () -> E?) = sequence<E> {
        do {
            val element = builderAction()?.also {
                yield(it)
            }
        } while (element != null)
    }

    private fun ScrollingLogic.tryToScrollBySmallDelta(
        delta: Float,
        threshold: Float = 4f,
        fallback: (Float) -> Boolean
    ): Boolean {
        return if (abs(delta) > threshold) {
            // Gather possibility to scroll by applying a piece of required delta.
            val testDelta = if (delta > 0f) threshold else -threshold
            val consumedDelta = scrollableState.dispatchRawDelta(testDelta)
            consumedDelta != 0f && fallback(delta - testDelta)
        } else {
            val consumedDelta = scrollableState.dispatchRawDelta(delta)
            consumedDelta != 0f
        }
    }

    private suspend fun ScrollingLogic.animatedDispatchScroll(
        eventDelta: Float,
        speed: Float = 1f,
        maxDurationMillis: Int = 100,
        tryReceiveNext: () -> Float?
    ) {
        var target = eventDelta
        tryReceiveNext()?.let {
            target += it
        }
        if (target.isLowScrollingDelta()) {
            return
        }
        var requiredAnimation = true
        var lastValue = 0f
        val anim = AnimationState(0f)
        while (requiredAnimation) {
            requiredAnimation = false
            val durationMillis = (abs(target - anim.value) / speed)
                .roundToInt()
                .coerceAtMost(maxDurationMillis)
            try {
                scrollableState.scroll {
                    anim.animateTo(
                        target,
                        animationSpec = tween(
                            durationMillis = durationMillis,
                            easing = LinearEasing
                        ),
                        sequentialAnimation = true
                    ) {
                        val delta = value - lastValue
                        if (!delta.isLowScrollingDelta()) {
                            val consumedDelta = scrollBy(delta)
                            if (!(delta - consumedDelta).isLowScrollingDelta()) {
                                cancelAnimation()
                                return@animateTo
                            }
                            lastValue += delta
                        }
                        tryReceiveNext()?.let {
                            target += it
                            requiredAnimation = !(target - lastValue).isLowScrollingDelta()
                            cancelAnimation()
                        }
                    }
                }
            } catch (ignore: CancellationException) {
                requiredAnimation = true
            }
        }
    }
}

/*
 * Returns true, if the value is too low for visible change in scroll (consumed delta, animation-based change, etc),
 * false otherwise
 */
private inline fun Float.isLowScrollingDelta(): Boolean = abs(this) < 0.5f
