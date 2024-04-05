/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.toIntRect
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
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@Composable
actual fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    var anchorWidth by remember { mutableIntStateOf(0) }
    var menuMaxHeight by remember { mutableIntStateOf(0) }
    val verticalMarginInPx = with(density) { MenuVerticalMargin.roundToPx() }

    val focusRequester = remember { FocusRequester() }
    val expandedDescription = getString(Strings.MenuExpanded)
    val collapsedDescription = getString(Strings.MenuCollapsed)

    val scope = remember(expanded, onExpandedChange, windowInfo, density) {
        object : ExposedDropdownMenuBoxScope() {
            override fun Modifier.menuAnchor(): Modifier = this
                .onGloballyPositioned {
                    anchorWidth = it.size.width
                    val boundsInWindow = it.boundsInWindow()
                    val visibleWindowBounds = windowInfo.containerSize.toIntRect()
                    val heightAbove = boundsInWindow.top - visibleWindowBounds.top
                    val heightBelow = visibleWindowBounds.height - boundsInWindow.bottom
                    menuMaxHeight = max(heightAbove, heightBelow).toInt() - verticalMarginInPx
                }
                .expandable(
                    expanded = expanded,
                    onExpandedChange = { onExpandedChange(!expanded) },
                    expandedDescription = expandedDescription,
                    collapsedDescription = collapsedDescription,
                )
                .focusRequester(focusRequester)

            override fun Modifier.exposedDropdownSize(matchTextFieldWidth: Boolean): Modifier =
                layout { measurable, constraints ->
                    val menuWidth = constraints.constrainWidth(anchorWidth)
                    val menuConstraints = constraints.copy(
                        maxHeight = constraints.constrainHeight(menuMaxHeight),
                        minWidth = if (matchTextFieldWidth) menuWidth else constraints.minWidth,
                        maxWidth = if (matchTextFieldWidth) menuWidth else constraints.maxWidth,
                    )
                    val placeable = measurable.measure(menuConstraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
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
}

@Composable
internal actual fun ExposedDropdownMenuBoxScope.ExposedDropdownMenuDefaultImpl(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.exposedDropdownSize(),
        scrollState = scrollState,
        content = content
    )
}
