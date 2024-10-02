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
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.internal.BackHandler
import androidx.compose.material3.internal.Icons
import androidx.compose.material3.internal.MenuPosition
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.rememberAccessibilityServiceState
import androidx.compose.material3.tokens.FilledAutocompleteTokens
import androidx.compose.material3.tokens.OutlinedAutocompleteTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * <a href="https://m3.material.io/components/menus/overview" class="external"
 * target="_blank">Material Design Exposed Dropdown Menu</a>.
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * Exposed dropdown menus, sometimes also called "spinners" or "combo boxes", display the currently
 * selected item in a text field to which the menu is anchored. In some cases, it can accept and
 * display user input (whether or not it’s listed as a menu choice), in which case it may be used to
 * implement autocomplete.
 *
 * ![Exposed dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu.png)
 *
 * The [ExposedDropdownMenuBox] is expected to contain a [TextField] (or [OutlinedTextField]) and
 * [ExposedDropdownMenu][ExposedDropdownMenuBoxScope.ExposedDropdownMenu] as content. The
 * [menuAnchor][ExposedDropdownMenuBoxScope.menuAnchor] modifier should be passed to the text field.
 *
 * An example of a read-only Exposed Dropdown Menu:
 *
 * @sample androidx.compose.material3.samples.ExposedDropdownMenuSample
 *
 * An example of an editable Exposed Dropdown Menu:
 *
 * @sample androidx.compose.material3.samples.EditableExposedDropdownMenuSample
 *
 * An example of an editable Exposed Dropdown Menu used like a MultiAutoCompleteTextView:
 *
 * @sample androidx.compose.material3.samples.MultiAutocompleteExposedDropdownMenuSample
 * @param expanded whether the menu is expanded or not
 * @param onExpandedChange called when the exposed dropdown menu is clicked and the expansion state
 *   changes.
 * @param modifier the [Modifier] to be applied to this ExposedDropdownMenuBox
 * @param content the content of this ExposedDropdownMenuBox, typically a [TextField] and an
 *   [ExposedDropdownMenu][ExposedDropdownMenuBoxScope.ExposedDropdownMenu].
 */
