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
import com.google.auto.common.MoreTypes
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeKind.BOOLEAN
import javax.lang.model.type.TypeKind.BYTE
import javax.lang.model.type.TypeKind.CHAR
import javax.lang.model.type.TypeKind.DOUBLE
import javax.lang.model.type.TypeKind.FLOAT
import javax.lang.model.type.TypeKind.INT
import javax.lang.model.type.TypeKind.LONG
import javax.lang.model.type.TypeKind.SHORT
import javax.lang.model.type.TypeMirror

fun TypeMirror.defaultValue(): String {
    return when (this.kind) {
        BOOLEAN -> "false"
        BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE -> "0"
        else -> "null"
    }
}

fun TypeMirror.asTypeElement(): TypeElement = MoreTypes.asTypeElement(this)

fun TypeMirror.isPrimitiveInt() = kind == TypeKind.INT

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

fun TypeMirror.isVoidObject() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(Void::class.java, this)

fun TypeMirror.isKotlinUnit() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(Unit::class.java, this)