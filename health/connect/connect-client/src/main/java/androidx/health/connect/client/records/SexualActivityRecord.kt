/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.records

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures an occurrence of sexual activity. Each record is a single occurrence. ProtectionUsed
 * field is optional.
 */
public class SexualActivityRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /**
     * Whether protection was used during sexual activity. Optional field, null if unknown. Allowed
     * values: [Protection].
     *
     * @see Protection
     */
    @property:Protections public val protectionUsed: Int = PROTECTION_USED_UNKNOWN,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SexualActivityRecord) return false

        if (protectionUsed != other.protectionUsed) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = protectionUsed
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        const val PROTECTION_USED_UNKNOWN = 0
        const val PROTECTION_USED_PROTECTED = 1
        const val PROTECTION_USED_UNPROTECTED = 2

        /** Internal mappings useful for interoperability between integers and strings. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val PROTECTION_USED_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                Protection.PROTECTED to PROTECTION_USED_PROTECTED,
                Protection.UNPROTECTED to PROTECTION_USED_UNPROTECTED,
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val PROTECTION_USED_INT_TO_STRING_MAP = PROTECTION_USED_STRING_TO_INT_MAP.reverse()
    }

    /** Whether protection was used during sexual activity. */
    internal object Protection {
        const val PROTECTED = "protected"
        const val UNPROTECTED = "unprotected"
    }

    /**
     * Whether protection was used during sexual activity.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                PROTECTION_USED_PROTECTED,
                PROTECTION_USED_UNPROTECTED,
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class Protections
}
