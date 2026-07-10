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

import android.content.Context
import android.graphics.PointF
import androidx.pdf.PdfPoint
import androidx.pdf.R
import androidx.pdf.selection.model.GoToLinkSelection

internal class GoToLinkSelectionMenuProvider(private val context: Context) :
    SelectionMenuProvider<GoToLinkSelection> {

    override suspend fun getMenuItems(selection: GoToLinkSelection): List<ContextMenuComponent> {
        val menuItems: MutableList<ContextMenuComponent> = mutableListOf()
        menuItems += getGoToMenuItem()
        menuItems += DefaultSelectionMenuProvider.getMenuItems(context)
        return menuItems
    }

    private fun getGoToMenuItem(): ContextMenuComponent {
        return DefaultSelectionMenuComponent(
            key = PdfSelectionMenuKeys.GoToKey,
            label = context.getString(R.string.label_jump_to),
            contentDescription = context.getString(R.string.desc_jump_to),
        ) { pdfView ->
            val localCurrentSelection = pdfView.currentSelection
            if (localCurrentSelection is GoToLinkSelection) {
                val destination =
                    PdfPoint(
                        pageNum = localCurrentSelection.destination.pageNumber,
                        pagePoint =
                            PointF(
                                localCurrentSelection.destination.xCoordinate,
                                localCurrentSelection.destination.yCoordinate,
                            ),
                    )
                pdfView.scrollToPosition(destination)
            }
            close()
            pdfView.clearCurrentSelection()
        }
    }
}
