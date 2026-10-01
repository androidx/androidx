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

import androidx.test.backup.BackupActionInputKeys.VALUE_TYPE
import androidx.test.backup.BackupActionValues.VALUE_TYPE_BOOLEAN
import androidx.test.backup.BackupActionValues.VALUE_TYPE_FLOAT
import androidx.test.backup.BackupActionValues.VALUE_TYPE_INT
import androidx.test.backup.BackupActionValues.VALUE_TYPE_LONG
import androidx.test.backup.BackupActionValues.VALUE_TYPE_STRING

/**
 * Parses a string into a typed preference value according to [valueType].
 *
 * Both actions in this package share this function so that the format cannot drift between the
 * populate and the verify phase.
 *
 * Boolean parsing is strict: only `"true"` and `"false"` (case-insensitively) are accepted.
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
        VALUE_TYPE_STRING -> value
        else -> throw IllegalArgumentException("Unsupported $VALUE_TYPE: $valueType")
    }
