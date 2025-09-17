/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusTargetModifierNodeTest {
    @get:Rule val rule = createComposeRule()

    @After
    fun resetTouchMode() = InstrumentationRegistry.getInstrumentation().resetInTouchModeCompat()

    @Test
    fun requestFocus() {
        val focusTargetModifierNode = FocusTargetModifierNode()

        rule.setFocusableContent { FocusTargetModifierNodeBox(focusTargetModifierNode) }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Active) }
    }

    @Test
    fun requestFocus_notFocusable_noContent() {
        val focusTargetModifierNode = FocusTargetModifierNode(Focusability.Never)

        rule.setFocusableContent { FocusTargetModifierNodeBox(focusTargetModifierNode) }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive) }
    }

    @Test
    fun requestFocus_notFocusable_noFocusableContent() {
        val focusTargetModifierNode = FocusTargetModifierNode(Focusability.Never)

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(focusTargetModifierNode) { Box(Modifier.size(10.dp)) }
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive) }
    }

    @Test
    fun requestFocus_notFocusable_focusableContent() {
        val focusTargetModifierNode = FocusTargetModifierNode(Focusability.Never)

        rule.setFocusableContent {
            FocusTargetModifierNodeBox(focusTargetModifierNode) {
                Box(Modifier.size(10.dp).testTag("focusableChild").focusable())
            }
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        @OptIn(ExperimentalComposeUiApi::class)
        if (ComposeUiFlags.isRequestFocusOnNonFocusableFocusTargetEnabled) {
            rule.runOnIdle {
                assertThat(focusTargetModifierNode.focusState).isEqualTo(ActiveParent)
            }
            rule.onNodeWithTag("focusableChild").assertIsFocused()
        } else {
            rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive) }
            rule.onNodeWithTag("focusableChild").assertIsNotFocused()
        }
    }

    @Test
    fun requestFocus_focusabilitySystemDefined_touchMode() {
        val focusTargetModifierNode = FocusTargetModifierNode(Focusability.SystemDefined)

        rule.setFocusableContent { FocusTargetModifierNodeBox(focusTargetModifierNode) }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive) }
    }

    @Test
    fun requestFocus_focusabilitySystemDefined_nonTouchMode() {
        val focusTargetModifierNode = FocusTargetModifierNode(Focusability.SystemDefined)

        lateinit var inputModeManager: InputModeManager
        rule.setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            FocusTargetModifierNodeBox(focusTargetModifierNode)
        }

        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class) inputModeManager.requestInputMode(Keyboard)
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Active) }
    }

    @Test
    fun focused_focusabilitySetToNotFocusableWhileFocused() {
        val focusTargetModifierNode = FocusTargetModifierNode()

        rule.setFocusableContent { FocusTargetModifierNodeBox(focusTargetModifierNode) }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Active)
            // Make the node not able to be focused, after it has already gained focus
            focusTargetModifierNode.focusability = Focusability.Never
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive) }
    }

    @Test
    fun focused_focusabilitySetToSystemDefinedWhileFocusedInTouchMode() {
        val focusTargetModifierNode = FocusTargetModifierNode()

        rule.setFocusableContent { FocusTargetModifierNodeBox(focusTargetModifierNode) }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Active)
            // Set the node to SystemDefined, after it has already gained focus - since we are in
            // touch mode, this is effectively making the node unable to be focused.
            focusTargetModifierNode.focusability = Focusability.SystemDefined
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive) }
    }

    @Test
    fun focused_focusabilitySetToSystemDefinedWhileFocusedInNonTouchMode() {
        val focusTargetModifierNode = FocusTargetModifierNode()

        lateinit var inputModeManager: InputModeManager
        rule.setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            FocusTargetModifierNodeBox(focusTargetModifierNode)
        }

        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class) inputModeManager.requestInputMode(Keyboard)
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Active)
            // Set the node to SystemDefined, after it has already gained focus - since we are in
            // non touch mode, this shouldn't change anything.
            focusTargetModifierNode.focusability = Focusability.SystemDefined
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Active) }
    }

    @Test
    fun onFocusChange() {
        val previousFocusStates = mutableListOf<FocusState?>()
        val currentFocusStates = mutableListOf<FocusState?>()
        val focusTargetModifierNode = FocusTargetModifierNode { previous, current ->
            previousFocusStates += previous
            currentFocusStates += current
        }

        rule.setFocusableContent { FocusTargetModifierNodeBox(focusTargetModifierNode) }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            assertThat(previousFocusStates).isEmpty()
            assertThat(currentFocusStates).isEmpty()
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Active)
            assertThat(previousFocusStates).containsExactly(Inactive)
            assertThat(currentFocusStates).containsExactly(Active)
            previousFocusStates.clear()
            currentFocusStates.clear()
            focusTargetModifierNode.focusability = Focusability.Never
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            assertThat(previousFocusStates).containsExactly(Active)
            assertThat(currentFocusStates).containsExactly(Inactive)
        }
    }

    @Test
    fun onFocusChange_requestingChildFocus() {
        val previousFocusStates = mutableListOf<FocusState?>()
        val currentFocusStates = mutableListOf<FocusState?>()
        val focusTargetModifierNode = FocusTargetModifierNode { previous, current ->
            previousFocusStates += previous
            currentFocusStates += current
        }

        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            FocusTargetModifierNodeBox(focusTargetModifierNode) {
                Box(Modifier.focusRequester(focusRequester).focusTarget())
            }
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            assertThat(previousFocusStates).isEmpty()
            assertThat(currentFocusStates).isEmpty()
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(ActiveParent)
            assertThat(previousFocusStates).containsExactly(Inactive).inOrder()
            assertThat(currentFocusStates).containsExactly(ActiveParent).inOrder()
        }
    }

    @Test
    fun onFocusChange_requestingChildFocusAfterParent() {
        // Arrange
        val previousParentFocusStates = mutableListOf<FocusState?>()
        val currentParentFocusStates = mutableListOf<FocusState?>()
        val parentFocusTargetModifierNode = FocusTargetModifierNode { previous, current ->
            previousParentFocusStates += previous
            currentParentFocusStates += current
        }
        val previousChildFocusStates = mutableListOf<FocusState?>()
        val currentChildFocusStates = mutableListOf<FocusState?>()
        val childFocusTargetModifierNode = FocusTargetModifierNode { previous, current ->
            previousChildFocusStates += previous
            currentChildFocusStates += current
        }

        val parentFocusRequester = FocusRequester()
        val childFocusRequester = FocusRequester()
        rule.setFocusableContent {
            FocusTargetModifierNodeBox(
                parentFocusTargetModifierNode,
                Modifier.focusRequester(parentFocusRequester),
            ) {
                FocusTargetModifierNodeBox(
                    childFocusTargetModifierNode,
                    Modifier.focusRequester(childFocusRequester),
                ) {}
            }
        }

        rule.runOnIdle {
            assertThat(parentFocusTargetModifierNode.focusState).isEqualTo(Inactive)
            assertThat(previousParentFocusStates).isEmpty()
            assertThat(currentParentFocusStates).isEmpty()
            assertThat(childFocusTargetModifierNode.focusState).isEqualTo(Inactive)
            assertThat(previousChildFocusStates).isEmpty()
            assertThat(currentChildFocusStates).isEmpty()
            parentFocusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(parentFocusTargetModifierNode.focusState).isEqualTo(Active)
            assertThat(previousParentFocusStates).containsExactly(Inactive)
            assertThat(currentParentFocusStates).containsExactly(Active)
            assertThat(childFocusTargetModifierNode.focusState).isEqualTo(Inactive)
            assertThat(previousChildFocusStates).isEmpty()
            assertThat(currentChildFocusStates).isEmpty()
        }

        // Act
        rule.runOnIdle { childFocusRequester.requestFocus() }

        // Assert
        rule.runOnIdle {
            assertThat(parentFocusTargetModifierNode.focusState).isEqualTo(ActiveParent)
            assertThat(previousParentFocusStates).containsExactly(Inactive, Active).inOrder()
            assertThat(currentParentFocusStates).containsExactly(Active, ActiveParent).inOrder()
            assertThat(childFocusTargetModifierNode.focusState).isEqualTo(Active)
            assertThat(previousChildFocusStates).containsExactly(Inactive)
            assertThat(currentChildFocusStates).containsExactly(Active)
        }
    }

    @Test
    fun onFocusChange_updatingFocusabilityBeforeAttach() {
        val previousFocusStates = mutableListOf<FocusState?>()
        val currentFocusStates = mutableListOf<FocusState?>()
        val focusTargetModifierNode = FocusTargetModifierNode { previous, current ->
            previousFocusStates += previous
            currentFocusStates += current
        }

        // Set mode
        focusTargetModifierNode.focusability = Focusability.Never

        // We shouldn't have initialized focus state yet / sent any callbacks, since we haven't been
        // attached
        assertThat(previousFocusStates).isEmpty()
        assertThat(currentFocusStates).isEmpty()
    }

    @Test
    fun focusPropertiesNodeInHierarchy() {
        var addFocusPropertiesModifier by mutableStateOf(true)
        val focusTargetModifierNode = FocusTargetModifierNode()
        // Make the node not focusable
        focusTargetModifierNode.focusability = Focusability.Never

        rule.setFocusableContent {
            // Force it to be focusable with focusProperties
            val focusPropertiesModifier =
                if (addFocusPropertiesModifier) {
                    Modifier.focusProperties { canFocus = true }
                } else {
                    Modifier
                }
            FocusTargetModifierNodeBox(focusTargetModifierNode, focusPropertiesModifier)
        }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle {
            // The focus properties modifier should take precedence
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Active)
            addFocusPropertiesModifier = false
        }

        rule.runOnIdle {
            // Now that we removed the focus properties node, we should no longer have focus
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun calculateFocusArea_findsTheFocusedChildOrSelf() {
        val nodes = (0 until 4).map { FocusTargetModifierNode() }
        rule.setFocusableContent {
            FocusTargetModifierNodeBox(nodes[0]) {
                Row {
                    FocusTargetModifierNodeBox(nodes[1], Modifier.size(10.toDp()))
                    FocusTargetModifierNodeBox(nodes[2], Modifier.size(10.toDp()))
                    FocusTargetModifierNodeBox(nodes[3], Modifier.size(10.toDp()))
                }
            }
        }

        rule.runOnIdle { nodes[2].requestFocus() }

        rule.runOnIdle {
            assertThat(nodes[0].getFocusedRect()).isEqualTo(Rect(10f, 0f, 20f, 10f))
            assertThat(nodes[1].getFocusedRect()).isNull()
            assertThat(nodes[2].getFocusedRect()).isEqualTo(Rect(0f, 0f, 10f, 10f))
            assertThat(nodes[3].getFocusedRect()).isNull()
        }
    }

    @Test
    fun calculateFocusArea_canFindSelf() {
        val parentNode = FocusTargetModifierNode()
        val childNode = FocusTargetModifierNode()
        rule.setFocusableContent {
            FocusTargetModifierNodeBox(parentNode, Modifier.size(30.toDp())) {
                FocusTargetModifierNodeBox(childNode, Modifier.size(10.toDp()))
            }
        }

        rule.runOnIdle { parentNode.requestFocus() }

        rule.runOnIdle {
            assertThat(parentNode.getFocusedRect()).isEqualTo(Rect(0f, 0f, 30f, 30f))
            assertThat(childNode.getFocusedRect()).isNull()
        }
    }

    @Test
    fun calculateFocusArea_isNotClipped() {
        val parentNode = FocusTargetModifierNode()
        val childNode = FocusTargetModifierNode()
        rule.setFocusableContent {
            FocusTargetModifierNodeBox(parentNode, modifier = Modifier.size(100.toDp())) {
                FocusTargetModifierNodeBox(
                    childNode,
                    modifier = Modifier.size(50.toDp()).offset(200.toDp(), 200.toDp()),
                )
            }
        }

        rule.runOnIdle { childNode.requestFocus() }

        rule.runOnIdle {
            assertThat(parentNode.getFocusedRect()).isEqualTo(Rect(200f, 200f, 250f, 250f))
        }
    }

    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    @Composable
    private fun FocusTargetModifierNodeBox(
        focusTargetModifierNode: FocusTargetModifierNode,
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit = {},
    ) {
        val node = remember {
            object : DelegatingNode() {
                init {
                    delegate(focusTargetModifierNode)
                }
            }
        }
        Box(modifier.elementFor(node), content = content)
    }
}
