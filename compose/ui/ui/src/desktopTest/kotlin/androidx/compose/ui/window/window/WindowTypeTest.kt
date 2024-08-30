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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.sendInputEvent
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.sendKeyTypedEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowTestScope
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.event.KeyEvent.KEY_PRESSED
import java.awt.event.KeyEvent.KEY_RELEASED
import org.junit.experimental.theories.DataPoint
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
class WindowTypeTest {
    @Theory
    internal fun `q, w, space, backspace 4x (English)`(
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "English") {
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Russian") {
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Arabic") {
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, Windows") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent(null, 0)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, Windows") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, Windows") {
        // f
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(81, 'f', KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // g
        window.sendInputEvent("ㅀ", 0)
        window.sendKeyEvent(87, 'g', KEY_RELEASED)
        assertStateEquals("ㅀ", selection = TextRange(1), composition = TextRange(0, 1))

        // space
        window.sendInputEvent(null, 0)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, Windows") {
        // f
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(81, 'f', KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // g
        window.sendInputEvent("ㅀ", 0)
        window.sendKeyEvent(87, 'g', KEY_RELEASED)
        assertStateEquals("ㅀ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, macOS") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent("ㅈ ", 0)
        window.sendInputEvent("ㅈ ", 2)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, macOS") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // backspace
        window.sendInputEvent("ㅈ", 0)
        window.sendInputEvent("ㅈ", 1)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, macOS") {
        // t
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(84, 'ㅅ', KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // y
        window.sendInputEvent("쇼", 0)
        window.sendKeyEvent(89, 'ㅛ', KEY_RELEASED)
        assertStateEquals("쇼", selection = TextRange(1), composition = TextRange(0, 1))

        // space
        window.sendInputEvent("쇼 ", 0)
        window.sendInputEvent("쇼 ", 2)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, macOS") {
        // t
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(84, 'ㅅ', KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // y
        window.sendInputEvent("쇼", 0)
        window.sendKeyEvent(89, 'ㅛ', KEY_RELEASED)
        assertStateEquals("쇼", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㅅ", 0)
        window.sendInputEvent("ㅅ", 1)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Korean, Linux") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(0, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent(null, 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(0, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent(null, 0)
        window.sendInputEvent("ㅈ", 1)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Chinese, Windows") {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputEvent("請問", 2)
        window.sendInputEvent(null, 0)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Chinese, Windows") {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // backspace
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Chinese, macOS") {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputEvent("请问", 2)
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
        textFieldKind: TextFieldKind
    ) = runTypeTest(textFieldKind, "Chinese, macOS") {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q w", selection = TextRange(3), composition = TextRange(0, 3))

        // backspace
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    internal interface TypeTestScope {
        val windowTestScope: WindowTestScope
        val window: ComposeWindow
        val text: String

        @Composable
        fun TextField()

        suspend fun assertStateEquals(actual: String, selection: TextRange, composition: TextRange?)

    }

    private fun runTypeTest(
        textFieldKind: TextFieldKind = TextField1,
        name: String,
        body: suspend TypeTestScope.() -> Unit
    ) = runApplicationTest(
        hasAnimations = true,
        animationsDelayMillis = 100
    ) {
        var scope: TypeTestScope? = null
        launchTestApplication {
            Window(onCloseRequest = ::exitApplication) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (scope == null) {
                        scope = textFieldKind.createScope(this@runApplicationTest, window)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$name ($scope)")
                        Box(Modifier.border(1.dp, Color.Black).padding(8.dp)) {
                            scope!!.TextField()
                        }
                    }
                }
            }
        }

        awaitIdle()
        scope!!.body()
    }

    internal fun interface TextFieldKind {
        fun createScope(windowTestScope: WindowTestScope, window: ComposeWindow): TypeTestScope
    }

    companion object {
        @JvmField
        @DataPoint
        internal val TextField1: TextFieldKind = TextFieldKind { windowTestScope, window ->
            object : TypeTestScope {
                override val windowTestScope: WindowTestScope
                    get() = windowTestScope

                override val window: ComposeWindow
                    get() = window

                private var textFieldValue by mutableStateOf(TextFieldValue())

                override val text: String
                    get() = textFieldValue.text

                @Composable
                override fun TextField() {
                    val focusRequester = FocusRequester()
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier.focusRequester(focusRequester)
                    )

                    LaunchedEffect(focusRequester) {
                        focusRequester.requestFocus()
                    }
                }

                override suspend fun assertStateEquals(
                    actual: String,
                    selection: TextRange,
                    composition: TextRange?
                ) {
                    windowTestScope.awaitIdle()
                    assertThat(textFieldValue.text).isEqualTo(actual)
                    assertThat(textFieldValue.selection).isEqualTo(selection)
                    assertThat(textFieldValue.composition).isEqualTo(composition)
                }

                override fun toString() = "TextField1"
            }
        }

        @JvmField
        @DataPoint
        internal val TextField2: TextFieldKind = TextFieldKind { windowTestScope, window ->
            object : TypeTestScope {
                override val windowTestScope: WindowTestScope
                    get() = windowTestScope

                override val window: ComposeWindow
                    get() = window

                private val textFieldState = TextFieldState()

                override val text: String
                    get() = textFieldState.text.toString()

                @Composable
                override fun TextField() {
                    val focusRequester = FocusRequester()
                    BasicTextField(
                        state = textFieldState,
                        modifier = Modifier.focusRequester(focusRequester)
                    )

                    LaunchedEffect(focusRequester) {
                        focusRequester.requestFocus()
                    }
                }

                override suspend fun assertStateEquals(
                    actual: String,
                    selection: TextRange,
                    composition: TextRange?
                ) {
                    windowTestScope.awaitIdle()
                    assertThat(textFieldState.text.toString()).isEqualTo(actual)
                    assertThat(textFieldState.selection).isEqualTo(selection)
                    assertThat(textFieldState.composition).isEqualTo(composition)

                }

                override fun toString() = "TextField2"
            }
        }
    }
}
