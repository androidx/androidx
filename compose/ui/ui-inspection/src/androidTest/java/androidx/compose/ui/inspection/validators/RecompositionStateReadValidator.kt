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

import android.util.Log
import androidx.compose.ui.inspection.LOG_TAG
import androidx.compose.ui.inspection.util.toMap
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetRecompositionStateReadResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter.Type
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.RecompositionStateRead
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.RecompositionStateReadEvent
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateRead

// Add a fudge value for line comparisons to avoid frequent test failures from
// changed code.
// For failures with this validator: disable the test and assign a bug to jlauridsen@
private const val LINE_FUDGE_VALUE = 50

// A pattern for matching a line from a stacktrace.
// Can be used to extract className, methodName, fileName and line number.
// example: "at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)"
private val stackTraceLinePattern =
    Regex("\\s*at ([\\w$.<>]+)\\.([\\w$-<>]+)\\(([ $.\\w<>]*):(-?\\d+)\\)")

/** Validate a DSL for a [GetRecompositionStateReadResponse]. */
internal fun validate(
    reads: GetRecompositionStateReadResponse,
    anchorHash: Int,
    block: MultiRecompositionStateReadValidator.() -> Unit = {},
) {
    assertThat(reads.anchorHash).isEqualTo(anchorHash)
    validate(reads.stringsList.toMap(), listOf(reads.read), block)
}

/** Validate a DSL for a [RecompositionStateReadEvent]. */
internal fun validate(
    event: RecompositionStateReadEvent,
    anchorHash: Int,
    block: MultiRecompositionStateReadValidator.() -> Unit = {},
) {
    assertThat(event.anchorHash).isEqualTo(anchorHash)
    validate(event.stringsList.toMap(), event.readList, block)
}

private fun validate(
    strings: Map<Int, String>,
    reads: List<RecompositionStateRead>,
    block: MultiRecompositionStateReadValidator.() -> Unit = {},
) {
    val map = reads.associate { it.recompositionNumber to it.readList }
    try {
        val validator = MultiRecompositionStateReadValidator(strings, map)
        validator.block()
        validator.end()
    } catch (ex: Throwable) {
        val output = StringBuilder()
        output.appendLine()
        output.appendLine("validate {")
        val validator = MultiRecompositionStateReadValidator(strings, map)
        validator.dump(output, 1)
        output.appendLine("}")
        System.err.println(output)
        throw ex
    }
}

/** Validator of a DSL for [GetRecompositionStateReadResponse] and [RecompositionStateReadEvent]. */
internal class MultiRecompositionStateReadValidator(
    private val strings: Map<Int, String>,
    private val reads: Map<Int, List<StateRead>>,
) {
    private val recompositionsChecked = mutableSetOf<Int>()

    /**
     * Specifies an expected [recomposition] number among the actual state reads and a [block] of
     * expected state reads.
     */
    fun recomposition(recomposition: Int, block: RecompositionStateReadValidator.() -> Unit) {
        val actual = reads.entries.joinToString { it.key.toString() }
        assertWithMessage("No reads for recomposition: $recomposition found, actual: [$actual]")
            .that(reads.containsKey(recomposition))
            .isTrue()
        assertWithMessage("Recomposition: $recomposition already checked")
            .that(recompositionsChecked)
            .doesNotContain(recomposition)
        recompositionsChecked.add(recomposition)
        val read = reads[recomposition] ?: emptyList()
        val validator = RecompositionStateReadValidator(strings, read)
        validator.block()
        validator.end()
    }

    /**
     * Called from [validate] to check that all actual recompositions were included in the expected
     * recompositions in the DSL.
     */
    fun end() {
        val checked = recompositionsChecked.size
        assertWithMessage("Only $checked out of ${reads.size} recompositions are accounted for")
            .that(reads.size)
            .isEqualTo(recompositionsChecked.size)
    }

    /** Formats the actual data (to be used in error messages). */
    fun dump(output: StringBuilder, indent: Int) {
        val recompositions = reads.keys.toSortedSet()
        val spaces = "    ".repeat(indent)
        for (recomposition in recompositions) {
            output.appendLine("${spaces}recomposition($recomposition) {")
            val read = reads[recomposition] ?: emptyList()
            val validator = RecompositionStateReadValidator(strings, read)
            validator.dump(output, indent + 1)
            output.appendLine("${spaces}}")
        }
    }
}

/**
 * Validator of a DSL of state reads from either a [GetRecompositionStateReadResponse] or a
 * [RecompositionStateReadEvent].
 */
