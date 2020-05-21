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

package androidx.ui.text.demos

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.focus.FocusModifier
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.input.ImeAction
import androidx.ui.savedinstancestate.savedInstanceState
import androidx.ui.text.TextStyle
import androidx.ui.unit.sp

@Composable
fun TextFieldFocusTransition() {
    val focusModifiers = List(6) { FocusModifier() }

    VerticalScroller {
        TextFieldWithFocusId(focusModifiers[0], focusModifiers[1])
        TextFieldWithFocusId(focusModifiers[1], focusModifiers[2])
        TextFieldWithFocusId(focusModifiers[2], focusModifiers[3])
        TextFieldWithFocusId(focusModifiers[3], focusModifiers[4])
        TextFieldWithFocusId(focusModifiers[4], focusModifiers[5])
        TextFieldWithFocusId(focusModifiers[5], focusModifiers[0])
    }
}

@Composable
private fun TextFieldWithFocusId(focusModifier: FocusModifier, nextFocusModifier: FocusModifier) {
    val state = savedInstanceState(saver = TextFieldValue.Saver) {
        TextFieldValue("Focus Transition Test")
    }
    val focused = state { false }
    val color = if (focused.value) {
        Color.Red
    } else {
        Color.Black
    }
    TextField(
        value = state.value,
        modifier = focusModifier,
        textColor = color,
        textStyle = TextStyle(fontSize = 32.sp),
        onValueChange = {
            state.value = it
        },
        onFocusChange = { focused.value = it },
        imeAction = ImeAction.Next,
        onImeActionPerformed = {
            if (it == ImeAction.Next)
                nextFocusModifier.requestFocus()
        }
    )
}
