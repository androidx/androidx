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

package androidx.xr.compose.unit

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalSpatialVolumeOffsetApi::class)
@RunWith(JUnit4::class)
class IntVolumeOffsetTest {

    @Test
    fun intVolumeOffset_zero_hasZeroDimensions() {
        assertThat(IntVolumeOffset.Zero).isEqualTo(IntVolumeOffset(0, 0, 0))
    }

    @Test
    fun intVolumeOffset_equals_sameInstance_returnsTrue() {
        val offset = IntVolumeOffset(10, 20, 30)

        assertThat(offset).isEqualTo(offset)
    }

    @Test
    fun intVolumeOffset_equalsAndHashCode_worksCorrectly() {
        val offset1 = IntVolumeOffset(10, 20, 30)
        val offset2 = IntVolumeOffset(10, 20, 30)
        val offsetDiffX = IntVolumeOffset(11, 20, 30)
        val offsetDiffY = IntVolumeOffset(10, 21, 30)
        val offsetDiffZ = IntVolumeOffset(10, 20, 31)

        assertThat(offset1).isEqualTo(offset2)
        assertThat(offset1.hashCode()).isEqualTo(offset2.hashCode())
        assertThat(offset1).isNotEqualTo(offsetDiffX)
        assertThat(offset1).isNotEqualTo(offsetDiffY)
        assertThat(offset1).isNotEqualTo(offsetDiffZ)
        assertThat(offset1.hashCode()).isNotEqualTo(offsetDiffX.hashCode())
    }

    @Test
    fun intVolumeOffset_equals_nullOrDifferentType_returnsFalse() {
        val offset = IntVolumeOffset(10, 20, 30)

        assertThat(offset).isNotEqualTo(null)
        assertThat(offset).isNotEqualTo("Dummy String")
    }

    @Test
    fun intVolumeOffset_toString_returnsExpectedString() {
        val offset = IntVolumeOffset(1, 2, 3)

        assertThat(offset.toString()).isEqualTo("IntVolumeOffset(x=1, y=2, z=3)")
    }
}
