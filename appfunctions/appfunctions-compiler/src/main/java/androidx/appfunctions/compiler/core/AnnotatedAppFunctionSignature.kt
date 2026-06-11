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
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionMetadataClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.DeprecatedAnnotation
import androidx.appfunctions.compiler.core.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDeprecationMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName

/** Represents a class or interface annotated with @AppFunctionSignature. */
data class AnnotatedAppFunctionSignature(
    /** The [KSClassDeclaration] of the class that contains the annotated app function signature. */
    val classDeclaration: KSClassDeclaration
) {
    /**
     * The [KSFunctionDeclaration] that is part of this signature.
     *
     * This is the single abstract function declared in the functional interface.
     */
    val appFunctionDeclaration: KSFunctionDeclaration
        get() = classDeclaration.getDeclaredFunctions().single { it.isAbstract }

    /** The custom name of the app function XML file. */
    val appFunctionXmlFileName: String by lazy {
        val appFunctionSignatureAnnotation =
            classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSignatureAnnotation.CLASS_NAME
            )
                ?: throw ProcessingException(
                    "AppFunctionSignature annotation not found on class ${classDeclaration.simpleName.asString()}",
                    classDeclaration,
                )
        val value =
            appFunctionSignatureAnnotation.requirePropertyValueOfType(
                IntrospectionHelper.AppFunctionSignatureAnnotation.PROPERTY_XML_FILE_NAME,
                String::class,
            )
        if (value.isBlank()) {
            throw ProcessingException("appFunctionXmlFileName cannot be empty", classDeclaration)
        }
        value
    }

    /** The lifecycle scope of the app function. */
    val scope: String by lazy {
        val appFunctionSignatureAnnotation =
            classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSignatureAnnotation.CLASS_NAME
            )
                ?: throw ProcessingException(
                    "AppFunctionSignature annotation not found on class ${classDeclaration.simpleName.asString()}",
                    classDeclaration,
                )
        val declaredScope =
            appFunctionSignatureAnnotation.requirePropertyValueOfType(
                IntrospectionHelper.AppFunctionSignatureAnnotation.PROPERTY_SCOPE,
                String::class,
            )

        if (declaredScope !in SUPPORTED_SCOPES) {
            throw ProcessingException(
                "Invalid scope: \"$declaredScope\". Supported scopes are \"${AppFunctionMetadataClass.SCOPE_GLOBAL}\" and \"${AppFunctionMetadataClass.SCOPE_ACTIVITY}\".",
                classDeclaration,
            )
        }
        declaredScope
    }

    /**
     * Creates a [CompileTimeAppFunctionMetadata] instance for the app function defined in this
     * class.
     */
    fun createAppFunctionMetadata(
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        sharedDataTypeDescriptionMap: Map<String, String> = mapOf(),
    ): CompileTimeAppFunctionMetadata {
        val metadataCreatorHelper = AppFunctionMetadataCreatorHelper(sharedDataTypeDescriptionMap)
        val functionDeclaration = appFunctionDeclaration
        val sharedDataTypeMap: MutableMap<String, AppFunctionDataTypeMetadata> = mutableMapOf()
        val seenDataTypeQualifiers: MutableSet<String> = mutableSetOf()

        // TODO: b/501032667 - Extract descriptions from KDoc
        val functionDescription = ""

        val parameterTypeMetadataList =
            metadataCreatorHelper.buildParameterTypeMetadataList(
                parameters = functionDeclaration.parameters,
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeMap = sharedDataTypeMap,
                seenDataTypeQualifiers = seenDataTypeQualifiers,
                // TODO: b/501032667 - Extract parameter descriptions from KDoc
                parameterDescriptionMap = emptyMap(),
            )
        val responseTypeMetadata =
            metadataCreatorHelper.buildResponseTypeMetadata(
                returnType = checkNotNull(functionDeclaration.returnType),
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeMap = sharedDataTypeMap,
                seenDataTypeQualifiers = seenDataTypeQualifiers,
                functionAnnotations = functionDeclaration.annotations,
            )
        val deprecationMetadata = functionDeclaration.getDeprecationMetadata()

        return CompileTimeAppFunctionMetadata(
            id = getAppFunctionIdentifier(functionDeclaration),
            isEnabledByDefault = false,
            schema = null,
            parameters = parameterTypeMetadataList,
            response =
                AppFunctionResponseMetadata(
                    valueType = responseTypeMetadata,
                    // TODO: b/501032667 - Extract response description from KDoc
                    description = "",
                ),
            components = AppFunctionComponentsMetadata(dataTypes = sharedDataTypeMap),
            description = functionDescription,
            deprecation = deprecationMetadata,
            scope = scope,
        )
    }

    private fun KSDeclaration.getDeprecationMetadata(): AppFunctionDeprecationMetadata? {
        val annotation = annotations.findAnnotation(DeprecatedAnnotation.CLASS_NAME) ?: return null
        val message =
            annotation.requirePropertyValueOfType(
                DeprecatedAnnotation.PROPERTY_MESSAGE,
                String::class,
            )
        return AppFunctionDeprecationMetadata(message)
    }

    /**
     * Returns the set of files that need to be processed to obtain the complete information about
     * the app functions defined in this class.
     */
    fun getSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()

        // Add the class file containing the function declarations
        classDeclaration.containingFile?.let { sourceFileSet.add(it) }

        // Traverse the function's parameters to obtain the relevant AppFunctionSerializable
        // class files
        for (ksValueParameter in appFunctionDeclaration.parameters) {
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
        return this.isOfTypeCategory(
            AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
        ) ||
            this.isOfTypeCategory(
                AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_LIST
            )
    }

    /** Validates if the AppFunction signature is valid. */
    fun validate(): AnnotatedAppFunctionSignature {
        if (!classDeclaration.validate()) {
            throw SymbolNotReadyException(
                "AppFunctionSignature class (${classDeclaration.simpleName.asString()}) not ready for processing yet",
                classDeclaration,
            )
        }

        if (
            classDeclaration.classKind != ClassKind.INTERFACE ||
                !classDeclaration.modifiers.contains(Modifier.FUN)
        ) {
            throw ProcessingException(
                "Only functional interfaces (fun interface) can be annotated with @AppFunctionSignature",
                classDeclaration,
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
        return this
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

    /** Gets the [classDeclaration]'s [ClassName]. */
    fun getEnclosingClassName(): ClassName {
        return classDeclaration.toClassName()
    }

    companion object {
        private val SUPPORTED_SCOPES =
            setOf(AppFunctionMetadataClass.SCOPE_GLOBAL, AppFunctionMetadataClass.SCOPE_ACTIVITY)
    }
}
