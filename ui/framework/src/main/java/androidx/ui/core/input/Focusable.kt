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

package androidx.ui.core.input

import androidx.ui.core.FocusManagerAmbient
import androidx.ui.core.gesture.PressGestureDetector
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.unaryPlus

/**
 * A composable for focusable component
 *
 * Focused composable can be a target of text input or keyboard event. For example, Button widget
 * can be a Focusable for accepting SPACE key event for performing click event by SPACE key.
 *
 * TODO(nona): Make Focusable to controlled component?
 */
@Composable
internal fun Focusable(
    /** A callback that is executed when this component gained the focus */
    onFocus: () -> Unit = {},

    /** A callback that is executed when this component is about to lose the focus */
    onBlur: () -> Unit = {},

    @Children children: @Composable() () -> Unit
) {
    val focusManager = +ambient(FocusManagerAmbient)

    PressGestureDetector(onPress = {
        focusManager.requestFocus(object : FocusManager.FocusNode {
            override fun onFocus() {
                onFocus()
            }

            override fun onBlur() {
                onBlur()
            }
        })
    }) {
        children()
    }

    // TODO(nona): Implement focus transitions other than onPress event, e.g. TAB key events.
}