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
package androidx.health.data.client.records

import androidx.annotation.RestrictTo
import androidx.health.data.client.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/**
 * Each record represents a report of the user's cervix. All fields are optional, and can be used to
 * add descriptions of the position, dilation and firmness of the cervix.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CervicalPosition(
    /**
     * The position of the user's cervix. Optional field. Allowed values: [CervicalPositionValue].
     */
    @property:CervicalPositionValue public val position: String? = null,
    /**
     * How open or dilated the user's cervix is. Optional field. Allowed values: [
     * CervicalDilation].
     */
    @property:CervicalDilation public val dilation: String? = null,
    /** How firm the user's cervix is. Optional field. Allowed values: [CervicalFirmness]. */
    @property:CervicalFirmness public val firmness: String? = null,
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CervicalPosition) return false

        if (position != other.position) return false
        if (dilation != other.dilation) return false
        if (firmness != other.firmness) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + position.hashCode()
        result = 31 * result + dilation.hashCode()
        result = 31 * result + firmness.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }
}
