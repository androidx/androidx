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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuComponent
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
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
import androidx.compose.ui.uikit.utils.CMPEditMenuCustomAction
import androidx.compose.ui.unit.Density
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UIView

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
        content()
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
            menuDelay = 100.milliseconds,
            modifier = manager.contextMenuAreaModifier,
            content = content
        )
    } else {
        content()
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
    val customActions: List<CMPEditMenuCustomAction>,
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

                    var copy: (() -> Unit)? = null
                    var paste: (() -> Unit)? = null
                    var cut: (() -> Unit)? = null
                    var selectAll: (() -> Unit)? = null
                    val customActions = mutableListOf<CMPEditMenuCustomAction>()

                    fun actionItem(component: TextContextMenuComponent): (() -> Unit)? {
                        val item = component as? TextContextMenuItemWithComposableLeadingIcon
                            ?: return null
                        if (!item.enabled) return null

                        return {
                            with(item) {
                                session?.onClick()
                            }
                        }
                    }

                    dataProvider.data().components.forEach { component ->
                        when (component.key) {
                            TextContextMenuKeys.CopyKey -> copy = actionItem(component)
                            TextContextMenuKeys.PasteKey -> paste = actionItem(component)
                            TextContextMenuKeys.SelectAllKey -> selectAll = actionItem(component)
                            TextContextMenuKeys.CutKey -> cut = actionItem(component)
                            else -> {
                                if (component is TextContextMenuItemWithComposableLeadingIcon &&
                                    component.enabled
                                ) {
                                    val actionItem = actionItem(component)
                                    if (actionItem != null) {
                                        customActions.add(
                                            CMPEditMenuCustomAction(
                                                title = component.label,
                                                action = actionItem
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ContextMenuItemsState(
                        copy = copy,
                        paste = paste,
                        cut = cut,
                        selectAll = selectAll,
                        customActions = customActions,
                        rect = rect
                    )
                }.filterNotNull().collect {
                    getEditMenuView().showEditMenuAtRect(
                        targetRect = it.rect.toCGRect(density),
                        copy = it.copy,
                        cut = it.cut,
                        paste = it.paste,
                        selectAll = it.selectAll,
                        customActions = it.customActions
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

    private fun getEditMenuView(): CMPEditMenuView {
        if (available(OS.Ios to OSVersion(16))) {
            return editMenuView
        } else {
            // HACK: On iOS < 16 it's required for UIMenuController to make target view a first
            // responder. If the keyboard is shown with IntermediateTextInputUIView, this will cause
            // the keyboard to hide.
            // To fix the problem, we're looking for the active IntermediateTextInputUIView in
            // UIVIew hierarchy and use it to show the menu.
            fun findEditMenuViewRecursively(view: UIView?): CMPEditMenuView? {
                if (view is CMPEditMenuView) {
                    return view
                }
                view?.subviews?.forEach {
                    if (it is UIView) {
                        val editMenuView = findEditMenuViewRecursively(it)
                        if (editMenuView != null && editMenuView.isFirstResponder()) {
                            return editMenuView
                        }
                    }
                }
                return null
            }
            return findEditMenuViewRecursively(editMenuView.superview) ?: editMenuView
        }
    }
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
