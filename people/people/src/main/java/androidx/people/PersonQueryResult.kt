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

/**
 * Result of a [PersonQuery].
 *
 * Call [getResultFor] to inspect the [MatchResult] of an individual criterion in a People Directory
 * application. At the batch level, this result splits the original query into two non-overlapping
 * sub-queries that together contain every criterion of the original query:
 * - [handledQuery]: criteria that were evaluated (whether they matched anyone in [matches] or
 *   matched nobody), whose outcome is [MatchResult.Handled].
 * - [unhandledQuery]: criteria that were not evaluated (for example because the directory's data
 *   was not cached yet), whose outcome is [MatchResult.Unhandled].
 *
 * ```
 * val result = client.queryPersons(query)
 * when (val match = result.getResultFor("com.example.voip", Criterion.Phone(number))) {
 *     is MatchResult.Handled -> show(match.matches)
 *     MatchResult.Unhandled -> showPending()
 * }
 * if (!result.unhandledQuery.isEmpty()) retryLater(result.unhandledQuery)
 * ```
 *
 * Each [PersonMatch] identifies its People Directory application through
 * [PersonId.directoryPackage].
 */
public class PersonQueryResult
internal constructor(
    /**
     * Every match across all People Directory applications, each person at most once per
     * application. Every reported criterion is part of [handledQuery]. The order is unspecified.
     */
    public val matches: Set<PersonMatch>,
    /**
     * The criteria of the original query that were evaluated ([MatchResult.Handled]), including
     * both criteria that matched at least one person and criteria that matched nobody. Empty if no
     * criterion was evaluated.
     */
    public val handledQuery: PersonQuery,
    /**
     * The criteria of the original query that were not evaluated ([MatchResult.Unhandled]), still
     * keyed by directory. Empty if every criterion was evaluated. Pass it back to the query
     * functions to retry.
     *
     * Criteria appear here only if the People Provider reports them as unhandled. A People Provider
     * that does not report unhandled criteria leaves this empty, and the criteria it did not
     * evaluate appear in [handledQuery] instead.
     */
    public val unhandledQuery: PersonQuery,
) {
    /**
     * Returns the [MatchResult] for [criterion] in the People Directory application
     * [directoryPackage]:
     * - [MatchResult.Handled] with every matching [PersonMatch] (or an empty set if evaluated with
     *   no match) when [criterion] is in [handledQuery] for [directoryPackage].
     * - [MatchResult.Unhandled] when [criterion] is in [unhandledQuery] for [directoryPackage], or
     *   was not addressed to [directoryPackage] in the original query.
     */
    public fun getResultFor(
        directoryPackage: String,
        criterion: Criterion,
    ): MatchResult<PersonMatch> {
        if (criterion !in handledQuery.criteriaFor(directoryPackage)) {
            return MatchResult.Unhandled
        }
        val matched =
            matches.filterTo(mutableSetOf()) {
                it.person.id.directoryPackage == directoryPackage && criterion in it.criteria
            }
        return MatchResult.Handled(matched)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is PersonQueryResult &&
                matches == other.matches &&
                handledQuery == other.handledQuery &&
                unhandledQuery == other.unhandledQuery)

    override fun hashCode(): Int {
        var result = matches.hashCode()
        result = 31 * result + handledQuery.hashCode()
        result = 31 * result + unhandledQuery.hashCode()
        return result
    }

    override fun toString(): String =
        "PersonQueryResult(matches=$matches, handledQuery=$handledQuery, " +
            "unhandledQuery=$unhandledQuery)"
}
