/*
 * Copyright 2024 The Android Open Source Project
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
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JWildcardTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName

// Represent '?' in Java and '*' in Kotlin
internal class KspStarTypeArgumentType(
    env: KspProcessingEnv,
    typeArg: KSTypeArgument,
    //  In Java '?' can have annotations.
    originalKSAnnotations: Sequence<KSAnnotation> = typeArg.annotations,
    scope: KSTypeVarianceResolverScope? = null,
    typeAlias: KSType? = null,
) :
    KspTypeArgumentType(
        env,
        typeArg,
        originalKSAnnotations,
        scope = scope,
        typeAlias = typeAlias,
        ksType =
            if (env.delegate.kspVersion >= KotlinVersion(2, 0)) {
                // `typeArg.type` is `null` in KSP2, here we use `Unit` as a placeholder.
                env.resolver.builtIns.unitType
            } else {
                typeArg.requireType()
            }
    ) {
    override fun resolveJTypeName(): JTypeName {
        return JWildcardTypeName.subtypeOf(JTypeName.OBJECT)
    }

    override fun resolveKTypeName(): KTypeName {
        return STAR
    }

    override fun copy(
        env: KspProcessingEnv,
        ksType: KSType,
        originalKSAnnotations: Sequence<KSAnnotation>,
        scope: KSTypeVarianceResolverScope?,
        typeAlias: KSType?
    ) =
        KspStarTypeArgumentType(
            env = env,
            typeArg = DelegatingTypeArg(typeArg, type = ksType.createTypeReference()),
            originalKSAnnotations,
            scope = scope,
            typeAlias = typeAlias
        )
}
