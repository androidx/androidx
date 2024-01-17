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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusTargetAttachDetachTest {
    @get:Rule
    val rule = createComposeRule()

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
                    modifier = if (observingFocusTarget1) {
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
                    modifier = if (onFocusChangedHasFocusTarget) {
                        onFocusChanged
                            .then(focusRequesterModifier)
                            .then(focusTarget)
                    } else {
                        focusTarget
                            .then(onFocusChanged)
                            .then(focusRequesterModifier)
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
                Modifier
                    .onFocusChanged { focusState = it }
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
                Modifier
                    .onFocusChanged { focusState = it }
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
                Modifier
                    .focusRequester(focusRequester)
                    .thenIf(optionalModifiers) {
                        Modifier
                            .onFocusEvent { focusState = it }
                            .focusTarget()
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
                    Modifier
                        .focusRequester(focusRequester)
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
                Modifier
                    .onFocusChanged { focusState = it }
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
                Modifier
                    .onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) { Modifier.focusTarget() }
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
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
                Modifier
                    .onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) {
                        Modifier
                            .focusTarget()
                            .focusRequester(focusRequester)
                            .focusTarget()
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
            Box(
                Modifier
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier
                        .onFocusChanged { focusState = it }
                        .thenIf(optionalFocusTargets) {
                            Modifier
                                .focusTarget()
                                .focusRequester(focusRequester)
                                .focusTarget()
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
            Box(
                Modifier
                    .onFocusChanged { parentFocusState = it }
                    .focusTarget()
            ) {
                if (optionalBox) {
                    Box(
                        Modifier
                            .onFocusChanged { focusState = it }
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
                Modifier
                    .onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                    }
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
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
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun removedDeactivatedParentFocusTarget_pointsToNextDeactivatedParentFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var optionalFocusTarget by mutableStateOf(true)
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .thenIf(optionalFocusTarget) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                    }
            ) {
                Box(
                    Modifier
                        .onFocusChanged { focusState = it }
                        .focusProperties { canFocus = false }
                        .focusTarget()
                ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .focusTarget()
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
                Modifier
                    .onFocusChanged { focusState = it }
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    Modifier.thenIf(optionalFocusTarget) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                    }
                ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .focusTarget()
                    )
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
                Modifier
                    .onFocusChanged { focusState = it }
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    Modifier.thenIf(optionalFocusTarget) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                        }
                ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .thenIf(optionalFocusTarget) { Modifier.focusTarget() }
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
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .focusTarget()
            ) {
                Box(
                    Modifier.thenIf(optionalFocusTarget) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                    }
                ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .thenIf(optionalFocusTarget) { Modifier.focusTarget() }
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
                Modifier
                    .onFocusChanged { focusState = it }
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
                Modifier
                    .onFocusChanged { focusState = it }
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
                Modifier
                    .onFocusChanged { focusState = it }
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
    fun addedFocusTarget_withinActiveNode() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier
                    .thenIf(addFocusTarget) {
                        Modifier
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(FocusStateImpl.ActiveParent) }
    }

    @Test
    fun addedFocusTarget_withinActiveHierarchy() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier.thenIf(addFocusTarget) {
                    Modifier
                        .onFocusChanged { focusState = it }
                        .focusTarget()
                }
            ) {
                Box(Modifier.focusRequester(focusRequester).focusTarget())
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(FocusStateImpl.ActiveParent) }
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
                Modifier
                    .thenIf(addFocusTarget1) {
                        Modifier
                            .onFocusChanged { focusState1 = it }
                            .focusTarget()
                    }
                    .thenIf(addFocusTarget2) {
                        Modifier
                            .onFocusChanged { focusState2 = it }
                            .focusTarget()
                    }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            addFocusTarget1 = true
            addFocusTarget2 = true
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState1).isEqualTo(FocusStateImpl.ActiveParent)
            assertThat(focusState2).isEqualTo(FocusStateImpl.ActiveParent)
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
                    Modifier
                        .onFocusChanged { focusState1 = it }
                        .focusTarget()
                    }
            ) {
                Box(
                    Modifier.thenIf(addFocusTarget1) {
                        Modifier
                            .onFocusChanged { focusState2 = it }
                            .focusTarget()
                    }
                ) {
                    Box(
                        Modifier
                            .focusRequester(focusRequester)
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        rule.runOnIdle {
            addFocusTarget1 = true
            addFocusTarget2 = true
        }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState1).isEqualTo(FocusStateImpl.ActiveParent)
            assertThat(focusState2).isEqualTo(FocusStateImpl.ActiveParent)
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
                Modifier
                    .thenIf(addFocusTarget) {
                        Modifier
                            .onFocusChanged { focusState = it }
                            .focusTarget()
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
        rule.runOnIdle { assertThat(focusState).isEqualTo(FocusStateImpl.ActiveParent) }
    }

    @Test
    fun addedFocusTarget_withinCapturedHierarchy() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        var addFocusTarget by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier
                    .thenIf(addFocusTarget) {
                        Modifier
                            .onFocusChanged { focusState = it }
                            .focusTarget()
                    }
            ) {
                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            focusRequester.captureFocus()
        }

        // Act.
        rule.runOnIdle { addFocusTarget = true }

        // Assert.
        rule.runOnIdle { assertThat(focusState).isEqualTo(FocusStateImpl.ActiveParent) }
    }

    @Test
    fun removingDeactivatedItem_withNoNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        var removeDeactivatedItem by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .thenIf(!removeDeactivatedItem) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                    }
            )
        }

        // Act.
        rule.runOnIdle { removeDeactivatedItem = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
        }
    }

    fun removingDeactivatedItem_withInactiveNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        var removeDeactivatedItem by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .thenIf(!removeDeactivatedItem) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                    }
            ) {
                Box(Modifier.focusTarget())
            }
        }

        // Act.
        rule.runOnIdle { removeDeactivatedItem = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun removingDeactivatedItem_withDeactivatedNextFocusTarget() {
        // Arrange.
        lateinit var focusState: FocusState
        var removeDeactivatedItem by mutableStateOf(false)
        rule.setFocusableContent {
            Box(
                Modifier
                    .onFocusChanged { focusState = it }
                    .thenIf(!removeDeactivatedItem) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .focusTarget()
                        }
            ) {
                Box(Modifier
                    .focusProperties { canFocus = false }
                    .focusTarget()
                )
            }
        }

        // Act.
        rule.runOnIdle { removeDeactivatedItem = true }

        // Assert.
        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
        }
    }

    private inline fun Modifier.thenIf(condition: Boolean, block: () -> Modifier): Modifier {
        return if (condition) then(block()) else this
    }
}
