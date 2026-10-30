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

@file:JvmName("GroupQueries")

package androidx.people

import androidx.annotation.RestrictTo
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.internal.addNonEmpty
import androidx.people.internal.directoryGroupQuery

/**
 * A query for groups across one or more People Directory applications.
 *
 * Each People Directory application in the query receives only the criteria addressed to it. Build
 * one with [groupQuery]:
 * ```
 * val query = groupQuery("com.example.voip") { addLookupKeys("group-1", "group-2") }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GroupQuery
internal constructor(
    /** The criteria addressed to each People Directory application, keyed by package name. */
    internal val directoryQueries: Map<String, DirectoryGroupQuery>
) {
    /** The package names of the People Directory applications this query addresses. */
    public val directoryPackages: Set<String>
        get() = directoryQueries.keys

    /**
     * Returns the criteria this query addresses to the People Directory application
     * [directoryPackage], in the order they were added, or an empty set if it addresses none.
     */
    public fun criteriaFor(directoryPackage: String): Set<Criterion> =
        directoryQueries[directoryPackage]?.criteria.orEmpty()

    /**
     * Returns whether this query contains no criterion. Queries built with [groupQuery] are never
     * empty; the queries reported by a [GroupQueryResult] may be.
     */
    public fun isEmpty(): Boolean = directoryQueries.isEmpty()

    /** Returns the union of this query and [other], directory by directory. */
    internal operator fun plus(other: GroupQuery): GroupQuery =
        groupQueryOf(
            (directoryQueries.keys + other.directoryQueries.keys).associateWith {
                criteriaFor(it) + other.criteriaFor(it)
            }
        )

    /**
     * Returns this query without the criteria of [other], directory by directory. Directories left
     * without criteria are dropped.
     */
    internal operator fun minus(other: GroupQuery): GroupQuery =
        groupQueryOf(
            directoryQueries.mapValues { (directoryPackage, query) ->
                query.criteria - other.criteriaFor(directoryPackage)
            }
        )

    override fun equals(other: Any?): Boolean =
        this === other || (other is GroupQuery && directoryQueries == other.directoryQueries)

    override fun hashCode(): Int = directoryQueries.hashCode()

    override fun toString(): String = "GroupQuery(directoryQueries=$directoryQueries)"
}

/**
 * Returns the [GroupQuery] carrying [criteriaByPackage], omitting packages without criteria. The
 * result is empty if no package has criteria.
 */
internal fun groupQueryOf(criteriaByPackage: Map<String, Set<Criterion>>): GroupQuery =
    GroupQuery(
        criteriaByPackage
            .filterValues { it.isNotEmpty() }
            .mapValues { (_, criteria) -> DirectoryGroupQuery(criteria) }
    )

/**
 * Receiver scope for the group criteria addressed to one People Directory application.
 *
 * Used by [GroupQueryScope.directory], [groupQuery], and
 * [PeopleDirectoryContentProvider.notifyGroupsChanged].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@PeopleQueryDsl
public class GroupCriteriaScope internal constructor() {
    private val criteria = mutableSetOf<Criterion>()

    /**
     * Matches the groups identified by any of [lookupKeys].
     *
     * @throws IllegalArgumentException if [lookupKeys] is empty.
     */
    public fun addLookupKeys(vararg lookupKeys: String) {
        addLookupKeys(lookupKeys.asList())
    }

    /**
     * Matches the groups identified by any of [lookupKeys].
     *
     * @throws IllegalArgumentException if [lookupKeys] is empty.
     */
    public fun addLookupKeys(lookupKeys: Iterable<String>) {
        criteria.addNonEmpty(lookupKeys.map(Criterion::GroupLookupKey), "lookup key")
    }

    /**
     * Builds the criteria-only query.
     *
     * @throws IllegalArgumentException if no criterion was added.
     */
    internal fun buildDirectoryQuery(): DirectoryGroupQuery = DirectoryGroupQuery(criteria)
}

/** Receiver scope for choosing the People Directory applications of a [GroupQuery]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@PeopleQueryDsl
public class GroupQueryScope internal constructor() {
    private val criteriaByPackage = mutableMapOf<String, MutableSet<Criterion>>()

    /**
     * Adds criteria for the People Directory application [directoryPackage].
     *
     * @throws IllegalArgumentException if [block] adds no criterion.
     */
    public fun directory(directoryPackage: String, block: GroupCriteriaScope.() -> Unit) {
        require(directoryPackage.isNotEmpty()) { "A directory package must not be empty." }
        criteriaByPackage.getOrPut(directoryPackage) { mutableSetOf() } +=
            directoryGroupQuery(block).criteria
    }

    internal fun build(): GroupQuery {
        require(criteriaByPackage.isNotEmpty()) { "A GroupQuery needs at least one directory." }
        return GroupQuery(criteriaByPackage.mapValues { DirectoryGroupQuery(it.value) })
    }
}

/**
 * Builds a [GroupQuery] that may address different criteria to different People Directory
 * applications.
 *
 * @throws IllegalArgumentException if [block] adds no directory, or adds a directory without
 *   criteria.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun groupQuery(block: GroupQueryScope.() -> Unit): GroupQuery =
    GroupQueryScope().apply(block).build()

/**
 * Builds a [GroupQuery] against the single People Directory application [directoryPackage].
 *
 * @throws IllegalArgumentException if [block] adds no criterion.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun groupQuery(
    directoryPackage: String,
    block: GroupCriteriaScope.() -> Unit,
): GroupQuery = groupQuery { directory(directoryPackage, block) }
