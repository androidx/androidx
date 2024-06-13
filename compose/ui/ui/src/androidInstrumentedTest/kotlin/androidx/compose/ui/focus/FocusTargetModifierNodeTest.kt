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

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.junit4.createComposeRule
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

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() =
        with(InstrumentationRegistry.getInstrumentation()) {
            if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
        }

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
    fun requestFocus_notFocusable() {
        val focusTargetModifierNode = FocusTargetModifierNode(Focusability.Never)

        rule.setFocusableContent { FocusTargetModifierNodeBox(focusTargetModifierNode) }

        rule.runOnIdle {
            assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive)
            focusTargetModifierNode.requestFocus()
        }

        rule.runOnIdle { assertThat(focusTargetModifierNode.focusState).isEqualTo(Inactive) }
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
}

@Composable
private fun FocusTargetModifierNodeBox(
    focusTargetModifierNode: FocusTargetModifierNode,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
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
