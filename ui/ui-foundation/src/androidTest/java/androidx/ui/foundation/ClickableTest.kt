/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.filters.MediumTest
import androidx.ui.core.TestTag
import androidx.ui.layout.Stack
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsNotEnabled
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ClickableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickableTest_defaultSemantics() {
        composeTestRule.setContent {
            Stack {
                TestTag(tag = "myClickable") {
                    Clickable(onClick = {}) {
                        Text("ClickableText")
                    }
                }
            }
        }

        findByTag("myClickable")
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun clickableTest_disabledSemantics() {
        composeTestRule.setContent {
            Stack {
                TestTag(tag = "myClickable") {
                    Clickable(onClick = {}, enabled = false) {
                        Text("ClickableText")
                    }
                }
            }
        }

        findByTag("myClickable")
            .assertIsNotEnabled()
            .assertHasNoClickAction()
    }

    @Test
    fun clickableTest_click() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        composeTestRule.setContent {
            Stack {
                TestTag(tag = "myClickable") {
                    Clickable(onClick = onClick) {
                        Text("ClickableText")
                    }
                }
            }
        }

        findByTag("myClickable")
            .doClick()

        runOnIdleCompose {
            assertThat(counter).isEqualTo(1)
        }

        findByTag("myClickable")
            .doClick()

        runOnIdleCompose {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_interactionState() {
        val interactionState = InteractionState()

        composeTestRule.setContent {
            Stack {
                TestTag(tag = "myClickable") {
                    Clickable(onClick = {}, interactionState = interactionState) {
                        Text("ClickableText")
                    }
                }
            }
        }

        runOnIdleCompose {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        // TODO: b/154498119 simulate press event, replace with gesture injection when supported
        runOnIdleCompose {
            interactionState.addInteraction(Interaction.Pressed)
        }

        runOnIdleCompose {
            assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        // TODO: b/154498119 simulate press event, replace with gesture injection when supported
        runOnIdleCompose {
            interactionState.removeInteraction(Interaction.Pressed)
        }

        runOnIdleCompose {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }

    @Test
    fun clickableTest_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitClickableText by mutableStateOf(true)

        composeTestRule.setContent {
            Stack {
                TestTag(tag = "myClickable") {
                    if (emitClickableText) {
                        Clickable(onClick = {}, interactionState = interactionState) {
                            Text("ClickableText")
                        }
                    }
                }
            }
        }

        runOnIdleCompose {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        // TODO: b/154498119 simulate press event, replace with gesture injection when supported
        runOnIdleCompose {
            interactionState.addInteraction(Interaction.Pressed)
        }

        runOnIdleCompose {
            assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        // Dispose clickable
        runOnIdleCompose {
            emitClickableText = false
        }

        runOnIdleCompose {
            assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }
}