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

package androidx.xr.compose.subspace.layout

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.InputEvent.Action
import androidx.xr.scenecore.InputEvent.Pointer
import androidx.xr.scenecore.InputEvent.Source
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialInputEventTest {

    @Test
    fun equalsAndHashCode_sameProperties_returnsTrue() {
        val event1 =
            SpatialInputEvent(
                source = Source.HANDS,
                pointerType = Pointer.LEFT,
                action = Action.DOWN,
                timestamp = 100L,
                hitPosition = Vector3(1f, 2f, 3f),
                origin = Vector3(0f, 0f, 0f),
                direction = Vector3(0f, 0f, 1f),
            )
        val event2 =
            SpatialInputEvent(
                source = Source.HANDS,
                pointerType = Pointer.LEFT,
                action = Action.DOWN,
                timestamp = 100L,
                hitPosition = Vector3(1f, 2f, 3f),
                origin = Vector3(0f, 0f, 0f),
                direction = Vector3(0f, 0f, 1f),
            )

        assertThat(event1).isEqualTo(event2)
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
    }

    @Test
    fun equalsAndHashCode_nullHitPosition_returnsTrue() {
        val event1 =
            SpatialInputEvent(
                source = Source.HANDS,
                pointerType = Pointer.LEFT,
                action = Action.UP,
                timestamp = 200L,
                hitPosition = null,
                origin = Vector3(0f, 0f, 0f),
                direction = Vector3(0f, 0f, 1f),
            )
        val event2 =
            SpatialInputEvent(
                source = Source.HANDS,
                pointerType = Pointer.LEFT,
                action = Action.UP,
                timestamp = 200L,
                hitPosition = null,
                origin = Vector3(0f, 0f, 0f),
                direction = Vector3(0f, 0f, 1f),
            )

        assertThat(event1).isEqualTo(event2)
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
    }

    @Test
    fun equals_differentHitPosition_returnsFalse() {
        val event1 =
            SpatialInputEvent(
                source = Source.HANDS,
                pointerType = Pointer.LEFT,
                action = Action.MOVE,
                timestamp = 100L,
                hitPosition = Vector3(1f, 2f, 3f),
                origin = Vector3(0f, 0f, 0f),
                direction = Vector3(0f, 0f, 1f),
            )
        val event2 =
            SpatialInputEvent(
                source = Source.HANDS,
                pointerType = Pointer.LEFT,
                action = Action.MOVE,
                timestamp = 100L,
                hitPosition = Vector3(4f, 5f, 6f),
                origin = Vector3(0f, 0f, 0f),
                direction = Vector3(0f, 0f, 1f),
            )
        val eventNullHit =
            SpatialInputEvent(
                source = Source.HANDS,
                pointerType = Pointer.LEFT,
                action = Action.MOVE,
                timestamp = 100L,
                hitPosition = null,
                origin = Vector3(0f, 0f, 0f),
                direction = Vector3(0f, 0f, 1f),
            )

        assertThat(event1).isNotEqualTo(event2)
        assertThat(event1).isNotEqualTo(eventNullHit)
    }
}
