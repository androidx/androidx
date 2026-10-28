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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteIntrinsicSizeTest {

    @Test
    fun constants_haveExpectedValues() {
        assertThat(RemoteIntrinsicSize.Min.value).isEqualTo(Type.INTRINSIC_MIN.ordinal)
        assertThat(RemoteIntrinsicSize.Max.value).isEqualTo(Type.INTRINSIC_MAX.ordinal)
    }

    @Test
    fun toString_returnsExpectedNames() {
        assertThat(RemoteIntrinsicSize.Min.toString()).isEqualTo("Min")
        assertThat(RemoteIntrinsicSize.Max.toString()).isEqualTo("Max")
        assertThat(RemoteIntrinsicSize(-1).toString()).isEqualTo("Unknown")
    }

    @Test
    fun equalityAndHashCode() {
        val min1 = RemoteIntrinsicSize.Min
        val min2 = RemoteIntrinsicSize(Type.INTRINSIC_MIN)
        assertThat(min1).isEqualTo(min2)
        assertThat(min1.hashCode()).isEqualTo(min2.hashCode())
        assertThat(min1).isNotEqualTo(RemoteIntrinsicSize.Max)
    }
}
