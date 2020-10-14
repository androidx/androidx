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

package androidx.wear.watchface.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.icu.util.Calendar
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.wear.watchface.Complication
import androidx.wear.watchface.data.RenderParametersWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat

/**
 * Interface for communication with the watch face.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public interface WatchFaceConfigDelegate {
    /**  Returns the style schema. */
    public fun getUserStyleSchema(): UserStyleSchemaWireFormat

    /** Returns the current user style. */
    public fun getUserStyle(): UserStyleWireFormat

    /** Sets the user style map. */
    public fun setUserStyle(userStyle: UserStyleWireFormat)

    /** Returns the id of the background complication or null if there isn't one. */
    public fun getBackgroundComplicationId(): Int?

    /**
     * Returns a map of complication IDs to complications.
     *
     * TODO(alexclarke): These should be passed as bundles.
     */
    public fun getComplicationsMap(): Map<Int, Complication>

    /** Returns the current [Calendar]. */
    public fun getCalendar(): Calendar

    /**
     * Returns the id of the complication at the given coordinates.
     */
    public fun getComplicationIdAt(tapX: Int, tapY: Int): Int?

    /**
     * Requests that the specified complication is highlighted for a short duration.
     */
    public fun brieflyHighlightComplicationId(complicationId: Int)

    /** Requests a screenshot of the watch face. */
    public fun takeScreenshot(
        drawRect: Rect,
        calendar: Calendar,
        renderParameters: RenderParametersWireFormat
    ): Bitmap
}
