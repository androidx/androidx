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

package androidx.compose.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.tokens.AppBarTokens
import androidx.compose.material3.tokens.CheckboxTokens
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.ColorToken
import androidx.compose.material3.tokens.RadioButtonTokens
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.material3.tokens.SearchBarTokens
import androidx.compose.material3.tokens.SearchViewTokens
import androidx.compose.material3.tokens.ShapeToken
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline

// Note that this file is supposed to be removed after the Style integration is done and should
// contain no public APIs. We created these internal APIs that "imitate" the Style API to unblock
// the development of features depending on the Style API before it's ready to be used.

// Below definitions include the base data structure of our "fake" component styles to support
// stateful styles.

@JvmInline
internal value class ComponentState(val mask: Int = 0) {
    infix fun has(flag: Int): Boolean = (mask and flag) != 0

    infix fun with(flag: Int): ComponentState = ComponentState(mask or flag)

    infix fun without(flag: Int): ComponentState = ComponentState(mask and flag.inv())

    fun set(flag: Int, with: Boolean): ComponentState = if (with) with(flag) else without(flag)

    fun enabled(enabled: Boolean) = set(ENABLED, enabled)

    fun checked(checked: Boolean) = set(CHECKED, checked)

    fun selected(selected: Boolean) = set(CHECKED, selected)

    fun indeterminate(indeterminate: Boolean) = set(INDETERMINATE, indeterminate)

    fun expanded(expanded: Boolean) = set(EXPANDED, expanded)

    @Composable
    fun interactionState(interactionSource: MutableInteractionSource): ComponentState {
        return set(PRESSED, interactionSource.collectIsPressedAsState().value)
            .set(FOCUSED, interactionSource.collectIsFocusedAsState().value)
            .set(HOVERED, interactionSource.collectIsHoveredAsState().value)
            .set(DRAGGED, interactionSource.collectIsDraggedAsState().value)
    }

    companion object {
        const val NONE = 0
        const val ENABLED = 1 shl 0
        const val CHECKED = 1 shl 1
        const val SELECTED = 1 shl 2
        const val INDETERMINATE = 1 shl 3
        const val PRESSED = 1 shl 4
        const val FOCUSED = 1 shl 5
        const val HOVERED = 1 shl 6
        const val DRAGGED = 1 shl 7
        const val EXPANDED = 1 shl 8

        val Default = ComponentState(ENABLED)

        fun of(vararg states: Int): ComponentState {
            var combined = 0
            for (f in states) combined = combined or f
            return ComponentState(combined)
        }

        fun enabled(enabled: Boolean) = Default.enabled(enabled)

        fun checked(checked: Boolean) = Default.checked(checked)

        fun selected(selected: Boolean) = Default.selected(selected)

        fun indeterminate(indeterminate: Boolean) = Default.indeterminate(indeterminate)

        fun expanded(expanded: Boolean) = Default.expanded(expanded)

        @Composable
        fun interactionState(interactionSource: MutableInteractionSource) =
            Default.interactionState(interactionSource)
    }
}

internal interface StyleResolver {
    fun resolve(block: () -> Unit)
}

private class StyleResolverImpl : StyleResolver {
    private val pendingBlocks: MutableList<() -> Unit> = mutableListOf()
    private var resolvingIndex = -1

    override fun resolve(block: () -> Unit) {
        pendingBlocks.add(block)
        if (resolvingIndex == -1) {
            resolvingIndex = 0
            while (resolvingIndex < pendingBlocks.size) {
                pendingBlocks[resolvingIndex++]()
            }
            pendingBlocks.clear()
            resolvingIndex = -1
        }
    }
}

@Suppress("UNCHECKED_CAST") // Guaranteed by implementation
internal interface StatefulStyleScope<T : StatefulStyleScope<T>> : StyleResolver {
    val state: ComponentState

    fun setState(state: Int, style: T.() -> Unit) {
        if (this.state has state) {
            resolve { (this as T).style() }
        }
    }

    fun setNotState(state: Int, style: T.() -> Unit) {
        if (!(this.state has state)) {
            resolve { (this as T).style() }
        }
    }
}

@Composable internal expect fun mediaQueryInfo(): MediaQueryInfo

internal class MediaQueryInfo(val isLaptop: Boolean, val isTv: Boolean, val isAuto: Boolean)

