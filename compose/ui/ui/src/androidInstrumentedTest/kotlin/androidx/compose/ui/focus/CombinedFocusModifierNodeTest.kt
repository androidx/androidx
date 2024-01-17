/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class CombinedFocusModifierNodeTest(private val delegatedFocusTarget: Boolean) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun requestFocus() {
        // Arrange.
        val combinedFocusNode = CombinedFocusNode()
        rule.setFocusableContent {
            Box(Modifier.combinedFocusElement(combinedFocusNode))
        }

        // Act.
        rule.runOnIdle {
            combinedFocusNode.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(combinedFocusNode.focusState.isFocused).isTrue()
        }
    }

    @Test
    fun captureFocus() {
        // Arrange.
        val combinedFocusNode = CombinedFocusNode()
        rule.setFocusableContent {
            Box(Modifier.combinedFocusElement(combinedFocusNode))
        }
        rule.runOnIdle {
            combinedFocusNode.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            combinedFocusNode.captureFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(combinedFocusNode.focusState.isFocused).isTrue()
            assertThat(combinedFocusNode.focusState.isCaptured).isTrue()
        }
    }

    @Test
    fun freeFocus() {
        // Arrange.
        val combinedFocusNode = CombinedFocusNode()
        rule.setFocusableContent {
            Box(Modifier.combinedFocusElement(combinedFocusNode))
        }
        rule.runOnIdle {
            combinedFocusNode.requestFocus()
            combinedFocusNode.captureFocus()
        }

        // Act.
        rule.runOnIdle {
            combinedFocusNode.freeFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(combinedFocusNode.focusState.isFocused).isTrue()
            assertThat(combinedFocusNode.focusState.isCaptured).isFalse()
        }
    }

    @Test
    fun requestFocusWhenCanFocusIsTrue() {
        // Arrange.
        val combinedFocusNode = CombinedFocusNode().apply { canFocus = true }
        rule.setFocusableContent {
            Box(Modifier.combinedFocusElement(combinedFocusNode))
        }

        // Act.
        rule.runOnIdle {
            combinedFocusNode.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(combinedFocusNode.focusState.isFocused).isTrue()
        }
    }

    @Test
    fun requestFocusWhenCanFocusIsFalse() {
        // Arrange.
        val combinedFocusNode = CombinedFocusNode().apply { canFocus = false }
        rule.setFocusableContent {
            Box(Modifier.combinedFocusElement(combinedFocusNode))
        }

        // Act.
        rule.runOnIdle {
            combinedFocusNode.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(combinedFocusNode.focusState.isFocused).isFalse()
        }
    }

    /**
     * This test checks that [FocusPropertiesModifierNode.applyFocusProperties] is called when a
     * property changes.
     */
    @Test
    fun losesFocusWhenCanFocusChangesToFalse() {
        // Arrange.
        val combinedFocusNode = CombinedFocusNode()
        rule.setFocusableContent {
            Box(Modifier.combinedFocusElement(combinedFocusNode))
        }
        rule.runOnIdle {
            combinedFocusNode.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            combinedFocusNode.canFocus = false
        }

        // Assert.
        rule.runOnIdle {
            assertThat(combinedFocusNode.focusState.isFocused).isFalse()
        }
    }

    @Test
    fun doesNotGainFocusWhenCanFocusChangesToTrue() {
        // Arrange.
        val combinedFocusNode = CombinedFocusNode()
        rule.setFocusableContent {
            Box(Modifier.combinedFocusElement(combinedFocusNode))
        }
        rule.runOnIdle {
            combinedFocusNode.requestFocus()
            combinedFocusNode.canFocus = false
        }

        // Act.
        rule.runOnIdle {
            combinedFocusNode.canFocus = true
        }

        // Assert.
        rule.runOnIdle {
            assertThat(combinedFocusNode.focusState.isFocused).isFalse()
        }
    }

    private fun Modifier.combinedFocusElement(
        combinedFocusNode: CombinedFocusModifierNode
    ): Modifier = if (delegatedFocusTarget) {
        this then DelegatedFocusTargetElement(combinedFocusNode as DelegatedFocusTargetNode)
    } else {
        this then AppendedFocusTargetElement(combinedFocusNode as AppendedFocusTargetNode)
            .focusTarget()
    }

    private data class AppendedFocusTargetElement(
        val appendedFocusTargetNode: AppendedFocusTargetNode
    ) : ModifierNodeElement<AppendedFocusTargetNode>() {
        override fun create(): AppendedFocusTargetNode = appendedFocusTargetNode
        override fun update(node: AppendedFocusTargetNode) {}
    }

    private data class DelegatedFocusTargetElement(
        val delegatedFocusTargetNode: DelegatedFocusTargetNode
    ) : ModifierNodeElement<DelegatedFocusTargetNode>() {
        override fun create(): DelegatedFocusTargetNode = delegatedFocusTargetNode
        override fun update(node: DelegatedFocusTargetNode) {}
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "delegatedFocusTarget = {0}")
        fun initParameters() = listOf(false, true)
    }

    private fun CombinedFocusNode(): CombinedFocusModifierNode {
        return if (delegatedFocusTarget) DelegatedFocusTargetNode() else AppendedFocusTargetNode()
    }

    private interface CombinedFocusModifierNode :
        FocusRequesterModifierNode, FocusPropertiesModifierNode {
        val focusState: FocusState
        var canFocus: Boolean
    }

    private class AppendedFocusTargetNode :
        CombinedFocusModifierNode,
        FocusEventModifierNode,
        DelegatingNode() {

        override lateinit var focusState: FocusState

        override var canFocus by mutableStateOf(true)

        override fun onFocusEvent(focusState: FocusState) {
            this.focusState = focusState
        }

        override fun applyFocusProperties(focusProperties: FocusProperties) {
            focusProperties.canFocus = canFocus
        }
    }

    private class DelegatedFocusTargetNode :
        CombinedFocusModifierNode,
        DelegatingNode() {

        val focusTargetModifierNode = delegate(FocusTargetModifierNode())

        override val focusState: FocusState
            @OptIn(ExperimentalComposeUiApi::class)
            get() = focusTargetModifierNode.focusState

        override var canFocus by mutableStateOf(true)

        override fun applyFocusProperties(focusProperties: FocusProperties) {
            focusProperties.canFocus = canFocus
        }
    }
}
