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

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.SoftKeyboardInterceptionModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.elementFor
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performRotaryScrollInput
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
class FocusTargetAttachDetachTest {
    @get:Rule val rule = createComposeRule()

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() =
        with(InstrumentationRegistry.getInstrumentation()) {
            if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
        }

    @Test
    fun reorderedFocusRequesterModifiers_onFocusChangedInSameModifierChain() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var observingFocusTarget1 by mutableStateOf(true)
        rule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val onFocusChanged = Modifier.onFocusChanged { focusState = it }
            val focusTarget1 = Modifier.focusTarget()
            val focusTarget2 = Modifier.focusTarget()
            Box {
                Box(
                    modifier =
                        if (observingFocusTarget1) {
                            onFocusChanged
                                .then(focusRequesterModifier)
                                .then(focusTarget1)
                                .then(focusTarget2)
                        } else {
                            focusTarget1
                                .then(onFocusChanged)
                                .then(focusRequesterModifier)
                                .then(focusTarget2)
                        }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { observingFocusTarget1 = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedModifier_onFocusChangedDoesNotHaveAFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var onFocusChangedHasFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            val focusRequesterModifier = Modifier.focusRequester(focusRequester)
            val onFocusChanged = Modifier.onFocusChanged { focusState = it }
            val focusTarget = Modifier.focusTarget()
            Box {
                Box(
                    modifier =
                        if (onFocusChangedHasFocusTarget) {
                            onFocusChanged.then(focusRequesterModifier).then(focusTarget)
                        } else {
                            focusTarget.then(onFocusChanged).then(focusRequesterModifier)
                        }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { onFocusChangedHasFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedFocusTarget_withNoNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .thenIf(optionalFocusTarget) { Modifier.focusTarget() }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .thenIf(optionalFocusTarget) { Modifier.focusTarget() }
            ) {
                Box(Modifier.focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveFocusTargetAndFocusChanged_triggersOnFocusEvent() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalModifiers by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.focusRequester(focusRequester).thenIf(optionalModifiers) {
                    Modifier.onFocusEvent { focusState = it }.focusTarget()
                }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalModifiers = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveComposable_doesNotTriggerOnFocusEvent() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalBox by mutableStateOf(true)
        rule.setFocusableContent {
            if (optionalBox) {
                Box(
                    Modifier.focusRequester(focusRequester)
                        .onFocusEvent { focusState = it }
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalBox = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedCapturedFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .thenIf(optionalFocusTarget) { Modifier.focusTarget() }
            ) {
                Box(Modifier.focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            assertThat(focusState.isCaptured).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveParentFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) { Modifier.focusTarget() }
            ) {
                Box(Modifier.focusRequester(focusRequester).focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(focusState.isFocused).isFalse()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.hasFocus).isTrue()
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun removedActiveParentFocusTarget_withNoNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) {
                        Modifier.focusTarget().focusRequester(focusRequester).focusTarget()
                    }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removedActiveParentFocusTargetAndFocusedChild_clearsFocusFromAllParents() {
        // Arrange.
        lateinit var focusState: FocusState
        lateinit var parentFocusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTargets by mutableStateOf(true)
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { parentFocusState = it }.focusTarget()) {
                Box(
                    Modifier.onFocusChanged { focusState = it }
                        .thenIf(optionalFocusTargets) {
                            Modifier.focusTarget().focusRequester(focusRequester).focusTarget()
                        }
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(parentFocusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTargets = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(parentFocusState.isFocused).isFalse()
        }
    }

    @Test
    fun removedActiveComposable_clearsFocusFromAllParents() {
        // Arrange.
        lateinit var focusState: FocusState
        lateinit var parentFocusState: FocusState
        val focusRequester = FocusRequester()
        var optionalBox by mutableStateOf(true)
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { parentFocusState = it }.focusTarget()) {
                if (optionalBox) {
                    Box(
                        Modifier.onFocusChanged { focusState = it }
                            .focusRequester(focusRequester)
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(parentFocusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalBox = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(parentFocusState.isFocused).isFalse()
        }
    }

    @Test
    fun removedDeactivatedParentFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) {
                        Modifier.focusProperties { canFocus = false }.focusTarget()
                    }
            ) {
                Box(Modifier.focusRequester(focusRequester).focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun removedDeactivatedParentFocusTarget_pointsToNextDeactivatedParentFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) {
                        Modifier.focusProperties { canFocus = false }.focusTarget()
                    }
            ) {
                Box(
                    Modifier.onFocusChanged { focusState = it }
                        .focusProperties { canFocus = false }
                        .focusTarget()
                ) {
                    Box(Modifier.focusRequester(focusRequester).focusTarget())
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.hasFocus).isTrue()
        }
    }

    @Test
    fun removedDeactivatedParent_parentsFocusTarget_isUnchanged() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    Modifier.thenIf(optionalFocusTarget) {
                        Modifier.focusProperties { canFocus = false }.focusTarget()
                    }
                ) {
                    Box(Modifier.focusRequester(focusRequester).focusTarget())
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.hasFocus).isTrue()
        }
    }

    @Test
    fun removedDeactivatedParentAndActiveChild_grandparent_retainsDeactivatedState() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    Modifier.thenIf(optionalFocusTarget) {
                        Modifier.focusProperties { canFocus = false }.focusTarget()
                    }
                ) {
                    Box(
                        Modifier.focusRequester(focusRequester).thenIf(optionalFocusTarget) {
                            Modifier.focusTarget()
                        }
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.hasFocus).isFalse()
        }
    }

    @Test
    fun removedDeactivatedParentAndActiveChild_grandParent_retainsNonDeactivatedState() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(Modifier.onFocusChanged { focusState = it }.focusTarget()) {
                Box(
                    Modifier.thenIf(optionalFocusTarget) {
                        Modifier.focusProperties { canFocus = false }.focusTarget()
                    }
                ) {
                    Box(
                        Modifier.focusRequester(focusRequester).thenIf(optionalFocusTarget) {
                            Modifier.focusTarget()
                        }
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.hasFocus).isFalse()
        }
    }

    @Test
    fun removedInactiveFocusTarget_pointsToNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) { Modifier.focusTarget() }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { optionalFocusTarget = false }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isTrue() }
    }

    @Test
    fun addedFocusTarget_pointsToTheFocusTargetJustAdded() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .thenIf(addFocusTarget) { Modifier.focusTarget() }
            ) {
                Box(Modifier.focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun addedFocusTarget_withNoNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .thenIf(addFocusTarget) { Modifier.focusTarget() }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun addedFocusTarget_withinInactiveNode() {
        // Arrange.
        val focusTarget1 = FocusTargetNode()
        val focusTarget2 = FocusTargetNode()
        var addFocusTarget2 by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.then(elementFor(instance = focusTarget1)).thenIf(addFocusTarget2) {
                    elementFor(instance = focusTarget2)
                }
            )
        }

        // Act.
        rule.runOnIdle { addFocusTarget2 = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget1.isInitialized()).isTrue()
            assertThat(focusTarget1.focusState).isEqualTo(Inactive)
            assertThat(focusTarget2.isInitialized()).isTrue()
            assertThat(focusTarget2.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun addedMultipleFocusTargets_withinInactiveHierarchy() {
        // Arrange.
        val focusTarget1 = FocusTargetNode()
        val focusTarget2 = FocusTargetNode()
        val focusTarget3 = FocusTargetNode()
        var addFocusTarget2 by mutableStateOf(false)
        var addFocusTarget3 by mutableStateOf(false)
        rule.setFocusableContent {
            Column(Modifier.then(elementFor(instance = focusTarget1))) {
                if (addFocusTarget2) Box(Modifier.then(elementFor(instance = focusTarget2)))
                if (addFocusTarget3) Box(Modifier.then(elementFor(instance = focusTarget3)))
            }
        }

        // Act.
        rule.runOnIdle {
            addFocusTarget2 = true
            addFocusTarget3 = true
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget1.isInitialized()).isTrue()
            assertThat(focusTarget1.focusState).isEqualTo(Inactive)
            assertThat(focusTarget2.isInitialized()).isTrue()
            assertThat(focusTarget2.focusState).isEqualTo(Inactive)
            assertThat(focusTarget3.isInitialized()).isTrue()
            assertThat(focusTarget3.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun addedFocusTarget_withinActiveNode() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.thenIf(addFocusTarget) {
                        Modifier.onFocusChanged { focusState = it }
                            .then(elementFor(instance = focusTarget))
                    }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun addedFocusTarget_withinActiveHierarchy() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Column(
                Modifier.thenIf(addFocusTarget) {
                    Modifier.onFocusChanged { focusState = it }
                        .then(elementFor(instance = focusTarget))
                }
            ) {
                Box(Modifier.focusTarget())
                Box(Modifier.focusRequester(focusRequester).focusTarget())
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(ActiveParent)
            assertThat(focusState).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun addedMultipleFocusTargets_withinActiveNode() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget1 by mutableStateOf(false)
        var addFocusTarget2 by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.thenIf(addFocusTarget1) {
                        Modifier.onFocusChanged { focusState1 = it }.focusTarget()
                    }
                    .thenIf(addFocusTarget2) {
                        Modifier.onFocusChanged { focusState2 = it }.focusTarget()
                    }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle {
            addFocusTarget1 = true
            addFocusTarget2 = true
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState1).isEqualTo(ActiveParent)
            assertThat(focusState2).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun addedMultipleFocusTargets_withinActiveHierarchy() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget1 by mutableStateOf(false)
        var addFocusTarget2 by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.thenIf(addFocusTarget1) {
                    Modifier.onFocusChanged { focusState1 = it }.focusTarget()
                }
            ) {
                Box(
                    Modifier.thenIf(addFocusTarget1) {
                        Modifier.onFocusChanged { focusState2 = it }.focusTarget()
                    }
                ) {
                    Box(Modifier.focusRequester(focusRequester).focusTarget())
                }
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Act.
        rule.runOnIdle {
            addFocusTarget1 = true
            addFocusTarget2 = true
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState1).isEqualTo(ActiveParent)
            assertThat(focusState2).isEqualTo(ActiveParent)
        }
    }

    @Test
    fun addedFocusTarget_withinCapturedNode() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.thenIf(addFocusTarget) {
                        Modifier.onFocusChanged { focusState = it }.focusTarget()
                    }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(ActiveParent) }
    }

    @Test
    fun addedFocusTarget_withinCapturedHierarchy() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.thenIf(addFocusTarget) {
                    Modifier.onFocusChanged { focusState = it }.focusTarget()
                }
            ) {
                Box(Modifier.focusRequester(focusRequester).focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(ActiveParent) }
    }