@Suppress("UNCHECKED_CAST") // Guaranteed by implementation
internal interface AdaptiveStyleScope<T : AdaptiveStyleScope<T>> {
    val mediaQueryInfo: MediaQueryInfo

    fun laptop(style: T.() -> Unit) {
        if (mediaQueryInfo.isLaptop) {
            (this as T).style()
        }
    }

    fun tv(style: T.() -> Unit) {
        if (mediaQueryInfo.isTv) {
            (this as T).style()
        }
    }

    fun auto(style: T.() -> Unit) {
        if (mediaQueryInfo.isAuto) {
            (this as T).style()
        }
    }
}

internal interface MaterialThemeAccessorScope {
    val theme: MaterialTheme.Values

    val ColorToken.value: Color
        get() = theme.colorScheme.fromToken(this)

    val ShapeToken.value: Shape
        get() = theme.shapes.fromToken(this)
}

internal interface CheckedState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun checked(style: T.() -> Unit) = setState(ComponentState.CHECKED, style)

    fun unchecked(style: T.() -> Unit) = setNotState(ComponentState.CHECKED, style)
}

internal interface SelectedState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun selected(style: T.() -> Unit) = setState(ComponentState.SELECTED, style)

    fun unselected(style: T.() -> Unit) = setNotState(ComponentState.SELECTED, style)
}

internal interface IndeterminateState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun indeterminate(style: T.() -> Unit) = setState(ComponentState.INDETERMINATE, style)
}

internal interface DisabledState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun enabled(style: T.() -> Unit) = setState(ComponentState.ENABLED, style)

    fun disabled(style: T.() -> Unit) = setNotState(ComponentState.ENABLED, style)
}

internal interface InteractionState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun pressed(style: T.() -> Unit) = setState(ComponentState.PRESSED, style)

    fun unpressed(style: T.() -> Unit) = setNotState(ComponentState.PRESSED, style)

    fun focused(style: T.() -> Unit) = setState(ComponentState.FOCUSED, style)

    fun unfocused(style: T.() -> Unit) = setNotState(ComponentState.FOCUSED, style)

    fun hovered(style: T.() -> Unit) = setState(ComponentState.HOVERED, style)

    fun unhovered(style: T.() -> Unit) = setNotState(ComponentState.HOVERED, style)

    fun dragged(style: T.() -> Unit) = setState(ComponentState.DRAGGED, style)

    fun undragged(style: T.() -> Unit) = setNotState(ComponentState.DRAGGED, style)
}

internal interface ExpandedState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun expanded(style: T.() -> Unit) = setState(ComponentState.EXPANDED, style)

    fun collapsed(style: T.() -> Unit) = setNotState(ComponentState.EXPANDED, style)
}

// Component style definitions start from here.

internal fun <T : StyleResolver> T.resolve(style: ComponentStyle<T>): T {
    resolve { with(style) { applyStyle() } }
    return this
}

internal fun interface ComponentStyle<T : StyleResolver> {
    fun T.applyStyle()
}

