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

/**
 * An immutable set of criteria for looking up people in one People Directory application.
 *
 * Criteria of different kinds are combined with OR: a person is returned if it matches any phone
 * number, any email address, any lookup key, or any group lookup key in the query. Each result
 * reports every criterion that matched it through [Person.matchedCriteria], so a batched lookup can
 * be correlated back to its inputs.
 *
 * ```
 * val query = personQuery("com.example.voip") {
 *     addPhones("+15551234567", "+15557654321")
 *     addEmails("someone@example.com")
 * }
 * ```
 */
public class PersonQuery
internal constructor(
    /** Package name of the People Directory application this query reads from. */
    public val directoryPackage: String,
    /** Phone numbers to match, possibly empty. */
    public val phones: List<String>,
    /** Email addresses to match, possibly empty. */
    public val emails: List<String>,
    /** Person lookup keys to match, possibly empty. */
    public val lookupKeys: List<String>,
    /** Lookup keys of groups whose members should be returned, possibly empty. */
    public val groupLookupKeys: List<String>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PersonQuery) return false
        return directoryPackage == other.directoryPackage &&
            phones == other.phones &&
            emails == other.emails &&
            lookupKeys == other.lookupKeys &&
            groupLookupKeys == other.groupLookupKeys
    }

    override fun hashCode(): Int {
        var result = directoryPackage.hashCode()
        result = 31 * result + phones.hashCode()
        result = 31 * result + emails.hashCode()
        result = 31 * result + lookupKeys.hashCode()
        result = 31 * result + groupLookupKeys.hashCode()
        return result
    }

    override fun toString(): String =
        "PersonQuery(directoryPackage=$directoryPackage, phones=$phones, emails=$emails, " +
            "lookupKeys=$lookupKeys, groupLookupKeys=$groupLookupKeys)"
}

/**
 * Receiver scope for configuring person queries across multiple directories.
 *
 * Only globally identifiable criteria (such as phone numbers and email addresses) can be queried
 * across multiple directory applications.
 */
@PeopleQueryDsl
public open class MultiDirectoryPersonQueryScope internal constructor() {
    internal val phones = mutableListOf<String>()
    internal val emails = mutableListOf<String>()

    /**
     * Matches people reachable at any of [phones].
     *
     * @throws IllegalArgumentException if [phones] is empty.
     */
    public fun addPhones(vararg phones: String) {
        require(phones.isNotEmpty()) { "At least one phone number is required." }
        this.phones += phones
    }

    /**
     * Matches people reachable at any of [phones].
     *
     * @throws IllegalArgumentException if [phones] is empty.
     */
    public fun addPhones(phones: Iterable<String>) {
        val list = phones.toList()
        require(list.isNotEmpty()) { "At least one phone number is required." }
        this.phones += list
    }

    /**
     * Matches people reachable at any of [emails].
     *
     * @throws IllegalArgumentException if [emails] is empty.
     */
    public fun addEmails(vararg emails: String) {
        require(emails.isNotEmpty()) { "At least one email address is required." }
        this.emails += emails
    }

    /**
     * Matches people reachable at any of [emails].
     *
     * @throws IllegalArgumentException if [emails] is empty.
     */
    public fun addEmails(emails: Iterable<String>) {
        val list = emails.toList()
        require(list.isNotEmpty()) { "At least one email address is required." }
        this.emails += list
    }
}

/**
 * Receiver scope for configuring a [PersonQuery] for a single directory.
 *
 * In addition to global criteria ([phones] and [emails]), single-directory queries may match
 * directory-specific [lookupKeys] and [groupLookupKeys].
 */
