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

package androidx.ui.test.util

import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputHandler
import androidx.ui.core.gesture.util.VelocityTracker
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.test.util.PointerInputRecorder.DataPoint
import androidx.ui.unit.Duration
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat

class PointerInputRecorder : PointerInputFilter() {

    data class DataPoint(val id: PointerId, val data: PointerInputData) {
        val timestamp get() = data.uptime!!
        val position get() = data.position!!
        val x get() = data.position!!.x
        val y get() = data.position!!.y
        val down get() = data.down
    }

    private val _events = mutableListOf<DataPoint>()
    val events get() = _events as List<DataPoint>

    private val velocityTracker = VelocityTracker()
    val recordedVelocity get() = velocityTracker.calculateVelocity()

    override val pointerInputHandler: PointerInputHandler =
        { changes, pass, _ ->
            if (pass == PointerEventPass.InitialDown) {
                changes.forEach {
                    _events.add(DataPoint(it.id, it.current))
                    velocityTracker.addPosition(it.current.uptime!!, it.current.position!!)
                }
            }
            changes
        }
    override val cancelHandler: () -> Unit = {}
}

val PointerInputRecorder.downEvents get() = events.filter { it.down }

val PointerInputRecorder.recordedDuration: Duration
    get() {
        check(events.isNotEmpty()) { "No events recorded" }
        return events.last().timestamp - events.first().timestamp
    }

fun PointerInputRecorder.assertTimestampsAreIncreasing() {
    check(events.isNotEmpty()) { "No events recorded" }
    events.reduce { prev, curr ->
        assertThat(curr.timestamp).isAtLeast(prev.timestamp)
        curr
    }
}

fun PointerInputRecorder.assertOnlyLastEventIsUp() {
    check(events.isNotEmpty()) { "No events recorded" }
    assertThat(events.last().down).isFalse()
    assertThat(events.count { !it.down }).isEqualTo(1)
}

/**
 * Checks that the coordinates are progressing in a monotonous direction
 */
fun List<DataPoint>.isMonotonousBetween(start: PxPosition, end: PxPosition) {
    map { it.x.value }.isMonotonousBetween(start.x.value, end.x.value, 1e-3f)
    map { it.y.value }.isMonotonousBetween(start.y.value, end.y.value, 1e-3f)
}

/**
 * Verifies that all [DataPoint]s in the list are equal to the given position, with a tolerance
 * of 0.001
 */
fun List<DataPoint>.areAlmostEqualTo(position: PxPosition) {
    forEach { it.position.isAlmostEqualTo(position, 1e-3f) }
}
