/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.window.window

import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focusedInputMethodRequests
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.sendKeyTypedEvent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.WindowTestScope
import androidx.compose.ui.window.runApplicationTest
import java.awt.event.KeyEvent.KEY_PRESSED
import java.awt.event.KeyEvent.KEY_RELEASED
import org.junit.Test

class WindowTypingLocationTest {
    @Test
    fun `input methods text location going right when type`() = runTextFieldTest {
        val location0 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        window.sendKeyEvent(81, 'a', KEY_PRESSED)
        window.sendKeyTypedEvent('a')
        window.sendKeyEvent(81, 'a', KEY_RELEASED)

        awaitIdle()
        val location1 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        window.sendKeyEvent(81, 'a', KEY_PRESSED)
        window.sendKeyTypedEvent('a')
        window.sendKeyEvent(81, 'a', KEY_RELEASED)
        awaitIdle()
        val location2 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        assert(location2.x > location1.x && location1.x > location0.x)
        // don't check location0.y, as it is different for empty text field
        assert(location2.y == location1.y)
    }

    @Test
    fun `input methods text location is inside window`() = runTextFieldTest {
        val windowLocation = window.contentPane.locationOnScreen
        val windowSize = window.contentPane.size

        val location0 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        window.sendKeyEvent(81, 'a', KEY_PRESSED)
        window.sendKeyTypedEvent('a')
        window.sendKeyEvent(81, 'a', KEY_RELEASED)
        awaitIdle()
        val location1 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        window.sendKeyEvent(81, 'a', KEY_PRESSED)
        window.sendKeyTypedEvent('a')
        window.sendKeyEvent(81, 'a', KEY_RELEASED)
        awaitIdle()
        val location2 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        assert(location0.x in windowLocation.x..windowLocation.x + windowSize.width)
        assert(location1.x in windowLocation.x..windowLocation.x + windowSize.width)
        assert(location2.x in windowLocation.x..windowLocation.x + windowSize.width)
        assert(location0.y in windowLocation.y..windowLocation.y + windowSize.height)
        assert(location1.y in windowLocation.y..windowLocation.y + windowSize.height)
        assert(location2.y in windowLocation.y..windowLocation.y + windowSize.height)
    }


    private fun runTextFieldTest(body: suspend WindowTestScope.() -> Unit) = runApplicationTest(
        hasAnimations = true,
        animationsDelayMillis = 100
    ) {
        launchTestWindowApplication {
            var text by remember { mutableStateOf(TextFieldValue()) }

            val focusRequester = FocusRequester()
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.focusRequester(focusRequester)
            )

            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }
        awaitIdle()
        body()
    }
}
