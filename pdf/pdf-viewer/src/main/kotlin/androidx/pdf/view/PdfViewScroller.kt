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

package androidx.pdf.view

import androidx.annotation.IntDef
import androidx.pdf.PdfPoint
import androidx.pdf.util.ZoomUtils
import androidx.pdf.view.layout.PageLayoutManager
import kotlin.math.roundToInt

internal class PdfViewScroller(private val pdfView: PdfView) {

    fun scrollToPage(pageNum: Int, onScrollDeferred: (DeferredScrollTarget) -> Unit) {
        with(pdfView) {
            val localPageLayoutManager =
                getPageLayoutManagerOrThrow(pageLayoutManager, "scrollToPage")
            require(pageNum < (pdfDocument?.pageCount ?: Int.MIN_VALUE)) {
                "Page $pageNum not in document"
            }

            if (localPageLayoutManager.reach >= pageNum) {
                gotoPage(pageNum)
            } else {
                localPageLayoutManager.increaseReach(pageNum)
                onScrollDeferred(DeferredScrollTarget.ToPage(pageNum))
            }
        }
    }

    fun scrollToPosition(
        position: PdfPoint,
        @ScrollAlignmentDef alignment: Int,
        onScrollDeferred: (DeferredScrollTarget) -> Unit,
    ) {
        with(pdfView) {
            val localPageLayoutManager =
                getPageLayoutManagerOrThrow(pageLayoutManager, "scrollToPosition")

            if (position.pageNum >= (pdfDocument?.pageCount ?: Int.MIN_VALUE)) {
                return
            }

            if (localPageLayoutManager.reach >= position.pageNum) {
                gotoPoint(position, alignment)
            } else {
                localPageLayoutManager.increaseReach(position.pageNum)
                onScrollDeferred(DeferredScrollTarget.ToPosition(position))
            }
        }
    }

    private fun gotoPage(pageNum: Int) {
        with(pdfView) {
            val localPageLayoutManager =
                getPageLayoutManagerOrThrow(pageLayoutManager, "scrollToPage")

            check(pageNum <= localPageLayoutManager.reach) { "Can't gotoPage that's not laid out" }

            val pageRect =
                localPageLayoutManager.getPageLocation(pageNum, getVisibleAreaInContentCoords())
            // Zoom should match the width of the page
            val zoom =
                ZoomUtils.calculateZoomToFit(
                    viewportWidth.toFloat(),
                    viewportHeight.toFloat(),
                    contentWidth,
                    1f,
                )
            val x =
                ((pageRect.left + pageRect.width() / 2f) * zoom - (viewportWidth / 2f)).roundToInt()
            val y =
                ((pageRect.top + pageRect.height() / 2f) * zoom - (viewportHeight / 2f))
                    .roundToInt()

            // Set zoom to fit the width of the page, then scroll to the center of the page
            this.zoom = zoom
            scrollTo(x, y)
        }
    }

    private fun gotoPoint(position: PdfPoint, @ScrollAlignmentDef alignment: Int) {
        with(pdfView) {
            val localPageLayoutManager =
                getPageLayoutManagerOrThrow(pageLayoutManager, "scrollToPage")

            check(position.pageNum <= localPageLayoutManager.reach) {
                "Can't gotoPoint on page that's not laid out"
            }

            val pageRect =
                localPageLayoutManager.getPageLocation(
                    position.pageNum,
                    getVisibleAreaInContentCoords(),
                )

            val x = ((pageRect.left + position.x) * zoom - (viewportWidth / 2f)).roundToInt()

            val y =
                when (alignment) {
                    ScrollAlignment.CENTRE -> {
                        ((pageRect.top + position.y) * zoom - (viewportHeight / 2f)).roundToInt()
                    }
                    ScrollAlignment.TOP -> {
                        ((pageRect.top + position.y) * zoom).roundToInt()
                    }
                    else -> throw IllegalArgumentException("Invalid scroll Alignment")
                }

            scrollTo(x, y)
        }
    }

    private fun getPageLayoutManagerOrThrow(
        pageLayoutManager: PageLayoutManager?,
        operationName: String,
    ): PageLayoutManager {
        return pageLayoutManager
            ?: throw IllegalStateException("Can't $operationName without PdfDocument")
    }
}

/** Defines the target for a deferred scroll operation. */
internal sealed interface DeferredScrollTarget {
    data class ToPage(val pageNum: Int) : DeferredScrollTarget

    data class ToPosition(val position: PdfPoint) : DeferredScrollTarget
}

/** Defines integer constants to represent relative position of selection handles */
internal object ScrollAlignment {
    const val CENTRE = 1
    const val TOP = 2
}

@IntDef(ScrollAlignment.CENTRE, ScrollAlignment.TOP)
@Retention(AnnotationRetention.SOURCE)
internal annotation class ScrollAlignmentDef
