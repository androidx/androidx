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

package androidx.pdf.compose.annotation.toolbar

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.viewinterop.AndroidView
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.view.annotation.AnnotationToolbarView
import androidx.pdf.view.annotation.tool.AnnotationToolInfo

/**
 * Scope for adding custom tools to an [AnnotationToolbar].
 *
 * Custom tools added through this scope are placed into the scrollable tool tray alongside the
 * built-in annotation tools.
 */
@LayoutScopeMarker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AnnotationToolbarScope internal constructor() {

    /**
     * Adds a custom tool item to the tool tray in [AnnotationToolbar] at the specified [index].
     *
     * @param key A unique identifier for this tool to preserve state across recompositions.
     * @param index The zero-based position within the tool tray at which to insert the custom tool,
     *   or `-1` to insert at the end of native tools.
     * @param content The composable content representing the custom tool action or button.
     */
    public abstract fun tool(key: String, index: Int = -1, content: @Composable () -> Unit)
}

internal class AnnotationToolbarScopeImpl : AnnotationToolbarScope() {
    internal val tools = mutableListOf<CustomToolItem>()

    override fun tool(key: String, index: Int, content: @Composable () -> Unit) {
        tools.add(CustomToolItem(key, index, content))
    }
}

internal data class CustomToolItem(
    val key: String,
    val index: Int,
    val content: @Composable () -> Unit,
)

/**
 * A [Composable] that presents an [AnnotationToolbar].
 *
 * A UI component representing a toolbar for annotation features. It provides a set of built-in
 * tools for interacting with PDF content, such as pen, highlighter, eraser, color palette, undo,
 * redo, and toggling annotation visibility.
 *
 * Developers can inject additional custom tools into the scrollable tool tray via the [content]
 * slot.
 *
 * @param state The state object used to observe and control the toolbar.
 * @param modifier The [Modifier] to be applied to this toolbar layout.
 * @param contentPadding The inner padding applied inside the toolbar background container.
 * @param onToolChanged A callback invoked when the selected annotation tool or its attributes
 *   change.
 * @param onUndo A callback invoked when the undo button is clicked.
 * @param onRedo A callback invoked when the redo button is clicked.
 * @param onAnnotationVisibilityChanged A callback invoked when annotation visibility is toggled.
 * @param content A receiver scope that defines the custom tools to be added to the
 *   [AnnotationToolbar]. By default, custom tools are placed at the end of the tool tray. To adjust
 *   a custom tool's position, specify a positive zero-based index when calling
 *   [AnnotationToolbarScope.tool].
 */
