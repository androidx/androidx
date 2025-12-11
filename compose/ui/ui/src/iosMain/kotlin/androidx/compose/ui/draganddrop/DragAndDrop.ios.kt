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

package androidx.compose.ui.draganddrop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.toOffset
import platform.UIKit.UIDragItem
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.UIView

/**
 * A representation of an event sent by the platform during a drag and drop operation.
 */
actual class DragAndDropEvent internal constructor(
    private val dropSessionContext: DropSessionContext
) {
    internal val view: UIView
        get() = dropSessionContext.view

    internal val session: UIDropSessionProtocol
        get() = dropSessionContext.session

    @ExperimentalComposeUiApi
    val items: List<UIDragItem>
        get() = session.items.filterIsInstance<UIDragItem>()
}

/**
 * On iOS drag and drop session data is represented by [UIDragItem]s, which contains
 * information about how data can be transferred across processes boundaries and an optional
 * local object to be used in the same app.
 */
actual class DragAndDropTransferData
@ExperimentalComposeUiApi
constructor(
    @property:ExperimentalComposeUiApi
    val items: List<UIDragItem>
)

/**
 * Returns the position of this [DragAndDropEvent] relative to the root Compose View in the
 * layout hierarchy.
 */
internal actual val DragAndDropEvent.positionInRoot: Offset
    get() =
        session
            .locationInView(view)
            .asDpOffset()
            .toOffset(view.density)
