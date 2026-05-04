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

import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DampingNodeTest {

    @Test
    fun dampingNodeConstructor_throwsForNonFiniteDampingGap() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            DampingNode(ProgressDomain.TIME_IN_SECONDS, Float.POSITIVE_INFINITY, input)
        }
        assertFailsWith<IllegalArgumentException> {
            DampingNode(ProgressDomain.TIME_IN_SECONDS, Float.NaN, input)
        }
    }

    @Test
    fun dampingNodeConstructor_throwsForNegativeDampingGap() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            DampingNode(ProgressDomain.TIME_IN_SECONDS, -1f, input)
        }
    }

    @Test
    fun dampingNodeInputs_containsInput() {
        val input = ConstantNode(0f)
        val node = DampingNode(ProgressDomain.TIME_IN_SECONDS, 1f, input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun dampingNodeToString() {
        val input = ConstantNode(0f)
        val node = DampingNode(ProgressDomain.TIME_IN_SECONDS, 1f, input)
        assertThat(node.toString())
            .isEqualTo("DampingNode(TIME_IN_SECONDS, 1.0, ConstantNode(0.0))")
    }

    @Test
    fun dampingNodeEquals_checksEqualityOfValues() {
        val node1 = DampingNode(ProgressDomain.TIME_IN_SECONDS, 1f, ConstantNode(1f))
        val node2 = DampingNode(ProgressDomain.TIME_IN_SECONDS, 1f, ConstantNode(1f))
        val node3 = DampingNode(ProgressDomain.TIME_IN_SECONDS, 1f, ConstantNode(2f))
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun dampingNodeHashCode_withIdenticalValues_match() {
        val node1 = DampingNode(ProgressDomain.TIME_IN_SECONDS, 1f, ConstantNode(1f))
        val node2 = DampingNode(ProgressDomain.TIME_IN_SECONDS, 1f, ConstantNode(1f))
        val node3 = DampingNode(ProgressDomain.TIME_IN_SECONDS, 1f, ConstantNode(2f))
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun dampingNodeConstructor_withInvalidArguments_throws() {
        // responseTimeMillis less than 0L
        val responseTimeMillisError =
            assertFailsWith<IllegalArgumentException> {
                DampingNode(
                    dampingSource = ProgressDomain.TIME_IN_SECONDS,
                    dampingGap = -0.001f, // Less than 0.
                    input = ConstantNode(0f),
                )
            }
        assertThat(responseTimeMillisError.message).contains("damping_gap")
        assertThat(responseTimeMillisError.message).contains("non-negative")
    }

    @Test
    fun dampingNodeEquals_withDifferentValues_returnsFalse() {
        val original =
            DampingNode(
                dampingSource = ProgressDomain.TIME_IN_SECONDS,
                dampingGap = 0.001f,
                input = ConstantNode(0f),
            )
        assertThat(
                original.equals(
                    DampingNode(
                        dampingSource = ProgressDomain.DISTANCE_IN_CENTIMETERS, // different
                        dampingGap = 0.001f,
                        input = ConstantNode(0f),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    DampingNode(
                        dampingSource = ProgressDomain.TIME_IN_SECONDS,
                        dampingGap = 0.035f, // different
                        input = ConstantNode(0f),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    DampingNode(
                        dampingSource = ProgressDomain.TIME_IN_SECONDS,
                        dampingGap = 0.001f,
                        input = ConstantNode(1f), // different
                    )
                )
            )
            .isFalse()
    }
}
