/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room

import androidx.room.compiler.processing.XProcessingEnvConfig
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.util.CompilationResultSubject
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import javax.annotation.processing.Processor

fun runProcessorTestWithK1(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    androidx.room.compiler.processing.util.runProcessorTest(
        sources = sources,
        classpath = classpath,
        options = options,
        javacArguments = javacArguments,
        kotlincArguments = listOf("-language-version=1.9", "-api-version=1.9") + kotlincArguments,
        handler = handler
    )
}

fun runProcessorTestWithK1(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    createProcessingSteps: () -> Iterable<XProcessingStep>,
    onCompilationResult: (CompilationResultSubject) -> Unit
) {
    androidx.room.compiler.processing.util.runProcessorTest(
        sources = sources,
        classpath = classpath,
        options = options,
        javacArguments = javacArguments,
        kotlincArguments = listOf("-language-version=1.9", "-api-version=1.9") + kotlincArguments,
        createProcessingSteps = createProcessingSteps,
        onCompilationResult = onCompilationResult
    )
}

fun runProcessorTestWithK1(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    javacProcessors: List<Processor>,
    symbolProcessorProviders: List<SymbolProcessorProvider>,
    onCompilationResult: (CompilationResultSubject) -> Unit
) {
    runProcessorTest(
        sources = sources,
        classpath = classpath,
        options = options,
        javacArguments = javacArguments,
        kotlincArguments = listOf("-language-version=1.9", "-api-version=1.9") + kotlincArguments,
        javacProcessors = javacProcessors,
        symbolProcessorProviders = symbolProcessorProviders,
        onCompilationResult = onCompilationResult
    )
}

fun runKspTestWithK1(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig? = null,
    handler: (XTestInvocation) -> Unit
) {
    if (config != null) {
        runKspTest(
            sources = sources,
            classpath = classpath,
            options = options,
            javacArguments = javacArguments,
            kotlincArguments =
                listOf("-language-version=1.9", "-api-version=1.9") + kotlincArguments,
            config = config,
            handler = handler
        )
    } else {
        runKspTest(
            sources = sources,
            classpath = classpath,
            options = options,
            javacArguments = javacArguments,
            kotlincArguments =
                listOf("-language-version=1.9", "-api-version=1.9") + kotlincArguments,
            handler = handler
        )
    }
}
