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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeVariableType
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName

internal class KspTypeVariableType(
    env: KspProcessingEnv,
    ksType: KSType,
    scope: KSTypeVarianceResolverScope?
) : KspType(env, ksType, scope), XTypeVariableType {
    private val typeVariable: KSTypeParameter by lazy {
        // Note: This is a workaround for a bug in KSP where we may get ERROR_TYPE in the bounds
        // (https://github.com/google/ksp/issues/1250). To work around it we get the matching
        // KSTypeParameter from the parent declaration instead.
        ksType.declaration.parentDeclaration!!.typeParameters
            .filter { it.name == (ksType.declaration as KSTypeParameter).name }
            .single()
    }

    override fun resolveJTypeName(): JTypeName {
        return typeVariable.asJTypeName(env.resolver)
    }

    override fun resolveKTypeName(): KTypeName {
        return typeVariable.asKTypeName(env.resolver)
    }

    override val upperBounds: List<XType> = typeVariable.bounds.map(env::wrap).toList()

    override fun boxed(): KspTypeVariableType {
        return this
    }

    override fun copyWithNullability(nullability: XNullability): KspTypeVariableType {
        return KspTypeVariableType(
            env = env,
            ksType = ksType,
            scope = scope
        )
    }

    override fun copyWithScope(scope: KSTypeVarianceResolverScope): KspType {
        return KspTypeVariableType(
            env = env,
            ksType = ksType,
            scope = scope
        )
    }
}