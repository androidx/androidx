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

package androidx.room.compiler.processing.util.runner

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.SyntheticJavacProcessor
import androidx.room.compiler.processing.util.CompilationResult
import androidx.room.compiler.processing.util.DiagnosticMessage
import androidx.room.compiler.processing.util.JavaCompileTestingCompilationResult
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.toDiagnosticMessages
import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import java.io.File

@ExperimentalProcessingApi
internal object JavacCompilationTestRunner : CompilationTestRunner {

    override val name: String = "javac"

    override fun canRun(params: TestCompilationParameters): Boolean {
        return params.sources.all { it is Source.JavaSource }
    }

    override fun compile(workingDir: File, params: TestCompilationParameters): CompilationResult {
        val syntheticJavacProcessor = SyntheticJavacProcessor(params.handlers)
        val sources = if (params.sources.isEmpty()) {
            // synthesize a source to trigger compilation
            listOf(
                Source.java(
                    qName = "foo.bar.SyntheticSource",
                    code = """
                    package foo.bar;
                    public class SyntheticSource {}
                    """.trimIndent()
                )
            )
        } else {
            params.sources
        }

        val optionsArg = params.options.entries.map {
            "-A${it.key}=${it.value}"
        }
        val compiler = Compiler
            .javac()
            .withProcessors(syntheticJavacProcessor)
            .withOptions(optionsArg + "-Xlint")
            .let {
                if (params.classpath.isNotEmpty()) {
                    it.withClasspath(params.classpath)
                } else {
                    it
                }
            }
        val javaFileObjects = sources.associateBy { it.toJFO() }
        val compilation = compiler.compile(javaFileObjects.keys)
        val generatedSources = if (compilation.status() == Compilation.Status.SUCCESS) {
            compilation.generatedSourceFiles().associate {
                it to Source.fromJavaFileObject(it)
            }
        } else {
            compilation.diagnostics().mapNotNull {
                it.source
            }.associate {
                it to Source.fromJavaFileObject(it)
            }
        }

        val diagnostics: List<DiagnosticMessage> = compilation.diagnostics().toDiagnosticMessages(
            javaFileObjects + generatedSources
        )

        return JavaCompileTestingCompilationResult(
            testRunner = this,
            delegate = compilation,
            processor = syntheticJavacProcessor,
            diagnostics = diagnostics.groupBy { it.kind },
            generatedSources = generatedSources.values.toList()
        )
    }
}