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

package androidx.xr.compose.subspace.animation.follow

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TrackedDimensionsTest {

    @Test
    fun trackedDimensions_equals_sameInstance_returnsTrue() {
        val dimensions = TrackedDimensions.All

        assertThat(dimensions).isEqualTo(dimensions)
    }

    @Test
    fun trackedDimensions_equals_sameProperties_returnsTrue() {
        val dim1 =
            TrackedDimensions(
                isXTracked = true,
                isYTracked = false,
                isZTracked = true,
                isPitchTracked = false,
                isYawTracked = true,
                isRollTracked = false,
            )
        val dim2 =
            TrackedDimensions(
                isXTracked = true,
                isYTracked = false,
                isZTracked = true,
                isPitchTracked = false,
                isYawTracked = true,
                isRollTracked = false,
            )

        assertThat(dim1).isEqualTo(dim2)
    }

    @Test
    fun trackedDimensions_equals_differentDimensions_returnsFalse() {
        val base =
            TrackedDimensions(
                isXTracked = true,
                isYTracked = true,
                isZTracked = true,
                isPitchTracked = true,
                isYawTracked = true,
                isRollTracked = true,
            )

        assertThat(base).isNotEqualTo(base.copy(isXTracked = false))
        assertThat(base).isNotEqualTo(base.copy(isYTracked = false))
        assertThat(base).isNotEqualTo(base.copy(isZTracked = false))
        assertThat(base).isNotEqualTo(base.copy(isPitchTracked = false))
        assertThat(base).isNotEqualTo(base.copy(isYawTracked = false))
        assertThat(base).isNotEqualTo(base.copy(isRollTracked = false))
    }

    @Test
    fun trackedDimensions_equals_nullOrDifferentType_returnsFalse() {
        val dimensions = TrackedDimensions.All

        assertFalse(dimensions.equals(null))
        assertFalse(dimensions.equals("Dummy String"))
    }

    @Test
    fun trackedDimensions_hashCode_sameProperties_matches() {
        val dim1 =
            TrackedDimensions(
                isXTracked = true,
                isYTracked = false,
                isZTracked = true,
                isPitchTracked = false,
                isYawTracked = true,
                isRollTracked = false,
            )
        val dim2 =
            TrackedDimensions(
                isXTracked = true,
                isYTracked = false,
                isZTracked = true,
                isPitchTracked = false,
                isYawTracked = true,
                isRollTracked = false,
            )

        assertThat(dim1.hashCode()).isEqualTo(dim2.hashCode())
    }

    @Test
    fun trackedDimensions_hashCode_differentProperties_differs() {
        val dim1 = TrackedDimensions.All
        val dim2 = TrackedDimensions.RotationOnly

        assertThat(dim1.hashCode()).isNotEqualTo(dim2.hashCode())
    }
}
