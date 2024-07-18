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

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.rememberAccessibilityServiceState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.roundToInt

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

    val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }

    var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var anchorWidth by remember { mutableIntStateOf(0) }
    var menuMaxHeight by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val expandedDescription = getString(Strings.MenuExpanded)
    val collapsedDescription = getString(Strings.MenuCollapsed)
    val toggleDescription = getString(Strings.ToggleDropdownMenu)
    val anchorTypeState = remember { mutableStateOf(MenuAnchorType.PrimaryNotEditable) }

    val scope =
        remember(expanded, onExpandedChange, windowInfo, density) {
            object : ExposedDropdownMenuBoxScopeImpl() {
                override fun Modifier.menuAnchor(type: MenuAnchorType, enabled: Boolean): Modifier =
                    this.focusRequester(focusRequester)
                        .then(
                            if (!enabled) Modifier
                            else
                                Modifier.expandable(
                                    expanded = expanded,
                                    onExpandedChange = {
                                        anchorTypeState.value = type
                                        onExpandedChange(!expanded)
                                    },
                                    anchorType = type,
                                    expandedDescription = expandedDescription,
                                    collapsedDescription = collapsedDescription,
                                    toggleDescription = toggleDescription,
                                    keyboardController = keyboardController,
                                )
                        )

                override val anchorType: MenuAnchorType
                    get() = anchorTypeState.value

                override fun Modifier.exposedDropdownSize(matchTextFieldWidth: Boolean): Modifier =
                    layout { measurable, constraints ->
                        val menuWidth = constraints.constrainWidth(anchorWidth)
                        val menuConstraints =
                            constraints.copy(
                                maxHeight = constraints.constrainHeight(menuMaxHeight),
                                minWidth =
                                if (matchTextFieldWidth) menuWidth else constraints.minWidth,
                                maxWidth =
                                if (matchTextFieldWidth) menuWidth else constraints.maxWidth,
                            )
                        val placeable = measurable.measure(menuConstraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
            }
        }

    Box(
        modifier.onGloballyPositioned {
            anchorCoordinates = it
            anchorWidth = it.size.width
            menuMaxHeight =
                calculateMaxHeight(
                    windowBounds = windowInfo.containerSize.toIntRect(),
                    anchorBounds = anchorCoordinates.getAnchorBounds(),
                    verticalMargin = verticalMargin,
                )
        }
    ) {
        scope.content()
    }

    SideEffect { if (expanded) focusRequester.requestFocus() }
}

@Composable
internal actual fun ExposedDropdownMenuBoxScope.ExposedDropdownMenuDefaultImpl(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    scrollState: ScrollState,
    matchTextFieldWidth: Boolean,
    shape: Shape,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    border: BorderStroke?,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Workaround for b/326394521. We create a state that's read in `calculatePosition`.
    // Then trigger a state change in `SoftKeyboardListener` to force recalculation.
    val keyboardSignalState = remember { mutableStateOf(Unit, neverEqualPolicy()) }
    val density = LocalDensity.current
    val topWindowInsets = WindowInsets.statusBars.getTop(density)

    // TODO(b/326064777): use DropdownMenu when it supports custom PositionProvider
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded

    if (expandedState.currentState || expandedState.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val popupPositionProvider =
            remember(density, topWindowInsets) {
                ExposedDropdownMenuPositionProvider(
                    density = density,
                    topWindowInsets = topWindowInsets,
                    keyboardSignalState = keyboardSignalState,
                ) { anchorBounds, menuBounds ->
                    transformOriginState.value =
                        calculateTransformOrigin(anchorBounds, menuBounds)
                }
            }

        var focusManager: FocusManager? by mutableStateOf(null)
        var inputModeManager: InputModeManager? by mutableStateOf(null)
        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = popupProperties(anchorType),
            onKeyEvent = {
                handleDropdownOnKeyEvent(it, focusManager, inputModeManager)
            },
        ) {
            focusManager = LocalFocusManager.current
            inputModeManager = LocalInputModeManager.current

            DropdownMenuContent(
                expandedState = expandedState,
                transformOriginState = transformOriginState,
                scrollState = scrollState,
                shape = shape,
                containerColor = containerColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation,
                border = border,
                modifier = modifier.exposedDropdownSize(matchTextFieldWidth),
                content = content,
            )
        }
    }
}

/**
 * Creates a [PopupProperties] used for [ExposedDropdownMenuBoxScope.ExposedDropdownMenu].
 *
 * @param anchorType the type of element that is anchoring the menu. See [MenuAnchorType].
 */
@Composable
private fun popupProperties(anchorType: MenuAnchorType): PopupProperties {
    val a11yServicesEnabled by rememberAccessibilityServiceState()

    // If typing on the IME is required, the menu should not be focusable
    // in order to prevent stealing focus from the input method.
    val imeRequired =
        anchorType == MenuAnchorType.PrimaryEditable ||
            (anchorType == MenuAnchorType.SecondaryEditable && !a11yServicesEnabled)
    return PopupProperties(
        focusable = !imeRequired
    )
}

private fun calculateMaxHeight(
    windowBounds: IntRect,
    anchorBounds: Rect?,
    verticalMargin: Int,
): Int {
    anchorBounds ?: return 0

    val marginedWindowTop = windowBounds.top + verticalMargin
    val marginedWindowBottom = windowBounds.bottom - verticalMargin
    val availableHeight =
        if (anchorBounds.top > windowBounds.bottom || anchorBounds.bottom < windowBounds.top) {
            (marginedWindowBottom - marginedWindowTop)
        } else {
            val heightAbove = anchorBounds.top - marginedWindowTop
            val heightBelow = marginedWindowBottom - anchorBounds.bottom
            max(heightAbove, heightBelow).roundToInt()
        }

    return max(availableHeight, 0)
}

private fun LayoutCoordinates?.getAnchorBounds(): Rect {
    // Don't use `boundsInWindow()` because it can report 0 when the window is animating/resizing
    return if (this == null) Rect.Zero else Rect(positionInWindow(), size.toSize())
}
