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

package androidx.compose.foundation.text

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.contextmenu.contextMenuGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.modifier.showTextContextMenuOnSecondaryClick
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.uikit.utils.CMPEditMenuView
import androidx.compose.ui.unit.Density
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGRectMake

/**
 * Context menu area for [BasicTextField] (with [TextFieldValue] argument).
 */
@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit
) {
    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        // The first time the menu is called up, the menu item provider contains a non-final set of
        // menu items, which causes the context menu callout to blink.
        // Adding a small delay resolves this issue.
        ProvideNewContextMenuDefaultProviders(
            menuDelay = 100.milliseconds,
            modifier = manager.contextMenuAreaModifier,
            content = content
        )
    } else {
        content()
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
        val modifier = if (enabled) {
            Modifier.showTextContextMenuOnSecondaryClick(
                onPreShowContextMenu = { selectionState.updateClipboardEntry() }
            )
        } else {
            Modifier
        }
        ProvideNewContextMenuDefaultProviders(
            modifier = modifier,
            content = content
        )
    } else {
        val scope = rememberCoroutineScope()
        PlatformContextMenu(
            getState = { selectionState.contextMenuItemsState(scope) },
            enabled = enabled,
            content = content
        )
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
        ProvideNewContextMenuDefaultProviders(
            modifier = manager.contextMenuAreaModifier,
            content = content
        )
    } else {
        PlatformContextMenu(
            getState = { manager.contextMenuItemsState() },
            content = content
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProvideNewContextMenuDefaultProviders(
    menuDelay: Duration = 0.seconds,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val toolbarProvider = LocalTextContextMenuToolbarProvider.current
    val dropdownProvider = LocalTextContextMenuDropdownProvider.current
    if (toolbarProvider == null || dropdownProvider == null) {
        val layoutCoordinates: MutableState<LayoutCoordinates?> = remember {
            mutableStateOf(null, neverEqualPolicy())
        }

        val density = LocalDensity.current
        val provider = remember {
            val editMenuView = CMPEditMenuView().also {
                it.userInteractionEnabled = false
            }

            ContextMenuToolbarProvider(
                menuDelay = menuDelay,
                editMenuView = editMenuView,
                density = density,
                coordinates = { layoutCoordinates.value }
            )
        }

        CompositionLocalProvider(
            LocalTextContextMenuToolbarProvider providesDefault provider,
            LocalTextContextMenuDropdownProvider providesDefault provider,
            content = {
                Box(
                    modifier = modifier.onGloballyPositioned { layoutCoordinates.value = it }
                        .then(ContextMenuLayoutElement(provider.editMenuView)),
                    propagateMinConstraints = true
                ) {
                    content()
                }
            }
        )
    } else {
        Box(modifier = modifier, propagateMinConstraints = true) {
            content()
        }
    }
}

private class ContextMenuItemsState(
    val copy: (() -> Unit)?,
    val paste: (() -> Unit)?,
    val cut: (() -> Unit)?,
    val selectAll: (() -> Unit)?,
    val rect: Rect
)

private class ContextMenuToolbarProvider(
    private val menuDelay: Duration,
    val editMenuView: CMPEditMenuView,
    private val density: Density,
    private val coordinates: () -> LayoutCoordinates?
): TextContextMenuProvider {
    @OptIn(FlowPreview::class)
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        var session: TextContextMenuSession? = null
        coroutineScope {
            val job = launch {
                delay(menuDelay)
                snapshotFlow {
                    val layoutCoordinates = coordinates() ?: return@snapshotFlow null

                    val layoutPosition = layoutCoordinates.positionInWindow()
                    val layoutBounds = layoutCoordinates.boundsInWindow()

                    val rect = dataProvider.contentBounds(layoutCoordinates)
                        .translate(layoutPosition - layoutBounds.topLeft)

                    val components = dataProvider.data().components

                    fun actionItem(key: Any): (() -> Unit)? {
                        val item = components.firstOrNull { it.key == key } ?: return null
                        if (item !is TextContextMenuItem) return null
                        if (!item.enabled) return null

                        return {
                            with(item) {
                                session?.onClick()
                            }
                        }
                    }

                    ContextMenuItemsState(
                        copy = actionItem(TextContextMenuKeys.CopyKey),
                        paste = actionItem(TextContextMenuKeys.PasteKey),
                        cut = actionItem(TextContextMenuKeys.CutKey),
                        selectAll = actionItem(TextContextMenuKeys.SelectAllKey),
                        rect = rect
                    )
                }.filterNotNull().collect {
                    editMenuView.showEditMenuAtRect(
                        targetRect = it.rect.toCGRect(density),
                        copy = it.copy,
                        cut = it.cut,
                        paste = it.paste,
                        selectAll = it.selectAll
                    )
                }
            }

            suspendCancellableCoroutine { continuation ->
                session = TextContextMenuSessionImpl(editMenuView, continuation)
                continuation.invokeOnCancellation {
                    editMenuView.hideEditMenu()
                }
            }
            job.cancel()
        }
    }
}

@Composable
private fun PlatformContextMenu(
    getState: () -> ContextMenuItemsState,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val layoutCoordinates: MutableState<LayoutCoordinates?> = remember {
        mutableStateOf(value = null, policy = neverEqualPolicy())
    }
    val editMenuView = remember {
        CMPEditMenuView().also {
            it.userInteractionEnabled = false
        }
    }

    val modifier = if (enabled) {
        val density = LocalDensity.current
        Modifier.onGloballyPositioned {
            layoutCoordinates.value = it
        }.contextMenuGestures { offset ->
            val coordinates = layoutCoordinates.value ?: return@contextMenuGestures
            val layoutPosition = coordinates.positionInWindow()
            val layoutBounds = coordinates.boundsInWindow()

            val state = getState()
            val rect = CGRectMake(
                x = with(density) {
                    (offset.x + layoutPosition.x - layoutBounds.left).toDp().value.toDouble()
                },
                y = with(density) {
                    (offset.y + layoutPosition.y - layoutBounds.top).toDp().value.toDouble()
                },
                width = 1.0,
                height = 1.0
            )
            editMenuView.showEditMenuAtRect(
                rect,
                state.copy,
                state.cut,
                state.paste,
                state.selectAll
            )
        }
    } else {
        Modifier
    }
    Box(
        modifier = modifier.then(ContextMenuLayoutElement(editMenuView)),
        propagateMinConstraints = true,
    ) {
        content()
    }
}

private fun TextFieldSelectionState.contextMenuItemsState(scope: CoroutineScope): ContextMenuItemsState {
    return ContextMenuItemsState(
        copy = if (canCopy()) {
            { scope.launch { copy(cancelSelection = true) } }
        } else {
            null
        },
        paste = if (canPaste()) {
            { scope.launch { paste() } }
        } else {
            null
        },
        cut = if (canCut()) {
            { scope.launch { cut() } }
        } else {
            null
        },
        selectAll = if (canSelectAll()) {
            { selectAll() }
        } else {
            null
        },
        rect = Rect.Zero
    )
}

private fun SelectionManager.contextMenuItemsState(): ContextMenuItemsState {
    return ContextMenuItemsState(
        copy = if (isNonEmptySelection()) {
            { copy() }
        } else {
            null
        },
        paste = null,
        cut = null,
        selectAll = if (!isEntireContainerSelected()) {
            { selectAll() }
        } else {
            null
        },
        rect = Rect.Zero
    )
}

private class TextContextMenuSessionImpl(
    val editMenuView: CMPEditMenuView,
    val continuation: CancellableContinuation<Unit>
) : TextContextMenuSession {
    override fun close() {
        editMenuView.hideEditMenu()
        if (continuation.isActive) {
            continuation.resume(Unit)
        }
    }
}
