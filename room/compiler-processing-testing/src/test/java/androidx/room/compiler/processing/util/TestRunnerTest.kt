/*
 * Copyright 2020 The Android Open Source Project
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

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import javax.tools.Diagnostic

class TestRunnerTest {
    @Test
    fun generatedBadCode_expected() = generatedBadCode(assertFailure = true)

    @Test(expected = AssertionError::class)
    fun generatedBadCode_unexpected() = generatedBadCode(assertFailure = false)

    private fun generatedBadCode(assertFailure: Boolean) {
        runProcessorTest {
            if (it.processingEnv.findTypeElement("foo.Foo") == null) {
                val badCode = TypeSpec.classBuilder("Foo").apply {
                    addStaticBlock(
                        CodeBlock.of("bad code")
                    )
                }.build()
                val badGeneratedFile = JavaFile.builder("foo", badCode).build()
                it.processingEnv.filer.write(
                    badGeneratedFile
                )
            }
            if (assertFailure) {
                it.assertCompilationResult {
                    compilationDidFail()
                }
            }
        }
    }

    @Test
    fun reportedError_expected() = reportedError(assertFailure = true)

    @Test(expected = AssertionError::class)
    fun reportedError_unexpected() = reportedError(assertFailure = false)

    fun reportedError(assertFailure: Boolean) {
        runProcessorTest {
            it.processingEnv.messager.printMessage(
                kind = Diagnostic.Kind.ERROR,
                msg = "reported error"
            )
            if (assertFailure) {
                it.assertCompilationResult {
                    hasError("reported error")
                }
            }
        }
    }
}