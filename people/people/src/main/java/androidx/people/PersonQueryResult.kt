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
 * The result of a [PersonQuery].
 *
 * The result splits the original query into three parts that do not overlap and together contain
 * every criterion of the original query:
 * - [matchedQuery]: criteria that matched at least one person, reported in [matches].
 * - [unmatchedQuery]: criteria that were evaluated and matched nobody.
 * - [unhandledQuery]: criteria that were not evaluated, for example because the directory's data
 *   was not cached yet.
 *
 * ```
 * val result = client.queryPersons(query)
 * show(result.matchesFor("com.example.voip", Criterion.Phone(number)))
 * if (!result.unhandledQuery.isEmpty()) retryLater(result.unhandledQuery)
 * ```
 *
 * Each [PersonMatch] identifies its People Directory application through
 * [PersonId.directoryPackage].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PersonQueryResult
internal constructor(
    /**
     * Every match across all People Directory applications, each person at most once per
     * application. Every reported criterion is part of [matchedQuery]. The order is unspecified.
     */
    public val matches: Set<PersonMatch>,
    /** The criteria that matched at least one person. Empty if none did. */
    public val matchedQuery: PersonQuery,
    /** The criteria that were evaluated and matched no person. Empty if there are none. */
    public val unmatchedQuery: PersonQuery,
    /**
     * The criteria that were not evaluated, still keyed by directory. Empty if every criterion was
     * evaluated. Pass it back to the query functions to retry.
     *
     * Criteria appear here only if the People Provider reports them as unhandled. A People Provider
     * that does not report unhandled criteria leaves this empty, and the criteria it did not
     * evaluate appear in [unmatchedQuery] instead.
     */
    public val unhandledQuery: PersonQuery,
) {
    /**
     * Returns the matches of the People Directory application [directoryPackage] that report
     * [criterion].
     */
    public fun matchesFor(directoryPackage: String, criterion: Criterion): Set<PersonMatch> =
        matches.filterTo(mutableSetOf()) {
            it.person.id.directoryPackage == directoryPackage && criterion in it.criteria
        }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is PersonQueryResult &&
                matches == other.matches &&
                matchedQuery == other.matchedQuery &&
                unmatchedQuery == other.unmatchedQuery &&
                unhandledQuery == other.unhandledQuery)

    override fun hashCode(): Int {
        var result = matches.hashCode()
        result = 31 * result + matchedQuery.hashCode()
        result = 31 * result + unmatchedQuery.hashCode()
        result = 31 * result + unhandledQuery.hashCode()
        return result
    }

    override fun toString(): String =
        "PersonQueryResult(matches=$matches, matchedQuery=$matchedQuery, " +
            "unmatchedQuery=$unmatchedQuery, unhandledQuery=$unhandledQuery)"
}
