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
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.javapoet.JClassName
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XTypeName) return false
        if (java != other.java) return false
        if (kotlin != other.kotlin) return false
        return true
    }

    override fun hashCode(): Int {
        var result = java.hashCode()
        result = 31 * result + kotlin.hashCode()
        return result
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
        /**
         * The default [KTypeName] returned by xprocessing APIs when the backend is not KSP.
         */
        internal val UNAVAILABLE_KTYPE_NAME =
            KClassName("androidx.room.compiler.codegen", "Unavailable")

        operator fun invoke(
            java: JTypeName,
            kotlin: KTypeName,
            nullability: XNullability
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

    fun copy(nullable: Boolean): XClassName {
        return XClassName(
            java = java,
            kotlin = kotlin.copy(nullable = nullable) as KClassName,
            nullability = if (nullable) XNullability.NULLABLE else XNullability.NONNULL
        )
    }

    companion object {
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

@OptIn(DelicateKotlinPoetApi::class)
fun Class<*>.asClassName(): XClassName {
    return XClassName(
        java = JClassName.get(this),
        kotlin = this.asClassName(),
        nullability = XNullability.NONNULL
    )
}

fun KClass<*>.asClassName(): XClassName {
    return XClassName(
        java = JClassName.get(this.java),
        kotlin = this.asClassName(),
        nullability = XNullability.NONNULL
    )
}

fun XTypeName.toJavaPoet() = this.java
fun XClassName.toJavaPoet() = this.java