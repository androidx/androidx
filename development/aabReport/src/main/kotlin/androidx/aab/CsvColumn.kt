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

package androidx.aab

import androidx.aab.cli.VERBOSE

fun <T> List<CsvColumn<T>>.rowStringForItem(t: T): String =
    filter { VERBOSE || !it.requiresVerbose }.joinToString(", ") { it.calculate(t) }

private fun <T> List<CsvColumn<T>>.toDescriptionRow() =
    filter { VERBOSE || !it.requiresVerbose }
        .joinToString(separator = ",") { "\"${it.description}\"" }

private fun <T> List<CsvColumn<T>>.toLabelRow() =
    filter { VERBOSE || !it.requiresVerbose }.joinToString(separator = ",") { it.columnLabel }

fun <T> List<CsvColumn<T>>.fullHeader(): String {
    return toDescriptionRow() + "\n" + toLabelRow()
}

fun <T, R> List<CsvColumn<T>>.mapAll(transform: (R) -> T) = map { it.map(transform) }

data class CsvColumn<T>(
    val columnLabel: String,
    val description: String,
    val requiresVerbose: Boolean = false,
    val calculate: (T) -> String,
) {
    fun <R> map(transform: (R) -> T) =
        CsvColumn<R>(
            columnLabel = columnLabel,
            description = description,
            requiresVerbose = requiresVerbose,
            calculate = { calculate(transform(it)) },
        )
}
