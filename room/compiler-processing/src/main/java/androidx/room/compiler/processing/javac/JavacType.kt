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
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.safeTypeName
import com.google.auto.common.MoreTypes
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.lang.model.util.SimpleTypeVisitor7
import kotlin.reflect.KClass

// TODO make this abstract for XEquality, not open
internal abstract class JavacType(
    protected val env: JavacProcessingEnv,
    open val typeMirror: TypeMirror
) : XType, XEquality {

    override fun isError() = typeMirror.kind == TypeKind.ERROR

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

    override fun boxed(): XType {
        return if (typeMirror.kind.isPrimitive) {
            env.wrap(
                env.typeUtils.boxedClass(MoreTypes.asPrimitiveType(typeMirror)).asType()
            )
        } else {
            this
        }
    }

    override fun asTypeElement(): XTypeElement {
        return env.wrapTypeElement(
            MoreTypes.asTypeElement(typeMirror)
        )
    }

    override fun isNone() = typeMirror.kind == TypeKind.NONE

    override fun toString(): String {
        return typeMirror.toString()
    }

    override fun extendsBound(): XType? {
        return typeMirror.extendsBound()?.let {
            env.wrap<JavacType>(it)
        }
    }

    private fun TypeMirror.extendsBound(): TypeMirror? {
        return this.accept(object : SimpleTypeVisitor7<TypeMirror?, Void?>() {
            override fun visitWildcard(type: WildcardType, ignored: Void?): TypeMirror? {
                return type.extendsBound ?: type.superBound
            }
        }, null)
    }

    override fun isAssignableFrom(other: XType): Boolean {
        return other is JavacType && env.typeUtils.isAssignable(
            other.typeMirror,
            typeMirror
        )
    }

    override fun erasure(): JavacType {
        return env.wrap(env.typeUtils.erasure(typeMirror))
    }

    override fun isTypeOf(other: KClass<*>): Boolean {
        return MoreTypes.isTypeOf(
            other.java,
            typeMirror
        )
    }

    override fun isSameType(other: XType): Boolean {
        return other is JavacType && env.typeUtils.isSameType(typeMirror, other.typeMirror)
    }

    override fun isType(): Boolean {
        return MoreTypes.isType(typeMirror)
    }
}
