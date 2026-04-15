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

import androidx.ink.brush.ExperimentalInkCustomBrushApi
import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class IntegralNodeTest {

    @Test
    fun integralNodeConstructor_throwsForNonFiniteValueRange() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = Float.NaN,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                input,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = Float.POSITIVE_INFINITY,
                OutOfRange.REPEAT,
                input,
            )
        }
    }

    @Test
    fun integralNodeConstructor_throwsForEmptyValueRange() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 5f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                input,
            )
        }
    }

    @Test
    fun integralNodeInputs_containsInput() {
        val input = ConstantNode(0f)
        val node =
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                input,
            )
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun integralNodeToString() {
        val input = ConstantNode(0f)
        val node =
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                input,
            )
        assertThat(node.toString())
            .isEqualTo("IntegralNode(TIME_IN_SECONDS, 0.0, 5.0, REPEAT, ConstantNode(0.0))")
    }

    @Test
    fun integralNodeEquals_checksEqualityOfValues() {
        val node1 =
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                ConstantNode(1f),
            )
        val node2 =
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                ConstantNode(1f),
            )
        val node3 =
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun integralNodeHashCode_withIdenticalValues_match() {
        val node1 =
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                ConstantNode(1f),
            )
        val node2 =
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                ConstantNode(1f),
            )
        val node3 =
            IntegralNode(
                ProgressDomain.TIME_IN_SECONDS,
                integralValueRangeStart = 0f,
                integralValueRangeEnd = 5f,
                OutOfRange.REPEAT,
                ConstantNode(2f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }
}
