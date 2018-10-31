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

/**
 * Controls the visual appearance of the text input control.
 *
 * See also:
 *
 *  * [TextInput.attach]
 */
// @immutable
data class TextInputConfiguration(
/**
 * Creates configuration information for a text input control.
 *
 * All arguments have default values, except [actionLabel]. Only
 * [actionLabel] may be null.
 */

    /** The type of information for which to optimize the text input control. */
    val inputType: TextInputType = TextInputType.text,

    /**
     * Whether to hide the text being edited (e.g., for passwords).
     *
     * Defaults to false.
     */
    val obscureText: Boolean = false,
    /**
     * Whether to enable autocorrection.
     *
     * Defaults to true.
     */
    val autocorrect: Boolean = true,
    /** What text to display in the text input control's action button. */
    val actionLabel: String?,
    /** What kind of action to request for the action button on the IME. */
    val inputAction: TextInputAction = TextInputAction.DONE
) {
//    TODO(Migration/haoyuchang): Need to check if we really need this toJson.
//    /// Returns a representation of this object as a JSON object.
//    fun toJSON(): Map<String, Any?> {
//        return mapOf(
//            "inputType" to inputType.toJSON(),
//            "obscureText" to obscureText,
//            "autocorrect" to autocorrect,
//            "actionLabel" to actionLabel,
//            "inputAction" to inputAction.toString()
//        );
//    }
}