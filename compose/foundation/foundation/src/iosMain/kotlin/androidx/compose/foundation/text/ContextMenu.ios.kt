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
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.modifier.collectTextContextMenuData
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.UIKitNativeTextInputContextMenuCustomAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.UIKitNativeTextInputContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.uikit.LocalNativeTextInputContext
import androidx.compose.ui.uikit.utils.CMPEditMenuView
import androidx.compose.ui.uikit.utils.CMPEditMenuCustomAction
import androidx.compose.ui.unit.Density
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
@OptIn(InternalComposeUiApi::class)
@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit
) {
    val nativeTextInputContext = LocalNativeTextInputContext.current

    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        val selectionProvider = remember(manager) {
            { manager.value.selection }
        }
        val onSelectionChanged: (TextContextMenuData) -> Unit = remember(manager, nativeTextInputContext) {
            { contextMenuData ->
                notifyAboutContextMenuItems(
                    nativeTextInputContext,
                    contextMenuData
                )
            }
        }
        val nativeContextMenuUpdaterModifier = NativeTextInputContextMenuUpdaterElement(
            context = nativeTextInputContext,
            selectionProvider = selectionProvider,
            onSelectionChanged = onSelectionChanged
        )

        // The first time the menu is called up, the menu item provider contains a non-final set of
        // menu items, which causes the context menu callout to blink.
        // Adding a small delay resolves this issue.
        ProvideNewContextMenuDefaultProviders(
            isNativeTextInputProvider = { nativeTextInputContext.usingNativeTextInput() },
            menuDelay = 100.milliseconds,
            modifier = manager.contextMenuAreaModifier then nativeContextMenuUpdaterModifier,
            content = content
        )
    } else {
        content()
        startNotifyingAboutContextMenuItems(manager, nativeTextInputContext)
    }
}

/**
 * Context menu area for [BasicTextField] (with [TextFieldState] argument).
 */
