/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.converter

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.events.PointerAddedEvent
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerHoverEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerRemovedEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.ui.pointer.PointerChange
import androidx.ui.ui.pointer.PointerData
import androidx.ui.ui.pointer.PointerDeviceKind
import kotlin.coroutines.experimental.buildSequence

// / Converts from engine pointer data to framework pointer events.
// /
// / This takes [PointerDataPacket] objects, as received from the engine via
// / [dart:ui.Window.onPointerDataPacket], and converts them to [PointerEvent]
// / objects.
object PointerEventConverter {

    // Map from platform pointer identifiers to PointerEvent pointer identifiers.
    private val _pointers: MutableMap<Int, _PointerState> = mutableMapOf()

    private fun _ensureStateForPointer(datum: PointerData, position: Offset): _PointerState {
        return _pointers.getOrPut(datum.device) {
            _PointerState(position)
        }
    }

    // / Expand the given packet of pointer data into a sequence of framework pointer events.
    // /
    // / The `devicePixelRatio` argument (usually given the value from
    // / [dart:ui.Window.devicePixelRatio]) is used to convert the incoming data
    // / from physical coordinates to logical pixels. See the discussion at
    // / [PointerEvent] for more details on the [PointerEvent] coordinate space.
    fun expand(data: Iterable<PointerData>, devicePixelRatio: Double) = buildSequence {
        data.forEach {
            val position: Offset = Offset(it.physicalX, it.physicalY) / devicePixelRatio
            val timeStamp: Duration = it.timeStamp
            val kind: PointerDeviceKind = it.kind
            when (it.change) {
                PointerChange.add -> {
                    assert(!_pointers.containsKey(it.device))
                    val state: _PointerState = _ensureStateForPointer(it, position)
                    assert(state.lastPosition == position)
                    yield(
                        PointerAddedEvent(
                            timeStamp = timeStamp,
                            kind = kind,
                            device = it.device,
                            position = position,
                            obscured = it.obscured,
                            pressureMin = it.pressureMin,
                            pressureMax = it.pressureMax,
                            distance = it.distance,
                            distanceMax = it.distanceMax,
                            radiusMin = it.radiusMin,
                            radiusMax = it.radiusMax,
                            orientation = it.orientation,
                            tilt = it.tilt
                        )
                    )
                }
                PointerChange.hover -> {
                    val alreadyAdded: Boolean = _pointers.containsKey(it.device)
                    val state: _PointerState = _ensureStateForPointer(it, position)
                    assert(!state.down)
                    if (!alreadyAdded) {
                        assert(state.lastPosition == position)
                        yield(
                            PointerAddedEvent(
                                timeStamp = timeStamp,
                                kind = kind,
                                device = it.device,
                                position = position,
                                obscured = it.obscured,
                                pressureMin = it.pressureMin,
                                pressureMax = it.pressureMax,
                                distance = it.distance,
                                distanceMax = it.distanceMax,
                                radiusMin = it.radiusMin,
                                radiusMax = it.radiusMax,
                                orientation = it.orientation,
                                tilt = it.tilt
                            )
                        )
                    }
                    val offset: Offset = position - state.lastPosition
                    state.lastPosition = position
                    yield(
                        PointerHoverEvent(
                            timeStamp = timeStamp,
                            kind = kind,
                            device = it.device,
                            position = position,
                            delta = offset,
                            buttons = it.buttons,
                            obscured = it.obscured,
                            pressureMin = it.pressureMin,
                            pressureMax = it.pressureMax,
                            distance = it.distance,
                            distanceMax = it.distanceMax,
                            radiusMajor = it.radiusMajor,
                            radiusMinor = it.radiusMajor,
                            radiusMin = it.radiusMin,
                            radiusMax = it.radiusMax,
                            orientation = it.orientation,
                            tilt = it.tilt
                        )
                    )
                    state.lastPosition = position
                }
                PointerChange.down -> {
                    val alreadyAdded: Boolean = _pointers.containsKey(it.device)
                    val state: _PointerState = _ensureStateForPointer(it, position)
                    assert(!state.down)
                    if (!alreadyAdded) {
                        assert(state.lastPosition == position)
                        yield(
                            PointerAddedEvent(
                                timeStamp = timeStamp,
                                kind = kind,
                                device = it.device,
                                position = position,
                                obscured = it.obscured,
                                pressureMin = it.pressureMin,
                                pressureMax = it.pressureMax,
                                distance = it.distance,
                                distanceMax = it.distanceMax,
                                radiusMin = it.radiusMin,
                                radiusMax = it.radiusMax,
                                orientation = it.orientation,
                                tilt = it.tilt
                            )
                        )
                    }
                    if (state.lastPosition != position) {
                        // Not all sources of pointer packets respect the invariant that
                        // they hover the pointer to the down location before sending the
                        // down event. We restore the invariant here for our clients.
                        val offset: Offset = position - state.lastPosition
                        state.lastPosition = position
                        yield(
                            PointerHoverEvent(
                                timeStamp = timeStamp,
                                kind = kind,
                                device = it.device,
                                position = position,
                                delta = offset,
                                buttons = it.buttons,
                                obscured = it.obscured,
                                pressureMin = it.pressureMin,
                                pressureMax = it.pressureMax,
                                distance = it.distance,
                                distanceMax = it.distanceMax,
                                radiusMajor = it.radiusMajor,
                                radiusMinor = it.radiusMajor,
                                radiusMin = it.radiusMin,
                                radiusMax = it.radiusMax,
                                orientation = it.orientation,
                                tilt = it.tilt,
                                synthesized = true
                            )
                        )
                        state.lastPosition = position
                    }
                    state.startNewPointer()
                    state.setDown()
                    yield(
                        PointerDownEvent(
                            timeStamp = timeStamp,
                            pointer = state.pointer,
                            kind = kind,
                            device = it.device,
                            position = position,
                            buttons = it.buttons,
                            obscured = it.obscured,
                            pressure = it.pressure,
                            pressureMin = it.pressureMin,
                            pressureMax = it.pressureMax,
                            distanceMax = it.distanceMax,
                            radiusMajor = it.radiusMajor,
                            radiusMinor = it.radiusMajor,
                            radiusMin = it.radiusMin,
                            radiusMax = it.radiusMax,
                            orientation = it.orientation,
                            tilt = it.tilt
                        )
                    )
                }
                PointerChange.move -> {
                    // If the service starts supporting hover pointers, then it must also
                    // start sending us ADDED and REMOVED data points.
                    // See also: https://github.com/flutter/flutter/issues/720
                    assert(_pointers.containsKey(it.device))
                    val state: _PointerState = _pointers[it.device]!!
                    assert(state.down)
                    val offset: Offset = position - state.lastPosition
                    state.lastPosition = position
                    yield(
                        PointerMoveEvent(
                            timeStamp = timeStamp,
                            pointer = state.pointer,
                            kind = kind,
                            device = it.device,
                            position = position,
                            delta = offset,
                            buttons = it.buttons,
                            obscured = it.obscured,
                            pressure = it.pressure,
                            pressureMin = it.pressureMin,
                            pressureMax = it.pressureMax,
                            distanceMax = it.distanceMax,
                            radiusMajor = it.radiusMajor,
                            radiusMinor = it.radiusMajor,
                            radiusMin = it.radiusMin,
                            radiusMax = it.radiusMax,
                            orientation = it.orientation,
                            tilt = it.tilt
                        )
                    )
                }
                PointerChange.up, PointerChange.cancel -> {
                    assert(_pointers.containsKey(it.device))
                    val state: _PointerState = _pointers[it.device]!!
                    assert(state.down)
                    if (position != state.lastPosition) {
                        // Not all sources of pointer packets respect the invariant that
                        // they move the pointer to the up location before sending the up
                        // event. For example, in the iOS simulator, of you drag outside the
                        // window, you'll get a stream of pointers that violates that
                        // invariant. We restore the invariant here for our clients.
                        val offset: Offset = position - state.lastPosition
                        state.lastPosition = position
                        yield(
                            PointerMoveEvent(
                                timeStamp = timeStamp,
                                pointer = state.pointer,
                                kind = kind,
                                device = it.device,
                                position = position,
                                delta = offset,
                                buttons = it.buttons,
                                obscured = it.obscured,
                                pressure = it.pressure,
                                pressureMin = it.pressureMin,
                                pressureMax = it.pressureMax,
                                distanceMax = it.distanceMax,
                                radiusMajor = it.radiusMajor,
                                radiusMinor = it.radiusMajor,
                                radiusMin = it.radiusMin,
                                radiusMax = it.radiusMax,
                                orientation = it.orientation,
                                tilt = it.tilt,
                                synthesized = true
                            )
                        )
                        state.lastPosition = position
                    }
                    assert(position == state.lastPosition)
                    state.setUp()
                    if (it.change == PointerChange.up) {
                        yield(
                            PointerUpEvent(
                                timeStamp = timeStamp,
                                pointer = state.pointer,
                                kind = kind,
                                device = it.device,
                                position = position,
                                buttons = it.buttons,
                                obscured = it.obscured,
                                pressureMax = it.pressureMax,
                                distance = it.distance,
                                distanceMax = it.distanceMax,
                                radiusMin = it.radiusMin,
                                radiusMax = it.radiusMax,
                                orientation = it.orientation,
                                tilt = it.tilt
                            )
                        )
                    } else {
                        yield(
                            PointerCancelEvent(
                                timeStamp = timeStamp,
                                pointer = state.pointer,
                                kind = kind,
                                device = it.device,
                                position = position,
                                buttons = it.buttons,
                                obscured = it.obscured,
                                pressureMin = it.pressureMin,
                                pressureMax = it.pressureMax,
                                distance = it.distance,
                                distanceMax = it.distanceMax,
                                radiusMin = it.radiusMin,
                                radiusMax = it.radiusMax,
                                orientation = it.orientation,
                                tilt = it.tilt
                            )
                        )
                    }
                }
                PointerChange.remove -> {
                    assert(_pointers.containsKey(it.device))
                    val state: _PointerState = _pointers[it.device]!!
                    if (state.down) {
                        yield(
                            PointerCancelEvent(
                                timeStamp = timeStamp,
                                pointer = state.pointer,
                                kind = kind,
                                device = it.device,
                                position = position,
                                buttons = it.buttons,
                                obscured = it.obscured,
                                pressureMin = it.pressureMin,
                                pressureMax = it.pressureMax,
                                distance = it.distance,
                                distanceMax = it.distanceMax,
                                radiusMin = it.radiusMin,
                                radiusMax = it.radiusMax,
                                orientation = it.orientation,
                                tilt = it.tilt
                            )
                        )
                    }
                    _pointers.remove(it.device)
                    yield(
                        PointerRemovedEvent(
                            timeStamp = timeStamp,
                            kind = kind,
                            device = it.device,
                            obscured = it.obscured,
                            pressureMin = it.pressureMin,
                            pressureMax = it.pressureMax,
                            distanceMax = it.distanceMax,
                            radiusMin = it.radiusMin,
                            radiusMax = it.radiusMax
                        )
                    )
                }
            }
        }
    }
}