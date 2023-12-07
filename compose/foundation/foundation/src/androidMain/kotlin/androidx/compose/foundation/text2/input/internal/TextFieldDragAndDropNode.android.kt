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

package androidx.compose.foundation.text2.input.internal

import android.content.ClipData
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

internal actual fun textFieldDragAndDropNode(
    acceptedMimeTypes: Set<String>,
    onDrop: (text: AnnotatedString) -> Boolean,
    onStarted: ((event: DragAndDropEvent) -> Unit)?,
    onEntered: ((event: DragAndDropEvent) -> Unit)?,
    onMoved: ((position: Offset) -> Unit)?,
    onChanged: ((event: DragAndDropEvent) -> Unit)?,
    onExited: ((event: DragAndDropEvent) -> Unit)?,
    onEnded: ((event: DragAndDropEvent) -> Unit)?,
): DragAndDropModifierNode {
    return DragAndDropModifierNode(
        shouldStartDragAndDrop = { dragAndDropEvent ->
            val clipDescription = dragAndDropEvent.toAndroidDragEvent().clipDescription
            acceptedMimeTypes.any { clipDescription.hasMimeType(it) }
        },
        target = object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean =
                onDrop.invoke(event.toAndroidDragEvent().clipData.convertToAnnotatedString())

            override fun onStarted(event: DragAndDropEvent) =
                onStarted?.invoke(event) ?: Unit

            override fun onEntered(event: DragAndDropEvent) =
                onEntered?.invoke(event) ?: Unit

            override fun onMoved(event: DragAndDropEvent) = with(event.toAndroidDragEvent()) {
                onMoved?.invoke(Offset(x, y)) ?: Unit
            }

            override fun onExited(event: DragAndDropEvent) =
                onExited?.invoke(event) ?: Unit

            override fun onChanged(event: DragAndDropEvent) =
                onChanged?.invoke(event) ?: Unit

            override fun onEnded(event: DragAndDropEvent) =
                onEnded?.invoke(event) ?: Unit
        }
    )
}

private fun ClipData.convertToAnnotatedString(): AnnotatedString {
    // TODO(halilibo): Implement stylized text to AnnotatedString conversion.
    return buildAnnotatedString {
        var isFirst = true
        for (i in 0 until itemCount) {
            if (isFirst) {
                getItemAt(i).text?.let { append(it) }
                isFirst = false
            } else {
                getItemAt(i).text?.let {
                    append("\n")
                    append(it)
                }
            }
        }
    }
}
