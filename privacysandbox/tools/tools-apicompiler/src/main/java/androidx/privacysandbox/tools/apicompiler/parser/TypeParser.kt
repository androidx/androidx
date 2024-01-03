/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apicompiler.parser

import androidx.privacysandbox.tools.core.model.Type
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance

internal class TypeParser(private val logger: KSPLogger) {
    fun parseFromDeclaration(declaration: KSDeclaration): Type {
        return Type(
            packageName = declaration.packageName.getFullName(),
            simpleName = declaration.simpleName.getShortName(),
        )
    }

    fun parseFromTypeReference(typeReference: KSTypeReference, debugName: String): Type {
        val resolvedType = typeReference.resolve()
        if (resolvedType.isError) {
            logger.error("Failed to resolve type for $debugName.")
        }
        val typeArguments = typeReference.element?.typeArguments?.mapNotNull {
            if (it.type == null) {
                logger.error("Error in $debugName: null type argument.")
            }
            if (it.variance != Variance.INVARIANT) {
                logger.error("Error in $debugName: only invariant type arguments are supported.")
            }
            it.type
        } ?: emptyList()
        return Type(
            packageName = resolvedType.declaration.packageName.getFullName(),
            simpleName = resolvedType.declaration.simpleName.getShortName(),
            typeParameters = typeArguments.map { parseFromTypeReference(it, debugName) },
            isNullable = resolvedType.isMarkedNullable,
        )
    }
}