internal fun interface CheckboxStyle : ComponentStyle<CheckboxStyleScope> {
    infix fun then(other: CheckboxStyle): CheckboxStyle = CheckboxStyle {
        this.applyStyle()
        with(other) { applyStyle() }
    }

    companion object {
        val Default = CheckboxStyle {
            size(CheckboxTokens.ContainerSize)
            cornerSize(2.dp)
            checkmarkColor(Color.Transparent)
            backgroundColor(Color.Transparent)
            borderColor(CheckboxTokens.UnselectedOutlineColor.value)
            rippleColor(Color.Transparent)
            checkmarkStroke(Stroke(CheckboxTokens.UnselectedOutlineWidth.value))
            borderStroke(Stroke(CheckboxTokens.UnselectedOutlineWidth.value))
            disabled {
                unchecked {
                    checkmarkColor(Color.Transparent)
                    backgroundColor(Color.Transparent)
                    borderColor(CheckboxTokens.UnselectedDisabledOutlineColor.value)
                    rippleColor(Color.Transparent)
                }
                checked {
                    checkmarkColor(CheckboxTokens.SelectedIconColor.value)
                    backgroundColor(
                        CheckboxTokens.SelectedDisabledContainerColor.value.copy(
                            alpha = CheckboxTokens.SelectedDisabledContainerOpacity
                        )
                    )
                    borderColor(
                        CheckboxTokens.SelectedDisabledContainerColor.value.copy(
                            alpha = CheckboxTokens.SelectedDisabledContainerOpacity
                        )
                    )
                    rippleColor(
                        CheckboxTokens.SelectedDisabledContainerColor.value.copy(
                            alpha = CheckboxTokens.SelectedDisabledContainerOpacity
                        )
                    )
                }
                indeterminate {
                    backgroundColor(
                        CheckboxTokens.SelectedDisabledContainerColor.value.copy(
                            alpha = CheckboxTokens.SelectedDisabledContainerOpacity
                        )
                    )
                    borderColor(
                        CheckboxTokens.SelectedDisabledContainerColor.value.copy(
                            alpha = CheckboxTokens.SelectedDisabledContainerOpacity
                        )
                    )
                    rippleColor(
                        CheckboxTokens.SelectedDisabledContainerColor.value.copy(
                            alpha = CheckboxTokens.SelectedDisabledContainerOpacity
                        )
                    )
                }
            }
        }

        val Adaptive =
            Default then
                {
                    auto {
                        size(22.dp)
                        cornerSize(4.dp)
                    }
                    tv {
                        size(22.dp)
                        cornerSize(4.dp)
                    }
                }
    }
}

internal class CheckboxStyleScope(
    override val theme: MaterialTheme.Values,
    override val mediaQueryInfo: MediaQueryInfo,
    override val state: ComponentState = ComponentState.Default,
) :
    CheckedState<CheckboxStyleScope>,
    IndeterminateState<CheckboxStyleScope>,
    DisabledState<CheckboxStyleScope>,
    AdaptiveStyleScope<CheckboxStyleScope>,
    MaterialThemeAccessorScope,
    StyleResolver by StyleResolverImpl() {

    var checkmarkColor: Color = Color.Unspecified
        private set

    var borderColor: Color = Color.Unspecified
        private set

    var rippleColor: Color = Color.Unspecified
        private set

    var backgroundColor: Color = Color.Unspecified
        private set

    var checkmarkStroke: Stroke? = null
        private set

    var borderStroke: Stroke? = null
        private set

    var cornerSize: Dp = Dp.Unspecified

    var size: Dp = Dp.Unspecified

    fun checkmarkStroke(stroke: Stroke) {
        checkmarkStroke = stroke
    }

    fun borderStroke(stroke: Stroke) {
        borderStroke = stroke
    }

    fun borderColor(color: Color) {
        borderColor = color
    }

    fun checkmarkColor(color: Color) {
        checkmarkColor = color
    }

    fun backgroundColor(color: Color) {
        backgroundColor = color
    }

    fun rippleColor(color: Color) {
        rippleColor = color
    }

    fun cornerSize(size: Dp) {
        cornerSize = size
    }

    fun size(size: Dp) {
        this.size = size
    }
}

internal fun interface RadioButtonStyle : ComponentStyle<RadioButtonStyleScope> {
    infix fun then(other: RadioButtonStyle): RadioButtonStyle = RadioButtonStyle {
        this.applyStyle()
        with(other) { applyStyle() }
    }

    companion object {
        val Default = RadioButtonStyle {
            radioColor(RadioButtonTokens.UnselectedIconColor.value)
            dotRadius(0.dp)
            selected {
                radioColor(RadioButtonTokens.SelectedIconColor.value)
                dotRadius(6.dp)
            }
            disabled {
                unselected {
                    radioColor(
                        RadioButtonTokens.DisabledUnselectedIconColor.value.copy(
                            alpha = RadioButtonTokens.DisabledSelectedIconOpacity
                        )
                    )
                }
                selected {
                    radioColor(
                        RadioButtonTokens.DisabledSelectedIconColor.value.copy(
                            alpha = RadioButtonTokens.DisabledUnselectedIconOpacity
                        )
                    )
                }
            }
        }
    }
}

internal class RadioButtonStyleScope(
    override val theme: MaterialTheme.Values,
    override val state: ComponentState = ComponentState.Default,
) :
    SelectedState<RadioButtonStyleScope>,
    DisabledState<RadioButtonStyleScope>,
    MaterialThemeAccessorScope,
    StyleResolver by StyleResolverImpl() {
    var radioColor: Color = Color.Unspecified
        private set

    var dotRadius: Dp = Dp.Unspecified
        private set

    fun radioColor(color: Color) {
        radioColor = color
    }

    fun dotRadius(radius: Dp) {
        dotRadius = radius
    }
}

