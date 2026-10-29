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
import androidx.core.os.BundleCompat
import androidx.people.Criterion
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.PeopleInteraction

/**
 * Serializes these per-directory queries, keyed by package name, into a query arguments [Bundle].
 */
internal fun Map<String, DirectoryPersonQuery>.toPersonQueryArgs(): Bundle =
    personQueryArgsOf(mapValues { (_, query) -> query.criteria })

/**
 * Serializes these per-directory queries, keyed by package name, into a query arguments [Bundle].
 */
internal fun Map<String, DirectoryGroupQuery>.toGroupQueryArgs(): Bundle =
    groupQueryArgsOf(mapValues { (_, query) -> query.criteria })

/** Serializes [criteriaByPackage] into a query arguments [Bundle] of person selections. */
internal fun personQueryArgsOf(criteriaByPackage: Map<String, Set<Criterion>>): Bundle =
    queryArgsOf(
        criteriaByPackage.flatMap { (directoryPackage, criteria) ->
            subQueriesOf(directoryPackage, criteria.map { it.toPersonSelection() })
        }
    )

/** Serializes [criteriaByPackage] into a query arguments [Bundle] of group selections. */
internal fun groupQueryArgsOf(criteriaByPackage: Map<String, Set<Criterion>>): Bundle =
    queryArgsOf(
        criteriaByPackage.flatMap { (directoryPackage, criteria) ->
            subQueriesOf(directoryPackage, criteria.map { it.toGroupSelection() })
        }
    )

/**
 * Parses these query arguments into person criteria keyed by package name, in the order the
 * packages first appear. Packages without a recognized criterion are omitted.
 */
internal fun Bundle?.personCriteriaByPackage(): Map<String, Set<Criterion>> = criteriaByPackage {
    it.toPersonCriterion()
}

/**
 * Parses these query arguments into group criteria keyed by package name, in the order the packages
 * first appear. Packages without a recognized criterion are omitted.
 */
internal fun Bundle?.groupCriteriaByPackage(): Map<String, Set<Criterion>> = criteriaByPackage {
    it.toGroupCriterion()
}

/** Serializes this interaction's target criteria into an interaction query arguments [Bundle]. */
internal fun PeopleInteraction.toInteractionQueryArgs(): Bundle =
    personQueryArgsOf(mapOf(directoryPackage to targetCriteria)).apply {
        putParcelable(
            PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI,
            PeopleContractCompat.Persons.CONTENT_URI,
        )
    }

private inline fun Bundle?.criteriaByPackage(
    toCriterion: (Selection) -> Criterion?
): Map<String, Set<Criterion>> = buildMap {
    for ((directoryPackage, subQueries) in subQueriesByPackage()) {
        val criteria = subQueries.selections().mapNotNullTo(mutableSetOf(), toCriterion)
        if (criteria.isNotEmpty()) put(directoryPackage, criteria)
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
