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

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import androidx.health.connect.client.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/**
 * Each record represents a report of the user's cervix. All fields are optional, and can be used to
 * add descriptions of the position, dilation and firmness of the cervix.
 */
// TODO(b/230490611): Cervical Position has dilation and firmness, find better names from product
// space.
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CervicalPosition(
    /**
     * The position of the user's cervix. Optional field. Allowed values: [CervicalPositionValue].
     */
    @property:CervicalPositionValue public val position: String? = null,
    /**
     * How open or dilated the user's cervix is. Optional field. Allowed values: [CervicalDilation].
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

    /** How open or dilated the user's cervix is. */
    public object Dilations {
        const val CLOSED = "closed"
        const val PARTIALLY_OPEN = "partially_open"
        const val FULLY_DILATED = "fully_dilated"
    }

    /** How firm the user's cervix is. */
    public object Firmness {
        const val SOFT = "soft"
        const val A_LITTLE_FIRM = "a_little_firm"
        const val FIRM = "firm"
    }

    /** The position of the user's cervix. */
    public object Positions {
        const val LOW = "low"
        const val MEDIUM = "medium"
        const val HIGH = "high"
    }
}

/**
 * How open or dilated the user's cervix is.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            CervicalPosition.Dilations.CLOSED,
            CervicalPosition.Dilations.PARTIALLY_OPEN,
            CervicalPosition.Dilations.FULLY_DILATED,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class CervicalDilation

/**
 * How firm the user's cervix is.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            CervicalPosition.Firmness.SOFT,
            CervicalPosition.Firmness.A_LITTLE_FIRM,
            CervicalPosition.Firmness.FIRM,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class CervicalFirmness

/**
 * The position of the user's cervix.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            CervicalPosition.Positions.LOW,
            CervicalPosition.Positions.MEDIUM,
            CervicalPosition.Positions.HIGH,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class CervicalPositionValue