internal fun interface SearchBarStyle : ComponentStyle<SearchBarStyleScope> {
    infix fun then(other: SearchBarStyle): SearchBarStyle = SearchBarStyle {
        this.applyStyle()
        with(other) { applyStyle() }
    }

    companion object {
        val Default = SearchBarStyle {
            containerColor(SearchBarTokens.ContainerColor.value)
            shape(SearchBarTokens.ContainerShape.value)
            tonalElevation(SearchBarDefaults.TonalElevation)
            shadowElevation(SearchBarDefaults.ShadowElevation)
        }

        val ExpandedFullScreenContained = SearchBarStyle {
            containerColor(SearchBarTokens.ContainerColor.value)
            expanded { containerColor(ColorSchemeKeyTokens.SurfaceContainerLow.value) }
            shape(SearchBarTokens.ContainerShape.value)
            tonalElevation(SearchBarDefaults.TonalElevation)
            shadowElevation(SearchBarDefaults.ShadowElevation)
        }

        val ExpandedFullScreen = SearchBarStyle {
            containerColor(SearchBarTokens.ContainerColor.value)
            shape(SearchBarTokens.ContainerShape.value)
            tonalElevation(SearchBarDefaults.TonalElevation)
            shadowElevation(SearchBarDefaults.ShadowElevation)
            dividerColor(SearchViewTokens.DividerColor.value)
        }
    }
}

internal class SearchBarStyleScope(
    override val theme: MaterialTheme.Values,
    override val state: ComponentState = ComponentState.Default,
) :
    ExpandedState<SearchBarStyleScope>,
    MaterialThemeAccessorScope,
    StyleResolver by StyleResolverImpl() {
    var containerColor: Color = Color.Unspecified
        private set

    var shape: Shape = RectangleShape
        private set

    var tonalElevation: Dp = Dp.Unspecified
        private set

    var shadowElevation: Dp = Dp.Unspecified
        private set

    var dividerColor: Color = Color.Unspecified
        private set

    fun containerColor(color: Color) {
        containerColor = color
    }

    fun shape(shape: Shape) {
        this.shape = shape
    }

    fun tonalElevation(tonalElevation: Dp) {
        this.tonalElevation = tonalElevation
    }

    fun shadowElevation(shadowElevation: Dp) {
        this.shadowElevation = shadowElevation
    }

    fun dividerColor(color: Color) {
        dividerColor = color
    }
}

@JvmInline
internal value class AppBarWithSearchStyle(
    private val block: AppBarWithSearchStyleScope.() -> Unit
) {
    fun AppBarWithSearchStyleScope.applyStyle() {
        block()
    }

    companion object {
        val Default = AppBarWithSearchStyle {
            searchBarContainerColor(SearchBarTokens.ContainerColor.value)
            scrolledSearchBarContainerColor(ColorSchemeKeyTokens.SurfaceContainerHighest.value)
            appBarContainerColor(AppBarTokens.ContainerColor.value)
            scrolledAppBarContainerColor(AppBarTokens.OnScrollContainerColor.value)
            appBarNavigationIconColor(AppBarTokens.LeadingIconColor.value)
            appBarActionIconColor(AppBarTokens.TrailingIconColor.value)
            shape(SearchBarTokens.ContainerShape.value)
            tonalElevation(SearchBarDefaults.TonalElevation)
            shadowElevation(SearchBarDefaults.ShadowElevation)
            contentPadding(0.dp, 0.dp, 0.dp, 0.dp)
        }
    }
}

