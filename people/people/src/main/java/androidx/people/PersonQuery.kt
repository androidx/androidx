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

@file:JvmName("PersonQueries")

package androidx.people

import androidx.annotation.RestrictTo
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.internal.addNonEmpty
import androidx.people.internal.directoryPersonQuery

/**
 * A query for people across one or more People Directory applications.
 *
 * Each People Directory application in the query receives only the criteria addressed to it. Build
 * one with [personQuery]:
 * ```
 * val query = personQuery {
 *     directories(packages) { addPhones("+16505550100") }      // phones and emails only
 *     directory("com.example.voip") { addLookupKeys("user_1") } // every criterion type
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PersonQuery
internal constructor(
    /** The criteria addressed to each People Directory application, keyed by package name. */
    internal val directoryQueries: Map<String, DirectoryPersonQuery>
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
     * Returns whether this query contains no criterion. Queries built with [personQuery] are never
     * empty; the queries reported by a [PersonQueryResult] may be.
     */
    public fun isEmpty(): Boolean = directoryQueries.isEmpty()

    /** Returns the union of this query and [other], directory by directory. */
    internal operator fun plus(other: PersonQuery): PersonQuery =
        personQueryOf(
            (directoryQueries.keys + other.directoryQueries.keys).associateWith {
                criteriaFor(it) + other.criteriaFor(it)
            }
        )

    /**
     * Returns this query without the criteria of [other], directory by directory. Directories left
     * without criteria are dropped.
     */
    internal operator fun minus(other: PersonQuery): PersonQuery =
        personQueryOf(
            directoryQueries.mapValues { (directoryPackage, query) ->
                query.criteria - other.criteriaFor(directoryPackage)
            }
        )

    override fun equals(other: Any?): Boolean =
        this === other || (other is PersonQuery && directoryQueries == other.directoryQueries)

    override fun hashCode(): Int = directoryQueries.hashCode()

    override fun toString(): String = "PersonQuery(directoryQueries=$directoryQueries)"
}

/**
 * Returns the [PersonQuery] carrying [criteriaByPackage], omitting packages without criteria. The
 * result is empty if no package has criteria.
 */
internal fun personQueryOf(criteriaByPackage: Map<String, Set<Criterion>>): PersonQuery =
    PersonQuery(
        criteriaByPackage
            .filterValues { it.isNotEmpty() }
            .mapValues { (_, criteria) -> DirectoryPersonQuery(criteria) }
    )

/**
 * Receiver scope for person criteria that identify a person across People Directory applications,
 * such as phone numbers and email addresses.
 *
 * Used by [PersonQueryScope.directories] and [personQuery] to send the same criteria to several
 * directories. [PersonCriteriaScope] adds the criteria that only make sense within one directory.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@PeopleQueryDsl
public open class SharedPersonCriteriaScope internal constructor() {
    internal val criteria = mutableSetOf<Criterion>()

    /**
     * Matches people reachable at any of [phones].
     *
     * @throws IllegalArgumentException if [phones] is empty.
     */
    public fun addPhones(vararg phones: String) {
        addPhones(phones.asList())
    }

    /**
     * Matches people reachable at any of [phones].
     *
     * @throws IllegalArgumentException if [phones] is empty.
     */
    public fun addPhones(phones: Iterable<String>) {
        criteria.addNonEmpty(phones.map(Criterion::Phone), "phone number")
    }

    /**
     * Matches people reachable at any of [emails].
     *
     * @throws IllegalArgumentException if [emails] is empty.
     */
    public fun addEmails(vararg emails: String) {
        addEmails(emails.asList())
    }

    /**
     * Matches people reachable at any of [emails].
     *
     * @throws IllegalArgumentException if [emails] is empty.
     */
    public fun addEmails(emails: Iterable<String>) {
        criteria.addNonEmpty(emails.map(Criterion::Email), "email address")
    }

    /**
     * Builds the criteria-only query.
     *
     * @throws IllegalArgumentException if no criterion was added.
     */
    internal fun buildDirectoryQuery(): DirectoryPersonQuery = DirectoryPersonQuery(criteria)
}

