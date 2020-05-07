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

package androidx.ui.core.focus

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue

internal class FocusModifierImpl(
    focusDetailedState: FocusDetailedState,
    var focusNode: ModifiedFocusNode? = null
) : FocusModifier {

    override var focusDetailedState: FocusDetailedState by mutableStateOf(focusDetailedState)
    var focusedChild: ModifiedFocusNode? = null

    override fun requestFocus() {
        val focusNode = focusNode
        requireNotNull(focusNode)
        focusNode.requestFocus()
    }

    override fun captureFocus(): Boolean {
        val focusNode = focusNode
        requireNotNull(focusNode)
        return focusNode.captureFocus()
    }

    override fun freeFocus(): Boolean {
        val focusNode = focusNode
        requireNotNull(focusNode)
        return focusNode.freeFocus()
    }
}
