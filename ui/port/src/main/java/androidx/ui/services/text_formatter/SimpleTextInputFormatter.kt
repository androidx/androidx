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
 *  Wiring for [TextInputFormatter.withFunction].
 */
class SimpleTextInputFormatter(formatFunction: TextInputFormatFunction) : TextInputFormatter() {

    private val formatFunction = formatFunction

    override fun formatEditUpdate(
        oldValue: TextEditingValue,
        newValue: TextEditingValue
    ): TextEditingValue {
        return formatFunction(oldValue, newValue)
    }
}