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

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DpVolumeOffsetTest {

    @Test
    fun dpVolumeOffset_zero_hasZeroDimensions() {
        assertThat(DpVolumeOffset.Zero).isEqualTo(DpVolumeOffset(0.dp, 0.dp, 0.dp))
    }

    @Test
    fun dpVolumeOffset_defaultConstructor_returnsZero() {
        assertThat(DpVolumeOffset()).isEqualTo(DpVolumeOffset.Zero)
    }

    @Test
    fun dpVolumeOffset_equals_sameInstance_returnsTrue() {
        val offset = DpVolumeOffset(10.dp, 20.dp, 30.dp)

        assertThat(offset).isEqualTo(offset)
    }

    @Test
    fun dpVolumeOffset_equalsAndHashCode_worksCorrectly() {
        val offset1 = DpVolumeOffset(10.dp, 20.dp, 30.dp)
        val offset2 = DpVolumeOffset(10.dp, 20.dp, 30.dp)
        val offsetDiffX = DpVolumeOffset(11.dp, 20.dp, 30.dp)
        val offsetDiffY = DpVolumeOffset(10.dp, 21.dp, 30.dp)
        val offsetDiffZ = DpVolumeOffset(10.dp, 20.dp, 31.dp)

        assertThat(offset1).isEqualTo(offset2)
        assertThat(offset1.hashCode()).isEqualTo(offset2.hashCode())
        assertThat(offset1).isNotEqualTo(offsetDiffX)
        assertThat(offset1).isNotEqualTo(offsetDiffY)
        assertThat(offset1).isNotEqualTo(offsetDiffZ)
        assertThat(offset1.hashCode()).isNotEqualTo(offsetDiffX.hashCode())
    }

    @Test
    fun dpVolumeOffset_equals_nullOrDifferentType_returnsFalse() {
        val offset = DpVolumeOffset(10.dp, 20.dp, 30.dp)

        assertThat(offset).isNotEqualTo(null)
        assertThat(offset).isNotEqualTo("Dummy String")
    }

    @Test
    fun dpVolumeOffset_toString_returnsExpectedString() {
        val offset = DpVolumeOffset(1.dp, 2.dp, 3.dp)

        assertThat(offset.toString()).isEqualTo("DpVolumeOffset(x=1.0.dp, y=2.0.dp, z=3.0.dp)")
    }
}
