/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.services.text_formatter

import androidx.ui.services.text_input.TextEditingValue

/**
 * A [TextInputFormatter] can be optionally injected into an [EditableText] to provide as-you-type
 * validation and formatting of the text being edited.
 *
 * Text modification should only be applied when text is being committed by the IME and not on text
 * under composition (i.e., only when [TextEditingValue.composing] is collapsed).
 *
 * Concrete implementations [BlacklistingTextInputFormatter], which removes blacklisted characters
 * upon edit commit, and [WhitelistingTextInputFormatter], which only allows entries of whitelisted
 * characters, are provided.
 *
 * To create custom formatters, extend the [TextInputFormatter] class and implement the
 * [formatEditUpdate] method.
 *
 * See also:
 * [EditableText] on which the formatting apply.
 * [BlacklistingTextInputFormatter], a provided formatter for blacklisting characters.
 * [WhitelistingTextInputFormatter], a provided formatter for whitelisting characters.
 */
open abstract class TextInputFormatter {
    /* Called when text is being typed or cut/copy/pasted in the [EditableText].
     *
     * You can override the resulting text based on the previous text value and the incoming new
     * text value.
     *
     * When formatters are chained, `oldValue` reflects the initial value of [TextEditingValue] at
     * the beginning of the chain.
     */
    abstract fun formatEditUpdate(
        oldValue: TextEditingValue,
        newValue: TextEditingValue
    ): TextEditingValue

    companion object {
        /**
         * A shorthand to creating a custom [TextInputFormatter] which formats incoming text input
         * changes with the given function.
         */
        fun withFunction(
            formatFunction: TextInputFormatFunction
        ): TextInputFormatter {
            return SimpleTextInputFormatter(formatFunction)
        }
    }
}
