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

import android.os.Parcel
import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.core.util.forEach
import kotlin.collections.filterIsInstance
import kotlin.collections.joinToString

/**
 * Represents the selected content in the document. The key is the page number and the value is a
 * list of selections in that page.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class DocumentSelection(val selectedContents: SparseArray<List<Selection>>) {

    val selection: Selection
        get() {

            val flattenedSelection = mutableListOf<Selection>()
            selectedContents.forEach { _, selections -> flattenedSelection.addAll(selections) }

            return TextSelection(
                flattenedSelection.filterIsInstance<TextSelection>().joinToString(" ") { it.text },
                flattenedSelection.flatMap { it.bounds },
            )
        }

    internal fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(selectedContents.size())
        selectedContents.forEach { pageNum, selections ->
            dest.writeInt(pageNum)
            // TODO: Add parcel logic for other types of selections
            val textSelections = selections.filterIsInstance<TextSelection>()
            dest.writeInt(textSelections.size)
            for (selection in textSelections) {
                selection.writeToParcel(dest, flags)
            }
        }
    }

    public companion object {
        public fun selectionValueFromParcel(parcel: Parcel): DocumentSelection {
            val selectionMap = SparseArray<List<Selection>>()
            val selectionSize: Int = parcel.readInt()
            for (i in 0 until selectionSize) {
                val pageNum = parcel.readInt()
                val selectionSize = parcel.readInt()
                // TODO: Add unparcel logic for other types of selections
                val selections = mutableListOf<Selection>()
                for (j in 0 until selectionSize) {
                    selections.add(textSelectionFromParcel(parcel))
                }
                selectionMap[pageNum] = selections
            }
            return DocumentSelection(selectionMap)
        }
    }
}