/**
 * Receiver scope for the person criteria addressed to one People Directory application.
 *
 * In addition to the criteria of [SharedPersonCriteriaScope], this allows directory-specific lookup
 * keys. Used by [PersonQueryScope.directory], [personQuery], and
 * [PeopleDirectoryContentProvider.notifyPersonsChanged].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@PeopleQueryDsl
public class PersonCriteriaScope internal constructor() : SharedPersonCriteriaScope() {

    /**
     * Matches the people identified by any of [lookupKeys].
     *
     * @throws IllegalArgumentException if [lookupKeys] is empty.
     */
    public fun addLookupKeys(vararg lookupKeys: String) {
        addLookupKeys(lookupKeys.asList())
    }

    /**
     * Matches the people identified by any of [lookupKeys].
     *
     * @throws IllegalArgumentException if [lookupKeys] is empty.
     */
    public fun addLookupKeys(lookupKeys: Iterable<String>) {
        criteria.addNonEmpty(lookupKeys.map(Criterion::PersonLookupKey), "lookup key")
    }

    /**
     * Matches the members of the groups identified by any of [groupLookupKeys].
     *
     * @throws IllegalArgumentException if [groupLookupKeys] is empty.
     */
    public fun addGroupLookupKeys(vararg groupLookupKeys: String) {
        addGroupLookupKeys(groupLookupKeys.asList())
    }

    /**
     * Matches the members of the groups identified by any of [groupLookupKeys].
     *
     * @throws IllegalArgumentException if [groupLookupKeys] is empty.
     */
    public fun addGroupLookupKeys(groupLookupKeys: Iterable<String>) {
        criteria.addNonEmpty(groupLookupKeys.map(Criterion::GroupLookupKey), "group lookup key")
    }
}

/** Receiver scope for choosing the People Directory applications of a [PersonQuery]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@PeopleQueryDsl
public class PersonQueryScope internal constructor() {
    private val criteriaByPackage = mutableMapOf<String, MutableSet<Criterion>>()

    /**
     * Adds criteria for the People Directory application [directoryPackage]. Every criterion type
     * is allowed, including directory-specific lookup keys.
     *
     * @throws IllegalArgumentException if [block] adds no criterion.
     */
    public fun directory(directoryPackage: String, block: PersonCriteriaScope.() -> Unit) {
        add(listOf(directoryPackage), directoryPersonQuery(block))
    }

    /**
     * Adds the same criteria for each of [directoryPackages]. Only criteria that identify a person
     * across applications, such as phone numbers and email addresses, are allowed.
     *
     * @throws IllegalArgumentException if [directoryPackages] is empty, or if [block] adds no
     *   criterion.
     */
    public fun directories(
        vararg directoryPackages: String,
        block: SharedPersonCriteriaScope.() -> Unit,
    ) {
        directories(directoryPackages.asList(), block)
    }

    /**
     * Adds the same criteria for each of [directoryPackages]. Only criteria that identify a person
     * across applications, such as phone numbers and email addresses, are allowed.
     *
     * @throws IllegalArgumentException if [directoryPackages] is empty, or if [block] adds no
     *   criterion.
     */
    public fun directories(
        directoryPackages: Iterable<String>,
        block: SharedPersonCriteriaScope.() -> Unit,
    ) {
        val packages = directoryPackages.toList()
        require(packages.isNotEmpty()) { "At least one directory package is required." }
        add(packages, SharedPersonCriteriaScope().apply(block).buildDirectoryQuery())
    }

    private fun add(directoryPackages: List<String>, query: DirectoryPersonQuery) {
        for (directoryPackage in directoryPackages) {
            require(directoryPackage.isNotEmpty()) { "A directory package must not be empty." }
            criteriaByPackage.getOrPut(directoryPackage) { mutableSetOf() } += query.criteria
        }
    }

    internal fun build(): PersonQuery {
        require(criteriaByPackage.isNotEmpty()) { "A PersonQuery needs at least one directory." }
        return PersonQuery(criteriaByPackage.mapValues { DirectoryPersonQuery(it.value) })
    }
}

/**
 * Builds a [PersonQuery] that may address different criteria to different People Directory
 * applications.
 *
 * @throws IllegalArgumentException if [block] adds no directory, or adds a directory without
 *   criteria.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun personQuery(block: PersonQueryScope.() -> Unit): PersonQuery =
    PersonQueryScope().apply(block).build()

/**
 * Builds a [PersonQuery] against the single People Directory application [directoryPackage].
 *
 * @throws IllegalArgumentException if [block] adds no criterion.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun personQuery(
    directoryPackage: String,
    block: PersonCriteriaScope.() -> Unit,
): PersonQuery = personQuery { directory(directoryPackage, block) }

/**
 * Builds a [PersonQuery] with the same criteria for each of [directoryPackages].
 *
 * @throws IllegalArgumentException if [directoryPackages] is empty, or if [block] adds no
 *   criterion.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun personQuery(
    directoryPackages: Iterable<String>,
    block: SharedPersonCriteriaScope.() -> Unit,
): PersonQuery = personQuery { directories(directoryPackages, block) }
