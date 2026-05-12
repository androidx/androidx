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

package androidx.ink.brush.behavior

import androidx.ink.nativeloader.testing.awaitNativePointerCleanupAfter
import androidx.kruth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class NoiseNodeTest {

    @Test
    fun noiseNodeNativePointers_cleanedUpWhenOutOfScope() {
        awaitNativePointerCleanupAfter {
            val unused = NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, 1f)
        }
    }

    @Test
    fun noiseNodeConstructor_throwsForNonFiniteBasePeriod() {
        assertFailsWith<IllegalArgumentException> {
            NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, Float.NaN)
        }
    }

    @Test
    fun noiseNodeConstructor_throwsForNegativeBasePeriod() {
        assertFailsWith<IllegalArgumentException> {
            NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, -1f)
        }
    }

    @Test
    fun noiseNodeToString() {
        val node = NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, 1f)
        assertThat(node.toString()).isEqualTo("NoiseNode(12345, TIME_IN_SECONDS, 1.0)")
    }

    @Test
    fun noiseNodeEquals_checksEqualityOfValues() {
        val node = NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, 1f)
        assertThat(node).isEqualTo(NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, 1f))
        assertThat(node).isNotEqualTo(NoiseNode(12346, ProgressDomain.TIME_IN_SECONDS, 1f))
        assertThat(node).isNotEqualTo(NoiseNode(12345, ProgressDomain.DISTANCE_IN_CENTIMETERS, 1f))
        assertThat(node).isNotEqualTo(NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, 2f))
    }

    @Test
    fun noiseNodeHashCode_withIdenticalValues_match() {
        val node1 = NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, 1f)
        val node2 = NoiseNode(12345, ProgressDomain.TIME_IN_SECONDS, 1f)
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
    }
}
