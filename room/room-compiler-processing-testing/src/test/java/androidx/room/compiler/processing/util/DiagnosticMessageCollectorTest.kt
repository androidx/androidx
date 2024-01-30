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

import androidx.room.compiler.processing.util.compiler.DiagnosticsMessageCollector
import androidx.room.compiler.processing.util.compiler.steps.RawDiagnosticMessage
import androidx.room.compiler.processing.util.compiler.steps.RawDiagnosticMessage.Location
import com.google.common.truth.Truth.assertThat
import javax.tools.Diagnostic
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class DiagnosticMessageCollectorTest(
    private val params: TestParams
) {
    @Test
    fun parseDiagnosticMessage() {
        val collector = DiagnosticsMessageCollector("test")
        collector.report(
            severity = params.severity,
            message = params.message
        )
        assertThat(
            collector.getDiagnostics().firstOrNull()
        ).isEqualTo(params.expected)
    }

    internal class TestParams(
        val message: String,
        val severity: CompilerMessageSeverity,
        val expected: RawDiagnosticMessage
    ) {
        override fun toString(): String {
            return message
        }
    }
    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        internal val testCases = listOf(
            // ksp kotlin
            TestParams(
                message = "[ksp] /foo/bar/Subject.kt:3: the real message",
                severity = CompilerMessageSeverity.ERROR,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.ERROR,
                    message = "the real message",
                    location = Location(
                        path = "/foo/bar/Subject.kt",
                        line = 3
                    )
                )
            ),
            // ksp java
            TestParams(
                message = "[ksp] /foo/bar/Subject.java:3: the real message",
                severity = CompilerMessageSeverity.ERROR,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.ERROR,
                    message = "the real message",
                    location = Location(
                        path = "/foo/bar/Subject.java",
                        line = 3
                    )
                )
            ),
            // ksp not a kotlin file - bad extension
            TestParams(
                message = "[ksp] /foo/bar/Subject.ktn:3: the real message",
                severity = CompilerMessageSeverity.ERROR,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.ERROR,
                    message = "/foo/bar/Subject.ktn:3: the real message",
                    location = null
                )
            ),
            // ksp not a kotlin file - no dot
            TestParams(
                message = "[ksp] /foo/bar/Subjectkt:3: the real message",
                severity = CompilerMessageSeverity.ERROR,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.ERROR,
                    message = "/foo/bar/Subjectkt:3: the real message",
                    location = null
                )
            ),
            // ksp not a java file - bad extension
            TestParams(
                message = "[ksp] /foo/bar/Subject.javax:3: the real message",
                severity = CompilerMessageSeverity.ERROR,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.ERROR,
                    message = "/foo/bar/Subject.javax:3: the real message",
                    location = null
                )
            ),
            // ksp not a java file - no dot
            TestParams(
                message = "[ksp] /foo/bar/Subjectjava:3: the real message",
                severity = CompilerMessageSeverity.ERROR,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.ERROR,
                    message = "/foo/bar/Subjectjava:3: the real message",
                    location = null
                )
            ),
            // kapt kotlin
            TestParams(
                message = "/foo/bar/Subject.kt:2: warning: the real message",
                severity = CompilerMessageSeverity.WARNING,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.WARNING,
                    message = "the real message",
                    location = Location(
                        path = "/foo/bar/Subject.kt",
                        line = 2
                    )
                )
            ),
            // kapt java
            TestParams(
                message = "/foo/bar/Subject.java:2: warning: the real message",
                severity = CompilerMessageSeverity.WARNING,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.WARNING,
                    message = "the real message",
                    location = Location(
                        path = "/foo/bar/Subject.java",
                        line = 2
                    )
                )
            ),
            // kapt not a kotlin file - bad extension
            TestParams(
                message = "/foo/bar/Subject.ktn:2: warning: the real message",
                severity = CompilerMessageSeverity.WARNING,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.WARNING,
                    message = "/foo/bar/Subject.ktn:2: warning: the real message",
                    location = null
                )
            ),
            // kapt not a kotlin file - no dot
            TestParams(
                message = "/foo/bar/Subjectkt:2: warning: the real message",
                severity = CompilerMessageSeverity.WARNING,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.WARNING,
                    message = "/foo/bar/Subjectkt:2: warning: the real message",
                    location = null
                )
            ),
            // kapt not a java file - bad extension
            TestParams(
                message = "/foo/bar/Subject.javan:2: warning: the real message",
                severity = CompilerMessageSeverity.WARNING,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.WARNING,
                    message = "/foo/bar/Subject.javan:2: warning: the real message",
                    location = null
                )
            ),
            // kapt not a java file - no dot
            TestParams(
                message = "/foo/bar/Subjectjava:2: warning: the real message",
                severity = CompilerMessageSeverity.WARNING,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.WARNING,
                    message = "/foo/bar/Subjectjava:2: warning: the real message",
                    location = null
                )
            ),
            // ksp kotlin on Windows
            TestParams(
                message = "[ksp] C:\\foo\\bar\\Subject.kt:3: the real message",
                severity = CompilerMessageSeverity.ERROR,
                expected = RawDiagnosticMessage(
                    kind = Diagnostic.Kind.ERROR,
                    message = "the real message",
                    location = Location(
                        path = "C:\\foo\\bar\\Subject.kt",
                        line = 3
                    )
                )
            ),
        )
    }
}
