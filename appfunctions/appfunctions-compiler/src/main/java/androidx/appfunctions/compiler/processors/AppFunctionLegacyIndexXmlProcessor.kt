/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.appfunctions.compiler.core.AppFunctionLegacySchemaXmlGenerator
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * Generates AppFunction's index xml file for the legacy AppSearch indexer to index.
 *
 * The generator would write an XML file as `/assets/app_functions.xml`. The file would be packaged
 * into the APK's asset when assembled. So that the AppSearch indexer can look up the asset and
 * inject metadata into platform AppSearch database accordingly.
 *
 * The new indexer will index additional properties based on the schema defined in SDK instead of
 * the pre-defined one in AppSearch.
 */
class AppFunctionLegacyIndexXmlProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: AppFunctionCompilerOptions,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val resolvedAnnotatedSerializableProxies =
            ResolvedAnnotatedSerializableProxies(
                appFunctionSymbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
            )
        val legacyXmlGenerator = AppFunctionLegacySchemaXmlGenerator(codeGenerator, logger)
        legacyXmlGenerator.generateLegacyIndexXml(
            appFunctionSymbolResolver.getAnnotatedAppFunctionsFromAllModules(),
            resolvedAnnotatedSerializableProxies,
            options.appFunctionsXmlLocation,
        )
        return emptyList()
    }
}
