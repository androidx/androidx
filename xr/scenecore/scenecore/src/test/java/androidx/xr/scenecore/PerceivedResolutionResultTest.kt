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

package androidx.xr.scenecore

import androidx.xr.runtime.math.IntSize2d
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerceivedResolutionResultTest {

    // --- Success Tests ---

    @Test
    fun success_equals_sameObject_returnsTrue() {
        val dimensions = IntSize2d(100, 200)
        val underTest = PerceivedResolutionResult.Success(dimensions)
        assertThat(underTest.equals(underTest)).isTrue()
    }

    @Test
    fun success_equals_differentObjectsSameValues_returnsTrue() {
        val dimensions1 = IntSize2d(100, 200)
        val underTest1 = PerceivedResolutionResult.Success(dimensions1)

        val dimensions2 = IntSize2d(100, 200)
        val underTest2 = PerceivedResolutionResult.Success(dimensions2)

        assertThat(underTest1.equals(underTest2)).isTrue()
    }

    @Test
    fun success_equals_differentObjectsDifferentValues_returnsFalse() {
        val dimensions1 = IntSize2d(100, 200)
        val underTest1 = PerceivedResolutionResult.Success(dimensions1)

        val dimensions2 = IntSize2d(300, 400)
        val underTest2 = PerceivedResolutionResult.Success(dimensions2)

        assertThat(underTest1.equals(underTest2)).isFalse()
    }

    @Test
    fun success_equals_null_returnsFalse() {
        val underTest = PerceivedResolutionResult.Success(IntSize2d(100, 200))
        assertThat(underTest.equals(null)).isFalse()
    }

    @Test
    fun success_equals_differentType_returnsFalse() {
        val underTest = PerceivedResolutionResult.Success(IntSize2d(100, 200))
        val otherObject = "Not a Success object"
        assertThat(underTest.equals(otherObject)).isFalse()
    }

    @Test
    fun success_hashCode_differentObjectsSameValues_returnsSameHashCode() {
        val dimensions1 = IntSize2d(100, 200)
        val underTest1 = PerceivedResolutionResult.Success(dimensions1)

        val dimensions2 = IntSize2d(100, 200)
        val underTest2 = PerceivedResolutionResult.Success(dimensions2)

        assertThat(underTest1.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun success_hashCode_differentObjectsDifferentValues_returnsDifferentHashCodes() {
        val underTest1 = PerceivedResolutionResult.Success(IntSize2d(100, 200))
        val underTest2 = PerceivedResolutionResult.Success(IntSize2d(300, 400))

        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }

    @Test
    fun success_toString() {
        val success = PerceivedResolutionResult.Success(IntSize2d(10, 20))
        val expectedString = "PerceivedResolutionResult.Success(PerceivedResolution(10x20))"
        assertThat(success.toString()).isEqualTo(expectedString)
    }

    // --- EntityTooClose Tests ---

    @Test
    fun entityTooClose_equals_sameObject_returnsTrue() {
        val underTest = PerceivedResolutionResult.EntityTooClose()
        assertThat(underTest.equals(underTest)).isTrue()
    }

    @Test
    fun entityTooClose_equals_differentObjects_returnsTrue() {
        val underTest1 = PerceivedResolutionResult.EntityTooClose()
        val underTest2 = PerceivedResolutionResult.EntityTooClose()
        // Different instances of the same stateless class should be equal
        assertThat(underTest1.equals(underTest2)).isTrue()
    }

    @Test
    fun entityTooClose_equals_null_returnsFalse() {
        val underTest = PerceivedResolutionResult.EntityTooClose()
        assertThat(underTest.equals(null)).isFalse()
    }

    @Test
    fun entityTooClose_equals_differentType_returnsFalse() {
        val underTest = PerceivedResolutionResult.EntityTooClose()
        val otherObject = "Not an EntityTooClose object"
        assertThat(underTest.equals(otherObject)).isFalse()
    }

    @Test
    fun entityTooClose_hashCode_differentObjects_returnsSameHashCode() {
        val underTest1 = PerceivedResolutionResult.EntityTooClose()
        val underTest2 = PerceivedResolutionResult.EntityTooClose()
        // HashCode for stateless objects of the same type should be the same
        assertThat(underTest1.hashCode()).isEqualTo(underTest2.hashCode())
    }

    // --- InvalidCameraView Tests ---

    @Test
    fun invalidCameraView_equals_sameObject_returnsTrue() {
        val underTest = PerceivedResolutionResult.InvalidCameraView()
        assertThat(underTest.equals(underTest)).isTrue()
    }

    @Test
    fun invalidCameraView_equals_differentObjects_returnsTrue() {
        val underTest1 = PerceivedResolutionResult.InvalidCameraView()
        val underTest2 = PerceivedResolutionResult.InvalidCameraView()
        // Different instances of the same stateless class should be equal
        assertThat(underTest1.equals(underTest2)).isTrue()
    }

    @Test
    fun invalidCameraView_equals_null_returnsFalse() {
        val underTest = PerceivedResolutionResult.InvalidCameraView()
        assertThat(underTest.equals(null)).isFalse()
    }

    @Test
    fun invalidCameraView_equals_differentType_returnsFalse() {
        val underTest = PerceivedResolutionResult.InvalidCameraView()
        val otherObject = "Not an InvalidCameraView object"
        assertThat(underTest.equals(otherObject)).isFalse()
    }

    @Test
    fun invalidCameraView_hashCode_differentObjects_returnsSameHashCode() {
        val underTest1 = PerceivedResolutionResult.InvalidCameraView()
        val underTest2 = PerceivedResolutionResult.InvalidCameraView()
        // HashCode for stateless objects of the same type should be the same
        assertThat(underTest1.hashCode()).isEqualTo(underTest2.hashCode())
    }

    // --- Cross-Type Inequality Tests ---

    @Test
    fun success_notEquals_entityTooClose() {
        val success = PerceivedResolutionResult.Success(IntSize2d(10, 20))
        val entityTooClose = PerceivedResolutionResult.EntityTooClose()
        assertThat(success.equals(entityTooClose)).isFalse()
        assertThat(entityTooClose.equals(success)).isFalse() // Check commutativity
    }

    @Test
    fun success_notEquals_invalidCameraView() {
        val success = PerceivedResolutionResult.Success(IntSize2d(10, 20))
        val invalidCameraView = PerceivedResolutionResult.InvalidCameraView()
        assertThat(success.equals(invalidCameraView)).isFalse()
        assertThat(invalidCameraView.equals(success)).isFalse() // Check commutativity
    }

    @Test
    fun entityTooClose_notEquals_invalidCameraView() {
        val entityTooClose = PerceivedResolutionResult.EntityTooClose()
        val invalidCameraView = PerceivedResolutionResult.InvalidCameraView()
        assertThat(entityTooClose.equals(invalidCameraView)).isFalse()
        assertThat(invalidCameraView.equals(entityTooClose)).isFalse() // Check commutativity
    }
}
