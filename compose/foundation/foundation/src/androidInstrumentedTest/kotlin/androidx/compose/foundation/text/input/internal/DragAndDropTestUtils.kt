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

package androidx.compose.foundation.text.input.internal

import android.content.ClipData
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.view.DragEvent
import androidx.compose.foundation.content.createClipData
import androidx.compose.ui.geometry.Offset

/**
 * Helper utilities for creating drag events.
 *
 * This class is a copy-paste from DragAndDrop artifact with the addition of configurable offset.
 * Also it does not mock but uses Parcel to create a DragEvent.
 */
object DragAndDropTestUtils {
    private const val SAMPLE_TEXT = "Drag Text"
    private val SAMPLE_URI = Uri.parse("http://www.google.com")

    /**
     * Makes a stub drag event containing fake text data.
     *
     * @param action One of the [DragEvent] actions.
     */
    fun makeTextDragEvent(
        action: Int,
        text: String = SAMPLE_TEXT,
        offset: Offset = Offset.Zero,
    ): DragEvent {
        return makeDragEvent(
            action = action,
            clipData = createClipData { addText(text) },
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
        offset: Offset = Offset.Zero,
    ): DragEvent {
        return makeDragEvent(
            action = action,
            clipData =
                createClipData {
                    // We're not actually resolving Uris in these tests, so this can be anything:
                    addUri(item, mimeType = "image/png")
                },
            offset = offset
        )
    }

    fun makeDragEvent(action: Int, clipData: ClipData, offset: Offset = Offset.Zero): DragEvent {
        val parcel = Parcel.obtain()

        parcel.writeInt(action)
        parcel.writeFloat(offset.x)
        parcel.writeFloat(offset.y)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // mOffset was made part of DragEvent in API 31.
            parcel.writeFloat(0f)
            parcel.writeFloat(0f)
        }
        if (Build.VERSION.SDK_INT >= 35) {
            // mFlags was added
            parcel.writeInt(0)
        }
        parcel.writeInt(0) // Result
        parcel.writeInt(1)
        clipData.writeToParcel(parcel, 0)
        parcel.writeInt(1)
        clipData.description.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)
        return DragEvent.CREATOR.createFromParcel(parcel)
    }
}
