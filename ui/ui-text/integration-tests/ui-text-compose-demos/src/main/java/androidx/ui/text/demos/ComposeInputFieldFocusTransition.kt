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
import androidx.compose.foundation.ScrollableColumn
import androidx.ui.graphics.Color
import androidx.ui.input.ImeAction
import androidx.ui.input.TextFieldValue
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.ui.text.CoreTextField
import androidx.ui.text.TextStyle
import androidx.ui.unit.sp

@Composable
fun TextFieldFocusTransition() {
    val focusModifiers = List(6) {
        // TODO(b/161297615): Replace FocusModifier with Modifier.focus()
        @Suppress("DEPRECATION")
        FocusModifier()
    }

    ScrollableColumn {
        TextFieldWithFocusId(focusModifiers[0], focusModifiers[1])
        TextFieldWithFocusId(focusModifiers[1], focusModifiers[2])
        TextFieldWithFocusId(focusModifiers[2], focusModifiers[3])
        TextFieldWithFocusId(focusModifiers[3], focusModifiers[4])
        TextFieldWithFocusId(focusModifiers[4], focusModifiers[5])
        TextFieldWithFocusId(focusModifiers[5], focusModifiers[0])
    }
}

// TODO(b/161297615): Replace the deprecated FocusModifier with the new Focus API.
@Suppress("DEPRECATION")
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
    CoreTextField(
        value = state.value,
        modifier = focusModifier,
        textStyle = TextStyle(color = color, fontSize = 32.sp),
        onValueChange = {
            state.value = it
        },
        onFocusChanged = { focused.value = it },
        imeAction = ImeAction.Next,
        onImeActionPerformed = {
            if (it == ImeAction.Next)
                nextFocusModifier.requestFocus()
        }
    )
}
