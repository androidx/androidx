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

package androidx.privacysandbox.tools.apicompiler

import androidx.privacysandbox.tools.testing.CompilationTestHelper
import androidx.privacysandbox.tools.testing.TestEnvironment
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationResult

/**
 * Compile the given sources using the PrivacySandboxKspCompiler.
 *
 * Default parameters will set required options like AIDL compiler path and use the latest
 * Android platform API stubs that support the Privacy Sandbox.
 */
fun compileWithPrivacySandboxKspCompiler(
    sources: List<Source>,
    extraProcessorOptions: Map<String, String> = mapOf(),
): TestCompilationResult {
    val provider = PrivacySandboxKspCompiler.Provider()

    val processorOptions = buildMap {
        put("aidl_compiler_path", TestEnvironment.aidlCompilerPath.toString())
        put("framework_aidl_path", TestEnvironment.frameworkAidlPath.toString())
        putAll(extraProcessorOptions)
    }

    return CompilationTestHelper.compileAll(
        sources,
        symbolProcessorProviders = listOf(provider),
        processorOptions = processorOptions,
    )
}
