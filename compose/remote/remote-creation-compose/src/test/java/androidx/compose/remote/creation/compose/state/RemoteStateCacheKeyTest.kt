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

import com.google.common.truth.Truth.assertThat
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
}
