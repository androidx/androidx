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

package androidx.room.ext

import java.util.Locale

private fun String.toCamelCase(): String {
    val split = this.split("_")
    if (split.isEmpty()) return ""
    if (split.size == 1) return split[0].capitalize(Locale.US)
    return split.joinToCamelCase()
}

private fun String.toCamelCaseAsVar(): String {
    val split = this.split("_")
    if (split.isEmpty()) return ""
    if (split.size == 1) return split[0]
    return split.joinToCamelCaseAsVar()
}

private fun List<String>.joinToCamelCase(): String = when (size) {
    0 -> throw IllegalArgumentException("invalid section size, cannot be zero")
    1 -> this[0].toCamelCase()
    else -> this.joinToString("") { it.toCamelCase() }
}

private fun List<String>.joinToCamelCaseAsVar(): String = when (size) {
    0 -> throw IllegalArgumentException("invalid section size, cannot be zero")
    1 -> this[0].toCamelCaseAsVar()
    else -> get(0).toCamelCaseAsVar() + drop(1).joinToCamelCase()
}

private val javaCharRegex = "[^a-zA-Z0-9]".toRegex()
fun String.stripNonJava(): String {
    return this.split(javaCharRegex)
        .map(String::trim)
        .joinToCamelCaseAsVar()
}

// TODO: Replace this with the function from the Kotlin stdlib once the API becomes stable
fun String.capitalize(locale: Locale): String = if (isNotEmpty() && this[0].isLowerCase()) {
    substring(0, 1).uppercase(locale) + substring(1)
} else {
    this
}

// TODO: Replace this with the function from the Kotlin stdlib once the API becomes stable
fun String.decapitalize(locale: Locale): String = if (isNotEmpty() && this[0].isUpperCase()) {
    substring(0, 1).lowercase(locale) + substring(1)
} else {
    this
}
