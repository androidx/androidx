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

import androidx.ink.brush.InputToolType
import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ToolTypeFilterNodeTest {

    @Test
    fun toolTypeFilterNodeConstructor_throwsForEmptyEnabledToolTypes() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> { ToolTypeFilterNode(emptySet(), input) }
    }

    @Test
    fun toolTypeFilterNodeInputs_containsInput() {
        val input = ConstantNode(0f)
        val node = ToolTypeFilterNode(setOf(InputToolType.STYLUS), input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun toolTypeFilterNodeToString() {
        val input = ConstantNode(0f)
        val node = ToolTypeFilterNode(setOf(InputToolType.STYLUS), input)
        assertThat(node.toString())
            .isEqualTo("ToolTypeFilterNode([InputToolType.STYLUS], ConstantNode(0.0))")
    }

    @Test
    fun toolTypeFilterNodeEquals_checksEqualityOfValues() {
        val node1 = ToolTypeFilterNode(setOf(InputToolType.STYLUS), ConstantNode(1f))
        val node2 = ToolTypeFilterNode(setOf(InputToolType.STYLUS), ConstantNode(1f))
        val node3 = ToolTypeFilterNode(setOf(InputToolType.STYLUS), ConstantNode(2f))
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun toolTypeFilterNodeHashCode_withIdenticalValues_match() {
        val node1 = ToolTypeFilterNode(setOf(InputToolType.STYLUS), ConstantNode(1f))
        val node2 = ToolTypeFilterNode(setOf(InputToolType.STYLUS), ConstantNode(1f))
        val node3 = ToolTypeFilterNode(setOf(InputToolType.STYLUS), ConstantNode(2f))
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun toolTypeFilterNodeConstructor_withInvalidArguments_throws() {
        // enabledToolType contains empty set.
        val enabledToolTypeError =
            assertFailsWith<IllegalArgumentException> {
                ToolTypeFilterNode(enabledToolTypes = setOf(), input = ConstantNode(0f))
            }
        assertThat(enabledToolTypeError.message).contains("enabled_tool_types")
        assertThat(enabledToolTypeError.message).contains("must enable at least one")
    }

    @Test
    fun toolTypeFilterNodeEquals_withDifferentValues_returnsFalse() {
        val original =
            ToolTypeFilterNode(
                enabledToolTypes = setOf(InputToolType.STYLUS),
                input = ConstantNode(0f),
            )
        assertThat(
                original.equals(
                    ToolTypeFilterNode(
                        enabledToolTypes = setOf(InputToolType.TOUCH), // different
                        input = ConstantNode(0f),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    ToolTypeFilterNode(
                        enabledToolTypes = setOf(InputToolType.STYLUS),
                        input = ConstantNode(1f), // different
                    )
                )
            )
            .isFalse()
    }
}
