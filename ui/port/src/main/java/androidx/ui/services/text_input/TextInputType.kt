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

package androidx.ui.services.text_input

import androidx.ui.runtimeType

/**
 * The type of information for which to optimize the text input control.
 *
 * On Android, behavior may vary across device and keyboard provider.
 *
 * This class stays as close to [Enum] interface as possible, and allows
 * for additional flags for some input types. For example, numeric input
 * can specify whether it supports decimal numbers and/or signed numbers.
 */
data class TextInputType private constructor(
    /** Enum value index, corresponds to one of the [values]. */
    val index: Int,
    /**
     * The number is signed, allowing a positive or negative sign at the start.
     *
     * This flag is only used for the [number] input type, otherwise `null`.
     * Use `const TextInputType.numberWithOptions(signed: true)` to set this.
     */
    val signed: Boolean? = null,
    /**
     * The number is decimal, allowing a decimal point to provide fractional.
     *
     * This flag is only used for the [number] input type, otherwise `null`.
     * Use `const TextInputType.numberWithOptions(decimal: true)` to set this.
     */
    val decimal: Boolean? = null
) {

    companion object {
        /**
         * Optimize for textual information.
         *
         * Requests a numeric keyboard with additional settings.
         * The [signed] and [decimal] parameters are optional.
         */
        fun numberWithOptions(
            signed: Boolean = false,
            decimal: Boolean = false
        ): TextInputType {
            return TextInputType(
                index = 2,
                signed = signed,
                decimal = decimal)
        }

        /**
         * Optimize for textual information.
         *
         * Requests the default platform keyboard.
         */
        val text: TextInputType = TextInputType(0)

        /**
         * Optimize for multi-line textual information.
         *
         * Requests the default platform keyboard, but accepts newlines when the
         * enter key is pressed. This is the input type used for all multi-line text
         * fields.
         */
        val multiline: TextInputType = TextInputType(1)

        /**
         * Optimize for numerical information.
         *
         * Requests a default keyboard with ready access to the number keys.
         * Additional options, such as decimal point and/or positive/negative
         * signs, can be requested using [TextInputType.numberWithOptions].
         */
        val number: TextInputType = TextInputType.numberWithOptions()

        /**
         * Optimize for telephone numbers.
         *
         * Requests a keyboard with ready access to the number keys, "*", and "#".
         */
        val phone: TextInputType = TextInputType(3)

        /**
         * Optimize for date and time information.
         *
         * On iOS, requests the default keyboard.
         *
         * On Android, requests a keyboard with ready access to the number keys,
         * ":", and "-".
         */
        val datetime: TextInputType = TextInputType(4)

        /**
         * Optimize for email addresses.
         *
         * Requests a keyboard with ready access to the "@" and "." keys.
         */
        val emailAddress: TextInputType = TextInputType(5)

        /**
         * Optimize for URLs.
         *
         * Requests a keyboard with ready access to the "/" and "." keys.
         */
        val url: TextInputType = TextInputType(6)

        /** All possible enum values. */
        val values: List<TextInputType> =
            listOf(text, multiline, number, phone, datetime, emailAddress, url)

        /** Corresponding string name for each the [values]. */
        val _names: List<String> =
            listOf("text", "multiline", "number", "phone", "datetime", "emailAddress", "url")
    }

    /** Enum value name, this is what enum.toString() would normally return. */
    val _name: String
        get() = "TextInputType.${_names[index]}"

//  TODO(Migration/haoyuchang):  Remove unused functions in the end.
//    /// Returns a representation of this object as a JSON object.
//    fun toJSON(): Map<String, Any?> =
//        mapOf("name" to _name, "signed" to signed, "decimal" to decimal)

    override fun toString(): String {
        return "${runtimeType()}(name: $_name, signed: $signed, decimal: $decimal)"
    }

//    override fun equals(other: Any?): Boolean {
//        if (other === this) {
//            return true
//        }
//        if (other !is TextInputType) {
//            return false;
//        }
//
//        return other.index == index
//                && other.signed == signed
//                && other.decimal == decimal;
//    }
//
//    override fun hashCode(): Int {
//        var hashSigned = 0
//        if (signed != null) {
//            hashSigned = 1 + if (signed) 1 else 0
//        }
//        var hashDecimal = 0
//        if (decimal != null) {
//            hashDecimal = 1 + if (decimal) 1 else 0
//        }
//        return hashSigned + hashDecimal * 31 + index * 31 * 31;
//    }
}