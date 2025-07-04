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

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSchemaDefinitionAnnotation
import androidx.appfunctions.compiler.core.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/** Represents the annotated @AppFunctionSchemaDefinition. */
class AnnotatedAppFunctionSchemaDefinition(private val classDeclaration: KSClassDeclaration) {
    private val schemaFunctionDeclaration: KSFunctionDeclaration by lazy {
        classDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>().singleOrNull()
            ?: throw ProcessingException(
                "The @AppFunctionSchemaDefinition should have exactly one function declaration",
                classDeclaration,
            )
    }

    /** The qualified name of the @AppFunctionSchemaDefinition */
    val qualifiedName: String by lazy { classDeclaration.ensureQualifiedName() }

    /** Gets all annotated nodes. */
    fun getAllAnnotated(): List<KSAnnotated> {
        return listOf(classDeclaration)
    }

    /** Gets the source files of @AppFunctionSchemaDefinition */
    fun getSourceFiles(): Set<KSFile> {
        return buildSet {
            val containingFile = classDeclaration.containingFile
            if (containingFile != null) {
                add(containingFile)
            }
        }
    }

    /** Creates [CompileTimeAppFunctionMetadata] from @AppFunctionSchemaDefinition. */
    fun createAppFunctionMetadata(
        resolvedAnnotatedSerializableProxies:
            AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
    ): CompileTimeAppFunctionMetadata {
        val metadataCreatorHelper = AppFunctionMetadataCreatorHelper()
        val annotation =
            classDeclaration.annotations.findAnnotation(
                AppFunctionSchemaDefinitionAnnotation.CLASS_NAME
            )
                ?: throw ProcessingException(
                    "Class not annotated with @AppFunctionSchemaDefinition",
                    classDeclaration,
                )
        val annotationProperties =
            metadataCreatorHelper.computeAppFunctionAnnotationProperties(
                schemaDefinitionAnnotation = annotation
            )

        val sharedDataTypeMap: MutableMap<String, AppFunctionDataTypeMetadata> = mutableMapOf()
        val seenDataTypeQualifiers: MutableSet<String> = mutableSetOf()

        val parameterTypeMetadataList =
            metadataCreatorHelper.buildParameterTypeMetadataList(
                parameters = schemaFunctionDeclaration.parameters,
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeMap = sharedDataTypeMap,
                seenDataTypeQualifiers = seenDataTypeQualifiers,
                allowSerializableInterfaceTypes = true,
            )
        val responseTypeMetadata =
            metadataCreatorHelper.buildResponseTypeMetadata(
                returnType =
                    checkNotNull(schemaFunctionDeclaration.returnType)
                        .resolveSelfOrUpperBoundType(),
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeMap = sharedDataTypeMap,
                seenDataTypeQualifiers = seenDataTypeQualifiers,
                allowSerializableInterfaceTypes = true,
            )

        return CompileTimeAppFunctionMetadata(
            id =
                "${annotationProperties.schemaCategory}/${annotationProperties.schemaName}/${annotationProperties.schemaVersion}",
            isEnabledByDefault = true,
            schema = annotationProperties.getAppFunctionSchemaMetadata(),
            parameters = parameterTypeMetadataList,
            response = AppFunctionResponseMetadata(responseTypeMetadata),
            components = AppFunctionComponentsMetadata(sharedDataTypeMap),
        )
    }
}
