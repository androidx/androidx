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

import androidx.ink.brush.behavior.PolarTargetNode.PolarTarget
import androidx.ink.nativeloader.testing.awaitNativePointerCleanupAfter
import androidx.kruth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PolarTargetNodeTest {

    @Test
    fun polarTargetNodeNativePointers_cleanedUpWhenOutOfScope() {
        awaitNativePointerCleanupAfter {
            val unused =
                PolarTargetNode(
                    PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                    0f,
                    1f,
                    ConstantNode(0f),
                    0f,
                    1f,
                    ConstantNode(1f),
                )
        }
    }

    @Test
    fun polarTargetToString_returnsCorrectString() {
        assertThat(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE
                    .toString()
            )
            .isEqualTo(
                "PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE"
            )
        assertThat(
                PolarTarget.POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE
                    .toString()
            )
            .isEqualTo(
                "PolarTarget.POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE"
            )
    }

    @Test
    fun polarTargetNodeConstructor_throwsForNonFiniteAngleRange() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                Float.NaN,
                1f,
                input,
                0f,
                1f,
                input,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                Float.POSITIVE_INFINITY,
                input,
                0f,
                1f,
                input,
            )
        }
    }

    @Test
    fun polarTargetNodeConstructor_throwsForNonFiniteMagnitudeRange() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                input,
                Float.NaN,
                1f,
                input,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                input,
                0f,
                Float.POSITIVE_INFINITY,
                input,
            )
        }
    }

    @Test
    fun polarTargetNodeConstructor_throwsForEmptyAngleRange() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0.5f,
                0.5f,
                input,
                0f,
                1f,
                input,
            )
        }
    }

    @Test
    fun polarTargetNodeConstructor_throwsForEmptyMagnitudeRange() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                input,
                0.5f,
                0.5f,
                input,
            )
        }
    }

    @Test
    fun polarTargetNodeInputs_containsInputs() {
        val angleInput = ConstantNode(0f)
        val magnitudeInput = ConstantNode(1f)
        val node =
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                angleInput,
                0f,
                1f,
                magnitudeInput,
            )
        assertThat(node.inputs).containsExactly(angleInput, magnitudeInput).inOrder()
    }

    @Test
    fun polarTargetNodeToString() {
        val angleInput = ConstantNode(2f)
        val magnitudeInput = ConstantNode(5f)
        val node =
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                angleInput,
                3f,
                4f,
                magnitudeInput,
            )
        assertThat(node.toString())
            .isEqualTo(
                "PolarTargetNode(POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE, 0.0, 1.0, ConstantNode(2.0), 3.0, 4.0, ConstantNode(5.0))"
            )
    }

    @Test
    fun polarTargetNodeEquals_checksEqualityOfValues() {
        val node1 =
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                ConstantNode(2f),
                3f,
                4f,
                ConstantNode(5f),
            )
        val node2 =
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                ConstantNode(2f),
                3f,
                4f,
                ConstantNode(5f),
            )
        val node3 =
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                ConstantNode(2f),
                3f,
                4f,
                ConstantNode(67f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun polarTargetNodeHashCode_withIdenticalValues_match() {
        val node1 =
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                ConstantNode(2f),
                3f,
                4f,
                ConstantNode(5f),
            )
        val node2 =
            PolarTargetNode(
                PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
                0f,
                1f,
                ConstantNode(2f),
                3f,
                4f,
                ConstantNode(5f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
    }
}
