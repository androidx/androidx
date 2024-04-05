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

package test.androidx.compose.foundation.text.selection

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.pressKey

internal interface KeyboardActions {
    fun KeyInjectionScope.selectAll()
    fun KeyInjectionScope.selectLineStart()
    fun KeyInjectionScope.selectTextStart()
    fun KeyInjectionScope.selectLineEnd()
    fun KeyInjectionScope.selectTextEnd()
    fun KeyInjectionScope.deleteAll()
}

internal object DefaultKeyboardActions : KeyboardActions {
    override fun KeyInjectionScope.selectAll() {
        keyDown(Key.CtrlLeft)
        pressKey(Key.A)
        keyUp(Key.CtrlLeft)
    }

    override fun KeyInjectionScope.selectLineStart() {
        keyDown(Key.ShiftLeft)
        pressKey(Key.MoveHome)
        keyUp(Key.ShiftLeft)
    }

    override fun KeyInjectionScope.selectTextStart() {
        keyDown(Key.CtrlLeft)
        keyDown(Key.ShiftLeft)
        pressKey(Key.MoveHome)
        keyUp(Key.ShiftLeft)
        keyUp(Key.CtrlLeft)
    }

    override fun KeyInjectionScope.selectLineEnd() {
        keyDown(Key.ShiftLeft)
        pressKey(Key.MoveEnd)
        keyUp(Key.ShiftLeft)
    }

    override fun KeyInjectionScope.selectTextEnd() {
        keyDown(Key.CtrlLeft)
        keyDown(Key.ShiftLeft)
        pressKey(Key.MoveEnd)
        keyUp(Key.ShiftLeft)
        keyUp(Key.CtrlLeft)
    }

    override fun KeyInjectionScope.deleteAll() {
        keyDown(Key.CtrlLeft)
        keyDown(Key.Backspace)
    }

    override fun toString() = "Win"
}

internal object MacosKeyboardActions : KeyboardActions {
    override fun KeyInjectionScope.selectAll() {
        keyDown(Key.MetaLeft)
        pressKey(Key.A)
        keyUp(Key.MetaLeft)
    }

    override fun KeyInjectionScope.selectLineStart() {
        keyDown(Key.ShiftLeft)
        keyDown(Key.MetaLeft)
        pressKey(Key.DirectionLeft)
        keyUp(Key.ShiftLeft)
        keyUp(Key.MetaLeft)
    }

    override fun KeyInjectionScope.selectTextStart() {
        keyDown(Key.ShiftLeft)
        pressKey(Key.Home)
        keyUp(Key.ShiftLeft)
    }

    override fun KeyInjectionScope.selectLineEnd() {
        keyDown(Key.ShiftLeft)
        keyDown(Key.MetaLeft)
        pressKey(Key.DirectionRight)
        keyUp(Key.ShiftLeft)
        keyUp(Key.MetaLeft)
    }

    override fun KeyInjectionScope.selectTextEnd() {
        keyDown(Key.ShiftLeft)
        pressKey(Key.MoveEnd)
        keyUp(Key.ShiftLeft)
    }

    override fun KeyInjectionScope.deleteAll() {
        keyDown(Key.MetaLeft)
        keyDown(Key.Delete)
    }

    override fun toString() = "MacOS"
}