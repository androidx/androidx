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

package androidx.compose.ui.platform

import androidx.compose.ui.text.input.PlatformTextInputMethodRequest
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import java.awt.Frame
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.font.TextHitInfo
import java.awt.im.InputMethodRequests
import java.text.AttributedCharacterIterator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class DesktopTextInputSessionTest {

    @Test
    fun startInputMethod_setsAndClearsRequestsAndListeners() = runTest {
        val inputComponent = TestInputComponent()
        val component = Frame()
        val session = DesktopTextInputSession(
            coroutineScope = this,
            inputComponent = inputComponent,
            component = component
        )
        val request = TestInputMethodRequest()

        val sessionJob = launch {
            session.startInputMethod(request)
        }
        advanceUntilIdle()

        assertThat(inputComponent.inputMethodRequests).isSameInstanceAs(request)
        assertThat(component.inputMethodListeners).asList().containsExactly(request)

        sessionJob.cancelAndJoin()

        assertThat(inputComponent.inputMethodRequests).isNull()
        assertThat(component.inputMethodListeners).isEmpty()
    }

    private class TestInputComponent : PlatformInputComponent {
        var inputMethodRequests: InputMethodRequests? = null

        override fun enableInput(inputMethodRequests: InputMethodRequests) {
            this.inputMethodRequests = inputMethodRequests
        }

        override fun disableInput(inputMethodRequests: InputMethodRequests?) {
            this.inputMethodRequests = null
        }

        override suspend fun textInputSession(
            session: suspend PlatformTextInputSessionScope.() -> Nothing
        ): Nothing {
            throw AssertionError("not supported")
        }

        override val locationOnScreen: Point
            get() = throw AssertionError("not supported")
        override val density: Density
            get() = throw AssertionError("not supported")
    }

    private class TestInputMethodRequest : PlatformTextInputMethodRequest, InputMethodListener,
        InputMethodRequests {

        override val inputMethodListener: InputMethodListener
            get() = this
        override val inputMethodRequests: InputMethodRequests
            get() = this

        override fun inputMethodTextChanged(event: InputMethodEvent?) {
            throw AssertionError("not supported")
        }

        override fun caretPositionChanged(event: InputMethodEvent?) {
            throw AssertionError("not supported")
        }

        override fun getTextLocation(offset: TextHitInfo?): Rectangle {
            throw AssertionError("not supported")
        }

        override fun getLocationOffset(x: Int, y: Int): TextHitInfo? {
            throw AssertionError("not supported")
        }

        override fun getInsertPositionOffset(): Int {
            throw AssertionError("not supported")
        }

        override fun getCommittedText(
            beginIndex: Int,
            endIndex: Int,
            attributes: Array<out AttributedCharacterIterator.Attribute>?
        ): AttributedCharacterIterator {
            throw AssertionError("not supported")
        }

        override fun getCommittedTextLength(): Int {
            throw AssertionError("not supported")
        }

        override fun cancelLatestCommittedText(
            attributes: Array<out AttributedCharacterIterator.Attribute>?
        ): AttributedCharacterIterator? {
            throw AssertionError("not supported")
        }

        override fun getSelectedText(
            attributes: Array<out AttributedCharacterIterator.Attribute>?
        ): AttributedCharacterIterator? {
            throw AssertionError("not supported")
        }
    }
}