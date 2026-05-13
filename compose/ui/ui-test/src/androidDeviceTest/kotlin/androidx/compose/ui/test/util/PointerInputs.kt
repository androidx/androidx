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

package androidx.compose.ui.test.util

import android.view.MotionEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventType
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage

data class DataPoint(
    val id: PointerId,
    val timestamp: Long,
    val position: Offset,
    val scrollDelta: Offset,
    val gesturePanOffset: Offset,
    val down: Boolean,
    val pointerType: PointerType,
    val eventType: PointerEventType,
    val buttons: PointerButtons,
    val keyboardModifiers: PointerKeyboardModifiers,
    val classification: Int,
    val axisGestureScrollXDistance: Float?,
    val axisGestureScrollYDistance: Float?,
    val axisGestureScaleFactor: Float?,
) {
    constructor(
        change: PointerInputChange,
        event: PointerEvent,
    ) : this(
        change.id,
        change.uptimeMillis,
        change.position,
        change.scrollDelta,
        change.panOffset,
        change.pressed,
        change.type,
        event.type,
        event.buttons,
        event.keyboardModifiers,
        event.classification,
        event.motionEvent?.getAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE),
        event.motionEvent?.getAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE),
        event.motionEvent?.getAxisValue(MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR),
    )

    constructor(
        change: IndirectPointerInputChange,
        event: IndirectPointerEvent,
    ) : this(
        id = change.id,
        timestamp = change.uptimeMillis,
        position = change.position,
        scrollDelta = Offset.Zero,
        gesturePanOffset = Offset.Zero,
        down = change.pressed,
        pointerType = Touch,
        eventType = event.type.toPointerEventType(),
        buttons = PointerButtons(),
        keyboardModifiers = PointerKeyboardModifiers(),
        classification = 0,
        axisGestureScrollXDistance = null,
        axisGestureScrollYDistance = null,
        axisGestureScaleFactor = null,
    )

    val x
        get() = position.x

    val y
        get() = position.y
}

private fun IndirectPointerEventType.toPointerEventType(): PointerEventType {
    return when (this) {
        IndirectPointerEventType.Press -> PointerEventType.Press
        IndirectPointerEventType.Release -> PointerEventType.Release
        IndirectPointerEventType.Move -> PointerEventType.Move
        else -> PointerEventType.Unknown
    }
}

/**
 * A [Modifier.Node] that records all [PointerEvent]s and [IndirectPointerEvent]s as they pass
 * through the [PointerEventPass.Initial] phase, without consuming anything. This modifier is
 * supposed to be completely transparent to the rest of the system.
 *
 * Does not support multiple pointers: all [PointerInputChange]s are flattened in the recorded list.
 */
class SinglePointerInputRecorder : ModifierNodeElement<SinglePointerInputRecorderNode>() {
    private val _events = mutableListOf<DataPoint>()
    val events
        get() = _events as List<DataPoint>

    private val velocityTracker = VelocityTracker()
    val recordedVelocity
        get() = velocityTracker.calculateVelocity()

    override fun create() = SinglePointerInputRecorderNode(_events, velocityTracker)

    override fun update(node: SinglePointerInputRecorderNode) {
        node.events = _events
        node.velocityTracker = velocityTracker
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "singlePointerInputRecorder"
        properties["events"] = events
        properties["recordedVelocity"] = recordedVelocity
    }

    override fun hashCode(): Int {
        var result = _events.hashCode()
        result = 31 * result + velocityTracker.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SinglePointerInputRecorder) return false
        if (_events !== other._events) return false
        if (velocityTracker !== other.velocityTracker) return false
        return true
    }
}

class SinglePointerInputRecorderNode(
    var events: MutableList<DataPoint>,
    var velocityTracker: VelocityTracker,
) : Modifier.Node(), PointerInputModifierNode, IndirectPointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass == PointerEventPass.Initial) {
            pointerEvent.changes.forEach {
                events.add(DataPoint(it, pointerEvent))
                velocityTracker.addPosition(it.uptimeMillis, it.position)
            }
        }
    }

    override fun onCancelPointerInput() {
        // Do nothing
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        if (pass == PointerEventPass.Initial) {
            event.changes.forEach {
                events.add(DataPoint(it, event))
                velocityTracker.addPosition(it.uptimeMillis, it.position)
            }
        }
    }

    override fun onCancelIndirectPointerInput() {
        // Do nothing
    }
}

/**
 * A [Modifier.Node] that records all [PointerEvent]s and [IndirectPointerEvent]s as they pass
 * through the [PointerEventPass.Initial] phase, without consuming anything. This modifier is
 * supposed to be completely transparent to the rest of the system.
 *
 * Supports multiple pointers: the set of [PointerInputChange]s from each event is kept together in
 * the recorded list.
 */
class MultiPointerInputRecorder : ModifierNodeElement<MultiPointerInputRecorderNode>() {
    data class Event(val pointers: List<DataPoint>) {
        val pointerCount: Int
            get() = pointers.size

        fun getPointer(index: Int) = pointers[index]
    }

    private val _events = mutableListOf<Event>()
    val events
        get() = _events as List<Event>

    override fun create() = MultiPointerInputRecorderNode(_events)

    override fun update(node: MultiPointerInputRecorderNode) {
        node.events = _events
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "multiPointerInputRecorder"
        properties["events"] = events
    }

    override fun hashCode(): Int {
        var result = _events.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultiPointerInputRecorder) return false
        if (_events !== other._events) return false
        return true
    }
}

