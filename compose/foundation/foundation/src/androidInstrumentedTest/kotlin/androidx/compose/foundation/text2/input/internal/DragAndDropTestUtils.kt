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
import android.content.ClipDescription
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.view.DragEvent
import androidx.compose.ui.geometry.Offset

/**
 * Helper utilities for creating drag events.
 *
 * This class is a copy-paste from DragAndDrop artifact with the addition of configurable offset.
 * Also it does not mock but uses Parcel to create a DragEvent.
 */
object DragAndDropTestUtils {
    private const val LABEL = "Label"
    const val SAMPLE_TEXT = "Drag Text"
    val SAMPLE_URI = Uri.parse("http://www.google.com")

    /**
     * Makes a stub drag event containing fake text data.
     *
     * @param action One of the [DragEvent] actions.
     */
    fun makeTextDragEvent(action: Int, offset: Offset = Offset.Zero): DragEvent {
        return makeDragEvent(
            action = action,
            items = listOf(ClipData.Item(SAMPLE_TEXT)),
            mimeTypes = listOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
            offset = offset
        )
    }

    /**
     * Makes a stub drag event containing text data.
     *
     * @param action One of the [DragEvent] actions.
     * @param text The text being dragged.
     */
    fun makeTextDragEvent(action: Int, text: String?, offset: Offset = Offset.Zero): DragEvent {
        return makeDragEvent(
            action = action,
            items = listOf(ClipData.Item(text)),
            mimeTypes = listOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
            offset = offset
        )
    }

    /**
     * Makes a stub drag event containing an image mimetype and fake uri.
     *
     * @param action One of the [DragEvent] actions.
     */
    fun makeImageDragEvent(
        action: Int,
        item: Uri = SAMPLE_URI,
        offset: Offset = Offset.Zero
    ): DragEvent {
        // We're not actually resolving Uris in these tests, so this can be anything:
        val mimeType = "image/*"
        return makeDragEvent(
            action = action,
            items = listOf(ClipData.Item(item)),
            mimeTypes = listOf(mimeType),
            offset = offset
        )
    }

    fun makeDragEvent(
        action: Int,
        items: List<ClipData.Item>,
        mimeTypes: List<String>,
        offset: Offset = Offset.Zero
    ): DragEvent {
        val clipDescription = ClipDescription(LABEL, mimeTypes.toTypedArray())
        val clipData = ClipData(clipDescription, items.first()).apply {
            items.drop(1).forEach { addItem(it) }
        }

        val parcel = Parcel.obtain()

        parcel.writeInt(action)
        parcel.writeFloat(offset.x)
        parcel.writeFloat(offset.y)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // mOffset was made part of DragEvent in API 31.
            parcel.writeFloat(0f)
            parcel.writeFloat(0f)
        }
        parcel.writeInt(0) // Result
        parcel.writeInt(1)
        clipData.writeToParcel(parcel, 0)
        parcel.writeInt(1)
        clipDescription.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)
        return DragEvent.CREATOR.createFromParcel(parcel)
    }
}
