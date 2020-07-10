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

internal class TextFieldActionModeCallback(
    private val view: View,
    private val onCopyRequested: () -> Unit,
    private val onPasteRequested: () -> Unit,
    private val onCutRequested: () -> Unit
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        requireNotNull(menu)
        requireNotNull(mode)

        menu.add(0, MENU_ITEM_COPY, 0, android.R.string.copy)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add(0, MENU_ITEM_PASTE, 1, android.R.string.paste)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add(0, MENU_ITEM_CUT, 2, android.R.string.cut)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item!!.itemId) {
            MENU_ITEM_COPY -> onCopyRequested()
            MENU_ITEM_PASTE -> onPasteRequested()
            MENU_ITEM_CUT -> onCutRequested()
            else -> return false
        }
        mode?.finish()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {}
}
