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

package androidx.ui.core.focus

import androidx.ui.core.FocusNode

/**
 * Find the first ancestor that is a [FocusNode].
 */
internal fun FocusNode.findParentFocusNode(): FocusNode? {
    var focusableParent = parent
    while (focusableParent != null) {
        if (focusableParent is FocusNode) {
            return focusableParent
        } else {
            focusableParent = focusableParent.parent
        }
    }
    return null
}

internal fun FocusNode.ownerHasFocus(): Boolean {
    // TODO(b/144895515): Read the focus state from the owner.
    return true
}

internal fun FocusNode.requestFocusForOwner() {
    // TODO(b/144893832): Ask the owner to request focus.
}

/**
 * Checks the focus state of the [Owner][androidx.ui.core.Owner] and Initializes the focus state of
 * the node.
 *
 * Note: This function acts only on the root node. It is a no-op for other nodes.
 */
fun FocusNode.initializeFocusState() {
    if (findParentFocusNode() == null && ownerHasFocus()) {
        requestFocus()
    }
}