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
package androidx.health.data.client.metadata

import java.time.Instant

/** Set of shared metadata fields for [androidx.health.data.client.records.Record]. */
public class Metadata(
    /**
     * Unique identifier of this data, assigned by the Android Health Platform at insertion time.
     */
    public val uid: String? = null,

    /**
     * Where the data comes from, such as application information originally generated this data.
     */
    public val dataOrigin: DataOrigin = DataOrigin(""),

    /** Automatically populated to when data was last modified (or originally created). */
    public val lastModifiedTime: Instant = Instant.EPOCH,

    /** Optional client supplied unique data identifier associated with the data. */
    public val clientId: String? = null,

    /** Optional client supplied version associated with the data. */
    public val clientVersion: Long = 0,

    /** Optional client supplied device information associated with the data. */
    public val device: Device? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Metadata) return false

        if (uid != other.uid) return false
        if (dataOrigin != other.dataOrigin) return false
        if (lastModifiedTime != other.lastModifiedTime) return false
        if (clientId != other.clientId) return false
        if (clientVersion != other.clientVersion) return false
        if (device != other.device) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid?.hashCode() ?: 0
        result = 31 * result + dataOrigin.hashCode()
        result = 31 * result + lastModifiedTime.hashCode()
        result = 31 * result + (clientId?.hashCode() ?: 0)
        result = 31 * result + clientVersion.hashCode()
        result = 31 * result + (device?.hashCode() ?: 0)
        return result
    }

    internal companion object {
        /** A default instance of metadata with no fields initialised. */
        @JvmField internal val EMPTY = Metadata()
    }
}
