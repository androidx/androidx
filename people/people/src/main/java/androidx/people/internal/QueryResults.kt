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

@file:JvmName("QueryResults")

package androidx.people.internal

import androidx.annotation.RestrictTo
import androidx.people.GroupMatch
import androidx.people.GroupQuery
import androidx.people.GroupQueryResult
import androidx.people.PersonMatch
import androidx.people.PersonQuery
import androidx.people.PersonQueryResult
import androidx.people.groupQueryOf
import androidx.people.personQueryOf

// Factories for the query results that a PeopleBackend returns. They are LIBRARY_GROUP so that
// backends and tests in other artifacts of this library group can build results too.

/**
 * Returns the result of [query] from the [matches] and the [unhandledQuery] that the People
 * Directory applications reported.
 *
 * [matches] and [unhandledQuery] must only contain directories and criteria of [query], and must
 * not report a criterion as both matched and unhandled. The criteria of [query] that are neither
 * matched nor unhandled matched nothing, and make up [PersonQueryResult.unmatchedQuery].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun personQueryResultOf(
    query: PersonQuery,
    matches: Set<PersonMatch>,
    unhandledQuery: PersonQuery,
): PersonQueryResult {
    val matchedQuery =
        personQueryOf(
            matches
                .groupBy { it.person.id.directoryPackage }
                .mapValues { (_, directoryMatches) ->
                    directoryMatches.flatMapTo(mutableSetOf()) { it.criteria }
                }
        )
    return PersonQueryResult(
        matches = matches,
        matchedQuery = matchedQuery,
        unmatchedQuery = query - matchedQuery - unhandledQuery,
        unhandledQuery = unhandledQuery,
    )
}

/**
 * Returns the result of [query] when no People Directory application answered it, for example
 * because the People Provider returned no cursor or is unavailable: every criterion is in
 * [PersonQueryResult.unhandledQuery].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failedPersonQueryResultOf(query: PersonQuery): PersonQueryResult =
    personQueryResultOf(query, matches = emptySet(), unhandledQuery = query)

/**
 * Returns the result of [query] from the [matches] and the [unhandledQuery] that the People
 * Directory applications reported, following the same rules as [personQueryResultOf].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun groupQueryResultOf(
    query: GroupQuery,
    matches: Set<GroupMatch>,
    unhandledQuery: GroupQuery,
): GroupQueryResult {
    val matchedQuery =
        groupQueryOf(
            matches
                .groupBy { it.group.id.directoryPackage }
                .mapValues { (_, directoryMatches) ->
                    directoryMatches.flatMapTo(mutableSetOf()) { it.criteria }
                }
        )
    return GroupQueryResult(
        matches = matches,
        matchedQuery = matchedQuery,
        unmatchedQuery = query - matchedQuery - unhandledQuery,
        unhandledQuery = unhandledQuery,
    )
}

/**
 * Returns the result of [query] when no People Directory application answered it, following the
 * same rules as [failedPersonQueryResultOf].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun failedGroupQueryResultOf(query: GroupQuery): GroupQueryResult =
    groupQueryResultOf(query, matches = emptySet(), unhandledQuery = query)
