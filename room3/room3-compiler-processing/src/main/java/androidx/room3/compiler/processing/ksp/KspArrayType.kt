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

import androidx.room3.compiler.processing.XArrayType
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XTypeArgument
import androidx.room3.compiler.processing.XVariance
import com.google.devtools.ksp.symbol.KSType

internal sealed class KspArrayType(
    env: KspProcessingEnv,
    ksType: KSType,
    scope: KSTypeVarianceResolverScope? = null,
) : KspType(env, ksType, scope), XArrayType {

    abstract override val componentType: KspType

    override fun boxed() = this

    override val typeArguments: List<XTypeArgument>
        get() = emptyList() // hide them to behave like java does

    /** Kotlin arrays in the form of Array<X>. */
    private class BoxedArray(
        env: KspProcessingEnv,
        ksType: KSType,
        scope: KSTypeVarianceResolverScope? = null,
    ) : KspArrayType(env, ksType, scope) {

        override val componentType: KspType by lazy {
            val arg = ksType.arguments.single()
            // https://kotlinlang.org/docs/reference/basic-types.html#primitive-type-arrays
            // these are always boxed
            env.wrap(ksType = checkNotNull(arg.type?.resolve()), allowPrimitives = false)
        }

        override fun copy(
            env: KspProcessingEnv,
            ksType: KSType,
            scope: KSTypeVarianceResolverScope?,
        ) = BoxedArray(env, ksType, scope)
    }

    /** Built in primitive array types (e.g. IntArray) */
    private class PrimitiveArray(
        env: KspProcessingEnv,
        ksType: KSType,
        scope: KSTypeVarianceResolverScope? = null,
        override val componentType: KspType,
    ) : KspArrayType(env, ksType, scope) {

        override fun copy(
            env: KspProcessingEnv,
            ksType: KSType,
            scope: KSTypeVarianceResolverScope?,
        ) = PrimitiveArray(env, ksType, scope, componentType)
    }

    /** Factory class to create instances of [KspArrayType]. */
    internal class Factory(private val env: KspProcessingEnv) {
        // map of built in array type to its component type
        private val builtInArrays =
            mapOf(
                "kotlin.BooleanArray" to KspPrimitiveType(env, env.resolver.builtIns.booleanType),
                "kotlin.ByteArray" to KspPrimitiveType(env, env.resolver.builtIns.byteType),
                "kotlin.CharArray" to KspPrimitiveType(env, env.resolver.builtIns.charType),
                "kotlin.DoubleArray" to KspPrimitiveType(env, env.resolver.builtIns.doubleType),
                "kotlin.FloatArray" to KspPrimitiveType(env, env.resolver.builtIns.floatType),
                "kotlin.IntArray" to KspPrimitiveType(env, env.resolver.builtIns.intType),
                "kotlin.LongArray" to KspPrimitiveType(env, env.resolver.builtIns.longType),
                "kotlin.ShortArray" to KspPrimitiveType(env, env.resolver.builtIns.shortType),
            )

        // map from the primitive to its array
        private val reverseBuiltInArrayLookup =
            builtInArrays.entries.associateBy { it.value.ksType }

        fun createWithComponentType(componentType: KspType): KspArrayType {
            return createWithComponentType(
                env.createTypeArgument(componentType, XVariance.INVARIANT)
            )
        }

        fun createWithComponentType(componentType: KspTypeArgument): KspArrayType {
            if (componentType.type.nullability == XNullability.NONNULL) {
                val primitiveArrayEntry: Map.Entry<String, KspPrimitiveType>? =
                    reverseBuiltInArrayLookup[componentType.type.ksType]
                if (primitiveArrayEntry != null) {
                    return PrimitiveArray(
                        env = env,
                        ksType = env.resolver.requireType(primitiveArrayEntry.key),
                        componentType = primitiveArrayEntry.value,
                    )
                }
            }

            return BoxedArray(
                env = env,
                ksType =
                    env.resolver.builtIns.arrayType.replace(listOf(componentType.ksTypeArgument)),
            )
        }

        /**
         * Creates and returns a [KspArrayType] if and only if the given [ksType] represents an
         * array.
         */
        fun create(ksType: KSType): KspArrayType {
            check(isArrayType(ksType)) { "Cannot create array type for $ksType" }
            val qName = ksType.declaration.qualifiedName?.asString()
            return if (qName == KOTLIN_ARRAY_Q_NAME) {
                BoxedArray(env = env, ksType = ksType)
            } else {
                PrimitiveArray(env = env, ksType = ksType, componentType = builtInArrays[qName]!!)
            }
        }

        /** Returns `true` if and only if the given [ksType] represents an array. */
        fun isArrayType(ksType: KSType): Boolean {
            val qName = ksType.declaration.qualifiedName?.asString() ?: return false
            return qName == KOTLIN_ARRAY_Q_NAME || builtInArrays.containsKey(qName)
        }
    }

    companion object {
        const val KOTLIN_ARRAY_Q_NAME = "kotlin.Array"
    }
}
