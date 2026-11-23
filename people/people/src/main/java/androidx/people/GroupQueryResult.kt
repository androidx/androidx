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
 * Result of a [GroupQuery], split into [handledQuery] and [unhandledQuery] as described on
 * [PersonQueryResult].
 *
 * Each [GroupMatch] identifies its People Directory application through [GroupId.directoryPackage].
 */
public class GroupQueryResult
internal constructor(
    /**
     * Every match across all People Directory applications, each group at most once per
     * application. Every reported criterion is part of [handledQuery]. The order is unspecified.
     */
    public val matches: Set<GroupMatch>,
    /**
     * The criteria of the original query that were evaluated ([MatchResult.Handled]), including
     * both criteria that matched at least one group and criteria that matched no group. Empty if no
     * criterion was evaluated.
     */
    public val handledQuery: GroupQuery,
    /**
     * The criteria of the original query that were not evaluated ([MatchResult.Unhandled]), still
     * keyed by directory. Empty if every criterion was evaluated. Pass it back to the query
     * functions to retry.
     *
     * Criteria appear here only if the People Provider reports them as unhandled. A People Provider
     * that does not report unhandled criteria leaves this empty, and the criteria it did not
     * evaluate appear in [handledQuery] instead.
     */
    public val unhandledQuery: GroupQuery,
) {
    /**
     * Returns the [MatchResult] for [criterion] in the People Directory application
     * [directoryPackage]:
     * - [MatchResult.Handled] with every matching [GroupMatch] (or an empty set if evaluated with
     *   no match) when [criterion] is in [handledQuery] for [directoryPackage].
     * - [MatchResult.Unhandled] when [criterion] is in [unhandledQuery] for [directoryPackage], or
     *   was not addressed to [directoryPackage] in the original query.
     */
    public fun getResultFor(
        directoryPackage: String,
        criterion: Criterion,
    ): MatchResult<GroupMatch> {
        if (criterion !in handledQuery.criteriaFor(directoryPackage)) {
            return MatchResult.Unhandled
        }
        val matched =
            matches.filterTo(mutableSetOf()) {
                it.group.id.directoryPackage == directoryPackage && criterion in it.criteria
            }
        return MatchResult.Handled(matched)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is GroupQueryResult &&
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
        "GroupQueryResult(matches=$matches, handledQuery=$handledQuery, " +
            "unhandledQuery=$unhandledQuery)"
}
