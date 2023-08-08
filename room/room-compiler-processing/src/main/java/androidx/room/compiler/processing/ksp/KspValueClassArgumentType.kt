/*
 * Copyright 2023 The Android Open Source Project
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

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName

/**
 * The value class type when it is being used as type argument.
 *
 * When value class is used as type argument, it is not inlined. Therefore, its type name is
 * different from the underlying type in this case.
 */
internal class KspValueClassArgumentType(
    env: KspProcessingEnv,
    // Using KSTypeArgument rather than resolved type to indicate do not inline in type name.
    val typeArg: KSTypeArgument,
    originalKSAnnotations: Sequence<KSAnnotation>,
    scope: KSTypeVarianceResolverScope? = null,
    typeAlias: KSType? = null,
) : KspType(
    env = env,
    ksType = typeArg.requireType(),
    originalKSAnnotations = originalKSAnnotations,
    scope = scope,
    typeAlias = typeAlias,
) {
    override fun resolveJTypeName(): JTypeName {
        return typeArg.asJTypeName(env.resolver)
    }

    override fun resolveKTypeName(): KTypeName {
        return typeArg.asKTypeName(env.resolver)
    }

    override fun boxed(): KspValueClassArgumentType {
        return this
    }

    override fun copy(
        env: KspProcessingEnv,
        ksType: KSType,
        originalKSAnnotations: Sequence<KSAnnotation>,
        scope: KSTypeVarianceResolverScope?,
        typeAlias: KSType?
    ) = KspValueClassArgumentType(
        env = env,
        typeArg = DelegatingTypeArg(typeArg, type = ksType.createTypeReference()),
        originalKSAnnotations = originalKSAnnotations,
        scope = scope,
        typeAlias = typeAlias
    )

    private class DelegatingTypeArg(
        val original: KSTypeArgument,
        override val type: KSTypeReference
    ) : KSTypeArgument by original
}
