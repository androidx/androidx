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
import kotlin.test.Test

class ResponseNodeTest {

    @Test
    fun responseNodeNativePointers_cleanedUpWhenOutOfScope() {
        awaitNativePointerCleanupAfter {
            val unused = ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(0f))
        }
    }

    @Test
    fun responseNode_usesPassedInEasingFunction() {
        val easingFunction = EasingFunction.CubicBezier(0f, 0f, 1f, 1f)
        val input = ConstantNode(0f)
        val node = ResponseNode(easingFunction, input)
        assertThat(node.responseCurve).isSameInstanceAs(easingFunction)
    }

    @Test
    fun responseNodeInputs_containsInput() {
        val input = ConstantNode(0f)
        val node = ResponseNode(EasingFunction.Predefined.EASE, input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun responseNodeToString() {
        val input = ConstantNode(0f)
        val node = ResponseNode(EasingFunction.Predefined.EASE, input)
        assertThat(node.toString())
            .isEqualTo("ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(0.0))")
    }

    @Test
    fun responseNodeEquals_checksEqualityOfValues() {
        val node1 = ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(1f))
        val node2 = ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(1f))
        val node3 = ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(2f))
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun responseNodeHashCode_withIdenticalValues_match() {
        val node1 = ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(1f))
        val node2 = ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(1f))
        val node3 = ResponseNode(EasingFunction.Predefined.EASE, ConstantNode(2f))
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun responseNodeEquals_withDifferentValues_returnsFalse() {
        val original =
            ResponseNode(
                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                input = ConstantNode(0f),
            )
        assertThat(
                original.equals(
                    ResponseNode(
                        responseCurve = EasingFunction.Predefined.LINEAR, // different
                        input = ConstantNode(0f),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    ResponseNode(
                        responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                        input = ConstantNode(1f), // different
                    )
                )
            )
            .isFalse()
    }
}
