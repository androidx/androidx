/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.ui.input.InputMode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalInputModeManager
import kotlin.jvm.JvmInline

/**
 * Focusability configures whether a focus target can be focused.
 *
 * @see Always
 * @see SystemDefined
 * @see Never
 */
@JvmInline
value class Focusability private constructor(private val value: Int) {
    companion object {
        /**
         * This focus target can always gain focus. This should be used for components that can be
         * focused regardless of input device / system state, such as text fields.
         */
        val Always = Focusability(1)

        /**
         * Focusability of this focus target will be defined by the system. This should be used for
         * clickable components such as buttons and checkboxes: these components should only gain
         * focus when they are used with certain types of input devices, such as keyboard / d-pad.
         */
        val SystemDefined = Focusability(0)

        /**
         * This focus target can not gain focus. This should be used for disabled components /
         * components that are currently not interactive.
         */
        val Never = Focusability(2)
    }

    override fun toString() =
        when (this) {
            Always -> "Always"
            SystemDefined -> "SystemDefined"
            Never -> "Never"
            // Should not be reached since the constructor is private
            else -> error("Unknown Focusability")
        }

    internal fun canFocus(node: CompositionLocalConsumerModifierNode): Boolean {
        return when (this@Focusability) {
            Always -> true
            SystemDefined ->
                with(node) { currentValueOf(LocalInputModeManager).inputMode != InputMode.Touch }
            Never -> false
            else -> error("Unknown Focusability")
        }
    }
}
