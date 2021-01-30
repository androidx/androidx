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
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

typealias TestRunner = (block: (XTestInvocation) -> Unit) -> Unit

@RunWith(Parameterized::class)
class GeneratedCodeMatchTest internal constructor(
    private val runTest: TestRunner
) {
    @Test
    fun successfulGeneratedCodeMatch() {
        val file = JavaFile.builder(
            "foo.bar",
            TypeSpec.classBuilder("Baz").build()
        ).build()
        runTest { invocation ->
            if (invocation.processingEnv.findTypeElement("foo.bar.Baz") == null) {
                invocation.processingEnv.filer.write(
                    file
                )
            }
            invocation.assertCompilationResult {
                generatedSource(
                    Source.java(
                        "foo.bar.Baz",
                        file.toString()
                    )
                )
            }
        }
    }

    @Test
    fun missingGeneratedCode() {
        val result = runCatching {
            runTest { invocation ->
                invocation.assertCompilationResult {
                    generatedSource(
                        Source.java(
                            "foo.bar.Baz",
                            ""
                        )
                    )
                }
            }
        }
        assertThat(result.exceptionOrNull())
            .hasMessageThat()
            .contains("Didn't generate SourceFile[foo/bar/Baz.java]")
    }

    @Test
    fun missingGeneratedCode_contentMismatch() {
        val generated = JavaFile.builder(
            "foo.bar",
            TypeSpec.classBuilder("Baz")
                .addField(
                    TypeName.BOOLEAN, "bar"
                )
                .build()
        ).build()
        val expected = JavaFile.builder(
            "foo.bar",
            TypeSpec.classBuilder("Baz").addField(
                TypeName.BOOLEAN, "foo"
            ).build()
        ).build()
        val result = runCatching {
            runTest { invocation: XTestInvocation ->
                if (invocation.processingEnv.findTypeElement("foo.bar.Baz") == null) {
                    invocation.processingEnv.filer.write(generated)
                }
                invocation.assertCompilationResult {
                    generatedSource(
                        Source.java("foo.bar.Baz", expected.toString())
                    )
                }
            }
        }

        val mismatch = SourceFileMismatch(
            expected = Line(
                pos = 4,
                content = "boolean foo;"
            ),
            actual = Line(
                pos = 4,
                content = "boolean bar;"
            )
        )
        assertThat(result.exceptionOrNull()).hasMessageThat().contains(mismatch.toString())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun runners(): List<TestRunner> = listOfNotNull(
            { block: (XTestInvocation) -> Unit ->
                runJavaProcessorTest(sources = emptyList(), handler = block)
            },
            { block: (XTestInvocation) -> Unit ->
                runKaptTest(sources = emptyList(), handler = block)
            },
            if (CompilationTestCapabilities.canTestWithKsp) {
                { block: (XTestInvocation) -> Unit ->
                    runKspTest(sources = emptyList(), handler = block)
                }
            } else {
                null
            }
        )
    }
}
