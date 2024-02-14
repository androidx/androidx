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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
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
 * A modifier which handles rotary events.
 * It accepts ScrollHandler as the input - a class where main logic about how
 * scroll should be handled is lying
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
