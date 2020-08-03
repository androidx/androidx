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

package androidx.room.compiler.processing

import com.squareup.javapoet.TypeName
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * Represents a type reference
 *
 * @see javax.lang.model.type.TypeMirror
 * @see [XArrayType]
 * @see [XDeclaredType]
 */
interface XType {
    /**
     * The Javapoet [TypeName] representation of the type
     */
    val typeName: TypeName

    /**
     * Casts the current type to [XTypeElement].
     *
     * @see isType
     */
    fun asTypeElement(): XTypeElement

    /**
     * Returns `true` if this type can be assigned from [other]
     */
    fun isAssignableFrom(other: XType): Boolean

    /**
     * Returns `true` if this type can be assigned from [other] while ignoring the type variance.
     */
    fun isAssignableFromWithoutVariance(other: XType): Boolean {
        return isAssignableWithoutVariance(other, this)
    }

    // TODO these is<Type> checks may need to be moved into the implementation.
    //  It is not yet clear how we will model some types in Kotlin (e.g. primitives)
    /**
     * Returns `true` if this is not `byte` type.
     */
    fun isNotByte() = !isByte()

    /**
     * Returns `true` if this is an error type.
     */
    fun isError(): Boolean

    /**
     * Returns the string representation of a possible default value for this type.
     * (e.g. `0` for `int`, `null` for `String`)
     */
    fun defaultValue(): String

    /**
     * Returns boxed version of this type if it is a primitive or itself if it is not a primitive
     * type.
     *
     * @see isPrimitive
     */
    fun boxed(): XType

    /**
     * Returns this type as an instance of [XArrayType] or fails if it is not an array.
     */
    fun asArray(): XArrayType = this as XArrayType

    /**
     * Returns `true` if this is an `int`
     */
    fun isPrimitiveInt(): Boolean {
        return typeName == TypeName.INT
    }

    /**
     * Returns `true` if this type represents a boxed int (java.lang.Integer)
     */
    fun isBoxedInt() = typeName == TypeName.INT.box()

    /**
     * Returns `true` if this is a primitive or boxed it
     */
    fun isInt() = isPrimitiveInt() || isBoxedInt()

    /**
     * Returns `true` if this is `long`
     */
    fun isPrimitiveLong() = typeName == TypeName.LONG

    /**
     * Returns `true` if this is a boxed long (java.lang.Long)
     */
    fun isBoxedLong() = typeName == TypeName.LONG.box()

    /**
     * Returns `true` if this is a primitive or boxed long
     */
    fun isLong() = isPrimitiveLong() || isBoxedLong()

    /**
     * Returns `true` if this is a [List]
     */
    fun isList(): Boolean = isType() && isTypeOf(List::class)

    /**
     * Returns `true` if this is `void`
     */
    fun isVoid() = typeName == TypeName.VOID

    /**
     * Returns `true` if this is a [Void]
     */
    fun isVoidObject(): Boolean = isType() && isTypeOf(Void::class)

    /**
     * Returns `true` if this represents a primitive type
     */
    fun isPrimitive() = typeName.isPrimitive

    /**
     * Returns `true` if this is the kotlin [Unit] type.
     */
    fun isKotlinUnit(): Boolean = isType() && isTypeOf(Unit::class)

    /**
     * Returns `true` if this is not `void`.
     */
    fun isNotVoid() = !isVoid()

    /**
     * Returns `true` if this type represents a valid resolvable type.
     */
    fun isNotError() = !isError()

    /**
     * Returns `true` if this represents a `byte`.
     */
    fun isByte() = typeName == TypeName.BYTE

    /**
     * Returns `true` if this is the None type.
     */
    fun isNone(): Boolean

    /**
     * Returns `true` if this is not the None type.
     */
    fun isNotNone() = !isNone()

    /**
     * Returns true if this represented by a [XTypeElement].
     */
    fun isType(): Boolean

    /**
     * Returns `true` if this is the same raw type as [other]
     */
    fun isTypeOf(other: KClass<*>): Boolean

    /**
     * Returns `true` if this represents the same type as [other]
     */
    fun isSameType(other: XType): Boolean

    /**
     * Returns the extends bound if this is a wildcard or self.
     */
    fun extendsBoundOrSelf(): XType = extendsBound() ?: this

    /**
     * Returns `true` if this can be assigned from an instance of [other] without checking for
     * variance.
     */
    fun isAssignableWithoutVariance(other: XType): Boolean {
        return isAssignableWithoutVariance(other, this)
    }

    /**
     * Returns the erasure of this type. (e.g. `List<String>` to `List`.
     */
    fun erasure(): XType

    /**
     * If this is a wildcard with an extends bound, returns that bounded typed.
     */
    fun extendsBound(): XType?
}

/**
 * Returns true if this is an [XDeclaredType].
 */
fun XType.isDeclared(): Boolean {
    contract {
        returns(true) implies (this@isDeclared is XDeclaredType)
    }
    return this is XDeclaredType
}

/**
 * Returns true if this is an [XArrayType].
 */
fun XType.isArray(): Boolean {
    contract {
        returns(true) implies (this@isArray is XArrayType)
    }
    return this is XArrayType
}

/**
 * Returns true if this is a [List] or [Set].
 */
fun XType.isCollection(): Boolean {
    contract {
        returns(true) implies (this@isCollection is XDeclaredType)
    }
    return isType() && (isTypeOf(List::class) || isTypeOf(Set::class))
}

/**
 * Returns `this` as an [XDeclaredType].
 */
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
