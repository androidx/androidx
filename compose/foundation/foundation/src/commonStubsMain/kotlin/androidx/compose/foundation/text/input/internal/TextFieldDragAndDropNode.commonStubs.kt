/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.implementedInJetBrainsFork
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTargetModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipMetadata

internal actual fun textFieldDragAndDropNode(
    hintMediaTypes: () -> Set<MediaType>,
    onDrop: (clipEntry: ClipEntry, clipMetadata: ClipMetadata) -> Boolean,
    dragAndDropRequestPermission: (DragAndDropEvent) -> Unit,
    onStarted: ((event: DragAndDropEvent) -> Unit)?,
    onEntered: ((event: DragAndDropEvent) -> Unit)?,
    onMoved: ((position: Offset) -> Unit)?,
    onChanged: ((event: DragAndDropEvent) -> Unit)?,
    onExited: ((event: DragAndDropEvent) -> Unit)?,
    onEnded: ((event: DragAndDropEvent) -> Unit)?,
): DragAndDropTargetModifierNode = implementedInJetBrainsFork()
