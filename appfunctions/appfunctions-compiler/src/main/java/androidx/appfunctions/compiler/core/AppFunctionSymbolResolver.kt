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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionComponentRegistryAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSchemaDefinitionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableProxyAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.SERIALIZABLE_PROXY_PACKAGE_NAME
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/** The helper class to resolve AppFunction related symbols. */
class AppFunctionSymbolResolver(private val resolver: Resolver) {

    /** Resolves symbols annotated with @AppFunctionSchemaDefinition. */
    fun resolveAnnotatedAppFunctionSchemaDefinitions(): List<AnnotatedAppFunctionSchemaDefinition> {
        return resolver
            .getSymbolsWithAnnotation(
                AppFunctionSchemaDefinitionAnnotation.CLASS_NAME.canonicalName
            )
            .map { declaration ->
                if (declaration !is KSClassDeclaration) {
                    throw ProcessingException(
                        "Only class can be annotated with @AppFunctionSchemaDefinition",
                        declaration,
                    )
                }
                AnnotatedAppFunctionSchemaDefinition(declaration)
            }
            .toList()
    }

    /** Resolves valid functions annotated with @AppFunction annotation. */
    fun resolveAnnotatedAppFunctions(): List<AnnotatedAppFunctions> {
        return resolver
            .getSymbolsWithAnnotation(AppFunctionAnnotation.CLASS_NAME.canonicalName)
            .map { declaration ->
                if (declaration !is KSFunctionDeclaration) {
                    throw ProcessingException(
                        "Only functions can be annotated with @AppFunction",
                        declaration,
                    )
                }
                declaration
            }
            .groupBy { declaration ->
                declaration.parentDeclaration as? KSClassDeclaration
                    ?: throw ProcessingException(
                        "Top level functions cannot be annotated with @AppFunction ",
                        declaration,
                    )
            }
            .map { (classDeclaration, appFunctionsDeclarations) ->
                AnnotatedAppFunctions(classDeclaration, appFunctionsDeclarations).validate()
            }
    }

    /**
     * Resolves all classes annotated with @AppFunctionSerializable
     *
     * @return a list of AnnotatedAppFunctionSerializable
     */
    fun resolveAnnotatedAppFunctionSerializables(): List<AnnotatedAppFunctionSerializable> {
        return resolver
            .getSymbolsWithAnnotation(AppFunctionSerializableAnnotation.CLASS_NAME.canonicalName)
            .map { declaration ->
                if (declaration !is KSClassDeclaration) {
                    throw ProcessingException(
                        "Only classes can be annotated with @AppFunctionSerializable",
                        declaration,
                    )
                }
                AnnotatedAppFunctionSerializable(declaration).validate()
            }
            .toList()
    }

    /**
     * Resolves all classes annotated with @AppFunctionSerializableProxy from the current
     * compilation unit.
     *
     * @return a list of AnnotatedAppFunctionSerializableProxy
     */
    fun resolveLocalAnnotatedAppFunctionSerializableProxy():
        List<AnnotatedAppFunctionSerializableProxy> {
        return resolver
            .getSymbolsWithAnnotation(
                AppFunctionSerializableProxyAnnotation.CLASS_NAME.canonicalName
            )
            .map { declaration ->
                if (declaration !is KSClassDeclaration) {
                    throw ProcessingException(
                        "Only classes can be annotated with @AppFunctionSerializableProxy",
                        declaration,
                    )
                }
                AnnotatedAppFunctionSerializableProxy(declaration).validate()
            }
            .toList()
    }

    /**
     * Resolves all classes annotated with @AppFunctionSerializableProxy from the
     * [SERIALIZABLE_PROXY_PACKAGE_NAME] package.
     *
     * @return a list of AnnotatedAppFunctionSerializableProxy
     */
    @OptIn(KspExperimental::class)
    fun resolveAllAnnotatedSerializableProxiesFromModule():
        List<AnnotatedAppFunctionSerializableProxy> {
        return resolver
            .getDeclarationsFromPackage(SERIALIZABLE_PROXY_PACKAGE_NAME)
            .filter {
                it.annotations.findAnnotation(AppFunctionSerializableProxyAnnotation.CLASS_NAME) !=
                    null
            }
            .map { declaration ->
                if (declaration !is KSClassDeclaration) {
                    throw ProcessingException(
                        "Only classes can be annotated with @AppFunctionSerializableProxy",
                        declaration,
                    )
                }
                AnnotatedAppFunctionSerializableProxy(declaration).validate()
            }
            .toList()
    }

