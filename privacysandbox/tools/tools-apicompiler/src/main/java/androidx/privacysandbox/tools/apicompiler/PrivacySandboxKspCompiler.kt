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

import androidx.privacysandbox.tools.apicompiler.generator.SdkCodeGenerator
import androidx.privacysandbox.tools.apicompiler.parser.ApiParser
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import java.nio.file.Paths

class PrivacySandboxKspCompiler(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
) :
    SymbolProcessor {
    companion object {
        const val AIDL_COMPILER_PATH_OPTIONS_KEY = "aidl_compiler_path"
    }

    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }
        invoked = true
        val parsedApi = ApiParser(resolver, logger).parseApi()

        val path = options[AIDL_COMPILER_PATH_OPTIONS_KEY]?.let(Paths::get)
        if (path == null) {
            logger.error("KSP argument '$AIDL_COMPILER_PATH_OPTIONS_KEY' was not set.")
            return emptyList()
        }
        SdkCodeGenerator(codeGenerator, parsedApi, path).generate()
        return emptyList()
    }

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return PrivacySandboxKspCompiler(
                environment.logger,
                environment.codeGenerator,
                environment.options
            )
        }
    }
}