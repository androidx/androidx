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

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Modifier

/**
 * Represents a class annotated with `@AppFunctionSerializable` that can be represented as a OneOf.
 */
class AnnotatedOneOfAppFunctionSerializable(override val classDeclaration: KSClassDeclaration) :
    AppFunctionSerializableType {

    override val isDescribedByKDoc: Boolean by lazy {
        val annotation =
            classDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
            )
        return@lazy annotation?.requirePropertyValueOfType(
            AppFunctionAnnotation.PROPERTY_IS_DESCRIBED_BY_KDOC,
            Boolean::class,
        ) ?: false
    }

    private val sealedSubclasses: Sequence<KSClassDeclaration> by lazy {
        classDeclaration.getSealedSubclasses()
    }

    /** List of serializable classes that extend this class. */
    val oneOfSerializables: Sequence<AppFunctionSerializableType> by lazy {
        sealedSubclasses.map { AnnotatedAppFunctionSerializable(it) }
    }

    override fun validate(
        allowSerializableInterfaceTypes: Boolean
    ): AnnotatedOneOfAppFunctionSerializable {
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

            if (
                sealedSubclass.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
                ) == null
            ) {
                throw ProcessingException(
                    "All subclasses of ${appFunctionSerializableTypeClassDeclaration.jvmClassName} should be annotated with @AppFunctionSerializable. Did you forget to annotate ${sealedSubclass.getJvmClassName()}?",
                    sealedSubclass,
                )
            }
        }

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
}
