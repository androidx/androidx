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
    /** Creates an [AppFunctionPropertyDeclaration] from [KSPropertyDeclaration]. */
    constructor(
        property: KSPropertyDeclaration,
        isDescribedByKdoc: Boolean,
        isRequired: Boolean,
        sharedDataTypeDescriptionMap: Map<String, String>,
    ) : this(
        checkNotNull(property.simpleName).asString(),
        property.type,
        if (isDescribedByKdoc) {
            property.docString?.ifEmpty {
                sharedDataTypeDescriptionMap[property.getQualifiedName()]
            } ?: ""
        } else {
            ""
        },
        isRequired,
        property.annotations,
        property.getQualifiedName(),
    )

    /** Indicates whether the [type] is a generic type or not. */
    val isGenericType: Boolean by lazy { type.resolve().declaration is KSTypeParameter }
}
