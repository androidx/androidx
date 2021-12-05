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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.javapoet.TypeName

/**
 * The typeName for type arguments requires the type parameter, hence we have a special type
 * for them when we produce them.
 */
internal class KspTypeArgumentType(
    env: KspProcessingEnv,
    val typeParam: KSTypeParameter,
    val typeArg: KSTypeArgument,
    jvmTypeResolver: KspJvmTypeResolver?
) : KspType(
    env = env,
    ksType = typeArg.requireType(),
    jvmTypeResolver = jvmTypeResolver
) {
    /**
     * When KSP resolves classes, it always resolves to the upper bound. Hence, the ksType we
     * pass to super is actually our extendsBound.
     */
    private val _extendsBound by lazy {
        env.wrap(
            ksType = ksType,
            allowPrimitives = false
        )
    }

    override fun resolveTypeName(): TypeName {
        return typeArg.typeName(env.resolver)
    }

    override fun boxed(): KspTypeArgumentType {
        return this
    }

    override fun extendsBound(): XType {
        return _extendsBound
    }

    override fun copyWithNullability(nullability: XNullability): KspTypeArgumentType {
        return KspTypeArgumentType(
            env = env,
            typeParam = typeParam,
            typeArg = DelegatingTypeArg(
                original = typeArg,
                type = ksType.withNullability(nullability).createTypeReference()
            ),
            jvmTypeResolver = jvmTypeResolver
        )
    }

    override fun copyWithJvmTypeResolver(jvmTypeResolver: KspJvmTypeResolver): KspType {
        return KspTypeArgumentType(
            env = env,
            typeParam = typeParam,
            typeArg = typeArg,
            jvmTypeResolver = jvmTypeResolver
        )
    }

    private class DelegatingTypeArg(
        val original: KSTypeArgument,
        override val type: KSTypeReference
    ) : KSTypeArgument by original
}
