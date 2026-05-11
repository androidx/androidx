/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.text.input

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.utils.CMPEditMenuCustomAction
import androidx.compose.ui.unit.toCGRect
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.ComposeTextInputView
import androidx.compose.ui.window.FocusedViewsList
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth

internal open class ComposeTextInputConnection(
    updateView: () -> Unit,
    view: UIView,
    coroutineScope: CoroutineScope,
    viewConfiguration: ViewConfiguration,
    focusedViewsList: FocusedViewsList?,
    onKeyboardPresses: (Set<*>) -> Unit,
    focusManager: () -> ComposeSceneFocusManager?
) : TextInputConnection(
    updateView,
    view,
    coroutineScope,
    focusedViewsList,
    onKeyboardPresses,
    focusManager
), TextToolbar {
    // Fixes a problem where the menu is shown before the textInputView gets its final layout.
    private var showMenuOrUpdatePosition = {}

    override val textInputView =
        ComposeTextInputView(
            doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis,
        ).also {
            it.setAutoresizingMask(
                UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            )
        }

    override fun attachInputToView() {
        view.addSubview(textInputView)
        textInputView.setFrame(view.bounds)

        textInputView.input = this
        onViewGeometryUpdated()
    }

    override fun detachView() {
        // Out-of-bounds non-empty frame is required to hide text keyboard focus frame
        val outOfBoundsFrame = CGRectMake(-100000.0, 0.0, 1.0, 1.0)

        textInputView.input = null

        showMenuOrUpdatePosition = {}
        textInputView.let { view ->
            view.setFrame(outOfBoundsFrame)
            coroutineScope.launch {
                delay(CLEAR_FOCUS_DELAY)
                view.removeFromSuperview()
            }
        }
    }

    override fun stateWillChange(textChanged: Boolean, selectionChanged: Boolean) {
        if (textChanged) {
            textInputView.textWillChange()
        }
        if (selectionChanged) {
            textInputView.selectionWillChange()
        }
    }

    override fun stateDidChange(textChanged: Boolean, selectionChanged: Boolean) {
        if (textChanged) {
            textInputView.textDidChange()
        }
        if (selectionChanged) {
            textInputView.selectionDidChange()
        }
    }

    override fun onViewGeometryUpdated() {
        val rect = textFieldRectInRoot ?: return
        textInputView.setFrame(rect.toDpRect(view.density).toCGRect())
        showMenuOrUpdatePosition()
    }

    override val status: TextToolbarStatus
        get() = if (textInputView.isTextMenuShown()) {
            TextToolbarStatus.Shown
        } else {
            TextToolbarStatus.Hidden
        }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        showMenuOrUpdatePosition = {
            textInputView.let { textInputView ->
                val density = view.density
                val offset = textInputView.frame.useContents { origin.toDpOffset().toOffset(density) }
                val target = rect.translate(-offset).toDpRect(density).toCGRect()
                textInputView.showEditMenuAtRect(
                    targetRect = target,
                    copy = onCopyRequested,
                    cut = onCutRequested,
                    paste = onPasteRequested,
                    selectAll = onSelectAllRequested,
                    customActions = emptyList<CMPEditMenuCustomAction>()
                )
                textMenuAppearanceChanged()
            }
        }
        showMenuOrUpdatePosition()
    }


    override fun hide() {
        showMenuOrUpdatePosition = {}
        textInputView.let {
            it.hideTextMenu()
            textMenuAppearanceChanged()
        }
    }
}
