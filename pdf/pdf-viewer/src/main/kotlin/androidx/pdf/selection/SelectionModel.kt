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

package androidx.pdf.selection

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.core.util.isEmpty
import androidx.pdf.PdfPoint
import androidx.pdf.content.PageSelection
import androidx.pdf.content.toViewSelection
import androidx.pdf.view.pdfPointFromParcel
import androidx.pdf.view.writeToParcel
import kotlin.collections.firstOrNull

/** Value class containing all data necessary to display UI related to content selection */
@SuppressLint("BanParcelableUsage")
internal class SelectionModel(
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
         * @param newPageSelections New [androidx.pdf.content.PageSelection] objects on different
         *   pages.
         * @return A [SelectionModel] that encompasses all selections, or `null` if none were found.
         */
        fun getCombinedSelectionModel(
            currentSelection: DocumentSelection,
            newPageSelections: List<PageSelection?>,
        ): SelectionModel? {

            val selection = mergeSelection(currentSelection, newPageSelections)
            if (selection.selectedContents.isEmpty()) return null

            val selectionBounds = selection.getSelectionEndpoints()

            val isRtl = newPageSelections.firstOrNull()?.start?.isRtl ?: false
            return SelectionModel(
                selection,
                UiSelectionBoundary(selectionBounds.first, isRtl),
                UiSelectionBoundary(selectionBounds.second, isRtl),
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
                        newPageSelection.toViewSelection()
                }
            }

            return DocumentSelection(currentSelection.selectedContents)
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
