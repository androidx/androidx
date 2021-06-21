/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

/**
 * Represents an error in configuration of a benchmark.
 */
internal data class ConfigurationError(
    /**
     * All-caps, publicly visible ID for the error.
     *
     * Used for suppression via instrumentation arguments.
     */
    val id: String,

    /**
     * One-line summary of the problem.
     */
    val summary: String,

    /**
     * Multi-line, preformatted detailed description of the problem.
     */
    val message: String
) {
    init {
        validateParams(id, summary)
    }

    companion object {
        internal fun validateParams(
            id: String,
            summary: String
        ) {
            require(!id.contains("[a-z]".toRegex())) {
                "IDs must be ALL-CAPs by convention (id=$id)"
            }
            require(!id.contains("_")) {
                "Use hyphen instead of underscore for consistency (id=$id)"
            }
            require(!summary.contains("\n")) {
                "Summary must be one line"
            }
        }
    }

    /**
     * Representation of suppressed errors during a running benchmark.
     */
    class SuppressionState(
        /**
         * Prefix for output data to mark as potentially invalid.
         */
        val prefix: String,

        /**
         * Warning message to present to the user.
         */
        val warningMessage: String
    )
}

internal fun conditionalError(
    hasError: Boolean,
    id: String,
    summary: String,
    message: String
): ConfigurationError? {
    // validation done here *and* in constructor to ensure it happens even when error doesn't fire
    ConfigurationError.validateParams(id, summary)
    return if (hasError) {
        ConfigurationError(id, summary, message)
    } else null
}

internal fun List<ConfigurationError>.prettyPrint(prefix: String): String {
    return joinToString("\n") {
        prefix + it.summary + "\n" + it.message.prependIndent() + "\n"
    }
}

/**
 * Throw an AssertionError if the list contains an unsuppressed error, and return either a
 * SuppressionState if errors are suppressed, or null otherwise.
 */
internal fun List<ConfigurationError>.checkAndGetSuppressionState(
    suppressedErrorIds: Set<String>,
): ConfigurationError.SuppressionState? {
    val (suppressed, unsuppressed) = partition {
        suppressedErrorIds.contains(it.id)
    }

    val prefix = suppressed.joinToString("_") { it.id } + "_"

    val unsuppressedString = unsuppressed.joinToString(" ") { it.id }
    val suppressedString = suppressed.joinToString(" ") { it.id }
    val howToSuppressString = this.joinToString(",") { it.id }

    if (unsuppressed.isNotEmpty()) {
        throw AssertionError(
            """
                |ERRORS (not suppressed): $unsuppressedString
                |WARNINGS (suppressed): $suppressedString
                |
                |${unsuppressed.prettyPrint("ERROR: ")}
                |While you can suppress these errors (turning them into warnings)
                |PLEASE NOTE THAT EACH SUPPRESSED ERROR COMPROMISES ACCURACY
                |
                |// Sample suppression, in a benchmark module's build.gradle:
                |android {
                |    defaultConfig {
                |        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "$howToSuppressString"
                |    }
                |}
            """.trimMargin()
        )
    }

    if (suppressed.isEmpty()) {
        return null
    }

    return ConfigurationError.SuppressionState(prefix, suppressed.prettyPrint("WARNING: "))
}
