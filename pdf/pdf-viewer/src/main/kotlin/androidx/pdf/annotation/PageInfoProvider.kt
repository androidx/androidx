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

package androidx.pdf.annotation

import android.graphics.Matrix
import android.graphics.RectF
import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.core.util.forEach

/** A provider class that provides utility functions to calculate page information. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PageInfoProvider {
    private var zoom: Float = 1f
    private var pageBounds: SparseArray<RectF> = SparseArray()

    public fun setZoom(zoom: Float) {
        this.zoom = zoom
    }

    public fun setPageBounds(pageBounds: SparseArray<RectF>) {
        this.pageBounds = pageBounds
    }

    public fun getPageInfo(pageNum: Int): PageInfo? {
        val pageBounds = pageBounds[pageNum] ?: return null
        val pageToViewTransform = getTransformMatrix(pageBounds)

        val viewToPageTransform = getInverseMatrix(pageToViewTransform) ?: return null

        return PageInfo(
            pageNum = pageNum,
            pageBounds = pageBounds,
            pageToViewTransform = pageToViewTransform,
            viewToPageTransform = viewToPageTransform,
        )
    }

    public fun getPageInfoFromViewCoordinates(viewX: Float, viewY: Float): PageInfo? {
        pageBounds.forEach { pageNum, pageBounds ->
            if (pageBounds.contains(viewX, viewY)) {
                val pageToViewTransform = getTransformMatrix(pageBounds)

                val viewToPageTransform = getInverseMatrix(pageToViewTransform) ?: return null

                return PageInfo(
                    pageNum = pageNum,
                    pageBounds = pageBounds,
                    pageToViewTransform = pageToViewTransform,
                    viewToPageTransform = viewToPageTransform,
                )
            }
        }
        return null
    }

    private fun getTransformMatrix(pageBounds: RectF): Matrix {
        return Matrix().apply {
            postScale(zoom, zoom)
            postTranslate(pageBounds.left, pageBounds.top)
        }
    }

    private fun getInverseMatrix(matrix: Matrix): Matrix? {
        val inverseMatrix = Matrix()
        if (!matrix.invert(inverseMatrix)) {
            return null
        }
        return inverseMatrix
    }

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
