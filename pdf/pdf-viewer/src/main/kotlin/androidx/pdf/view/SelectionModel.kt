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

package androidx.pdf.view

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.core.util.isEmpty
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.pdf.content.PageSelection
import kotlin.collections.forEach

/** Value class containing all data necessary to display UI related to content selection */
@SuppressLint("BanParcelableUsage")
internal class SelectionModel
@VisibleForTesting
internal constructor(
    val documentSelection: DocumentSelection,
    val startBoundary: UiSelectionBoundary,
    val endBoundary: UiSelectionBoundary,
) : Parcelable {
    constructor(
        parcel: Parcel
    ) : this(
        documentSelection = DocumentSelection.selectionValueFromParcel(parcel = parcel),
        startBoundary = UiSelectionBoundary(parcel),
        endBoundary = UiSelectionBoundary(parcel),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        documentSelection.writeToParcel(dest, flags)
        startBoundary.writeToParcel(dest, flags)
        endBoundary.writeToParcel(dest, flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SelectionModel) return false

        if (other.documentSelection != documentSelection) return false
        if (other.startBoundary != startBoundary) return false
        if (other.endBoundary != endBoundary) return false
        return true
    }

    override fun hashCode(): Int {
        var result = documentSelection.hashCode()
        result = 31 * result + startBoundary.hashCode()
        result = 31 * result + endBoundary.hashCode()
        return result
    }

    companion object {
        /**
         * Combines multiple selections from different pages into a single [SelectionModel].
         *
         * @param currentSelection The current selection, can be `null` if no selection yet exists.
         * @param newPageSelections New [PageSelection] objects on different pages.
         * @return A [SelectionModel] that encompasses all selections, or `null` if none were found.
         */
        fun getCombinedSelectionModel(
            currentSelection: DocumentSelection,
            newPageSelections: List<PageSelection?>,
        ): SelectionModel? {

            val selection = mergeSelection(currentSelection, newPageSelections)
            if (selection.selectedContents.isEmpty()) return null

            // Finding the first selection bound of first page in the selection
            val firstPage = selection.selectedContents.keyAt(0)
            val firstBound: PointF =
                selection.selectedContents[firstPage].firstOrNull()?.bounds?.firstOrNull()?.let {
                    PointF(it.left, it.bottom)
                } ?: PointF(0f, 0f)

            // Finding the last selection bound of last page in the selection
            val lastPage = selection.selectedContents.keyAt(selection.selectedContents.size() - 1)
            val lastBound: PointF =
                selection.selectedContents[lastPage].lastOrNull()?.bounds?.lastOrNull()?.let {
                    PointF(it.right, it.bottom)
                } ?: PointF(0f, 0f)

            val isRtl = newPageSelections.firstOrNull()?.start?.isRtl ?: false
            return SelectionModel(
                selection,
                UiSelectionBoundary(PdfPoint(firstPage, firstBound), isRtl),
                UiSelectionBoundary(PdfPoint(lastPage, lastBound), isRtl),
            )
        }

        /**
         * Returns a merged [DocumentSelection] from [currentSelection] with a list of
         * [newPageSelections]
         */
        private fun mergeSelection(
            currentSelection: DocumentSelection,
            newPageSelections: List<PageSelection?>,
        ): DocumentSelection {

            // Process new selection
            newPageSelections.forEach { newPageSelection ->
                if (newPageSelection != null) {
                    currentSelection.selectedContents[newPageSelection.page] =
                        listOf(newPageSelection.toViewSelection())
                }
            }

            return DocumentSelection(currentSelection.selectedContents)
        }

        /**
         * Returns a [Selection] as exposed in the [PdfView] API from a [PageSelection] as produced
         * by the [PdfDocument] API
         */
        private fun PageSelection.toViewSelection(): Selection {
            val flattenedBounds =
                this.selectedTextContents.map { it.bounds }.flatten().map { PdfRect(this.page, it) }
            val concatenatedText = this.selectedTextContents.joinToString(" ") { it.text }
            return TextSelection(concatenatedText, flattenedBounds)
        }

        @JvmField
        val CREATOR =
            object : Parcelable.Creator<SelectionModel> {
                override fun createFromParcel(parcel: Parcel): SelectionModel {
                    return SelectionModel(parcel)
                }

                override fun newArray(size: Int): Array<SelectionModel?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

/**
 * Represents a selection boundary that includes the page on which it exists and the point at which
 * it exists (as [PdfPoint]), as well as the direction of the selection ([isRtl] is true if the
 * selection was made in a right-to-left direction).
 */
@SuppressLint("BanParcelableUsage")
internal class UiSelectionBoundary(val location: PdfPoint, val isRtl: Boolean) : Parcelable {
    constructor(parcel: Parcel) : this(pdfPointFromParcel(parcel), parcel.readBoolean())

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        location.writeToParcel(dest)
        dest.writeBoolean(isRtl)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is UiSelectionBoundary) return false

        if (other.location != this.location) return false
        if (other.isRtl != this.isRtl) return false
        return true
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + isRtl.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<UiSelectionBoundary> {
        override fun createFromParcel(parcel: Parcel): UiSelectionBoundary {
            return UiSelectionBoundary(parcel)
        }

        override fun newArray(size: Int): Array<UiSelectionBoundary?> {
            return arrayOfNulls(size)
        }
    }
}
