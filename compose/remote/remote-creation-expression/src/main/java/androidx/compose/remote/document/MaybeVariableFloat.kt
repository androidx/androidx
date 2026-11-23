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

package androidx.compose.remote.document

import androidx.annotation.RestrictTo

/**
 * Logical representation of a float-or-variable slot in the RemoteCompose binary format.
 *
 * In RemoteCompose, variable-capable float fields carry either a 32-bit IEEE 754 literal float or a
 * NaN-boxed variable reference (where bits 21..0 encode the 22-bit variable ID under a negative NaN
 * prefix `0xFF800000`). Storing [rawBits] as an [Int] preserves exact NaN bit patterns across
 * deserialization, equality checks, and re-serialization.
 *
 * @property rawBits the raw 32-bit integer bit pattern of this value
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class MaybeVariableFloat private constructor(public val rawBits: Int) {

    /**
     * Returns true if this [MaybeVariableFloat] represents a NaN-boxed variable reference rather
     * than a literal float.
     */
    public val isVariable: Boolean
        get() = NanEncoding.isNanBits(rawBits)

    /**
     * Returns the literal float value if this is a literal, or a NaN [Float] preserving [rawBits]
     * if this is a variable reference.
     */
    public val floatValue: Float
        get() = Float.fromBits(rawBits)

    /**
     * Returns the 22-bit variable ID if [isVariable] is true, or `-1` if this is a literal float.
     */
    public val variableId: Int
        get() = NanEncoding.idFromNanBits(rawBits)

    override fun toString(): String =
        if (isVariable) {
            "MaybeVariableFloat(variableId=$variableId)"
        } else {
            "MaybeVariableFloat($floatValue)"
        }

    public companion object {
        /** Canonical non-variable `NaN` sentinel (e.g. for unset pivot coordinates). */
        public val NaN: MaybeVariableFloat = MaybeVariableFloat(Float.NaN.toRawBits())

        /** Creates a [MaybeVariableFloat] representing a literal 32-bit [value]. */
        @JvmStatic
        public fun literal(value: Float): MaybeVariableFloat = MaybeVariableFloat(value.toRawBits())

        /**
         * Creates a [MaybeVariableFloat] representing a NaN-boxed variable reference with
         * [variableId].
         */
        @JvmStatic
        public fun variable(variableId: Int): MaybeVariableFloat =
            MaybeVariableFloat(NanEncoding.asNanBits(variableId))

        /** Creates a [MaybeVariableFloat] from raw 32-bit integer [rawBits]. */
        @JvmStatic
        public fun fromRawBits(rawBits: Int): MaybeVariableFloat = MaybeVariableFloat(rawBits)
    }
}

/** Converts this [Float] into a [MaybeVariableFloat] preserving raw bit representation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Float.toMaybeVariableFloat(): MaybeVariableFloat =
    MaybeVariableFloat.fromRawBits(this.toRawBits())

/** Extension property to wrap this literal [Float] in a [MaybeVariableFloat]. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Float.mvf: MaybeVariableFloat
    get() = MaybeVariableFloat.literal(this)
