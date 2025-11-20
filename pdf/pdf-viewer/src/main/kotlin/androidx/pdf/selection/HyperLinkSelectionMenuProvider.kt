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
import androidx.pdf.R
import androidx.pdf.selection.model.HyperLinkSelection
import androidx.pdf.util.ClipboardUtils

internal class HyperLinkSelectionMenuProvider(private val context: Context) :
    SelectionMenuProvider<HyperLinkSelection> {

    override suspend fun getMenuItems(selection: HyperLinkSelection): List<ContextMenuComponent> {
        val menuItems: MutableList<ContextMenuComponent> = mutableListOf()
        // Avoid copy link context menu option for mail link as it is not intuitive.
        if (!selection.link.toString().contains(MAIL_TO)) {
            menuItems += getCopyLinkMenuItem()
        }
        menuItems += DefaultSelectionMenuProvider.getMenuItems(context)
        return menuItems
    }

    private fun getCopyLinkMenuItem(): ContextMenuComponent {
        return DefaultSelectionMenuComponent(
            key = PdfSelectionMenuKeys.CopyLinkKey,
            label = context.getString(R.string.label_copy_link),
            contentDescription = context.getString(R.string.desc_copy_link),
        ) { pdfView ->
            val localCurrentSelection = pdfView.currentSelection
            if (localCurrentSelection is HyperLinkSelection) {
                ClipboardUtils.copyToClipboard(context, localCurrentSelection.link.toString())
            }
            close()
            pdfView.clearCurrentSelection()
        }
    }

    companion object {
        private const val MAIL_TO = "mailto:"

        // Filter link helps in providing better context menu options depending on the type of link
        // As copy link is not intuitive option for a mail link rather prefer an email menu.
        fun filterLink(link: String): String {
            return if (link.contains(MAIL_TO)) {
                link.substringAfter(MAIL_TO)
            } else {
                link
            }
        }
    }
}
