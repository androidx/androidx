/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.input

import androidx.compose.ui.events.keyDownEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.key.utf16CodePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import org.w3c.dom.events.KeyboardEvent

private fun KeyboardEvent.assertEquivalence(key: Key, codePoint: Int = key.keyCode.toInt()) {
    val keyEvent = toComposeEvent()
    assertEquals(actual = keyEvent.key, expected = key, message = "key doesn't match for ${this.key} / ${this.code}")
    assertEquals(actual = keyEvent.utf16CodePoint, expected = codePoint, message = "utf16CodePoint doesn't match for ${this.key} / ${this.code}")
}

class KeyEventConversionTests {

    @Test
    fun standardKeyboardLayout() {
        keyDownEvent("Escape", code = "Escape", keyCode = Key.Escape.keyCode.toInt()).assertEquivalence(key = Key.Escape)

        keyDownEvent("CapsLock", code = "CapsLock", keyCode = Key.CapsLock.keyCode.toInt()).assertEquivalence(key = Key.CapsLock)
        keyDownEvent("Tab", code = "Tab", keyCode = Key.Tab.keyCode.toInt()).assertEquivalence(key = Key.Tab)
        keyDownEvent("Enter", code = "Enter", keyCode = Key.Enter.keyCode.toInt()).assertEquivalence(key = Key.Enter)

        keyDownEvent("a", code = "KeyA").assertEquivalence(key = Key.A, codePoint = 97)
        keyDownEvent("b", code = "KeyB").assertEquivalence(key = Key.B, codePoint = 98)
        keyDownEvent("c", code = "KeyC").assertEquivalence(key = Key.C, codePoint = 99)
        keyDownEvent("d", code = "KeyD").assertEquivalence(key = Key.D, codePoint = 100)
        keyDownEvent("e", code = "KeyE").assertEquivalence(key = Key.E, codePoint = 101)
        keyDownEvent("f", code = "KeyF").assertEquivalence(key = Key.F, codePoint = 102)
        keyDownEvent("g", code = "KeyG").assertEquivalence(key = Key.G, codePoint = 103)
        keyDownEvent("h", code = "KeyH").assertEquivalence(key = Key.H, codePoint = 104)
        keyDownEvent("i", code = "KeyI").assertEquivalence(key = Key.I, codePoint = 105)
        keyDownEvent("j", code = "KeyJ").assertEquivalence(key = Key.J, codePoint = 106)
        keyDownEvent("k", code = "KeyK").assertEquivalence(key = Key.K, codePoint = 107)
        keyDownEvent("l", code = "KeyL").assertEquivalence(key = Key.L, codePoint = 108)
        keyDownEvent("m", code = "KeyM").assertEquivalence(key = Key.M, codePoint = 109)
        keyDownEvent("n", code = "KeyN").assertEquivalence(key = Key.N, codePoint = 110)
        keyDownEvent("o", code = "KeyO").assertEquivalence(key = Key.O, codePoint = 111)
        keyDownEvent("p", code = "KeyP").assertEquivalence(key = Key.P, codePoint = 112)
        keyDownEvent("q", code = "KeyQ").assertEquivalence(key = Key.Q, codePoint = 113)
        keyDownEvent("r", code = "KeyR").assertEquivalence(key = Key.R, codePoint = 114)
        keyDownEvent("s", code = "KeyS").assertEquivalence(key = Key.S, codePoint = 115)
        keyDownEvent("t", code = "KeyT").assertEquivalence(key = Key.T, codePoint = 116)
        keyDownEvent("u", code = "KeyU").assertEquivalence(key = Key.U, codePoint = 117)
        keyDownEvent("v", code = "KeyV").assertEquivalence(key = Key.V, codePoint = 118)
        keyDownEvent("w", code = "KeyW").assertEquivalence(key = Key.W, codePoint = 119)
        keyDownEvent("x", code = "KeyX").assertEquivalence(key = Key.X, codePoint = 120)
        keyDownEvent("y", code = "KeyY").assertEquivalence(key = Key.Y, codePoint = 121)
        keyDownEvent("z", code = "KeyZ").assertEquivalence(key = Key.Z, codePoint = 122)

        keyDownEvent("`", code = "Backquote", keyCode = Key.Grave.keyCode.toInt()).assertEquivalence(key = Key.Grave, codePoint = 96)
        keyDownEvent("0", code = "Digit0").assertEquivalence(key = Key.Zero)
        keyDownEvent("1", code = "Digit1").assertEquivalence(key = Key.One)
        keyDownEvent("2", code = "Digit2").assertEquivalence(key = Key.Two)
        keyDownEvent("3", code = "Digit3").assertEquivalence(key = Key.Three)
        keyDownEvent("4", code = "Digit4").assertEquivalence(key = Key.Four)
        keyDownEvent("5", code = "Digit5").assertEquivalence(key = Key.Five)
        keyDownEvent("6", code = "Digit6").assertEquivalence(key = Key.Six)
        keyDownEvent("7", code = "Digit7").assertEquivalence(key = Key.Seven)
        keyDownEvent("8", code = "Digit8").assertEquivalence(key = Key.Eight)
        keyDownEvent("9", code = "Digit9").assertEquivalence(key = Key.Nine)

        keyDownEvent("0", code = "Numpad0").assertEquivalence(key = Key.NumPad0, codePoint = 48)
        keyDownEvent("1", code = "Numpad1").assertEquivalence(key = Key.NumPad1, codePoint = 49)
        keyDownEvent("2", code = "Numpad2").assertEquivalence(key = Key.NumPad2, codePoint = 50)
        keyDownEvent("3", code = "Numpad3").assertEquivalence(key = Key.NumPad3, codePoint = 51)
        keyDownEvent("4", code = "Numpad4").assertEquivalence(key = Key.NumPad4, codePoint = 52)
        keyDownEvent("5", code = "Numpad5").assertEquivalence(key = Key.NumPad5, codePoint = 53)
        keyDownEvent("6", code = "Numpad6").assertEquivalence(key = Key.NumPad6, codePoint = 54)
        keyDownEvent("7", code = "Numpad7").assertEquivalence(key = Key.NumPad7, codePoint = 55)
        keyDownEvent("8", code = "Numpad8").assertEquivalence(key = Key.NumPad8, codePoint = 56)
        keyDownEvent("9", code = "Numpad9").assertEquivalence(key = Key.NumPad9, codePoint = 57)

        keyDownEvent("=", code = "NumpadEqual", keyCode = Key.NumPadEquals.keyCode.toInt()).assertEquivalence(key = Key.NumPadEquals, codePoint = 61)
        keyDownEvent("/", code = "NumpadDivide", keyCode = Key.NumPadDivide.keyCode.toInt()).assertEquivalence(key = Key.NumPadDivide, codePoint = 47)
        keyDownEvent("*", code = "NumpadMultiply", keyCode = Key.NumPadMultiply.keyCode.toInt()).assertEquivalence(key = Key.NumPadMultiply, codePoint = 42)
        keyDownEvent("-", code = "NumpadSubtract", keyCode = Key.NumPadSubtract.keyCode.toInt()).assertEquivalence(key = Key.NumPadSubtract, codePoint = 45)
        keyDownEvent("+", code = "NumpadAdd", keyCode = Key.NumPadAdd.keyCode.toInt()).assertEquivalence(key = Key.NumPadAdd, codePoint = 43)

        //TODO: Situation with NumpadEnter is not clear so far keyDownEvent("Enter", code = "NumpadEnter").assertEquivalence(key = Key.NumPadEnter, codePoint = 13)
        keyDownEvent(".", code = "NumpadDecimal", keyCode = Key.NumPadDot.keyCode.toInt()).assertEquivalence(key = Key.NumPadDot, codePoint = 46)

        keyDownEvent("Backspace", code = "Backspace", keyCode = Key.Backspace.keyCode.toInt()).assertEquivalence(key = Key.Backspace)
        keyDownEvent("Delete", code = "Delete", keyCode = Key.Delete.keyCode.toInt()).assertEquivalence(key = Key.Delete)

        keyDownEvent("[", code = "BracketLeft", keyCode = Key.LeftBracket.keyCode.toInt()).assertEquivalence(key = Key.LeftBracket, codePoint = 91)
        keyDownEvent("]", code = "BracketRight", keyCode = Key.RightBracket.keyCode.toInt()).assertEquivalence(key = Key.RightBracket, codePoint = 93)
        keyDownEvent("\\", code = "Backslash",  keyCode = Key.Backslash.keyCode.toInt()).assertEquivalence(key = Key.Backslash, codePoint = 92)

        keyDownEvent("Home", code = "Home", keyCode = Key.MoveHome.keyCode.toInt()).assertEquivalence(key = Key.MoveHome)
        keyDownEvent("End", code = "End", keyCode = Key.MoveEnd.keyCode.toInt()).assertEquivalence(key = Key.MoveEnd)
        keyDownEvent("PageUp", code = "PageUp", keyCode = Key.PageUp.keyCode.toInt()).assertEquivalence(key = Key.PageUp)
        keyDownEvent("PageDown", code = "PageDown", keyCode = Key.PageDown.keyCode.toInt()).assertEquivalence(key = Key.PageDown)


        keyDownEvent("ShiftLeft", code = "ShiftLeft", keyCode = Key.ShiftLeft.keyCode.toInt()).assertEquivalence(key = Key.ShiftLeft)
        keyDownEvent("ShiftRight", code = "ShiftRight", keyCode = Key.ShiftRight.keyCode.toInt()).assertEquivalence(key = Key.ShiftRight)

        keyDownEvent("Control", code = "ControlLeft", keyCode = Key.CtrlLeft.keyCode.toInt()).assertEquivalence(key = Key.CtrlLeft)
        keyDownEvent("Control", code = "ControlRight", keyCode = Key.CtrlRight.keyCode.toInt()).assertEquivalence(key = Key.CtrlRight)

        keyDownEvent("Meta", code = "MetaLeft", keyCode = Key.MetaLeft.keyCode.toInt()).assertEquivalence(key = Key.MetaLeft)
        keyDownEvent("Meta", code = "MetaRight", keyCode = Key.MetaRight.keyCode.toInt()).assertEquivalence(key = Key.MetaRight)

        keyDownEvent("-", code = "Minus", keyCode = Key.Minus.keyCode.toInt()).assertEquivalence(key = Key.Minus, codePoint = 45)
        keyDownEvent("=", code = "Equal", keyCode = Key.Equals.keyCode.toInt()).assertEquivalence(key = Key.Equals, codePoint = 61)
        keyDownEvent(";", code = "Semicolon", keyCode = Key.Semicolon.keyCode.toInt()).assertEquivalence(key = Key.Semicolon, codePoint = 59)

        keyDownEvent(",", code = "Comma", keyCode = Key.Comma.keyCode.toInt()).assertEquivalence(key = Key.Comma, codePoint = 44)
        keyDownEvent(".", code = "Period", keyCode = Key.Period.keyCode.toInt()).assertEquivalence(key = Key.Period, codePoint = 46)
        keyDownEvent("/", code = "Slash", keyCode = Key.Slash.keyCode.toInt()).assertEquivalence(key = Key.Slash, codePoint = 47)

        keyDownEvent("Alt", code = "AltLeft", keyCode = Key.AltLeft.keyCode.toInt()).assertEquivalence(key = Key.AltLeft)
        keyDownEvent("Alt", code = "AltRight", keyCode = Key.AltRight.keyCode.toInt()).assertEquivalence(key = Key.AltRight)

        keyDownEvent("ArrowUp", code = "ArrowUp", keyCode = Key.DirectionUp.keyCode.toInt()).assertEquivalence(key = Key.DirectionUp)
        keyDownEvent("ArrowRight", code = "ArrowRight", keyCode = Key.DirectionRight.keyCode.toInt()).assertEquivalence(key = Key.DirectionRight)
        keyDownEvent("ArrowDown", code = "ArrowDown", keyCode = Key.DirectionDown.keyCode.toInt()).assertEquivalence(key = Key.DirectionDown)
        keyDownEvent("ArrowLeft", code = "ArrowLeft", keyCode = Key.DirectionLeft.keyCode.toInt()).assertEquivalence(key = Key.DirectionLeft)

        keyDownEvent("CapsLock", code = "CapsLock", keyCode = Key.CapsLock.keyCode.toInt()).assertEquivalence(key = Key.CapsLock)

        keyDownEvent("F1", code = "F1", keyCode = Key.F1.keyCode.toInt()).assertEquivalence(key = Key.F1)
        keyDownEvent("F2", code = "F2", keyCode = Key.F2.keyCode.toInt()).assertEquivalence(key = Key.F2)
        keyDownEvent("F3", code = "F3", keyCode = Key.F3.keyCode.toInt()).assertEquivalence(key = Key.F3)
        keyDownEvent("F4", code = "F4", keyCode = Key.F4.keyCode.toInt()).assertEquivalence(key = Key.F4)
        keyDownEvent("F5", code = "F5", keyCode = Key.F5.keyCode.toInt()).assertEquivalence(key = Key.F5)
        keyDownEvent("F6", code = "F6", keyCode = Key.F6.keyCode.toInt()).assertEquivalence(key = Key.F6)
        keyDownEvent("F7", code = "F7", keyCode = Key.F7.keyCode.toInt()).assertEquivalence(key = Key.F7)
        keyDownEvent("F8", code = "F8", keyCode = Key.F8.keyCode.toInt()).assertEquivalence(key = Key.F8)
        keyDownEvent("F9", code = "F9", keyCode = Key.F9.keyCode.toInt()).assertEquivalence(key = Key.F9)
        keyDownEvent("F10", code = "F10", keyCode = Key.F10.keyCode.toInt()).assertEquivalence(key = Key.F10)
        keyDownEvent("F11", code = "F11", keyCode = Key.F11.keyCode.toInt()).assertEquivalence(key = Key.F11)
        keyDownEvent("F12", code = "F12", keyCode = Key.F12.keyCode.toInt()).assertEquivalence(key = Key.F12)

        keyDownEvent("", code = "Space", keyCode = Key.Spacebar.keyCode.toInt()).assertEquivalence(key = Key.Spacebar)
    }