@OptIn(InternalComposeUiApi::class)
@Composable
internal actual fun ContextMenuArea(
    selectionState: TextFieldSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val nativeTextInputContext = LocalNativeTextInputContext.current

    if (ComposeFoundationFlags.isNewContextMenuEnabled) {
        val selectionProvider = remember(selectionState) {
            { selectionState.textFieldState.visualText.selection }
        }
        val onSelectionChanged: (TextContextMenuData) -> Unit = remember(selectionState, nativeTextInputContext) {
            { contextMenuData ->
                notifyAboutContextMenuItems(
                    nativeTextInputContext,
                    contextMenuData
                )
            }
        }
        val nativeContextMenuUpdaterModifier = NativeTextInputContextMenuUpdaterElement(
            context = nativeTextInputContext,
            selectionProvider = selectionProvider,
            onSelectionChanged = onSelectionChanged
        )

        val modifier = if (enabled) {
            Modifier.showTextContextMenuOnSecondaryClick(
                onPreShowContextMenu = { selectionState.updateClipboardEntry() }
            )
        } else {
            Modifier
        } then nativeContextMenuUpdaterModifier
        ProvideNewContextMenuDefaultProviders(
            isNativeTextInputProvider = { nativeTextInputContext.usingNativeTextInput() },
            modifier = modifier,
            content = content
        )
    } else {
        content()
        startNotifyingAboutContextMenuItems(selectionState, nativeTextInputContext)
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
    // We should adopt the native iOS text input approach for non-editable containers as well
    // https://youtrack.jetbrains.com/issue/CMP-9733/Adopt-NITI-approach-to-the-Selection-Container
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

@Composable
private fun ProvideNewContextMenuDefaultProviders(
    isNativeTextInputProvider: () -> Boolean = { false },
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

            // Native Text Input flag is being set during startInput(), which is being called later than creating this provider,
            // so we need to forward it here to prevent showing several context menus
            // And that's why we can't hide creating editMenuView under Native Text Input flag
            ContextMenuToolbarProvider(
                isNativeTextInputProvider = isNativeTextInputProvider,
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

@OptIn(InternalComposeUiApi::class)
private class ContextMenuItemsState(
    val copy: (() -> Unit)?,
    val paste: (() -> Unit)?,
    val cut: (() -> Unit)?,
    val selectAll: (() -> Unit)?,
    val customActions: List<UIKitNativeTextInputContextMenuCustomAction>,
    val rect: Rect? = null
)

private class ContextMenuToolbarProvider(
    private val isNativeTextInputProvider: () -> Boolean,
    private val menuDelay: Duration,
    val editMenuView: CMPEditMenuView,
    private val density: Density,
    private val coordinates: () -> LayoutCoordinates?
): TextContextMenuProvider {

@OptIn(FlowPreview::class, InternalComposeUiApi::class)
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        var session: TextContextMenuSession? = null
        coroutineScope {
            val job = launch {
                delay(menuDelay)
                snapshotFlow {
                    if (isNativeTextInputProvider()) return@snapshotFlow null
                    val layoutCoordinates = coordinates() ?: return@snapshotFlow null

                    val layoutPosition = layoutCoordinates.positionInWindow()
                    val layoutBounds = layoutCoordinates.boundsInWindow()

                    val rect = dataProvider.contentBounds(layoutCoordinates)
                        .translate(layoutPosition - layoutBounds.topLeft)

                    // Without this, we would have two conflicting context menus:
                    // one native (updating as intended by iOS), one compose (updating by click, which can have outdated state for the Native Text Input scenario)
                    // So explicit filtration is required here
                    buildContextMenuItemsState(rect, dataProvider.data(), session)
                }
                    .filterNotNull()
                    .collect {
                        getEditMenuView().showEditMenuAtRect(
                            targetRect = (it.rect ?: Rect.Zero).toCGRect(density),
                            copy = it.copy,
                            cut = it.cut,
                            paste = it.paste,
                            selectAll = it.selectAll,
                            customActions = it.customActions.map { action ->
                                CMPEditMenuCustomAction(action.title, action.action)
                            }
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

@OptIn(InternalComposeUiApi::class)
private fun buildContextMenuItemsState(
    calculatedRect: Rect?,
    data: TextContextMenuData,
    session: TextContextMenuSession?
): ContextMenuItemsState {
    var copy: (() -> Unit)? = null
    var paste: (() -> Unit)? = null
    var cut: (() -> Unit)? = null
    var selectAll: (() -> Unit)? = null
    val customActions = mutableListOf<UIKitNativeTextInputContextMenuCustomAction>()

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

    data.components.forEach { component ->
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
                            UIKitNativeTextInputContextMenuCustomAction(
                                title = component.label,
                                action = actionItem
                            )
                        )
                    }
                }
            }
        }
    }

    return ContextMenuItemsState(
        copy = copy,
        paste = paste,
        cut = cut,
        selectAll = selectAll,
        customActions = customActions,
        rect = calculatedRect
    )
}

@OptIn(InternalComposeUiApi::class)
private data class NativeTextInputContextMenuUpdaterElement(
    val context: UIKitNativeTextInputContext,
    val selectionProvider: () -> TextRange,
    val onSelectionChanged: (TextContextMenuData) -> Unit
): ModifierNodeElement<NativeTextInputContextMenuUpdaterNode>() {
    override fun create(): NativeTextInputContextMenuUpdaterNode =
        NativeTextInputContextMenuUpdaterNode(context, selectionProvider, onSelectionChanged)

    override fun update(node: NativeTextInputContextMenuUpdaterNode) {
        val restartRequired = node.context != context ||
            node.selectionProvider !== selectionProvider ||
            node.onSelectionChanged !== onSelectionChanged

        if (restartRequired) {
            node.context = context
            node.selectionProvider = selectionProvider
            node.onSelectionChanged = onSelectionChanged
            node.restartObserving()
        }
    }
}

@OptIn(InternalComposeUiApi::class)
private class NativeTextInputContextMenuUpdaterNode(
    var context: UIKitNativeTextInputContext,
    var selectionProvider: () -> TextRange,
    var onSelectionChanged: (TextContextMenuData) -> Unit
): DelegatingNode() {
    private var job: Job? = null

    override fun onAttach() {
        startObserving()
    }

    override fun onDetach() {
        stopObserving()
    }

    private fun startObserving() {
        job = coroutineScope.launch {
            snapshotFlow {
                if (context.usingNativeTextInput()) selectionProvider() else null
            }
                .filterNotNull()
                .collect {
                    onSelectionChanged(collectTextContextMenuData())
                }
        }
    }

    private fun stopObserving() {
        job?.cancel()
        job = null
    }

    fun restartObserving() {
        stopObserving()
        startObserving()
    }
}

/**
 * Starts notifying the native iOS input system about the available context menu items (isNewContextMenu = true)
 * in both [BasicTextField]s (with [TextFieldState] and [TextFieldValue])
 *
 * @param nativeTextInputContext The context of the native text input in UIKit to interact with for updates.
 * @param contextMenuData Data for building the context menu items to display.
 */
@OptIn(InternalComposeUiApi::class)
private fun notifyAboutContextMenuItems(
    nativeTextInputContext: UIKitNativeTextInputContext,
    contextMenuData: TextContextMenuData
) {
    // Native text input shouldn't require TextContextMenuSessionImpl,
    // because native text input doesn't use CMPEditMenuView
    // However, empty implementation should be passed because menu items aren't being invoked without it
    val nativeTextInputTextMenuSession = object : TextContextMenuSession {
        override fun close() {}
    }
    val contextMenuItemsState = buildContextMenuItemsState(null, contextMenuData, nativeTextInputTextMenuSession)
    nativeTextInputContext.updateEditMenuState(contextMenuItemsState)
}

@OptIn(InternalComposeUiApi::class)
@Composable
private fun startObservingSelectionChanges(
    context: UIKitNativeTextInputContext,
    selectionProvider: () -> TextRange,
    onSelectionChanged: () -> Unit
) {
    LaunchedEffect(selectionProvider) {
        snapshotFlow { if (context.usingNativeTextInput()) selectionProvider() else null }
            .filterNotNull()
            .collect {
                onSelectionChanged()
            }
    }
}

/**
 * Starts notifying the native iOS input system about the available context menu items (isNewContextMenu = false) in [BasicTextField] (with [TextFieldValue] argument)
 *
 * @param manager The manager responsible for tracking and controlling the text field selection.
 * @param nativeTextInputContext The native text input context used to update the state of the context menu
 *                     and provide actions such as copy, paste, cut, and select all.
 */
@OptIn(InternalComposeUiApi::class)
@Composable
private fun startNotifyingAboutContextMenuItems(
    manager: TextFieldSelectionManager,
    nativeTextInputContext: UIKitNativeTextInputContext,
) {
    startObservingSelectionChanges(
        nativeTextInputContext,
        selectionProvider = { manager.value.selection },
        onSelectionChanged = {
            nativeTextInputContext.updateNativeTextInputEditMenuState(
                copy = if (manager.isCopyAllowed()) ({ manager.copy(cancelSelection = false) }) else null,
                paste = if (manager.canShowPasteMenuItem()) ({ manager.paste() }) else null,
                cut = if (manager.canShowCutMenuItem()) ({ manager.cut() }) else null,
                selectAll = if (manager.canShowSelectAllMenuItem()) ({ manager.selectAll() }) else null,
                customActions = emptyList()
            )
        })
}

/**
 * Starts notifying the native iOS input system about the available context menu items (isNewContextMenu = false) in [BasicTextField] (with [TextFieldState] argument)
 *
 * @param selectionState The current state of the text field selection, including selection bounds
 * and related actions.
 * @param nativeTextInputContext The UIKitNativeTextInputContext instance used to update the edit menu state
 * with actions.
 */
@OptIn(InternalComposeUiApi::class)
@Composable
private fun startNotifyingAboutContextMenuItems(
    selectionState: TextFieldSelectionState,
    nativeTextInputContext: UIKitNativeTextInputContext,
) {
    // this should be the same scope as at the root of BasicTextField
    val coroutineScope = rememberCoroutineScope()
    startObservingSelectionChanges(
        nativeTextInputContext,
        selectionProvider = { selectionState.textFieldState.visualText.selection },
        onSelectionChanged = {
            val copyBlock: () -> Unit =
                { coroutineScope.launch { selectionState.copy(cancelSelection = false) } }
            val pasteBlock: () -> Unit = { coroutineScope.launch { selectionState.paste() } }
            val cutBlock: () -> Unit = { coroutineScope.launch { selectionState.cut() } }
            val selectAllBlock: () -> Unit = {
                coroutineScope.launch {
                    selectionState.selectAll()
                }
            }

            nativeTextInputContext.updateNativeTextInputEditMenuState(
                copy = if (selectionState.canShowCopyMenuItem()) (copyBlock) else null,
                paste = if (selectionState.canShowPasteMenuItem()) (pasteBlock) else null,
                cut = if (selectionState.canShowCutMenuItem()) (cutBlock) else null,
                selectAll = if (selectionState.canShowSelectAllMenuItem()) (selectAllBlock) else null,
                customActions = emptyList()
            )
        }
    )
}


@OptIn(InternalComposeUiApi::class)
private fun UIKitNativeTextInputContext.updateEditMenuState(state: ContextMenuItemsState) =
    updateNativeTextInputEditMenuState(
        copy = state.copy,
        paste = state.paste,
        cut = state.cut,
        selectAll = state.selectAll,
        customActions = state.customActions
    )
