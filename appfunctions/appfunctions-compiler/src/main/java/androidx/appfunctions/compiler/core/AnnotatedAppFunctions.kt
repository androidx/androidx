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

import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.Companion.SUPPORTED_TYPES_STRING
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.Companion.isAllowToBeOptional
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.Companion.isSupportedType
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionContextClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSchemaDefinitionAnnotation
import androidx.appfunctions.compiler.core.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName

/**
 * Represents a collection of functions within a specific class that are annotated as app functions.
 */
data class AnnotatedAppFunctions(
    /** The [KSClassDeclaration] of the class that contains the annotated app functions. */
    val classDeclaration: KSClassDeclaration,
    /** The list of [KSFunctionDeclaration] that are annotated as app function. */
    val appFunctionDeclarations: List<KSFunctionDeclaration>,
) {
    /** Gets all annotated nodes. */
    fun getAllAnnotated(): List<KSAnnotated> {
        return buildList {
            // Only functions are annotated.
            for (appFunctionDeclaration in appFunctionDeclarations) {
                add(appFunctionDeclaration)
            }
        }
    }

    // TODO(b/410746104): Re-evaluate the validation pipeline.
    /**
     * Validates if the AppFunction implementation is valid.
     *
     * @throws SymbolNotReadyException if any related nodes are not ready for processing yet.
     */
    fun validate(): AnnotatedAppFunctions {
        if (!classDeclaration.validate()) {
            throw SymbolNotReadyException(
                "AppFunction enclosing class not ready for processing yet",
                classDeclaration,
            )
        }
        for (appFunction in appFunctionDeclarations) {
            if (!appFunction.validate()) {
                throw SymbolNotReadyException(
                    "AppFunction method not ready for processing yet",
                    appFunction,
                )
            }
        }
        validateFirstParameter()
        validateParameterTypes()
        return this
    }

    private fun validateFirstParameter() {
        for (appFunctionDeclaration in appFunctionDeclarations) {
            val firstParam = appFunctionDeclaration.parameters.firstOrNull()
            if (firstParam == null) {
                throw ProcessingException(
                    "The first parameter of an app function must be " +
                        "${AppFunctionContextClass.CLASS_NAME}",
                    appFunctionDeclaration,
                )
            }
            if (!firstParam.type.isOfType(AppFunctionContextClass.CLASS_NAME)) {
                throw ProcessingException(
                    "The first parameter of an app function must be " +
                        "${AppFunctionContextClass.CLASS_NAME}",
                    firstParam,
                )
            }
        }
    }

    private fun validateParameterTypes() {
        for (appFunctionDeclaration in appFunctionDeclarations) {
            for ((paramIndex, ksValueParameter) in appFunctionDeclaration.parameters.withIndex()) {
                if (paramIndex == 0) {
                    // Skip the first parameter which is always the `AppFunctionContext`.
                    continue
                }

                if (!isSupportedType(ksValueParameter.type)) {
                    throw ProcessingException(
                        "App function parameters must be a supported type, or a type " +
                            "annotated as @AppFunctionSerializable. See list of supported types:\n" +
                            "${
                                SUPPORTED_TYPES_STRING
                            }\n" +
                            "but found ${
                                AppFunctionTypeReference(ksValueParameter.type)
                                    .selfOrItemTypeReference.ensureQualifiedTypeName()
                                    .asString()
                            }",
                        ksValueParameter,
                    )
                }

                val isOptional = ksValueParameter.hasDefault
                if (isOptional && !isAllowToBeOptional(ksValueParameter.type)) {
                    throw ProcessingException(
                        "Type ${ksValueParameter.type.toTypeName()} cannot be optional",
                        ksValueParameter,
                    )
                }
            }
        }
    }

    /**
     * Gets the identifier of an app functions.
     *
     * The format of the identifier is `packageName.className#methodName`.
     */
    fun getAppFunctionIdentifier(functionDeclaration: KSFunctionDeclaration): String {
        val fullClassName = classDeclaration.toClassName()
        val methodName = functionDeclaration.simpleName.asString()
        return "$fullClassName#${methodName}"
    }

    /**
     * Returns the set of files that need to be processed to obtain the complete information about
     * the app functions defined in this class.
     *
     * This includes the class file containing the function declarations, the class file containing
     * the schema definitions, and the class files containing the AppFunctionSerializable classes
     * used in the function parameters.
     */
    fun getSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()

        // Add the class file containing the function declarations
        classDeclaration.containingFile?.let { sourceFileSet.add(it) }

        for (functionDeclaration in appFunctionDeclarations) {
            // Add the class file containing the schema definitions
            val rootAppFunctionSchemaInterface =
                findRootAppFunctionSchemaInterface(functionDeclaration)
            rootAppFunctionSchemaInterface?.containingFile?.let { sourceFileSet.add(it) }

            // Traverse each functions parameter to obtain the relevant AppFunctionSerializable
            // class files
            for ((paramIndex, ksValueParameter) in functionDeclaration.parameters.withIndex()) {
                if (paramIndex == 0) {
                    // Skip the first parameter which is always the `AppFunctionContext`.
                    continue
                }
                val parameterTypeReference = AppFunctionTypeReference(ksValueParameter.type)
                if (parameterTypeReference.typeOrItemTypeIsAppFunctionSerializable()) {
                    sourceFileSet.addAll(
                        getAnnotatedAppFunctionSerializable(parameterTypeReference)
                            .getTransitiveSerializableSourceFiles()
                    )
                }
            }

            val returnTypeReference =
                AppFunctionTypeReference(checkNotNull(functionDeclaration.returnType))
            if (returnTypeReference.typeOrItemTypeIsAppFunctionSerializable()) {
                sourceFileSet.addAll(
                    getAnnotatedAppFunctionSerializable(returnTypeReference)
                        .getTransitiveSerializableSourceFiles()
                )
            }
        }
        return sourceFileSet
    }

    /** Gets the [classDeclaration]'s [ClassName]. */
    fun getEnclosingClassName(): ClassName {
        return classDeclaration.toClassName()
    }

    /**
     * Creates a list of [CompileTimeAppFunctionMetadata]] instances for each of the app functions
     * defined in this class.
     */
    fun createAppFunctionMetadataList(
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies
    ): List<CompileTimeAppFunctionMetadata> {
        val metadataCreatorHelper = AppFunctionMetadataCreatorHelper()
        return appFunctionDeclarations.map { functionDeclaration ->
            // Defining the shared types locally for this iteration is to isolate the components
            // used per function. This is done with the expectation that they can be globally
            // merged without encountering mismatching datatype metadata for the same object key.
            val sharedDataTypeMap: MutableMap<String, AppFunctionDataTypeMetadata> = mutableMapOf()
            val seenDataTypeQualifiers: MutableSet<String> = mutableSetOf()

            val appFunctionAnnotationProperties =
                metadataCreatorHelper.computeAppFunctionAnnotationProperties(functionDeclaration)
            val parameterTypeMetadataList =
                metadataCreatorHelper.buildParameterTypeMetadataList(
                    parameters = functionDeclaration.parameters,
                    resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                    sharedDataTypeMap = sharedDataTypeMap,
                    seenDataTypeQualifiers = seenDataTypeQualifiers,
                )
            val responseTypeMetadata =
                metadataCreatorHelper.buildResponseTypeMetadata(
                    returnType = checkNotNull(functionDeclaration.returnType),
                    resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                    sharedDataTypeMap = sharedDataTypeMap,
                    seenDataTypeQualifiers = seenDataTypeQualifiers,
                )

            CompileTimeAppFunctionMetadata(
                id = getAppFunctionIdentifier(functionDeclaration),
                isEnabledByDefault =
                    checkNotNull(appFunctionAnnotationProperties.isEnabledByDefault),
                schema = appFunctionAnnotationProperties.getAppFunctionSchemaMetadata(),
                parameters = parameterTypeMetadataList,
                response = AppFunctionResponseMetadata(valueType = responseTypeMetadata),
                components = AppFunctionComponentsMetadata(dataTypes = sharedDataTypeMap),
                description =
                    if (appFunctionAnnotationProperties.isDescribedByKdoc == true) {
                        functionDeclaration.docString.orEmpty()
                    } else {
                        ""
                    },
            )
        }
    }

    private fun AppFunctionMetadataCreatorHelper.computeAppFunctionAnnotationProperties(
        functionDeclaration: KSFunctionDeclaration
    ): AppFunctionMetadataCreatorHelper.AppFunctionAnnotationProperties {
        val appFunctionAnnotation =
            functionDeclaration.annotations.findAnnotation(AppFunctionAnnotation.CLASS_NAME)
                ?: throw ProcessingException(
                    "Function not annotated with @AppFunction.",
                    functionDeclaration,
                )
        val rootInterfaceWithAppFunctionSchemaDefinition =
            findRootAppFunctionSchemaInterface(functionDeclaration)
        val schemaDefinitionAnnotation =
            rootInterfaceWithAppFunctionSchemaDefinition
                ?.annotations
                ?.findAnnotation(AppFunctionSchemaDefinitionAnnotation.CLASS_NAME)
        return computeAppFunctionAnnotationProperties(
            appFunctionAnnotation = appFunctionAnnotation,
            schemaDefinitionAnnotation = schemaDefinitionAnnotation,
        )
    }

    private fun findRootAppFunctionSchemaInterface(
        function: KSFunctionDeclaration
    ): KSClassDeclaration? {
        val parentDeclaration = function.parentDeclaration as? KSClassDeclaration ?: return null

        // Check if the enclosing class has the @AppFunctionSchemaDefinition
        val annotation =
            parentDeclaration.annotations.findAnnotation(
                AppFunctionSchemaDefinitionAnnotation.CLASS_NAME
            )
        if (annotation != null) {
            return parentDeclaration
        }

        val superClassFunction = (function.findOverridee() as? KSFunctionDeclaration) ?: return null
        return findRootAppFunctionSchemaInterface(superClassFunction)
    }

    private fun getAnnotatedAppFunctionSerializable(
        appFunctionTypeReference: AppFunctionTypeReference
    ): AnnotatedAppFunctionSerializable {
        val appFunctionSerializableClassDeclaration =
            appFunctionTypeReference.selfOrItemTypeReference.resolve().declaration
                as KSClassDeclaration
        return AnnotatedAppFunctionSerializable(appFunctionSerializableClassDeclaration)
            .parameterizedBy(appFunctionTypeReference.selfOrItemTypeReference.resolve().arguments)
            .validate()
    }

    private fun AppFunctionTypeReference.typeOrItemTypeIsAppFunctionSerializable(): Boolean {
        return this.isOfTypeCategory(SERIALIZABLE_SINGULAR) ||
            this.isOfTypeCategory(SERIALIZABLE_LIST)
    }
}
