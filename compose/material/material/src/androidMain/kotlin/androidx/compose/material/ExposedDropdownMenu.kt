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
package androidx.compose.material

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.internal.ExposedDropdownMenuPopup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import kotlin.math.max

/**
 * [Material Design Exposed Dropdown Menu](https://material.io/components/menus#exposed-dropdown-menu).
 *
 * Box for Exposed Dropdown Menu. Expected to contain [TextField] and
 * [ExposedDropdownMenuBoxScope.ExposedDropdownMenu] as a content.
 *
 * An example of read-only Exposed Dropdown Menu:
 *
 * @sample androidx.compose.material.samples.ExposedDropdownMenuSample
 *
 * An example of editable Exposed Dropdown Menu:
 *
 * @sample androidx.compose.material.samples.EditableExposedDropdownMenuSample
 *
 * @param expanded Whether Dropdown Menu should be expanded or not.
 * @param onExpandedChange Executes when the user clicks on the ExposedDropdownMenuBox.
 * @param modifier The modifier to apply to this layout
 * @param content The content to be displayed inside ExposedDropdownMenuBox.
 */
@ExperimentalMaterialApi
@Composable
actual fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current
    var width by remember { mutableIntStateOf(0) }
    var menuHeight by remember { mutableIntStateOf(0) }
    val verticalMarginInPx = with(density) { MenuVerticalMargin.roundToPx() }
    val coordinates = remember { Ref<LayoutCoordinates>() }

    val scope = remember(density, menuHeight, width) {
        object : ExposedDropdownMenuBoxScope {
            override fun Modifier.exposedDropdownSize(matchTextFieldWidth: Boolean): Modifier {
                return with(density) {
                    heightIn(max = menuHeight.toDp()).let {
                        if (matchTextFieldWidth) {
                            it.width(width.toDp())
                        } else it
                    }
                }
            }
        }
    }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier.onGloballyPositioned {
            width = it.size.width
            coordinates.value = it
            updateHeight(
                view.rootView,
                coordinates.value,
                verticalMarginInPx
            ) { newHeight ->
                menuHeight = newHeight
            }
        }.expandable(
            onExpandedChange = { onExpandedChange(!expanded) },
            menuLabel = getString(Strings.ExposedDropdownMenu)
        ).focusRequester(focusRequester)
    ) {
        scope.content()
    }

    SideEffect {
        if (expanded) focusRequester.requestFocus()
    }

    DisposableEffect(view) {
        val listener = OnGlobalLayoutListener(view) {
            // We want to recalculate the menu height on relayout - e.g. when keyboard shows up.
            updateHeight(
                view.rootView,
                coordinates.value,
                verticalMarginInPx
            ) { newHeight ->
                menuHeight = newHeight
            }
        }
        onDispose { listener.dispose() }
    }
}

/**
 * Subscribes to onGlobalLayout and correctly removes the callback when the View is detached.
 * Logic copied from AndroidPopup.android.kt.
 */
private class OnGlobalLayoutListener(
    private val view: View,
    private val onGlobalLayoutCallback: () -> Unit
) : View.OnAttachStateChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
    private var isListeningToGlobalLayout = false

    init {
        view.addOnAttachStateChangeListener(this)
        registerOnGlobalLayoutListener()
    }

    override fun onViewAttachedToWindow(p0: View) = registerOnGlobalLayoutListener()

    override fun onViewDetachedFromWindow(p0: View) = unregisterOnGlobalLayoutListener()

    override fun onGlobalLayout() = onGlobalLayoutCallback()

    private fun registerOnGlobalLayoutListener() {
        if (isListeningToGlobalLayout || !view.isAttachedToWindow) return
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
        isListeningToGlobalLayout = true
    }

    private fun unregisterOnGlobalLayoutListener() {
        if (!isListeningToGlobalLayout) return
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        isListeningToGlobalLayout = false
    }

    fun dispose() {
        unregisterOnGlobalLayoutListener()
        view.removeOnAttachStateChangeListener(this)
    }
}

internal actual fun ExposedDropdownMenuBoxScope.ExposedDropdownMenuDefaultImpl(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
    // TODO(b/202810604): use DropdownMenu when PopupProperties constructor is stable
    // return DropdownMenu(
    //     expanded = expanded,
    //     onDismissRequest = onDismissRequest,
    //     modifier = modifier.exposedDropdownSize(),
    //     properties = ExposedDropdownMenuDefaults.PopupProperties,
    //     content = content
    // )

    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val density = LocalDensity.current
        val popupPositionProvider = DropdownMenuPositionProvider(
            DpOffset.Zero,
            density
        ) { parentBounds, menuBounds ->
            transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
        }

        ExposedDropdownMenuPopup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider
        ) {
            DropdownMenuContent(
                expandedStates = expandedStates,
                transformOriginState = transformOriginState,
                scrollState = scrollState,
                modifier = modifier.exposedDropdownSize(),
                content = content
            )
        }
    }
}

private fun updateHeight(
    view: View,
    coordinates: LayoutCoordinates?,
    verticalMarginInPx: Int,
    onHeightUpdate: (Int) -> Unit
) {
    coordinates ?: return
    val visibleWindowBounds = Rect().let {
        view.getWindowVisibleDisplayFrame(it)
        it
    }
    val heightAbove = coordinates.boundsInWindow().top - visibleWindowBounds.top
    val heightBelow =
        visibleWindowBounds.bottom - visibleWindowBounds.top - coordinates.boundsInWindow().bottom
    onHeightUpdate(max(heightAbove, heightBelow).toInt() - verticalMarginInPx)
}
