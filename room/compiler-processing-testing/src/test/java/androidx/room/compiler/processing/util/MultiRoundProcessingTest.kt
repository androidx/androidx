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

import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.tools.Diagnostic

@RunWith(Parameterized::class)
class MultiRoundProcessingTest(
    private val testRunner: TestRunner
) : MultiBackendTest() {
    private fun generateCode(index: Int): JavaFile {
        val typeSpec = TypeSpec.classBuilder(
            ClassName.bestGuess("foo.bar.Baz$index")
        ).build()
        return JavaFile.builder("foo.bar", typeSpec).build()
    }

    @Test
    fun dontRequestAnotherRound() {
        var runCnt = 0
        testRunner {
            runCnt++
        }
        // only run 1 if a second round is not explicitly requested
        assertThat(runCnt).isEqualTo(1)
    }

    @Suppress("NAME_SHADOWING") // intentional to avoid accessing the wrong one
    @Test
    fun multipleRounds() {
        var runCnt = 0
        fun checkAndIncrementRunCount(expected: Int) {
            assertThat(runCnt).isEqualTo(expected)
            runCnt++
        }
        testRunner(
            handlers = listOf(
                { invocation ->
                    checkAndIncrementRunCount(0)
                    invocation.processingEnv.filer.write(generateCode(0))
                },
                { invocation ->
                    checkAndIncrementRunCount(1)
                    invocation.processingEnv.filer.write(generateCode(1))
                },
                {
                    checkAndIncrementRunCount(2)
                }
            )
        )
        checkAndIncrementRunCount(3)
    }

    @Suppress("NAME_SHADOWING") // intentional to avoid accessing the wrong one
    @Test
    fun validateMessagesFromDifferentRounds() {
        var didRunFirstRoundAssertions = false
        var didRunSecondRoundAssertions = false
        testRunner(
            handlers = listOf(
                { invocation ->
                    invocation.processingEnv.messager.printMessage(
                        Diagnostic.Kind.NOTE,
                        "note from 1"
                    )
                    invocation.processingEnv.messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "warning from 1"
                    )
                    invocation.processingEnv.filer.write(generateCode(0))
                    invocation.assertCompilationResult {
                        // can assert diagnostics from followup rounds
                        hasWarning("warning from 1")
                        hasWarning("warning from 2")
                        hasError("error from 2")
                        hasNote("note from 1")
                        hasNote("note from 2")
                        didRunFirstRoundAssertions = true
                    }
                },
                { invocation ->
                    check(!didRunFirstRoundAssertions) {
                        "shouldn't run assertions before all runs are completed"
                    }
                    invocation.processingEnv.messager.printMessage(
                        Diagnostic.Kind.NOTE,
                        "note from 2"
                    )
                    invocation.processingEnv.messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "warning from 2"
                    )
                    invocation.processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "error from 2"
                    )
                    invocation.assertCompilationResult {
                        hasWarning("warning from 1")
                        hasWarning("warning from 2")
                        hasError("error from 2")
                        hasNote("note from 1")
                        hasNote("note from 2")
                        didRunSecondRoundAssertions = true
                    }
                }
            )
        )
        // just to make sure test didn't pass by failing to run assertions.
        assertThat(didRunFirstRoundAssertions).isTrue()
        assertThat(didRunSecondRoundAssertions).isTrue()
    }

    @Test
    fun validateIfRequestedRoundIsRun() {
        val result = runCatching {
            testRunner(
                handlers = listOf(
                    {},
                    {
                        // this won't happen because no code is generated in the first run
                    }
                )
            )
        }
        assertThat(
            result.isFailure
        ).isTrue()
        assertThat(
            result.exceptionOrNull()
        ).hasMessageThat()
            .contains(
                "Test runner requested another round but that didn't happen"
            )
    }

    @Test
    fun accessingDisposedHandlerIsNotAllowed() {
        val result = runCatching {
            lateinit var previousInvocation: XTestInvocation
            testRunner(
                handlers = listOf(
                    { invocation1 ->
                        invocation1.processingEnv.filer.write(generateCode(0))
                        previousInvocation = invocation1
                    },
                    {
                        previousInvocation.processingEnv.filer.write(generateCode(1))
                    }
                )
            )
        }
        assertThat(
            result.exceptionOrNull()?.cause
        ).hasMessageThat()
            .contains("Cannot use a test invocation after it is disposed")
    }

    @Test
    fun checkFailureFromAPreviousRoundIsNotMissed() {
        val result = runCatching {
            testRunner(
                handlers = listOf(
                    { invocation1 ->
                        invocation1.processingEnv.filer.write(generateCode(0))
                        // this will fail
                        throw AssertionError("i failed")
                    },
                    {
                        // this won't run
                        throw AssertionError("this shouldn't run as prev one failed")
                    }
                )
            )
        }
        assertThat(
            result.exceptionOrNull()?.cause
        ).hasMessageThat().contains("i failed")
    }
}