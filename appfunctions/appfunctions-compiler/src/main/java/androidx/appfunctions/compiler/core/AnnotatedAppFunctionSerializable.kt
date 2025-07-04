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

import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_PROXY_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_PROXY_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

// TODO(b/410764334): Re-evaluate the abstraction layer.
/** Represents a class annotated with [androidx.appfunctions.AppFunctionSerializable]. */
open class AnnotatedAppFunctionSerializable(
    private val appFunctionSerializableClass: KSClassDeclaration
) {
    /**
     * The JVM qualified name of the class being annotated with AppFunctionSerializable. For
     * example, `com.example.InnerClass$OuterClass`.
     */
    open val jvmQualifiedName: String by lazy { appFunctionSerializableClass.getJvmQualifiedName() }

    /**
     * Returns the JVM class name which takes into account multi-layer class declarations. For
     * example, `InnerClass$OuterClass`.
     */
    open val jvmClassName: String by lazy { appFunctionSerializableClass.getJvmClassName() }

    /** The generated factory ClassName. */
    open val factoryClassName: ClassName by lazy {
        ClassName(
            originalClassName.packageName,
            "\$${appFunctionSerializableClass.getJvmClassName()}Factory",
        )
    }

    /** The name to be assigned to the serializable factory's instance. */
    open val factoryVariableName: String by lazy {
        "${ jvmClassName.replace("$", "").replaceFirstChar { it -> it.lowercase() } }Factory"
    }

    /** The super type of the class being annotated with AppFunctionSerializable */
    val superTypes: Sequence<KSTypeReference> by lazy { appFunctionSerializableClass.superTypes }

    /** The modifier of the class being annotated with AppFunctionSerializable. */
    val modifiers: Set<Modifier> by lazy { appFunctionSerializableClass.modifiers }

    /** The primary constructor if available. */
    val primaryConstructor: KSFunctionDeclaration? by lazy {
        appFunctionSerializableClass.primaryConstructor
    }

    /** The [KSNode] to which the processing error is attributed. */
    val attributeNode: KSNode by lazy { appFunctionSerializableClass }

    /** The list of [KSTypeParameter] of the AppFunctionSerializable */
    val typeParameters: List<KSTypeParameter> by lazy {
        appFunctionSerializableClass.typeParameters
    }

    /** The original [ClassName] of the AppFunctionSerializable. */
    val originalClassName: ClassName by lazy { appFunctionSerializableClass.toClassName() }

    /** The [TypeName] of the AppFunctionSerializable. */
    val typeName: TypeName by lazy {
        if (typeParameters.isEmpty()) {
            originalClassName
        } else {
            originalClassName.parameterizedBy(
                typeParameters.map(KSTypeParameter::toTypeVariableName)
            )
        }
    }

    val isDescribedByKdoc: Boolean by lazy {
        val annotation =
            appFunctionSerializableClass.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
            )
        return@lazy annotation?.requirePropertyValueOfType(
            AppFunctionAnnotation.PROPERTY_IS_DESCRIBED_BY_KDOC,
            Boolean::class,
        ) ?: false
    }

    /** A description of the AppFunctionSerializable class and its intended use. */
    val description: String by lazy {
        if (isDescribedByKdoc) {
            appFunctionSerializableClass.docString.orEmpty()
        } else {
            ""
        }
    }

    /** All the [KSDeclaration] from the AppFunctionSerializable. */
    val declarations: Sequence<KSDeclaration> by lazy { appFunctionSerializableClass.declarations }

    /**
     * Parameterize [AnnotatedAppFunctionSerializable] with [arguments].
     *
     * If [arguments] is empty, the original [AnnotatedAppFunctionSerializable] would be returned
     * directly.
     */
    fun parameterizedBy(arguments: List<KSTypeArgument>): AnnotatedAppFunctionSerializable {
        if (arguments.isEmpty()) {
            return this
        }
        return AnnotatedParameterizedAppFunctionSerializable(
            appFunctionSerializableClass,
            arguments,
        )
    }

    // TODO(b/392587953): throw an error if a property has the same name as one of the factory
    //  method parameters
    /**
     * Validates that the class annotated with AppFunctionSerializable follows app function's spec.
     *
     * @param allowSerializableInterfaceTypes Whether to allow the serializable to use serializable
     *   interface types. The @AppFunctionSerializableInterface should only be considered as a
     *   supported type when processing schema definitions.
     * @throws ProcessingException if the class does not adhere to the requirements
     */
    open fun validate(
        allowSerializableInterfaceTypes: Boolean = false
    ): AnnotatedAppFunctionSerializable {
        val validateHelper = AppFunctionSerializableValidateHelper(this)
        validateHelper.validatePrimaryConstructor()
        validateHelper.validateParameters(allowSerializableInterfaceTypes)
        return this
    }

    /**
     * Finds all super types of the serializable [appFunctionSerializableClass] that are annotated
     * with the [androidx.appfunctions.AppFunctionSchemaCapability] annotation.
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
     * [androidx.appfunctions.AppFunctionSchemaCapability] annotation.
     *
     * @return a set of [KSClassDeclaration] for all super types of the
     *   [appFunctionSerializableClass] that are annotated with
     *   [androidx.appfunctions.AppFunctionSchemaCapability].
     */
    fun findSuperTypesWithCapabilityAnnotation(): Set<KSClassDeclaration> {
        return buildSet {
            val unvisitedSuperTypes: MutableList<KSTypeReference> =
                appFunctionSerializableClass.superTypes.toMutableList()

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
    }

    /**
     * Finds all super types of the serializable [appFunctionSerializableClass] that are annotated
     * with the [androidx.appfunctions.AppFunctionSerializable] annotation.
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
     * [androidx.appfunctions.AppFunctionSerializable] annotation.
     *
     * @return a set of [KSClassDeclaration] for all super types of the
     *   [appFunctionSerializableClass] that are annotated with
     *   [androidx.appfunctions.AppFunctionSerializable].
     */
    fun findSuperTypesWithSerializableAnnotation(): Set<KSClassDeclaration> {
        return appFunctionSerializableClass.superTypes
            .map { it.resolve().declaration as KSClassDeclaration }
            .filter {
                it.annotations.findAnnotation(AppFunctionSerializableAnnotation.CLASS_NAME) != null
            }
            .toSet()
    }

    /** Returns the annotated class's properties as defined in its primary constructor. */
    open fun getProperties(): List<AppFunctionPropertyDeclaration> {
        val primaryConstructorProperties =
            checkNotNull(appFunctionSerializableClass.primaryConstructor).parameters

        val allProperties: Map<String, KSPropertyDeclaration> =
            appFunctionSerializableClass.getAllProperties().associateBy {
                (it.simpleName.asString())
            }

        return primaryConstructorProperties.mapNotNull { valueParameter ->
            allProperties[valueParameter.name?.asString()]?.let {
                AppFunctionPropertyDeclaration(
                    it,
                    isDescribedByKdoc,
                    isRequired = !valueParameter.hasDefault,
                )
            }
        }
    }

    /** Returns the properties that have @AppFunctionSerializable class types. */
    fun getSerializablePropertyTypeReferences(): Set<AppFunctionTypeReference> {
        return getProperties()
            .filterNot { it.isGenericType }
            .map { property -> AppFunctionTypeReference(property.type) }
            .filter { afType ->
                afType.isOfTypeCategory(SERIALIZABLE_SINGULAR) ||
                    afType.isOfTypeCategory(SERIALIZABLE_LIST)
            }
            .toSet()
    }

    /** Returns the properties that have @AppFunctionSerializableProxy class types. */
    fun getSerializableProxyPropertyTypeReferences(): Set<AppFunctionTypeReference> {
        return getProperties()
            .filterNot { it.isGenericType }
            .map { it -> AppFunctionTypeReference(it.type) }
            .filter { afType ->
                afType.isOfTypeCategory(SERIALIZABLE_PROXY_SINGULAR) ||
                    afType.isOfTypeCategory(SERIALIZABLE_PROXY_LIST)
            }
            .toSet()
    }

    /**
     * Returns the set of source files that contain the definition of [appFunctionSerializableClass]
     * and all @AppFunctionSerializable classes directly reachable through its fields. This method
     * differs from [getTransitiveSerializableSourceFiles] by excluding transitively
     * nested @AppFunctionSerializable classes.
     */
    fun getSerializableSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        appFunctionSerializableClass.containingFile?.let { sourceFileSet.add(it) }
        for (serializableAfType in getSerializablePropertyTypeReferences()) {
            val appFunctionSerializableDefinition =
                serializableAfType.selfOrItemTypeReference.resolve().declaration
                    as KSClassDeclaration
            appFunctionSerializableDefinition.containingFile?.let { sourceFileSet.add(it) }
        }
        return sourceFileSet
    }

    /**
     * Returns the set of source files that contain the definition of [appFunctionSerializableClass]
     * and all @AppFunctionSerializable classes transitively reachable through its fields or nested
     * classes.
     */
    fun getTransitiveSerializableSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        val visitedSerializableSet: MutableSet<ClassName> = mutableSetOf()

        // Add the file containing the AppFunctionSerializable class definition immediately it's
        // seen
        appFunctionSerializableClass.containingFile?.let { sourceFileSet.add(it) }
        visitedSerializableSet.add(originalClassName)
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
            if (visitedSerializableSet.contains(originalClassName)) {
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
}
