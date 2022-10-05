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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KClassName
import com.squareup.kotlinpoet.javapoet.KTypeName
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
    internal val nullability: XNullability
) {
    val isPrimitive: Boolean
        get() = java.isPrimitive

    open fun copy(nullable: Boolean): XTypeName {
        return XTypeName(
            java = java,
            kotlin = kotlin.copy(nullable = nullable),
            nullability = if (nullable) XNullability.NULLABLE else XNullability.NONNULL
        )
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

    companion object {
        val PRIMITIVE_BOOLEAN = Boolean::class.asPrimitiveTypeName()
        val PRIMITIVE_BYTE = Byte::class.asPrimitiveTypeName()
        val PRIMITIVE_SHORT = Short::class.asPrimitiveTypeName()
        val PRIMITIVE_INT = Int::class.asPrimitiveTypeName()
        val PRIMITIVE_LONG = Long::class.asPrimitiveTypeName()
        val PRIMITIVE_CHAR = Char::class.asPrimitiveTypeName()
        val PRIMITIVE_FLOAT = Float::class.asPrimitiveTypeName()
        val PRIMITIVE_DOUBLE = Double::class.asPrimitiveTypeName()

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

    fun parametrizedBy(
        vararg typeArguments: XTypeName,
    ): XTypeName {
        return XTypeName(
            java = JParameterizedTypeName.get(java, *typeArguments.map { it.java }.toTypedArray()),
            kotlin = kotlin.parameterizedBy(typeArguments.map { it.kotlin })
        )
    }

    override fun copy(nullable: Boolean): XClassName {
        return XClassName(
            java = java,
            kotlin = kotlin.copy(nullable = nullable) as KClassName,
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
 * then the returned [XClassName] the corresponding JavaPoet class name. See:
 * https://kotlinlang.org/docs/reference/java-interop.html#mapped-types.
 *
 * When the receiver [KClass] is a Kotlin mutable collection, such as
 * [kotlin.collections.MutableList] then the non-mutable [XClassName] is returned due to the
 * mutable interfaces only existing at compile-time, see:
 * https://youtrack.jetbrains.com/issue/KT-11754.
 */
fun KClass<*>.asClassName(): XClassName {
    val jClassName = if (this.java.isPrimitive) {
        getBoxedJClassName(this.java)
    } else {
        JClassName.get(this.java)
    }
    val kClassName = this.asClassName()
    return XClassName(
        java = jClassName,
        kotlin = kClassName,
        nullability = XNullability.NONNULL
    )
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
    val kTypeName = this.asTypeName()
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

fun XTypeName.toJavaPoet(): JTypeName = this.java
fun XClassName.toJavaPoet(): JClassName = this.java