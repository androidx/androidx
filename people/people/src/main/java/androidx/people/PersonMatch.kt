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
 * A [person] returned by a query, together with the query [criteria] it matched.
 *
 * A batched query, for example a lookup of two hundred phone numbers, returns one match per person.
 * Each match reports every criterion that matched the person, so results can be correlated back to
 * their inputs:
 * ```
 * val matches = matches.any { Criterion.Phone(number) in it.criteria }
 * ```
 *
 * @throws IllegalArgumentException if [criteria] is empty.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PersonMatch(
    /** The matched person. */
    public val person: Person,
    /**
     * The query criteria that matched [person], each echoed exactly as supplied in the query. Never
     * empty. See [Criterion] for the full contract.
     */
    public val criteria: Set<Criterion>,
) {
    init {
        require(criteria.isNotEmpty()) { "A match needs at least one criterion." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PersonMatch) return false
        return person == other.person && criteria == other.criteria
    }

    override fun hashCode(): Int = 31 * person.hashCode() + criteria.hashCode()

    override fun toString(): String = "PersonMatch(person=$person, criteria=$criteria)"
}
