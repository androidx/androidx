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
import androidx.appfunctions.compiler.core.metadata.AppFunctionDeprecationMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

/** Represents an individual function that is annotated as an app function. */
data class AnnotatedAppFunction(
    /** The [KSFunctionDeclaration] that is annotated as app function. */
    val appFunctionDeclaration: KSFunctionDeclaration,
    /** Optional docstring from the component registry. */
    private val docString: String? = null,
) {
    /** Returns true if the function is opted into description by KDoc. */
    val isDescribedByKDoc: Boolean by lazy {
        AppFunctionMetadataCreatorHelper()
            .computeAppFunctionAnnotationProperties(appFunctionDeclaration)
            .isDescribedByKDoc ?: false
    }

    // TODO(b/463909015): Remove this once service module is deleted
    /** Returns true if the function is annotated with the base @AppFunction annotation. */
    val isBaseAnnotation: Boolean by lazy {
        appFunctionDeclaration.annotations.findAnnotation(AppFunctionAnnotation.CLASS_NAME_BASE) !=
            null
    }

    /** Returns true if the function is deprecated. */
    val isDeprecated: Boolean by lazy { appFunctionDeclaration.getDeprecationMetadata() != null }

    /**
     * Validates if the AppFunction implementation is valid.
     *
     * @throws SymbolNotReadyException if any related nodes are not ready for processing yet.
     */
    fun validate(skipFirstParameterValidation: Boolean = false): AnnotatedAppFunction {
        val hasServiceAnnotation =
            appFunctionDeclaration.annotations.findAnnotation(AppFunctionAnnotation.CLASS_NAME) !=
                null
        val hasBaseAnnotation =
            appFunctionDeclaration.annotations.findAnnotation(
                AppFunctionAnnotation.CLASS_NAME_BASE
            ) != null
        if (hasServiceAnnotation && hasBaseAnnotation) {
            throw ProcessingException(
                "An app function cannot be annotated with both @androidx.appfunctions.AppFunction and @androidx.appfunctions.service.AppFunction",
                appFunctionDeclaration,
            )
        }

        for (parameter in appFunctionDeclaration.parameters) {
            if (!parameter.validate()) {
                throw SymbolNotReadyException(
                    "AppFunction parameter ($parameter) not ready for processing yet",
                    appFunctionDeclaration,
                )
            }
        }

        if (appFunctionDeclaration.returnType?.validate() == false) {
            throw SymbolNotReadyException(
                "AppFunction return type not ready for processing yet",
                appFunctionDeclaration,
            )
        }
        if (!skipFirstParameterValidation) {
            validateFirstParameter()
        }
        validateParameterTypes()
        return this
    }

    /**
     * Gets the identifier of the app function.
     *
     * The format of the identifier is `packageName.className#methodName`.
     */
    fun getAppFunctionIdentifier(enclosingClass: KSClassDeclaration): String {
        val fullClassName = enclosingClass.toClassName()
        val methodName = appFunctionDeclaration.simpleName.asString()
        return "$fullClassName#$methodName"
    }

    /** Creates a [CompileTimeAppFunctionMetadata] instance for the app function. */
    fun createAppFunctionMetadata(
        enclosingClass: KSClassDeclaration,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        sharedDataTypeDescriptionMap: Map<String, String> = mapOf(),
    ): CompileTimeAppFunctionMetadata {
        val metadataCreatorHelper = AppFunctionMetadataCreatorHelper(sharedDataTypeDescriptionMap)
        // Defining the shared types locally for this iteration is to isolate the components
        // used per function. This is done with the expectation that they can be globally
        // merged without encountering mismatching datatype metadata for the same object key.
        val sharedDataTypeMap: MutableMap<String, AppFunctionDataTypeMetadata> = mutableMapOf()
        val seenDataTypeQualifiers: MutableSet<String> = mutableSetOf()

        val appFunctionAnnotationProperties =
            metadataCreatorHelper.computeAppFunctionAnnotationProperties(appFunctionDeclaration)
        val functionDescription = getFunctionDescription(appFunctionAnnotationProperties)
        val parameterTypeMetadataList =
            metadataCreatorHelper.buildParameterTypeMetadataList(
                parameters = appFunctionDeclaration.parameters,
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeMap = sharedDataTypeMap,
                seenDataTypeQualifiers = seenDataTypeQualifiers,
                parameterDescriptionMap = getParamDescriptionsFromKDoc(functionDescription),
            )
        val responseTypeMetadata =
            metadataCreatorHelper.buildResponseTypeMetadata(
                returnType = checkNotNull(appFunctionDeclaration.returnType),
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeMap = sharedDataTypeMap,
                seenDataTypeQualifiers = seenDataTypeQualifiers,
                functionAnnotations = appFunctionDeclaration.annotations,
            )
        val deprecationMetadata = appFunctionDeclaration.getDeprecationMetadata()

        return CompileTimeAppFunctionMetadata(
            id = getAppFunctionIdentifier(enclosingClass),
            isEnabledByDefault = checkNotNull(appFunctionAnnotationProperties.isEnabledByDefault),
            schema = appFunctionAnnotationProperties.getAppFunctionSchemaMetadata(),
            parameters = parameterTypeMetadataList,
            response =
                AppFunctionResponseMetadata(
                    valueType = responseTypeMetadata,
                    description = getResponseDescriptionFromKDoc(functionDescription),
                ),
            components = AppFunctionComponentsMetadata(dataTypes = sharedDataTypeMap),
            description = sanitizeKDoc(functionDescription),
            deprecation = deprecationMetadata,
        )
    }

    /**
     * Returns the set of files that need to be processed to obtain the complete information about
     * the app function.
     *
     * This includes the file containing the function declaration, the class file containing the
     * schema definitions, and the class files containing the AppFunctionSerializable classes used
     * in the function parameters and return type.
     */
    fun getSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()

        // Add the class file containing the function declaration
        appFunctionDeclaration.containingFile?.let { sourceFileSet.add(it) }

        // Add the class file containing the schema definitions
        val rootAppFunctionSchemaInterface =
            findRootAppFunctionSchemaInterface(appFunctionDeclaration)
        rootAppFunctionSchemaInterface?.containingFile?.let { sourceFileSet.add(it) }

        for (ksValueParameter in appFunctionDeclaration.parameters) {
            if (ksValueParameter.type.isOfType(AppFunctionContextClass.CLASS_NAME)) {
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
            AppFunctionTypeReference(checkNotNull(appFunctionDeclaration.returnType))
        if (returnTypeReference.typeOrItemTypeIsAppFunctionSerializable()) {
            sourceFileSet.addAll(
                getAnnotatedAppFunctionSerializable(returnTypeReference)
                    .getTransitiveSerializableSourceFiles()
            )
        }

        return sourceFileSet
    }

    private fun validateFirstParameter() {
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

    private fun validateParameterTypes() {
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

    private fun AppFunctionMetadataCreatorHelper.computeAppFunctionAnnotationProperties(
        functionDeclaration: KSFunctionDeclaration
    ): AppFunctionMetadataCreatorHelper.AppFunctionAnnotationProperties {
        val appFunctionAnnotation =
            functionDeclaration.annotations.findAnnotation(AppFunctionAnnotation.CLASS_NAME)
                ?: functionDeclaration.annotations.findAnnotation(
                    AppFunctionAnnotation.CLASS_NAME_BASE
                )
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

    private fun getFunctionDescription(
        appFunctionAnnotationProperties:
            AppFunctionMetadataCreatorHelper.AppFunctionAnnotationProperties
    ): String {
        return if (appFunctionAnnotationProperties.isDescribedByKDoc == true) {
            this@AnnotatedAppFunction.docString ?: ""
        } else {
            ""
        }
    }

    private fun KSDeclaration.getDeprecationMetadata(): AppFunctionDeprecationMetadata? {
        val annotation =
            annotations.findAnnotation(IntrospectionHelper.DeprecatedAnnotation.CLASS_NAME)
                ?: return null
        val message =
            annotation.requirePropertyValueOfType(
                IntrospectionHelper.DeprecatedAnnotation.PROPERTY_MESSAGE,
                String::class,
            )
        return AppFunctionDeprecationMetadata(message)
    }

    private fun getAnnotatedAppFunctionSerializable(
        appFunctionTypeReference: AppFunctionTypeReference
    ): AppFunctionSerializableType {
        val appFunctionSerializableKSType =
            appFunctionTypeReference.selfOrItemTypeReference.resolve()
        return AppFunctionSerializableType.create(
            classDeclaration =
                appFunctionSerializableKSType.declaration as? KSClassDeclaration
                    ?: throw ProcessingException(
                        "Only classes/interfaces should be annotated with @AppFunctionSerializable",
                        appFunctionSerializableKSType.declaration,
                    ),
            typeArguments = appFunctionSerializableKSType.arguments,
        )
    }

    private fun AppFunctionTypeReference.typeOrItemTypeIsAppFunctionSerializable(): Boolean {
        return this.isOfTypeCategory(SERIALIZABLE_SINGULAR) ||
            this.isOfTypeCategory(SERIALIZABLE_LIST)
    }
}
