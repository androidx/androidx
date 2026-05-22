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

package androidx.compose.foundation.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextFieldBufferAppendDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = TextFieldBufferAppendDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(TextFieldBufferAppendDetector.TextFieldBufferInternalAppend)

    private val textFieldBufferStub =
        kotlin(
            """
        package androidx.compose.foundation.text.input

        open class TextFieldBuffer {
            fun append(text: CharSequence) {}
            fun replace(start: Int, end: Int, text: CharSequence, isFromHardwareSource: Boolean = false) {}
        }
        """
        )

    @Test
    fun appendFromInternalFoundation_flagsError() {
        lint()
            .files(
                textFieldBufferStub,
                kotlin(
                    """
                package androidx.compose.foundation.text.input.internal

                import androidx.compose.foundation.text.input.TextFieldBuffer

                fun test(buffer: TextFieldBuffer) {
                    buffer.append("test")
                }
                """
                ),
            )
            .run()
            .expect(
                """
                src/androidx/compose/foundation/text/input/internal/test.kt:7: Error: Do not use append on TextFieldBuffer internally as it swallows the hardware source tracking information. Use the internal replace overload instead. [TextFieldBufferInternalAppend]
                                    buffer.append("test")
                                    ~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun appendFromExternalPackage_noError() {
        lint()
            .files(
                textFieldBufferStub,
                kotlin(
                    """
                package com.example.app

                import androidx.compose.foundation.text.input.TextFieldBuffer

                fun test(buffer: TextFieldBuffer) {
                    buffer.append("test")
                }
                """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun replaceFromInternalFoundation_noError() {
        lint()
            .files(
                textFieldBufferStub,
                kotlin(
                    """
                package androidx.compose.foundation.text.input.internal

                import androidx.compose.foundation.text.input.TextFieldBuffer

                fun test(buffer: TextFieldBuffer) {
                    buffer.replace(0, 0, "test", isFromHardwareSource = true)
                }
                """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun appendOnStringBuilderFromInternalFoundation_noError() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.foundation.text.input.internal

                fun test(builder: StringBuilder) {
                    builder.append("test")
                }
                """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun appendOnSubclassFromInternalFoundation_flagsError() {
        lint()
            .files(
                textFieldBufferStub,
                kotlin(
                    """
                package androidx.compose.foundation.text.input.internal

                import androidx.compose.foundation.text.input.TextFieldBuffer

                class MyBuffer : TextFieldBuffer()

                fun test(buffer: MyBuffer) {
                    buffer.append("test")
                }
                """
                ),
            )
            .run()
            .expect(
                """
                src/androidx/compose/foundation/text/input/internal/MyBuffer.kt:9: Error: Do not use append on TextFieldBuffer internally as it swallows the hardware source tracking information. Use the internal replace overload instead. [TextFieldBufferInternalAppend]
                                    buffer.append("test")
                                    ~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun appendFromJavaCaller_flagsError() {
        lint()
            .files(
                textFieldBufferStub,
                java(
                    """
                package androidx.compose.foundation.text.input.internal;

                import androidx.compose.foundation.text.input.TextFieldBuffer;

                public class JavaCaller {
                    public void test(TextFieldBuffer buffer) {
                        buffer.append("test");
                    }
                }
                """
                ),
            )
            .run()
            .expect(
                """
                src/androidx/compose/foundation/text/input/internal/JavaCaller.java:8: Error: Do not use append on TextFieldBuffer internally as it swallows the hardware source tracking information. Use the internal replace overload instead. [TextFieldBufferInternalAppend]
                                        buffer.append("test");
                                        ~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
