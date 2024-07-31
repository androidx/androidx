/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MutableAffineTransformTest {

    @Test
    fun defaultConstructor_shouldBeEqualToIdentity() {
        val identity = MutableAffineTransform()

        assertThat(identity).isEqualTo(AffineTransform.IDENTITY)
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val affineTransform = MutableAffineTransform(A, B, C, D, E, F)

        // Ensure test coverage of the same-instance case, but call .equals directly for lint.
        assertThat(affineTransform.equals(affineTransform)).isTrue()
        assertThat(affineTransform.hashCode()).isEqualTo(affineTransform.hashCode())
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val affineTransform = MutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = MutableAffineTransform(A, B, C, D, E, F)

        assertThat(affineTransform).isEqualTo(otherTransform)
        assertThat(affineTransform.hashCode()).isEqualTo(otherTransform.hashCode())
    }

    @Test
    fun equals_whenSameInterfacePropertiesAndDifferentType_returnsTrue() {
        val immutable = ImmutableAffineTransform(A, B, C, D, E, F)
        val mutable = MutableAffineTransform(A, B, C, D, E, F)

        assertThat(mutable).isEqualTo(immutable)
    }

    @Test
    fun equals_whenDifferentA_returnsFalse() {
        val affineTransform = MutableAffineTransform(1f, B, C, D, E, F)
        val otherTransform = MutableAffineTransform(10f, B, C, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentB_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, 2f, C, D, E, F)
        val otherTransform = MutableAffineTransform(A, 20f, C, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentC_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, B, 3f, D, E, F)
        val otherTransform = MutableAffineTransform(A, B, 30f, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentD_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, B, C, 4f, E, F)
        val otherTransform = MutableAffineTransform(A, B, C, 40f, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentE_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, B, C, D, 5f, F)
        val otherTransform = MutableAffineTransform(A, B, C, D, 50f, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentF_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, B, C, D, E, 6f)
        val otherTransform = MutableAffineTransform(A, B, C, D, E, 60f)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun asImmutable_returnsEquivalentImmutableAffineTransform() {
        val affineTransform = MutableAffineTransform(A, B, C, D, E, F)

        val output = affineTransform.asImmutable()

        assertThat(output).isEqualTo(ImmutableAffineTransform(A, B, C, D, E, F))
        assertThat(output).isInstanceOf(ImmutableAffineTransform::class.java)
    }

    companion object {
        private const val A = 1f

        private const val B = 2f

        private const val C = 3f

        private const val D = 4f

        private const val E = 5f

        private const val F = 6f
    }
}
