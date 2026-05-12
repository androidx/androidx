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

import androidx.ink.brush.behavior.InterpolationNode.Interpolation
import androidx.ink.nativeloader.testing.awaitNativePointerCleanupAfter
import androidx.kruth.assertThat
import kotlin.test.Test

class InterpolationNodeTest {

    @Test
    fun interpolationNodeNativePointers_cleanedUpWhenOutOfScope() {
        awaitNativePointerCleanupAfter {
            val unused =
                InterpolationNode(
                    Interpolation.LERP,
                    ConstantNode(0.5f),
                    ConstantNode(0f),
                    ConstantNode(1f),
                )
        }
    }

    @Test
    fun interpolationToString_returnsCorrectString() {
        assertThat(Interpolation.LERP.toString()).isEqualTo("Interpolation.LERP")
        assertThat(Interpolation.INVERSE_LERP.toString()).isEqualTo("Interpolation.INVERSE_LERP")
    }

    @Test
    fun interpolationNodeInputs_containsInputsInOrder() {
        val paramInput = ConstantNode(0.5f)
        val startInput = ConstantNode(0f)
        val endInput = ConstantNode(1f)
        val node =
            InterpolationNode(
                interpolation = Interpolation.LERP,
                paramInput = paramInput,
                startInput = startInput,
                endInput = endInput,
            )
        assertThat(node.inputs).containsExactly(paramInput, startInput, endInput).inOrder()
    }

    @Test
    fun interpolationNodeToString() {
        val node =
            InterpolationNode(
                Interpolation.LERP,
                ConstantNode(0.5f),
                ConstantNode(0f),
                ConstantNode(1f),
            )
        assertThat(node.toString())
            .isEqualTo(
                "InterpolationNode(LERP, ConstantNode(0.5), ConstantNode(0.0), ConstantNode(1.0))"
            )
    }

    @Test
    fun interpolationNodeEquals_checksEqualityOfValues() {
        val node1 =
            InterpolationNode(
                Interpolation.LERP,
                ConstantNode(0.5f),
                ConstantNode(0f),
                ConstantNode(1f),
            )
        val node2 =
            InterpolationNode(
                Interpolation.LERP,
                ConstantNode(0.5f),
                ConstantNode(0f),
                ConstantNode(1f),
            )
        val node3 =
            InterpolationNode(
                Interpolation.LERP,
                ConstantNode(0.5f),
                ConstantNode(0f),
                ConstantNode(2f),
            )
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun interpolationNodeHashCode_withIdenticalValues_match() {
        val node1 =
            InterpolationNode(
                Interpolation.LERP,
                ConstantNode(0.5f),
                ConstantNode(0f),
                ConstantNode(1f),
            )
        val node2 =
            InterpolationNode(
                Interpolation.LERP,
                ConstantNode(0.5f),
                ConstantNode(0f),
                ConstantNode(1f),
            )
        val node3 =
            InterpolationNode(
                Interpolation.LERP,
                ConstantNode(0.5f),
                ConstantNode(0f),
                ConstantNode(2f),
            )
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }
}
