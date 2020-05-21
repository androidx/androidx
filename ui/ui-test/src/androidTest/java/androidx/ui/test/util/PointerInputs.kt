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
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.gesture.util.VelocityTracker
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.test.util.PointerInputRecorder.DataPoint
import androidx.ui.unit.Duration
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
import com.google.common.truth.Truth.assertThat

class PointerInputRecorder : PointerInputModifier {

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

    override val pointerInputFilter = RecordingFilter {
        _events.add(DataPoint(it.id, it.current))
        velocityTracker.addPosition(it.current.uptime!!, it.current.position!!)
    }
}

class RecordingFilter(private val record: (PointerInputChange) -> Unit) : PointerInputFilter() {
    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange> {
        if (pass == PointerEventPass.InitialDown) {
            changes.forEach {
                record(it)
            }
        }
        return changes
    }

    override fun onCancel() {
        // Do nothing
    }
}

fun DataPoint.timeDiffWith(other: DataPoint): Long {
    return (timestamp - other.timestamp).inMilliseconds()
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
fun List<DataPoint>.isMonotonicBetween(start: PxPosition, end: PxPosition) {
    map { it.x }.isMonotonicBetween(start.x, end.x, 1e-3f)
    map { it.y }.isMonotonicBetween(start.y, end.y, 1e-3f)
}
