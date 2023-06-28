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

package androidx.room.compiler.processing.util

import androidx.kruth.FailureMetadata
import androidx.kruth.Subject
import androidx.kruth.assertAbout

/**
 * Truth subject for diagnostic messages
 */
class DiagnosticMessagesSubject internal constructor(
    failureMetadata: FailureMetadata,
    private val diagnosticMessages: List<DiagnosticMessage>,
) : Subject<List<DiagnosticMessage>>(
    diagnosticMessages, failureMetadata
) {

    private val lineContents by lazy {
        diagnosticMessages.mapNotNull {
            it.location?.let { location ->
                location.source?.contents?.lines()?.getOrNull(
                    location.line - 1
                )
            }
        }
    }

    private val locations by lazy {
        diagnosticMessages.mapNotNull { it.location }
    }

    /**
     * Checks the location of the diagnostic message against the given [lineNumber].
     *
     * Note that if there are multiple messages, any match will be sufficient.
     */
    fun onLine(lineNumber: Int) = apply {
        if (locations.none { it.line == lineNumber }) {
            failWithActual(
                "expected line $lineNumber but it was " +
                    locations.joinToString(",")
            )
        }
    }

    /**
     * Checks the number of messages in the subject.
     */
    fun hasCount(expected: Int) = apply {
        if (diagnosticMessages.size != expected) {
            failWithActual(
                "expected $expected messages, found ${diagnosticMessages.size}"
            )
        }
    }

    /**
     * Checks the contents of the line from the original file against the given [content].
     */
    fun onLineContaining(content: String) = apply {
        if (lineContents.isEmpty()) {
            failWithActual(
                "Cannot validate line content due to missing location information"
            )
        }
        if (lineContents.none {
            it.contains(content)
        }
        ) {
            failWithActual(
                "expected line content with $content but was " +
                    lineContents.joinToString("\n")
            )
        }
    }

    /**
     * Checks the contents of the source where the diagnostic message was reported on, against
     * the given [source].
     */
    fun onSource(source: Source) = apply {
        if (locations.none { it.source == source }) {
            failWithActual(
                """
                    Expected diagnostic to be on $source but found it on
                    ${locations.joinToString(",")}
                """.trimIndent()
            )
        }
    }

    companion object {
        fun assertThat(
            diagnosticMessages: List<DiagnosticMessage>
        ): DiagnosticMessagesSubject =
            assertAbout(::DiagnosticMessagesSubject).that(diagnosticMessages)
    }
}