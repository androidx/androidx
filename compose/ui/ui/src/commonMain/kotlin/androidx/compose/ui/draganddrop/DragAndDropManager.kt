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

package androidx.compose.ui.draganddrop

import androidx.compose.ui.geometry.Offset

internal interface DragAndDropManager {

    /**
     * Returns a boolean value indicating whether requesting drag and drop transfer is supported. If
     * it's not, the transfer might be initiated only be system and calling
     * [requestDragAndDropTransfer] will throw an error.
     */
    val isRequestDragAndDropTransferSupported: Boolean
        get() = false

    /**
     * Requests a drag and drop transfer. It might throw [UnsupportedOperationException] in case if
     * the operation is not supported. [isRequestDragAndDropTransferSupported] can be used to check
     * if it might be performed.
     */
    fun requestDragAndDropTransfer(node: DragAndDropNode, offset: Offset) {
        throw UnsupportedOperationException(
            "requestDragAndDropTransfer is not supported in the current environment. " +
                "A Drag & Drop transfer will be initiated by the platform itself"
        )
    }
}
