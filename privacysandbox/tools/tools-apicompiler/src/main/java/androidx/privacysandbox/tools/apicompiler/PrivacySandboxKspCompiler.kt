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

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.apicompiler.generator.SandboxApiVersion
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
) : SymbolProcessor {
    companion object {
        const val AIDL_COMPILER_PATH_OPTIONS_KEY = "aidl_compiler_path"
        const val FRAMEWORK_AIDL_PATH_OPTIONS_KEY = "framework_aidl_path"
        const val SKIP_SDK_RUNTIME_COMPAT_LIBRARY_OPTIONS_KEY = "skip_sdk_runtime_compat_library"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // This method is called multiple times during compilation and the resolver will only return
        // relevant files for each particular processing round. This instance might also be kept
        // by KSP between rounds or for incremental compilation. This means that at some point
        // KSP will always invoke this processor with no valid services, so we should just stop
        // processing.
        if (resolver.getSymbolsWithAnnotation(
                PrivacySandboxService::class.qualifiedName!!).none()) {
            return emptyList()
        }

        val aidlCompilerPath = options[AIDL_COMPILER_PATH_OPTIONS_KEY]?.let(Paths::get)
        if (aidlCompilerPath == null) {
            logger.error("KSP argument '$AIDL_COMPILER_PATH_OPTIONS_KEY' was not set.")
            return emptyList()
        }
        val frameworkAidlPath = options[FRAMEWORK_AIDL_PATH_OPTIONS_KEY]?.let(Paths::get)
        if (frameworkAidlPath == null) {
            logger.warn(
                "KSP argument '$FRAMEWORK_AIDL_PATH_OPTIONS_KEY' was not set. This " +
                "will become a required argument in the future."
            )
        }

        val skipCompatLibrary =
            options[SKIP_SDK_RUNTIME_COMPAT_LIBRARY_OPTIONS_KEY]?.lowercase().toBoolean()
        val target = if (skipCompatLibrary) {
            SandboxApiVersion.API_33
        } else SandboxApiVersion.SDK_RUNTIME_COMPAT_LIBRARY

        val parsedApi = ApiParser(resolver, logger).parseApi()

        SdkCodeGenerator(
            codeGenerator,
            parsedApi,
            aidlCompilerPath,
            frameworkAidlPath,
            target,
        ).generate()
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
