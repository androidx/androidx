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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

    companion object {
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
}

internal interface CheckedState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun checked(style: T.() -> Unit) = setState(ComponentState.CHECKED, style)
}

internal interface IndeterminateState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun indeterminate(style: T.() -> Unit) = setState(ComponentState.INDETERMINATE, style)
}

internal interface DisabledState<T : StatefulStyleScope<T>> : StatefulStyleScope<T> {
    fun disabled(style: T.() -> Unit) = setState(ComponentState.DISABLED, style)
}

// Component style definitions start from here.

@JvmInline
internal value class CheckboxStyle(private val block: CheckboxStyleScope.() -> Unit) {
    fun CheckboxStyleScope.applyStyle() {
        block()
    }
}

internal class CheckboxStyleScope(override val state: ComponentState = ComponentState.Default) :
    CheckedState<CheckboxStyleScope>,
    IndeterminateState<CheckboxStyleScope>,
    DisabledState<CheckboxStyleScope> {
    var checkmarkColor: Color = Color.Unspecified
        private set

    var borderColor: Color = Color.Unspecified
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
}
