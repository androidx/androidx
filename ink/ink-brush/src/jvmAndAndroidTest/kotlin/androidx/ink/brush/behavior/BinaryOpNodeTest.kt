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
import androidx.ink.brush.behavior.BinaryOpNode.BinaryOp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BinaryOpNodeTest {

    @Test
    fun binaryOpToString_returnsCorrectString() {
        assertThat(BinaryOp.PRODUCT.toString()).isEqualTo("BinaryOp.PRODUCT")
        assertThat(BinaryOp.SUM.toString()).isEqualTo("BinaryOp.SUM")
        assertThat(BinaryOp.MIN.toString()).isEqualTo("BinaryOp.MIN")
        assertThat(BinaryOp.MAX.toString()).isEqualTo("BinaryOp.MAX")
        assertThat(BinaryOp.AND_THEN.toString()).isEqualTo("BinaryOp.AND_THEN")
        assertThat(BinaryOp.OR_ELSE.toString()).isEqualTo("BinaryOp.OR_ELSE")
        assertThat(BinaryOp.XOR_ELSE.toString()).isEqualTo("BinaryOp.XOR_ELSE")
    }

    @Test
    fun binaryOpNodeInputs_containsInputsInOrder() {
        val firstInput = ConstantNode(0f)
        val secondInput = ConstantNode(1f)
        val node = BinaryOpNode(BinaryOp.SUM, firstInput, secondInput)
        assertThat(node.inputs).containsExactly(firstInput, secondInput).inOrder()
    }

    @Test
    fun binaryOpNodeToString() {
        val firstInput = ConstantNode(0f)
        val secondInput = ConstantNode(1f)
        val node = BinaryOpNode(BinaryOp.SUM, firstInput, secondInput)
        assertThat(node.toString())
            .isEqualTo("BinaryOpNode(SUM, ConstantNode(0.0), ConstantNode(1.0))")
    }

    @Test
    fun binaryOpNodeEquals_checksEqualityOfValues() {
        val node1 = BinaryOpNode(BinaryOp.SUM, ConstantNode(0f), ConstantNode(1f))
        val node2 = BinaryOpNode(BinaryOp.SUM, ConstantNode(0f), ConstantNode(1f))
        val node3 = BinaryOpNode(BinaryOp.SUM, ConstantNode(0f), ConstantNode(2f))
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun binaryOpNodeHashCode_withIdenticalValues_match() {
        val node1 = BinaryOpNode(BinaryOp.SUM, ConstantNode(0f), ConstantNode(1f))
        val node2 = BinaryOpNode(BinaryOp.SUM, ConstantNode(0f), ConstantNode(1f))
        val node3 = BinaryOpNode(BinaryOp.SUM, ConstantNode(0f), ConstantNode(2f))
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }
}
