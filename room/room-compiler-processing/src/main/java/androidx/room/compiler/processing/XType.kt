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

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * Represents a type reference
 *
 * @see javax.lang.model.type.TypeMirror
 * @see [XArrayType]
 */
interface XType {
    /**
     * The Javapoet [TypeName] representation of the type
     */
    val typeName: TypeName

    /**
     * Returns the rawType of this type. (e.g. `List<String>` to `List`.
     */
    val rawType: XRawType

    /**
     * Nullability declared in the code.
     * For Kotlin types, it will be inferred from type declaration.
     * For Java types, it will be inferred from annotations.
     */
    val nullability: XNullability

    /**
     * The [XTypeElement] that represents this type.
     *
     * Note that it might be null if the type is not backed by a type element (e.g. if it is a
     * primitive, wildcard etc)
     *
     * @see isTypeElement
     */
    val typeElement: XTypeElement?

    /**
     * Type arguments for the element. Note that they might be either placeholders or real
     * resolvable types depending on the usage.
     *
     * If the type is not declared (e.g. a primitive), the list is empty.
     *
     * @see [javax.lang.model.type.DeclaredType.getTypeArguments]
     */
    val typeArguments: List<XType>

    /**
     * Returns `true` if this type can be assigned from [other]
     */
    fun isAssignableFrom(other: XType): Boolean

    /**
     * Returns `true` if this can be assigned from an instance of [other] without checking for
     * variance.
     */
    fun isAssignableFromWithoutVariance(other: XType): Boolean {
        return isAssignableWithoutVariance(other, this)
    }

    // TODO these is<Type> checks may need to be moved into the implementation.
    //  It is not yet clear how we will model some types in Kotlin (e.g. primitives)
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
     */
    fun boxed(): XType

    /**
     * Returns `true` if this is a [List]
     */
    fun isList(): Boolean = isTypeOf(List::class)

    /**
     * Returns `true` if this is the None type.
     */
    fun isNone(): Boolean

    /**
     * Returns `true` if this is the same raw type as [other]
     */
    fun isTypeOf(other: KClass<*>): Boolean

    /**
     * Returns `true` if this represents the same type as [other].
     * TODO: decide on how we want to handle nullability here.
     */
    fun isSameType(other: XType): Boolean

    /**
     * Returns the extends bound if this is a wildcard or self.
     */
    fun extendsBoundOrSelf(): XType = extendsBound() ?: this

    /**
     * If this is a wildcard with an extends bound, returns that bounded typed.
     */
    fun extendsBound(): XType?

    /**
     * Creates a type with nullability [XNullability.NULLABLE] or returns this if the nullability is
     * already [XNullability.NULLABLE].
     */
    fun makeNullable(): XType

    /**
     * Creates a type with nullability [XNullability.NONNULL] or returns this if the nullability is
     * already [XNullability.NONNULL].
     */
    fun makeNonNullable(): XType
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
    return isTypeOf(List::class) || isTypeOf(Set::class)
}

private fun isAssignableWithoutVariance(from: XType, to: XType): Boolean {
    val assignable = to.isAssignableFrom(from)
    if (assignable) {
        return true
    }
    val fromTypeArgs = from.typeArguments
    val toTypeArgs = to.typeArguments
    // no type arguments, we don't need extra checks
    if (fromTypeArgs.isEmpty() || fromTypeArgs.size != toTypeArgs.size) {
        return false
    }
    // check erasure version first, if it does not match, no reason to proceed
    if (!to.rawType.isAssignableFrom(from)) {
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

/**
 * Returns `true` if this is a primitive or boxed it
 */
fun XType.isInt(): Boolean = typeName == TypeName.INT || typeName == KnownTypeNames.BOXED_INT

/**
 * Returns `true` if this is a primitive or boxed long
 */
fun XType.isLong(): Boolean = typeName == TypeName.LONG || typeName == KnownTypeNames.BOXED_LONG
/**
 * Returns `true` if this is `void`
 */
fun XType.isVoid() = typeName == TypeName.VOID

/**
 * Returns `true` if this is a [Void]
 */
fun XType.isVoidObject(): Boolean = typeName == KnownTypeNames.BOXED_VOID

/**
 * Returns `true` if this is the kotlin [Unit] type.
 */
fun XType.isKotlinUnit(): Boolean = typeName == KnownTypeNames.KOTLIN_UNIT

/**
 * Returns `true` if this represents a `byte`.
 */
fun XType.isByte(): Boolean = typeName == TypeName.BYTE || typeName == KnownTypeNames.BOXED_BYTE

internal object KnownTypeNames {
    val BOXED_VOID = TypeName.VOID.box()
    val BOXED_INT = TypeName.INT.box()
    val BOXED_LONG = TypeName.LONG.box()
    val BOXED_BYTE = TypeName.BYTE.box()
    val KOTLIN_UNIT = ClassName.get("kotlin", "Unit")
}
