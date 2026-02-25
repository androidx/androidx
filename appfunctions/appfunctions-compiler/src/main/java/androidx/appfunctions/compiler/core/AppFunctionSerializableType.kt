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
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_PROXY_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_PROXY_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec

/** An interface representing a type that can be serialized by AppFunctions. */
interface AppFunctionSerializableType {
    val classDeclaration: KSClassDeclaration

    val appFunctionSerializableTypeClassDeclaration: AppFunctionSerializableTypeClassDeclaration
        get() = AppFunctionSerializableTypeClassDeclaration(classDeclaration)

    /**
     * The JVM qualified name of the class being annotated with AppFunctionSerializable. For
     * example, `com.example.InnerClass$OuterClass`.
     */
    val jvmQualifiedName: String
        get() = appFunctionSerializableTypeClassDeclaration.jvmQualifiedName

    /** The generated factory ClassName. */
    val factoryClassName: ClassName
        get() =
            ClassName(
                appFunctionSerializableTypeClassDeclaration.originalClassName.packageName,
                "$${appFunctionSerializableTypeClassDeclaration.jvmClassName}Factory",
            )

    /** The generated factory variable name. */
    val factoryVariableName: String
        get() =
            "${
                appFunctionSerializableTypeClassDeclaration.jvmClassName.replace("$", "")
                    .replaceFirstChar { it -> it.lowercase() }
            }Factory"

    /** The docstring of the annotated class. */
    val docString: String
        get() =
            if (isDescribedByKDoc) {
                appFunctionSerializableTypeClassDeclaration.docString
            } else {
                ""
            }

    val isDescribedByKDoc: Boolean

    fun getDescription(sharedDataTypeDescriptionMap: Map<String, String> = mapOf()): String =
        docString.ifEmpty { sharedDataTypeDescriptionMap[jvmQualifiedName] ?: "" }

    fun validate(allowSerializableInterfaceTypes: Boolean = false): AppFunctionSerializableType

    /**
     * Finds all super types of the serializable [classDeclaration] that are annotated with the
     * `androidx.appfunctions.AppFunctionSchemaCapability` annotation.
     *
     * For example, consider the following classes:
     * ```
     * @AppFunctionSchemaCapability
     * public interface AppFunctionOpenable {
     *     public val intentToOpen: PendingIntent
     * }
     *
     * public interface OpenableResponse : AppFunctionOpenable {
     *     override val intentToOpen: PendingIntent
     * }
     *
     * @AppFunctionSerializable
     * class MySerializableClass(
     *   override val intentToOpen: PendingIntent
     * ) : OpenableResponse
     * ```
     *
     * This method will return the [KSClassDeclaration] of `AppFunctionOpenable` since it is a super
     * type of `MySerializableClass` and is annotated with the
     * `androidx.appfunctions.AppFunctionSchemaCapability` annotation.
     *
     * @return a set of [KSClassDeclaration] for all super types of the [classDeclaration] that are
     *   annotated with `androidx.appfunctions.AppFunctionSchemaCapability`.
     */
    fun findSuperTypesWithCapabilityAnnotation(): Set<KSClassDeclaration> = buildSet {
        val unvisitedSuperTypes: MutableList<KSTypeReference> =
            classDeclaration.superTypes.toMutableList()

        while (!unvisitedSuperTypes.isEmpty()) {
            val superTypeClassDeclaration =
                unvisitedSuperTypes.removeLast().resolve().declaration as KSClassDeclaration
            if (
                superTypeClassDeclaration.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionSchemaCapability.CLASS_NAME
                ) != null
            ) {
                add(superTypeClassDeclaration)
            }
            if (
                superTypeClassDeclaration.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
                ) == null
            ) {
                // Only consider non serializable super types since serializable super types
                // are already handled separately
                unvisitedSuperTypes.addAll(superTypeClassDeclaration.superTypes)
            }
        }
    }

    /**
     * Finds all super types of the serializable [classDeclaration] that are annotated with the
     * `androidx.appfunctions.AppFunctionSerializable` annotation.
     *
     * For example, consider the following classes:
     * ```
     * @AppFunctionSerializable
     * open class Address (
     *     open val street: String,
     *     open val city: String,
     *     open val state: String,
     *     open val zipCode: String,
     * )
     *
     * @AppFunctionSerializable
     * class MySerializableClass(
     *     override val street: String,
     *     override val city: String,
     *     override val state: String,
     *     override val zipCode: String,
     * ) : Address
     * ```
     *
     * This method will return the [KSClassDeclaration] of `Address` since it is a super type of
     * `MySerializableClass` and is annotated with the
     * `androidx.appfunctions.AppFunctionSerializable` annotation.
     *
     * @return a set of [KSClassDeclaration] for all super types of the [classDeclaration] that are
     *   annotated with `androidx.appfunctions.AppFunctionSerializable`.
     */
    fun findSuperTypesWithSerializableAnnotation(): Set<KSClassDeclaration> =
        classDeclaration.superTypes
            .map { it.resolve().declaration as KSClassDeclaration }
            .filter {
                it.annotations.findAnnotation(AppFunctionSerializableAnnotation.CLASS_NAME) !=
                    null && !it.modifiers.contains(Modifier.SEALED)
            }
            .toSet()

    /** Returns the annotated class's properties as defined in its primary constructor. */
    fun getProperties(
        sharedDataTypeDescriptionMap: Map<String, String> = emptyMap()
    ): List<AppFunctionPropertyDeclaration> {
        val primaryConstructorProperties =
            checkNotNull(classDeclaration.primaryConstructor).parameters

        val allProperties: Map<String, KSPropertyDeclaration> =
            classDeclaration.getAllProperties().associateBy { (it.simpleName.asString()) }

        return primaryConstructorProperties.mapNotNull { valueParameter ->
            allProperties[valueParameter.name?.asString()]?.let {
                AppFunctionPropertyDeclaration(
                    property = it,
                    isDescribedByKDoc = isDescribedByKDoc,
                    isRequired = !valueParameter.hasDefault,
                    sharedDataTypeDescriptionMap = sharedDataTypeDescriptionMap,
                )
            }
        }
    }

    /** Returns the properties that have @AppFunctionSerializable class types. */
    fun getSerializablePropertyTypeReferences(): Set<AppFunctionTypeReference> =
        getProperties()
            .filterNot { it.isGenericType }
            .map { property -> AppFunctionTypeReference(property.type) }
            .filter { afType ->
                afType.isOfTypeCategory(SERIALIZABLE_SINGULAR) ||
                    afType.isOfTypeCategory(SERIALIZABLE_LIST)
            }
            .toSet()

    /** Returns the properties that have @AppFunctionSerializableProxy class types. */
    fun getSerializableProxyPropertyTypeReferences(): Set<AppFunctionTypeReference> =
        getProperties()
            .filterNot { it.isGenericType }
            .map { it -> AppFunctionTypeReference(it.type) }
            .filter { afType ->
                afType.isOfTypeCategory(SERIALIZABLE_PROXY_SINGULAR) ||
                    afType.isOfTypeCategory(SERIALIZABLE_PROXY_LIST)
            }
            .toSet()

    /**
     * Returns the set of source files that contain the definition of [classDeclaration] and
     * all @AppFunctionSerializable classes directly reachable through its fields. This method
     * differs from [getTransitiveSerializableSourceFiles] by excluding transitively
     * nested @AppFunctionSerializable classes.
     */
    fun getSerializableSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        classDeclaration.containingFile?.let { sourceFileSet.add(it) }
        for (serializableAfType in getSerializablePropertyTypeReferences()) {
            val appFunctionSerializableDefinition =
                serializableAfType.selfOrItemTypeReference.resolve().declaration
                    as KSClassDeclaration
            appFunctionSerializableDefinition.containingFile?.let { sourceFileSet.add(it) }
        }
        return sourceFileSet
    }

    /**
     * Returns the set of source files that contain the definition of [classDeclaration] and
     * all @AppFunctionSerializable classes transitively reachable through its fields or nested
     * classes.
     */
    fun getTransitiveSerializableSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        val visitedSerializableSet: MutableSet<ClassName> = mutableSetOf()

        // Add the file containing the AppFunctionSerializable class definition immediately it's
        // seen
        classDeclaration.containingFile?.let { sourceFileSet.add(it) }
        visitedSerializableSet.add(appFunctionSerializableTypeClassDeclaration.originalClassName)
        traverseSerializableClassSourceFiles(sourceFileSet, visitedSerializableSet)
        return sourceFileSet
    }

    private fun traverseSerializableClassSourceFiles(
        sourceFileSet: MutableSet<KSFile>,
        visitedSerializableSet: MutableSet<ClassName>,
    ) {
        for (serializableAfType in getSerializablePropertyTypeReferences()) {
            val appFunctionSerializableDefinition =
                serializableAfType.selfOrItemTypeReference.resolve().declaration
                    as KSClassDeclaration
            // Skip serializable that have been seen before
            if (
                visitedSerializableSet.contains(
                    appFunctionSerializableTypeClassDeclaration.originalClassName
                )
            ) {
                continue
            }
            // Process newly found serializable
            sourceFileSet.addAll(
                AnnotatedAppFunctionSerializable(appFunctionSerializableDefinition)
                    .parameterizedBy(serializableAfType.selfOrItemTypeReference.resolve().arguments)
                    .getTransitiveSerializableSourceFiles()
            )
        }
    }

    /**
     * Returns a [FactoryCodeBuilder] that can be used to generate an implementation of
     * `androidx.appfunctions.AppFunctionSerializableFactory`.
     */
    fun getFactoryCodeBuilder(
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies
    ): FactoryCodeBuilder

    /**
     * Interface for generating an implementation of
     * `androidx.appfunctions.AppFunctionSerializableFactory`.
     */
    interface FactoryCodeBuilder {
        // TODO: b/410764334 - Consider abstracting FileSpec builder logic
        fun buildAppFunctionSerializableFactoryClass(): FileSpec
    }

    companion object {
        /**
         * Creates a new [AppFunctionSerializableType] from the given [classDeclaration].
         *
         * If the [typeArguments] are provided, and the [classDeclaration] is a
         * [AnnotatedAppFunctionSerializable] then it will be parameterized accordingly
         */
        fun create(
            classDeclaration: KSClassDeclaration,
            typeArguments: List<KSTypeArgument> = emptyList(),
        ): AppFunctionSerializableType =
            when {
                isAnnotatedWithAppFunctionSerializableInterface(classDeclaration) ->
                    AnnotatedAppFunctionSerializableInterface(classDeclaration)

                isAnnotatedWithAppFunctionSerializableProxy(classDeclaration) ->
                    AnnotatedAppFunctionSerializableProxy(classDeclaration)

                isAnnotatedWithAppFunctionSerializable(classDeclaration) &&
                    isOneOfType(classDeclaration) ->
                    AnnotatedOneOfAppFunctionSerializable(classDeclaration)

                isAnnotatedWithAppFunctionSerializable(classDeclaration) ->
                    AnnotatedAppFunctionSerializable(classDeclaration)
                        .parameterizedBy(typeArguments)

                else ->
                    throw ProcessingException(
                        "Invalid AppFunctionSerializable type.",
                        classDeclaration,
                    )
            }

        fun isAnnotatedWithAppFunctionSerializable(classDeclaration: KSClassDeclaration): Boolean =
            classDeclaration.annotations.findAnnotation(
                AppFunctionSerializableAnnotation.CLASS_NAME
            ) != null

        fun isAnnotatedWithAppFunctionSerializableProxy(
            classDeclaration: KSClassDeclaration
        ): Boolean =
            classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSerializableProxyAnnotation.CLASS_NAME
            ) != null

        fun isAnnotatedWithAppFunctionSerializableInterface(
            classDeclaration: KSClassDeclaration
        ): Boolean =
            classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSerializableInterfaceAnnotation.CLASS_NAME
            ) != null

        fun isOneOfType(classDeclaration: KSClassDeclaration) =
            classDeclaration.modifiers.contains(Modifier.SEALED)
    }
}
