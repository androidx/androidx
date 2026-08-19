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

import androidx.compose.material3.tokens.CheckboxTokens
import androidx.compose.material3.tokens.ColorToken
import androidx.compose.material3.tokens.RadioButtonTokens
import androidx.compose.material3.tokens.ShapeToken
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

    fun disabled(disabled: Boolean) = set(DISABLED, disabled)

    fun checked(checked: Boolean) = set(CHECKED, checked)

    fun selected(selected: Boolean) = set(CHECKED, selected)

    fun focused(focused: Boolean) = set(FOCUSED, focused)

    fun indeterminate(indeterminate: Boolean) = set(INDETERMINATE, indeterminate)

    companion object {
        const val NONE = 0
        const val DISABLED = 1 shl 0 // 0b001 = 1
        const val CHECKED = 1 shl 1 // 0b010 = 2
        const val FOCUSED = 1 shl 2 // 0b100 = 4
        const val INDETERMINATE = 1 shl 3 // 0b1000 = 8

        val Default = ComponentState()

        fun of(vararg states: Int): ComponentState {
            var combined = 0
            for (f in states) combined = combined or f
            return ComponentState(combined)
        }

        fun disabled(disabled: Boolean) = Default.disabled(disabled)

        fun checked(checked: Boolean) = Default.checked(checked)

        fun selected(selected: Boolean) = Default.selected(selected)

        fun focused(focused: Boolean) = Default.focused(focused)

        fun indeterminate(indeterminate: Boolean) = Default.indeterminate(indeterminate)
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

    fun selected(style: T.() -> Unit) = checked(style)

    fun unselected(style: T.() -> Unit) = unchecked(style)
}

internal interface IndeterminateState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun indeterminate(style: T.() -> Unit) = setState(ComponentState.INDETERMINATE, style)
}

internal interface DisabledState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun enabled(style: T.() -> Unit) = setNotState(ComponentState.DISABLED, style)

    fun disabled(style: T.() -> Unit) = setState(ComponentState.DISABLED, style)
}

// Component style definitions start from here.

@JvmInline
internal value class CheckboxStyle(private val block: CheckboxStyleScope.() -> Unit) {
    fun CheckboxStyleScope.applyStyle() {
        block()
    }

    companion object {
        val Default = CheckboxStyle {
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
    }
}

internal class CheckboxStyleScope(
    override val theme: MaterialTheme.Values,
    override val state: ComponentState = ComponentState.Default,
) :
    CheckedState<CheckboxStyleScope>,
    IndeterminateState<CheckboxStyleScope>,
    DisabledState<CheckboxStyleScope>,
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
    CheckedState<RadioButtonStyleScope>,
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
