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
import androidx.room.compiler.processing.util.JavaFileObjectResource
import androidx.room.compiler.processing.util.Resource
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.toDiagnosticMessages
import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import java.io.File
import javax.annotation.processing.Processor
import javax.tools.JavaFileObject

@ExperimentalProcessingApi
internal class JavacCompilationTestRunner(
    private val testProcessors: List<Processor> = emptyList()
) : CompilationTestRunner {

    override val name: String = "javac"

    override fun canRun(params: TestCompilationParameters): Boolean {
        return params.sources.all { it is Source.JavaSource }
    }

    override fun compile(workingDir: File, params: TestCompilationParameters): CompilationResult {
        val syntheticJavacProcessor = SyntheticJavacProcessor(params.config, params.handlers)
        val processors = testProcessors + syntheticJavacProcessor
        val sources =
            params.sources.ifEmpty {
                // synthesize a source to trigger compilation
                listOf(
                    Source.java(
                        qName = "xprocessing.generated.SyntheticSource",
                        code =
                            """
                            package xprocessing.generated;
                            public class SyntheticSource {}
                            """
                                .trimIndent()
                    )
                )
            }

        val optionsArg = params.options.entries.map { "-A${it.key}=${it.value}" }
        val compiler =
            Compiler.javac()
                .withProcessors(processors)
                .withOptions(params.javacArguments + optionsArg + "-Xlint")
                .let {
                    if (params.classpath.isNotEmpty()) {
                        it.withClasspath(params.classpath)
                    } else {
                        it
                    }
                }
        val javaFileObjects = sources.associateBy { it.toJFO() }
        val compilation = compiler.compile(javaFileObjects.keys)
        val generatedSources =
            if (compilation.status() == Compilation.Status.SUCCESS) {
                compilation.generatedSourceFiles().associateWith { Source.fromJavaFileObject(it) }
            } else {
                compilation
                    .diagnostics()
                    .mapNotNull { it.source }
                    .associateWith { Source.fromJavaFileObject(it) }
            }
        val generatedResources: List<Resource> =
            if (compilation.status() == Compilation.Status.SUCCESS) {
                compilation
                    .generatedFiles()
                    .filter { it.kind == JavaFileObject.Kind.OTHER }
                    .map {
                        JavaFileObjectResource(it.toUri().path.substringAfter("CLASS_OUTPUT/"), it)
                    }
            } else {
                emptyList()
            }

        val diagnostics: List<DiagnosticMessage> =
            compilation.diagnostics().toDiagnosticMessages(javaFileObjects + generatedSources)

        return JavaCompileTestingCompilationResult(
            testRunner = this,
            delegate = compilation,
            processor = syntheticJavacProcessor,
            diagnostics = diagnostics.groupBy { it.kind },
            generatedSources = generatedSources.values.toList(),
            generatedResources = generatedResources
        )
    }
}