    @Test
    fun removingDeactivatedItem_withNoNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        var removeDeactivatedItem by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .thenIf(!removeDeactivatedItem) {
                        Modifier.focusProperties { canFocus = false }.focusTarget()
                    }
            )
        }

        // Act.
        rule.runOnIdle { removeDeactivatedItem = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    fun removingDeactivatedItem_withInactiveNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        var removeDeactivatedItem by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .thenIf(!removeDeactivatedItem) {
                        Modifier.focusProperties { canFocus = false }.focusTarget()
                    }
            ) {
                Box(Modifier.focusTarget())
            }
        }

        // Act.
        rule.runOnIdle { removeDeactivatedItem = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @Test
    fun removingDeactivatedItem_withDeactivatedNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        var removeDeactivatedItem by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.onFocusChanged { focusState = it }
                    .thenIf(!removeDeactivatedItem) {
                        Modifier.focusProperties { canFocus = false }.focusTarget()
                    }
            ) {
                Box(Modifier.focusProperties { canFocus = false }.focusTarget())
            }
        }

        // Act.
        rule.runOnIdle { removeDeactivatedItem = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState.isFocused).isFalse() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusTarget_nodeThatIsKeyInputNodeKind_implementing_receivesKeyEventsWhenFocused() {
        class FocusTargetAndKeyInputNode : DelegatingNode(), KeyInputModifierNode {
            val keyEvents = mutableListOf<KeyEvent>()
            val focusTargetNode = FocusTargetNode()

            init {
                delegate(focusTargetNode)
            }

            override fun onKeyEvent(event: KeyEvent): Boolean {
                keyEvents.add(event)
                return true
            }

            override fun onPreKeyEvent(event: KeyEvent) = false
        }

        val focusTargetAndKeyInputNode = FocusTargetAndKeyInputNode()
        val focusTargetAndKeyInputModifier = elementFor(key1 = null, focusTargetAndKeyInputNode)

        val focusRequester = FocusRequester()
        val targetTestTag = "target"
        lateinit var inputModeManager: InputModeManager

        rule.setFocusableContent(extraItemForInitialFocus = false) {
            inputModeManager = LocalInputModeManager.current
            Box(
                modifier =
                    Modifier.testTag(targetTestTag)
                        .focusRequester(focusRequester)
                        .then(focusTargetAndKeyInputModifier)
            )
        }

        rule.runOnUiThread {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }

        assertThat(focusTargetAndKeyInputNode.focusTargetNode.focusState.isFocused).isTrue()

        rule.onNodeWithTag(targetTestTag).performKeyInput { keyDown(Key.Enter) }

        assertThat(focusTargetAndKeyInputNode.keyEvents).hasSize(1)
        assertThat(focusTargetAndKeyInputNode.keyEvents[0].key).isEqualTo(Key.Enter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusTarget_nodeThatIsKeyInputNodeKind_delegating_receivesKeyEventsWhenFocused() {
        class FocusTargetAndKeyInputNode : DelegatingNode() {
            val keyEvents = mutableListOf<KeyEvent>()
            val focusTargetNode = FocusTargetNode()
            val keyInputNode =
                object : KeyInputModifierNode, Modifier.Node() {
                    override fun onKeyEvent(event: KeyEvent): Boolean {
                        keyEvents.add(event)
                        return true
                    }

                    override fun onPreKeyEvent(event: KeyEvent) = false
                }

            init {
                delegate(focusTargetNode)
                delegate(keyInputNode)
            }
        }

        val focusTargetAndKeyInputNode = FocusTargetAndKeyInputNode()
        val focusTargetAndKeyInputModifier = elementFor(key1 = null, focusTargetAndKeyInputNode)

        val focusRequester = FocusRequester()
        val targetTestTag = "target"
        lateinit var inputModeManager: InputModeManager

        rule.setFocusableContent(extraItemForInitialFocus = false) {
            inputModeManager = LocalInputModeManager.current
            Box(
                modifier =
                    Modifier.testTag(targetTestTag)
                        .focusRequester(focusRequester)
                        .then(focusTargetAndKeyInputModifier)
            )
        }

        rule.runOnUiThread {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }

        assertThat(focusTargetAndKeyInputNode.focusTargetNode.focusState.isFocused).isTrue()

        rule.onNodeWithTag(targetTestTag).performKeyInput { keyDown(Key.Enter) }

        assertThat(focusTargetAndKeyInputNode.keyEvents).hasSize(1)
        assertThat(focusTargetAndKeyInputNode.keyEvents[0].key).isEqualTo(Key.Enter)
    }

    @Test
    fun focusTarget_nodeThatIsSoftKeyInputNodeKind_implementing_receivesSoftKeyEventsWhenFocused() {
        class FocusTargetAndSoftKeyboardNode :
            DelegatingNode(), SoftKeyboardInterceptionModifierNode {
            val keyEvents = mutableListOf<KeyEvent>()
            val focusTargetNode = FocusTargetNode()

            init {
                delegate(focusTargetNode)
            }

            override fun onInterceptKeyBeforeSoftKeyboard(event: KeyEvent) = keyEvents.add(event)

            override fun onPreInterceptKeyBeforeSoftKeyboard(event: KeyEvent) = false
        }

        val focusTargetAndSoftKeyboardNode = FocusTargetAndSoftKeyboardNode()
        val focusTargetAndSoftKeyboardModifier =
            elementFor(key1 = null, focusTargetAndSoftKeyboardNode)

        val focusRequester = FocusRequester()
        val targetTestTag = "target"

        rule.setFocusableContent(extraItemForInitialFocus = false) {
            Box(
                modifier =
                    Modifier.testTag(targetTestTag)
                        .focusRequester(focusRequester)
                        .then(focusTargetAndSoftKeyboardModifier)
            )
        }

        rule.runOnUiThread { focusRequester.requestFocus() }
        assertThat(focusTargetAndSoftKeyboardNode.focusTargetNode.focusState.isFocused).isTrue()

        // This test specifically uses performKeyPress over performKeyInput as performKeyPress calls
        // sendKeyEvent, which in turn notifies FocusOwner that there's a
        // SoftKeyboardInterceptionModifierNode-interceptable key event first. performKeyInput goes
        // through dispatchKeyEvent which does not notify SoftKeyboardInterceptionModifierNodes.
        rule
            .onRoot()
            .performKeyPress(
                KeyEvent(
                    NativeKeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER
                    )
                )
            )

        assertThat(focusTargetAndSoftKeyboardNode.keyEvents).hasSize(1)
        assertThat(focusTargetAndSoftKeyboardNode.keyEvents[0].key).isEqualTo(Key.Enter)
    }

    @Test
    fun focusTarget_nodeThatIsSoftKeyInputNodeKind_delegating_receivesSoftKeyEventsWhenFocused() {
        class FocusTargetAndSoftKeyboardNode : DelegatingNode() {
            val keyEvents = mutableListOf<KeyEvent>()
            val focusTargetNode = FocusTargetNode()
            val softKeyboardInterceptionNode =
                object : SoftKeyboardInterceptionModifierNode, Modifier.Node() {
                    override fun onInterceptKeyBeforeSoftKeyboard(event: KeyEvent) =
                        keyEvents.add(event)

                    override fun onPreInterceptKeyBeforeSoftKeyboard(event: KeyEvent) = false
                }

            init {
                delegate(focusTargetNode)
                delegate(softKeyboardInterceptionNode)
            }
        }

        val focusTargetAndSoftKeyboardNode = FocusTargetAndSoftKeyboardNode()
        val focusTargetAndSoftKeyboardModifier =
            elementFor(key1 = null, focusTargetAndSoftKeyboardNode)

        val focusRequester = FocusRequester()
        val targetTestTag = "target"

        rule.setFocusableContent(extraItemForInitialFocus = false) {
            Box(
                modifier =
                    Modifier.testTag(targetTestTag)
                        .focusRequester(focusRequester)
                        .then(focusTargetAndSoftKeyboardModifier)
            )
        }

        rule.runOnUiThread { focusRequester.requestFocus() }
        assertThat(focusTargetAndSoftKeyboardNode.focusTargetNode.focusState.isFocused).isTrue()

        // This test specifically uses performKeyPress over performKeyInput as performKeyPress calls
        // sendKeyEvent, which in turn notifies FocusOwner that there's a
        // SoftKeyboardInterceptionModifierNode-interceptable key event first. performKeyInput goes
        // through dispatchKeyEvent which does not notify SoftKeyboardInterceptionModifierNodes.
        rule
            .onRoot()
            .performKeyPress(
                KeyEvent(
                    NativeKeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER
                    )
                )
            )

        assertThat(focusTargetAndSoftKeyboardNode.keyEvents).hasSize(1)
        assertThat(focusTargetAndSoftKeyboardNode.keyEvents[0].key).isEqualTo(Key.Enter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusTarget_nodeThatIsRotaryInputNodeKind_implementing_receivesRotaryEventsWhenFocused() {
        class FocusTargetAndRotaryNode : DelegatingNode(), RotaryInputModifierNode {
            val events = mutableListOf<RotaryScrollEvent>()
            val focusTargetNode = FocusTargetNode()

            init {
                delegate(focusTargetNode)
            }

            override fun onRotaryScrollEvent(event: RotaryScrollEvent) = events.add(event)

            override fun onPreRotaryScrollEvent(event: RotaryScrollEvent) = false
        }

        val focusTargetAndRotaryNode = FocusTargetAndRotaryNode()
        val focusTargetAndRotaryModifier = elementFor(key1 = null, focusTargetAndRotaryNode)

        val focusRequester = FocusRequester()
        val targetTestTag = "target"

        rule.setFocusableContent(extraItemForInitialFocus = false) {
            Box(
                modifier =
                    Modifier.testTag(targetTestTag)
                        .focusRequester(focusRequester)
                        .then(focusTargetAndRotaryModifier)
            )
        }

        rule.runOnUiThread { focusRequester.requestFocus() }
        assertThat(focusTargetAndRotaryNode.focusTargetNode.focusState.isFocused).isTrue()

        rule.onNodeWithTag(targetTestTag).performRotaryScrollInput {
            rotateToScrollVertically(100f)
        }

        assertThat(focusTargetAndRotaryNode.events).hasSize(1)
        assertThat(focusTargetAndRotaryNode.events[0].verticalScrollPixels).isEqualTo(100f)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun focusTarget_nodeThatIsRotaryInputNodeKind_delegating_receivesRotaryEventsWhenFocused() {
        class FocusTargetAndRotaryNode : DelegatingNode() {
            val events = mutableListOf<RotaryScrollEvent>()
            val focusTargetNode = FocusTargetNode()
            val rotaryInputNode =
                object : RotaryInputModifierNode, Modifier.Node() {
                    override fun onRotaryScrollEvent(event: RotaryScrollEvent) = events.add(event)

                    override fun onPreRotaryScrollEvent(event: RotaryScrollEvent) = false
                }

            init {
                delegate(focusTargetNode)
                delegate(rotaryInputNode)
            }
        }

        val focusTargetAndRotaryNode = FocusTargetAndRotaryNode()
        val focusTargetAndRotaryModifier = elementFor(key1 = null, focusTargetAndRotaryNode)

        val focusRequester = FocusRequester()
        val targetTestTag = "target"

        rule.setFocusableContent(extraItemForInitialFocus = false) {
            Box(
                modifier =
                    Modifier.testTag(targetTestTag)
                        .focusRequester(focusRequester)
                        .then(focusTargetAndRotaryModifier)
            )
        }

        rule.runOnUiThread { focusRequester.requestFocus() }
        assertThat(focusTargetAndRotaryNode.focusTargetNode.focusState.isFocused).isTrue()

        rule.onNodeWithTag(targetTestTag).performRotaryScrollInput {
            rotateToScrollVertically(100f)
        }

        assertThat(focusTargetAndRotaryNode.events).hasSize(1)
        assertThat(focusTargetAndRotaryNode.events[0].verticalScrollPixels).isEqualTo(100f)
    }

    /**
     * Tests that when a node is both a NodeKind.FocusEvent and NodeKind.FocusTarget, the node
     * receives events on detach.
     */
    @Test
    fun focusEventNodeDelegatingToFocusTarget_invalidatedOnRemoval() {
        var composeFocusableBox by mutableStateOf(true)
        val focusTargetNode = FocusTargetNode()

        class FocusEventAndFocusTargetNode : DelegatingNode(), FocusEventModifierNode {
            val focusStates = mutableListOf<FocusState>()

            override fun onFocusEvent(focusState: FocusState) {
                focusStates.add(focusState)
            }

            init {
                delegate(focusTargetNode)
            }
        }

        val focusEventAndFocusTargetNode = FocusEventAndFocusTargetNode()
        val focusEventAndFocusTargetModifier = elementFor(key1 = null, focusEventAndFocusTargetNode)

        val focusRequester = FocusRequester()

        rule.setFocusableContent(extraItemForInitialFocus = false) {
            if (composeFocusableBox) {
                Box(
                    modifier =
                        Modifier.focusRequester(focusRequester)
                            .then(focusEventAndFocusTargetModifier)
                )
            }
        }

        assertThat(focusEventAndFocusTargetNode.focusStates).hasSize(1)
        assertThat(focusEventAndFocusTargetNode.focusStates[0].isFocused).isFalse()

        rule.runOnUiThread { focusRequester.requestFocus() }
        rule.waitForIdle()

        assertThat(focusEventAndFocusTargetNode.focusStates).hasSize(2)
        assertThat(focusEventAndFocusTargetNode.focusStates[0].isFocused).isFalse()
        assertThat(focusEventAndFocusTargetNode.focusStates[1].isFocused).isTrue()

        composeFocusableBox = false
        rule.waitForIdle()

        assertThat(focusEventAndFocusTargetNode.focusStates).hasSize(3)
        assertThat(focusEventAndFocusTargetNode.focusStates[0].isFocused).isFalse()
        assertThat(focusEventAndFocusTargetNode.focusStates[1].isFocused).isTrue()
        assertThat(focusEventAndFocusTargetNode.focusStates[2].isFocused).isFalse()
    }

    @Test
    fun modifierDelegatingToFocusEventAndFocusTarget_invalidatedOnRemoval() {
        var composeFocusableBox by mutableStateOf(true)

        class MyFocusEventNode : Modifier.Node(), FocusEventModifierNode {
            val focusStates = mutableListOf<FocusState>()

            override fun onFocusEvent(focusState: FocusState) {
                focusStates.add(focusState)
            }
        }

        val eventNode = MyFocusEventNode()
        val focusTargetNode = FocusTargetNode()

        class FocusEventAndFocusTargetNode : DelegatingNode() {
            init {
                delegate(focusTargetNode)
                delegate(eventNode)
            }
        }

        val focusEventAndFocusTargetNode = FocusEventAndFocusTargetNode()
        val focusEventAndFocusTargetModifier = elementFor(key1 = null, focusEventAndFocusTargetNode)

        val focusRequester = FocusRequester()

        rule.setFocusableContent(extraItemForInitialFocus = false) {
            if (composeFocusableBox) {
                Box(
                    modifier =
                        Modifier.focusRequester(focusRequester)
                            .then(focusEventAndFocusTargetModifier)
                )
            }
        }

        assertThat(eventNode.focusStates).hasSize(1)
        assertThat(eventNode.focusStates[0].isFocused).isFalse()

        rule.runOnUiThread { focusRequester.requestFocus() }
        rule.waitForIdle()

        assertThat(eventNode.focusStates).hasSize(2)
        assertThat(eventNode.focusStates[0].isFocused).isFalse()
        assertThat(eventNode.focusStates[1].isFocused).isTrue()

        composeFocusableBox = false
        rule.waitForIdle()

        assertThat(eventNode.focusStates).hasSize(3)
        assertThat(eventNode.focusStates[0].isFocused).isFalse()
        assertThat(eventNode.focusStates[1].isFocused).isTrue()
        assertThat(eventNode.focusStates[2].isFocused).isFalse()
    }

    @Test
    fun reuseInactiveFocusTarget_stateInitializedToInactive() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        var reuseKey by mutableStateOf(0)
        rule.setFocusableContent {
            ReusableContent(reuseKey) { Box(Modifier.then(elementFor(instance = focusTarget))) }
        }
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }

        // Act.
        rule.runOnIdle { reuseKey = 1 }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun reuseActiveFocusTarget_stateInitializedToInactive() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        val focusRequester = FocusRequester()
        var reuseKey by mutableStateOf(0)
        rule.setFocusableContent {
            ReusableContent(reuseKey) {
                Box(
                    Modifier.focusRequester(focusRequester).then(elementFor(instance = focusTarget))
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Active)
        }

        // Act.
        rule.runOnIdle { reuseKey = 1 }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun reuseCapturedFocusTarget_stateInitializedToInactive() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        val focusRequester = FocusRequester()
        var reuseKey by mutableStateOf(0)
        rule.setFocusableContent {
            ReusableContent(reuseKey) {
                Box(
                    Modifier.focusRequester(focusRequester).then(elementFor(instance = focusTarget))
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Captured)
        }

        // Act.
        rule.runOnIdle { reuseKey = 1 }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun reuseActiveParentFocusTarget_stateInitializedToInactive() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        val focusRequester = FocusRequester()
        var reuseKey by mutableStateOf(0)
        rule.setFocusableContent {
            ReusableContent(reuseKey) {
                Box(Modifier.then(elementFor(instance = focusTarget))) {
                    Box(Modifier.focusRequester(focusRequester).focusTarget())
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(ActiveParent)
        }

        // Act.
        rule.runOnIdle { reuseKey = 1 }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun moveInactiveFocusTarget_stateInitializedToInactive() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        var moveContent by mutableStateOf(false)
        val content = movableContentOf { Box(Modifier.then(elementFor(instance = focusTarget))) }
        rule.setFocusableContent {
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }

        // Act.
        rule.runOnIdle { moveContent = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun moveActiveFocusTarget_stateInitializedToInactive() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        val focusRequester = FocusRequester()
        var moveContent by mutableStateOf(false)
        val content = movableContentOf {
            Box(Modifier.focusRequester(focusRequester).then(elementFor(instance = focusTarget)))
        }
        rule.setFocusableContent {
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Active)
        }

        // Act.
        rule.runOnIdle { moveContent = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun moveCapturedFocusTarget_stateInitializedToInactive() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        val focusRequester = FocusRequester()
        var moveContent by mutableStateOf(false)
        val content = movableContentOf {
            Box(Modifier.focusRequester(focusRequester).then(elementFor(instance = focusTarget)))
        }
        rule.setFocusableContent {
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Captured)
        }

        // Act.
        rule.runOnIdle { moveContent = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }
    }

    @Test
    fun moveActiveParentFocusTarget_stateInitializedToInactive() {
        // Arrange.
        val focusTarget = FocusTargetNode()
        val focusRequester = FocusRequester()
        var moveContent by mutableStateOf(false)
        val content = movableContentOf {
            Box(Modifier.then(elementFor(instance = focusTarget))) {
                Box(Modifier.focusRequester(focusRequester).focusTarget())
            }
        }
        rule.setFocusableContent {
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(ActiveParent)
        }

        // Act.
        rule.runOnIdle { moveContent = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusTarget.isInitialized()).isTrue()
            assertThat(focusTarget.focusState).isEqualTo(Inactive)
        }
    }

    private inline fun Modifier.thenIf(condition: Boolean, block: () -> Modifier): Modifier {
        return if (condition) then(block()) else this
    }
}
