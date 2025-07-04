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

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference

/**
 * Represents a class annotated with [androidx.appfunctions.AppFunctionSerializable] that is
 * parameterized.
 *
 * When the serializable has type parameter (e.g. `SetField<T>`), the type arguments must be
 * provided as [arguments] to resolve the actual type reference.
 */
class AnnotatedParameterizedAppFunctionSerializable(
    private val appFunctionSerializableClass: KSClassDeclaration,
    private val arguments: List<KSTypeArgument>,
) : AnnotatedAppFunctionSerializable(appFunctionSerializableClass) {
    /** A map of type parameter name to its parameterized type. */
    val typeParameterMap: Map<String, KSTypeReference> = buildMap {
        for ((index, typeParameter) in appFunctionSerializableClass.typeParameters.withIndex()) {
            val typeParameterName = typeParameter.name.asString()
            val actualType =
                arguments.getOrNull(index)?.type
                    ?: throw ProcessingException(
                        "Missing type argument for $typeParameterName",
                        typeParameter,
                    )
            this[typeParameterName] = actualType
        }
    }

    /**
     * The JVM qualified name of the class being annotated with AppFunctionSerializable with the
     * parameterized type information included as a suffix.
     */
    override val jvmQualifiedName: String by lazy {
        val originalQualifiedName = super.jvmQualifiedName
        buildString {
            append(originalQualifiedName)

            for ((index, entry) in typeParameterMap.entries.withIndex()) {
                if (index == 0) {
                    append("<")
                }

                val (_, typeRef) = entry
                append(typeRef.toTypeName())

                if (index != typeParameterMap.size - 1) {
                    append(",")
                } else {
                    append(">")
                }
            }
        }
    }

    override val factoryVariableName: String by lazy {
        val variableName = jvmClassName.replace("$", "").replaceFirstChar { it -> it.lowercase() }
        val typeArgumentSuffix =
            typeParameterMap.values.joinToString { typeArgument ->
                typeArgument
                    .toTypeName()
                    .toString()
                    .replace(Regex("[_<>]"), "_")
                    .replace("?", "_Nullable")
                    .toPascalCase()
            }
        "${variableName}${typeArgumentSuffix}Factory"
    }

    /**
     * Returns the annotated class's properties as defined in its primary constructor.
     *
     * When the property is generic type, it will try to resolve the actual type reference from
     * [arguments].
     */
    override fun getProperties(): List<AppFunctionPropertyDeclaration> {
        return super.getProperties().map { propertyDeclaration ->
            val valueTypeDeclaration = propertyDeclaration.type.resolve().declaration
            if (valueTypeDeclaration is KSTypeParameter) {
                val actualType =
                    typeParameterMap[valueTypeDeclaration.name.asString()]
                        ?: throw ProcessingException(
                            "Unable to resolve actual type",
                            propertyDeclaration.type,
                        )
                AppFunctionPropertyDeclaration(
                    name = propertyDeclaration.name,
                    type = actualType,
                    description = propertyDeclaration.description,
                    isRequired = propertyDeclaration.isRequired,
                )
            } else {
                propertyDeclaration
            }
        }
    }
}