    /**
     * Gets all [AnnotatedAppFunctions] from all processed modules.
     *
     * Unlike [resolveAnnotatedAppFunctions] that resolves symbols from annotation within the same
     * compilation unit. [getAnnotatedAppFunctionsFromAllModules] looks up all AppFunction symbols,
     * including those are already processed.
     */
    fun getAnnotatedAppFunctionsFromAllModules(): List<AnnotatedAppFunctions> {
        return filterAppFunctionComponentQualifiedNames(
                AppFunctionComponentRegistryAnnotation.Category.FUNCTION
            )
            .map { componentName ->
                val ksName = resolver.getKSNameFromString(componentName)
                val functionDeclarations = resolver.getFunctionDeclarationsByName(ksName).toList()
                if (functionDeclarations.isEmpty()) {
                    throw ProcessingException(
                        "Unable to find KSFunctionDeclaration for ${ksName.asString()}",
                        null,
                    )
                }
                if (functionDeclarations.size > 1) {
                    throw ProcessingException(
                        "Conflicts KSFunctionDeclaration for ${ksName.asString()}",
                        null,
                    )
                }
                functionDeclarations.single()
            }
            .groupBy { declaration ->
                declaration.parentDeclaration as? KSClassDeclaration
                    ?: throw ProcessingException(
                        "Top level functions cannot be annotated with @AppFunction ",
                        declaration,
                    )
            }
            .map { (classDeclaration, appFunctionsDeclarations) ->
                AnnotatedAppFunctions(classDeclaration, appFunctionsDeclarations).validate()
            }
    }

    /** Gets generated AppFunctionInventory implementations. */
    fun getGeneratedAppFunctionInventories(): List<KSClassDeclaration> {
        return filterAppFunctionComponentQualifiedNames(
                AppFunctionComponentRegistryAnnotation.Category.INVENTORY
            )
            .map { componentName ->
                val ksName = resolver.getKSNameFromString(componentName)
                resolver.getClassDeclarationByName(ksName)
                    ?: throw ProcessingException(
                        "Unable to find KSClassDeclaration for ${ksName.asString()}",
                        null,
                    )
            }
    }

    /** Gets generated AppFunctionInvoker implementations. */
    fun getGeneratedAppFunctionInvokers(): List<KSClassDeclaration> {
        return filterAppFunctionComponentQualifiedNames(
                AppFunctionComponentRegistryAnnotation.Category.INVOKER
            )
            .map { componentName ->
                val ksName = resolver.getKSNameFromString(componentName)
                resolver.getClassDeclarationByName(ksName)
                    ?: throw ProcessingException(
                        "Unable to find KSClassDeclaration for ${ksName.asString()}",
                        null,
                    )
            }
    }

    /** Gets all @AppFunctionSchemaDefinition from all modules. */
    fun getAppFunctionSchemaDefinitionFromAllModules(): List<AnnotatedAppFunctionSchemaDefinition> {
        return filterAppFunctionComponentQualifiedNames(
                AppFunctionComponentRegistryAnnotation.Category.SCHEMA_DEFINITION
            )
            .map { componentName ->
                val ksName = resolver.getKSNameFromString(componentName)
                val classDeclaration =
                    resolver.getClassDeclarationByName(ksName)
                        ?: throw ProcessingException(
                            "Unable to find KSClassDeclaration for ${ksName.asString()}",
                            null,
                        )
                AnnotatedAppFunctionSchemaDefinition(classDeclaration)
            }
    }

    @OptIn(KspExperimental::class)
    private fun filterAppFunctionComponentQualifiedNames(
        filterComponentCategory: String
    ): List<String> {
        return resolver
            .getDeclarationsFromPackage(APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME)
            .flatMap { node ->
                val registryAnnotation =
                    node.annotations.findAnnotation(
                        AppFunctionComponentRegistryAnnotation.CLASS_NAME
                    ) ?: return@flatMap emptyList<String>()
                val componentCategory =
                    registryAnnotation.requirePropertyValueOfType(
                        AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_CATEGORY,
                        String::class,
                    )
                val componentNames =
                    registryAnnotation.requirePropertyValueOfType(
                        AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_NAMES,
                        List::class,
                    )
                return@flatMap if (componentCategory == filterComponentCategory) {
                    componentNames.filterIsInstance<String>()
                } else {
                    emptyList<String>()
                }
            }
            .toList()
    }
}
