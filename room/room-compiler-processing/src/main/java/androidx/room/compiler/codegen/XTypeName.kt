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

package androidx.room.compiler.codegen

import androidx.room.compiler.processing.XNullability
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.asTypeName as asKTypeName
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import com.squareup.kotlinpoet.javapoet.JWildcardTypeName
import com.squareup.kotlinpoet.javapoet.KClassName
import com.squareup.kotlinpoet.javapoet.KParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName
import com.squareup.kotlinpoet.javapoet.KTypeVariableName
import com.squareup.kotlinpoet.javapoet.KWildcardTypeName
import kotlin.reflect.KClass

/**
 * Represents a type name in Java and Kotlin's type system.
 *
 * It simply contains a [com.squareup.javapoet.TypeName] and a [com.squareup.kotlinpoet.TypeName].
 * If the name comes from xprocessing APIs then the KotlinPoet name will default to 'Unavailable' if
 * the processing backend is not KSP.
 *
 * @see [androidx.room.compiler.processing.XType.asTypeName]
 */
open class XTypeName
protected constructor(
    internal open val java: JTypeName,
    internal open val kotlin: KTypeName,
    val nullability: XNullability,
) {
    val isPrimitive: Boolean
        get() = java.isPrimitive

    val isBoxedPrimitive: Boolean
        get() = java.isBoxedPrimitive

    /**
     * Returns the raw [XTypeName] if this is a parametrized type name, or itself if not.
     *
     * @see [XClassName.parametrizedBy]
     */
    val rawTypeName: XTypeName
        get() {
            val javaRawType = java.let { if (it is JParameterizedTypeName) it.rawType else it }
            val kotlinRawType = kotlin.let { if (it is KParameterizedTypeName) it.rawType else it }
            return XTypeName(javaRawType, kotlinRawType, nullability)
        }

    open fun copy(nullable: Boolean): XTypeName {
        // TODO(b/248633751): Handle primitive to boxed when becoming nullable?
        return XTypeName(
            java = java,
            kotlin =
                if (kotlin != UNAVAILABLE_KTYPE_NAME) {
                    kotlin.copy(nullable = nullable)
                } else {
                    UNAVAILABLE_KTYPE_NAME
                },
            nullability = if (nullable) XNullability.NULLABLE else XNullability.NONNULL,
        )
    }

    fun equalsIgnoreNullability(other: XTypeName): Boolean {
        return this.copy(nullable = false) == other.copy(nullable = false)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XTypeName) return false
        if (java != other.java) return false
        if (kotlin != UNAVAILABLE_KTYPE_NAME && other.kotlin != UNAVAILABLE_KTYPE_NAME) {
            if (kotlin != other.kotlin) return false
        }
        return true
    }

    override fun hashCode(): Int {
        return java.hashCode()
    }

    override fun toString() = buildString {
        append("XTypeName[")
        append(java)
        append(" / ")
        if (kotlin != UNAVAILABLE_KTYPE_NAME) {
            append(kotlin)
        } else {
            append("UNAVAILABLE")
        }
        append("]")
    }

    fun toString(codeLanguage: CodeLanguage) =
        when (codeLanguage) {
            CodeLanguage.JAVA -> java.toString()
            CodeLanguage.KOTLIN -> kotlin.toString()
        }

    companion object {
        /** A convenience [XTypeName] that represents [Unit] in Kotlin and `void` in Java. */
        @JvmField
        val UNIT_VOID = XTypeName(java = JTypeName.VOID, kotlin = com.squareup.kotlinpoet.UNIT)

        /** A convenience [XTypeName] that represents [Any] in Kotlin and [Object] in Java. */
        @JvmField
        val ANY_OBJECT = XTypeName(java = JTypeName.OBJECT, kotlin = com.squareup.kotlinpoet.ANY)

        /**
         * A convenience [XTypeName] that represents [Suppress] in Kotlin and [SuppressWarnings] in
         * Java.
         */
        @JvmField
        val SUPPRESS =
            XClassName(
                java = JClassName.get("java.lang", "SuppressWarnings"),
                kotlin = KClassName("kotlin", "Suppress"),
                nullability = XNullability.NONNULL,
            )

        /**
         * A convenience [XTypeName] that represents [kotlin.Enum] in Kotlin and [java.lang.Enum] in
         * Java.
         */
        @JvmField
        val ENUM =
            XTypeName(
                java = JClassName.get(java.lang.Enum::class.java),
                kotlin = com.squareup.kotlinpoet.ENUM,
            )

        @JvmField val STRING = String::class.asClassName()
        @JvmField val ITERABLE = Iterable::class.asClassName()
        @JvmField val COLLECTION = Collection::class.asClassName()
        @JvmField val LIST = List::class.asClassName()
        @JvmField val SET = Set::class.asClassName()
        @JvmField val MAP = Map::class.asClassName()
        @JvmField val MAP_ENTRY = Map.Entry::class.asClassName()

        @JvmField val MUTABLE_ITERABLE = Iterable::class.asMutableClassName()
        @JvmField val MUTABLE_COLLECTION = Collection::class.asMutableClassName()
        @JvmField val MUTABLE_LIST = List::class.asMutableClassName()
        @JvmField val MUTABLE_SET = Set::class.asMutableClassName()
        @JvmField val MUTABLE_MAP = Map::class.asMutableClassName()
        @JvmField val MUTABLE_MAP_ENTRY = Map.Entry::class.asMutableClassName()

        @JvmField val PRIMITIVE_BOOLEAN = Boolean::class.asPrimitiveTypeName()
        @JvmField val PRIMITIVE_BYTE = Byte::class.asPrimitiveTypeName()
        @JvmField val PRIMITIVE_SHORT = Short::class.asPrimitiveTypeName()
        @JvmField val PRIMITIVE_INT = Int::class.asPrimitiveTypeName()
        @JvmField val PRIMITIVE_LONG = Long::class.asPrimitiveTypeName()
        @JvmField val PRIMITIVE_CHAR = Char::class.asPrimitiveTypeName()
        @JvmField val PRIMITIVE_FLOAT = Float::class.asPrimitiveTypeName()
        @JvmField val PRIMITIVE_DOUBLE = Double::class.asPrimitiveTypeName()

        @JvmField val BOXED_BOOLEAN = Boolean::class.asClassName()
        @JvmField val BOXED_BYTE = Byte::class.asClassName()
        @JvmField val BOXED_SHORT = Short::class.asClassName()
        @JvmField val BOXED_INT = Int::class.asClassName()
        @JvmField val BOXED_LONG = Long::class.asClassName()
        @JvmField val BOXED_CHAR = Char::class.asClassName()
        @JvmField val BOXED_FLOAT = Float::class.asClassName()
        @JvmField val BOXED_DOUBLE = Double::class.asClassName()

        @JvmField
        val ANY_WILDCARD =
            XTypeName(
                java = JWildcardTypeName.subtypeOf(Object::class.java),
                kotlin = com.squareup.kotlinpoet.STAR,
            )

        /** The default [KTypeName] returned by xprocessing APIs when the backend is not KSP. */
        internal val UNAVAILABLE_KTYPE_NAME =
            KClassName("androidx.room.compiler.codegen", "Unavailable")

        operator fun invoke(
            java: JTypeName,
            kotlin: KTypeName,
            nullability: XNullability = XNullability.NONNULL,
        ): XTypeName {
            return XTypeName(java, kotlin, nullability)
        }

        /**
         * Gets a [XTypeName] that represents an array.
         *
         * If the [componentTypeName] is one of the primitive names, such as [PRIMITIVE_INT], then
         * the equivalent Kotlin and Java type names are represented, [IntArray] and `int[]`
         * respectively.
         */
        @JvmStatic
        fun getArrayName(componentTypeName: XTypeName): XTypeName {
            componentTypeName.java.let {
                require(it !is JWildcardTypeName || it.lowerBounds.isEmpty()) {
                    "Can't have contra-variant component types in Java arrays. Found '$it'."
                }
            }

            val (java, kotlin) =
                when (componentTypeName) {
                    PRIMITIVE_BOOLEAN -> JArrayTypeName.of(JTypeName.BOOLEAN) to BOOLEAN_ARRAY
                    PRIMITIVE_BYTE -> JArrayTypeName.of(JTypeName.BYTE) to BYTE_ARRAY
                    PRIMITIVE_SHORT -> JArrayTypeName.of(JTypeName.SHORT) to SHORT_ARRAY
                    PRIMITIVE_INT -> JArrayTypeName.of(JTypeName.INT) to INT_ARRAY
                    PRIMITIVE_LONG -> JArrayTypeName.of(JTypeName.LONG) to LONG_ARRAY
                    PRIMITIVE_CHAR -> JArrayTypeName.of(JTypeName.CHAR) to CHAR_ARRAY
                    PRIMITIVE_FLOAT -> JArrayTypeName.of(JTypeName.FLOAT) to FLOAT_ARRAY
                    PRIMITIVE_DOUBLE -> JArrayTypeName.of(JTypeName.DOUBLE) to DOUBLE_ARRAY
                    else -> {
                        componentTypeName.java.let {
                            if (it is JWildcardTypeName) {
                                JArrayTypeName.of(it.upperBounds.single())
                            } else {
                                JArrayTypeName.of(it)
                            }
                        } to ARRAY.parameterizedBy(componentTypeName.kotlin)
                    }
                }
            return XTypeName(
                java = java,
                kotlin =
                    if (componentTypeName.kotlin != UNAVAILABLE_KTYPE_NAME) {
                        kotlin
                    } else {
                        UNAVAILABLE_KTYPE_NAME
                    },
            )
        }

        /**
         * Create a contravariant wildcard type name, to use as a consumer site-variance
         * declaration.
         *
         * In Java: `? super <bound>` In Kotlin `in <bound>
         */
        @JvmStatic
        fun getConsumerSuperName(bound: XTypeName): XTypeName {
            return XTypeName(
                java = JWildcardTypeName.supertypeOf(bound.java),
                kotlin =
                    if (bound.kotlin != UNAVAILABLE_KTYPE_NAME) {
                        KWildcardTypeName.consumerOf(bound.kotlin)
                    } else {
                        UNAVAILABLE_KTYPE_NAME
                    },
            )
        }

        /**
         * Create a covariant wildcard type name, to use as a producer site-variance declaration.
         *
         * In Java: `? extends <bound>` In Kotlin `out <bound>
         */
        @JvmStatic
        fun getProducerExtendsName(bound: XTypeName): XTypeName {
            return XTypeName(
                java = JWildcardTypeName.subtypeOf(bound.java),
                kotlin =
                    if (bound.kotlin != UNAVAILABLE_KTYPE_NAME) {
                        KWildcardTypeName.producerOf(bound.kotlin)
                    } else {
                        UNAVAILABLE_KTYPE_NAME
                    },
            )
        }

        /** Creates a type variable named with bounds. */
        @JvmStatic
        fun getTypeVariableName(name: String, bounds: List<XTypeName> = emptyList()): XTypeName {
            return XTypeName(
                java = JTypeVariableName.get(name, *bounds.map { it.java }.toTypedArray()),
                kotlin = KTypeVariableName(name, bounds.map { it.kotlin }),
            )
        }
    }
}

