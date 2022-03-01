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
 * Captures the description of cervical mucus. Each record represents a self-assessed description of
 * cervical mucus for a user. All fields are optional and can be used to describe the look and feel
 * of cervical mucus, and the amount.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CervicalMucus(
    /**
     * The consistency or texture of the user's cervical mucus. Optional field. Allowed values:
     * [CervicalMucusTexture].
     */
    @property:CervicalMucusTexture public val texture: String? = null,
    /**
     * The amount of cervical mucus the user observes. Optional field. Allowed values:
     * [CervicalMucusAmount].
     */
    @property:CervicalMucusAmount public val amount: String? = null,
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CervicalMucus) return false

        if (texture != other.texture) return false
        if (amount != other.amount) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + texture.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }
}
