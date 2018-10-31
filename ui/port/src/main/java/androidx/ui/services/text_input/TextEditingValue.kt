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

import androidx.ui.services.text_editing.TextSelection
import androidx.ui.services.text_editing.TextRange

/** The current text, selection, and composing state for editing a run of text.
 *
 * @constructor Creates information for editing a run of text.
 *
 * The selection and composing range must be within the text.
 *
 * The [text], [selection], and [composing] arguments must not be null but
 * each have default values.
 */
data class TextEditingValue(
    /** The current text being edited. */
    val text: String = "",

    /** The range of text that is currently selected. */
    val selection: TextSelection = TextSelection.collapsed(-1),

    /** The range of text that is still being composed. */
    val composing: TextRange = TextRange.empty
) {

    companion object {
//        TODO(Migration/haoyuchang): Handle serialization later.
//        /// Creates an instance of this class from a JSON object.
//        fun fromJSON(encoded: Map<String, Any>): TextEditingValue {
//            return TextEditingValue(
//                encoded["text"] as? String ?: "",
//                TextSelection(
//                    encoded["selectionBase"] as? Int ?: -1,
//                    encoded["selectionExtent"] as? Int ?: -1,
//                    _toTextAffinity(
//                            encoded["selectionAffinity"] as? String) ?: TextAffinity.downstream,
//                    encoded["selectionIsDirectional"] as? Boolean ?: false
//                ),
//                TextRange(
//                    encoded["composingBase"] as? Int ?:-1,
//                    encoded["composingExtent"] as? Int ?: -1
//                )
//            )
//        }

//        fun _toTextAffinity(affinity: String?): TextAffinity? {
//            when (affinity) {
//                "TextAffinity.downstream" -> return TextAffinity.downstream;
//                "TextAffinity.upstream" -> return TextAffinity.upstream;
//            }
//            return null;
//        }
        /** A value that corresponds to the empty string with no selection and no composing range. */
        val empty: TextEditingValue = TextEditingValue()
    }
//    TODO(Migration/haoyuchang): Remove those unnecessary method in the end.
//    /// Returns a representation of this object as a JSON object.
//    fun toJSON(): Map<String, Any> {
//        return mapOf(
//            "text" to text,
//            "selectionBase" to selection.baseOffset,
//            "selectionExtent" to selection.extentOffset,
//            "selectionAffinity" to selection.affinity.toString(),
//            "selectionIsDirectional" to selection.isDirectional,
//            "composingBase" to composing.start,
//            "composingExtent" to composing.end
//        )
//    }

//    /// Creates a copy of this value but with the given fields replaced with the new values.
//    fun copyWith(
//        text: String? = null,
//        selection: TextSelection? = null,
//        composing: TextRange? = null
//    ): TextEditingValue {
//        return TextEditingValue(
//            text ?: this.text,
//            selection ?: this.selection,
//        composing ?: this.composing
//        )
//    }
    override fun toString(): String =
            "${runtimeType()}(\u2524$text\u251C, selection: $selection, $composing)"
//
//    override fun equals(other: Any?): Boolean {
//        if (this == other)
//            return true;
//        if (other !is TextEditingValue)
//            return false;
//        return other.text == text
//                && other.selection == selection
//                && other.composing == composing;
//    }
//
//    override fun hashCode(): Int =
//        text.hashCode() + selection.hashCode() * 31 + composing.hashCode() * 31 * 31
}