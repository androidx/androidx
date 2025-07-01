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

import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.R
import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.contextmenu.close
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.internal.ProvideDefaultPlatformTextContextMenuProviders
import androidx.compose.foundation.text.contextmenu.modifier.textContextMenuGestures
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
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
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
internal actual fun ContextMenuArea(
    selectionState: TextFieldSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        val modifier =
            if (enabled) {
                Modifier.textContextMenuGestures(
                    onPreShowContextMenu = {
                        selectionState.updateClipboardEntry()
                        selectionState.platformSelectionBehaviors?.onShowContextMenu(
                            text = selectionState.textFieldState.visualText.text,
                            selection = selectionState.textFieldState.visualText.selection,
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
internal actual fun ContextMenuArea(manager: SelectionManager, content: @Composable () -> Unit) {
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

/**
 * The default text context menu items.
 *
 * @param stringId The android [android.R.string] id for the label of this item
 */
internal enum class TextContextMenuItems(val key: Any, val stringId: Int, val drawableId: Int) {
    Cut(
        key = TextContextMenuKeys.CutKey,
        stringId = android.R.string.cut,
        drawableId = android.R.attr.actionModeCutDrawable,
    ),
    Copy(
        key = TextContextMenuKeys.CopyKey,
        stringId = android.R.string.copy,
        drawableId = android.R.attr.actionModeCopyDrawable,
    ),
    Paste(
        key = TextContextMenuKeys.PasteKey,
        stringId = android.R.string.paste,
        drawableId = android.R.attr.actionModePasteDrawable,
    ),
    SelectAll(
        key = TextContextMenuKeys.SelectAllKey,
        stringId = android.R.string.selectAll,
        drawableId = android.R.attr.actionModeSelectAllDrawable,
    ),
    Autofill(
        key = TextContextMenuKeys.AutofillKey,
        stringId =
            if (Build.VERSION.SDK_INT <= 26) {
                R.string.autofill
            } else {
                android.R.string.autofill
            },
        // Platform also doesn't have an icon for the autofill item.
        drawableId = Resources.ID_NULL,
    );

    @ReadOnlyComposable @Composable fun resolvedString(): String = stringResource(stringId)

    fun resolveString(resources: Resources): String = resources.getString(stringId)
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

internal fun TextContextMenuBuilderScope.textItem(
    resources: Resources,
    item: TextContextMenuItems,
    enabled: Boolean,
    onClick: TextContextMenuSession.() -> Unit,
) {
    if (enabled) {
        item(
            key = item.key,
            label = item.resolveString(resources),
            leadingIcon = item.drawableId,
            onClick = onClick,
        )
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