internal class RecompositionStateReadValidator(
    private val strings: Map<Int, String>,
    private val reads: List<StateRead>,
) {
    private var readIndex = 0

    /** Specifies an expected state read with a [block] for the expected value and state trace. */
    fun read(block: StateReadValidator.() -> Unit = {}) {
        if (readIndex >= reads.size) {
            error("There are only ${reads.size} state reads for this recomposition")
        }
        val read = reads[readIndex++]
        val validator = StateReadValidator(strings, read)
        validator.block()
    }

    /**
     * Called from [MultiRecompositionStateReadValidator] to make sure all actual state reads are
     * accounted for.
     */
    fun end() {
        assertWithMessage("Only $readIndex out of ${reads.size} state reads are accounted for")
            .that(readIndex)
            .isEqualTo(reads.size)
    }

    /** Formats the actual data (to be used in error messages). */
    fun dump(output: StringBuilder, indent: Int) {
        val spaces = "    ".repeat(indent)
        for (read in reads) {
            output.appendLine("${spaces}read {")
            val validator = StateReadValidator(strings, read)
            validator.dump(output, indent + 1)
            output.appendLine("$spaces}")
        }
    }
}

/**
 * Validator of a DSL of a single state read from either a [GetRecompositionStateReadResponse] or a
 * [RecompositionStateReadEvent].
 */
internal class StateReadValidator(
    private val strings: Map<Int, String>,
    private val read: StateRead,
) {
    private val stackTraces = read.stackTraceLineList
    private var invalidatedChecked = false

    /** Specifies an expected value from a state variable. */
    fun value(type: Type, value: Any, block: ParameterListValidator.() -> Unit = {}) {
        val validator = ParameterListValidator(strings, listOf(read.value))
        validator.parameter("value", type, value, block)
    }

    /**
     * Specifies that the state variable is expected to have been invalidated before recomposition.
     * If not specified it is expected that the state variable was NOT invalidated.
     */
    fun invalidated(invalidated: Boolean) {
        assertThat(read.invalidated).isEqualTo(invalidated)
        invalidatedChecked = true
    }

    private fun checkInvalidated() {
        if (!invalidatedChecked) {
            assertThat(read.invalidated).isFalse()
        }
    }

    /**
     * Specifies the expected stack trace for when the state read was observed in the
     * RecompositionHandler.
     */
    fun trace(trace: String) {
        var traceIndex = 0
        val lines = trace.lines()
        lines.forEachIndexed { index, line ->
            // Warning: without failing the test:
            when (line.trim()) {
                "" -> return@forEachIndexed
                "..." -> return
            }
            val match =
                stackTraceLinePattern.matchEntire(line) ?: error("Could not parse: \"$line\"")
            if (traceIndex >= stackTraces.size) {
                error("Only ${stackTraces.size} stack traces found at this level")
            }
            val expectedClass = match.groupValues[1]
            val expectedMethod = match.groupValues[2]
            val expectedFile = match.groupValues[3]
            val expectedLine = match.groupValues[4].toInt()
            val actual = stackTraces[traceIndex++]
            assertIsMatch(line, strings[actual.declaringClass], expectedClass)
            assertIsMatch(line, strings[actual.methodName], expectedMethod)
            assertIsMatch(line, strings[actual.fileName], expectedFile)
            assertThat(actual.lineNumber)
                .isIn(expectedLine - LINE_FUDGE_VALUE..expectedLine + LINE_FUDGE_VALUE)
            if (actual.lineNumber != expectedLine) {
                // Warning: without failing the test:
                Log.w(LOG_TAG, Exception("Expected: $expectedLine was: ${actual.lineNumber}"))
            }
        }
        if (traceIndex < stackTraces.size) {
            error("Only $traceIndex stack trace lines of ${stackTraces.size} are accounted for.")
        }
        checkInvalidated()
    }

    private fun assertIsMatch(line: String, actual: String?, expected: String) {
        val message = "Found in line: $line"
        if (expected.endsWith("<any>")) {
            val prefix = expected.substring(0, expected.length - 5)
            assertWithMessage(message).that(actual).startsWith(prefix)
        } else {
            assertWithMessage(message).that(actual).isEqualTo(expected)
        }
    }

    /** Formats the actual data (to be used in error messages). */
    fun dump(output: StringBuilder, indent: Int) {
        val validator = ParameterListValidator(strings, listOf(read.value))
        validator.dump(output, indent, "value", showName = false)
        val spaces = "    ".repeat(indent)
        if (read.invalidated) {
            output.appendLine("${spaces}invalidated(${read.invalidated})")
        }
        output.appendLine("${spaces}trace(\"\"\"")
        for (trace in read.stackTraceLineList) {
            output.append(spaces)
            output.append("  at ")
            output.append(formatExpectedString(strings[trace.declaringClass]!!))
            output.append(".")
            output.append(formatExpectedString(strings[trace.methodName]!!))
            output.append("(")
            output.append(formatExpectedString(strings[trace.fileName]!!))
            output.append(":")
            output.append(trace.lineNumber)
            output.appendLine(")")
        }
        output.appendLine("$spaces\"\"\".trimIndent())")
    }

    private fun formatExpectedString(value: String): String {
        val dollarIndex = value.indexOf('$')
        val hyphenIndex = value.indexOf('-')
        val index =
            if (dollarIndex >= 0 && hyphenIndex >= 0) minOf(dollarIndex, hyphenIndex)
            else maxOf(dollarIndex, hyphenIndex)

        return if (index < 0) value else value.substring(0, index) + "<any>"
    }
}
