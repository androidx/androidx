/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people

import androidx.annotation.RestrictTo

/**
 * An aggregated group record returned by a People Directory application.
 *
 * Each [id] appears at most once in a result. [matchedCriteria] reports every query input that
 * matched this group, so that a batched query can be correlated back to its inputs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Group(
    /** Unique compound identifier for this group record. */
    public val id: GroupId,
    /** The group's display name, or `null` if the directory did not supply one. */
    public val displayName: String?,
    /**
     * The query inputs that matched this group, or an empty set if none were reported.
     *
     * Each value is echoed exactly as supplied in the [GroupQuery]. Groups are currently matched
     * only by their own lookup key, reported as [Criterion.GroupLookupKey]. See [Criterion] for the
     * full contract.
     */
    public val matchedCriteria: Set<Criterion>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Group) return false
        return id == other.id &&
            displayName == other.displayName &&
            matchedCriteria == other.matchedCriteria
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + matchedCriteria.hashCode()
        return result
    }

    override fun toString(): String =
        "Group(id=$id, displayName=$displayName, matchedCriteria=$matchedCriteria)"
}
