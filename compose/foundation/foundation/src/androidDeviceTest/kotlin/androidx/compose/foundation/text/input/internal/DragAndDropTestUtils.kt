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
import android.view.Display
import android.view.DragEvent
import androidx.compose.foundation.content.createClipData
import androidx.compose.ui.geometry.Offset

/**
 * Helper utilities for creating drag events.
 *
 * This class originated from the DragAndDrop artifact with the addition of configurable offset.
 * Also it does not mock but uses Parcel to create a DragEvent.
 */
object DragAndDropTestUtils {
    private const val SAMPLE_TEXT = "Drag Text"
    private val SAMPLE_URI = Uri.parse("http://www.google.com")

    /**
     * Makes a stub drag event containing fake text data.
     *
     * @param action The action passed to [DragEvent], for example [DragEvent.ACTION_DRAG_ENDED]
     * @param text The text of the event
     * @param position The position of the drag event
     * @param displayId The display id of the drag event, [Display.DEFAULT_DISPLAY] by default. Only
     *   used on API 36+. This is only relevant for multi-display environments. For technical
     *   correctness, you should obtain the correct display id, for example from the closest View's
     *   display (see [android.view.View.getDisplay]).
     */
    fun makeTextDragEvent(
        action: Int,
        text: String = SAMPLE_TEXT,
        position: Offset = Offset.Zero,
        displayId: Int = Display.DEFAULT_DISPLAY,
    ): DragEvent {
        return makeDragEvent(
            action = action,
            clipData = createClipData { addText(text) },
            position = position,
            displayId = displayId,
        )
    }

    /**
     * Makes a stub drag event containing an image mimetype and fake uri.
     *
     * @param action The action passed to [DragEvent], for example [DragEvent.ACTION_DRAG_ENDED]
     * @param item The [Uri] of the item
     * @param position The position of the drag event
     * @param displayId The display id of the drag event, [Display.DEFAULT_DISPLAY] by default. Only
     *   used on API 36+. This is only relevant for multi-display environments. For technical
     *   correctness, you should obtain the correct display id, for example from the closest View's
     *   display (see [android.view.View.getDisplay]).
     */
    fun makeImageDragEvent(
        action: Int,
        item: Uri = SAMPLE_URI,
        position: Offset = Offset.Zero,
        displayId: Int = Display.DEFAULT_DISPLAY,
    ): DragEvent {
        return makeDragEvent(
            action = action,
            clipData =
                createClipData {
                    // We're not actually resolving Uris in these tests, so this can be anything:
                    addUri(item, mimeType = "image/png")
                },
            position = position,
            displayId = displayId,
        )
    }

    /**
     * Create a platform [DragEvent] instance. [DragEvent]s are usually not constructed directly, so
     * we create it from a parcel. This parcel needs to match the structure used in
     * [DragEvent.CREATOR].
     *
     * @param action The action passed to [DragEvent], for example [DragEvent.ACTION_DRAG_ENDED]
     * @param clipData The [ClipData] associated with the event
     * @param position The position of the drag event
     * @param displayId The display id of the drag event, [Display.DEFAULT_DISPLAY] by default. Only
     *   used on API 36+. This is only relevant for multi-display environments. For technical
     *   correctness, you should obtain the correct display id, for example from the closest View's
     *   display (see [android.view.View.getDisplay]).
     */
    fun makeDragEvent(
        action: Int,
        clipData: ClipData,
        position: Offset = Offset.Zero,
        displayId: Int = Display.DEFAULT_DISPLAY,
    ): DragEvent {
        val parcel = Parcel.obtain()

        // mAction
        parcel.writeInt(action)
        // mX
        parcel.writeFloat(position.x)
        // mY
        parcel.writeFloat(position.y)
        // mOffset
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // mOffset was made part of DragEvent in API 31.
            parcel.writeFloat(0f)
            parcel.writeFloat(0f)
        }
        // mDisplayId
        if (Build.VERSION.SDK_INT >= 36) {
            parcel.writeInt(displayId)
        }
        // mFlags
        if (Build.VERSION.SDK_INT >= 35) {
            parcel.writeInt(0)
        }
        // mDragResult
        parcel.writeInt(0)
        // mClipData
        parcel.writeInt(1) // 0 = no clip data, 1 = clip data present
        clipData.writeToParcel(parcel, 0)
        // mClipDescription
        parcel.writeInt(1) // 0 = no description, 1 = description present
        clipData.description.writeToParcel(parcel, 0)
        // mDragSurface
        parcel.writeInt(0) // 0 = no SurfaceControl, 1 = SurfaceControl present
        // mDragAndDropPermissions
        parcel.writeInt(0) // 0 = no permissions, 1 = permissions present

        parcel.setDataPosition(0)
        return DragEvent.CREATOR.createFromParcel(parcel)
    }
}
