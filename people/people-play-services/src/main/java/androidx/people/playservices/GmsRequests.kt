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

package androidx.people.playservices

import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.GroupQuery
import androidx.people.PeopleInteraction
import androidx.people.PersonQuery
import com.google.android.gms.people.peopleprovider.CreateInteractionPendingIntentRequest
import com.google.android.gms.people.peopleprovider.NotifyDirectoryChangeRequest
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Groups
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Persons
import com.google.android.gms.people.peopleprovider.PeopleProviderSubQuery
import com.google.android.gms.people.peopleprovider.QueryGroupsRequest
import com.google.android.gms.people.peopleprovider.QueryPersonsRequest

// Converts androidx.people queries into Google Play services People Provider requests.

/**
 * Returns the request for this query, or `null` if it contains no criteria, since Google Play
 * services rejects a request without sub-queries.
 */
internal fun PersonQuery.toQueryPersonsRequest(): QueryPersonsRequest? {
    val subQueries = directoryPackages.flatMap { personSubQueriesOf(it, criteriaFor(it)) }
    if (subQueries.isEmpty()) return null
    return QueryPersonsRequest.builder().setSubQueries(subQueries).build()
}

/**
 * Returns the request for this query, or `null` if it contains no criteria, since Google Play
 * services rejects a request without sub-queries.
 */
internal fun GroupQuery.toQueryGroupsRequest(): QueryGroupsRequest? {
    val subQueries = directoryPackages.flatMap { groupSubQueriesOf(it, criteriaFor(it)) }
    if (subQueries.isEmpty()) return null
    return QueryGroupsRequest.builder().setSubQueries(subQueries).build()
}

/** Returns the notification that the people of [directoryPackage] matching [criteria] changed. */
internal fun personsChangedRequestOf(
    directoryPackage: String,
    criteria: Set<Criterion>,
): NotifyDirectoryChangeRequest =
    NotifyDirectoryChangeRequest.builder()
        .setQueryType(NotifyDirectoryChangeRequest.QUERY_TYPE_PERSONS)
        .setModifiedQueries(personSubQueriesOf(directoryPackage, criteria))
        .build()

/** Returns the notification that the groups of [directoryPackage] matching [criteria] changed. */
internal fun groupsChangedRequestOf(
    directoryPackage: String,
    criteria: Set<Criterion>,
): NotifyDirectoryChangeRequest =
    NotifyDirectoryChangeRequest.builder()
        .setQueryType(NotifyDirectoryChangeRequest.QUERY_TYPE_GROUPS)
        .setModifiedQueries(groupSubQueriesOf(directoryPackage, criteria))
        .build()

/**
 * Returns the request that mints a [android.app.PendingIntent] for this interaction.
 *
 * @throws IllegalArgumentException if the target criteria are of more than one type, since Google
 *   Play services takes a single selection type per interaction, or if the capability has no
 *   interaction type.
 */
internal fun PeopleInteraction.toCreateInteractionPendingIntentRequest():
    CreateInteractionPendingIntentRequest {
    val selections = targetCriteria.map { it.toPersonSelection() }
    val selectionType =
        requireNotNull(selections.map { it.first }.distinct().singleOrNull()) {
            "Google Play services takes one selection type per interaction: $targetCriteria"
        }
    return CreateInteractionPendingIntentRequest.builder()
        .setTargetPackage(directoryPackage)
        .setInteractionType(capability.toInteractionType())
        .setSelectionType(selectionType)
        .setSelectionValues(selections.map { it.second })
        .build()
}

private fun Capability.toInteractionType(): String =
    when (this) {
        Capability.VOICE -> CreateInteractionPendingIntentRequest.PEOPLE_INTERACTION_TYPE_VOICE
        Capability.VIDEO -> CreateInteractionPendingIntentRequest.PEOPLE_INTERACTION_TYPE_VIDEO
        Capability.MESSAGE -> CreateInteractionPendingIntentRequest.PEOPLE_INTERACTION_TYPE_MESSAGE
        else -> throw IllegalArgumentException("Unsupported capability: $this")
    }

/** Returns the person selection type and value of this criterion. */
private fun Criterion.toPersonSelection(): Pair<String, String> =
    when (this) {
        is Criterion.Phone -> Persons.Query.SELECTION_TYPE_PHONE to number
        is Criterion.Email -> Persons.Query.SELECTION_TYPE_EMAIL to address
        is Criterion.PersonLookupKey -> Persons.Query.SELECTION_TYPE_LOOKUP_KEY to key
        is Criterion.GroupLookupKey -> Persons.Query.SELECTION_TYPE_GROUP_LOOKUP_KEY to key
        else -> throw IllegalArgumentException("$this has no person selection type")
    }

/** Returns the group selection type and value of this criterion. */
private fun Criterion.toGroupSelection(): Pair<String, String> =
    when (this) {
        is Criterion.GroupLookupKey -> Groups.Query.SELECTION_TYPE_LOOKUP_KEY to key
        else -> throw IllegalArgumentException("$this has no group selection type")
    }

/**
 * Returns one sub-query per person selection type in [criteria], restricted to [directoryPackage],
 * in the order the types first appear.
 */
private fun personSubQueriesOf(
    directoryPackage: String,
    criteria: Set<Criterion>,
): List<PeopleProviderSubQuery> =
    subQueriesOf(directoryPackage, criteria.map { it.toPersonSelection() })

/** Returns the group sub-queries of [criteria], as [personSubQueriesOf] does for people. */
private fun groupSubQueriesOf(
    directoryPackage: String,
    criteria: Set<Criterion>,
): List<PeopleProviderSubQuery> =
    subQueriesOf(directoryPackage, criteria.map { it.toGroupSelection() })

private fun subQueriesOf(
    directoryPackage: String,
    selections: List<Pair<String, String>>,
): List<PeopleProviderSubQuery> =
    selections.groupBy({ it.first }, { it.second }).map { (selectionType, values) ->
        PeopleProviderSubQuery.builder()
            .setTargetPackages(listOf(directoryPackage))
            .setSelectionType(selectionType)
            .setSelectionValues(values)
            .build()
    }