@ExperimentalMaterial3Api
@Composable
fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit,
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
    val anchorTypeState = remember {
        mutableStateOf(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
    }

    val scope =
        remember(expanded, onExpandedChange, config, view, density) {
            object : ExposedDropdownMenuBoxScopeImpl() {
                override fun Modifier.menuAnchor(
                    type: ExposedDropdownMenuAnchorType,
                    enabled: Boolean
                ): Modifier =
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

                override val anchorType: ExposedDropdownMenuAnchorType
                    get() = anchorTypeState.value

                override fun Modifier.exposedDropdownSize(matchAnchorWidth: Boolean): Modifier =
                    layout { measurable, constraints ->
                        val menuWidth = constraints.constrainWidth(anchorWidth)
                        val menuConstraints =
                            constraints.copy(
                                maxHeight = constraints.constrainHeight(menuMaxHeight),
                                minWidth =
                                    if (matchAnchorWidth) menuWidth else constraints.minWidth,
                                maxWidth =
                                    if (matchAnchorWidth) menuWidth else constraints.maxWidth,
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

/** Scope for [ExposedDropdownMenuBox]. */
@ExperimentalMaterial3Api
sealed class ExposedDropdownMenuBoxScope {
    /**
     * Modifier which should be applied to an element inside the [ExposedDropdownMenuBoxScope],
     * typically a text field or an icon within the text field. It's responsible for requesting
     * focus, and if [enabled], expanding/collapsing the menu on click and applying semantics.
     *
     * @param type the type of element that is anchoring the menu. See
     *   [ExposedDropdownMenuAnchorType].
     * @param enabled whether controlling the menu via this anchor is enabled. When `false`, the
     *   component will not expand or collapse the menu in response to user input, and menu
     *   semantics will be invisible to accessibility services. Note that this value only controls
     *   interactions with the menu. It does not affect the enabled state of other kinds of
     *   interactions, such as [TextField]'s `enabled` parameter.
     */
    abstract fun Modifier.menuAnchor(
        type: ExposedDropdownMenuAnchorType,
        enabled: Boolean = true
    ): Modifier

    /**
     * Modifier which should be applied to a menu placed inside the [ExposedDropdownMenuBoxScope].
     * It will set constraints on the width and height of the menu so it will not overlap the text
     * field or software keyboard.
     *
     * [ExposedDropdownMenu] applies this modifier automatically, so this is only needed when using
     * custom menu components.
     *
     * @param matchAnchorWidth whether the menu's width should be forcefully constrained to match
     *   the width of the text field to which it's attached.
     */
    abstract fun Modifier.exposedDropdownSize(matchAnchorWidth: Boolean = true): Modifier

    internal abstract val anchorType: ExposedDropdownMenuAnchorType

    /**
     * Popup which contains content for Exposed Dropdown Menu. Should be used inside the content of
     * [ExposedDropdownMenuBox].
     *
     * @param expanded whether the menu is expanded
     * @param onDismissRequest called when the user requests to dismiss the menu, such as by tapping
     *   outside the menu's bounds
     * @param modifier the [Modifier] to be applied to this menu
     * @param scrollState a [ScrollState] used by the menu's content for items vertical scrolling
     * @param matchAnchorWidth whether the menu's width should be forcefully constrained to match
     *   the width of the text field to which it's attached.
     * @param shape the shape of the menu
     * @param containerColor the container color of the menu
     * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary
     *   color overlay is applied on top of the container. A higher tonal elevation value will
     *   result in a darker color in light theme and lighter color in dark theme. See also:
     *   [Surface].
     * @param shadowElevation the elevation for the shadow below the menu
     * @param border the border to draw around the container of the menu. Pass `null` for no border.
     * @param content the content of the menu
     */
    @Composable
    fun ExposedDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        scrollState: ScrollState = rememberScrollState(),
        matchAnchorWidth: Boolean = true,
        shape: Shape = MenuDefaults.shape,
        containerColor: Color = MenuDefaults.containerColor,
        tonalElevation: Dp = MenuDefaults.TonalElevation,
        shadowElevation: Dp = MenuDefaults.ShadowElevation,
        border: BorderStroke? = null,
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
                properties = ExposedDropdownMenuDefaults.popupProperties(anchorType),
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
                    modifier = modifier.exposedDropdownSize(matchAnchorWidth),
                    content = content,
                )
            }
        }
    }

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Use overload that takes ExposedDropdownMenuAnchorType and enabled parameters",
        replaceWith = ReplaceWith("menuAnchor(type, enabled)")
    )
    fun Modifier.menuAnchor(): Modifier =
        menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message =
            "The `focusable` parameter is unused. Pass the proper ExposedDropdownMenuAnchorType " +
                "to Modifier.menuAnchor instead, which will handle focusability automatically.",
    )
    @Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
    @Composable
    fun ExposedDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        scrollState: ScrollState = rememberScrollState(),
        focusable: Boolean = true,
        matchTextFieldWidth: Boolean = true,
        shape: Shape = MenuDefaults.shape,
        containerColor: Color = MenuDefaults.containerColor,
        tonalElevation: Dp = MenuDefaults.TonalElevation,
        shadowElevation: Dp = MenuDefaults.ShadowElevation,
        border: BorderStroke? = null,
        content: @Composable ColumnScope.() -> Unit,
    ) =
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            scrollState = scrollState,
            matchAnchorWidth = matchTextFieldWidth,
            shape = shape,
            containerColor = containerColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            content = content,
        )

    @Suppress("DEPRECATION")
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message =
            "Maintained for binary compatibility. " +
                "Use overload with customization options parameters."
    )
    @Composable
    fun ExposedDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        scrollState: ScrollState = rememberScrollState(),
        content: @Composable ColumnScope.() -> Unit,
    ) =
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            matchTextFieldWidth = true,
            scrollState = scrollState,
            shape = MenuDefaults.shape,
            containerColor = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
            border = null,
            content = content,
        )
}

// Sealed classes don't allow dynamic anonymous subclasses.
@OptIn(ExperimentalMaterial3Api::class)
private abstract class ExposedDropdownMenuBoxScopeImpl : ExposedDropdownMenuBoxScope()

