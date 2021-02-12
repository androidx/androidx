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

/**
 * Throw if the list is non-empty, with a formatted message.
 *
 * Format the error to look like the following:
 *
 * java.lang.AssertionError: ERRORS: FOO, BAR
 * ERROR: Running with Foo
 *     Foo line 1...
 *     Foo line 2.
 *
 * ERROR: Running with Bar
 *     Foo line 1...
 *     Foo line 2.
 */
internal fun List<ConfigurationError>.throwErrorIfNotEmpty() {
    if (isNotEmpty()) {
        val errorIdList = "ERRORS: " + this.joinToString(", ") { it.id }
        val errorFullMessage = this.joinToString("\n") {
            "ERROR: " + it.summary + "\n" + it.message.prependIndent() + "\n"
        }
        throw AssertionError(errorIdList + "\n" + errorFullMessage)
    }
}