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

package androidx.room.processing

import com.squareup.javapoet.TypeName
import kotlin.contracts.contract
import kotlin.reflect.KClass

interface XType {
    val typeName: TypeName

    fun asTypeElement(): XTypeElement

    fun isAssignableFrom(other: XType): Boolean

    fun isAssignableFromWithoutVariance(other: XType): Boolean {
        return isAssignableWithoutVariance(other, this)
    }

    // TODO these is<Type> checks may need to be moved into the implementation.
    //  It is not yet clear how we will model some types in Kotlin (e.g. primitives)
    fun isNotByte() = !isByte()

    fun isError(): Boolean

    fun defaultValue(): String

    fun boxed(): XType

    fun asArray(): XArrayType = this as XArrayType

    fun isPrimitiveInt(): Boolean {
        return typeName == TypeName.INT
    }

    fun isBoxedInt() = typeName == TypeName.INT.box()

    fun isInt() = isPrimitiveInt() || isBoxedInt()

    fun isPrimitiveLong() = typeName == TypeName.LONG

    fun isBoxedLong() = typeName == TypeName.LONG.box()

    fun isLong() = isPrimitiveLong() || isBoxedLong()

    fun isList(): Boolean = isType() && isTypeOf(List::class)

    fun isVoid() = typeName == TypeName.VOID

    fun isVoidObject(): Boolean = isType() && isTypeOf(Void::class)

    fun isPrimitive() = typeName.isPrimitive

    fun isKotlinUnit(): Boolean = isType() && isTypeOf(Unit::class)

    fun isNotVoid() = !isVoid()

    fun isNotError() = !isError()

    fun isByte() = typeName == TypeName.BYTE

    fun isNone(): Boolean

    fun isNotNone() = !isNone()

    fun isType(): Boolean

    fun isTypeOf(other: KClass<*>): Boolean

    fun isSameType(other: XType): Boolean

    fun extendsBoundOrSelf(): XType = extendsBound() ?: this

    fun isAssignableWithoutVariance(other: XType): Boolean {
        return isAssignableWithoutVariance(other, this)
    }

    fun erasure(): XType

    fun extendsBound(): XType?
}

fun XType.isDeclared(): Boolean {
    contract {
        returns(true) implies (this@isDeclared is XDeclaredType)
    }
    return this is XDeclaredType
}

fun XType.isArray(): Boolean {
    contract {
        returns(true) implies (this@isArray is XArrayType)
    }
    return this is XArrayType
}

fun XType.isCollection(): Boolean {
    contract {
        returns(true) implies (this@isCollection is XDeclaredType)
    }
    return isType() && (isTypeOf(List::class) || isTypeOf(Set::class))
}

fun XType.asDeclaredType() = this as XDeclaredType

private fun isAssignableWithoutVariance(from: XType, to: XType): Boolean {
    val assignable = to.isAssignableFrom(from)
    if (assignable) {
        return true
    }
    if (!from.isDeclared() || !to.isDeclared()) {
        return false
    }
    val declaredFrom = from.asDeclaredType()
    val declaredTo = to.asDeclaredType()
    val fromTypeArgs = declaredFrom.typeArguments
    val toTypeArgs = declaredTo.typeArguments
    // no type arguments, we don't need extra checks
    if (fromTypeArgs.isEmpty() || fromTypeArgs.size != toTypeArgs.size) {
        return false
    }
    // check erasure version first, if it does not match, no reason to proceed
    if (!to.erasure().isAssignableFrom(from.erasure())) {
        return false
    }
    // convert from args to their upper bounds if it exists
    val fromExtendsBounds = fromTypeArgs.map {
        it.extendsBound()
    }
    // if there are no upper bound conversions, return.
    if (fromExtendsBounds.all { it == null }) {
        return false
    }
    // try to move the types of the from to their upper bounds. It does not matter for the "to"
    // because Types.isAssignable handles it as it is valid java
    return (fromTypeArgs.indices).all { index ->
        isAssignableWithoutVariance(
            from = fromExtendsBounds[index] ?: fromTypeArgs[index],
            to = toTypeArgs[index]
        )
    }
}
