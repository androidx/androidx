/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.ink

import android.graphics.Matrix
import android.graphics.RectF
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.pdf.annotation.PageInfoProvider

/** Default implementation of [PageInfoProvider] that calculates page information. */
internal class PageInfoProviderImpl : PageInfoProvider {
    var zoom: Float = 1f
    var pageLocations: SparseArray<RectF> = SparseArray()

    override fun getPageInfoFromViewCoordinates(
        viewX: Float,
        viewY: Float,
    ): PageInfoProvider.PageInfo? {
        pageLocations.forEach { pageNum, pageBounds ->
            if (pageBounds.contains(viewX, viewY)) {
                val pageToViewTransform =
                    Matrix().apply {
                        postScale(zoom, zoom)
                        postTranslate(pageBounds.left, pageBounds.top)
                    }

                val viewToPageTransform = Matrix()
                if (!pageToViewTransform.invert(viewToPageTransform)) {
                    return null
                }

                return PageInfoProvider.PageInfo(
                    pageNum = pageNum,
                    pageBounds = pageBounds,
                    pageToViewTransform = pageToViewTransform,
                    viewToPageTransform = viewToPageTransform,
                )
            }
        }
        return null
    }
}
