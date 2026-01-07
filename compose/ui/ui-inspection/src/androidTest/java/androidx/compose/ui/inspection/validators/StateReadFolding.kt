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

package androidx.compose.ui.inspection.validators

import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StackTraceLine

private const val LINE_START = "    at "

private const val RECORD_READ_OF = "at androidx.compose.runtime.CompositionImpl.recordReadOf"
private const val SNAPSHOT_READABLE = "at androidx.compose.runtime.snapshots.SnapshotKt.readable"
private const val SNAPSHOT_PACKAGE = "at androidx.compose.runtime.snapshots."
private const val SNAPSHOT_CLASS = "at androidx.compose.runtime.Snapshot"
private const val DYNAMIC_VALUE = "at androidx.compose.runtime.DynamicValueHolder.readValue"
private const val KOTLIN_METHOD = "at kotlin."
private const val RECOMPOSE = "at androidx.compose.runtime.RecomposeScopeImpl.compose"
private const val LAMBDA_INVOKE = "at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke"

private val RECORD_READ_EXCEPTION_PREFIXES =
    listOf(SNAPSHOT_PACKAGE, SNAPSHOT_CLASS, KOTLIN_METHOD, DYNAMIC_VALUE)

fun fold(strings: Map<Int, String>, stackTrace: List<StackTraceLine>): List<Int> {
    val folding = StateReadFolding(convertToLines(strings, stackTrace))
    return folding.fold()
}

/** Mimic the folding that Android Studio performs. */
@DoNotChangeMayRequireChangesInAndroidStudio
private class StateReadFolding(private val lines: List<String>) {
    private val foldRanges = mutableListOf<IntRange>()

    /** Fold the [lines] from a stack trace and return in indices of the unfolded lines */
    fun fold(): List<Int> {
        foldLines()
        return computeUnfoldedLines()
    }

    private fun computeUnfoldedLines(): List<Int> {
        val unfolded = mutableListOf<Int>()
        var lineIndex = 0
        foldRanges.forEach { fold ->
            while (lineIndex < fold.first) {
                unfolded.add(lineIndex++)
            }
            lineIndex = fold.last + 1
        }
        while (lineIndex < lines.size) {
            unfolded.add(lineIndex++)
        }
        return unfolded.dropLast(1) // Drop the empty line at the end
    }

    private fun foldLines() {
        var lineIndex = 0
        while (lineIndex < lines.size) {
            val line = line(lineIndex)
            when {
                line.startsWith(RECORD_READ_OF) -> lineIndex = foldStartOfReadException(lineIndex)
                line.startsWith(RECOMPOSE) || line.startsWith(LAMBDA_INVOKE) ->
                    lineIndex = foldEndOfException(lineIndex)
                else -> lineIndex++
            }
        }
    }

    private fun foldStartOfReadException(start: Int): Int {
        // First skip the `CompositionImpl.recordReadOf` before we enter this function:
        var next = start + 1
        var nextLine = line(next)

        // Skip down to `SnapshotKt.readable` in the stacktrace:
        while (nextLine.isNotEmpty() && !nextLine.startsWith(SNAPSHOT_READABLE)) {
            nextLine = line(++next)
        }

        // Stop now and abandon the fold if we didn't find `SnapshotKt.readable`:
        if (!nextLine.startsWith(SNAPSHOT_READABLE)) {
            return start
        }

        // Skip any snapshot or kotlin runtime frames:
        while (RECORD_READ_EXCEPTION_PREFIXES.any { nextLine.startsWith(it) }) {
            nextLine = line(++next)
        }
        next--

        if (next > start) {
            foldRanges.add(start..next)
        }
        return next
    }

    private fun foldEndOfException(start: Int): Int {
        var next = lines.indexOfFirst(start) { it.isEmpty() }
        if (next < 0) {
            return start
        }
        next--

        if (next > start) {
            foldRanges.add(start..next)
        }
        return next
    }

    private fun line(lineNumber: Int): String =
        if (lines.size > lineNumber) lines[lineNumber].trimStart() else ""
}

fun convertToLines(strings: Map<Int, String>, stackTrace: List<StackTraceLine>): List<String> {
    val lines = mutableListOf<String>()
    stackTrace.forEach { trace ->
        val declaringClass = strings[trace.declaringClass]
        val methodName = strings[trace.methodName]
        val fileName = strings[trace.fileName]?.takeIf { it.isNotEmpty() } ?: "Unknown Source"
        lines.add("$LINE_START${declaringClass}.${methodName}($fileName:${trace.lineNumber})")
    }
    lines.add("")
    return lines
}

private fun <T> List<T>.indexOfFirst(startFrom: Int, predicate: (T) -> Boolean): Int {
    for (index in startFrom..lastIndex) {
        if (predicate(this[index])) return index
    }
    return -1
}
