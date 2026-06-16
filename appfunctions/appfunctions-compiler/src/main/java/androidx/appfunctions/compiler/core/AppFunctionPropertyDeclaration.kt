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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference

// TODO(b/403525399): Add support for checking optional property.
/** A wrapper class to store the property declaration in a class. */
data class AppFunctionPropertyDeclaration(
    val name: String,
    val type: KSTypeReference,
    val description: String,
    val isRequired: Boolean,
    val propertyAnnotations: Sequence<KSAnnotation> = emptySequence(),
    val qualifiedName: String,
) {
    /** Indicates whether the [type] is a generic type or not. */
    val isGenericType: Boolean by lazy { type.resolve().declaration is KSTypeParameter }

    companion object {
        /** Creates an [AppFunctionPropertyDeclaration] from [KSPropertyDeclaration]. */
        fun create(
            property: KSPropertyDeclaration,
            isDescribedByKDoc: Boolean,
            isRequired: Boolean,
            sharedDataTypeDescriptionMap: Map<String, String>,
            properTagDescriptions: Map<String, String> = emptyMap(),
            paramTagDescriptions: Map<String, String> = emptyMap(),
        ): AppFunctionPropertyDeclaration {
            val instruction =
                property.annotations
                    .findAnnotation(IntrospectionHelper.AppFunctionInstructionAnnotation.CLASS_NAME)
                    ?.requirePropertyValueOfType(
                        IntrospectionHelper.AppFunctionInstructionAnnotation.PROPERTY_INSTRUCTION,
                        String::class,
                    )

            val docString = property.docString
            val propertyName = checkNotNull(property.simpleName).asString()
            val description =
                instruction
                    ?: if (isDescribedByKDoc) {
                        if (!docString.isNullOrEmpty()) {
                            docString
                        } else {
                            properTagDescriptions[propertyName]
                                ?: paramTagDescriptions[propertyName]
                                ?: sharedDataTypeDescriptionMap[property.getQualifiedName()]
                                ?: ""
                        }
                    } else {
                        ""
                    }

            return AppFunctionPropertyDeclaration(
                checkNotNull(property.simpleName).asString(),
                property.type,
                description,
                isRequired,
                property.annotations,
                property.getQualifiedName(),
            )
        }
    }
}
