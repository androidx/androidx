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

package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

/** A platform implementation for drag and drop functionality. */
// TODO: Combine with [DragAndDropManager]
@InternalComposeUiApi
interface PlatformDragAndDropManager {

    /**
     * Returns a boolean value indicating whether requesting drag and drop transfer is required. If
     * it's not, the transfer might be initiated only be system and calling
     * [requestDragAndDropTransfer] will be ignored.
     */
    val isRequestDragAndDropTransferRequired: Boolean
        get() = false

    /**
     * Requests a drag and drop transfer. It might ignored in case if the operation performed by
     * system. [isRequestDragAndDropTransferRequired] can be used to check if it should be used
     * explicitly.
     */
    fun requestDragAndDropTransfer(source: PlatformDragAndDropSource, offset: Offset) {
        throw UnsupportedOperationException(
            "requestDragAndDropTransfer is not supported in the current environment. " +
                "A Drag & Drop transfer will be initiated by the platform itself"
        )
    }
}

// TODO: Combine with [DragAndDropStartTransferScope]
@InternalComposeUiApi
interface PlatformDragAndDropSource {
    /**
     * Initiates a drag-and-drop operation for transferring data.
     *
     * @param offset the offset value representing position of the input pointer.
     * @param isTransferStarted a lambda function that returns true if the drag-and-drop transfer
     *   has started, or false otherwise.
     */
    fun StartTransferScope.startDragAndDropTransfer(
        offset: Offset,
        isTransferStarted: () -> Boolean
    )

    /** A scope that allows starting a drag and drop session. */
    interface StartTransferScope {
        /**
         * Initiates a drag-and-drop operation for transferring data.
         *
         * @param transferData the data to be transferred after successful completion of the drag and
         *   drop gesture.
         * @param decorationSize the size of the drag decoration to be drawn.
         * @param drawDragDecoration provides the visual representation of the item dragged during the
         *   drag and drop gesture.
         * @return true if the method completes successfully, or false if it fails anywhere. Returning
         *   false means the system was unable to do a drag because of another ongoing operation or some
         *   other reasons.
         */
        fun startDragAndDropTransfer(
            transferData: DragAndDropTransferData,
            decorationSize: Size,
            drawDragDecoration: DrawScope.() -> Unit,
        ): Boolean
    }
}