class MultiPointerInputRecorderNode(var events: MutableList<MultiPointerInputRecorder.Event>) :
    Modifier.Node(), PointerInputModifierNode, IndirectPointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass == PointerEventPass.Initial) {
            events.add(
                MultiPointerInputRecorder.Event(
                    pointerEvent.changes.map { DataPoint(it, pointerEvent) }
                )
            )
        }
    }

    override fun onCancelPointerInput() {
        // Do nothing
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        if (pass == PointerEventPass.Initial) {
            events.add(MultiPointerInputRecorder.Event(event.changes.map { DataPoint(it, event) }))
        }
    }

    override fun onCancelIndirectPointerInput() {
        // Do nothing
    }
}

/**
 * A [PointerInputFilter] that [record]s each [PointerEvent][onPointerEvent] during the
 * [PointerEventPass.Initial] pass. Does not consume anything itself, although implementation can
 * (but really shouldn't).
 */
class RecordingFilter(private val record: (PointerEvent) -> Unit) : PointerInputFilter() {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass == PointerEventPass.Initial) {
            record(pointerEvent)
        }
    }

    override fun onCancel() {
        // Do nothing
    }
}

val SinglePointerInputRecorder.downEvents
    get() = events.filter { it.down }

val SinglePointerInputRecorder.recordedDurationMillis: Long
    get() {
        check(events.isNotEmpty()) { "No events recorded" }
        return events.last().timestamp - events.first().timestamp
    }

fun SinglePointerInputRecorder.assertTimestampsAreIncreasing() {
    check(events.isNotEmpty()) { "No events recorded" }
    events.reduce { prev, curr ->
        assertThat(curr.timestamp).isAtLeast(prev.timestamp)
        curr
    }
}

fun MultiPointerInputRecorder.assertTimestampsAreIncreasing() {
    check(events.isNotEmpty()) { "No events recorded" }
    // Check that each event has the same timestamp
    events.forEach { event ->
        assertThat(event.pointerCount).isAtLeast(1)
        val currTime = event.pointers[0].timestamp
        for (i in 1 until event.pointerCount) {
            assertThat(event.pointers[i].timestamp).isEqualTo(currTime)
        }
    }
    // Check that the timestamps are ordered
    assertThat(events.map { it.pointers[0].timestamp }).isInOrder()
}

fun SinglePointerInputRecorder.assertOnlyLastEventIsUp() {
    check(events.isNotEmpty()) { "No events recorded" }
    assertThat(events.last().down).isFalse()
    assertThat(events.count { !it.down }).isEqualTo(1)
}

fun SinglePointerInputRecorder.assertUpSameAsLastMove() {
    check(events.isNotEmpty()) { "No events recorded" }
    events.last().let {
        assertThat(it.eventType).isEqualTo(Release)
        downEvents.last().verify(it.timestamp, it.id, true, it.position, it.pointerType, Move)
    }
}

fun SinglePointerInputRecorder.assertSinglePointer() {
    assertThat(events.map { it.id }.distinct()).hasSize(1)
}

fun SinglePointerInputRecorder.verifyEvents(vararg verifiers: DataPoint.() -> Unit) {
    assertThat(events).hasSize(verifiers.size)
    if (events.isNotEmpty()) {
        assertTimestampsAreIncreasing()
        events.zip(verifiers) { event, verification -> verification.invoke(event) }
    }
}

fun DataPoint.verify(
    expectedTimestamp: Long?,
    expectedId: PointerId?,
    expectedDown: Boolean,
    expectedPosition: Offset,
    expectedPointerType: PointerType,
    expectedEventType: PointerEventType,
    expectedScrollDelta: Offset = Offset.Zero,
    expectedButtons: PointerButtons = PointerButtons(),
    expectedKeyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
) {
    val s = " of $this"
    if (expectedTimestamp != null) {
        assertWithMessage("timestamp$s").that(timestamp).isEqualTo(expectedTimestamp)
    }
    if (expectedId != null) {
        assertWithMessage("pointerId$s").that(id).isEqualTo(expectedId)
    }
    assertWithMessage("isDown$s").that(down).isEqualTo(expectedDown)
    position.isAlmostEqualTo(expectedPosition, message = "position$s")
    assertWithMessage("pointerType$s").that(pointerType).isEqualTo(expectedPointerType)
    assertWithMessage("eventType$s").that(eventType).isEqualTo(expectedEventType)
    scrollDelta.isAlmostEqualTo(expectedScrollDelta, message = "scrollDelta$s")
    assertWithMessage("buttonsDown$s").that(buttons).isEqualTo(expectedButtons)
    assertWithMessage("keyModifiers$s").that(keyboardModifiers).isEqualTo(expectedKeyboardModifiers)
}

/** Checks that the coordinates are progressing in a monotonous direction */
fun List<DataPoint>.isMonotonicBetween(start: Offset, end: Offset) {
    map { it.x }.isMonotonicBetween(start.x, end.x, 1e-3f)
    map { it.y }.isMonotonicBetween(start.y, end.y, 1e-3f)
}

fun List<DataPoint>.hasSameTimeBetweenEvents() {
    zipWithNext { a, b -> b.timestamp - a.timestamp }
        .sorted()
        .apply { assertThat(last() - first()).isAtMost(1L) }
}

fun List<DataPoint>.areSampledFromCurve(curve: (Long) -> Offset) {
    val t0 = first().timestamp
    forEach { it.position.isAlmostEqualTo(curve(it.timestamp - t0)) }
}
