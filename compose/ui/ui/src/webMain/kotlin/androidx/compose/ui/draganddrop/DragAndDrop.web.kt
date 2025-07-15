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

/**
 * A representation of an event sent by the platform during a drag and drop operation.
 */
actual class DragAndDropEvent internal constructor(
    internal val offset: Offset,

    @property:ExperimentalComposeUiApi
    val transferData: DragAndDropTransferData?
)

/**
 * Returns the position of this [DragAndDropEvent] relative to the root Compose View in the
 * layout hierarchy.
 */
internal actual val DragAndDropEvent.positionInRoot: Offset
    get() = offset