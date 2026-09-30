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

package androidx.credentials.registry.provider.digitalcredentials

import androidx.annotation.IntRange

/**
 * Deduplication metadata for a [DigitalCredentialEntry] used to deduplicate and order entries
 * across providers.
 *
 * @property id the identifier used to deduplicate entries across providers
 * @property rank the rank of this entry for ordering among deduplicated entries, where a lower
 *   value indicates higher priority
 * @throws IllegalArgumentException if [id] is empty or [rank] is negative
 */
public class DeduplicationInfo
@JvmOverloads
constructor(
    public val id: String,
    @param:IntRange(from = 0) @get:IntRange(from = 0) public val rank: Int = 0,
) {
    init {
        require(id.isNotEmpty()) { "`id` must not be empty" }
        require(rank >= 0) { "`rank` must be non-negative" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeduplicationInfo) return false
        return this.id == other.id && this.rank == other.rank
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rank
        return result
    }
}
