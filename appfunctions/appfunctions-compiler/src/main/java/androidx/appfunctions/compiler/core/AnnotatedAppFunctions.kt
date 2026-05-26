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
import androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName

/**
 * Represents a collection of functions within a specific class that are annotated as app functions.
 */
data class AnnotatedAppFunctions(
    /** The [KSClassDeclaration] of the class that contains the annotated app functions. */
    val classDeclaration: KSClassDeclaration,
    /** The list of [AnnotatedAppFunction] that are annotated as app function. */
    val appFunctions: List<AnnotatedAppFunction>,
) {
    // TODO(b/463909015): Remove this once service module is deleted
    /** Returns true if any function contains the base annotation. */
    val hasBaseAnnotation: Boolean by lazy { appFunctions.any { it.isBaseAnnotation } }

    /** Gets all annotated nodes. */
    fun getAllAnnotated(): List<KSAnnotated> {
        return buildList {
            // Only functions are annotated.
            for (appFunction in appFunctions) {
                add(appFunction.appFunctionDeclaration)
            }
        }
    }

    /**
     * Validates if the AppFunction implementation is valid.
     *
     * @throws SymbolNotReadyException if any related nodes are not ready for processing yet.
     */
    fun validate(skipFirstParameterValidation: Boolean = false): AnnotatedAppFunctions {
        for (appFunction in appFunctions) {
            appFunction.validate(skipFirstParameterValidation)
        }
        return this
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

        for (appFunction in appFunctions) {
            sourceFileSet.addAll(appFunction.getSourceFiles())
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
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        sharedDataTypeDescriptionMap: Map<String, String> = mapOf(),
    ): List<CompileTimeAppFunctionMetadata> {
        return appFunctions.map { appFunction ->
            appFunction.createAppFunctionMetadata(
                enclosingClass = classDeclaration,
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sharedDataTypeDescriptionMap = sharedDataTypeDescriptionMap,
            )
        }
    }
}
