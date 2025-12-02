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
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.JPopupContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.contextMenuOpenDetector
import androidx.compose.foundation.internal.nativeClipboardHasText
import androidx.compose.foundation.text.TextContextMenu.TextManager
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.internal.ProvideDefaultPlatformTextContextMenuProviders
import androidx.compose.foundation.text.contextmenu.modifier.showTextContextMenuOnSecondaryClick
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.getSelectedText
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.platform.PlatformLocalization
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import java.awt.Component
import javax.swing.JPopupMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Context menu area for [BasicTextField] (with [TextFieldValue] argument).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit
) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        ProvideDefaultPlatformTextContextMenuProviders(manager.contextMenuAreaModifier, content)
    } else {
        val state = remember { ContextMenuState() }
        val textManager = remember(manager) { manager.textManager }
        LocalTextContextMenu.current.Area(textManager, state, content)
    }
}

/**
 * Context menu area for [BasicTextField] (with [TextFieldState] argument).
 */
@Composable
internal actual fun ContextMenuArea(
    selectionState: TextFieldSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        val modifier =
            if (enabled) {
                Modifier.showTextContextMenuOnSecondaryClick(
                    onPreShowContextMenu = { selectionState.updateClipboardEntry() }
                )
            } else {
                Modifier
            }
        ProvideDefaultPlatformTextContextMenuProviders(modifier, content)
    } else {
        if (!enabled) {
            content()
            return
        }

        val state = remember { ContextMenuState() }
        val coroutineScope = rememberCoroutineScope()
        val textManager = remember(selectionState, coroutineScope) {
            selectionState.textManager(coroutineScope)
        }
        LocalTextContextMenu.current.Area(textManager, state, content)
    }
}

/**
 * Context menu area for [SelectionContainer].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
    manager: SelectionManager,
    content: @Composable () -> Unit
) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        ProvideDefaultPlatformTextContextMenuProviders(manager.contextMenuAreaModifier, content)
    } else {
        val state = remember { ContextMenuState() }
        val textManager = remember(manager) { manager.textManager }
        LocalTextContextMenu.current.Area(textManager, state, content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val TextFieldSelectionManager.textManager: TextManager get() = object : TextManager {
    override val selectedText get() = value.getSelectedText()

    val isPassword get() = visualTransformation is PasswordVisualTransformation

    override val cut: (() -> Unit)? get() =
        if (!value.selection.collapsed && editable && !isPassword) {
            {
                cut()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val copy: (() -> Unit)? get() =
        if (!value.selection.collapsed && !isPassword) {
            {
                copy(false)
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val paste: (() -> Unit)? get() =
        if (editable && clipboard?.nativeClipboardHasText() == true) {
            {
                paste()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val selectAll: (() -> Unit)? get() =
        if (value.selection.length != value.text.length) {
            {
                selectAll()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override fun selectWordAtPositionIfNotAlreadySelected(offset: Offset) {
        this@textManager.selectWordAtPositionIfNotAlreadySelected(offset)
    }
}

private fun TextFieldSelectionState.textManager(coroutineScope: CoroutineScope): TextManager {
    return object : TextManager {

        override val selectedText
            get() = AnnotatedString(textFieldState.visualText.getSelectedText().toString())

        private fun launchUndispatched(block: suspend CoroutineScope.() -> Unit) {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED, block = block)
        }

        private fun cutImpl() = launchUndispatched { cut() }

        private fun copyImpl() = launchUndispatched { copy() }

        private fun pasteImpl() = launchUndispatched { paste() }

        override val cut: (() -> Unit)?
            get() = if (canShowCutMenuItem()) ::cutImpl else null

        override val copy: (() -> Unit)?
            get() = if (canShowCopyMenuItem()) ::copyImpl else null

        override val paste: (() -> Unit)?
            get() {
                launchUndispatched { updateClipboardEntry() }
                return if (canShowPasteMenuItem()) ::pasteImpl else null
            }

        override val selectAll: (() -> Unit)?
            get() = if (canShowSelectAllMenuItem()) ::selectAll else null

        override fun selectWordAtPositionIfNotAlreadySelected(offset: Offset) {
            if (!textLayoutState.isPositionOnText(offset)) return
            val index = textLayoutState.getOffsetForPosition(offset, coerceInVisibleBounds = false)
            if (index == -1) return

            if (index !in textFieldState.visualText.selection) {
                val selection = updateSelection(
                    textFieldCharSequence = textFieldState.visualText,
                    startOffset = index,
                    endOffset = index,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.Word,
                )
                textFieldState.selectCharsIn(selection)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val SelectionManager.textManager: TextManager get() = object : TextManager {
    override val selectedText get() = getSelectedText() ?: AnnotatedString("")
    override val cut = null
    override val copy = { copy() }
    override val paste = null
    override val selectAll = null
    override fun selectWordAtPositionIfNotAlreadySelected(offset: Offset) {
        this@textManager.selectWordAtPositionIfNotAlreadySelected(offset)
    }
}

/**
 * Composition local that keeps [TextContextMenu].
 */
@ExperimentalFoundationApi
val LocalTextContextMenu:
    ProvidableCompositionLocal<TextContextMenu> = staticCompositionLocalOf { TextContextMenu.Default }

