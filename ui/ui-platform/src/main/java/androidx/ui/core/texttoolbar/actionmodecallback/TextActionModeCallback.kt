/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.texttoolbar.actionmodecallback

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View

internal const val MENU_ITEM_COPY = 0

internal class TextActionModeCallback(
    private val view: View,
    private val onDeselectRequested: () -> Unit,
    private val onCopyRequested: () -> Unit
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        requireNotNull(menu)
        requireNotNull(mode)

        menu.add(0, MENU_ITEM_COPY, 0, "copy")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        if (item!!.itemId == MENU_ITEM_COPY) {
            onCopyRequested()
            onDeselectRequested()
            mode?.finish()
            return true
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {}
}