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

package androidx.people.internal.platform

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import androidx.people.Criterion
import androidx.people.GroupQuery
import androidx.people.PersonQuery

/** Serializes these queries into a query arguments [Bundle]. */
internal fun Iterable<PersonQuery>.toQueryArgs(): Bundle =
    queryArgsOf(
        flatMap { query ->
            subQueriesOf(
                query.directoryPackage,
                query.toCriteria().mapNotNull { it.toPersonSelection() },
            )
        }
    )

/** Serializes this query into a query arguments [Bundle]. */
internal fun PersonQuery.toQueryArgs(): Bundle = listOf(this).toQueryArgs()

/** Serializes these queries into a query arguments [Bundle]. */
@JvmName("groupQueriesToQueryArgs")
internal fun Iterable<GroupQuery>.toQueryArgs(): Bundle =
    queryArgsOf(
        flatMap { query ->
            subQueriesOf(
                query.directoryPackage,
                query.toCriteria().mapNotNull { it.toGroupSelection() },
            )
        }
    )

/** Serializes this query into a query arguments [Bundle]. */
internal fun GroupQuery.toQueryArgs(): Bundle = listOf(this).toQueryArgs()

/**
 * Parses these query arguments into one [PersonQuery] per People Directory application, in the
 * order the applications first appear.
 */
internal fun Bundle?.toPersonQueries(): List<PersonQuery> =
    subQueriesByPackage().mapNotNull { (directoryPackage, subQueries) ->
        val criteria = subQueries.selections().mapNotNull { it.toPersonCriterion() }
        if (criteria.isEmpty()) null else personQueryOf(directoryPackage, criteria)
    }

/**
 * Parses these query arguments into the [PersonQuery] for the first People Directory application.
 */
internal fun Bundle?.toPersonQuery(): PersonQuery? = toPersonQueries().firstOrNull()

/**
 * Parses these query arguments into one [GroupQuery] per People Directory application, in the order
 * the applications first appear.
 */
internal fun Bundle?.toGroupQueries(): List<GroupQuery> =
    subQueriesByPackage().mapNotNull { (directoryPackage, subQueries) ->
        val lookupKeys =
            subQueries
                .selections()
                .mapNotNull { it.toGroupCriterion() }
                .filterIsInstance<Criterion.GroupLookupKey>()
                .map(Criterion.GroupLookupKey::key)
        if (lookupKeys.isEmpty()) {
            null
        } else {
            GroupQuery(directoryPackage = directoryPackage, lookupKeys = lookupKeys)
        }
    }

/**
 * Parses these query arguments into the [GroupQuery] for the first People Directory application.
 */
internal fun Bundle?.toGroupQuery(): GroupQuery? = toGroupQueries().firstOrNull()

/**
 * Returns the single criterion of this interaction target.
 *
 * `PeopleContract.createPeopleInteractionRequest` requires exactly one sub-query targeting a single
 * People Directory application, so this query must carry exactly one criterion value.
 *
 * @throws IllegalArgumentException if this query does not contain exactly one criterion value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun PersonQuery.requireInteractionTarget(): Criterion {
    val criteria = toCriteria()
    require(criteria.size == 1) {
        "An interaction target must contain exactly one phone number, email address, lookup " +
            "key, or group lookup key, but was $this"
    }
    return criteria.single()
}

/**
 * Serializes this interaction target into an interaction query arguments [Bundle].
 *
 * @throws IllegalArgumentException if this query does not contain exactly one criterion value.
 * @see requireInteractionTarget
 */
internal fun PersonQuery.toInteractionQueryArgs(): Bundle {
    val target = listOfNotNull(requireInteractionTarget().toPersonSelection())
    return queryArgsOf(subQueriesOf(directoryPackage, target)).apply {
        putParcelable(
            PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI,
            PeopleContractCompat.Persons.CONTENT_URI,
        )
    }
}

/**
 * Returns a query arguments [Bundle] carrying [subQueries].
 *
 * The list is added with [Bundle.putParcelableArrayList].
 */
private fun queryArgsOf(subQueries: List<Bundle>): Bundle =
    Bundle().apply {
        putParcelableArrayList(PeopleContractCompat.QUERY_ARG_QUERIES, ArrayList(subQueries))
    }

/**
 * Returns one sub-query per selection type in [selections], in the order the types first appear.
 */
private fun subQueriesOf(directoryPackage: String, selections: List<Selection>): List<Bundle> =
    selections.groupBy(Selection::type, Selection::value).map { (selectionType, values) ->
        Bundle().apply {
            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, directoryPackage)
            putString(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE, selectionType)
            putStringArray(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE, values.toTypedArray())
        }
    }

/** Returns the sub-queries in these query arguments, grouped by non-empty package in order. */
private fun Bundle?.subQueriesByPackage(): Map<String, List<Bundle>> {
    val subQueries =
        this?.let {
            BundleCompat.getParcelableArrayList(
                it,
                PeopleContractCompat.QUERY_ARG_QUERIES,
                Bundle::class.java,
            )
        } ?: return emptyMap()
    return subQueries
        .groupBy { it.getString(PeopleContractCompat.QUERY_ARG_PACKAGE).orEmpty() }
        .filterKeys { it.isNotEmpty() }
}

/** Returns every [Selection] across these sub-queries, in order. */
private fun List<Bundle>.selections(): List<Selection> = flatMap { subQuery ->
    val type = subQuery.getString(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE)
    val values = subQuery.getStringArray(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)
    if (type == null) emptyList() else values.orEmpty().map { Selection(type, it) }
}

/** Returns the [PersonQuery] for [directoryPackage] that carries exactly [criteria]. */
private fun personQueryOf(directoryPackage: String, criteria: List<Criterion>): PersonQuery =
    PersonQuery(
        directoryPackage = directoryPackage,
        phones = criteria.filterIsInstance<Criterion.Phone>().map { it.number },
        emails = criteria.filterIsInstance<Criterion.Email>().map { it.address },
        lookupKeys = criteria.filterIsInstance<Criterion.PersonLookupKey>().map { it.key },
        groupLookupKeys = criteria.filterIsInstance<Criterion.GroupLookupKey>().map { it.key },
    )
