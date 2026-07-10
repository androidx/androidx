/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.compose.ui.input.indirect

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerId
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 29)
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

    @Test
    fun historical_propertiesAreCorrect() {
        val historical =
            listOf(
                HistoricalChange(uptimeMillis = 2000L, position = Offset(5f, 5f)),
                HistoricalChange(uptimeMillis = 3000L, position = Offset(8f, 8f)),
            )
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
                historical = historical,
            )

        assertThat(change.historical).hasSize(2)
        assertThat(change.historical[0].uptimeMillis).isEqualTo(2000L)
        assertThat(change.historical[0].position).isEqualTo(Offset(5f, 5f))
        assertThat(change.historical[1].uptimeMillis).isEqualTo(3000L)
        assertThat(change.historical[1].position).isEqualTo(Offset(8f, 8f))
    }

    @Test
    fun copy_copiesHistoricalAndOtherProperties() {
        val historical = listOf(HistoricalChange(uptimeMillis = 2000L, position = Offset(5f, 5f)))
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
                historical = historical,
            )

        val copiedChange = change.copy(uptimeMillis = 6000L, position = Offset(15f, 25f))

        assertThat(copiedChange.id).isEqualTo(PointerId(1L))
        assertThat(copiedChange.uptimeMillis).isEqualTo(6000L)
        assertThat(copiedChange.position).isEqualTo(Offset(15f, 25f))
        assertThat(copiedChange.pressed).isTrue()
        assertThat(copiedChange.pressure).isEqualTo(0.5f)
        assertThat(copiedChange.previousUptimeMillis).isEqualTo(1000L)
        assertThat(copiedChange.previousPosition).isEqualTo(Offset(0f, 0f))
        assertThat(copiedChange.previousPressed).isFalse()
        assertThat(copiedChange.historical).isEqualTo(historical)
    }

    @Test
    fun copy_overrideHistorical() {
        val historical = listOf(HistoricalChange(uptimeMillis = 2000L, position = Offset(5f, 5f)))
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
                historical = historical,
            )

        val newHistorical =
            listOf(HistoricalChange(uptimeMillis = 4000L, position = Offset(9f, 9f)))
        val copiedChange = change.copy(historical = newHistorical)

        assertThat(copiedChange.historical).isEqualTo(newHistorical)
    }

    @Test
    fun toString_containsHistorical() {
        val historical = listOf(HistoricalChange(uptimeMillis = 2000L, position = Offset(5f, 5f)))
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
                historical = historical,
            )

        assertThat(change.toString()).contains("historical=$historical")
    }

    @Test
    fun createIndirectPointerInputChangesFromMotionEvents_withHistoricalChanges_isProperlyConverted() {
        val motionEvent =
            MotionEvent.obtain(
                0L /* downTime */,
                2000L /* eventTime */,
                MotionEvent.ACTION_MOVE,
                5f /* x */,
                5f /* y */,
                0, /* metaState */
            )
        // Add historical batch events
        // 2nd historical point: time 3000L, position (8f, 8f)
        motionEvent.addBatch(
            3000L /* eventTime */,
            8f /* x */,
            8f /* y */,
            0.7f /* pressure */,
            0f /* size */,
            0, /* metaState */
        )
        // Main event point: time 5000L, position (10f, 20f)
        motionEvent.addBatch(
            5000L /* eventTime */,
            10f /* x */,
            20f /* y */,
            0.5f /* pressure */,
            0f /* size */,
            0, /* metaState */
        )

        val changes =
            createIndirectPointerInputChangesFromMotionEvents(
                motionEvent = motionEvent,
                previousMotionEvent = null,
            )

        assertThat(changes).hasSize(1)
        val change = changes[0]
        assertThat(change.uptimeMillis).isEqualTo(5000L)
        assertThat(change.position).isEqualTo(Offset(10f, 20f))

        assertThat(change.historical).hasSize(2)
        assertThat(change.historical[0].uptimeMillis).isEqualTo(2000L)
        assertThat(change.historical[0].position).isEqualTo(Offset(5f, 5f))
        assertThat(change.historical[1].uptimeMillis).isEqualTo(3000L)
        assertThat(change.historical[1].position).isEqualTo(Offset(8f, 8f))
    }
}
