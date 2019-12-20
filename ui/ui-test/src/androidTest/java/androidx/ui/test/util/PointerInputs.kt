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

import androidx.ui.core.IntPxSize
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import com.google.common.truth.Truth.assertThat

class PointerInputRecorder {

    data class DataPoint(val id: Int, val data: PointerInputData) {
        val timestamp get() = data.uptime!!
        val position get() = data.position
        val x get() = data.position!!.x
        val y get() = data.position!!.y
        val down get() = data.down
    }

    private val _events = mutableListOf<DataPoint>()
    val events get() = _events as List<DataPoint>

    fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        @Suppress("UNUSED_PARAMETER") bounds: IntPxSize
    ): List<PointerInputChange> {
        if (pass == PointerEventPass.InitialDown) {
            changes.forEach {
                _events.add(DataPoint(it.id, it.current))
            }
        }
        return changes
    }
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
