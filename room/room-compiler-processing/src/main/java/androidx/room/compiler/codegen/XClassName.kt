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

package androidx.room.compiler.codegen

import androidx.room.compiler.processing.XNullability
import com.squareup.kotlinpoet.MUTABLE_COLLECTION
import com.squareup.kotlinpoet.MUTABLE_ITERABLE
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_MAP_ENTRY
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName as asKClassName
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KClassName
import kotlin.reflect.KClass

/**
 * Represents a fully-qualified class name.
 *
 * It simply contains a [com.squareup.javapoet.ClassName] and a [com.squareup.kotlinpoet.ClassName].
 *
 * @see [androidx.room.compiler.processing.XTypeElement.asClassName]
 */
class XClassName
internal constructor(
    override val java: JClassName,
    override val kotlin: KClassName,
    nullability: XNullability,
) : XTypeName(java, kotlin, nullability) {

    // TODO(b/248000692): Using the JClassName as source of truth. This is wrong since we need to
    //  handle Kotlin interop types for KotlinPoet, i.e. java.lang.String to kotlin.String.
    //  But a decision has to be made...
    val packageName: String = java.packageName()
    val simpleNames: List<String> = java.simpleNames()
    val simpleName = simpleNames.last()
    val canonicalName: String = java.canonicalName()
    val reflectionName: String = java.reflectionName()

    /**
     * Returns a parameterized type, applying the `typeArguments` to `this`.
     *
     * @see [XTypeName.rawTypeName]
     */
    fun parametrizedBy(vararg typeArguments: XTypeName): XTypeName {
        return XTypeName(
            java = JParameterizedTypeName.get(java, *typeArguments.map { it.java }.toTypedArray()),
            kotlin =
                if (
                    kotlin != UNAVAILABLE_KTYPE_NAME &&
                        typeArguments.none { it.kotlin == UNAVAILABLE_KTYPE_NAME }
                ) {
                    kotlin.parameterizedBy(typeArguments.map { it.kotlin })
                } else {
                    UNAVAILABLE_KTYPE_NAME
                },
        )
    }

    fun nestedClass(name: String) =
        XClassName(
            java = java.nestedClass(name),
            kotlin = kotlin.nestedClass(name),
            nullability = XNullability.NONNULL,
        )

    fun peerClass(name: String) =
        XClassName(
            java = java.peerClass(name),
            kotlin = kotlin.peerClass(name),
            nullability = XNullability.NONNULL,
        )

    fun enclosingClassName(): XClassName? {
        return if (java.enclosingClassName() != null && kotlin.enclosingClassName() != null) {
            XClassName(
                java = java.enclosingClassName(),
                kotlin = kotlin.enclosingClassName()!!,
                nullability = XNullability.NONNULL,
            )
        } else {
            check(java.enclosingClassName() == null)
            check(kotlin.enclosingClassName() == null)
            null
        }
    }

    fun topLevelClass() =
        XClassName(
            java = java.topLevelClassName(),
            kotlin = kotlin.topLevelClassName(),
            nullability = XNullability.NONNULL,
        )

    override fun copy(nullable: Boolean): XClassName {
        return XClassName(
            java = java,
            kotlin =
                if (kotlin != UNAVAILABLE_KTYPE_NAME) {
                    kotlin.copy(nullable = nullable) as KClassName
                } else {
                    UNAVAILABLE_KTYPE_NAME
                },
            nullability = if (nullable) XNullability.NULLABLE else XNullability.NONNULL,
        )
    }

    companion object {
        // TODO(b/248633751): Handle interop types.
        @JvmStatic
        fun get(packageName: String, vararg names: String): XClassName {
            return XClassName(
                java = JClassName.get(packageName, names.first(), *names.drop(1).toTypedArray()),
                kotlin = KClassName(packageName, *names),
                nullability = XNullability.NONNULL,
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
 * When the receiver [KClass] is a Kotlin interop collection, such as [kotlin.collections.List] then
 * the returned [XClassName] contains the corresponding JavaPoet class name. See:
 * https://kotlinlang.org/docs/reference/java-interop.html#mapped-types.
 *
 * When the receiver [KClass] is a Kotlin mutable collection, such as
 * [kotlin.collections.MutableList] then the non-mutable [XClassName] is returned due to the mutable
 * interfaces only existing at compile-time, see: https://youtrack.jetbrains.com/issue/KT-11754.
 *
 * If the mutable [XClassName] is needed, use [asMutableClassName].
 */
fun KClass<*>.asClassName(): XClassName {
    val jClassName =
        if (this.java.isPrimitive) {
            getBoxedJClassName(this.java)
        } else {
            JClassName.get(this.java)
        }
    val kClassName = this.asKClassName()
    return XClassName(java = jClassName, kotlin = kClassName, nullability = XNullability.NONNULL)
}

/**
 * Creates a mutable [XClassName] from the receiver [KClass]
 *
 * This is a workaround for: https://github.com/square/kotlinpoet/issues/279
 * https://youtrack.jetbrains.com/issue/KT-11754
 *
 * When the receiver [KClass] is a Kotlin interop collection, such as [kotlin.collections.List] then
 * the returned [XClassName] contains the corresponding JavaPoet class name. See:
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
    val kotlin =
        when (this) {
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

private fun getBoxedJClassName(klass: Class<*>): JClassName =
    when (klass) {
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
    }
        as JClassName
