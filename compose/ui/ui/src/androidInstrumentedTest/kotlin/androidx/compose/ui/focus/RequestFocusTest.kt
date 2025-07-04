/*
 * Copyright 2020 The Android Open Source Project
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

import android.view.View
import android.widget.LinearLayout
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RequestFocusTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun active_isUnchanged() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun captured_isUnchanged() {

        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Captured) }
    }

    @Test
    fun deactivated_isUnchanged() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Inactive) }
    }

    @Test
    fun activeParent_propagateFocus() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var childFocusState: FocusState
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier.focusRequester(initialFocus)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
            assertThat(childFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun deactivatedParent_propagateFocus() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier.focusRequester(initialFocus)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            // Unchanged.
            assertThat(focusState).isEqualTo(ActiveParent)
            assertThat(childFocusState).isEqualTo(Active)
        }
    }

    @Test
    fun deactivatedParent_activeChild_propagateFocus() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()

        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        lateinit var grandChildFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier.focusRequester(initialFocus)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                ) {
                    Box(Modifier.onFocusChanged { grandChildFocusState = it }.focusTarget())
                }
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(ActiveParent)
            assertThat(childFocusState).isEqualTo(Active)
            assertThat(grandChildFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun inactiveRoot_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun inactiveRootWithChildren_propagateFocusSendsRequestToOwner_systemCanGrantFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(Modifier.onFocusChanged { childFocusState = it }.focusTarget())
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(Active)
            assertThat(childFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun inactiveNonRootWithChildren() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        lateinit var parentFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(initialFocus)
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                ) {
                    Box(Modifier.onFocusChanged { childFocusState = it }.focusTarget())
                }
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
            assertThat(childFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun rootNode() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun rootNodeWithChildren() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(Modifier.focusTarget())
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                ) {
                    Box(Modifier.focusTarget())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun parentNodeWithNoFocusedAncestor_childRequestsFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(Modifier.onFocusChanged { focusState = it }.focusTarget()) {
                    Box(Modifier.focusRequester(focusRequester).focusTarget())
                }
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(ActiveParent) }
    }

    @Test
    fun childNodeWithNoFocusedAncestor() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(Modifier.focusTarget()) {
                    Box(
                        Modifier.focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
            }
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun requestFocus_parentIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(initialFocus)
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // After executing requestFocus, siblingNode will be 'Active'.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocus_childIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier.focusRequester(initialFocus)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(Active)
            assertThat(focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_childHasCapturedFocus() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier.focusRequester(initialFocus)
                        .onFocusChanged { childFocusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            initialFocus.requestFocus()
            initialFocus.captureFocus()
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState).isEqualTo(ActiveParent)
            assertThat(childFocusState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_siblingIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        lateinit var siblingFocusState: FocusState

        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { parentFocusState = it }.focusTarget()) {
                Box(
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
                Box(
                    Modifier.focusRequester(initialFocus)
                        .onFocusChanged { siblingFocusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
            assertThat(siblingFocusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun requestFocus_siblingHasCapturedFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        lateinit var siblingFocusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { parentFocusState = it }.focusTarget()) {
                Box(
                    Modifier.focusRequester(focusRequester)
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                )
                Box(
                    Modifier.focusRequester(initialFocus)
                        .onFocusChanged { siblingFocusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            initialFocus.requestFocus()
            initialFocus.captureFocus()
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Inactive)
            assertThat(siblingFocusState).isEqualTo(Captured)
        }
    }

    @Test
    fun requestFocus_cousinIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(Modifier.focusTarget()) {
                Box(Modifier.focusTarget()) {
                    Box(
                        Modifier.focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
                Box(Modifier.focusTarget()) {
                    Box(Modifier.focusRequester(initialFocus).focusTarget())
                }
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(Active) }
    }

    @Test
    fun requestFocus_grandParentIsFocused() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        lateinit var grandParentFocusState: FocusState
        lateinit var parentFocusState: FocusState
        lateinit var focusState: FocusState
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(initialFocus)
                    .onFocusChanged { grandParentFocusState = it }
                    .focusTarget()
            ) {
                Box(Modifier.onFocusChanged { parentFocusState = it }.focusTarget()) {
                    Box(
                        Modifier.focusRequester(focusRequester)
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { initialFocus.requestFocus() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(grandParentFocusState).isEqualTo(ActiveParent)
            assertThat(parentFocusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(Active)
        }
    }

    @Test
    fun requestFocusWithDirection() {
        val requester1 = FocusRequester()
        val requester2 = FocusRequester()
        var focusDirection: FocusDirection? = null
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.size(10.dp)
                        .align(Alignment.TopCenter)
                        .focusProperties { onEnter = { focusDirection = requestedFocusDirection } }
                        .focusGroup()
                ) {
                    Box(Modifier.focusRequester(requester1).focusTarget().size(10.dp))
                }
                Box(
                    Modifier.size(10.dp)
                        .align(Alignment.BottomCenter)
                        .focusProperties { onEnter = { focusDirection = requestedFocusDirection } }
                        .focusGroup()
                ) {
                    Box(Modifier.focusRequester(requester2).focusTarget().size(10.dp))
                }
            }
        }

        rule.runOnIdle { requester1.requestFocus(FocusDirection.Up) }
        rule.runOnIdle {
            assertThat(focusDirection).isEqualTo(FocusDirection.Up)
            requester2.requestFocus(FocusDirection.Left)
        }
        rule.runOnIdle {
            assertThat(focusDirection).isEqualTo(FocusDirection.Left)
            requester1.requestFocus(FocusDirection.Right)
        }
        rule.runOnIdle {
            assertThat(focusDirection).isEqualTo(FocusDirection.Right)
            requester2.requestFocus(FocusDirection.Down)
        }
        rule.runOnIdle {
            assertThat(focusDirection).isEqualTo(FocusDirection.Down)
            requester1.requestFocus(FocusDirection.Enter)
        }
        rule.runOnIdle {
            assertThat(focusDirection).isEqualTo(FocusDirection.Enter)
            requester2.requestFocus(FocusDirection.Exit)
        }
        rule.runOnIdle { assertThat(focusDirection).isEqualTo(FocusDirection.Exit) }
    }

    @Test
    fun requestFocus_eventSequence() {
        // Arrange.
        val initialFocus = FocusRequester()
        val focusRequester = FocusRequester()
        val eventSequence = mutableListOf<String>()
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { eventSequence.add("1 $it") }.focusTarget()) {
                Box(Modifier.onFocusChanged { eventSequence.add("2 $it") }.focusTarget()) {
                    Box(
                        Modifier.focusRequester(initialFocus)
                            .onFocusChanged { eventSequence.add("3 $it") }
                            .focusTarget()
                    )
                }
                Box(Modifier.onFocusChanged { eventSequence.add("4 $it") }.focusTarget()) {
                    Box(
                        Modifier.focusRequester(focusRequester)
                            .onFocusChanged { eventSequence.add("5 $it") }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle { eventSequence.clear() }

        // Act.
        rule.runOnIdle { initialFocus.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(eventSequence)
                .containsExactly("1 ActiveParent", "2 ActiveParent", "3 Active")
                .inOrder()
        }

        // Act.
        rule.runOnIdle {
            eventSequence.clear()
            focusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(eventSequence)
                .containsExactly("3 Inactive", "2 Inactive", "4 ActiveParent", "5 Active")
                .inOrder()
        }
    }

    @Test
    fun requestFocusOnParentAndThenChild_eventSequence() {
        // Arrange.
        val parentFocusRequester = FocusRequester()
        val childFocusRequester = FocusRequester()
        val eventSequence = mutableListOf<String>()
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(parentFocusRequester)
                    .onFocusChanged { eventSequence.add("Parent $it") }
                    .focusTarget()
            ) {
                Box(
                    Modifier.focusRequester(childFocusRequester)
                        .onFocusChanged { eventSequence.add("Child $it") }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle { eventSequence.clear() }

        // Act.
        rule.runOnIdle { parentFocusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(eventSequence).containsExactly("Parent Active").inOrder() }

        // Act.
        rule.runOnIdle {
            eventSequence.clear()
            childFocusRequester.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(eventSequence)
                .containsExactly("Parent ActiveParent", "Child Active")
                .inOrder()
        }
    }

    @Test
    fun requestFocus_wrongDirection() {
        val tag1 = "tag 1"
        val tag2 = "tag 2"
        val tag3 = "tag 3"
        lateinit var button2: View
        lateinit var button3: View
        lateinit var inputModeManager: InputModeManager

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    LinearLayout(it).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(
                            ComposeView(it).apply {
                                setContent {
                                    Button(onClick = {}, Modifier.testTag(tag1)) {
                                        Text("Button 1")
                                    }
                                }
                            }
                        )
                        addView(
                            ComposeView(it).apply {
                                button2 = this
                                setContent {
                                    Button(onClick = {}, Modifier.testTag(tag2)) {
                                        Text("Button 2")
                                    }
                                }
                            }
                        )
                        addView(
                            ComposeView(it).apply {
                                button3 = this
                                setContent {
                                    Button(onClick = {}, Modifier.testTag(tag3)) {
                                        Text("Button 3")
                                    }
                                }
                            }
                        )
                    }
                },
            )
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        rule.runOnIdle { button3.requestFocus() }
        rule.onNodeWithTag(tag3).assertIsFocused()

        val success =
            rule.runOnIdle { button2.requestFocus(View.FOCUS_UP, android.graphics.Rect()) }

        // TODO(b/406327273): Support this use case without isViewFocusFixEnabled. This was
        //  added in aosp/3447394 but depends on aosp/3417182 which is behind a flag.
        if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isViewFocusFixEnabled) {
            assertThat(success).isTrue()
            rule.onNodeWithTag(tag1).assertIsFocused()
        } else {
            assertThat(success).isFalse()
        }
    }
}
