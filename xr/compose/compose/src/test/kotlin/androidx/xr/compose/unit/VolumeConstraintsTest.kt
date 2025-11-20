/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VolumeConstraintsTest {

    @Test
    fun volumeConstraints_hasBoundedWidth_returnsTrue() {
        val volumeConstraints = VolumeConstraints(0, 0, 0, 0, 0, 0)

        val hasBoundedWidth = volumeConstraints.hasBoundedWidth

        assertThat(hasBoundedWidth).isTrue()
    }

    @Test
    fun volumeConstraints_hasBoundedWidth_returnsFalse() {
        val volumeConstraints = VolumeConstraints(0, VolumeConstraints.INFINITY, 0, 0, 0, 0)

        val hasBoundedWidth = volumeConstraints.hasBoundedWidth

        assertThat(hasBoundedWidth).isFalse()
    }

    @Test
    fun volumeConstraints_hasBoundedHeight_returnsTrue() {
        val volumeConstraints = VolumeConstraints(0, 0, 0, 0, 0, 0)

        val hasBoundedHeight = volumeConstraints.hasBoundedHeight

        assertThat(hasBoundedHeight).isTrue()
    }

    @Test
    fun volumeConstraints_hasBoundedHeight_returnsFalse() {
        val volumeConstraints = VolumeConstraints(0, 0, 0, VolumeConstraints.INFINITY, 0, 0)

        val hasBoundedHeight = volumeConstraints.hasBoundedHeight

        assertThat(hasBoundedHeight).isFalse()
    }

    @Test
    fun volumeConstraints_hasBoundedDepth_returnsTrue() {
        val volumeConstraints = VolumeConstraints(0, 0, 0, 0, 0, 0)

        val hasBoundedDepth = volumeConstraints.hasBoundedDepth

        assertThat(hasBoundedDepth).isTrue()
    }

    @Test
    fun volumeConstraints_hasBoundedDepth_returnsFalse() {
        val volumeConstraints = VolumeConstraints(0, 0, 0, 0, 0, VolumeConstraints.INFINITY)

        val hasBoundedDepth = volumeConstraints.hasBoundedDepth

        assertThat(hasBoundedDepth).isFalse()
    }

    @Test
    fun volumeConstraints_toString_returnsString() {
        val volumeConstraints = VolumeConstraints(0, 0, 0, 0, 0, 0)

        val toString = volumeConstraints.toString()

        assertThat(toString).isEqualTo("width: 0-0, height: 0-0, depth=0-0")
    }

    @Test
    fun volumeConstraints_copy_returnsCorrectVolumeConstraints() {
        val volumeConstraints = VolumeConstraints(0, 0, 0, 0, 0, 0)

        val copyVolumeConstraints = volumeConstraints.copy()

        assertThat(copyVolumeConstraints).isEqualTo(volumeConstraints)
    }

    @Test
    fun volumeConstraints_infinity_returnsCorrectVolumeConstraints() {
        val infinity = VolumeConstraints.INFINITY

        assertThat(infinity).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun volumeConstraints_default_returnsCorrectVolumeConstraints() {
        val defaultVolumeConstraints = VolumeConstraints()

        assertThat(defaultVolumeConstraints.minWidth).isEqualTo(0)
        assertThat(defaultVolumeConstraints.maxWidth).isEqualTo(VolumeConstraints.INFINITY)
        assertThat(defaultVolumeConstraints.minHeight).isEqualTo(0)
        assertThat(defaultVolumeConstraints.maxHeight).isEqualTo(VolumeConstraints.INFINITY)
        assertThat(defaultVolumeConstraints.minDepth).isEqualTo(0)
        assertThat(defaultVolumeConstraints.maxDepth).isEqualTo(VolumeConstraints.INFINITY)
    }

    @Test
    fun volumeConstraints_constrain_returnsCorrectVolumeConstraints() {
        val volumeConstraints = VolumeConstraints(1, 2, 1, 2, 1, 2)
        val otherVolumeConstraints = VolumeConstraints(4, 5, 4, 5, 4, 5)

        val constrainedVolumeConstraints = volumeConstraints.constrain(otherVolumeConstraints)

        assertThat(constrainedVolumeConstraints).isEqualTo(VolumeConstraints(2, 2, 2, 2, 2, 2))
    }

    @Test
    fun volumeConstraints_constrainWidth_returnsCorrectWidth() {
        val volumeConstraints = VolumeConstraints(1, 2, 1, 2, 1, 2)

        val constrainedWidth = volumeConstraints.constrainWidth(3)

        assertThat(constrainedWidth).isEqualTo(2)
    }

    @Test
    fun volumeConstraints_constrainHeight_returnsCorrectHeight() {
        val volumeConstraints = VolumeConstraints(1, 2, 1, 2, 1, 2)

        val constrainedHeight = volumeConstraints.constrainHeight(3)

        assertThat(constrainedHeight).isEqualTo(2)
    }

    @Test
    fun volumeConstraints_constrainDepth_returnsCorrectDepth() {
        val volumeConstraints = VolumeConstraints(1, 2, 1, 2, 1, 2)

        val constrainedDepth = volumeConstraints.constrainDepth(3)

        assertThat(constrainedDepth).isEqualTo(2)
    }

    @Test
    fun volumeConstraints_offset_returnsCorrectVolumeConstraints() {
        val volumeConstraints = VolumeConstraints(1, 2, 1, 2, 1, 2)

        val offsetVolumeConstraints = volumeConstraints.offset(1, 2, 2)

        assertThat(offsetVolumeConstraints).isEqualTo(VolumeConstraints(2, 3, 3, 4, 3, 4))
    }
}
