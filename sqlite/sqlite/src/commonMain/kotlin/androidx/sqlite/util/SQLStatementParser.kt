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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.sqlite.util

import androidx.annotation.RestrictTo

/** Returns the 3-character prefix of the SQL statement or null if the statement is malformed. */
public fun getStatementPrefix(sql: String): String? {
    val index = getStatementPrefixIndex(sql)
    if (index < 0 || index > sql.length) {
        return null
    }
    return sql.substring(index, minOf(index + 3, sql.length))
}

private fun getStatementPrefixIndex(s: String): Int {
    val limit: Int = s.length - 2
    if (limit < 0) return -1
    var i = 0
    while (i < limit) {
        val c = s[i]
        when {
            c <= ' ' -> i++
            c == '-' -> {
                if (s[i + 1] != '-') return i
                i = s.indexOf('\n', i + 2)
                if (i < 0) return -1
                i++
            }
            c == '/' -> {
                if (s[i + 1] != '*') return i
                i++
                do {
                    i = s.indexOf('*', i + 1)
                    if (i < 0) return -1
                } while (i + 1 < limit && s[i + 1] != '/')
                i += 2
            }
            else -> return i
        }
    }
    return -1
}
