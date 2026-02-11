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

import androidx.compose.ui.inspection.util.toMap
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetRecompositionStateReadResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter.Type
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StackTraceLine
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateRead
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadGroup

// A pattern for matching a line from a stacktrace.
// Can be used to extract className, methodName, fileName and line number.
// example: "at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)"
private val stackTraceLinePattern =
    Regex("\\s*at ([\\w$.<>]+)\\.([\\w$-<>]+)\\(([ $.\\w<>]*):(-?\\d+)\\)")

private val composers = listOf("GapComposer", "LinkComposer", "ComposerImpl")

/** Validate a DSL for a [GetRecompositionStateReadResponse]. */
internal fun validate(
    reads: GetRecompositionStateReadResponse,
    anchorHash: Int,
    block: MultiRecompositionStateReadValidator.() -> Unit = {},
) {
    assertThat(reads.anchorHash).isEqualTo(anchorHash)
    validate(reads.stringsList.toMap(), reads.readList, block)
}

private fun validate(
    strings: Map<Int, String>,
    reads: List<StateReadGroup>,
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

/** Validator of a DSL for [GetRecompositionStateReadResponse]. */
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

/** Validator of a DSL of state reads from a [GetRecompositionStateReadResponse]. */
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

/** Validator of a DSL of state reads from a [GetRecompositionStateReadResponse]. */
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

    /** Specifies the possible values a state variable may have. */
    fun valueOptions(block: ValueOptionValidator.() -> Unit = {}) {
        val validator = ValueOptionValidator(strings, read)
        validator.block()
        validator.end()
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
    @DoNotChangeMayRequireChangesInAndroidStudio
    fun trace(trace: String) {
        var traceIndex = 0
        var skipUntilFound = false
        val lines = trace.lines()
        lines.forEach { line ->
            // Warning: without failing the test:
            when (line.trim()) {
                "" -> return@forEach
                "..." -> {
                    skipUntilFound = true
                    return@forEach
                }
            }
            val match =
                stackTraceLinePattern.matchEntire(line) ?: error("Could not parse: \"$line\"")
            if (traceIndex >= stackTraces.size) {
                error("Only ${stackTraces.size} stack traces found at this level, not found: $line")
            }
            var actual = stackTraces[traceIndex++]
            if (!skipUntilFound) {
                assertIsMatch(line, actual, match)
            } else {
                while (!isMatch(actual, match)) {
                    if (traceIndex >= stackTraces.size) {
                        error("Line not found: $line")
                    }
                    actual = stackTraces[traceIndex++]
                }
                skipUntilFound = false
            }
        }
        if (!skipUntilFound && traceIndex < stackTraces.size) {
            error("Only $traceIndex stack trace lines of ${stackTraces.size} are accounted for.")
        }
        checkInvalidated()
    }

    @DoNotChangeMayRequireChangesInAndroidStudio
    fun folding(unfolded: String) {
        val unfoldedLineIndexes = fold(strings, stackTraces)
        val unfoldedLines = unfoldedLineIndexes.map { stackTraces[it] }
        val expectedLines = unfolded.trimIndent().lines()
        if (unfoldedLines.size != expectedLines.size) {
            assertThat(convertToLines(strings, unfoldedLines)).isEqualTo(expectedLines)
        }
        for (index in expectedLines.indices) {
            val line = expectedLines[index]
            val match =
                stackTraceLinePattern.matchEntire(line) ?: error("Could not parse: \"$line\"")
            assertIsMatch(line, unfoldedLines[index], match)
        }
    }

    private fun isMatch(actual: StackTraceLine, match: MatchResult): Boolean {
        val expectedClass = match.groupValues[1]
        val expectedMethod = match.groupValues[2]
        val expectedFile = match.groupValues[3]
        return isMatch(strings[actual.declaringClass], expectedClass) &&
            isMatch(strings[actual.methodName], expectedMethod) &&
            isMatch(strings[actual.fileName], expectedFile)
    }

    private fun isMatch(actual: String?, expected: String): Boolean {
        if (expected.endsWith("<any>")) {
            return actual!!.startsWith(expected.dropLast(5))
        } else {
            return actual == expected
        }
    }

    private fun assertIsMatch(line: String, actual: StackTraceLine, match: MatchResult) {
        val expectedClass = match.groupValues[1]
        val expectedMethod = match.groupValues[2]
        val expectedFile = match.groupValues[3]
        assertIsMatch(line, strings[actual.declaringClass], expectedClass)
        assertIsMatch(line, strings[actual.methodName], expectedMethod)
        assertIsMatch(line, strings[actual.fileName], expectedFile)
    }

    private fun assertIsMatch(line: String, actual: String?, expected: String) {
        val message = "Found in line: $line"
        when {
            expected.endsWith("<any>") -> {
                val prefix = expected.substring(0, expected.length - 5)
                assertWithMessage(message).that(actual).startsWith(prefix)
            }
            expected.contains("<composer>") -> {
                val composer = composers.find { expected.replace("<composer>", it) == actual }
                if (composer == null) {
                    assertWithMessage(message).that(actual).isEqualTo(expected)
                }
            }
            else -> assertWithMessage(message).that(actual).isEqualTo(expected)
        }
    }

    /** Validate a parameter value that may be one of several possibilities */
    internal class ValueOptionValidator(
        private val strings: Map<Int, String>,
        private val read: StateRead,
    ) {
        private var type: Type = Type.UNSPECIFIED
        private val possibilities = mutableMapOf<Any, ParameterListValidator.() -> Unit>()

        fun value(type: Type, value: Any, block: ParameterListValidator.() -> Unit = {}) {
            if (this.type != Type.UNSPECIFIED) {
                assertThat(type).isEqualTo(this.type)
            }
            this.type = type
            possibilities[value] = block
        }

        fun end() {
            assertThat(possibilities.size).isGreaterThan(0)
            possibilities.forEach { (value, block) ->
                try {
                    val validator = ParameterListValidator(strings, listOf(read.value))
                    validator.parameter("value", type, value, block)
                    return
                } catch (_: Throwable) {
                    // Ignore and try the next possibility...
                }
            }
            // No matches found. Give the error from the first possibility:
            val (value, block) = possibilities.entries.first()
            val validator = ParameterListValidator(strings, listOf(read.value))
            validator.parameter("value", type, value, block)
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
