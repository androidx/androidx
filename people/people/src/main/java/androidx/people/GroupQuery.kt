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

/**
 * An immutable set of criteria for looking up groups in one People Directory application.
 *
 * Each result reports every criterion that matched it through [Group.matchedCriteria].
 *
 * ```
 * val query = groupQuery("com.example.voip") {
 *     addLookupKeys("group-1", "group-2")
 * }
 * ```
 */
public class GroupQuery
internal constructor(
    /** Package name of the People Directory application this query reads from. */
    public val directoryPackage: String,
    /** Group lookup keys to match. */
    public val lookupKeys: List<String>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupQuery) return false
        return directoryPackage == other.directoryPackage && lookupKeys == other.lookupKeys
    }

    override fun hashCode(): Int = 31 * directoryPackage.hashCode() + lookupKeys.hashCode()

    override fun toString(): String =
        "GroupQuery(directoryPackage=$directoryPackage, lookupKeys=$lookupKeys)"
}

/** Receiver scope for configuring a [GroupQuery]. */
@PeopleQueryDsl
public class GroupQueryScope internal constructor(private val directoryPackage: String) {
    private val lookupKeys = mutableListOf<String>()

    /**
     * Matches the groups identified by any of [lookupKeys].
     *
     * @throws IllegalArgumentException if [lookupKeys] is empty.
     */
    public fun addLookupKeys(vararg lookupKeys: String) {
        require(lookupKeys.isNotEmpty()) { "At least one lookup key is required." }
        this.lookupKeys += lookupKeys
    }

    /**
     * Matches the groups identified by any of [lookupKeys].
     *
     * @throws IllegalArgumentException if [lookupKeys] is empty.
     */
    public fun addLookupKeys(lookupKeys: Iterable<String>) {
        val list = lookupKeys.toList()
        require(list.isNotEmpty()) { "At least one lookup key is required." }
        this.lookupKeys += list
    }

    /**
     * Builds the query.
     *
     * @throws IllegalArgumentException if no criterion was added.
     */
    internal fun build(): GroupQuery {
        require(lookupKeys.isNotEmpty()) { "A GroupQuery needs at least one lookup key." }
        return GroupQuery(directoryPackage, lookupKeys.toList())
    }
}

/**
 * Builds a [GroupQuery] against the People Directory application [directoryPackage].
 *
 * @throws IllegalArgumentException if [block] adds no criterion.
 */
public fun groupQuery(directoryPackage: String, block: GroupQueryScope.() -> Unit): GroupQuery =
    GroupQueryScope(directoryPackage).apply(block).build()
