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

import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

/**
 * Truth subject for diagnostic messages
 */
class DiagnosticMessageSubject internal constructor(
    failureMetadata: FailureMetadata,
    private val diagnosticMessage: DiagnosticMessage,
) : Subject<DiagnosticMessageSubject, DiagnosticMessage>(
    failureMetadata, diagnosticMessage
) {
    private val lineContent by lazy {
        val location = diagnosticMessage.location ?: return@lazy null
        location.source?.contents?.lines()?.getOrNull(
            location.line - 1
        )
    }

    /**
     * Checks the location of the diagnostic message against the given [lineNumber].
     */
    fun onLine(lineNumber: Int) = apply {
        if (diagnosticMessage.location?.line != lineNumber) {
            failWithActual(
                simpleFact(
                    "expected line $lineNumber but it was ${diagnosticMessage.location}"
                )
            )
        }
    }

    /**
     * Checks the contents of the line from the original file against the given [content].
     */
    fun onLineContaining(content: String) = apply {
        if (lineContent == null) {
            failWithActual(
                simpleFact("Cannot validate line content due to missing location information")
            )
        }
        if (lineContent?.contains(content) != true) {
            failWithActual(
                simpleFact("expected line content with $content but was $lineContent")
            )
        }
    }

    /**
     * Checks the contents of the source where the diagnostic message was reported on, against
     * the given [source].
     */
    fun onSource(source: Source) = apply {
        if (diagnosticMessage.location?.source != source) {
            failWithActual(
                simpleFact(
                    """
                    Expected diagnostic to be on $source but found it on
                    ${diagnosticMessage.location?.source}
                    """.trimIndent()
                )
            )
        }
    }

    companion object {
        private val FACTORY =
            Factory<DiagnosticMessageSubject, DiagnosticMessage> { metadata, actual ->
                DiagnosticMessageSubject(metadata, actual)
            }

        fun assertThat(
            diagnosticMessage: DiagnosticMessage
        ): DiagnosticMessageSubject {
            return Truth.assertAbout(FACTORY).that(
                diagnosticMessage
            )
        }
    }
}