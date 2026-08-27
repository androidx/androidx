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
import androidx.compose.material3.tokens.CheckboxTokens
import androidx.compose.material3.tokens.ColorToken
import androidx.compose.material3.tokens.RadioButtonTokens
import androidx.compose.material3.tokens.ShapeToken
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

        @Composable
        fun interactionState(interactionSource: MutableInteractionSource) =
            Default.interactionState(interactionSource)
    }
}

@Suppress("UNCHECKED_CAST") // Guaranteed by implementation
internal interface StatefulStyleScope<T : StatefulStyleScope<T>> {
    val state: ComponentState

    fun setState(state: Int, style: T.() -> Unit) {
        if (this.state has state) {
            (this as T).style()
        }
    }

    fun setNotState(state: Int, style: T.() -> Unit) {
        if (!(this.state has state)) {
            (this as T).style()
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

// Component style definitions start from here.

@JvmInline
internal value class CheckboxStyle
private constructor(private val block: CheckboxStyleScope.() -> Unit) {
    fun CheckboxStyleScope.applyStyle() {
        block()
    }

    infix fun then(other: CheckboxStyle): CheckboxStyle = then(other.block)

    infix fun then(block: CheckboxStyleScope.() -> Unit) = CheckboxStyle {
        applyStyle()
        block()
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
    MaterialThemeAccessorScope {
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

@JvmInline
internal value class RadioButtonStyle(private val block: RadioButtonStyleScope.() -> Unit) {
    fun RadioButtonStyleScope.applyStyle() {
        block()
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
    MaterialThemeAccessorScope {
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
