/*
 * Copyright (C) 2016 The Android Open Source Project
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
import androidx.room.ext.type
import com.google.auto.common.MoreTypes
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeKind.ARRAY
import javax.lang.model.type.TypeKind.BOOLEAN
import javax.lang.model.type.TypeKind.BYTE
import javax.lang.model.type.TypeKind.CHAR
import javax.lang.model.type.TypeKind.DECLARED
import javax.lang.model.type.TypeKind.DOUBLE
import javax.lang.model.type.TypeKind.FLOAT
import javax.lang.model.type.TypeKind.INT
import javax.lang.model.type.TypeKind.LONG
import javax.lang.model.type.TypeKind.SHORT
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

fun TypeMirror.defaultValue(): String {
    return when (this.kind) {
        BOOLEAN -> "false"
        BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE -> "0"
        else -> "null"
    }
}

fun TypeMirror.box(typeUtils: Types) = if (this.kind.isPrimitive) {
    typeUtils.boxedClass(this as PrimitiveType).type
} else {
    this
}
fun TypeMirror.asTypeElement(): TypeElement = MoreTypes.asTypeElement(this)

fun TypeMirror.asElement(): Element = MoreTypes.asElement(this)

fun TypeMirror.isPrimitiveInt() = kind == TypeKind.INT

fun TypeMirror.isPrimitive() = kind.isPrimitive

fun TypeMirror.isBoxedInt() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(java.lang.Integer::class.java, this)

fun TypeMirror.isInt() = isPrimitiveInt() || isBoxedInt()

fun TypeMirror.isPrimitiveLong() = kind == TypeKind.LONG

fun TypeMirror.isBoxedLong() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(java.lang.Long::class.java, this)

fun TypeMirror.isLong() = isPrimitiveLong() || isBoxedLong()

fun TypeMirror.isList() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(List::class.java, this)

fun TypeMirror.isVoid() = kind == TypeKind.VOID

fun TypeMirror.isNotVoid() = !isVoid()

fun TypeMirror.isError() = kind == TypeKind.ERROR

fun TypeMirror.isNotError() = !isError()

fun TypeMirror.isNone() = kind == TypeKind.NONE

fun TypeMirror.isNotNone() = !isNone()

fun TypeMirror.isByte() = kind == BYTE

fun TypeMirror.isNotByte() = !isByte()

@OptIn(ExperimentalContracts::class)
fun TypeMirror.isDeclared(): Boolean {
    contract {
        returns(true) implies (this@isDeclared is DeclaredType)
    }
    return kind == DECLARED
}

fun TypeMirror.isVoidObject() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(Void::class.java, this)

fun TypeMirror.isKotlinUnit() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(Unit::class.java, this)

fun TypeMirror.isAssignableFrom(typeUtils: Types, other: TypeMirror): Boolean {
    return typeUtils.isAssignable(other, this)
}

fun TypeMirror.asDeclaredType() = MoreTypes.asDeclared(this)

fun TypeMirror.isType() = MoreTypes.isType(this)

fun TypeMirror.isTypeOf(klass: KClass<*>) = MoreTypes.isTypeOf(
    klass.java,
    this
)

fun TypeMirror.isArray(): Boolean {
    contract {
        returns(true) implies (this@isArray is ArrayType)
    }
    return kind == ARRAY
}

fun TypeMirror.asArray() = MoreTypes.asArray(this)

fun TypeMirror.asPrimitive() = MoreTypes.asPrimitiveType(this)

fun TypeMirror.isSameType(utils: Types, other: TypeMirror) = utils.isSameType(this, other)

fun TypeMirror.erasure(utils: Types) = utils.erasure(this)
