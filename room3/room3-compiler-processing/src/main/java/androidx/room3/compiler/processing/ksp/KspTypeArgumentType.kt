/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room3.compiler.processing.ksp

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance

/**
 * The typeName for type arguments requires the type parameter, hence we have a special type for
 * them when we produce them.
 */
internal open class KspTypeArgumentType
private constructor(
    env: KspProcessingEnv,
    val typeArg: KSTypeArgument,
    originalKSAnnotations: Sequence<KSAnnotation> = typeArg.annotations,
    scope: KSTypeVarianceResolverScope? = null,
    typeAlias: KSType? = null,
    ksType: KSType = typeArg.requireType(),
) :
    KspType(
        env = env,
        ksType = ksType,
        originalKSAnnotations = originalKSAnnotations,
        scope = scope,
        typeAlias = typeAlias,
    ) {
    /**
     * When KSP resolves classes, it always resolves to the upper bound. Hence, the ksType we pass
     * to super is actually our extendsBound. Note that an unbound type argument will resolve to
     * itself thus we need to check if the extendBound is not the same as this type arg.
     */
    private val _extendsBound by lazy {
        val extendBound = env.wrap(ksType = ksType, allowPrimitives = false)
        if (isStar() || (this.ksType.declaration is KSTypeParameter && this == extendBound)) {
            null
        } else {
            extendBound
        }
    }

    override fun isStar() = typeArg.variance == Variance.STAR

    override fun extendsBound() = _extendsBound

    override fun resolveJTypeName() = typeArg.asJTypeName(env.resolver)

    override fun resolveKTypeName() = typeArg.asKTypeName(env.resolver)

    override fun boxed() = this

    override fun copy(
        env: KspProcessingEnv,
        ksType: KSType,
        originalKSAnnotations: Sequence<KSAnnotation>,
        scope: KSTypeVarianceResolverScope?,
        typeAlias: KSType?,
    ) =
        KspTypeArgumentType(
            env = env,
            typeArg = DelegatingTypeArg(typeArg, type = ksType.createTypeReference()),
            originalKSAnnotations,
            scope = scope,
            typeAlias = typeAlias,
        )

    internal class DelegatingTypeArg(
        val original: KSTypeArgument,
        override val type: KSTypeReference,
    ) : KSTypeArgument by original

    companion object {
        fun create(env: KspProcessingEnv, typeArg: KSTypeArgument): KspTypeArgumentType {
            return when (typeArg.variance) {
                Variance.STAR ->
                    if (env.delegate.kspVersion >= KotlinVersion(2, 0)) {
                        // `typeArg.type` is `null` in KSP2, here we use `Unit` as a placeholder.
                        KspTypeArgumentType(env, typeArg, ksType = env.resolver.builtIns.unitType)
                    } else {
                        KspTypeArgumentType(env, typeArg)
                    }
                Variance.COVARIANT,
                Variance.CONTRAVARIANT -> KspTypeArgumentType(env, typeArg)
                Variance.INVARIANT -> error("Unexpected INVARIANT type argument: $typeArg")
            }
        }
    }
}
