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

import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.wear.watchface.Complication

/**
 * Interface for communication with the watch face.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
interface WatchFaceConfigDelegate {
    /**
     * Returns a list of UserStyleCategories serialized into bundles. See {@link
     * UserStyleCategory.bundleToUserStyleCategoryLists}.
     */
    fun getUserStyleSchema(): List<Bundle>

    /**
     * Returns the current user style map serialized into a bundle. See
     * {@link UserStyleCategory.bundleToStyleMap}.
     */
    fun getUserStyle(): Bundle

    /** Sets the user style map. See {@link UserStyleCategory.styleMapToBundle}. */
    fun setUserStyle(style: Bundle)

    /** Returns the id of the background complication or null if there isn't one. */
    fun getBackgroundComplicationId(): Int?

    /**
     * Returns a map of complication IDs to complications.
     *
     * TODO(alexclarke): These should be passed as bundles.
     */
    fun getComplicationsMap(): Map<Int, Complication>

    /** Returns the current {@link Calendar}. */
    fun getCalendar(): Calendar

    /**
     * Returns the id of the complication at the given coordinates and the specified
     * {@link Calendar}.
     */
    fun getComplicationIdAt(tapX: Int, tapY: Int, calendar: Calendar): Int?

    /**
     * Requests that the specified complication is highlighted for a short duration.
     */
    fun brieflyHighlightComplicationId(complicationId: Int)

    /** TODO(alexclarke): This should be refactored in terms of the screen shot APIs.*/
    fun drawComplicationSelect(
        canvas: Canvas,
        drawRect: Rect,
        calendar: Calendar
    )
}