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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Utils
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFails
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteStateCacheKeyTest {

    @Test
    fun remoteConstantCacheKey_Equality() {
        val key1 = RemoteConstantCacheKey(10f)
        val key1Dup = RemoteConstantCacheKey(10f)
        val key2 = RemoteConstantCacheKey(20f)
        val keyString = RemoteConstantCacheKey("test")
        val keyNull = RemoteConstantCacheKey(null)

        assertThat(key1).isEqualTo(key1Dup)
        assertThat(key1.hashCode()).isEqualTo(key1Dup.hashCode())

        assertThat(key1).isNotEqualTo(key2)
        assertThat(key1).isNotEqualTo(keyString)
        assertThat(key1).isNotEqualTo(keyNull)
    }

    @Test
    fun remoteNamedCacheKey_Equality() {
        val key1 = RemoteNamedCacheKey(RemoteState.Domain.User, "width")
        val key1Dup = RemoteNamedCacheKey(RemoteState.Domain.User, "width")
        val key2 = RemoteNamedCacheKey(RemoteState.Domain.User, "height")
        val key3 = RemoteNamedCacheKey(RemoteState.Domain.System, "width")

        assertThat(key1).isEqualTo(key1Dup)
        assertThat(key1.hashCode()).isEqualTo(key1Dup.hashCode())

        assertThat(key1).isNotEqualTo(key2)
        assertThat(key1).isNotEqualTo(key3)
    }

    @Test
    fun remoteComponentCacheKey_Equality() {
        val key1 = RemoteComponentCacheKey(123, "center_x")
        val key1Dup = RemoteComponentCacheKey(123, "center_x")
        val key2 = RemoteComponentCacheKey(123, "center_y")
        val key3 = RemoteComponentCacheKey(456, "center_x")

        assertThat(key1).isEqualTo(key1Dup)
        assertThat(key1.hashCode()).isEqualTo(key1Dup.hashCode())

        assertThat(key1).isNotEqualTo(key2)
        assertThat(key1).isNotEqualTo(key3)
    }

    @Test
    fun remoteOperationCacheKey_Equality() {
        val op = RemoteFloat.OperationKey.Plus
        val key1 = RemoteOperationCacheKey.create(op, 10f.rf, 5f.rf)
        val key1Dup = RemoteOperationCacheKey.create(op, 10f.rf, 5f.rf)
        val key2 = RemoteOperationCacheKey.create(op, 10f.rf, 6f.rf)
        val keyDifferentOp =
            RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Minus, 10f.rf, 5f.rf)

        assertThat(key1).isEqualTo(key1Dup)
        assertThat(key1.hashCode()).isEqualTo(key1Dup.hashCode())

        assertThat(key1).isNotEqualTo(key2)
        assertThat(key1).isNotEqualTo(keyDifferentOp)
    }

    @Test
    fun remoteOperationCacheKey_EqualityWithDifferentStateInstances() {
        val op = RemoteFloat.OperationKey.Plus
        // Create two different RemoteFloat instances with the same constant value
        // They will have the same RemoteConstantCacheKey
        val state1 = RemoteFloat(10f)
        val state2 = RemoteFloat(10f)

        assertThat(state1).isNotSameInstanceAs(state2)
        assertThat(state1.cacheKey).isEqualTo(state2.cacheKey)

        val key1 = RemoteOperationCacheKey.create(op, state1, 5f.rf)
        val key2 = RemoteOperationCacheKey.create(op, state2, 5f.rf)

        assertThat(key1).isEqualTo(key2)
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode())
    }

    @Test
    fun remoteStateInstanceKey_Equality() {
        val key1 = RemoteStateInstanceKey()
        val key2 = RemoteStateInstanceKey()

        assertThat(key1).isNotEqualTo(key2)
    }

    @Test
    fun crossTypeCollisions_ToString() {
        val intOp = RemoteInt.OperationKey.ToRemoteString
        val floatOp = RemoteFloat.OperationKey.ToRemoteString

        val intKey = RemoteOperationCacheKey.create(intOp, 10.ri)
        val floatKey = RemoteOperationCacheKey.create(floatOp, 10f.rf)

        // These should NOT be equal because the operation enums are different types
        assertThat(intKey).isNotEqualTo(floatKey)
    }

    @Test
    fun crossTypeCollisions_Plus() {
        val intOp = RemoteInt.OperationKey.Add
        val floatOp = RemoteFloat.OperationKey.Plus

        val intKey = RemoteOperationCacheKey.create(intOp, 10.ri, 5.ri)
        val floatKey = RemoteOperationCacheKey.create(floatOp, 10f.rf, 5f.rf)

        assertThat(intKey).isNotEqualTo(floatKey)
    }

    @Test
    fun remoteStateIdKey_Equality() {
        val key1 = RemoteStateIdKey(10)
        val key1Dup = RemoteStateIdKey(10)
        val key2 = RemoteStateIdKey(20)

        assertThat(key1).isEqualTo(key1Dup)
        assertThat(key1.hashCode()).isEqualTo(key1Dup.hashCode())

        assertThat(key1).isNotEqualTo(key2)
    }

    @Test
    fun remoteStateIdKey_toDebugString() {
        assertThat(RemoteStateIdKey(RemoteContext.ID_CONTINUOUS_SEC).toDebugString())
            .isEqualTo("context:continuous_sec")
        assertThat(RemoteStateIdKey(RemoteContext.ID_TIME_IN_SEC).toDebugString())
            .isEqualTo("context:time_in_sec")
        assertThat(RemoteStateIdKey(RemoteContext.ID_TIME_IN_MIN).toDebugString())
            .isEqualTo("context:time_in_min")
        assertThat(RemoteStateIdKey(RemoteContext.ID_TIME_IN_HR).toDebugString())
            .isEqualTo("context:time_in_hr")
        assertThat(RemoteStateIdKey(RemoteContext.ID_WINDOW_WIDTH).toDebugString())
            .isEqualTo("context:window_width")
        assertThat(RemoteStateIdKey(RemoteContext.ID_WINDOW_HEIGHT).toDebugString())
            .isEqualTo("context:window_height")
        assertThat(RemoteStateIdKey(RemoteContext.ID_COMPONENT_WIDTH).toDebugString())
            .isEqualTo("context:component_width")
        assertThat(RemoteStateIdKey(RemoteContext.ID_COMPONENT_HEIGHT).toDebugString())
            .isEqualTo("context:component_height")
        assertThat(RemoteStateIdKey(RemoteContext.ID_CALENDAR_MONTH).toDebugString())
            .isEqualTo("context:calendar_month")
        assertThat(RemoteStateIdKey(RemoteContext.ID_OFFSET_TO_UTC).toDebugString())
            .isEqualTo("context:offset_to_utc")
        assertThat(RemoteStateIdKey(RemoteContext.ID_WEEK_DAY).toDebugString())
            .isEqualTo("context:week_day")
        assertThat(RemoteStateIdKey(RemoteContext.ID_DAY_OF_MONTH).toDebugString())
            .isEqualTo("context:day_of_month")
        assertThat(RemoteStateIdKey(RemoteContext.ID_DAY_OF_YEAR).toDebugString())
            .isEqualTo("context:day_of_year")
        assertThat(RemoteStateIdKey(RemoteContext.ID_YEAR).toDebugString())
            .isEqualTo("context:year")
        assertThat(RemoteStateIdKey(RemoteContext.ID_TOUCH_POS_X).toDebugString())
            .isEqualTo("context:touch_pos_x")
        assertThat(RemoteStateIdKey(RemoteContext.ID_TOUCH_POS_Y).toDebugString())
            .isEqualTo("context:touch_pos_y")
        assertThat(RemoteStateIdKey(RemoteContext.ID_TOUCH_VEL_X).toDebugString())
            .isEqualTo("context:touch_vel_x")
        assertThat(RemoteStateIdKey(RemoteContext.ID_TOUCH_VEL_Y).toDebugString())
            .isEqualTo("context:touch_vel_y")
        assertThat(RemoteStateIdKey(RemoteContext.ID_ACCELERATION_X).toDebugString())
            .isEqualTo("context:acceleration_x")
        assertThat(RemoteStateIdKey(RemoteContext.ID_ACCELERATION_Y).toDebugString())
            .isEqualTo("context:acceleration_y")
        assertThat(RemoteStateIdKey(RemoteContext.ID_ACCELERATION_Z).toDebugString())
            .isEqualTo("context:acceleration_z")
        assertThat(RemoteStateIdKey(RemoteContext.ID_GYRO_ROT_X).toDebugString())
            .isEqualTo("context:gyro_rot_x")
        assertThat(RemoteStateIdKey(RemoteContext.ID_GYRO_ROT_Y).toDebugString())
            .isEqualTo("context:gyro_rot_y")
        assertThat(RemoteStateIdKey(RemoteContext.ID_GYRO_ROT_Z).toDebugString())
            .isEqualTo("context:gyro_rot_z")
        assertThat(RemoteStateIdKey(RemoteContext.ID_MAGNETIC_X).toDebugString())
            .isEqualTo("context:magnetic_x")
        assertThat(RemoteStateIdKey(RemoteContext.ID_MAGNETIC_Y).toDebugString())
            .isEqualTo("context:magnetic_y")
        assertThat(RemoteStateIdKey(RemoteContext.ID_MAGNETIC_Z).toDebugString())
            .isEqualTo("context:magnetic_z")
        assertThat(RemoteStateIdKey(RemoteContext.ID_LIGHT).toDebugString())
            .isEqualTo("context:light")
        assertThat(RemoteStateIdKey(RemoteContext.ID_DENSITY).toDebugString())
            .isEqualTo("context:density")
        assertThat(RemoteStateIdKey(RemoteContext.ID_API_LEVEL).toDebugString())
            .isEqualTo("context:api_level")
        assertThat(RemoteStateIdKey(RemoteContext.ID_TOUCH_EVENT_TIME).toDebugString())
            .isEqualTo("context:touch_event_time")
        assertThat(RemoteStateIdKey(RemoteContext.ID_ANIMATION_TIME).toDebugString())
            .isEqualTo("context:animation_time")
        assertThat(RemoteStateIdKey(RemoteContext.ID_ANIMATION_DELTA_TIME).toDebugString())
            .isEqualTo("context:animation_delta_time")
        assertThat(RemoteStateIdKey(RemoteContext.ID_EPOCH_SECOND).toDebugString())
            .isEqualTo("context:epoch_second")
        assertThat(RemoteStateIdKey(RemoteContext.ID_FONT_SIZE).toDebugString())
            .isEqualTo("context:font_size")
        assertThat(RemoteStateIdKey(9999).toDebugString()).isEqualTo("context:#9999")
    }

    @Test
    fun floatArrayCacheKey_Equality() {
        val array1 = floatArrayOf(1f, 2f)
        val array2 = floatArrayOf(1f, 2f)
        val array3 = floatArrayOf(1f, 3f)

        val key1 = FloatArrayCacheKey(array1)
        val key1Dup = FloatArrayCacheKey(array2)
        val key2 = FloatArrayCacheKey(array3)

        assertThat(key1).isEqualTo(key1Dup)
        assertThat(key1.hashCode()).isEqualTo(key1Dup.hashCode())

        assertThat(key1).isNotEqualTo(key2)
    }

    @Test
    fun hashCode_IsStableAfterMemoization() {
        val keys =
            listOf(
                RemoteConstantCacheKey(10f),
                RemoteNamedCacheKey(RemoteState.Domain.User, "test"),
                RemoteStateIdKey(123),
                RemoteComponentCacheKey(1, "width"),
                RemoteOperationCacheKey.create(RemoteFloat.OperationKey.Plus, 1f.rf, 2f.rf),
                FloatArrayCacheKey(floatArrayOf(1f)),
            )

        for (key in keys) {
            val hash1 = key.hashCode()
            val hash2 = key.hashCode()
            assertThat(hash1).isEqualTo(hash2)
            assertThat(hash1).isNotEqualTo(0)
        }
    }

    @Test
    fun remoteConstantCacheKey_FloatNaN_NotSupported() {
        val t = assertFails { RemoteConstantCacheKey(Utils.asNan(101)) }
        assertThat(t.message).isEqualTo("Float constant value cannot be NaN")
    }

    @Test
    fun remoteConstantCacheKey_DoubleNaN_NotSupported() {
        val nan1 = java.lang.Double.longBitsToDouble(0x7ff0000000000001L)
        val t = assertFails { RemoteConstantCacheKey(nan1) }
        assertThat(t.message).isEqualTo("Double constant value cannot be NaN")
    }
}
