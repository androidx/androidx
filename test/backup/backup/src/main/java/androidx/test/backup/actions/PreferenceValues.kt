/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.actions

import android.util.Base64
import androidx.test.backup.BackupActionInputKeys.VALUE_TYPE
import androidx.test.backup.BackupActionValues.VALUE_TYPE_BOOLEAN
import androidx.test.backup.BackupActionValues.VALUE_TYPE_BYTES
import androidx.test.backup.BackupActionValues.VALUE_TYPE_DOUBLE
import androidx.test.backup.BackupActionValues.VALUE_TYPE_FLOAT
import androidx.test.backup.BackupActionValues.VALUE_TYPE_INT
import androidx.test.backup.BackupActionValues.VALUE_TYPE_LONG
import androidx.test.backup.BackupActionValues.VALUE_TYPE_STRING
import androidx.test.backup.BackupActionValues.VALUE_TYPE_STRING_SET
import org.json.JSONArray
import org.json.JSONException

/**
 * Parses a string into a typed preference value according to [valueType].
 *
 * Both actions in this package share this function so that the format cannot drift between the
 * populate and the verify phase.
 *
 * Boolean parsing is strict: only `"true"` and `"false"` (case-insensitively) are accepted. A
 * string set is a JSON array of strings, and parses to a `Set<String>`.
 *
 * @throws IllegalArgumentException if [valueType] is unsupported or [value] cannot be parsed
 */
internal fun parsePreferenceValue(value: String, valueType: String): Any =
    when (valueType.uppercase()) {
        VALUE_TYPE_INT ->
            value.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid integer preference value: '$value'")
        VALUE_TYPE_LONG ->
            value.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid long preference value: '$value'")
        VALUE_TYPE_FLOAT ->
            value.toFloatOrNull()
                ?: throw IllegalArgumentException("Invalid float preference value: '$value'")
        VALUE_TYPE_BOOLEAN ->
            when {
                value.equals("true", ignoreCase = true) -> true
                value.equals("false", ignoreCase = true) -> false
                else ->
                    throw IllegalArgumentException(
                        "Invalid boolean preference value '$value': expected 'true' or 'false'"
                    )
            }
        VALUE_TYPE_STRING_SET -> parseStringSet(value)
        VALUE_TYPE_STRING -> value
        else -> throw IllegalArgumentException("Unsupported $VALUE_TYPE: $valueType")
    }

/**
 * Parses a string into a typed Preferences DataStore value according to [valueType].
 *
 * Supports every [parsePreferenceValue] type, plus [VALUE_TYPE_DOUBLE] and Base64-encoded
 * [VALUE_TYPE_BYTES].
 *
 * @throws IllegalArgumentException if [valueType] is unsupported or [value] cannot be parsed
 */
internal fun parseDataStoreValue(value: String, valueType: String): Any =
    when (valueType.uppercase()) {
        VALUE_TYPE_DOUBLE ->
            value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid double preference value: '$value'")
        VALUE_TYPE_BYTES ->
            try {
                Base64.decode(value, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid bytes preference value ${shown(value)}: expected Base64",
                    e,
                )
            }
        else -> parsePreferenceValue(value, valueType)
    }

/**
 * Parses [value], a JSON array of strings, into the set of those strings.
 *
 * @throws IllegalArgumentException if [value] is not a JSON array, or has an element that is not a
 *   string
 */
private fun parseStringSet(value: String): Set<String> {
    val array =
        try {
            JSONArray(value)
        } catch (e: JSONException) {
            throw IllegalArgumentException(
                "Invalid string set preference value ${shown(value)}: expected a JSON array of " +
                    "strings",
                e,
            )
        }
    return (0 until array.length()).mapTo(LinkedHashSet()) { index ->
        array.get(index) as? String
            ?: throw IllegalArgumentException(
                "Invalid string set preference value ${shown(value)}: element $index is not a " +
                    "string"
            )
    }
}
