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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asTypeName
import java.lang.RuntimeException
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter

fun TypeMirror.asTypeElement(): TypeElement = MoreTypes.asTypeElement(this)

fun TypeMirror.isList(): Boolean {
    return MoreTypes.isTypeOf(java.util.List::class.java, this)
}

fun TypeMirror.isSet(): Boolean {
    // Is of type kotlin.collections.Set?
    return MoreTypes.isTypeOf(Set::class.java, this)
}

fun TypeMirror.isSupportedCollection(): Boolean {
    return isList() || isSet()
}

fun TypeMirror.isOptional(): Boolean {
    return MoreTypes.isTypeOf(java.util.Optional::class.java, this)
}

fun TypeMirror.isFlowable(): Boolean {
    return this.asTypeName() == ClassName("io.reactivex", "Flowable")
}

fun TypeMirror.isSupportedGenericType(): Boolean {
    return isSupportedCollection() || isOptional() || isFlowable()
}

fun TypeMirror.extractImmediateTypeParameter():
        TypeMirror {
    val asDeclared = MoreTypes.asDeclared(this)
    return asDeclared.typeArguments.first()
}

fun TypeMirror.extractIntendedReturnType(): TypeMirror {
    if (!this.isSupportedGenericType()) {
        return this
    }
    val firstWrappedType = extractImmediateTypeParameter()
    if (isFlowable() && (firstWrappedType.isSupportedCollection() ||
                firstWrappedType.isOptional())) {
        return firstWrappedType.extractImmediateTypeParameter()
    }
    return firstWrappedType
}

fun TypeMirror.toKotlinClassName(): ClassName {
    if (isInt()) {
        return ClassName("kotlin", "Int")
    } else if (isLong()) {
        return ClassName("kotlin", "Long")
    } else if (isDouble()) {
        return ClassName("kotlin", "Double")
    } else if (isShort()) {
        return ClassName("kotlin", "Short")
    } else if (isFloat()) {
        return ClassName("kotlin", "Short")
    }
    return ClassName.bestGuess(this.toString())
}

fun TypeMirror.boxIfPrimitive(processingEnv: ProcessingEnvironment): TypeMirror {
    val types = processingEnv.typeUtils
    if (isInt()) {
        return types.boxedClass(types.getPrimitiveType(TypeKind.INT)).asType()
    } else if (isLong()) {
        return types.boxedClass(types.getPrimitiveType(TypeKind.LONG)).asType()
    } else if (isDouble()) {
        return types.boxedClass(types.getPrimitiveType(TypeKind.DOUBLE)).asType()
    } else if (isShort()) {
        return types.boxedClass(types.getPrimitiveType(TypeKind.SHORT)).asType()
    } else if (isFloat()) {
        return types.boxedClass(types.getPrimitiveType(TypeKind.FLOAT)).asType()
    }
    return this
}

fun TypeMirror.isVoidObject() =
    MoreTypes.isType(this) && MoreTypes.isTypeOf(Void::class.java, this)

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

fun TypeMirror.isShort() = isPrimitiveShort() || isBoxedShort()

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

fun TypeMirror.isSupportedColumnType() = isBlob() || isInt() || isString() || isFloat() ||
        isDouble() || isShort() || isLong()

fun TypeMirror.isPrimitive(): Boolean = isPrimitiveLong() || isPrimitiveShort() ||
        isPrimitiveBlob() || isPrimitiveDouble() || isPrimitiveFloat() || isPrimitiveInt()

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
    }
    // This should honestly only ever be called after checking isSupportedColumnType() but you
    // never know.
    throw RuntimeException("No cursor method for the given return type.")
}

fun TypeMirror.getOrderedConstructorParams(): List<VariableElement> {
    val constructors = ElementFilter.constructorsIn(this.asTypeElement().enclosedElements)
    if (constructors.size > 1) {
        // TODO(obenabde): error, should have only one constructor otherwise it becomes
        //  ambiguous
    }
    return constructors.first().parameters
}
