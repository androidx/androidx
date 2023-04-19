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

package androidx.compose.ui.input.key

import android.view.KeyEvent.ACTION_DOWN as ActionDown
import android.view.KeyEvent.ACTION_UP as ActionUp
import android.view.KeyEvent.KEYCODE_A as KeyCodeA
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.input.key.Key.Companion.A
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.semantics.elementFor
import org.junit.Ignore
import org.mockito.kotlin.inOrder

/**
 * This test verifies that an Android key event triggers a Compose key event. More detailed test
 * cases are present at [ProcessKeyInputTest].
 */
@SmallTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalComposeUiApi::class)
class AndroidProcessKeyInputTest(private val keyEventAction: Int) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "keyEventAction = {0}")
        fun initParameters() = listOf(ActionUp, ActionDown)
    }

    @Test
    fun onKeyEvent_triggered() {
        // Arrange.
        lateinit var ownerView: View
        var receivedKeyEvent: KeyEvent? = null
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onKeyEvent {
                        receivedKeyEvent = it
                        true
                    }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        val keyConsumed = rule.runOnIdle {
            ownerView.dispatchKeyEvent(AndroidKeyEvent(keyEventAction, KeyCodeA))
        }

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedKeyEvent)
            assertThat(keyEvent.type).isEqualTo(
                when (keyEventAction) {
                    ActionUp -> KeyUp
                    ActionDown -> KeyDown
                    else -> error("No tests for this key action.")
                }
            )
            assertThat(keyEvent.key).isEqualTo(A)
            assertThat(keyConsumed).isTrue()
        }
    }

    @Ignore("b/279178695")
    @Test
    fun delegated_onKeyEvent_triggered() {
        // Arrange.
        lateinit var ownerView: View
        var receivedKeyEvent: KeyEvent? = null
        val focusRequester = FocusRequester()
        val node = object : DelegatingNode() {
            val ki = delegate(object : KeyInputModifierNode, Modifier.Node() {
                override fun onKeyEvent(event: KeyEvent): Boolean {
                    receivedKeyEvent = event
                    return true
                }

                override fun onPreKeyEvent(event: KeyEvent): Boolean {
                    return false
                }
            })
        }

        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .elementFor(node)
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        val keyConsumed = rule.runOnIdle {
            ownerView.dispatchKeyEvent(AndroidKeyEvent(keyEventAction, KeyCodeA))
        }

        rule.waitUntil { receivedKeyEvent != null }

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedKeyEvent)
            assertThat(keyEvent.type).isEqualTo(
                when (keyEventAction) {
                    ActionUp -> KeyUp
                    ActionDown -> KeyDown
                    else -> error("No tests for this key action.")
                }
            )
            assertThat(keyEvent.key).isEqualTo(A)
            assertThat(keyConsumed).isTrue()
        }
    }

    @Ignore("b/279178695")
    @Test
    fun delegated_multiple_onKeyEvent_triggered() {
        // Arrange.
        lateinit var ownerView: View
        var receivedKeyEvent1: KeyEvent? = null
        var receivedKeyEvent2: KeyEvent? = null
        val eventLog = mutableListOf<KeyEvent>()
        val focusRequester = FocusRequester()
        val node = object : DelegatingNode() {
            val a = delegate(object : KeyInputModifierNode, Modifier.Node() {
                override fun onKeyEvent(event: KeyEvent): Boolean {
                    receivedKeyEvent1 = event
                    eventLog.add(event)
                    return false
                }

                override fun onPreKeyEvent(event: KeyEvent): Boolean {
                    return false
                }
            })
            val b = delegate(object : KeyInputModifierNode, Modifier.Node() {
                override fun onKeyEvent(event: KeyEvent): Boolean {
                    receivedKeyEvent2 = event
                    eventLog.add(event)
                    return false
                }

                override fun onPreKeyEvent(event: KeyEvent): Boolean {
                    return false
                }
            })
        }

        rule.setFocusableContent {
            ownerView = LocalView.current
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .elementFor(node)
                    .focusTarget()
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Act.
        val keyConsumed = rule.runOnIdle {
            ownerView.dispatchKeyEvent(AndroidKeyEvent(keyEventAction, KeyCodeA))
        }

        rule.waitUntil { receivedKeyEvent2 != null }

        // Assert.
        rule.runOnIdle {
            val keyEvent1 = checkNotNull(receivedKeyEvent1)
            assertThat(keyEvent1.type).isEqualTo(
                when (keyEventAction) {
                    ActionUp -> KeyUp
                    ActionDown -> KeyDown
                    else -> error("No tests for this key action.")
                }
            )
            assertThat(keyEvent1.key).isEqualTo(A)
            assertThat(keyConsumed).isFalse()

            val keyEvent2 = checkNotNull(receivedKeyEvent2)
            assertThat(keyEvent2.type).isEqualTo(
                when (keyEventAction) {
                    ActionUp -> KeyUp
                    ActionDown -> KeyDown
                    else -> error("No tests for this key action.")
                }
            )
            assertThat(keyEvent2.key).isEqualTo(A)
            assertThat(keyConsumed).isFalse()

            assertThat(eventLog)
                .containsExactly(receivedKeyEvent1, receivedKeyEvent2)
                .inOrder()
        }
    }
}
