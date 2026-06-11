/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.compiler.AppFunctionCompilerOptions
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.AppFunctionXmlGenerator
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

/** The processor to validate and generate adapters for AppFunctionSignature. */
class AppFunctionSignatureProcessor(
    private val options: AppFunctionCompilerOptions,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        hasProcessed = true

        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionSignatures =
            appFunctionSymbolResolver.resolveAnnotatedAppFunctionSignatures()
        val resolvedAnnotatedSerializableProxies =
            ResolvedAnnotatedSerializableProxies(
                appFunctionSymbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
            )

        if (appFunctionSignatures.isNotEmpty()) {
            val xmlGenerator = AppFunctionXmlGenerator(codeGenerator, logger)
            val groupedSignatures =
                appFunctionSignatures.groupBy { signature -> signature.appFunctionXmlFileName }

            for ((appFunctionXmlFileName, signaturesInGroup) in groupedSignatures) {
                xmlGenerator.generateXml(
                    signaturesInGroup,
                    resolvedAnnotatedSerializableProxies,
                    appFunctionSymbolResolver.getAppFunctionSerializablesDescriptionMap(),
                    XML_PACKAGE_NAME,
                    appFunctionXmlFileName,
                    options.appFunctionsXmlLocation,
                )
            }
        }

        return emptyList()
    }

    private companion object {
        const val XML_PACKAGE_NAME = "assets"
    }
}
