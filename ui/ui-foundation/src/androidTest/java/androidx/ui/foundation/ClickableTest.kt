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
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.layout.Stack
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsNotEnabled
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendClick
import androidx.ui.test.sendDoubleClick
import androidx.ui.test.sendLongClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class ClickableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickableTest_defaultSemantics() {
        composeTestRule.setContent {
            Stack {
                Text("ClickableText", modifier = Modifier.testTag("myClickable").clickable {})
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
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(enabled = false) {}
                )
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
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onClick = onClick)
                )
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
    fun clickableTest_longClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onLongClick = onClick) {}
                )
            }
        }

        findByTag("myClickable")
            .doGesture {
                sendLongClick()
            }

        runOnIdleCompose {
            assertThat(counter).isEqualTo(1)
        }

        findByTag("myClickable")
            .doGesture {
                sendLongClick()
            }

        runOnIdleCompose {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_click_withLongClick() {
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        findByTag("myClickable")
            .doGesture {
                sendClick()
            }

        runOnIdleCompose {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
        }

        findByTag("myClickable")
            .doGesture {
                sendLongClick()
            }

        runOnIdleCompose {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun clickableTest_click_withDoubleClick() {
        val clickLatch = CountDownLatch(1)
        var doubleClickCounter = 0
        val onClick: () -> Unit = { clickLatch.countDown() }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            onDoubleClick = onDoubleClick,
                            onClick = onClick
                        )
                )
            }
        }

        findByTag("myClickable")
            .doClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        findByTag("myClickable")
            .doGesture {
                sendDoubleClick()
            }

        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    fun clickableTest_click_withDoubleClick_andLongClick() {
        val clickLatch = CountDownLatch(1)
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { clickLatch.countDown() }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            onDoubleClick = onDoubleClick,
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        findByTag("myClickable")
            .doClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        findByTag("myClickable")
            .doGesture {
                sendDoubleClick()
            }

        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }

        findByTag("myClickable")
            .doGesture {
                sendLongClick()
            }

        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    fun clickableTest_doubleClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onDoubleClick = onClick) {}
                )
            }
        }

        findByTag("myClickable")
            .doGesture {
                sendDoubleClick()
            }

        runOnIdleCompose {
            assertThat(counter).isEqualTo(1)
        }

        findByTag("myClickable")
            .doGesture {
                sendDoubleClick()
            }

        runOnIdleCompose {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun clickableTest_interactionState() {
        val interactionState = InteractionState()

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(interactionState = interactionState) {}
                )
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
                if (emitClickableText) {
                    Text(
                        "ClickableText",
                        modifier = Modifier
                            .testTag("myClickable")
                            .clickable(interactionState = interactionState) {}
                    )
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

    @Test
    fun clickableTest_click_withDoubleClick_andLongClick_disabled() {
        val enabled = mutableStateOf(false)
        val clickLatch = CountDownLatch(1)
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { clickLatch.countDown() }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        composeTestRule.setContent {
            Stack {
                Text(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            enabled = enabled.value,
                            onDoubleClick = onDoubleClick,
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        findByTag("myClickable")
            .doClick()

        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
        }

        findByTag("myClickable")
            .doGesture {
                sendDoubleClick()
            }

        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
        }

        findByTag("myClickable")
            .doGesture {
                sendLongClick()
            }

        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.count).isEqualTo(1)
            enabled.value = true
        }

        findByTag("myClickable")
            .doClick()

        val res = clickLatch.await(1000, TimeUnit.MILLISECONDS)
        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(res).isTrue()
        }

        findByTag("myClickable")
            .doGesture {
                sendDoubleClick()
            }

        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }

        findByTag("myClickable")
            .doGesture {
                sendLongClick()
            }

        runOnIdleCompose {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }
}