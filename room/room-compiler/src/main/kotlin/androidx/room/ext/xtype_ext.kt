/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.ext

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.isByte
import androidx.room.compiler.processing.isEnum
import androidx.room.compiler.processing.isKotlinUnit
import androidx.room.compiler.processing.isVoid
import androidx.room.compiler.processing.isVoidObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

/**
 * Returns `true` if this type is not the `void` type.
 */
fun XType.isNotVoid() = !isVoid()

/**
 * Returns `true` if this does not represent a [Void] type.
 */
fun XType.isNotVoidObject() = !isVoidObject()

/**
 * Returns `true` if this type does not represent a [Unit] type.
 */
fun XType.isNotKotlinUnit() = !isKotlinUnit()

/**
 * Returns `true` if this type represents a valid resolvable type.
 */
fun XType.isNotError() = !isError()

/**
 * Returns `true` if this is not the None type.
 */
fun XType.isNotNone() = !isNone()

/**
 * Returns `true` if this is not `byte` type.
 */
fun XType.isNotByte() = !isByte()

/**
 * Returns `true` if this represents a `UUID` type.
 */
fun XType.isUUID(): Boolean = typeName == CommonTypeNames.UUID

/**
 * Checks if the class of the provided type has the equals() and hashCode() methods declared.
 *
 * Certain Room types and database primitive types are considered to implements equals and
 * hashcode.
 *
 * If they are not found at the current class level, the method recursively moves on to the
 * super class level and continues to look for these declared methods.
 */
fun XType.implementsEqualsAndHashcode(): Boolean {
    if (this.isSupportedMapTypeArg()) return true

    val typeElement = this.typeElement ?: return false
    if (typeElement.className == ClassName.OBJECT) {
        return false
    }

    if (typeElement.isDataClass()) {
        return true
    }
    val hasEquals = typeElement.getDeclaredMethods().any {
        it.jvmName == "equals" &&
            it.returnType.typeName == TypeName.BOOLEAN &&
            it.parameters.count() == 1 &&
            it.parameters[0].type.typeName == TypeName.OBJECT
    }
    val hasHashCode = typeElement.getDeclaredMethods().any {
        it.jvmName == "hashCode" &&
            it.returnType.typeName == TypeName.INT &&
            it.parameters.count() == 0
    }

    if (hasEquals && hasHashCode) return true

    return typeElement.superType?.let { it.implementsEqualsAndHashcode() } ?: false
}

/**
 * Checks if the class of the provided type is one of the types supported in Dao functions with a
 * Map or Multimap return type.
 */
fun XType.isSupportedMapTypeArg(): Boolean {
    if (this.typeName.isPrimitive) return true
    if (this.typeName.isBoxedPrimitive) return true
    if (this.typeName == CommonTypeNames.STRING) return true
    if (this.isTypeOf(ByteArray::class)) return true
    if (this.isArray() && this.isByte()) return true
    val typeElement = this.typeElement ?: return false
    if (typeElement.isEnum()) return true
    return false
}