/**
 * Describes how to show the text context menu for selectable texts and text fields.
 */
@ExperimentalFoundationApi
interface TextContextMenu {
    /**
     * Defines an area, that describes how to open and show text context menus.
     * Usually it uses [ContextMenuArea] as the implementation.
     * Note that it's up to the [Area] implementation to trigger the opening of the context menu on
     * the appropriate user events (e.g. right-click).
     *
     * @param textManager Provides useful methods and information for text for which we show the
     * text context menu.
     * @param state [ContextMenuState] of menu controlled by this area.
     * @param content The content of the [ContextMenuArea].
     */
    @Composable
    fun Area(textManager: TextManager, state: ContextMenuState, content: @Composable () -> Unit)

    /**
     * Provides useful methods and information for text for which we show the text context menu.
     */
    @ExperimentalFoundationApi
    interface TextManager {
        /**
         * The current selected text.
         */
        val selectedText: AnnotatedString

        /**
         * Action for cutting the selected text to the clipboard. Null if there is no text to cut.
         */
        val cut: (() -> Unit)?

        /**
         * Action for copy the selected text to the clipboard. Null if there is no text to copy.
         */
        val copy: (() -> Unit)?

        /**
         * Action for pasting text from the clipboard. Null if there is no text in the clipboard.
         */
        val paste: (() -> Unit)?

        /**
         * Action for selecting the whole text. Null if the text is already selected.
         */
        val selectAll: (() -> Unit)?

        /**
         * Selects the word at the given [offset], unless the current selection already encompasses
         * that position.
         */
        fun selectWordAtPositionIfNotAlreadySelected(offset: Offset)
    }

    companion object {
        /**
         * [TextContextMenu] that is used by default in Compose.
         */
        @ExperimentalFoundationApi
        val Default = object : TextContextMenu {
            @Composable
            override fun Area(textManager: TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
                val localization = LocalLocalization.current
                val items = {
                    listOfNotNull(
                        textManager.cut?.let {
                            ContextMenuItem(localization.cut, it)
                        },
                        textManager.copy?.let {
                            ContextMenuItem(localization.copy, it)
                        },
                        textManager.paste?.let {
                            ContextMenuItem(localization.paste, it)
                        },
                        textManager.selectAll?.let {
                            ContextMenuItem(localization.selectAll, it)
                        },
                    )
                }

                TextContextMenuArea(
                    textManager = textManager,
                    items = items,
                    state = state,
                    content = content
                )
            }
        }
    }
}

/**
 * [TextContextMenu] that uses [JPopupMenu] to show the text context menu.
 *
 * You can use it by overriding [TextContextMenu] on the top level of your application.
 *
 * @param owner The root component that owns a context menu. Usually it is [ComposeWindow] or [ComposePanel].
 * @param createMenu Describes how to create [JPopupMenu] from [TextManager] and from list of custom [ContextMenuItem]
 * defined by [CompositionLocalProvider].
 */
@ExperimentalFoundationApi
class JPopupTextMenu(
    private val owner: Component,
    private val createMenu: (TextManager, List<ContextMenuItem>) -> JPopupMenu,
) : TextContextMenu {
    @Composable
    override fun Area(textManager: TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalContextMenuRepresentation provides JPopupContextMenuRepresentation(owner) {
                createMenu(textManager, it)
            }
        ) {
            TextContextMenuArea(
                textManager = textManager,
                // We pass emptyList, but it will be merged with the other custom items defined via
                // ContextMenuDataProvider, and passed to createMenu
                items = { emptyList() },
                state = state,
                content = content
            )
        }
    }
}

/**
 * The context menu area for textual content.
 *
 * @param textManager The [TextManager] associated with the area.
 * @param items List of context menu items. Final context menu contains all items from descendant
 * [ContextMenuArea] and [ContextMenuDataProvider].
 * @param state The [ContextMenuState] whose opening to trigger.
 * @param content The content of the [ContextMenuArea].
 */
@ExperimentalFoundationApi
@Composable
fun TextContextMenuArea(
    textManager: TextManager,
    items: () -> List<ContextMenuItem>,
    state: ContextMenuState,
    content: @Composable () -> Unit
) {
    ContextMenuArea(
        items = items,
        state = state,
        modifier = Modifier.contextMenuOpenDetector(
            key = Pair(textManager, state)
        ) { pointerPosition ->
            if (DesktopPlatform.Current == DesktopPlatform.MacOS) {
                textManager.selectWordAtPositionIfNotAlreadySelected(pointerPosition)
            }
            state.status = ContextMenuState.Status.Open(Rect(pointerPosition, 0f))
        },
        content = content
    )
}

/**
 * The default text context menu items.
 *
 * @param label The label of this item
 */
internal enum class DesktopTextContextMenuItems(val key: Any, val label: (PlatformLocalization) -> String) {
    Cut(
        key = TextContextMenuKeys.CutKey,
        label = { it.cut },
    ),
    Copy(
        key = TextContextMenuKeys.CopyKey,
        label = { it.copy },
    ),
    Paste(
        key = TextContextMenuKeys.PasteKey,
        label = { it.paste },
    ),
    SelectAll(
        key = TextContextMenuKeys.SelectAllKey,
        label = { it.selectAll },
    )
}
