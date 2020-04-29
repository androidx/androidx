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

package androidx.ui.core.texttoolbar

import android.os.Build
import android.view.ActionMode
import android.view.View
import androidx.ui.core.texttoolbar.actionmodecallback.FloatingTextActionModeCallback
import androidx.ui.core.texttoolbar.actionmodecallback.PrimaryTextActionModeCallback
import androidx.ui.core.texttoolbar.actionmodecallback.TextActionModeCallback
import androidx.ui.geometry.Rect

/**
 * Android implementation for [TextToolbar].
 */
internal class AndroidTextToolbar(private val view: View) : TextToolbar {
    private var actionMode: ActionMode? = null
    private var textToolbarStatus = TextToolbarStatus.Hidden

    override fun showCopyMenu(
        rect: Rect,
        onDeselectRequested: () -> Unit,
        onCopyRequested: () -> Unit
    ) {
        textToolbarStatus = TextToolbarStatus.Shown
        if (Build.VERSION.SDK_INT >= 23) {
            val actionModeCallback =
                FloatingTextActionModeCallback(
                    TextActionModeCallback(
                        view = view,
                        onCopyRequested = onCopyRequested,
                        onDeselectRequested = onDeselectRequested
                    )
                )
            actionModeCallback.setRect(rect)
            actionMode = view.startActionMode(
                actionModeCallback,
                ActionMode.TYPE_FLOATING
            )
        } else {
            val actionModeCallback =
                PrimaryTextActionModeCallback(
                    TextActionModeCallback(
                        view = view,
                        onCopyRequested = onCopyRequested,
                        onDeselectRequested = onDeselectRequested
                    )
                )
            actionMode = view.startActionMode(actionModeCallback)
        }
    }

    override fun hide() {
        textToolbarStatus = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }

    override val status: TextToolbarStatus
        get() = textToolbarStatus
}
