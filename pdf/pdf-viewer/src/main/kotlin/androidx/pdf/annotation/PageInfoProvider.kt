/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.annotation

import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.RestrictTo

/** Provides page information from view coordinates */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun interface PageInfoProvider {

    /** Returns [PageInfo] at the given coordinates, or null if outside any page. */
    public fun getPageInfoFromViewCoordinates(viewX: Float, viewY: Float): PageInfo?

    /**
     * Holds page metadata and coordinate transforms.
     *
     * @property pageNum 0-based page index.
     * @property pageBounds Bounds in view coordinates.
     * @property pageToViewTransform Maps page content to view coordinates.
     * @property viewToPageTransform Maps view coordinates to page content.
     */
    public data class PageInfo(
        val pageNum: Int,
        val pageBounds: RectF,
        val pageToViewTransform: Matrix,
        val viewToPageTransform: Matrix,
    )
}