internal class AppBarWithSearchStyleScope(override val theme: MaterialTheme.Values) :
    MaterialThemeAccessorScope {
    var searchBarContainerColor: Color = Color.Unspecified
        private set

    var scrolledSearchBarContainerColor: Color = Color.Unspecified
        private set

    var appBarContainerColor: Color = Color.Unspecified
        private set

    var scrolledAppBarContainerColor: Color = Color.Unspecified
        private set

    var appBarNavigationIconColor: Color = Color.Unspecified
        private set

    var appBarActionIconColor: Color = Color.Unspecified
        private set

    var shape: Shape = RectangleShape
        private set

    var tonalElevation: Dp = Dp.Unspecified
        private set

    var shadowElevation: Dp = Dp.Unspecified
        private set

    var contentPaddingStart: Dp = Dp.Unspecified
        private set

    var contentPaddingTop: Dp = Dp.Unspecified
        private set

    var contentPaddingEnd: Dp = Dp.Unspecified
        private set

    var contentPaddingBottom: Dp = Dp.Unspecified
        private set

    fun searchBarContainerColor(color: Color) {
        searchBarContainerColor = color
    }

    fun scrolledSearchBarContainerColor(color: Color) {
        scrolledSearchBarContainerColor = color
    }

    fun appBarContainerColor(color: Color) {
        appBarContainerColor = color
    }

    fun scrolledAppBarContainerColor(color: Color) {
        scrolledAppBarContainerColor = color
    }

    fun appBarNavigationIconColor(color: Color) {
        appBarNavigationIconColor = color
    }

    fun appBarActionIconColor(color: Color) {
        appBarActionIconColor = color
    }

    fun shape(shape: Shape) {
        this.shape = shape
    }

    fun tonalElevation(tonalElevation: Dp) {
        this.tonalElevation = tonalElevation
    }

    fun shadowElevation(shadowElevation: Dp) {
        this.shadowElevation = shadowElevation
    }

    fun contentPadding(start: Dp, top: Dp, end: Dp, bottom: Dp) {
        contentPaddingStart = start
        contentPaddingTop = top
        contentPaddingEnd = end
        contentPaddingBottom = bottom
    }
}

@JvmInline
internal value class ExpandedDockedSearchBarStyle(
    private val block: ExpandedDockedSearchBarStyleScope.() -> Unit
) {
    fun ExpandedDockedSearchBarStyleScope.applyStyle() {
        block()
    }

    companion object {
        val Default = ExpandedDockedSearchBarStyle {
            shape(SearchViewTokens.DockedContainerShape.value)
            containerColor(SearchBarTokens.ContainerColor.value)
            dividerColor(SearchViewTokens.DividerColor.value)
            tonalElevation(SearchBarDefaults.TonalElevation)
            shadowElevation(SearchBarDefaults.ShadowElevation)
        }

        val WithGap = ExpandedDockedSearchBarStyle {
            shape(SearchViewTokens.DockedContainerShape.value)
            dropdownShape(SearchBarDefaults.dockedDropdownShape)
            dropdownGapSize(SearchBarDefaults.dockedDropdownGapSize)
            dropdownScrimColor(ScrimTokens.ContainerColor.value)
            containerColor(SearchBarTokens.ContainerColor.value)
            dividerColor(SearchViewTokens.DividerColor.value)
            tonalElevation(SearchBarDefaults.TonalElevation)
            shadowElevation(SearchBarDefaults.ShadowElevation)
        }
    }
}

internal class ExpandedDockedSearchBarStyleScope(override val theme: MaterialTheme.Values) :
    MaterialThemeAccessorScope {
    var shape: Shape = RectangleShape
        private set

    var containerColor: Color = Color.Unspecified
        private set

    var dividerColor: Color = Color.Unspecified
        private set

    var tonalElevation: Dp = Dp.Unspecified
        private set

    var shadowElevation: Dp = Dp.Unspecified
        private set

    var dropdownShape: Shape? = null
        private set

    var dropdownGapSize: Dp? = null
        private set

    var dropdownScrimColor: Color = Color.Unspecified
        private set

    fun shape(shape: Shape) {
        this.shape = shape
    }

    fun containerColor(color: Color) {
        containerColor = color
    }

    fun dividerColor(color: Color) {
        dividerColor = color
    }

    fun tonalElevation(tonalElevation: Dp) {
        this.tonalElevation = tonalElevation
    }

    fun shadowElevation(shadowElevation: Dp) {
        this.shadowElevation = shadowElevation
    }

    fun dropdownShape(shape: Shape) {
        this.dropdownShape = shape
    }

    fun dropdownGapSize(color: Dp) {
        this.dropdownGapSize = color
    }

    fun dropdownScrimColor(color: Color) {
        this.dropdownScrimColor = color
    }
}