@OptIn(ExperimentalPdfApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun AnnotationToolbar(
    state: AnnotationToolbarState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onToolChanged: (AnnotationToolInfo) -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onAnnotationVisibilityChanged: (Boolean) -> Unit = {},
    content: AnnotationToolbarScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val listener =
        rememberAnnotationToolbarListener(
            onToolChanged = onToolChanged,
            onUndo = onUndo,
            onRedo = onRedo,
            onAnnotationVisibilityChanged = onAnnotationVisibilityChanged,
        )

    val toolbarView = remember(context) { AnnotationToolbarView(context) }
    val customToolsState = rememberCustomTools(content)
    // Extract only the structural identity (key and index) of custom tools so that changes to
    // the inner content lambda reference alone do not trigger view recreation.
    val toolKeys = customToolsState.value.fastMap { it.key to it.index }

    DisposableEffect(toolbarView, state) {
        val dockListener = AnnotationToolbarView.OnDockStateChangedListener { dockStateInt ->
            state._dockState.value = AnnotationToolbarDockState.fromViewDockState(dockStateInt)
        }
        val popupVisibilityListener =
            AnnotationToolbarView.OnConfigPopupVisibilityChangedListener { isVisible ->
                state._isConfigPopupVisible.value = isVisible
            }
        val expandedStateListener =
            AnnotationToolbarView.OnExpandedStateChangedListener { isExpanded ->
                state._isExpanded.value = isExpanded
            }

        toolbarView.apply {
            addOnDockChangedListener(dockListener)
            addOnConfigPopupVisibilityChangedListener(popupVisibilityListener)
            addOnExpandedStateChangedListener(expandedStateListener)
            setAnnotationToolbarListener(listener)
        }

        state.toolbarView = toolbarView

        onDispose {
            state.toolbarView = null
            toolbarView.apply {
                setAnnotationToolbarListener(null)
                removeOnDockChangedListener(dockListener)
                removeOnConfigPopupVisibilityChangedListener(popupVisibilityListener)
                removeOnExpandedStateChangedListener(expandedStateListener)
            }
        }
    }

    // Custom tools are ComposeViews embedded inside the toolbar ViewGroup. Manage their
    // subcompositions in a DisposableEffect keyed on toolKeys so views are only recreated when
    // tools are added, removed, or reordered, while content updates are observed in-place.
    DisposableEffect(toolbarView, toolKeys) {
        val customComposeViews = attachCustomToolsToView(toolbarView, customToolsState)
        onDispose {
            customComposeViews.fastForEach { it.disposeComposition() }
            toolbarView.removeAllViews()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { toolbarView },
        update = { view ->
            applyToolbarPadding(view, density, layoutDirection, contentPadding)
        },
    )
}

@Composable
@OptIn(ExperimentalPdfApi::class)
private fun rememberAnnotationToolbarListener(
    onToolChanged: (AnnotationToolInfo) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAnnotationVisibilityChanged: (Boolean) -> Unit,
): AnnotationToolbarView.AnnotationToolbarListener {
    // Use rememberUpdatedState to ensure the listener invokes the latest callbacks across
    // recompositions without recreating the listener instance or causing effect restarts.
    val currentOnToolChanged by rememberUpdatedState(onToolChanged)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnRedo by rememberUpdatedState(onRedo)
    val currentOnAnnotationVisibilityChanged by rememberUpdatedState(onAnnotationVisibilityChanged)

    return remember {
        object : AnnotationToolbarView.AnnotationToolbarListener {
            override fun onToolChanged(toolInfo: AnnotationToolInfo) {
                currentOnToolChanged(toolInfo)
            }

            override fun onUndo() {
                currentOnUndo()
            }

            override fun onRedo() {
                currentOnRedo()
            }

            override fun onAnnotationVisibilityChanged(isVisible: Boolean) {
                currentOnAnnotationVisibilityChanged(isVisible)
            }
        }
    }
}

@Composable
private fun rememberCustomTools(
    content: AnnotationToolbarScope.() -> Unit
): State<List<CustomToolItem>> {
    // Combine rememberUpdatedState with derivedStateOf so the DSL block is re-evaluated whenever
    // the content lambda reference changes OR any snapshot state (e.g. SnapshotStateList) read
    // inside the DSL block changes, without re-running on unrelated toolbar recompositions.
    val latestContent = rememberUpdatedState(content)
    return remember {
        derivedStateOf { AnnotationToolbarScopeImpl().apply(latestContent.value).tools }
    }
}

@OptIn(ExperimentalPdfApi::class)
private fun applyToolbarPadding(
    toolbarView: AnnotationToolbarView,
    density: Density,
    layoutDirection: LayoutDirection,
    contentPadding: PaddingValues,
) {
    with(density) {
        val leftPx = contentPadding.calculateStartPadding(layoutDirection).roundToPx()
        val topPx = contentPadding.calculateTopPadding().roundToPx()
        val rightPx = contentPadding.calculateEndPadding(layoutDirection).roundToPx()
        val bottomPx = contentPadding.calculateBottomPadding().roundToPx()

        toolbarView.setPadding(leftPx, topPx, rightPx, bottomPx)
    }
}

@OptIn(ExperimentalPdfApi::class)
private fun attachCustomToolsToView(
    toolbarView: AnnotationToolbarView,
    customToolsState: State<List<CustomToolItem>>,
): List<ComposeView> {
    toolbarView.removeAllViews()
    val customTools by customToolsState
    val composeViews = customTools.fastMapIndexed { i, _ ->
        ComposeView(toolbarView.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                // Read from customToolsState inside setContent so existing ComposeViews
                // recompose in-place when a tool's content lambda updates. Guard the index
                // in case a tool is removed and its ComposeView recomposes before onDispose
                // detaches it.
                if (i in customTools.indices) {
                    customTools[i].content()
                }
            }
        }
    }
    composeViews.fastForEachIndexed { i, composeView ->
        toolbarView.addView(composeView, customTools[i].index)
    }
    return composeViews
}
