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

package androidx.xr.scenecore

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpatialVisibilityTest {

    @Test
    fun equals_sameObject_returnsTrue() {
        val underTest = SpatialVisibility(SpatialVisibility.WITHIN_FOV)

        assertThat(underTest.equals(underTest)).isTrue()
    }

    @Test
    fun equals_differentObjectsSameValues_returnsTrue() {
        val underTest1 = SpatialVisibility(SpatialVisibility.WITHIN_FOV)
        val underTest2 = SpatialVisibility(SpatialVisibility.WITHIN_FOV)

        assertThat(underTest1.equals(underTest2)).isTrue()
    }

    @Test
    fun equals_differentObjectsDifferentValues_returnsFalse() {
        val underTest1 = SpatialVisibility(SpatialVisibility.WITHIN_FOV)
        val underTest2 = SpatialVisibility(SpatialVisibility.OUTSIDE_FOV)

        assertThat(underTest1.equals(underTest2)).isFalse()
    }

    @Test
    fun hashCode_differentObjectsSameValues_returnsSameHashCode() {
        val underTest1 = SpatialVisibility(SpatialVisibility.WITHIN_FOV)
        val underTest2 = SpatialVisibility(SpatialVisibility.WITHIN_FOV)

        assertThat(underTest1.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCode_differentObjectsDifferentValues_returnsDifferentHashCodes() {
        val underTest1 = SpatialVisibility(SpatialVisibility.WITHIN_FOV)
        val underTest2 = SpatialVisibility(SpatialVisibility.OUTSIDE_FOV)

        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }

    @Test
    fun toString_containsCorrectString() {
        assertThat(SpatialVisibility(SpatialVisibility.UNKNOWN).toString()).contains("UNKNOWN")
        assertThat(SpatialVisibility(SpatialVisibility.OUTSIDE_FOV).toString())
            .contains("OUTSIDE_FOV")
        assertThat(SpatialVisibility(SpatialVisibility.PARTIALLY_WITHIN_FOV).toString())
            .contains("PARTIALLY_WITHIN_FOV")
        assertThat(SpatialVisibility(SpatialVisibility.WITHIN_FOV).toString())
            .contains("WITHIN_FOV")
    }

    @Test
    fun toString_containsUnknownStringForInvalidValue() {
        assertThat(SpatialVisibility(100).toString()).contains("UNKNOWN")
    }
}
