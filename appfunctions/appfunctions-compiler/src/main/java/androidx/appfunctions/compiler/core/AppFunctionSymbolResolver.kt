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
import com.google.devtools.ksp.symbol.Modifier

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

    /**
     * Resolves functions annotated with @AppFunction annotation ***that are not validated yet***.
     *
     * The caller should generally prefer using [resolveAnnotatedAppFunctions] to ensure that the
     * processor is working on validated AppFunctions. This should only be used when the visibility
     * to invalidated AppFunctions is required, such as for determining the symbols for next turn
     * processing.
     */
    fun resolveUnvalidatedAnnotatedAppFunctions(): List<AnnotatedAppFunctions> {
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
                AnnotatedAppFunctions(classDeclaration, appFunctionsDeclarations)
            }
    }

    /** Resolves valid functions annotated with @AppFunction annotation. */
    fun resolveAnnotatedAppFunctions(): List<AnnotatedAppFunctions> {
        return resolveUnvalidatedAnnotatedAppFunctions().map { annotatedAppFunction ->
            annotatedAppFunction.validate()
        }
    }

    /**
     * Resolves all classes annotated with @AppFunctionSerializable
     *
     * @return a list of [AppFunctionSerializableType]
     */
    fun resolveAnnotatedAppFunctionSerializables(): List<AppFunctionSerializableType> {
        return resolver
            .getSymbolsWithAnnotation(AppFunctionSerializableAnnotation.CLASS_NAME.canonicalName)
            .map { declaration ->
                if (declaration !is KSClassDeclaration) {
                    throw ProcessingException(
                        "Only classes can be annotated with @AppFunctionSerializable",
                        declaration,
                    )
                }
                if (declaration.modifiers.contains(Modifier.SEALED)) {
                    AnnotatedOneOfAppFunctionSerializable(declaration).validate()
                } else {
                    AnnotatedAppFunctionSerializable(declaration).validate()
                }
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
        val filteredAppFunctionComponents =
            filterAppFunctionComponent(AppFunctionComponentRegistryAnnotation.Category.FUNCTION)

        return filteredAppFunctionComponents
            .map { component ->
                val ksName = resolver.getKSNameFromString(component.qualifiedName)
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
                AnnotatedAppFunctions(
                        classDeclaration,
                        appFunctionsDeclarations,
                        filteredAppFunctionComponents.associate { it.qualifiedName to it.docString },
                    )
                    .validate()
            }
    }

    /**
     * Gets a map of qualified name to docstring for [AnnotatedAppFunctionSerializable] from all
     * processed modules.
     */
    fun getAppFunctionSerializablesDescriptionMap(): Map<String, String> {
        return filterAppFunctionComponent(
                AppFunctionComponentRegistryAnnotation.Category.SERIALIZABLE
            )
            .associate { it -> it.qualifiedName to it.docString }
    }

    /** Gets generated AppFunctionInventory implementations. */
    fun getGeneratedAppFunctionInventories(): List<KSClassDeclaration> {
        return filterAppFunctionComponent(AppFunctionComponentRegistryAnnotation.Category.INVENTORY)
            .map { component ->
                val ksName = resolver.getKSNameFromString(component.qualifiedName)
                resolver.getClassDeclarationByName(ksName)
                    ?: throw ProcessingException(
                        "Unable to find KSClassDeclaration for ${ksName.asString()}",
                        null,
                    )
            }
    }

    /** Gets generated AppFunctionInvoker implementations. */
    fun getGeneratedAppFunctionInvokers(): List<KSClassDeclaration> {
        return filterAppFunctionComponent(AppFunctionComponentRegistryAnnotation.Category.INVOKER)
            .map { component ->
                val ksName = resolver.getKSNameFromString(component.qualifiedName)
                resolver.getClassDeclarationByName(ksName)
                    ?: throw ProcessingException(
                        "Unable to find KSClassDeclaration for ${ksName.asString()}",
                        null,
                    )
            }
    }

    /** Gets all @AppFunctionSchemaDefinition from all modules. */
    fun getAppFunctionSchemaDefinitionFromAllModules(): List<AnnotatedAppFunctionSchemaDefinition> {
        return filterAppFunctionComponent(
                AppFunctionComponentRegistryAnnotation.Category.SCHEMA_DEFINITION
            )
            .map { component ->
                val ksName = resolver.getKSNameFromString(component.qualifiedName)
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
    private fun filterAppFunctionComponent(
        filterComponentCategory: String
    ): List<AppFunctionComponentRegistryGenerator.AppFunctionComponent> {
        return resolver
            .getDeclarationsFromPackage(APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME)
            .flatMap { node ->
                val registryAnnotation =
                    node.annotations.findAnnotation(
                        AppFunctionComponentRegistryAnnotation.CLASS_NAME
                    ) ?: return@flatMap emptyList()
                val componentCategory =
                    registryAnnotation.requirePropertyValueOfType(
                        AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_CATEGORY,
                        String::class,
                    )

                if (componentCategory != filterComponentCategory) {
                    return@flatMap emptyList()
                }

                val componentNames =
                    registryAnnotation
                        .requirePropertyValueOfType(
                            AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_NAMES,
                            List::class,
                        )
                        .filterIsInstance<String>()

                // Only functions and serializables require component docstrings.
                if (
                    filterComponentCategory !=
                        AppFunctionComponentRegistryAnnotation.Category.FUNCTION &&
                        filterComponentCategory !=
                            AppFunctionComponentRegistryAnnotation.Category.SERIALIZABLE
                ) {
                    return@flatMap componentNames
                        .map { qualifiedName ->
                            AppFunctionComponentRegistryGenerator.AppFunctionComponent(
                                qualifiedName = qualifiedName
                            )
                        }
                        .toList()
                }

                val componentDocStrings =
                    registryAnnotation
                        .requirePropertyValueOfType(
                            AppFunctionComponentRegistryAnnotation.PROPERTY_COMPONENT_DOCSTRINGS,
                            List::class,
                        )
                        .filterIsInstance<String>()

                check(componentDocStrings.size == componentNames.size) {
                    "Function's componentDocStrings must have the same size as componentNames."
                }
                return@flatMap componentNames.indices.map { index ->
                    AppFunctionComponentRegistryGenerator.AppFunctionComponent(
                        qualifiedName = componentNames[index],
                        docString = componentDocStrings[index],
                    )
                }
            }
            .toList()
    }
}