/** The type of element that can serve as a dropdown menu anchor. */
@JvmInline
value class ExposedDropdownMenuAnchorType private constructor(private val name: String) {
    companion object {
        /**
         * A non-editable primary anchor of the dropdown menu, such as a read-only text field.
         *
         * An anchor of this type will open the menu with focus.
         */
        val PrimaryNotEditable = ExposedDropdownMenuAnchorType("PrimaryNotEditable")

        /**
         * An editable primary anchor of the dropdown menu, such as a text field that allows user
         * input.
         *
         * An anchor of this type will open the menu without focus in order to preserve focus on the
         * soft keyboard (IME).
         */
        val PrimaryEditable = ExposedDropdownMenuAnchorType("PrimaryEditable")

        /**
         * A secondary anchor of the dropdown menu that lives alongside an editable primary anchor,
         * such as an icon within an editable text field.
         *
         * If accessibility services are enabled, an anchor of this type will open the menu with
         * focus. Otherwise, the menu is opened without focus in order to preserve focus on the soft
         * keyboard (IME).
         */
        val SecondaryEditable = ExposedDropdownMenuAnchorType("SecondaryEditable")
    }

    override fun toString(): String = name
}

@Deprecated(
    message = "Renamed to ExposedDropdownMenuAnchorType",
    replaceWith = ReplaceWith("ExposedDropdownMenuAnchorType"),
)
typealias MenuAnchorType = ExposedDropdownMenuAnchorType

