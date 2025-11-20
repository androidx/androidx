/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.selection

import android.graphics.PointF
import android.os.Parcel
import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.core.util.forEach
import androidx.pdf.PdfPoint
import androidx.pdf.selection.model.GoToLinkSelection
import androidx.pdf.selection.model.HyperLinkSelection
import androidx.pdf.selection.model.TextSelection
import androidx.pdf.selection.model.goToLinkSelectionFromParcel
import androidx.pdf.selection.model.hyperLinkSelectionFromParcel
import androidx.pdf.selection.model.textSelectionFromParcel

/**
 * Represents the selected content in the document. The key is the page number and the value is a
 * list of selections in that page. Currently, multi-content selection is not supported, so only one
 * type of selection will be present.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class DocumentSelection(val selectedContents: SparseArray<List<Selection>>) {

    class SelectionType {
        companion object {
            const val GOTOLINK = 1
            const val HYPERLINK = 2
            const val TEXT = 3
        }
    }

    val containsOnlyTextSelections: Boolean
        get() {
            selectedContents.forEach { _, selections ->
                val firstSelection = selections.firstOrNull()
                // If a selection exists, check its type and return immediately as we only support
                // single content type selection thus no need to check the rest.
                if (firstSelection != null) {
                    return firstSelection is TextSelection
                }
            }
            return false
        }

    val selection: Selection?
        get() {
            val flattenedSelection = mutableListOf<Selection>()
            selectedContents.forEach { _, selections -> flattenedSelection += selections }

            // If all selected items are text, combine them.
            val combinedTextSelection = flattenedSelection.filterIsInstance<TextSelection>()
            if (combinedTextSelection.isNotEmpty()) {
                val text = combinedTextSelection.joinToString(" ") { it.text }
                val bounds = flattenedSelection.flatMap { it.bounds }
                return TextSelection(text, bounds)
            }

            // Otherwise, return the other selection which will be a single element or null.
            return flattenedSelection.firstOrNull()
        }

    /**
     * Gets the start and end points of selection.
     *
     * @return A pair of [PdfPoint] objects representing the start and end of the selection.
     */
    fun getSelectionEndpoints(): Pair<PdfPoint, PdfPoint> {
        // Finding the first selection bound of the first page
        val firstPage = selectedContents.keyAt(0)
        val firstBound: PointF =
            selectedContents[firstPage]?.firstOrNull()?.bounds?.firstOrNull()?.let {
                PointF(it.left, it.bottom)
            } ?: PointF(0f, 0f)

        // Finding the last selection bound of the last page
        val lastPage = selectedContents.keyAt(selectedContents.size() - 1)
        val lastBound: PointF =
            selectedContents[lastPage]?.lastOrNull()?.bounds?.lastOrNull()?.let {
                PointF(it.right, it.bottom)
            } ?: PointF(0f, 0f)

        // Create PdfPoint objects
        val firstPdfPoint = PdfPoint(firstPage, firstBound)
        val lastPdfPoint = PdfPoint(lastPage, lastBound)

        return Pair(firstPdfPoint, lastPdfPoint)
    }

    internal fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(selectedContents.size())
        selectedContents.forEach { pageNum, selections ->
            dest.writeInt(pageNum)
            dest.writeInt(selections.size)
            for (selection in selections) {
                when (selection) {
                    is TextSelection -> {
                        dest.writeInt(SelectionType.TEXT)
                        selection.writeToParcel(dest, flags)
                    }
                    is HyperLinkSelection -> {
                        dest.writeInt(SelectionType.HYPERLINK)
                        selection.writeToParcel(dest, flags)
                    }
                    is GoToLinkSelection -> {
                        dest.writeInt(SelectionType.GOTOLINK)
                        selection.writeToParcel(dest, flags)
                    }
                }
            }
        }
    }

    companion object {
        fun selectionValueFromParcel(parcel: Parcel): DocumentSelection {
            val selectionMap = SparseArray<List<Selection>>()
            val selectionSize: Int = parcel.readInt()
            for (i in 0 until selectionSize) {
                val pageNum = parcel.readInt()
                val selections = mutableListOf<Selection>()
                val listSize = parcel.readInt()
                for (j in 0 until listSize) {
                    val selectionType = parcel.readInt()
                    val selection =
                        when (selectionType) {
                            SelectionType.TEXT -> textSelectionFromParcel(parcel)
                            SelectionType.HYPERLINK -> hyperLinkSelectionFromParcel(parcel)
                            SelectionType.GOTOLINK -> goToLinkSelectionFromParcel(parcel)
                            else -> null
                        }
                    selection?.let { selections.add(selection) }
                }
                selectionMap[pageNum] = selections
            }
            return DocumentSelection(selectionMap)
        }
    }
}
