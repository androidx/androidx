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
 * Captures a description of how heavy a user's menstrual flow was (light, medium, or heavy). Each
 * record represents a description of how heavy the user's menstrual bleeding was.
 */
public class MenstruationFlowRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /** How heavy the user's menstrual flow was. Optional field. */
    @property:Flows public val flow: Int = FLOW_UNKNOWN,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MenstruationFlowRecord) return false

        if (flow != other.flow) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = flow
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        const val FLOW_UNKNOWN = 0
        const val FLOW_LIGHT = 1
        const val FLOW_MEDIUM = 2
        const val FLOW_HEAVY = 3

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val FLOW_TYPE_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf("light" to FLOW_LIGHT, "medium" to FLOW_MEDIUM, "heavy" to FLOW_HEAVY)

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val FLOW_TYPE_INT_TO_STRING_MAP: Map<Int, String> =
            FLOW_TYPE_STRING_TO_INT_MAP.entries.associateBy({ it.value }, { it.key })
    }
    /**
     * How heavy the user's menstruation flow was.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [FLOW_UNKNOWN, FLOW_LIGHT, FLOW_MEDIUM, FLOW_HEAVY])
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class Flows
}