/** Contains default values used by Exposed Dropdown Menu. */
@ExperimentalMaterial3Api
object ExposedDropdownMenuDefaults {
    /**
     * Default trailing icon for Exposed Dropdown Menu.
     *
     * @param expanded whether the menu is expanded or not. Affects the appearance of the icon.
     * @param modifier the [Modifier] to be applied to this icon
     */
    @ExperimentalMaterial3Api
    @Composable
    fun TrailingIcon(
        expanded: Boolean,
        modifier: Modifier = Modifier,
    ) {
        Icon(Icons.Filled.ArrowDropDown, null, modifier.rotate(if (expanded) 180f else 0f))
    }

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in a [TextField] within an
     * [ExposedDropdownMenuBox].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     *   focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     *   state
     * @param focusedContainerColor the container color for this text field when focused
     * @param unfocusedContainerColor the container color for this text field when not focused
     * @param disabledContainerColor the container color for this text field when disabled
     * @param errorContainerColor the container color for this text field when in error state
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param selectionColors the colors used when the input text of this text field is selected
     * @param focusedIndicatorColor the indicator color for this text field when focused
     * @param unfocusedIndicatorColor the indicator color for this text field when not focused
     * @param disabledIndicatorColor the indicator color for this text field when disabled
     * @param errorIndicatorColor the indicator color for this text field when in error state
     * @param focusedLeadingIconColor the leading icon color for this text field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
     * @param disabledLeadingIconColor the leading icon color for this text field when disabled
     * @param errorLeadingIconColor the leading icon color for this text field when in error state
     * @param focusedTrailingIconColor the trailing icon color for this text field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not
     *   focused
     * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
     * @param errorTrailingIconColor the trailing icon color for this text field when in error state
     * @param focusedLabelColor the label color for this text field when focused
     * @param unfocusedLabelColor the label color for this text field when not focused
     * @param disabledLabelColor the label color for this text field when disabled
     * @param errorLabelColor the label color for this text field when in error state
     * @param focusedPlaceholderColor the placeholder color for this text field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     * @param errorPlaceholderColor the placeholder color for this text field when in error state
     * @param focusedPrefixColor the prefix color for this text field when focused
     * @param unfocusedPrefixColor the prefix color for this text field when not focused
     * @param disabledPrefixColor the prefix color for this text field when disabled
     * @param errorPrefixColor the prefix color for this text field when in error state
     * @param focusedSuffixColor the suffix color for this text field when focused
     * @param unfocusedSuffixColor the suffix color for this text field when not focused
     * @param disabledSuffixColor the suffix color for this text field when disabled
     * @param errorSuffixColor the suffix color for this text field when in error state
     */
    @Composable
    fun textFieldColors(
        focusedTextColor: Color = FilledAutocompleteTokens.FieldFocusInputTextColor.value,
        unfocusedTextColor: Color = FilledAutocompleteTokens.FieldInputTextColor.value,
        disabledTextColor: Color =
            FilledAutocompleteTokens.FieldDisabledInputTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledInputTextOpacity
            ),
        errorTextColor: Color = FilledAutocompleteTokens.FieldErrorInputTextColor.value,
        focusedContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.value,
        unfocusedContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.value,
        disabledContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.value,
        errorContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.value,
        cursorColor: Color = FilledAutocompleteTokens.TextFieldCaretColor.value,
        errorCursorColor: Color = FilledAutocompleteTokens.TextFieldErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldFocusActiveIndicatorColor.value,
        unfocusedIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldActiveIndicatorColor.value,
        disabledIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledActiveIndicatorColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledActiveIndicatorOpacity
            ),
        errorIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldErrorActiveIndicatorColor.value,
        focusedLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldFocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color = FilledAutocompleteTokens.TextFieldLeadingIconColor.value,
        disabledLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledLeadingIconColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledLeadingIconOpacity
            ),
        errorLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldFocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldTrailingIconColor.value,
        disabledTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledTrailingIconColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledTrailingIconOpacity
            ),
        errorTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldErrorTrailingIconColor.value,
        focusedLabelColor: Color = FilledAutocompleteTokens.FieldFocusLabelTextColor.value,
        unfocusedLabelColor: Color = FilledAutocompleteTokens.FieldLabelTextColor.value,
        disabledLabelColor: Color = FilledAutocompleteTokens.FieldDisabledLabelTextColor.value,
        errorLabelColor: Color = FilledAutocompleteTokens.FieldErrorLabelTextColor.value,
        focusedPlaceholderColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedPlaceholderColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPlaceholderColor: Color =
            FilledAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorPlaceholderColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        focusedPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPrefixColor: Color =
            FilledAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        focusedSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        disabledSuffixColor: Color =
            FilledAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
    ): TextFieldColors =
        TextFieldDefaults.colors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            disabledContainerColor = disabledContainerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            selectionColors = selectionColors,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in an [OutlinedTextField] within an
     * [ExposedDropdownMenuBox].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     *   focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     *   state
     * @param focusedContainerColor the container color for this text field when focused
     * @param unfocusedContainerColor the container color for this text field when not focused
     * @param disabledContainerColor the container color for this text field when disabled
     * @param errorContainerColor the container color for this text field when in error state
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param selectionColors the colors used when the input text of this text field is selected
     * @param focusedBorderColor the border color for this text field when focused
     * @param unfocusedBorderColor the border color for this text field when not focused
     * @param disabledBorderColor the border color for this text field when disabled
     * @param errorBorderColor the border color for this text field when in error state
     * @param focusedLeadingIconColor the leading icon color for this text field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
     * @param disabledLeadingIconColor the leading icon color for this text field when disabled
     * @param errorLeadingIconColor the leading icon color for this text field when in error state
     * @param focusedTrailingIconColor the trailing icon color for this text field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not
     *   focused
     * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
     * @param errorTrailingIconColor the trailing icon color for this text field when in error state
     * @param focusedLabelColor the label color for this text field when focused
     * @param unfocusedLabelColor the label color for this text field when not focused
     * @param disabledLabelColor the label color for this text field when disabled
     * @param errorLabelColor the label color for this text field when in error state
     * @param focusedPlaceholderColor the placeholder color for this text field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     * @param errorPlaceholderColor the placeholder color for this text field when in error state
     * @param focusedPrefixColor the prefix color for this text field when focused
     * @param unfocusedPrefixColor the prefix color for this text field when not focused
     * @param disabledPrefixColor the prefix color for this text field when disabled
     * @param errorPrefixColor the prefix color for this text field when in error state
     * @param focusedSuffixColor the suffix color for this text field when focused
     * @param unfocusedSuffixColor the suffix color for this text field when not focused
     * @param disabledSuffixColor the suffix color for this text field when disabled
     * @param errorSuffixColor the suffix color for this text field when in error state
     */
    @Composable
    fun outlinedTextFieldColors(
        focusedTextColor: Color = OutlinedAutocompleteTokens.FieldFocusInputTextColor.value,
        unfocusedTextColor: Color = OutlinedAutocompleteTokens.FieldInputTextColor.value,
        disabledTextColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledInputTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledInputTextOpacity
            ),
        errorTextColor: Color = OutlinedAutocompleteTokens.FieldErrorInputTextColor.value,
        focusedContainerColor: Color = Color.Transparent,
        unfocusedContainerColor: Color = Color.Transparent,
        disabledContainerColor: Color = Color.Transparent,
        errorContainerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedAutocompleteTokens.TextFieldCaretColor.value,
        errorCursorColor: Color = OutlinedAutocompleteTokens.TextFieldErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedBorderColor: Color = OutlinedAutocompleteTokens.TextFieldFocusOutlineColor.value,
        unfocusedBorderColor: Color = OutlinedAutocompleteTokens.TextFieldOutlineColor.value,
        disabledBorderColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledOutlineColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledOutlineOpacity
            ),
        errorBorderColor: Color = OutlinedAutocompleteTokens.TextFieldErrorOutlineColor.value,
        focusedLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldFocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldLeadingIconColor.value,
        disabledLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledLeadingIconColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledLeadingIconOpacity
            ),
        errorLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldFocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldTrailingIconColor.value,
        disabledTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledTrailingIconColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledTrailingIconOpacity
            ),
        errorTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorTrailingIconColor.value,
        focusedLabelColor: Color = OutlinedAutocompleteTokens.FieldFocusLabelTextColor.value,
        unfocusedLabelColor: Color = OutlinedAutocompleteTokens.FieldLabelTextColor.value,
        disabledLabelColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledLabelTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledLabelTextOpacity
            ),
        errorLabelColor: Color = OutlinedAutocompleteTokens.FieldErrorLabelTextColor.value,
        focusedPlaceholderColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorPlaceholderColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        focusedPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPrefixColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        focusedSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        disabledSuffixColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
    ): TextFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            disabledContainerColor = disabledContainerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            selectionColors = selectionColors,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            disabledBorderColor = disabledBorderColor,
            errorBorderColor = errorBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    /**
     * Padding for [DropdownMenuItem]s within [ExposedDropdownMenuBoxScope.ExposedDropdownMenu] to
     * align them properly with [TextField] components.
     */
    val ItemContentPadding: PaddingValues =
        PaddingValues(horizontal = ExposedDropdownMenuItemHorizontalPadding, vertical = 0.dp)

    /**
     * Creates a [PopupProperties] used for [ExposedDropdownMenuBoxScope.ExposedDropdownMenu].
     *
     * @param anchorType the type of element that is anchoring the menu. See
     *   [ExposedDropdownMenuAnchorType].
     */
    @Composable
    internal fun popupProperties(anchorType: ExposedDropdownMenuAnchorType): PopupProperties {
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
            anchorType == ExposedDropdownMenuAnchorType.PrimaryEditable ||
                (anchorType == ExposedDropdownMenuAnchorType.SecondaryEditable &&
                    !a11yServicesEnabled)
        if (imeRequired) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        return PopupProperties(flags = flags)
    }

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @ExperimentalMaterial3Api
    @Composable
    fun TrailingIcon(expanded: Boolean) = TrailingIcon(expanded, Modifier)

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @Composable
    fun textFieldColors(
        focusedTextColor: Color = FilledAutocompleteTokens.FieldFocusInputTextColor.value,
        unfocusedTextColor: Color = FilledAutocompleteTokens.FieldInputTextColor.value,
        disabledTextColor: Color =
            FilledAutocompleteTokens.FieldDisabledInputTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledInputTextOpacity
            ),
        errorTextColor: Color = FilledAutocompleteTokens.FieldErrorInputTextColor.value,
        containerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.value,
        errorContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.value,
        cursorColor: Color = FilledAutocompleteTokens.TextFieldCaretColor.value,
        errorCursorColor: Color = FilledAutocompleteTokens.TextFieldErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldFocusActiveIndicatorColor.value,
        unfocusedIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldActiveIndicatorColor.value,
        disabledIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledActiveIndicatorColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledActiveIndicatorOpacity
            ),
        errorIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldErrorActiveIndicatorColor.value,
        focusedLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldFocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color = FilledAutocompleteTokens.TextFieldLeadingIconColor.value,
        disabledLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledLeadingIconColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledLeadingIconOpacity
            ),
        errorLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldFocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldTrailingIconColor.value,
        disabledTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledTrailingIconColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledTrailingIconOpacity
            ),
        errorTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldErrorTrailingIconColor.value,
        focusedLabelColor: Color = FilledAutocompleteTokens.FieldFocusLabelTextColor.value,
        unfocusedLabelColor: Color = FilledAutocompleteTokens.FieldLabelTextColor.value,
        disabledLabelColor: Color = FilledAutocompleteTokens.FieldDisabledLabelTextColor.value,
        errorLabelColor: Color = FilledAutocompleteTokens.FieldErrorLabelTextColor.value,
        focusedPlaceholderColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedPlaceholderColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPlaceholderColor: Color =
            FilledAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorPlaceholderColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        focusedPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPrefixColor: Color =
            FilledAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        focusedSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        disabledSuffixColor: Color =
            FilledAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
    ): TextFieldColors =
        textFieldColors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            selectionColors = selectionColors,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @Composable
    fun outlinedTextFieldColors(
        focusedTextColor: Color = OutlinedAutocompleteTokens.FieldFocusInputTextColor.value,
        unfocusedTextColor: Color = OutlinedAutocompleteTokens.FieldInputTextColor.value,
        disabledTextColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledInputTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledInputTextOpacity
            ),
        errorTextColor: Color = OutlinedAutocompleteTokens.FieldErrorInputTextColor.value,
        containerColor: Color = Color.Transparent,
        errorContainerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedAutocompleteTokens.TextFieldCaretColor.value,
        errorCursorColor: Color = OutlinedAutocompleteTokens.TextFieldErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedBorderColor: Color = OutlinedAutocompleteTokens.TextFieldFocusOutlineColor.value,
        unfocusedBorderColor: Color = OutlinedAutocompleteTokens.TextFieldOutlineColor.value,
        disabledBorderColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledOutlineColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledOutlineOpacity
            ),
        errorBorderColor: Color = OutlinedAutocompleteTokens.TextFieldErrorOutlineColor.value,
        focusedLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldFocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldLeadingIconColor.value,
        disabledLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledLeadingIconColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledLeadingIconOpacity
            ),
        errorLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldFocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldTrailingIconColor.value,
        disabledTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledTrailingIconColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledTrailingIconOpacity
            ),
        errorTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorTrailingIconColor.value,
        focusedLabelColor: Color = OutlinedAutocompleteTokens.FieldFocusLabelTextColor.value,
        unfocusedLabelColor: Color = OutlinedAutocompleteTokens.FieldLabelTextColor.value,
        disabledLabelColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledLabelTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledLabelTextOpacity
            ),
        errorLabelColor: Color = OutlinedAutocompleteTokens.FieldErrorLabelTextColor.value,
        focusedPlaceholderColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorPlaceholderColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        focusedPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPrefixColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        focusedSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        unfocusedSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        disabledSuffixColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
            ),
        errorSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
    ): TextFieldColors =
        outlinedTextFieldColors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            selectionColors = selectionColors,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            disabledBorderColor = disabledBorderColor,
            errorBorderColor = errorBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @Composable
    fun textFieldColors(
        textColor: Color = FilledAutocompleteTokens.FieldInputTextColor.value,
        disabledTextColor: Color =
            FilledAutocompleteTokens.FieldDisabledInputTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledInputTextOpacity
            ),
        containerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.value,
        cursorColor: Color = FilledAutocompleteTokens.TextFieldCaretColor.value,
        errorCursorColor: Color = FilledAutocompleteTokens.TextFieldErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldFocusActiveIndicatorColor.value,
        unfocusedIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldActiveIndicatorColor.value,
        disabledIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledActiveIndicatorColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledActiveIndicatorOpacity
            ),
        errorIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldErrorActiveIndicatorColor.value,
        focusedLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldFocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color = FilledAutocompleteTokens.TextFieldLeadingIconColor.value,
        disabledLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledLeadingIconColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledLeadingIconOpacity
            ),
        errorLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldFocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldTrailingIconColor.value,
        disabledTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledTrailingIconColor.value.copy(
                alpha = FilledAutocompleteTokens.TextFieldDisabledTrailingIconOpacity
            ),
        errorTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldErrorTrailingIconColor.value,
        focusedLabelColor: Color = FilledAutocompleteTokens.FieldFocusLabelTextColor.value,
        unfocusedLabelColor: Color = FilledAutocompleteTokens.FieldLabelTextColor.value,
        disabledLabelColor: Color = FilledAutocompleteTokens.FieldDisabledLabelTextColor.value,
        errorLabelColor: Color = FilledAutocompleteTokens.FieldErrorLabelTextColor.value,
        placeholderColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPlaceholderColor: Color =
            FilledAutocompleteTokens.FieldDisabledInputTextColor.value.copy(
                alpha = FilledAutocompleteTokens.FieldDisabledInputTextOpacity
            )
    ): TextFieldColors =
        textFieldColors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = textColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = containerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            selectionColors = selectionColors,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = placeholderColor,
            unfocusedPlaceholderColor = placeholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = placeholderColor,
            focusedPrefixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            unfocusedPrefixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            disabledPrefixColor =
                OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                    alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
                ),
            errorPrefixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            focusedSuffixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            unfocusedSuffixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            disabledSuffixColor =
                OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                    alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
                ),
            errorSuffixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @Composable
    fun outlinedTextFieldColors(
        textColor: Color = OutlinedAutocompleteTokens.FieldInputTextColor.value,
        disabledTextColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledInputTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledInputTextOpacity
            ),
        containerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedAutocompleteTokens.TextFieldCaretColor.value,
        errorCursorColor: Color = OutlinedAutocompleteTokens.TextFieldErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedBorderColor: Color = OutlinedAutocompleteTokens.TextFieldFocusOutlineColor.value,
        unfocusedBorderColor: Color = OutlinedAutocompleteTokens.TextFieldOutlineColor.value,
        disabledBorderColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledOutlineColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledOutlineOpacity
            ),
        errorBorderColor: Color = OutlinedAutocompleteTokens.TextFieldErrorOutlineColor.value,
        focusedLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldFocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldLeadingIconColor.value,
        disabledLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledLeadingIconColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledLeadingIconOpacity
            ),
        errorLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldFocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldTrailingIconColor.value,
        disabledTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledTrailingIconColor.value.copy(
                alpha = OutlinedAutocompleteTokens.TextFieldDisabledTrailingIconOpacity
            ),
        errorTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorTrailingIconColor.value,
        focusedLabelColor: Color = OutlinedAutocompleteTokens.FieldFocusLabelTextColor.value,
        unfocusedLabelColor: Color = OutlinedAutocompleteTokens.FieldLabelTextColor.value,
        disabledLabelColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledLabelTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledLabelTextOpacity
            ),
        errorLabelColor: Color = OutlinedAutocompleteTokens.FieldErrorLabelTextColor.value,
        placeholderColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        disabledPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledInputTextColor.value.copy(
                alpha = OutlinedAutocompleteTokens.FieldDisabledInputTextOpacity
            )
    ): TextFieldColors =
        outlinedTextFieldColors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = textColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            errorContainerColor = containerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            selectionColors = selectionColors,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            disabledBorderColor = disabledBorderColor,
            errorBorderColor = errorBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = placeholderColor,
            unfocusedPlaceholderColor = placeholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = placeholderColor,
            focusedPrefixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            unfocusedPrefixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            disabledPrefixColor =
                OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                    alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
                ),
            errorPrefixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            focusedSuffixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            unfocusedSuffixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
            disabledSuffixColor =
                OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.value.copy(
                    alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity
                ),
            errorSuffixColor = OutlinedAutocompleteTokens.FieldSupportingTextColor.value,
        )
}