    @Test
    fun standardKeyboardLayoutUpper() {
        keyDownEvent("A", code = "KeyA").assertEquivalence(key = Key.A)
        keyDownEvent("B", code = "KeyB").assertEquivalence(key = Key.B)
        keyDownEvent("C", code = "KeyC").assertEquivalence(key = Key.C)
        keyDownEvent("D", code = "KeyD").assertEquivalence(key = Key.D)
        keyDownEvent("E", code = "KeyE").assertEquivalence(key = Key.E)
        keyDownEvent("F", code = "KeyF").assertEquivalence(key = Key.F)
        keyDownEvent("G", code = "KeyG").assertEquivalence(key = Key.G)
        keyDownEvent("H", code = "KeyH").assertEquivalence(key = Key.H)
        keyDownEvent("I", code = "KeyI").assertEquivalence(key = Key.I)
        keyDownEvent("J", code = "KeyJ").assertEquivalence(key = Key.J)
        keyDownEvent("K", code = "KeyK").assertEquivalence(key = Key.K)
        keyDownEvent("L", code = "KeyL").assertEquivalence(key = Key.L)
        keyDownEvent("M", code = "KeyM").assertEquivalence(key = Key.M)
        keyDownEvent("N", code = "KeyN").assertEquivalence(key = Key.N)
        keyDownEvent("O", code = "KeyO").assertEquivalence(key = Key.O)
        keyDownEvent("P", code = "KeyP").assertEquivalence(key = Key.P)
        keyDownEvent("Q", code = "KeyQ").assertEquivalence(key = Key.Q)
        keyDownEvent("R", code = "KeyR").assertEquivalence(key = Key.R)
        keyDownEvent("S", code = "KeyS").assertEquivalence(key = Key.S)
        keyDownEvent("T", code = "KeyT").assertEquivalence(key = Key.T)
        keyDownEvent("U", code = "KeyU").assertEquivalence(key = Key.U)
        keyDownEvent("V", code = "KeyV").assertEquivalence(key = Key.V)
        keyDownEvent("W", code = "KeyW").assertEquivalence(key = Key.W)
        keyDownEvent("X", code = "KeyX").assertEquivalence(key = Key.X)
        keyDownEvent("Y", code = "KeyY").assertEquivalence(key = Key.Y)
        keyDownEvent("Z", code = "KeyZ").assertEquivalence(key = Key.Z)

        keyDownEvent("~", code = "Backquote", keyCode = Key.Grave.keyCode.toInt()).assertEquivalence(key = Key.Grave, codePoint = 126)
        keyDownEvent(")", code = "Digit0", keyCode = Key.Zero.keyCode.toInt()).assertEquivalence(key = Key.Zero, codePoint = 41)
        keyDownEvent("!", code = "Digit1", keyCode = Key.One.keyCode.toInt()).assertEquivalence(key = Key.One, codePoint = 33)
        keyDownEvent("@", code = "Digit2", keyCode = Key.Two.keyCode.toInt()).assertEquivalence(key = Key.Two, codePoint = 64)
        keyDownEvent("#", code = "Digit3", keyCode = Key.Three.keyCode.toInt()).assertEquivalence(key = Key.Three, codePoint = 35)
        keyDownEvent("$", code = "Digit4", keyCode = Key.Four.keyCode.toInt()).assertEquivalence(key = Key.Four, codePoint = 36)
        keyDownEvent("%", code = "Digit5", keyCode = Key.Five.keyCode.toInt()).assertEquivalence(key = Key.Five, codePoint = 37)
        keyDownEvent("^", code = "Digit6", keyCode = Key.Six.keyCode.toInt()).assertEquivalence(key = Key.Six, codePoint = 94)
        keyDownEvent("&", code = "Digit7", keyCode = Key.Seven.keyCode.toInt()).assertEquivalence(key = Key.Seven, codePoint = 38)
        keyDownEvent("*", code = "Digit8", keyCode = Key.Eight.keyCode.toInt()).assertEquivalence(key = Key.Eight, codePoint = 42)
        keyDownEvent("(", code = "Digit9", keyCode = Key.Nine.keyCode.toInt()).assertEquivalence(key = Key.Nine, codePoint = 40)
        keyDownEvent("_", code = "Minus", keyCode = Key.Minus.keyCode.toInt()).assertEquivalence(key = Key.Minus, codePoint = 95)
        keyDownEvent("+", code = "Equal", keyCode = Key.Equals.keyCode.toInt()).assertEquivalence(key = Key.Equals, codePoint = 43)
    }

    @Test
    fun standardVirtualKeyboardLayout() {
        // Virtual keyboard generates actual keyboard events for some of the keys pressed
        // This keyboard events, however, actually differ - the code is always "" while key contains the value that we need
        keyDownEvent("ArrowRight", code = "", keyCode = Key.DirectionRight.keyCode.toInt()).assertEquivalence(key = Key.DirectionRight)
        keyDownEvent("ArrowLeft", code = "", keyCode = Key.DirectionLeft.keyCode.toInt()).assertEquivalence(key = Key.DirectionLeft)
        keyDownEvent("Delete", code = "", keyCode = Key.Delete.keyCode.toInt()).assertEquivalence(key = Key.Delete)
        keyDownEvent("Backspace", code = "", keyCode = Key.Backspace.keyCode.toInt()).assertEquivalence(key = Key.Backspace)
        keyDownEvent("Enter", code = "", keyCode = Key.Enter.keyCode.toInt()).assertEquivalence(key = Key.Enter)
    }

}
