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

import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN as ActionDown
import android.view.KeyEvent.ACTION_UP as ActionUp
import android.view.KeyEvent.KEYCODE_A as KeyCodeA
import android.view.View
import androidx.compose.foundation.layout.Box
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

/**
 * This test verifies that an Android key event triggers a Compose key event. More detailed test
 * cases are present at [ProcessKeyInputTest].
 */
@SmallTest
@RunWith(Parameterized::class)
class AndroidProcessKeyInputTest(private val keyEventActions: List<Int>) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "keyEventAction = {0}")
        fun initParameters() = listOf(
            listOf(ActionDown),
            listOf(ActionDown, ActionUp)
        )
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
        var lastKeyConsumed = false
        rule.runOnIdle {
            keyEventActions.forEachIndexed { i, keyEventAction ->
                val thisKeyConsumed =
                    ownerView.dispatchKeyEvent(AndroidKeyEvent(keyEventAction, KeyCodeA))
                if (i == keyEventActions.lastIndex) {
                    lastKeyConsumed = thisKeyConsumed
                }
            }
        }

        // Assert.
        rule.runOnIdle {
            val keyEvent = checkNotNull(receivedKeyEvent)
            assertThat(keyEvent.type).isEqualTo(
                when (keyEventActions.last()) {
                    ActionUp -> KeyUp
                    ActionDown -> KeyDown
                    else -> error("No tests for this key action.")
                }
            )
            assertThat(keyEvent.key).isEqualTo(A)
            assertThat(lastKeyConsumed).isTrue()
        }
    }
}
