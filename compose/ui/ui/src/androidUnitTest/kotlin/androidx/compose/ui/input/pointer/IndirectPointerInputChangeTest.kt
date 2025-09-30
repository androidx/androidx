/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.ui.input.pointer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IndirectPointerInputChangeTest {

    @Test
    fun downChange_propertiesAreCorrect() {
        val change =
            IndirectPointerInputChange(
                id = PointerId(1L),
                uptimeMillis = 5000L,
                position = Offset(10f, 20f),
                pressed = true,
                pressure = 0.5f,
                previousUptimeMillis = 1000L,
                previousPosition = Offset(0f, 0f),
                previousPressed = false,
            )

        assertThat(change.id).isEqualTo(PointerId(1L))
        assertThat(change.uptimeMillis).isEqualTo(5000L)
        assertThat(change.position).isEqualTo(Offset(10f, 20f))
        assertThat(change.pressed).isTrue()
        assertThat(change.pressure).isEqualTo(0.5f)
        assertThat(change.isConsumed).isFalse()
        assertThat(change.previousUptimeMillis).isEqualTo(1000L)
        assertThat(change.previousPosition).isEqualTo(Offset(0f, 0f))
        assertThat(change.previousPressed).isFalse()
    }

    @Test
    fun upChange_propertiesAreCorrect() {
        val change =
            IndirectPointerInputChange(
                id = PointerId(2L),
                uptimeMillis = 4000L,
                position = Offset(30f, 40f),
                pressed = false,
                pressure = 0.0f,
                previousUptimeMillis = 2000L,
                previousPosition = Offset(10f, 20f),
                previousPressed = true,
            )

        assertThat(change.id).isEqualTo(PointerId(2L))
        assertThat(change.uptimeMillis).isEqualTo(4000L)
        assertThat(change.position).isEqualTo(Offset(30f, 40f))
        assertThat(change.pressed).isFalse()
        assertThat(change.pressure).isEqualTo(0.0f)
        assertThat(change.isConsumed).isFalse()
        assertThat(change.previousUptimeMillis).isEqualTo(2000L)
        assertThat(change.previousPosition).isEqualTo(Offset(10f, 20f))
        assertThat(change.previousPressed).isTrue()
    }

    @Test
    fun moveChange_propertiesAreCorrect() {
        val change =
            IndirectPointerInputChange(
                id = PointerId(3L),
                uptimeMillis = 4000L,
                position = Offset(50f, 60f),
                pressed = true,
                pressure = 0.7f,
                previousUptimeMillis = 3000L,
                previousPosition = Offset(30f, 40f),
                previousPressed = true,
            )

        assertThat(change.id).isEqualTo(PointerId(3L))
        assertThat(change.uptimeMillis).isEqualTo(4000L)
        assertThat(change.position).isEqualTo(Offset(50f, 60f))
        assertThat(change.pressed).isTrue()
        assertThat(change.pressure).isEqualTo(0.7f)
        assertThat(change.isConsumed).isFalse()
        assertThat(change.previousUptimeMillis).isEqualTo(3000L)
        assertThat(change.previousPosition).isEqualTo(Offset(30f, 40f))
        assertThat(change.previousPressed).isTrue()
    }

    @Test
    fun consume_setsIsConsumedToTrue() {
        val change =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = 0L,
                position = Offset.Zero,
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = 0L,
                previousPosition = Offset.Zero,
                previousPressed = false,
            )
        assertThat(change.isConsumed).isFalse()

        change.consume()

        assertThat(change.isConsumed).isTrue()
    }

    @Test
    fun consume_multipleCalls_isIdempotent() {
        val change =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = 0L,
                position = Offset.Zero,
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = 0L,
                previousPosition = Offset.Zero,
                previousPressed = false,
            )
        assertThat(change.isConsumed).isFalse()

        change.consume()
        assertThat(change.isConsumed).isTrue()

        change.consume()
        assertThat(change.isConsumed).isTrue()
    }
}
