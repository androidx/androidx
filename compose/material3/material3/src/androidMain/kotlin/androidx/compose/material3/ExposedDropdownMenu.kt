/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.internal.ExposedDropdownMenuPopup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.DpOffset
import kotlin.math.max

/**
 * <a href="https://m3.material.io/components/menus/overview" class="external" target="_blank">Material Design Exposed Dropdown Menu</a>.
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * Exposed dropdown menus display the currently selected item in a text field to which the menu is
 * anchored. In some cases, it can accept and display user input (whether or not it’s listed as a
 * menu choice). If the text field input is used to filter results in the menu, the component is
 * also known as "autocomplete" or a "combobox".
 *
 * ![Exposed dropdown menu image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu.png)
 *
 * The [ExposedDropdownMenuBox] is expected to contain a [TextField] (or [OutlinedTextField]) and
 * [ExposedDropdownMenuBoxScope.ExposedDropdownMenu] as content.
 *
 * An example of read-only Exposed Dropdown Menu:
 * @sample androidx.compose.material3.samples.ExposedDropdownMenuSample
 *
 * An example of editable Exposed Dropdown Menu:
 * @sample androidx.compose.material3.samples.EditableExposedDropdownMenuSample
 *
 * @param expanded whether the menu is expanded or not
 * @param onExpandedChange called when the exposed dropdown menu is clicked and the expansion state
 * changes.
 * @param modifier the [Modifier] to be applied to this exposed dropdown menu
 * @param content the content of this exposed dropdown menu, typically a [TextField] and an
 * [ExposedDropdownMenuBoxScope.ExposedDropdownMenu]. The [TextField] within [content] should be
 * passed the [ExposedDropdownMenuBoxScope.menuAnchor] modifier for proper menu behavior.
 */
@ExperimentalMaterial3Api
@Composable
actual fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current
    var width by remember { mutableStateOf(0) }
    var menuHeight by remember { mutableStateOf(0) }
    val verticalMarginInPx = with(density) { MenuVerticalMargin.roundToPx() }
    val coordinates = remember { Ref<LayoutCoordinates>() }

    val focusRequester = remember { FocusRequester() }

    val scope = remember(expanded, onExpandedChange, density, menuHeight, width) {
        object : ExposedDropdownMenuBoxScope {
            override fun Modifier.menuAnchor(): Modifier {
                return composed(inspectorInfo = debugInspectorInfo { name = "menuAnchor" }) {
                    onGloballyPositioned {
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
                        expanded = expanded,
                        onExpandedChange = { onExpandedChange(!expanded) },
                    ).focusRequester(focusRequester)
                }
            }
            override fun Modifier.exposedDropdownSize(matchTextFieldWidth: Boolean): Modifier {
                return with(density) {
                    heightIn(max = menuHeight.toDp()).let {
                        if (matchTextFieldWidth) {
                            it.width(width.toDp())
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    Box(modifier) {
        scope.content()
    }

    SideEffect {
        if (expanded) focusRequester.requestFocus()
    }

    DisposableEffect(view) {
        val listener = OnGlobalLayoutListener(view) {
            // We want to recalculate the menu height on relayout - e.g. when keyboard shows up.
            updateHeight(view.rootView, coordinates.value, verticalMarginInPx) { newHeight ->
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

@Composable
internal actual fun ExposedDropdownMenuBoxScope.ExposedDropdownMenuDefaultImpl(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
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
        ) { anchorBounds, menuBounds ->
            transformOriginState.value = calculateTransformOrigin(anchorBounds, menuBounds)
        }

        ExposedDropdownMenuPopup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider
        ) {
            DropdownMenuContent(
                expandedStates = expandedStates,
                transformOriginState = transformOriginState,
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
