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
 * Query results wrap each group in a [GroupMatch], which also reports the query criteria that
 * matched it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Group(
    /** Unique compound identifier for this group record. */
    public val id: GroupId,
    /** The group's display name, or `null` if the directory did not supply one. */
    public val displayName: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Group) return false
        return id == other.id && displayName == other.displayName
    }

    override fun hashCode(): Int = 31 * id.hashCode() + (displayName?.hashCode() ?: 0)

    override fun toString(): String = "Group(id=$id, displayName=$displayName)"
}
