/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.data

import androidx.annotation.RestrictTo
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ComparisonType.COMPARISON_TYPE_GREATER_THAN
import androidx.health.services.client.proto.DataProto.ComparisonType.COMPARISON_TYPE_GREATER_THAN_OR_EQUAL
import androidx.health.services.client.proto.DataProto.ComparisonType.COMPARISON_TYPE_LESS_THAN
import androidx.health.services.client.proto.DataProto.ComparisonType.COMPARISON_TYPE_LESS_THAN_OR_EQUAL
import androidx.health.services.client.proto.DataProto.ComparisonType.COMPARISON_TYPE_UNKNOWN

/** For determining when a threshold has been met or exceeded in a [DataTypeCondition]. */
public class ComparisonType private constructor(public val id: Int, public val name: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComparisonType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    override fun toString(): String = name

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun toProto(): DataProto.ComparisonType =
        when (this) {
            GREATER_THAN -> COMPARISON_TYPE_GREATER_THAN
            GREATER_THAN_OR_EQUAL -> COMPARISON_TYPE_GREATER_THAN_OR_EQUAL
            LESS_THAN -> COMPARISON_TYPE_LESS_THAN
            LESS_THAN_OR_EQUAL -> COMPARISON_TYPE_LESS_THAN_OR_EQUAL
            else -> COMPARISON_TYPE_UNKNOWN
        }

    public companion object {
        // **Note**: If new values are added the remote SDK version must be checked, since
        // DataTypeCondition previously threw an IllegalStateException if fromProto returned null,
        // which it did for UNKNOWN.
        // TODO(b/175064823): investigate adding EQUAL comparison type

        /** The comparison type is unknown, or this library version is too old to recognize it. */
        @JvmField public val UNKNOWN: ComparisonType = ComparisonType(0, "UNKNOWN")

        /** The comparison should be `currentValue > threshold`. */
        @JvmField public val GREATER_THAN: ComparisonType = ComparisonType(1, "GREATER_THAN")

        /** The comparison should be `currentValue >= threshold`. */
        @JvmField
        public val GREATER_THAN_OR_EQUAL: ComparisonType =
            ComparisonType(2, "GREATER_THAN_OR_EQUAL")

        /** The comparison should be `currentValue < threshold`. */
        @JvmField public val LESS_THAN: ComparisonType = ComparisonType(3, "LESS_THAN")

        /** The comparison should be `currentValue <= threshold`. */
        @JvmField
        public val LESS_THAN_OR_EQUAL: ComparisonType = ComparisonType(4, "LESS_THAN_OR_EQUAL")

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        public val VALUES: List<ComparisonType> =
            listOf(GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL)

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        internal fun fromProto(proto: DataProto.ComparisonType): ComparisonType =
            when (proto) {
                COMPARISON_TYPE_GREATER_THAN -> GREATER_THAN
                COMPARISON_TYPE_GREATER_THAN_OR_EQUAL -> GREATER_THAN_OR_EQUAL
                COMPARISON_TYPE_LESS_THAN -> LESS_THAN
                COMPARISON_TYPE_LESS_THAN_OR_EQUAL -> LESS_THAN_OR_EQUAL
                COMPARISON_TYPE_UNKNOWN -> UNKNOWN
            }
    }
}
