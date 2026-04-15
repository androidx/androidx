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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer.ChangeList
import androidx.compose.ui.isMacOs
import androidx.compose.ui.sendCharTypedEvents
import androidx.compose.ui.sendInputMethodEvent
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.sendKeyTypedEvent
import androidx.compose.ui.sendPressAndReleaseKeyEvents
import androidx.compose.ui.text.TextRange
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.KEY_PRESSED
import java.awt.event.KeyEvent.KEY_RELEASED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

/**
 * Tests for emulate input to the native window on various systems.
 *
 * Events were captured on each system via logging.
 * All tests can run on all OSes.
 * The OS names in test names just represent a unique order of input events on these OSes.
 */
@RunWith(Theories::class)
class WindowTypeTest : BaseWindowTextFieldTest() {
    @Theory
    internal fun `q, w, space, backspace 4x (English)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "English") {
        // q
        window.sendKeyEvent(81, 'q', KEY_PRESSED)
        window.sendKeyTypedEvent('q')
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(87, 'w', KEY_PRESSED)
        window.sendKeyTypedEvent('w')
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("qw", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("qw ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("qw", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 4x (Russian)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Russian") {
        // q
        window.sendKeyEvent(81, 'й', KEY_PRESSED)
        window.sendKeyTypedEvent('й')
        window.sendKeyEvent(81, 'й', KEY_RELEASED)
        assertStateEquals("й", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(87, 'ц', KEY_PRESSED)
        window.sendKeyTypedEvent('ц')
        window.sendKeyEvent(87, 'ц', KEY_RELEASED)
        assertStateEquals("йц", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("йц ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("йц", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("й", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `f, g, space, backspace 4x (Arabic)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Arabic") {
        // q
        window.sendKeyEvent(70, 'ب', KEY_PRESSED)
        window.sendKeyTypedEvent('ب')
        window.sendKeyEvent(70, 'ب', KEY_RELEASED)
        assertStateEquals("ب", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(71, 'ل', KEY_PRESSED)
        window.sendKeyTypedEvent('ل')
        window.sendKeyEvent(71, 'ل', KEY_RELEASED)
        assertStateEquals("بل", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("بل ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("بل", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ب", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 4x (Korean, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Windows") {
        // q
        window.sendInputMethodEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("ㅂ", 1)
        window.sendInputMethodEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputMethodEvent(null, 0)
        window.sendKeyTypedEvent('ㅈ')
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, backspace 3x (Korean, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Windows") {
        // q
        window.sendInputMethodEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("ㅂ", 1)
        window.sendInputMethodEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // backspace
        window.sendInputMethodEvent(null, 0)
        window.sendInputMethodEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `f, g, space, backspace 3x (Korean, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Windows") {
        // f
        window.sendInputMethodEvent("ㄹ", 0)
        window.sendKeyEvent(81, 'f', KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // g
        window.sendInputMethodEvent("ㅀ", 0)
        window.sendKeyEvent(87, 'g', KEY_RELEASED)
        assertStateEquals("ㅀ", selection = TextRange(1), composition = TextRange(0, 1))

        // space
        window.sendInputMethodEvent(null, 0)
        window.sendKeyTypedEvent('ㅀ')
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("ㅀ ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅀ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `f, g, backspace 2x (Korean, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Windows") {
        // f
        window.sendInputMethodEvent("ㄹ", 0)
        window.sendKeyEvent(81, 'f', KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // g
        window.sendInputMethodEvent("ㅀ", 0)
        window.sendKeyEvent(87, 'g', KEY_RELEASED)
        assertStateEquals("ㅀ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputMethodEvent("ㄹ", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputMethodEvent(null, 0)
        window.sendInputMethodEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 4x (Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, macOS") {
        // q
        window.sendInputMethodEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("ㅂ", 0)
        window.sendInputMethodEvent("ㅂ", 1)
        window.sendInputMethodEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputMethodEvent("ㅈ ", 0)
        window.sendInputMethodEvent("ㅈ ", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, backspace 3x (Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, macOS") {
        // q
        window.sendInputMethodEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("ㅂ", 0)
        window.sendInputMethodEvent("ㅂ", 1)
        window.sendInputMethodEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // backspace
        window.sendInputMethodEvent("ㅈ", 0)
        window.sendInputMethodEvent("ㅈ", 1)
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    // f, g on macOS prints 2 separate symbols (comparing to Windows), so we test t + y
    @Theory
    internal fun `t, y, space, backspace 3x (Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, macOS") {
        // t
        window.sendInputMethodEvent("ㅅ", 0)
        window.sendKeyEvent(84, 'ㅅ', KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // y
        window.sendInputMethodEvent("쇼", 0)
        window.sendKeyEvent(89, 'ㅛ', KEY_RELEASED)
        assertStateEquals("쇼", selection = TextRange(1), composition = TextRange(0, 1))

        // space
        window.sendInputMethodEvent("쇼 ", 0)
        window.sendInputMethodEvent("쇼 ", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("쇼 ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("쇼", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `t, y, backspace 2x (Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, macOS") {
        // t
        window.sendInputMethodEvent("ㅅ", 0)
        window.sendKeyEvent(84, 'ㅅ', KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // y
        window.sendInputMethodEvent("쇼", 0)
        window.sendKeyEvent(89, 'ㅛ', KEY_RELEASED)
        assertStateEquals("쇼", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputMethodEvent("ㅅ", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputMethodEvent("ㅅ", 0)
        window.sendInputMethodEvent("ㅅ", 1)
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 4x (Korean, Linux)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Linux") {
        // q
        window.sendInputMethodEvent("ㅂ", 0)
        window.sendKeyEvent(0, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent(null, 0)
        window.sendInputMethodEvent("ㅂ", 1)
        window.sendInputMethodEvent("ㅈ", 0)
        window.sendKeyEvent(0, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputMethodEvent(null, 0)
        window.sendInputMethodEvent("ㅈ", 1)
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 3x (Chinese, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, Windows") {
        // q
        window.sendInputMethodEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputMethodEvent("請問", 2)
        window.sendInputMethodEvent(null, 0)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("請問", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("請", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, backspace 3x (Chinese, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, Windows") {
        // q
        window.sendInputMethodEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // backspace
        window.sendInputMethodEvent("q", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputMethodEvent(null, 0)
        window.sendInputMethodEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 3x (Chinese, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, macOS") {
        // q
        window.sendInputMethodEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("q w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputMethodEvent("请问", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("请问", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("请", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, backspace 3x (Chinese, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, macOS") {
        // q
        window.sendInputMethodEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("q w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q w", selection = TextRange(3), composition = TextRange(0, 3))

        // backspace
        window.sendInputMethodEvent("q", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputMethodEvent("", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    // Verifies the fix to https://youtrack.jetbrains.com/issue/CMP-8184
    @Theory
    internal fun `q, w, space, backspace 2x, q, w, space (Chinese, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, macOS") {
        if (!isMacOs) return@runTextFieldTest  // Assume.assumeTrue doesn't work with @Theory

        // q
        window.sendInputMethodEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputMethodEvent("請問", 2)
        window.sendInputMethodEvent(null, 0)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("請問", selection = TextRange(2), composition = null)

        // backspace
        window.sendCharTypedEvents(Char(8), triggerAccentedInputHack = true)
        assertStateEquals("請", selection = TextRange(1), composition = null)

        // backspace
        window.sendCharTypedEvents(Char(8), triggerAccentedInputHack = true)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // q
        window.sendInputMethodEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputMethodEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputMethodEvent("請問", 2)
        window.sendInputMethodEvent(null, 0)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("請問", selection = TextRange(2), composition = null)
    }

    // Verifies the fix to https://youtrack.jetbrains.com/issue/CMP-8200
    @Theory
    internal fun `v, x, click before v, x, click at end(Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, macOS") {
        // Press 'v' key
        window.sendKeyTypedEvent('v')
        assertStateEquals("v", selection = TextRange(1), composition = null)

        // Press 'x' key (produces 'ㅌ')
        window.sendInputMethodEvent("ㅌ", 0)
        window.sendKeyEvent(88, 'ㅌ', KEY_RELEASED)
        assertStateEquals("vㅌ", selection = TextRange(2), composition = TextRange(1, 2))

        clickBeforeIndex(0)
        awaitIdle()
        // Here PlatformComponent.endComposition() should be called, and in response the
        // system should send a composition-ending event
        window.sendInputMethodEvent("ㅌ", 1)
        assertStateEquals("vㅌ", selection = TextRange(0), composition = null)

        // Press 'x' key (produces 'ㅌ')
        window.sendInputMethodEvent("ㅌ", 0)
        window.sendKeyEvent(88, 'ㅌ', KEY_RELEASED)
        assertStateEquals("ㅌvㅌ", selection = TextRange(1), composition = TextRange(0, 1))

        clickBeforeIndex(3)
        awaitIdle()
        window.sendInputMethodEvent("ㅌ", 1)
        assertStateEquals("ㅌvㅌ", selection = TextRange(3), composition = null)
    }

    @Theory
    internal fun `q, w, space (Chinese Wubi, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese Wubi, macOS") {
        // Wubi input method sends each composing InputMethodEvent twice for some reason
        suspend fun sendInputMethodEventTwice(text: String?, committedCharacterCount: Int = 0) {
            repeat(2) {
                window.sendInputMethodEvent(text, committedCharacterCount)
                awaitIdle()  // Wait for recomposition
            }
        }

        // q
        sendInputMethodEventTwice("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        sendInputMethodEventTwice("qw", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("qw", selection = TextRange(2), composition = TextRange(0, 2))

        // space
        window.sendInputMethodEvent("欠", 1)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("欠", selection = TextRange(1), composition = null)
    }

    @Theory
    internal fun `1, n, i, space (Chinese - Pinyin Simplified , macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese Wubi, macOS") {
        // 1
        window.sendCharTypedEvents('1', triggerAccentedInputHack = true)

        // n
        window.sendInputMethodEvent("n", 0)
        window.sendKeyEvent(78, 'n', KEY_RELEASED)

        // i
        window.sendInputMethodEvent("ni", 0)
        window.sendKeyEvent(73, 'i', KEY_RELEASED)

        // space
        window.sendInputMethodEvent("你", 1)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)

        assertStateEquals("1你", selection = TextRange(2), composition = null)
    }

    // Verifies that each typed character and character replaced by the input service is reported
    // (to `inputTransformation`) as a change in the last character only.
    // The behavior of `SecureTextField` with `TextObfuscationMode.RevealLastTyped` depends on this,
    // but since we don't have direct access to `visualText`, we're checking the correctness of the
    // changes, which `TextObfuscationMode.RevealLastTyped` depends on.
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    internal fun changesVisibleInInputTransformation() = runTextFieldTest(
        textFieldKind = TextField2,
        name = "Changes, Chinese, macOS"
    ) {
        var lastChanges: ChangeList? = null
        inputTransformation = InputTransformation {
            lastChanges = changes
        }

        awaitIdle()

        // c
        window.sendInputMethodEvent("c", 0)
        window.sendKeyEvent(67, 'c', KEY_RELEASED)
        assertStateEquals("c", selection = TextRange(1), composition = TextRange(0, 1))
        lastChanges.let {
            assertNotNull(it)
            assertEquals(1, it.changeCount)
            assertEquals(TextRange(0, 1), it.getRange(0))
            assertEquals(TextRange(0, 0), it.getOriginalRange(0))
        }

        // space
        window.sendInputMethodEvent("才", 1)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("才", selection = TextRange(1), composition = null)
        lastChanges.let {
            assertNotNull(it)
            assertEquals(1, it.changeCount)
            assertEquals(TextRange(0, 1), it.getRange(0))
            assertEquals(TextRange(0, 1), it.getOriginalRange(0))
        }

        // c
        window.sendInputMethodEvent("c", 0)
        window.sendKeyEvent(67, 'c', KEY_RELEASED)
        assertStateEquals("才c", selection = TextRange(2), composition = TextRange(1, 2))
        lastChanges.let {
            assertNotNull(it)
            assertEquals(1, it.changeCount)
            assertEquals(TextRange(1, 2), it.getRange(0))
            assertEquals(TextRange(1, 1), it.getOriginalRange(0))
        }

        // space
        window.sendInputMethodEvent("才", 1)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("才才", selection = TextRange(2), composition = null)
        lastChanges.let {
            assertNotNull(it)
            assertEquals(1, it.changeCount)
            assertEquals(TextRange(1, 2), it.getRange(0))
            assertEquals(TextRange(1, 2), it.getOriginalRange(0))
        }
    }

    @Theory
    internal fun macOsAccentedCharacterByLongPressInput(textFieldKind: TextFieldKind<*>) =
        runTextFieldTest(
            name = "ç, macOS",
            textFieldKind = textFieldKind,
        ) {
            if (!isMacOs) return@runTextFieldTest  // Assume.assumeTrue doesn't work with @Theory

            window.sendCharTypedEvents('c', triggerAccentedInputHack = true)
            assertStateEquals("c", selection = TextRange(1), composition = null)

            window.sendInputMethodEvent("ç", 1)
            assertStateEquals("ç", selection = TextRange(1), composition = null)
        }

    @Theory
    internal fun committedTextEventSentImmediatelyCommitsText(textFieldKind: TextFieldKind<*>) =
        runTextFieldTest(
            name = "first InputMethodEvent commits text",
            textFieldKind = textFieldKind,
        ) {
            window.sendInputMethodEvent("·", committedCharacterCount = 1)
            assertStateEquals("·", selection = TextRange(1), composition = null)
        }

    @Theory
    internal fun `select text backwards, then input via IME`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(
        textFieldKind = textFieldKind,
        name = "Select text backwards, then input via IME",
        initialText = "abcdef",
        initialSelection = TextRange(6)
    ) {
        // Select "def"
        window.sendKeyEvent(KeyEvent.VK_SHIFT, id = KEY_PRESSED)
        repeat(3) {
            window.sendPressAndReleaseKeyEvents(KeyEvent.VK_LEFT, modifiers = KeyEvent.SHIFT_DOWN_MASK)
            awaitIdle()
        }

        assertStateEquals("abcdef", selection = TextRange(6, 3), composition = null)

        // Insert character via IME
        window.sendInputMethodEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)

        assertStateEquals("abcㅂ", selection = TextRange(4, 4), composition = TextRange(3, 4))
    }
}
