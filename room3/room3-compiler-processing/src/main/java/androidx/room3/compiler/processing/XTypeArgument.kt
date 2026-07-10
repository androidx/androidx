/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.compiler.processing

import androidx.room3.compiler.codegen.XTypeName

/** Represents a type argument for a generic type. */
interface XTypeArgument {
    /** The variance of the type argument. */
    val variance: XVariance

    /** The type of the type argument, or `null` for star types. */
    val type: XType

    /** Gets the [XTypeName] representing the type argument. */
    fun asTypeName(): XTypeName

    /** Returns the extends bound if this is a wildcard or self. */
    fun extendsBoundOrSelf(): XType = extendsBound() ?: type

    /** If this is a wildcard with an extends bound, returns that bounded typed. */
    fun extendsBound(): XType? {
        return when (variance) {
            XVariance.INVARIANT,
            XVariance.STAR -> null
            else -> type
        }
    }

    /**
     * Returns true if this is a star type argument.
     *
     * In Java, this returns true for the `?` type argument.
     *
     * In Kotlin, this returns true for the `*` type argument.
     *
     * Note that `? extends Object` and `out Any?` are not considered star types even though they're
     * equivalent.
     */
    fun isStar(): Boolean = variance == XVariance.STAR

    /**
     * Returns `true` if the type argument has the same variance and type as [other].
     *
     * TODO: decide on how we want to handle nullability here.
     */
    fun isSameType(other: XTypeArgument) = variance == other.variance && type.isSameType(other.type)
}