@PeopleQueryDsl
public class PersonQueryScope internal constructor(private val directoryPackage: String) :
    MultiDirectoryPersonQueryScope() {
    private val lookupKeys = mutableListOf<String>()
    private val groupLookupKeys = mutableListOf<String>()

    /**
     * Matches the people identified by any of [lookupKeys].
     *
     * @throws IllegalArgumentException if [lookupKeys] is empty.
     */
    public fun addLookupKeys(vararg lookupKeys: String) {
        require(lookupKeys.isNotEmpty()) { "At least one lookup key is required." }
        this.lookupKeys += lookupKeys
    }

    /**
     * Matches the people identified by any of [lookupKeys].
     *
     * @throws IllegalArgumentException if [lookupKeys] is empty.
     */
    public fun addLookupKeys(lookupKeys: Iterable<String>) {
        val list = lookupKeys.toList()
        require(list.isNotEmpty()) { "At least one lookup key is required." }
        this.lookupKeys += list
    }

    /**
     * Matches the members of the groups identified by any of [groupLookupKeys].
     *
     * @throws IllegalArgumentException if [groupLookupKeys] is empty.
     */
    public fun addGroupLookupKeys(vararg groupLookupKeys: String) {
        require(groupLookupKeys.isNotEmpty()) { "At least one group lookup key is required." }
        this.groupLookupKeys += groupLookupKeys
    }

    /**
     * Matches the members of the groups identified by any of [groupLookupKeys].
     *
     * @throws IllegalArgumentException if [groupLookupKeys] is empty.
     */
    public fun addGroupLookupKeys(groupLookupKeys: Iterable<String>) {
        val list = groupLookupKeys.toList()
        require(list.isNotEmpty()) { "At least one group lookup key is required." }
        this.groupLookupKeys += list
    }

    /**
     * Builds the query.
     *
     * @throws IllegalArgumentException if no criterion was added.
     */
    internal fun build(): PersonQuery {
        require(
            phones.isNotEmpty() ||
                emails.isNotEmpty() ||
                lookupKeys.isNotEmpty() ||
                groupLookupKeys.isNotEmpty()
        ) {
            "A PersonQuery needs at least one criterion."
        }
        return PersonQuery(
            directoryPackage = directoryPackage,
            phones = phones.toList(),
            emails = emails.toList(),
            lookupKeys = lookupKeys.toList(),
            groupLookupKeys = groupLookupKeys.toList(),
        )
    }
}

/**
 * Builds a [PersonQuery] against the People Directory application [directoryPackage].
 *
 * @throws IllegalArgumentException if [block] adds no criterion.
 */
public fun personQuery(
    directoryPackage: String,
    block: PersonQueryScope.() -> Unit,
): PersonQuery = PersonQueryScope(directoryPackage).apply(block).build()

/**
 * Builds [PersonQuery] instances with the same criteria for each of [directoryPackages].
 *
 * @throws IllegalArgumentException if [directoryPackages] is empty, or if [block] adds no
 *   criterion.
 */
public fun personQueries(
    vararg directoryPackages: String,
    block: MultiDirectoryPersonQueryScope.() -> Unit,
): List<PersonQuery> {
    require(directoryPackages.isNotEmpty()) { "At least one directory package is required." }
    return personQueries(directoryPackages.asIterable(), block)
}

/**
 * Builds [PersonQuery] instances with the same criteria for each of [directoryPackages].
 *
 * @throws IllegalArgumentException if [directoryPackages] is empty, or if [block] adds no
 *   criterion.
 */
public fun personQueries(
    directoryPackages: Iterable<String>,
    block: MultiDirectoryPersonQueryScope.() -> Unit,
): List<PersonQuery> {
    val packages = directoryPackages.toList()
    require(packages.isNotEmpty()) { "At least one directory package is required." }
    val scope = MultiDirectoryPersonQueryScope().apply(block)
    require(scope.phones.isNotEmpty() || scope.emails.isNotEmpty()) {
        "A PersonQuery needs at least one criterion."
    }
    return packages.map { pkg ->
        PersonQuery(
            directoryPackage = pkg,
            phones = scope.phones.toList(),
            emails = scope.emails.toList(),
            lookupKeys = emptyList(),
            groupLookupKeys = emptyList(),
        )
    }
}
