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

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeVariableType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName

/**
 * An [XType] representing the type var type in a function parameter, return type or class
 * declaration.
 *
 * This is different than [KspMethodTypeVariableType] because the [KSType] has as reference the
 * [KSTypeParameter] declaration.
 */
internal class KspTypeVariableType(
    env: KspProcessingEnv,
    val ksTypeVariable: KSTypeParameter,
    ksType: KSType,
    originalKSAnnotations: Sequence<KSAnnotation> = ksTypeVariable.annotations,
    scope: KSTypeVarianceResolverScope? = null,
) : KspType(env, ksType, originalKSAnnotations, scope, null), XTypeVariableType {

    override fun resolveJTypeName(): JTypeName {
        return ksTypeVariable.asJTypeName(env.resolver)
    }

    override fun resolveKTypeName(): KTypeName {
        return ksTypeVariable.asKTypeName(env.resolver)
    }

    override val upperBounds: List<XType> = ksTypeVariable.bounds.map(env::wrap).toList()

    override fun boxed(): KspTypeVariableType {
        return this
    }

    override fun copy(
        env: KspProcessingEnv,
        ksType: KSType,
        originalKSAnnotations: Sequence<KSAnnotation>,
        scope: KSTypeVarianceResolverScope?,
        typeAlias: KSType?
    ) = KspTypeVariableType(
        env,
        ksType.declaration as KSTypeParameter,
        ksType,
        originalKSAnnotations,
        scope
    )

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(ksTypeVariable)
    }
}
