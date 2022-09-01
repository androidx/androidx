/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.Method
import androidx.privacysandbox.tools.core.Parameter
import androidx.privacysandbox.tools.core.ParsedApi
import androidx.privacysandbox.tools.core.Type
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Files.createTempDirectory
import javax.tools.Diagnostic
import kotlin.io.path.Path
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AidlGeneratorTest {
    @Test
    fun generate() {
        val api = ParsedApi(
            services = mutableSetOf(
                AnnotatedInterface(
                    name = "MySdk",
                    packageName = "com.mysdk",
                    methods = listOf(
                        Method(
                            name = "doStuff",
                            parameters = listOf(
                                Parameter(
                                    name = "x",
                                    type = Type(
                                        name = "Int",
                                    )
                                ),
                                Parameter(
                                    name = "y",
                                    type = Type(
                                        name = "Int",
                                    )
                                )
                            ),
                            returnType = Type(
                                name = "String",
                            )
                        )
                    )
                )
            )
        )
        val aidlPath = System.getProperty("aidl_compiler_path")?.let(::Path)
            ?: throw IllegalArgumentException("aidl_compiler_path flag not set.")
        val tmpDir = createTempDirectory("test")
        val aidlCompiler = AidlCompiler(aidlPath)

        val javaGeneratedSources = AidlGenerator(aidlCompiler).generate(api, tmpDir)

        // Check expected java sources were generated.
        assertThat(javaGeneratedSources.map { it.packageName to it.interfaceName })
            .containsExactly("com.mysdk" to "IMySdk")

        // Check the contents of the AIDL generated files.
        val aidlGeneratedFiles = tmpDir.toFile().walk().filter { it.extension == "aidl" }.toList()
        assertThat(aidlGeneratedFiles).hasSize(1)
        assertThat(aidlGeneratedFiles.first().readText()).isEqualTo(
            """
                package com.mysdk;

                oneway interface IMySdk {
                    void doStuff(int x, int y);
                }
            """.trimIndent()
        )

        // Check that the Java generated sources compile.
        ensureCompiles(
            listOf(
                Source.java("com/mysdk/IMySdk", javaGeneratedSources.first().file.readText())
            )
        )
    }

    private fun ensureCompiles(sources: List<Source>) {
        val result = compile(
            createTempDirectory("compile").toFile(),
            TestCompilationArguments(
                sources = sources,
            )
        )
        assertWithMessage(
            "Compilation of java files generated from AIDL failed with errors: " +
                "${result.diagnostics[Diagnostic.Kind.ERROR]?.joinToString("\n") { it.msg }}"
        ).that(
            result.success
        ).isTrue()
    }
}