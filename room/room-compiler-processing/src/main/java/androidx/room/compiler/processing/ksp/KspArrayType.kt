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

import androidx.room.compiler.processing.XArrayType
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName

internal sealed class KspArrayType(
    env: KspProcessingEnv,
    ksType: KSType
) : KspType(
    env, ksType
),
    XArrayType {

    override val typeName: TypeName by lazy {
        ArrayTypeName.of(componentType.typeName)
    }

    override fun boxed() = this

    override val typeArguments: List<XType>
        get() = emptyList() // hide them to behave like java does

    /**
     * Kotlin arrays in the form of Array<X>.
     */
    private class BoxedArray(
        env: KspProcessingEnv,
        ksType: KSType
    ) : KspArrayType(
        env, ksType
    ) {
        override val componentType: XType by lazy {
            val arg = ksType.arguments.single()
            // https://kotlinlang.org/docs/reference/basic-types.html#primitive-type-arrays
            // these are always boxed
            env.wrap(
                ksType = checkNotNull(arg.type?.resolve()),
                allowPrimitives = false
            )
        }

        override fun copyWithNullability(nullability: XNullability): BoxedArray {
            return BoxedArray(
                env = env,
                ksType = ksType.withNullability(nullability)
            )
        }
    }

    /**
     * Built in primitive array types (e.g. IntArray)
     */
    private class PrimitiveArray(
        env: KspProcessingEnv,
        ksType: KSType,
        override val componentType: KspType
    ) : KspArrayType(
        env, ksType
    ) {
        override fun copyWithNullability(nullability: XNullability): PrimitiveArray {
            return PrimitiveArray(
                env = env,
                ksType = ksType.withNullability(nullability),
                componentType = componentType
            )
        }
    }

    /**
     * Factory class to create instances of [KspArrayType].
     */
    internal class Factory(private val env: KspProcessingEnv) {
        // map of built in array type to its component type
        private val builtInArrays = mapOf(
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
        private val reverseBuiltInArrayLookup = builtInArrays.entries
            .associateBy { it.value.ksType }

        fun createWithComponentType(componentType: KspType): KspArrayType {
            if (componentType.nullability == XNullability.NONNULL) {
                val primitiveArrayEntry: Map.Entry<String, KspPrimitiveType>? =
                    reverseBuiltInArrayLookup[componentType.ksType]
                if (primitiveArrayEntry != null) {
                    return PrimitiveArray(
                        env = env,
                        ksType = env.resolver.requireType(
                            primitiveArrayEntry.key
                        ),
                        componentType = primitiveArrayEntry.value
                    )
                }
            }

            return BoxedArray(
                env = env,
                ksType = env.resolver.builtIns.arrayType.replace(
                    listOf(
                        env.resolver.getTypeArgument(
                            componentType.ksType.createTypeReference(),
                            Variance.INVARIANT
                        )
                    )
                )
            )
        }

        /**
         * Creates and returns a [KspArrayType] if and only if the given [ksType] represents an
         * array.
         */
        fun createIfArray(ksType: KSType): KspArrayType? {
            val qName = ksType.declaration.qualifiedName?.asString()
            if (qName == KOTLIN_ARRAY_Q_NAME) {
                return BoxedArray(
                    env = env,
                    ksType = ksType
                )
            }
            builtInArrays[qName]?.let { primitiveType ->
                return PrimitiveArray(
                    env = env,
                    ksType = ksType,
                    componentType = primitiveType
                )
            }
            return null
        }
    }

    companion object {
        private const val KOTLIN_ARRAY_Q_NAME = "kotlin.Array"
    }
}