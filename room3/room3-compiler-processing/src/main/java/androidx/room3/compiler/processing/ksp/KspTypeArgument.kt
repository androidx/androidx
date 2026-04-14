/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XEquality
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeArgument
import androidx.room3.compiler.processing.XVariance
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Variance

internal class KspTypeArgument(val env: KspProcessingEnv, val ksTypeArgument: KSTypeArgument) :
    XTypeArgument, XEquality {

    override val type: KspType by lazy {
        val ksType =
            when (ksTypeArgument.variance) {
                Variance.INVARIANT,
                Variance.COVARIANT,
                Variance.CONTRAVARIANT -> ksTypeArgument.requireType()
                Variance.STAR -> env.resolver.builtIns.anyType.makeNullable()
            }
        env.wrap(ksType, allowPrimitives = false)
    }

    override val variance: XVariance by lazy {
        when (ksTypeArgument.variance) {
            Variance.INVARIANT -> XVariance.INVARIANT
            Variance.COVARIANT -> XVariance.OUT
            Variance.CONTRAVARIANT -> XVariance.IN
            Variance.STAR -> XVariance.STAR
        }
    }

    override fun asTypeName(): XTypeName {
        val jvmKsTypeArgument =
            env.resolver.getTypeArgument(
                env.resolver.createKSTypeReferenceFromKSType(type.jvmKsType),
                ksTypeArgument.variance,
            )
        return XTypeName(
            jvmKsTypeArgument.asJTypeName(env.resolver),
            ksTypeArgument.asKTypeName(env.resolver),
        )
    }

    override val equalityItems: Array<out Any?> by lazy { arrayOf(variance, type) }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return "${ksTypeArgument.variance} ${ksTypeArgument.type}"
    }

    companion object {
        fun create(env: KspProcessingEnv, ksTypeArgument: KSTypeArgument) =
            KspTypeArgument(env, ksTypeArgument)

        fun create(env: KspProcessingEnv, type: XType, variance: XVariance): KspTypeArgument {
            check(type is KspType)
            return KspTypeArgument(
                env = env,
                ksTypeArgument =
                    env.resolver.getTypeArgument(
                        typeRef = env.resolver.createKSTypeReferenceFromKSType(type.ksType),
                        variance =
                            when (variance) {
                                XVariance.INVARIANT -> Variance.INVARIANT
                                XVariance.OUT -> Variance.COVARIANT
                                XVariance.IN -> Variance.CONTRAVARIANT
                                XVariance.STAR -> Variance.STAR
                            },
                    ),
            )
        }
    }
}
