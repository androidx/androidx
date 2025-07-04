/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.core

/** Transforms a camel case string to screaming snake case string. */
fun String.fromCamelCaseToScreamingSnakeCase(): String {
    fun isFullyCapitalized(input: String): Boolean {
        val uppercaseInput = input.uppercase()
        return input == uppercaseInput
    }
    if (isFullyCapitalized(this)) {
        return this
    }

    val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
    return camelRegex.replace(this) { "_${it.value}" }.uppercase()
}

/** Transforms any string to a pascal case string, removing all separators (".", "/", "_", "-"). */
fun String.toPascalCase(): String {
    if (this.isEmpty()) return ""

    val separatorsRegex = Regex("[._/-]+|\\s+") // Matches separators and spaces
    val words = split(separatorsRegex).filter { it.isNotEmpty() }

    return words.joinToString("") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
}
