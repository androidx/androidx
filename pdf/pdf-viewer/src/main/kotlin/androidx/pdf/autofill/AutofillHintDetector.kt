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

package androidx.pdf.autofill

/** Interface for detecting Autofill hints from text. */
internal interface AutofillHintDetector {
    /** Returns a list of Autofill hints for the given [text]. */
    fun detectHints(text: String): List<String>
}

/**
 * Default implementation of [AutofillHintDetector] that uses pre-defined regex patterns from
 * [AutofillHintPatterns].
 */
internal class DefaultAutofillHintDetector(
    private val patterns: Map<String, Regex> = AutofillHintPatterns
) : AutofillHintDetector {

    override fun detectHints(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val normalized =
            text
                .lowercase()
                .replace(NON_ALPHANUMERIC, SINGLE_SPACE)
                .replace(WHITESPACE, SINGLE_SPACE)
                .trim()

        if (normalized.isEmpty()) return emptyList()

        return patterns.mapNotNull { (hint, regex) ->
            hint.takeIf { regex.containsMatchIn(normalized) }
        }
    }

    companion object {

        private const val SINGLE_SPACE = " "
        private val NON_ALPHANUMERIC = "[^a-z0-9]".toRegex()
        private val WHITESPACE = "\\s+".toRegex()
    }
}