/**
 * Creates a [XTypeName] whose JavaPoet name is a primitive name and KotlinPoet is the interop type.
 *
 * This function is useful since [asClassName] only supports creating class names and specifically
 * only the boxed version of primitives.
 */
internal fun KClass<*>.asPrimitiveTypeName(): XTypeName {
    require(this.java.isPrimitive) { "$this does not represent a primitive." }
    val jTypeName = getPrimitiveJTypeName(this.java)
    val kTypeName = this.asKTypeName()
    return XTypeName(jTypeName, kTypeName)
}

private fun getPrimitiveJTypeName(klass: Class<*>): JTypeName =
    when (klass) {
        java.lang.Void.TYPE -> JTypeName.VOID
        java.lang.Boolean.TYPE -> JTypeName.BOOLEAN
        java.lang.Byte.TYPE -> JTypeName.BYTE
        java.lang.Short.TYPE -> JTypeName.SHORT
        java.lang.Integer.TYPE -> JTypeName.INT
        java.lang.Long.TYPE -> JTypeName.LONG
        java.lang.Character.TYPE -> JTypeName.CHAR
        java.lang.Float.TYPE -> JTypeName.FLOAT
        java.lang.Double.TYPE -> JTypeName.DOUBLE
        else -> error("Can't get JTypeName from java.lang.Class: $klass")
    }

fun XTypeName.box() = XTypeName(java.box(), kotlin)

fun XTypeName.unbox() = XTypeName(java.unbox(), kotlin.copy(nullable = false), XNullability.NONNULL)

fun XTypeName.toJavaPoet(): JTypeName = this.java

fun XClassName.toJavaPoet(): JClassName = this.java

fun XTypeName.toKotlinPoet(): KTypeName = this.kotlin

fun XClassName.toKotlinPoet(): KClassName = this.kotlin
