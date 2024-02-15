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

package androidx.compose.ui.node

import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.internal.checkPrecondition

/**
 * The Android [View] hosting the composition.
 *
 * @throws IllegalStateException If the modifier node is not [attached][Modifier.Node.isAttached].
 */
fun DelegatableNode.requireView(): View {
    checkPrecondition(node.isAttached) {
        "Cannot get View because the Modifier node is not currently attached."
    }
    return requireLayoutNode().requireOwner() as View
}
