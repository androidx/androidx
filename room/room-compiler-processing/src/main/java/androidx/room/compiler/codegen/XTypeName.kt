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
import com.squareup.kotlinpoet.MUTABLE_COLLECTION
import com.squareup.kotlinpoet.MUTABLE_ITERABLE
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_MAP_ENTRY
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.asClassName as asKClassName
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
 * If the name comes from xprocessing APIs then the KotlinPoet name will default to 'Unavailable'
 * if the processing backend is not KSP.
 *
 * @see [androidx.room.compiler.processing.XType.asTypeName]
 */
open class XTypeName protected constructor(
    internal open val java: JTypeName,
    internal open val kotlin: KTypeName,
    val nullability: XNullability
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
            val javaRawType = java.let {
                if (it is JParameterizedTypeName) it.rawType else it
            }
            val kotlinRawType = kotlin.let {
                if (it is KParameterizedTypeName) it.rawType else it
            }
            return XTypeName(javaRawType, kotlinRawType, nullability)
        }

    open fun copy(nullable: Boolean): XTypeName {
        // TODO(b/248633751): Handle primitive to boxed when becoming nullable?
        return XTypeName(
            java = java,
            kotlin = if (kotlin != UNAVAILABLE_KTYPE_NAME) {
                kotlin.copy(nullable = nullable)
            } else {
                UNAVAILABLE_KTYPE_NAME
            },
            nullability = if (nullable) XNullability.NULLABLE else XNullability.NONNULL
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

    fun toString(codeLanguage: CodeLanguage) = when (codeLanguage) {
        CodeLanguage.JAVA -> java.toString()
        CodeLanguage.KOTLIN -> kotlin.toString()
    }

    companion object {
        /**
         * A convenience [XTypeName] that represents [Unit] in Kotlin and `void` in Java.
         */
        val UNIT_VOID = XTypeName(
            java = JTypeName.VOID,
            kotlin = com.squareup.kotlinpoet.UNIT
        )

        /**
         * A convenience [XTypeName] that represents [Any] in Kotlin and [Object] in Java.
         */
        val ANY_OBJECT = XTypeName(
            java = JTypeName.OBJECT,
            kotlin = com.squareup.kotlinpoet.ANY
        )

        val PRIMITIVE_BOOLEAN = Boolean::class.asPrimitiveTypeName()
        val PRIMITIVE_BYTE = Byte::class.asPrimitiveTypeName()
        val PRIMITIVE_SHORT = Short::class.asPrimitiveTypeName()
        val PRIMITIVE_INT = Int::class.asPrimitiveTypeName()
        val PRIMITIVE_LONG = Long::class.asPrimitiveTypeName()
        val PRIMITIVE_CHAR = Char::class.asPrimitiveTypeName()
        val PRIMITIVE_FLOAT = Float::class.asPrimitiveTypeName()
        val PRIMITIVE_DOUBLE = Double::class.asPrimitiveTypeName()

        val BOXED_BOOLEAN = Boolean::class.asClassName()
        val BOXED_BYTE = Byte::class.asClassName()
        val BOXED_SHORT = Short::class.asClassName()
        val BOXED_INT = Int::class.asClassName()
        val BOXED_LONG = Long::class.asClassName()
        val BOXED_CHAR = Char::class.asClassName()
        val BOXED_FLOAT = Float::class.asClassName()
        val BOXED_DOUBLE = Double::class.asClassName()

        val ANY_WILDCARD = XTypeName(
            java = JWildcardTypeName.subtypeOf(Object::class.java),
            kotlin = com.squareup.kotlinpoet.STAR
        )

        /**
         * The default [KTypeName] returned by xprocessing APIs when the backend is not KSP.
         */
        internal val UNAVAILABLE_KTYPE_NAME =
            KClassName("androidx.room.compiler.codegen", "Unavailable")

        operator fun invoke(
            java: JTypeName,
            kotlin: KTypeName,
            nullability: XNullability = XNullability.NONNULL
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
        fun getArrayName(componentTypeName: XTypeName): XTypeName {
            val (java, kotlin) = when (componentTypeName) {
                PRIMITIVE_BOOLEAN ->
                    JArrayTypeName.of(JTypeName.BOOLEAN) to BOOLEAN_ARRAY
                PRIMITIVE_BYTE ->
                    JArrayTypeName.of(JTypeName.BYTE) to BYTE_ARRAY
                PRIMITIVE_SHORT ->
                    JArrayTypeName.of(JTypeName.SHORT) to SHORT_ARRAY
                PRIMITIVE_INT ->
                    JArrayTypeName.of(JTypeName.INT) to INT_ARRAY
                PRIMITIVE_LONG ->
                    JArrayTypeName.of(JTypeName.LONG) to LONG_ARRAY
                PRIMITIVE_CHAR ->
                    JArrayTypeName.of(JTypeName.CHAR) to CHAR_ARRAY
                PRIMITIVE_FLOAT ->
                    JArrayTypeName.of(JTypeName.FLOAT) to FLOAT_ARRAY
                PRIMITIVE_DOUBLE ->
                    JArrayTypeName.of(JTypeName.DOUBLE) to DOUBLE_ARRAY
                else ->
                    JArrayTypeName.of(componentTypeName.java) to
                        ARRAY.parameterizedBy(componentTypeName.kotlin)
            }
            return XTypeName(
                java = java,
                kotlin = if (componentTypeName.kotlin != UNAVAILABLE_KTYPE_NAME) {
                    kotlin
                } else {
                    UNAVAILABLE_KTYPE_NAME
                }
            )
        }

        /**
         * Create a contravariant wildcard type name, to use as a consumer site-variance
         * declaration.
         *
         * In Java: `? super <bound>`
         * In Kotlin `in <bound>
         */
        fun getConsumerSuperName(bound: XTypeName): XTypeName {
            return XTypeName(
                java = JWildcardTypeName.supertypeOf(bound.java),
                kotlin = if (bound.kotlin != UNAVAILABLE_KTYPE_NAME) {
                    KWildcardTypeName.consumerOf(bound.kotlin)
                } else {
                    UNAVAILABLE_KTYPE_NAME
                }
            )
        }

        /**
         * Create a covariant wildcard type name, to use as a producer site-variance
         * declaration.
         *
         * In Java: `? extends <bound>`
         * In Kotlin `out <bound>
         */
        fun getProducerExtendsName(bound: XTypeName): XTypeName {
            return XTypeName(
                java = JWildcardTypeName.subtypeOf(bound.java),
                kotlin = if (bound.kotlin != UNAVAILABLE_KTYPE_NAME) {
                    KWildcardTypeName.producerOf(bound.kotlin)
                } else {
                    UNAVAILABLE_KTYPE_NAME
                }
            )
        }

        /**
         * Creates a type variable named with bounds.
         */
        fun getTypeVariableName(name: String, bounds: List<XTypeName> = emptyList()): XTypeName {
            return XTypeName(
                java = JTypeVariableName.get(name, *bounds.map { it.java }.toTypedArray()),
                kotlin = KTypeVariableName(name, bounds.map { it.kotlin })
            )
        }
    }
}

/**
 * Represents a fully-qualified class name.
 *
 * It simply contains a [com.squareup.javapoet.ClassName] and a [com.squareup.kotlinpoet.ClassName].
 *
 * @see [androidx.room.compiler.processing.XTypeElement.asClassName]
 */
class XClassName internal constructor(
    override val java: JClassName,
    override val kotlin: KClassName,
    nullability: XNullability
) : XTypeName(java, kotlin, nullability) {

    // TODO(b/248000692): Using the JClassName as source of truth. This is wrong since we need to
    //  handle Kotlin interop types for KotlinPoet, i.e. java.lang.String to kotlin.String.
    //  But a decision has to be made...
    val packageName: String = java.packageName()
    val simpleNames: List<String> = java.simpleNames()
    val canonicalName: String = java.canonicalName()
    val reflectionName: String = java.reflectionName()

    /**
     * Returns a parameterized type, applying the `typeArguments` to `this`.
     *
     * @see [XTypeName.rawTypeName]
     */
    fun parametrizedBy(
        vararg typeArguments: XTypeName,
    ): XTypeName {
        return XTypeName(
            java = JParameterizedTypeName.get(java, *typeArguments.map { it.java }.toTypedArray()),
            kotlin = if (
                kotlin != UNAVAILABLE_KTYPE_NAME &&
                typeArguments.none { it.kotlin == UNAVAILABLE_KTYPE_NAME }
            ) {
                kotlin.parameterizedBy(typeArguments.map { it.kotlin })
            } else {
                UNAVAILABLE_KTYPE_NAME
            }
        )
    }

    override fun copy(nullable: Boolean): XClassName {
        return XClassName(
            java = java,
            kotlin = if (kotlin != UNAVAILABLE_KTYPE_NAME) {
                kotlin.copy(nullable = nullable) as KClassName
            } else {
                UNAVAILABLE_KTYPE_NAME
            },
            nullability = if (nullable) XNullability.NULLABLE else XNullability.NONNULL
        )
    }

    companion object {
        /**
         * Creates an class name from the given parts.
         */
        // TODO(b/248633751): Handle interop types.
        fun get(
            packageName: String,
            vararg names: String
        ): XClassName {
            return XClassName(
                java = JClassName.get(packageName, names.first(), *names.drop(1).toTypedArray()),
                kotlin = KClassName(packageName, *names),
                nullability = XNullability.NONNULL
            )
        }
    }
}

/**
 * Creates a [XClassName] from the receiver [KClass]
 *
 * When the receiver [KClass] is a Kotlin interop primitive, such as [kotlin.Int] then the returned
 * [XClassName] contains the boxed JavaPoet class name.
 *
 * When the receiver [KClass] is a Kotlin interop collection, such as [kotlin.collections.List]
 * then the returned [XClassName] contains the corresponding JavaPoet class name. See:
 * https://kotlinlang.org/docs/reference/java-interop.html#mapped-types.
 *
 * When the receiver [KClass] is a Kotlin mutable collection, such as
 * [kotlin.collections.MutableList] then the non-mutable [XClassName] is returned due to the
 * mutable interfaces only existing at compile-time, see:
 * https://youtrack.jetbrains.com/issue/KT-11754.
 *
 * If the mutable [XClassName] is needed, use [asMutableClassName].
 */
fun KClass<*>.asClassName(): XClassName {
    val jClassName = if (this.java.isPrimitive) {
        getBoxedJClassName(this.java)
    } else {
        JClassName.get(this.java)
    }
    val kClassName = this.asKClassName()
    return XClassName(
        java = jClassName,
        kotlin = kClassName,
        nullability = XNullability.NONNULL
    )
}

/**
 * Creates a mutable [XClassName] from the receiver [KClass]
 *
 * This is a workaround for:
 * https://github.com/square/kotlinpoet/issues/279
 * https://youtrack.jetbrains.com/issue/KT-11754
 *
 * When the receiver [KClass] is a Kotlin interop collection, such as [kotlin.collections.List]
 * then the returned [XClassName] contains the corresponding JavaPoet class name. See:
 * https://kotlinlang.org/docs/reference/java-interop.html#mapped-types.
 *
 * When the receiver [KClass] is a Kotlin mutable collection, such as
 * [kotlin.collections.MutableList] then the returned [XClassName] contains the corresponding
 * KotlinPoet class name.
 *
 * If an equivalent interop [XClassName] mapping for a Kotlin mutable Kotlin collection receiver
 * [KClass] is not found, the method will error out.
 */
fun KClass<*>.asMutableClassName(): XClassName {
    val java = JClassName.get(this.java)
    val kotlin = when (this) {
        Iterable::class -> MUTABLE_ITERABLE
        Collection::class -> MUTABLE_COLLECTION
        List::class -> MUTABLE_LIST
        Set::class -> MUTABLE_SET
        Map::class -> MUTABLE_MAP
        Map.Entry::class -> MUTABLE_MAP_ENTRY
        else -> error("No equivalent mutable Kotlin interop found for `$this`.")
    }
    return XClassName(java, kotlin, XNullability.NONNULL)
}

private fun getBoxedJClassName(klass: Class<*>): JClassName = when (klass) {
    java.lang.Void.TYPE -> JTypeName.VOID.box()
    java.lang.Boolean.TYPE -> JTypeName.BOOLEAN.box()
    java.lang.Byte.TYPE -> JTypeName.BYTE.box()
    java.lang.Short.TYPE -> JTypeName.SHORT.box()
    java.lang.Integer.TYPE -> JTypeName.INT.box()
    java.lang.Long.TYPE -> JTypeName.LONG.box()
    java.lang.Character.TYPE -> JTypeName.CHAR.box()
    java.lang.Float.TYPE -> JTypeName.FLOAT.box()
    java.lang.Double.TYPE -> JTypeName.DOUBLE.box()
    else -> error("Can't get JTypeName from java.lang.Class: $klass")
} as JClassName

/**
 * Creates a [XTypeName] whose JavaPoet name is a primitive name and KotlinPoet is the interop type.
 *
 * This function is useful since [asClassName] only supports creating class names and specifically
 * only the boxed version of primitives.
 */
internal fun KClass<*>.asPrimitiveTypeName(): XTypeName {
    require(this.java.isPrimitive) {
        "$this does not represent a primitive."
    }
    val jTypeName = getPrimitiveJTypeName(this.java)
    val kTypeName = this.asKTypeName()
    return XTypeName(jTypeName, kTypeName)
}

private fun getPrimitiveJTypeName(klass: Class<*>): JTypeName = when (klass) {
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