@Stable
internal class ExposedDropdownMenuPositionProvider(
    val density: Density,
    val topWindowInsets: Int,
    val keyboardSignalState: State<Unit>? = null,
    val verticalMargin: Int = with(density) { MenuVerticalMargin.roundToPx() },
    val onPositionCalculated: (anchorBounds: IntRect, menuBounds: IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    // Horizontal position
    private val startToAnchorStart = MenuPosition.startToAnchorStart()
    private val endToAnchorEnd = MenuPosition.endToAnchorEnd()
    private val leftToWindowLeft = MenuPosition.leftToWindowLeft()
    private val rightToWindowRight = MenuPosition.rightToWindowRight()

    // Vertical position
    private val topToAnchorBottom = MenuPosition.topToAnchorBottom()
    private val bottomToAnchorTop = MenuPosition.bottomToAnchorTop()
    private val topToWindowTop = MenuPosition.topToWindowTop(margin = verticalMargin)
    private val bottomToWindowBottom = MenuPosition.bottomToWindowBottom(margin = verticalMargin)

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Workaround for b/326394521
        // Read the state because we want any changes to the state to trigger recalculation.
        // See PopupLayout.snapshotStateObserver and PopupLayout.onCommitAffectingPopupPosition
        // for more info.
        keyboardSignalState?.value

        // Workaround for b/256233441
        // Popup fails to account for window insets so we do it here instead
        @Suppress("NAME_SHADOWING")
        val windowSize = IntSize(windowSize.width, windowSize.height + topWindowInsets)

        val xCandidates =
            listOf(
                startToAnchorStart,
                endToAnchorEnd,
                if (anchorBounds.center.x < windowSize.width / 2) {
                    leftToWindowLeft
                } else {
                    rightToWindowRight
                }
            )
        var x = 0
        for (index in xCandidates.indices) {
            val xCandidate =
                xCandidates[index].position(
                    anchorBounds = anchorBounds,
                    windowSize = windowSize,
                    menuWidth = popupContentSize.width,
                    layoutDirection = layoutDirection
                )
            if (
                index == xCandidates.lastIndex ||
                    (xCandidate >= 0 && xCandidate + popupContentSize.width <= windowSize.width)
            ) {
                x = xCandidate
                break
            }
        }

        val yCandidates =
            listOf(
                topToAnchorBottom,
                bottomToAnchorTop,
                if (anchorBounds.center.y < windowSize.height / 2) {
                    topToWindowTop
                } else {
                    bottomToWindowBottom
                }
            )
        var y = 0
        for (index in yCandidates.indices) {
            val yCandidate =
                yCandidates[index].position(
                    anchorBounds = anchorBounds,
                    windowSize = windowSize,
                    menuHeight = popupContentSize.height
                )
            if (
                index == yCandidates.lastIndex ||
                    (yCandidate >= 0 && yCandidate + popupContentSize.height <= windowSize.height)
            ) {
                y = yCandidate
                break
            }
        }

        val menuOffset = IntOffset(x, y)
        onPositionCalculated(
            /* anchorBounds = */ anchorBounds,
            /* menuBounds = */ IntRect(offset = menuOffset, size = popupContentSize)
        )
        return menuOffset
    }
}

