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

import android.graphics.Rect as ViewRect
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.roundToInt

@ExperimentalMaterial3Api
@Composable
actual fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    val config = LocalConfiguration.current
    val view = LocalView.current
    val density = LocalDensity.current

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
        remember(expanded, onExpandedChange, config, view, density) {
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
                    windowBounds = view.rootView.getWindowBounds(),
                    anchorBounds = anchorCoordinates.getAnchorBounds(),
                    verticalMargin = verticalMargin,
                )
        }
    ) {
        scope.content()
    }

    if (expanded) {
        SoftKeyboardListener(view, density) {
            menuMaxHeight =
                calculateMaxHeight(
                    windowBounds = view.rootView.getWindowBounds(),
                    anchorBounds = anchorCoordinates.getAnchorBounds(),
                    verticalMargin = verticalMargin,
                )
        }
    }

    SideEffect { if (expanded) focusRequester.requestFocus() }

    // Back events are handled in the Popup layer if the menu is focusable.
    // If it's not focusable, we handle them here.
    BackHandler(enabled = expanded) { onExpandedChange(false) }
}

@Composable
private fun SoftKeyboardListener(
    view: View,
    density: Density,
    onKeyboardVisibilityChange: () -> Unit,
) {
    // It would be easier to listen to WindowInsets.ime, but that doesn't work with
    // `setDecorFitsSystemWindows(window, true)`. Instead, listen to the view tree's global layout.
    DisposableEffect(view, density) {
        val listener =
            object : View.OnAttachStateChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
                private var isListeningToGlobalLayout = false

                init {
                    view.addOnAttachStateChangeListener(this)
                    registerOnGlobalLayoutListener()
                }

                override fun onViewAttachedToWindow(p0: View) = registerOnGlobalLayoutListener()

                override fun onViewDetachedFromWindow(p0: View) = unregisterOnGlobalLayoutListener()

                override fun onGlobalLayout() = onKeyboardVisibilityChange()

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

        onDispose { listener.dispose() }
    }
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
    val view = LocalView.current
    val density = LocalDensity.current
    val topWindowInsets = WindowInsets.statusBars.getTop(density)

    if (expanded) {
        SoftKeyboardListener(view, density) { keyboardSignalState.value = Unit }
    }

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

        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = popupProperties(anchorType),
        ) {
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
    var flags =
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM

    // In order for a11y focus to jump to the menu when opened, it needs to be
    // focusable and touch modal (NOT_FOCUSABLE and NOT_TOUCH_MODAL are *not* set).
    if (!a11yServicesEnabled) {
        flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    }
    // If typing on the IME is required, the menu should not be focusable
    // in order to prevent stealing focus from the input method.
    val imeRequired =
        anchorType == MenuAnchorType.PrimaryEditable ||
            (anchorType == MenuAnchorType.SecondaryEditable && !a11yServicesEnabled)
    if (imeRequired) {
        flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    }

    return PopupProperties(flags = flags)
}

private fun calculateMaxHeight(
    windowBounds: Rect,
    anchorBounds: Rect?,
    verticalMargin: Int,
): Int {
    anchorBounds ?: return 0

    val marginedWindowTop = windowBounds.top + verticalMargin
    val marginedWindowBottom = windowBounds.bottom - verticalMargin
    val availableHeight =
        if (anchorBounds.top > windowBounds.bottom || anchorBounds.bottom < windowBounds.top) {
            (marginedWindowBottom - marginedWindowTop).roundToInt()
        } else {
            val heightAbove = anchorBounds.top - marginedWindowTop
            val heightBelow = marginedWindowBottom - anchorBounds.bottom
            max(heightAbove, heightBelow).roundToInt()
        }

    return max(availableHeight, 0)
}

private fun View.getWindowBounds(): Rect =
    ViewRect().let {
        this.getWindowVisibleDisplayFrame(it)
        it.toComposeRect()
    }

private fun LayoutCoordinates?.getAnchorBounds(): Rect {
    // Don't use `boundsInWindow()` because it can report 0 when the window is animating/resizing
    return if (this == null) Rect.Zero else Rect(positionInWindow(), size.toSize())
}
