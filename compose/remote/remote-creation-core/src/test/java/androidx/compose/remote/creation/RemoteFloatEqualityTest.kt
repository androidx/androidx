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

package androidx.compose.remote.creation

import androidx.compose.remote.core.operations.Utils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteFloatEqualityTest {
    private val idA = Utils.asNan(42)
    private val idB = Utils.asNan(43)

    @Test
    fun contentEquals_treatsDifferentNanIdsAsEqual() {
        // Documents the underlying problem these helpers work around.
        assertThat(floatArrayOf(idA).contentEquals(floatArrayOf(idB))).isTrue()
    }

    @Test
    fun remoteFloatEquals_comparesNanIds() {
        assertThat(remoteFloatEquals(idA, Utils.asNan(42))).isTrue()
        assertThat(remoteFloatEquals(idA, idB)).isFalse()
        assertThat(remoteFloatEquals(idA, 42f)).isFalse()
        assertThat(remoteFloatEquals(42f, idA)).isFalse()
    }

    @Test
    fun remoteFloatEquals_comparesNumbersByRawBits() {
        assertThat(remoteFloatEquals(1.5f, 1.5f)).isTrue()
        assertThat(remoteFloatEquals(1.5f, 2.5f)).isFalse()
        assertThat(remoteFloatEquals(0f, -0f)).isFalse()
    }

    @Test
    fun remoteContentEquals_distinguishesIdsAndOperators() {
        val add = floatArrayOf(idA, 2f, Rc.FloatExpression.ADD)

        assertThat(add.remoteContentEquals(floatArrayOf(idA, 2f, Rc.FloatExpression.ADD))).isTrue()
        assertThat(add.remoteContentEquals(floatArrayOf(idB, 2f, Rc.FloatExpression.ADD))).isFalse()
        assertThat(add.remoteContentEquals(floatArrayOf(idA, 2f, Rc.FloatExpression.MUL))).isFalse()
        assertThat(add.remoteContentEquals(floatArrayOf(idA, 2f))).isFalse()
        assertThat(add.remoteContentEquals(null)).isFalse()
        assertThat((null as FloatArray?).remoteContentEquals(null)).isTrue()
    }

    @Test
    fun remoteContentHashCode_isConsistentWithEquals() {
        val a = floatArrayOf(idA, 2f, Rc.FloatExpression.ADD)
        val b = floatArrayOf(Utils.asNan(42), 2f, Rc.FloatExpression.ADD)

        assertThat(a.remoteContentHashCode()).isEqualTo(b.remoteContentHashCode())
        assertThat(a.remoteContentHashCode())
            .isNotEqualTo(floatArrayOf(idB, 2f, Rc.FloatExpression.ADD).remoteContentHashCode())
        assertThat((null as FloatArray?).remoteContentHashCode()).isEqualTo(0)
    }
}
