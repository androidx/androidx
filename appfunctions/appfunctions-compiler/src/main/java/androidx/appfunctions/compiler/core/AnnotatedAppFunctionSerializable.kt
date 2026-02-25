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

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.RESTRICT_API_TO_33_ANNOTATION
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper.Companion.buildFromAppFunctionDataFunction
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper.Companion.buildToAppFunctionDataFunction
import androidx.appfunctions.compiler.processors.AppFunctionSerializableFactoryCodeBuilderHelper.Companion.setGenericPrimaryConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

// TODO(b/410764334): Re-evaluate the abstraction layer.
/** Represents a class annotated with `androidx.appfunctions.AppFunctionSerializable`. */
open class AnnotatedAppFunctionSerializable(override val classDeclaration: KSClassDeclaration) :
    AppFunctionSerializableType {

    /** The name to be assigned to the serializable factory's instance. */
    override val factoryVariableName: String by lazy {
        "${appFunctionSerializableTypeClassDeclaration.jvmClassName.replace("$", "").replaceFirstChar {  it.lowercase() } }Factory"
    }

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

    /** A description of the AppFunctionSerializable class and its intended use. */
    override fun getDescription(sharedDataTypeDescriptionMap: Map<String, String>): String {
        return docString.ifEmpty { sharedDataTypeDescriptionMap[jvmQualifiedName] ?: "" }
    }

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
        return AnnotatedParameterizedAppFunctionSerializable(classDeclaration, arguments)
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
    override fun validate(
        allowSerializableInterfaceTypes: Boolean
    ): AnnotatedAppFunctionSerializable {
        val validateHelper = AppFunctionSerializableValidateHelper(this)
        validateHelper.validatePrimaryConstructor()
        validateHelper.validateParameters(allowSerializableInterfaceTypes)
        return this
    }

    override fun getFactoryCodeBuilder(
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies
    ): AppFunctionSerializableType.FactoryCodeBuilder =
        AnnotatedAppFunctionSerializableFactoryCodeBuilder(
            this,
            resolvedAnnotatedSerializableProxies,
        )

    private class AnnotatedAppFunctionSerializableFactoryCodeBuilder(
        val annotatedClass: AnnotatedAppFunctionSerializable,
        val resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
    ) : AppFunctionSerializableType.FactoryCodeBuilder {
        override fun buildAppFunctionSerializableFactoryClass(): FileSpec {
            val superInterfaceClass =
                AppFunctionSerializableFactoryClass.CLASS_NAME.parameterizedBy(
                    listOf(annotatedClass.appFunctionSerializableTypeClassDeclaration.typeName)
                )

            val factoryCodeBuilder =
                AppFunctionSerializableFactoryCodeBuilderHelper(
                    annotatedClass,
                    resolvedAnnotatedSerializableProxies,
                )

            val generatedFactoryClassName = annotatedClass.factoryClassName.simpleName
            return FileSpec.builder(
                    annotatedClass.appFunctionSerializableTypeClassDeclaration.originalClassName
                        .packageName,
                    generatedFactoryClassName,
                )
                .addType(
                    TypeSpec.classBuilder(generatedFactoryClassName)
                        .addAnnotation(RESTRICT_API_TO_33_ANNOTATION)
                        .addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
                        .addSuperinterface(superInterfaceClass)
                        .apply {
                            if (
                                annotatedClass.appFunctionSerializableTypeClassDeclaration.modifiers
                                    .contains(Modifier.INTERNAL)
                            ) {
                                addModifiers(KModifier.INTERNAL)
                            }

                            if (
                                annotatedClass.appFunctionSerializableTypeClassDeclaration
                                    .typeParameters
                                    .isNotEmpty()
                            ) {
                                setGenericPrimaryConstructor(
                                    annotatedClass.appFunctionSerializableTypeClassDeclaration
                                        .typeParameters
                                )
                            }
                        }
                        .addFunction(
                            buildFromAppFunctionDataFunction(
                                factoryCodeBuilder.buildFromAppFunctionDataMethodBody(),
                                returnType =
                                    annotatedClass.appFunctionSerializableTypeClassDeclaration
                                        .typeName,
                            )
                        )
                        .addFunction(
                            buildToAppFunctionDataFunction(
                                factoryCodeBuilder.buildToAppFunctionDataMethodBody(),
                                parameterType =
                                    annotatedClass.appFunctionSerializableTypeClassDeclaration
                                        .typeName,
                            )
                        )
                        .build()
                )
                .build()
        }
    }
}
