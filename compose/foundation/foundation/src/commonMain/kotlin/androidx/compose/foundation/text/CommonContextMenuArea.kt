/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.contextmenu.close
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.internal.ProvideDefaultPlatformTextContextMenuProviders
import androidx.compose.foundation.text.contextmenu.modifier.showTextContextMenuOnSecondaryClick
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.input.internal.selection.contextMenuBuilder
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.selection.contextMenuBuilder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlin.jvm.JvmInline
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommonContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit,
) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        ProvideDefaultPlatformTextContextMenuProviders(manager.contextMenuAreaModifier, content)
    } else {
        val state = remember { ContextMenuState() }
        val coroutineScope = rememberCoroutineScope()
        val menuItemsAvailability = remember { mutableStateOf(MenuItemsAvailability.None) }

        androidx.compose.foundation.contextmenu.ContextMenuArea(
            state = state,
            onDismiss = { state.close() },
            contextMenuBuilderBlock = manager.contextMenuBuilder(state, menuItemsAvailability),
            enabled = manager.enabled,
            onOpenGesture = {
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    menuItemsAvailability.value = manager.getContextMenuItemsAvailability()
                }
            },
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommonContextMenuArea(
    selectionState: TextFieldSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        val modifier =
            if (enabled) {
                Modifier.showTextContextMenuOnSecondaryClick(
                    onPreShowContextMenu = { clickLocation ->
                        selectionState.updateClipboardEntry()
                        selectionState.platformSelectionBehaviors?.onShowContextMenu(
                            text = selectionState.textFieldState.visualText.text,
                            selection = selectionState.textFieldState.visualText.selection,
                            secondaryClickLocation = clickLocation,
                        )
                    }
                )
            } else {
                Modifier
            }
        ProvideDefaultPlatformTextContextMenuProviders(modifier, content)
    } else {
        val state = remember { ContextMenuState() }
        val coroutineScope = rememberCoroutineScope()
        val menuItemsAvailability = remember { mutableStateOf(MenuItemsAvailability.None) }
        val menuBuilder =
            selectionState.contextMenuBuilder(
                state = state,
                itemsAvailability = menuItemsAvailability,
                onMenuItemClicked = { item ->
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        when (item) {
                            TextContextMenuItems.Cut -> cut()
                            TextContextMenuItems.Copy -> copy(false)
                            TextContextMenuItems.Paste -> paste()
                            TextContextMenuItems.SelectAll -> selectAll()
                            TextContextMenuItems.Autofill -> autofill()
                        }
                    }
                },
            )

        androidx.compose.foundation.contextmenu.ContextMenuArea(
            state = state,
            onDismiss = { state.close() },
            contextMenuBuilderBlock = menuBuilder,
            enabled = enabled,
            onOpenGesture = {
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    menuItemsAvailability.value = selectionState.getContextMenuItemsAvailability()
                }
            },
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommonContextMenuArea(manager: SelectionManager, content: @Composable () -> Unit) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        ProvideDefaultPlatformTextContextMenuProviders(manager.contextMenuAreaModifier, content)
    } else {
        val state = remember { ContextMenuState() }
        androidx.compose.foundation.contextmenu.ContextMenuArea(
            state = state,
            onDismiss = { state.close() },
            contextMenuBuilderBlock = manager.contextMenuBuilder(state),
            content = content,
        )
    }
}

/** The default text context menu items. */
internal enum class TextContextMenuItems(
    val key: Any,
    val stringId: ContextMenuStrings,
    val drawableId: ContextMenuIcons,
) {
    Cut(
        key = TextContextMenuKeys.CutKey,
        stringId = ContextMenuStrings.Cut,
        drawableId = ContextMenuIcons.ActionModeCutDrawable,
    ),
    Copy(
        key = TextContextMenuKeys.CopyKey,
        stringId = ContextMenuStrings.Copy,
        drawableId = ContextMenuIcons.ActionModeCopyDrawable,
    ),
    Paste(
        key = TextContextMenuKeys.PasteKey,
        stringId = ContextMenuStrings.Paste,
        drawableId = ContextMenuIcons.ActionModePasteDrawable,
    ),
    SelectAll(
        key = TextContextMenuKeys.SelectAllKey,
        stringId = ContextMenuStrings.SelectAll,
        drawableId = ContextMenuIcons.ActionModeSelectAllDrawable,
    ),
    Autofill(
        key = TextContextMenuKeys.AutofillKey,
        stringId = ContextMenuStrings.Autofill,
        // Platform also doesn't have an icon for the autofill item.
        drawableId = ContextMenuIcons.ID_NULL,
    );

    @ReadOnlyComposable @Composable fun resolvedString(): String = getString(stringId)
}

internal inline fun ContextMenuScope.TextItem(
    state: ContextMenuState,
    label: TextContextMenuItems,
    enabled: Boolean,
    crossinline operation: () -> Unit,
) {
    // b/365619447 - instead of setting `enabled = enabled` in `item`,
    //  just remove the item from the menu.
    if (enabled) {
        item(label = { label.resolvedString() }) {
            operation()
            state.close()
        }
    }
}

internal suspend fun TextFieldSelectionState.getContextMenuItemsAvailability():
    MenuItemsAvailability {
    updateClipboardEntry()
    return MenuItemsAvailability(
        canCopy = canCopy(),
        canPaste = canPaste(),
        canCut = canCut(),
        canSelectAll = canSelectAll(),
        canAutofill = canAutofill(),
    )
}

internal suspend fun TextFieldSelectionManager.getContextMenuItemsAvailability():
    MenuItemsAvailability {
    updateClipboardEntry()
    return MenuItemsAvailability(
        canCopy = canCopy(),
        canPaste = canPaste(),
        canCut = canCut(),
        canSelectAll = canSelectAll(),
        canAutofill = canAutofill(),
    )
}

@JvmInline
internal value class MenuItemsAvailability private constructor(val value: Int) {
    constructor(
        canCopy: Boolean,
        canPaste: Boolean,
        canCut: Boolean,
        canSelectAll: Boolean,
        canAutofill: Boolean,
    ) : this(
        (if (canCopy) COPY else 0) or
            (if (canPaste) PASTE else 0) or
            (if (canCut) CUT else 0) or
            (if (canSelectAll) SELECT_ALL else 0) or
            (if (canAutofill) AUTO_FILL else 0)
    )

    companion object {
        private const val COPY = 0b0001
        private const val PASTE = 0b0010
        private const val CUT = 0b0100
        private const val SELECT_ALL = 0b1000
        private const val AUTO_FILL = 0b10000
        private const val NONE = 0

        val None = MenuItemsAvailability(NONE)
    }

    val canCopy
        get() = value and COPY == COPY

    val canPaste
        get() = value and PASTE == PASTE

    val canCut
        get() = value and CUT == CUT

    val canSelectAll
        get() = value and SELECT_ALL == SELECT_ALL

    val canAutofill
        get() = value and AUTO_FILL == AUTO_FILL
}
