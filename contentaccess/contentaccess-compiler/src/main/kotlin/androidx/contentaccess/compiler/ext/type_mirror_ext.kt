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
import java.lang.RuntimeException
import java.util.Optional
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

fun TypeMirror.asTypeElement(): TypeElement = MoreTypes.asTypeElement(this)

fun TypeMirror.isSupportedWrapper(): Boolean {
    for (supportedType in SUPPORTED_RETURN_WRAPPERS) {
        if (MoreTypes.isTypeOf(supportedType, this)) {
            return true
        }
    }
    return false
}

fun TypeMirror.isVoidObject() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(Void::class.java, this)

// TODO(obenabde): this assumes we already know the type is among the supported types, make
// nullable eventually and let the caller decide whether it could be nullable
fun TypeMirror.extractSingleTypeArgument(processingEnv: ProcessingEnvironment):
        TypeMirror {
    return processingEnv.elementUtils.getTypeElement(this.toString()
        .substringAfter("<").substringBeforeLast(">")).asType()
}

fun TypeMirror.isPrimitiveInt() = kind == TypeKind.INT

fun TypeMirror.isBoxedInt() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(java.lang.Integer::class.java, this)

fun TypeMirror.isInt() = isPrimitiveInt() || isBoxedInt()

fun TypeMirror.isPrimitiveLong() = kind == TypeKind.LONG

fun TypeMirror.isBoxedLong() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(java.lang.Long::class.java, this)

fun TypeMirror.isLong() = isPrimitiveLong() || isBoxedLong()

fun TypeMirror.isPrimitiveFloat() = kind == TypeKind.FLOAT

fun TypeMirror.isBoxedFloat() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(java.lang.Float::class.java, this)

fun TypeMirror.isFloat() = isPrimitiveFloat() || isBoxedFloat()

fun TypeMirror.isPrimitiveShort() = kind == TypeKind.SHORT

fun TypeMirror.isBoxedShort() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(java.lang.Short::class.java, this)

fun TypeMirror.isShort() = isPrimitiveFloat() || isBoxedShort()

fun TypeMirror.isPrimitiveDouble() = kind == TypeKind.DOUBLE

fun TypeMirror.isBoxedDouble() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(java.lang.Double::class.java, this)

fun TypeMirror.isDouble() = isPrimitiveDouble() || isBoxedDouble()

fun TypeMirror.isString() = MoreTypes.isType(this) && MoreTypes.isTypeOf(String::class
    .java, this)

fun TypeMirror.isPrimitiveBlob() = MoreTypes.isType(this) && MoreTypes.isTypeOf(ByteArray::class
    .java, this)

fun TypeMirror.isBoxedBlob() = MoreTypes.isType(this) && MoreTypes.isTypeOf(Array<Byte>::class
    .java, this)

fun TypeMirror.isBlob() = isPrimitiveBlob() || isBoxedBlob()

fun TypeMirror.isPrimitiveBoolean() = kind == TypeKind.BOOLEAN

fun TypeMirror.isBoxedBoolean() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(java.lang.Boolean::class.java, this)

fun TypeMirror.isBoolean() = isPrimitiveBoolean() || isBoxedBoolean()

fun TypeMirror.isSupportedColumnType() = isBlob() || isInt() || isString() || isFloat() ||
        isDouble() || isShort() || isLong() || isBoolean()

fun TypeMirror.getCursorMethod(): String {
    if (isShort()) {
        return "getShort"
    } else if (isLong()) {
        return "getLong"
    } else if (isInt()) {
        return "getInt"
    } else if (isString()) {
        return "getString"
    } else if (isFloat()) {
        return "getFloat"
    } else if (isBlob()) {
        return "getBlob"
    } else if (isDouble()) {
        return "getDouble"
    } else if (isBoolean()) {
        return "getBoolean"
    }
    // This should honestly only ever be called after checking isSupportedColumnType() but you
    // never know.
    throw RuntimeException("No cursor method for the given return type.")
}

fun TypeMirror.getOrderedConstructorParams(): List<String> {
    val constructors = this.asTypeElement().enclosedElements.filter { e ->
        e.kind == ElementKind.CONSTRUCTOR
    }
    if (constructors.size > 1) {
        // TODO(obenabde): error, should have only one constructor otherwise it becomes
        //  ambiguous
    }
    return constructors.first().enclosedElements.filter { e -> e.kind == ElementKind.PARAMETER }
        .map { e -> e.simpleName.toString() }
}

internal val SUPPORTED_RETURN_WRAPPERS = listOf(
    List::class.java,
    Set::class.java,
    Optional::class.java
)
