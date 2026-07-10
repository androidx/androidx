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

import androidx.appfunctions.compiler.core.AnnotatedOneOfAppFunctionSerializable.Companion.create
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

/**
 * Represents a class annotated with `@AppFunctionSerializable` that can be represented as a OneOf.
 */
sealed class AnnotatedOneOfAppFunctionSerializable(
    override val classDeclaration: KSClassDeclaration
) : AppFunctionSerializableType {

    override val isDescribedByKDoc: Boolean by lazy {
        val annotation =
            classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
            )
        return@lazy annotation?.requirePropertyValueOfType(
            IntrospectionHelper.AppFunctionSerializableAnnotation.PROPERTY_IS_DESCRIBED_BY_KDOC,
            Boolean::class,
        ) ?: false
    }

    /** List of serializable classes that extend this class. */
    abstract val oneOfSerializables: Sequence<AnnotatedAppFunctionSerializable>

    abstract fun supportsExhaustiveWhen(): Boolean

    override fun validate(
        allowSerializableInterfaceTypes: Boolean
    ): AnnotatedOneOfAppFunctionSerializable {

        for (oneOfSerializable in oneOfSerializables) {
            oneOfSerializable.validate()
        }

        return this
    }

    override fun getSerializableSourceFiles(): Set<KSFile> = buildSet {
        add(checkNotNull(classDeclaration.containingFile))

        addAll(oneOfSerializables.flatMap { it.getSerializableSourceFiles() })
    }

    override fun getFactoryCodeBuilder(
        resolvedAnnotatedSerializableProxies:
            AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
    ): AppFunctionSerializableType.FactoryCodeBuilder =
        OneOfAppFunctionSerializableFactoryCodeBuilder(this)

    /** Returns an emptyList since a sealed interface properties will be inherited by subclass. */
    override fun getProperties(
        sharedDataTypeDescriptionMap: Map<String, String>
    ): List<AppFunctionPropertyDeclaration> = emptyList()

    private class SealedOneOfSerializable(classDeclaration: KSClassDeclaration) :
        AnnotatedOneOfAppFunctionSerializable(classDeclaration) {

        private val sealedSubclasses: Sequence<KSClassDeclaration> by lazy {
            classDeclaration.getSealedSubclasses()
        }

        /** List of serializable classes that extend this class. */
        override val oneOfSerializables: Sequence<AnnotatedAppFunctionSerializable> by lazy {
            sealedSubclasses.map { AnnotatedAppFunctionSerializable(it) }
        }

        override fun validate(allowSerializableInterfaceTypes: Boolean): SealedOneOfSerializable {
            if (!appFunctionSerializableTypeClassDeclaration.modifiers.contains(Modifier.SEALED)) {
                throw ProcessingException(
                    "Non-sealed classes cannot be used to represent OneOf." +
                        " ${appFunctionSerializableTypeClassDeclaration.jvmClassName} is not sealed.",
                    appFunctionSerializableTypeClassDeclaration.attributeNode,
                )
            }

            for (sealedSubclass in sealedSubclasses) {
                if (sealedSubclass.modifiers.contains(Modifier.SEALED)) {
                    throw ProcessingException(
                        "Nested sealed classes are not allowed. ${sealedSubclass.getJvmClassName()} is a sealed class within ${appFunctionSerializableTypeClassDeclaration.jvmClassName}.",
                        sealedSubclass,
                    )
                }
            }

            return super.validate(allowSerializableInterfaceTypes) as SealedOneOfSerializable
        }

        override fun supportsExhaustiveWhen(): Boolean = true
    }

    /**
     * Represents a class annotated with `@AppFunctionSerializable` that is not sealed but has a
     * list of possible subclasses listed by the `@AppFunctionOneOfType` annotation.
     */
    private class NonSealedOneOfSerializable(classDeclaration: KSClassDeclaration) :
        AnnotatedOneOfAppFunctionSerializable(classDeclaration) {

        private val oneOfTypeAnnotation: KSAnnotation by lazy {
            findOneOfTypeAnnotation(classDeclaration)
                ?: throw ProcessingException(
                    "@AppFunctionOneOfType annotation not found",
                    classDeclaration,
                )
        }

        private val allowedOneOfDeclarations: Sequence<KSClassDeclaration> by lazy {
            oneOfTypeAnnotation
                .requirePropertyValueOfType(
                    IntrospectionHelper.AppFunctionOneOfTypeAnnotation.PROPERTY_MATCH_ONE_OF,
                    List::class,
                )
                .asSequence()
                .map {
                    val type =
                        it as? KSType
                            ?: throw ProcessingException(
                                "$it provided in matchOneOf property of AppFunctionOneOfType could not be processed as KSType",
                                oneOfTypeAnnotation,
                            )

                    type.declaration as? KSClassDeclaration
                        ?: throw ProcessingException(
                            "${type.declaration} cannot be cast to a Class declaration",
                            type.declaration,
                        )
                }
        }

        /** List of serializable classes that extend this class. */
        override val oneOfSerializables: Sequence<AnnotatedAppFunctionSerializable> by lazy {
            allowedOneOfDeclarations.map { AnnotatedAppFunctionSerializable(it) }
        }

        override fun validate(
            allowSerializableInterfaceTypes: Boolean
        ): NonSealedOneOfSerializable {
            for (allowedOneOfDeclaration in allowedOneOfDeclarations) {
                if (!isSubclass(allowedOneOfDeclaration, classDeclaration)) {
                    throw ProcessingException(
                        "${allowedOneOfDeclaration.getJvmClassName()} is not" +
                            " a subclass of " +
                            classDeclaration.getJvmClassName(),
                        allowedOneOfDeclaration,
                    )
                }
            }

            return super.validate(allowSerializableInterfaceTypes) as NonSealedOneOfSerializable
        }

        private fun isSubclass(
            subclassDecl: KSClassDeclaration,
            superclassDecl: KSClassDeclaration,
        ): Boolean {
            val subType = subclassDecl.asStarProjectedType()
            val superType = superclassDecl.asStarProjectedType()

            return superType.isAssignableFrom(subType)
        }

        override fun supportsExhaustiveWhen(): Boolean = false
    }

    companion object {
        /**
         * Creates [AnnotatedOneOfAppFunctionSerializable] from the given [classDeclaration].
         *
         * A one of type can be defined in two ways:
         * 1. The class declaration is marked sealed and annotated with `@AppFunctionSerializable`.
         * 2. The class declaration is annotated with `@AppFunctionOneOfType` listing all the
         *    possible one of types and also annotated with `@AppFunctionSerializable`.
         */
        fun create(classDeclaration: KSClassDeclaration): AnnotatedOneOfAppFunctionSerializable =
            when {
                classDeclaration.modifiers.contains(Modifier.SEALED) -> {
                    SealedOneOfSerializable(classDeclaration)
                }
                classDeclaration.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionOneOfTypeAnnotation.CLASS_NAME
                ) != null -> {
                    NonSealedOneOfSerializable(classDeclaration)
                }

                else ->
                    throw ProcessingException(
                        "Cannot be represented as a oneOf Type",
                        classDeclaration,
                    )
            }

        private fun findOneOfTypeAnnotation(classDeclaration: KSClassDeclaration): KSAnnotation? =
            classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionOneOfTypeAnnotation.CLASS_NAME
            )

        /**
         * Returns true if the given [classDeclaration] can be represented as a
         * [AnnotatedOneOfAppFunctionSerializable].
         *
         * @see create for more details.
         */
        fun isOneOfType(classDeclaration: KSClassDeclaration): Boolean =
            classDeclaration.modifiers.contains(Modifier.SEALED) ||
                findOneOfTypeAnnotation(classDeclaration) != null
    }
}
