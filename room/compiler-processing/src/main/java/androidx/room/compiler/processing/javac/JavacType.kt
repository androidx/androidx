/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XRawType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.javac.kotlin.KmType
import androidx.room.compiler.processing.ksp.ERROR_TYPE_NAME
import androidx.room.compiler.processing.safeTypeName
import com.google.auto.common.MoreTypes
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

internal abstract class JavacType(
    protected val env: JavacProcessingEnv,
    open val typeMirror: TypeMirror
) : XType, XEquality {
    // Kotlin type information about the type if this type is driven from kotlin code.
    abstract val kotlinType: KmType?

    override val rawType: XRawType by lazy {
        JavacRawType(env, this)
    }

    override val typeElement by lazy {
        val element = try {
            MoreTypes.asTypeElement(typeMirror)
        } catch (notAnElement: IllegalArgumentException) {
            null
        }
        element?.let {
            env.wrapTypeElement(it)
        }
    }

    override fun isError(): Boolean {
        return typeMirror.kind == TypeKind.ERROR ||
            // https://kotlinlang.org/docs/reference/kapt.html#non-existent-type-correction
            (kotlinType != null && typeName == ERROR_TYPE_NAME)
    }

    override val typeName by lazy {
        typeMirror.safeTypeName()
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun defaultValue(): String {
        return when (typeMirror.kind) {
            TypeKind.BOOLEAN -> "false"
            TypeKind.BYTE, TypeKind.SHORT, TypeKind.INT, TypeKind.LONG, TypeKind.CHAR -> "0"
            TypeKind.FLOAT -> "0f"
            TypeKind.DOUBLE -> "0.0"
            else -> "null"
        }
    }

    override fun boxed(): JavacType {
        return when {
            typeMirror.kind.isPrimitive -> {
                env.wrap(
                    typeMirror = env.typeUtils.boxedClass(MoreTypes.asPrimitiveType(typeMirror))
                        .asType(),
                    kotlinType = kotlinType,
                    elementNullability = XNullability.NULLABLE
                )
            }
            typeMirror.kind == TypeKind.VOID -> {
                env.wrap(
                    typeMirror = env.elementUtils.getTypeElement("java.lang.Void").asType(),
                    kotlinType = kotlinType,
                    elementNullability = XNullability.NULLABLE
                )
            }
            else -> {
                this
            }
        }
    }

    override fun isNone() = typeMirror.kind == TypeKind.NONE

    override fun toString(): String {
        return typeMirror.toString()
    }

    override fun extendsBound(): XType? {
        return typeMirror.extendsBound()?.let {
            env.wrap<JavacType>(
                typeMirror = it,
                kotlinType = kotlinType?.extendsBound,
                elementNullability = nullability
            )
        }
    }

    override fun isAssignableFrom(other: XType): Boolean {
        return other is JavacType && env.typeUtils.isAssignable(
            other.typeMirror,
            typeMirror
        )
    }

    override fun isTypeOf(other: KClass<*>): Boolean {
        return try {
            MoreTypes.isTypeOf(
                other.java,
                typeMirror
            )
        } catch (notAType: IllegalArgumentException) {
            // `MoreTypes.isTypeOf` might throw if the current TypeMirror is not a type.
            // for Room, a `false` response is good enough.
            false
        }
    }

    override fun isSameType(other: XType): Boolean {
        return other is JavacType && env.typeUtils.isSameType(typeMirror, other.typeMirror)
    }

    /**
     * Create a copy of this type with the given nullability.
     * This method is not called if the nullability of the type is already equal to the given
     * nullability.
     */
    protected abstract fun copyWithNullability(nullability: XNullability): JavacType

    final override fun makeNullable(): JavacType {
        if (nullability == XNullability.NULLABLE) {
            return this
        }
        if (typeMirror.kind.isPrimitive || typeMirror.kind == TypeKind.VOID) {
            return boxed().makeNullable()
        }
        return copyWithNullability(XNullability.NULLABLE)
    }

    final override fun makeNonNullable(): JavacType {
        if (nullability == XNullability.NONNULL) {
            return this
        }
        // unlike makeNullable, we don't try to degrade to primitives here because it is valid for
        // a boxed primitive to be marked as non-null.
        return copyWithNullability(XNullability.NONNULL)
    }
}
