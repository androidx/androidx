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
                                    name = "a",
                                    type = Type(
                                        name = "kotlin.Boolean",
                                    )
                                ),
                                Parameter(
                                    name = "b",
                                    type = Type(
                                        name = "kotlin.Int",
                                    )
                                ),
                                Parameter(
                                    name = "c",
                                    type = Type(
                                        name = "kotlin.Long",
                                    )
                                ),
                                Parameter(
                                    name = "d",
                                    type = Type(
                                        name = "kotlin.Float",
                                    )
                                ),
                                Parameter(
                                    name = "e",
                                    type = Type(
                                        name = "kotlin.Double",
                                    )
                                ),
                                Parameter(
                                    name = "f",
                                    type = Type(
                                        name = "kotlin.Char",
                                    )
                                ),
                                Parameter(
                                    name = "g",
                                    type = Type(
                                        name = "kotlin.Short",
                                    )
                                )
                            ),
                            returnType = Type(
                                name = "kotlin.String",
                            )
                        ),
                        Method(
                            name = "doMoreStuff",
                            parameters = listOf(),
                            returnType = Type(
                                name = "kotlin.Unit",
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

        val javaGeneratedSources = AidlGenerator.generate(aidlCompiler, api, tmpDir)

        // Check expected java sources were generated.
        assertThat(javaGeneratedSources.map { it.packageName to it.interfaceName })
            .containsExactly(
                "com.mysdk" to "IMySdk",
                "com.mysdk" to "IStringTransactionCallback",
                "com.mysdk" to "IUnitTransactionCallback",
                "com.mysdk" to "ICancellationSignal",
            )

        // Check the contents of the AIDL generated files.
        val aidlGeneratedFiles = tmpDir.toFile().walk().filter { it.extension == "aidl" }
            .map { it.name to it.readText() }.toList()
        assertThat(aidlGeneratedFiles).containsExactly(
            "IMySdk.aidl" to """
                package com.mysdk;
                import com.mysdk.IStringTransactionCallback;
                import com.mysdk.IUnitTransactionCallback;
                oneway interface IMySdk {
                    void doStuff(boolean a, int b, long c, float d, double e, char f, int g, IStringTransactionCallback transactionCallback);
                    void doMoreStuff(IUnitTransactionCallback transactionCallback);
                }
            """.trimIndent(),
            "ICancellationSignal.aidl" to """
                package com.mysdk;
                oneway interface ICancellationSignal {
                    void cancel();
                }
            """.trimIndent(),
            "IStringTransactionCallback.aidl" to """
                package com.mysdk;
                import com.mysdk.ICancellationSignal;
                oneway interface IStringTransactionCallback {
                    void onCancellable(ICancellationSignal cancellationSignal);
                    void onSuccess(String result);
                    void onFailure(int errorCode, String errorMessage);
                }
            """.trimIndent(),
            "IUnitTransactionCallback.aidl" to """
                package com.mysdk;
                import com.mysdk.ICancellationSignal;
                oneway interface IUnitTransactionCallback {
                    void onCancellable(ICancellationSignal cancellationSignal);
                    void onSuccess();
                    void onFailure(int errorCode, String errorMessage);
                }
            """.trimIndent(),
        )

        // Check that the Java generated sources compile.
        ensureCompiles(
            javaGeneratedSources.map {
                Source.java(
                    "${it.packageName.replace('.', '/')}/${it.interfaceName}",
                    it.file.readText()
                )
            }.toList()
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