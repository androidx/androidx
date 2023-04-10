/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class RequestFocusEnterExitTest {
    @get:Rule
    val rule = createComposeRule()

    private val source = FocusRequester()
    private val destination = FocusRequester()
    private var counter = 0
    private val grandParent = EnterExitCounter()
    private val parent1 = EnterExitCounter()
    private val parent2 = EnterExitCounter()
    private val child1 = EnterExitCounter()
    private val child2 = EnterExitCounter()
    private val child3 = EnterExitCounter()
    private val child4 = EnterExitCounter()

    @Test
    fun triggersExitAndEnter() {
        // Arrange.
        rule.setFocusableContent {
            Box(Modifier.focusTarget(grandParent)) {
                Box(Modifier.focusTarget(parent1)) {
                    Box(Modifier.focusRequester(source).focusTarget(child1))
                    Box(Modifier.focusTarget(child2))
                }
                Box(Modifier.focusTarget(parent2)) {
                    Box(Modifier.focusTarget(child3))
                    Box(Modifier.focusRequester(destination).focusTarget(child4))
                }
            }
        }
        rule.runOnIdle {
            source.requestFocus()
            resetCounters()
        }

        // Act.
        rule.runOnIdle {
            destination.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(child1.enterExit).isEqualTo("0, 0")
            assertThat(child2.enterExit).isEqualTo("0, 0")
            assertThat(parent1.enterExit).isEqualTo("0, 1")
            assertThat(grandParent.enterExit).isEqualTo("0, 0")
            assertThat(parent2.enterExit).isEqualTo("2, 0")
            assertThat(child3.enterExit).isEqualTo("0, 0")
            assertThat(child4.enterExit).isEqualTo("0, 0")
        }
    }

    @Test
    fun exitPropertyOnFocusedItem_cantStopFocusFromLeaving() {
        // Arrange.
        rule.setFocusableContent {
            Box(Modifier.focusTarget(grandParent)) {
                Box(Modifier.focusTarget(parent1)) {
                    Box(
                        Modifier
                            .focusRequester(source)
                            .focusProperties {
                                enter = { child1.enter = counter++; Default }
                                exit = { child1.exit = counter++; Cancel }
                            }
                            .focusTarget()
                    )
                    Box(Modifier.focusTarget(child2))
                }
                Box(Modifier.focusTarget(parent2)) {
                    Box(Modifier.focusTarget(child3))
                    Box(Modifier.focusRequester(destination).focusTarget(child4))
                }
            }
        }
        rule.runOnIdle {
            source.requestFocus()
            resetCounters()
        }

        // Act.
        rule.runOnIdle {
            destination.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(child1.enterExit).isEqualTo("0, 0")
            assertThat(child2.enterExit).isEqualTo("0, 0")
            assertThat(parent1.enterExit).isEqualTo("0, 1")
            assertThat(grandParent.enterExit).isEqualTo("0, 0")
            assertThat(parent2.enterExit).isEqualTo("2, 0")
            assertThat(child3.enterExit).isEqualTo("0, 0")
            assertThat(child4.enterExit).isEqualTo("0, 0")
        }
    }

    @Test
    fun exitPropertyOnParent1_stopsFocusChange() {
        // Arrange.
        rule.setFocusableContent {
            Box(Modifier.focusTarget(grandParent)) {
                Box(
                    Modifier
                        .focusProperties {
                            enter = { parent1.enter = counter++; Default }
                            exit = { parent1.exit = counter++; Cancel }
                        }
                        .focusTarget()
                ) {
                    Box(Modifier.focusRequester(source).focusTarget(child1))
                    Box(Modifier.focusTarget(child2))
                }
                Box(Modifier.focusTarget(parent2)) {
                    Box(Modifier.focusTarget(child3))
                    Box(Modifier.focusRequester(destination).focusTarget(child4))
                }
            }
        }
        rule.runOnIdle {
            source.requestFocus()
            resetCounters()
        }

        // Act.
        rule.runOnIdle {
            destination.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(child1.enterExit).isEqualTo("0, 0")
            assertThat(child2.enterExit).isEqualTo("0, 0")
            assertThat(parent1.enterExit).isEqualTo("0, 1")
            assertThat(grandParent.enterExit).isEqualTo("0, 0")
            assertThat(parent2.enterExit).isEqualTo("0, 0")
            assertThat(child3.enterExit).isEqualTo("0, 0")
            assertThat(child4.enterExit).isEqualTo("0, 0")
        }
    }

    @Test
    fun exitPropertyOnGrandparent_cantStopFocusChange() {
        // Arrange.
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties {
                        enter = { grandParent.enter = counter++; Default }
                        exit = { grandParent.exit = counter++; Cancel }
                    }
                    .focusTarget()
            ) {
                Box(Modifier.focusTarget(parent1)) {
                    Box(Modifier.focusRequester(source).focusTarget(child1))
                    Box(Modifier.focusTarget(child2))
                }
                Box(Modifier.focusTarget(parent2)) {
                    Box(Modifier.focusTarget(child3))
                    Box(Modifier.focusRequester(destination).focusTarget(child4))
                }
            }
        }
        rule.runOnIdle {
            source.requestFocus()
            resetCounters()
        }

        // Act.
        rule.runOnIdle {
            destination.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(child1.enterExit).isEqualTo("0, 0")
            assertThat(child2.enterExit).isEqualTo("0, 0")
            assertThat(parent1.enterExit).isEqualTo("0, 1")
            assertThat(grandParent.enterExit).isEqualTo("0, 0")
            assertThat(parent2.enterExit).isEqualTo("2, 0")
            assertThat(child3.enterExit).isEqualTo("0, 0")
            assertThat(child4.enterExit).isEqualTo("0, 0")
        }
    }

    @Test
    fun enterPropertyOnGrandparent_cantStopFocusChange() {
        // Arrange.
        var init = true
        rule.setFocusableContent {
            Box(
                Modifier
                    .focusProperties {
                        enter = { grandParent.enter = counter++; if (init) Default else Cancel }
                        exit = { grandParent.exit = counter++; Default }
                    }
                    .focusTarget()
            ) {
                Box(Modifier.focusTarget(parent1)) {
                    Box(Modifier.focusRequester(source).focusTarget(child1))
                    Box(Modifier.focusTarget(child2))
                }
                Box(Modifier.focusTarget(parent2)) {
                    Box(Modifier.focusTarget(child3))
                    Box(Modifier.focusRequester(destination).focusTarget(child4))
                }
            }
        }
        rule.runOnIdle {
            source.requestFocus()
            resetCounters()
            init = false
        }

        // Act.
        rule.runOnIdle {
            destination.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(child1.enterExit).isEqualTo("0, 0")
            assertThat(child2.enterExit).isEqualTo("0, 0")
            assertThat(parent1.enterExit).isEqualTo("0, 1")
            assertThat(grandParent.enterExit).isEqualTo("0, 0")
            assertThat(parent2.enterExit).isEqualTo("2, 0")
            assertThat(child3.enterExit).isEqualTo("0, 0")
            assertThat(child4.enterExit).isEqualTo("0, 0")
        }
    }

    @Test
    fun enterPropertyOnParent2_stopsFocusChange() {
        // Arrange.
        rule.setFocusableContent {
            Box(Modifier.focusTarget(grandParent)) {
                Box(Modifier.focusTarget(parent1)) {
                    Box(Modifier.focusRequester(source).focusTarget(child1))
                    Box(Modifier.focusTarget(child2))
                }
                Box(
                    Modifier
                        .focusProperties {
                            enter = { parent2.enter = counter++; Cancel }
                            exit = { parent2.exit = counter++; Default }
                        }
                        .focusTarget()
                ) {
                    Box(Modifier.focusTarget(child3))
                    Box(Modifier.focusRequester(destination).focusTarget(child4))
                }
            }
        }
        rule.runOnIdle {
            source.requestFocus()
            resetCounters()
        }

        // Act.
        rule.runOnIdle {
            destination.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(child1.enterExit).isEqualTo("0, 0")
            assertThat(child2.enterExit).isEqualTo("0, 0")
            assertThat(parent1.enterExit).isEqualTo("0, 1")
            assertThat(grandParent.enterExit).isEqualTo("0, 0")
            assertThat(parent2.enterExit).isEqualTo("2, 0")
            assertThat(child3.enterExit).isEqualTo("0, 0")
            assertThat(child4.enterExit).isEqualTo("0, 0")
        }
    }

    @Test
    fun enterPropertyOnDestination_cantStopFocusFromMoving() {
        // Arrange.
        rule.setFocusableContent {
            Box(Modifier.focusTarget(grandParent)) {
                Box(Modifier.focusTarget(parent1)) {
                    Box(Modifier.focusRequester(source).focusTarget(child1))
                    Box(Modifier.focusTarget(child2))
                }
                Box(Modifier.focusTarget(parent2)) {
                    Box(Modifier.focusTarget(child3))
                    Box(
                        Modifier
                            .focusRequester(destination)
                            .focusProperties {
                                enter = { child4.enter = counter++; Cancel }
                                exit = { child4.exit = counter++; Default }
                            }
                            .focusTarget()
                    )
                }
            }
        }
        rule.runOnIdle {
            source.requestFocus()
            resetCounters()
        }

        // Act.
        rule.runOnIdle {
            destination.requestFocus()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(child1.enterExit).isEqualTo("0, 0")
            assertThat(child2.enterExit).isEqualTo("0, 0")
            assertThat(parent1.enterExit).isEqualTo("0, 1")
            assertThat(grandParent.enterExit).isEqualTo("0, 0")
            assertThat(parent2.enterExit).isEqualTo("2, 0")
            assertThat(child3.enterExit).isEqualTo("0, 0")
            assertThat(child4.enterExit).isEqualTo("0, 0")
        }
    }
    private class EnterExitCounter(var enter: Int = 0, var exit: Int = 0) {
        val enterExit: String get() = "$enter, $exit"

        fun reset() {
            enter = 0
            exit = 0
        }
    }

    private fun Modifier.focusTarget(enterExitCounter: EnterExitCounter): Modifier = this
        .focusProperties {
            enter = { enterExitCounter.enter = counter++; Default }
            exit = { enterExitCounter.exit = counter++; Default }
        }
        .focusTarget()

    private fun resetCounters() {
        counter = 1
        grandParent.reset()
        parent1.reset()
        parent2.reset()
        child1.reset()
        child2.reset()
        child3.reset()
        child4.reset()
    }
}