private fun Modifier.expandable(
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    anchorType: ExposedDropdownMenuAnchorType,
    expandedDescription: String,
    collapsedDescription: String,
    toggleDescription: String,
    keyboardController: SoftwareKeyboardController?,
) =
    pointerInput(onExpandedChange) {
            awaitEachGesture {
                // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                // in the Initial pass to observe events before the text field consumes them
                // in the Main pass.
                val downEvent = awaitFirstDown(pass = PointerEventPass.Initial)
                if (anchorType == ExposedDropdownMenuAnchorType.SecondaryEditable) {
                    downEvent.consume()
                }
                val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (upEvent != null) {
                    onExpandedChange()
                }
            }
        }
        .semantics {
            if (anchorType == ExposedDropdownMenuAnchorType.SecondaryEditable) {
                role = Role.Button
                stateDescription = if (expanded) expandedDescription else collapsedDescription
                contentDescription = toggleDescription
            } else {
                role = Role.DropdownList
            }
            onClick {
                onExpandedChange()
                if (anchorType == ExposedDropdownMenuAnchorType.PrimaryEditable) {
                    keyboardController?.show()
                }
                true
            }
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

// Don't use `boundsInWindow()` because it can report 0 when the window is animating/resizing
private fun LayoutCoordinates?.getAnchorBounds(): Rect =
    if (this == null || !this.isAttached) Rect.Zero else Rect(positionInWindow(), size.toSize())

private val ExposedDropdownMenuItemHorizontalPadding = 16.dp
