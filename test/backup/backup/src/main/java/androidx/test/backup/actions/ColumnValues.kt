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

import androidx.test.backup.BackupActionInputKeys
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Decodes the [BackupActionInputKeys.VALUES] wire format into ordered column name/value pairs.
 *
 * The format is `application/x-www-form-urlencoded`: pairs separated by `&`, each pair
 * `name=value`, with both halves percent-encoded in UTF-8. Decoding both halves is what allows a
 * column value to contain `&`, `=`, `%` or `+`.
 *
 * Both actions in this package share this function so that the format cannot drift between the
 * populate and the verify phase.
 *
 * @throws IllegalArgumentException if [encoded] is empty, if a segment is not a `name=value` pair,
 *   if a column name is empty, or if a half is not valid percent-encoded UTF-8
 */
internal fun decodeColumnValues(encoded: String): List<Pair<String, String>> {
    require(encoded.isNotEmpty()) { "'${BackupActionInputKeys.VALUES}' must not be empty." }
    return encoded.split("&").map { segment ->
        val separator = segment.indexOf('=')
        require(separator >= 0) {
            "'${BackupActionInputKeys.VALUES}' segment '$segment' is not a 'name=value' pair."
        }
        val name = decodeComponent(segment.substring(0, separator), segment)
        require(name.isNotEmpty()) {
            "'${BackupActionInputKeys.VALUES}' segment '$segment' has an empty column name."
        }
        name to decodeComponent(segment.substring(separator + 1), segment)
    }
}

private fun decodeComponent(component: String, segment: String): String =
    try {
        URLDecoder.decode(component, StandardCharsets.UTF_8.name())
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException(
            "'${BackupActionInputKeys.VALUES}' segment '$segment' is not valid percent-encoded " +
                "UTF-8: ${e.message}",
            e,
        )
    }
