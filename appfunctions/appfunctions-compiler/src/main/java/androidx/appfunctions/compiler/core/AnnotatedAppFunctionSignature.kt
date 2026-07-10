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
import androidx.appfunctions.compiler.core.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
                Int::class,
            )

        when (declaredScope) {
            AppFunctionMetadataClass.SCOPE_GLOBAL -> "global"
            AppFunctionMetadataClass.SCOPE_ACTIVITY -> "activity"
            else ->
                throw ProcessingException(
                    "Invalid scope: \"$declaredScope\". Supported scopes are \"${AppFunctionMetadataClass.SCOPE_GLOBAL}\" and \"${AppFunctionMetadataClass.SCOPE_ACTIVITY}\".",
                    classDeclaration,
                )
        }
    }

    /** Whether the app function is described by KDoc. */
    val isDescribedByKDoc: Boolean by lazy {
        val appFunctionSignatureAnnotation =
            classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSignatureAnnotation.CLASS_NAME
            )
        appFunctionSignatureAnnotation?.requirePropertyValueOfType(
            IntrospectionHelper.AppFunctionSignatureAnnotation.PROPERTY_IS_DESCRIBED_BY_KDOC,
            Boolean::class,
        ) ?: false
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

        val rawKDoc = getRawKDoc()
        val functionDescription = functionDeclaration.getFunctionDescription(rawKDoc)

        val parameterTypeMetadataList =
            metadataCreatorHelper.buildParameterTypeMetadataList(
                parameters = functionDeclaration.parameters,
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeMap = sharedDataTypeMap,
                seenDataTypeQualifiers = seenDataTypeQualifiers,
                parameterDescriptionMap = getParamDescriptionsFromKDoc(rawKDoc),
            )
        val responseTypeMetadata =
            metadataCreatorHelper.buildResponseTypeMetadata(
                returnType = checkNotNull(functionDeclaration.returnType),
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeMap = sharedDataTypeMap,
                seenDataTypeQualifiers = seenDataTypeQualifiers,
                functionAnnotations = functionDeclaration.annotations,
            )
        return CompileTimeAppFunctionMetadata(
            id = getAppFunctionIdentifier(functionDeclaration),
            isEnabledByDefault = null,
            schema = null,
            parameters = parameterTypeMetadataList,
            response =
                AppFunctionResponseMetadata(
                    valueType = responseTypeMetadata,
                    description = functionDeclaration.getResponseDescription(rawKDoc),
                ),
            components = AppFunctionComponentsMetadata(dataTypes = sharedDataTypeMap),
            description = functionDescription,
            deprecation = null,
            scope = scope,
        )
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
                    parameterTypeReference
                        .getAnnotatedAppFunctionSerializable()
                        .getTransitiveSerializableSourceFiles()
                )
            }
        }

        val returnTypeReference =
            AppFunctionTypeReference(checkNotNull(appFunctionDeclaration.returnType))
        if (returnTypeReference.typeOrItemTypeIsAppFunctionSerializable()) {
            sourceFileSet.addAll(
                returnTypeReference
                    .getAnnotatedAppFunctionSerializable()
                    .getTransitiveSerializableSourceFiles()
            )
        }
        return sourceFileSet
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

    private fun getRawKDoc(): String {
        return if (isDescribedByKDoc) {
            appFunctionDeclaration.docString ?: ""
        } else {
            ""
        }
    }
}
