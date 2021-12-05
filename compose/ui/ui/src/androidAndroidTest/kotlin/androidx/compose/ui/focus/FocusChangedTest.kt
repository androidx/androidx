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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusChangedTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun active_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget(FocusModifier(Active))
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @ExperimentalComposeUiApi
    @Test
    fun activeParent_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        val (focusRequester, childFocusRequester) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { childFocusState = it }
                        .focusRequester(childFocusRequester)
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            childFocusRequester.requestFocus()
            assertThat(focusState.hasFocus).isTrue()
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
            assertThat(childFocusState.isFocused).isFalse()
        }
    }

    @Test
    fun captured_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget(FocusModifier(Captured))
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isCaptured).isTrue()
        }
    }

    @Test
    fun deactivated_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusTarget()
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isDeactivated).isTrue()
        }
    }

    @ExperimentalComposeUiApi
    @Test
    fun deactivatedParent_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        lateinit var childFocusState: FocusState
        val (focusRequester, childFocusRequester) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = false }
                    .focusTarget()
            ) {
                Box(
                    modifier = Modifier
                        .onFocusChanged { childFocusState = it }
                        .focusRequester(childFocusRequester)
                        .focusTarget()
                )
            }
        }
        rule.runOnIdle {
            childFocusRequester.requestFocus()
            assertThat(childFocusState.isFocused).isTrue()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.isDeactivated).isTrue()
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(childFocusState.isFocused).isTrue()
            assertThat(focusState.hasFocus).isTrue()
            assertThat(focusState.isFocused).isFalse()
            assertThat(focusState.isDeactivated).isTrue()
        }
    }

    @Test
    fun inactive_requestFocus() {
        // Arrange.
        lateinit var focusState: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState = it }
                    .focusRequester(focusRequester)
                    .focusTarget(FocusModifier(Inactive))
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState.isFocused).isTrue()
        }
    }

    @Test
    fun inactive_requestFocus_multipleObservers() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        lateinit var focusState3: FocusState
        lateinit var focusState4: FocusState
        lateinit var focusState5: FocusState
        lateinit var focusState6: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState1 = it }
                    .onFocusChanged { focusState2 = it }
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .onFocusChanged { focusState3 = it }
                            .onFocusChanged { focusState4 = it }
                    ) {
                        Box(
                            modifier = Modifier
                                .onFocusChanged { focusState5 = it }
                                .onFocusChanged { focusState6 = it }
                                .focusRequester(focusRequester)
                                .focusTarget(FocusModifier(Inactive))
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState1.isFocused).isTrue()
            assertThat(focusState2.isFocused).isTrue()
            assertThat(focusState3.isFocused).isTrue()
            assertThat(focusState4.isFocused).isTrue()
            assertThat(focusState5.isFocused).isTrue()
            assertThat(focusState6.isFocused).isTrue()
        }
    }

    @Test
    fun active_requestFocus_multipleObserversWithExtraFocusTargetInBetween() {
        // Arrange.
        lateinit var focusState1: FocusState
        lateinit var focusState2: FocusState
        lateinit var focusState3: FocusState
        lateinit var focusState4: FocusState
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            Box(
                modifier = Modifier
                    .onFocusChanged { focusState1 = it }
                    .onFocusChanged { focusState2 = it }
                    .focusTarget()
                    .onFocusChanged { focusState3 = it }
                    .onFocusChanged { focusState4 = it }
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }

        rule.runOnIdle {
            // Act.
            focusRequester.requestFocus()

            // Assert.
            assertThat(focusState1.hasFocus).isTrue()
            assertThat(focusState2.hasFocus).isTrue()
            assertThat(focusState3.isFocused).isTrue()
            assertThat(focusState4.isFocused).isTrue()
        }
    }
}

private val FocusState.isDeactivated: Boolean
    get() = (this as FocusStateImpl).isDeactivated
