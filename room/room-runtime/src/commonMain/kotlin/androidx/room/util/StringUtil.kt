/*
 * Copyright (C) 2016 The Android Open Source Project
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

@file:JvmName("StringUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

package androidx.room.util

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

@Suppress("unused")
@JvmField
@Deprecated("No longer used by generated code")
val EMPTY_STRING_ARRAY = arrayOfNulls<String>(0)

/**
 * Returns a new StringBuilder to be used while producing SQL queries.
 *
 * @return A new or recycled StringBuilder
 */
@Deprecated("No longer used by generated code")
fun newStringBuilder(): StringBuilder = StringBuilder()

/**
 * Adds bind variable placeholders (?) to the given string. Each placeholder is separated by a
 * comma.
 *
 * @param builder The StringBuilder for the query
 * @param count Number of placeholders
 */
fun appendPlaceholders(builder: StringBuilder, count: Int) {
    if (count == 0) {
        return
    }
    builder.append("?")
    for (i in 1 until count) {
        builder.append(",?")
    }
}

/**
 * Splits a comma separated list of integers to integer list.
 *
 * If an input is malformed, it is omitted from the result.
 *
 * @param input Comma separated list of integers.
 * @return A List containing the integers or null if the input is null.
 */
fun splitToIntList(input: String?): List<Int>? {
    return input?.split(',')?.mapNotNull { item ->
        try {
            item.toInt()
        } catch (ex: NumberFormatException) {
            null
        }
    }
}

/**
 * Joins the given list of integers into a comma separated list.
 *
 * @param input The list of integers.
 * @return Comma separated string composed of integers in the list. If the list is null, return
 *   value is null.
 */
fun joinIntoString(input: List<Int>?): String? {
    return input?.joinToString(",")
}
