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

import android.content.Context
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Region
import android.util.SparseArray
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.pdf.annotation.AnnotationsView.PageAnnotationsData
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PathPdfObject.PathInput
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.StampAnnotation
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/**
 * Handles touch events to detect hits on existing annotations.
 *
 * This handler converts view coordinates to PDF page coordinates and checks for intersections with
 * annotations currently rendered by [AnnotationsView].
 */
internal class AnnotationsLocator(
    context: Context,
    private val pageInfoProvider: PageInfoProvider,
) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    // Track the starting point of the current gesture
    private var startingPoint: PointF? = null

    /** Handles the touch event to perform hit detection. */
    internal fun findAnnotations(
        annotations: SparseArray<PageAnnotationsData>,
        event: MotionEvent,
    ): List<KeyedPdfAnnotation> {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Start tracking the gesture
                startingPoint = PointF(event.x, event.y)
                findAnnotationsAtPoint(annotations, event)
            }
            MotionEvent.ACTION_MOVE -> {
                val start = startingPoint
                // Perform hit detection if the gesture start was missed or if moved beyond slop.
                if (start == null || hypot(start.x - event.x, start.y - event.y) > touchSlop) {
                    findAnnotationsAtPoint(annotations, event)
                } else {
                    emptyList()
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // Reset the state
                startingPoint = null
                emptyList()
            }
            else -> emptyList()
        }
    }

    /**
     * Finds the list of annotations ordered by z-index (top -> bottom) at the given touch point
     * using precise path intersection.
     */
    private fun findAnnotationsAtPoint(
        annotations: SparseArray<PageAnnotationsData>,
        event: MotionEvent,
    ): List<KeyedPdfAnnotation> {
        val pageInfo =
            pageInfoProvider.getPageInfoFromViewCoordinates(event.x, event.y) ?: return emptyList()

        val touchRectView =
            RectF(
                event.x - touchSlop,
                event.y - touchSlop,
                event.x + touchSlop,
                event.y + touchSlop,
            )

        val touchRectPdf = RectF()
        pageInfo.viewToPageTransform.mapRect(touchRectPdf, touchRectView)
        val touchRegion = touchRectPdf.toRegion()

        val keyedPdfAnnotations =
            annotations.get(pageInfo.pageNum)?.keyedAnnotations ?: return emptyList()

        return keyedPdfAnnotations
            .filter { keyedPdfAnnotation ->
                isAnnotationHit(keyedPdfAnnotation.annotation, touchRegion, touchRectPdf)
            }
            .asReversed()
    }

    /**
     * Determines if the given touch point (as a Region and RectF) intersects with an annotation.
     */
    private fun isAnnotationHit(
        annotation: PdfAnnotation,
        touchRegion: Region,
        touchRectPdf: RectF,
    ): Boolean {
        return when (annotation) {
            is StampAnnotation -> {
                // Fast Bounding Box check.
                if (RectF.intersects(annotation.bounds, touchRectPdf)) {
                    annotation.pdfObjects.any { pdfObject ->
                        if (pdfObject is PathPdfObject) {
                            val path =
                                Path().apply {
                                    pdfObject.inputs.forEach { pathInput ->
                                        if (pathInput.command == PathInput.MOVE_TO) {
                                            moveTo(pathInput.x, pathInput.y)
                                        } else if (pathInput.command == PathInput.LINE_TO) {
                                            lineTo(pathInput.x, pathInput.y)
                                        }
                                    }
                                }

                            val pathRegion = Region()
                            val clip = annotation.bounds.toRegion()
                            pathRegion.setPath(path, clip)

                            // Intersect the path region with our touch square region.
                            if (pathRegion.op(touchRegion, Region.Op.INTERSECT) == true) {
                                return true
                            }
                        }
                        false
                    }
                }
                false
            }
            else -> false
        }
    }

    /** Extension to convert RectF to a Region with outward rounding. */
    private fun RectF.toRegion(): Region {
        return Region(
            floor(left).toInt(),
            floor(top).toInt(),
            ceil(right).toInt(),
            ceil(bottom).toInt(),
        )
    }
}
