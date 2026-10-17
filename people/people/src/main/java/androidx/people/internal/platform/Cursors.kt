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

import android.database.Cursor
import android.database.MatrixCursor
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.people.Criterion
import androidx.people.DirectoryInfo
import androidx.people.Group
import androidx.people.GroupId
import androidx.people.GroupQuery
import androidx.people.Person
import androidx.people.PersonId
import androidx.people.PersonQuery
import androidx.people.internal.platform.PeopleContractCompat.Directory
import androidx.people.internal.platform.PeopleContractCompat.Groups
import androidx.people.internal.platform.PeopleContractCompat.Persons

private const val TAG = "PeopleCursors"

/** The columns written for persons when the caller does not request a projection. */
internal val DEFAULT_PERSON_PROJECTION: Array<String> =
    arrayOf(
        Persons.LOOKUP_KEY,
        Persons.DISPLAY_NAME,
        Persons.PHOTO_URI,
        Persons.CAPABILITY,
        Persons.IS_KNOWN,
        PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
        PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
        Directory.PACKAGE_NAME,
    )

/** The columns written for groups when the caller does not request a projection. */
internal val DEFAULT_GROUP_PROJECTION: Array<String> =
    arrayOf(
        Groups.LOOKUP_KEY,
        Groups.DISPLAY_NAME,
        PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
        PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
        Directory.PACKAGE_NAME,
    )

/**
 * Serializes these persons into a [Cursor] according to [projection].
 *
 * Each person is written as one row per matched [Criterion], or as a single row with `null`
 * selection columns if it reports none. When [query] is non-null, criteria that do not appear in
 * [query] are dropped, which enforces the [Criterion] echo contract. Criterion types that have no
 * person selection type are also dropped.
 */
@JvmName("personsToCursor")
internal fun List<Person>.toCursor(
    projection: Array<out String>? = null,
    query: PersonQuery? = null,
): Cursor {
    val columns = projection ?: DEFAULT_PERSON_PROJECTION
    val allowedCriteria = query?.toCriteria()?.toSet()
    var dropped = 0
    val cursor = MatrixCursor(columns, size)
    for (person in this) {
        val selections =
            person.matchedCriteria
                .filter { allowedCriteria == null || it in allowedCriteria }
                .mapNotNull { it.toPersonSelection() }
        dropped += person.matchedCriteria.size - selections.size
        for (selection in selections.ifEmpty { listOf(null) }) {
            cursor.addRow(
                columns.map { column ->
                    when (column) {
                        Persons.LOOKUP_KEY -> person.id.lookupKey
                        Persons.DISPLAY_NAME -> person.displayName
                        Persons.PHOTO_URI -> person.photoUri?.toString()
                        Persons.CAPABILITY -> person.capabilities.toReachabilityBitmask()
                        Persons.IS_KNOWN -> if (person.isKnown) 1 else 0
                        PeopleContractCompat.QUERY_ARG_SELECTION_TYPE -> selection?.type
                        PeopleContractCompat.QUERY_ARG_SELECTION_VALUE -> selection?.value
                        Directory.PACKAGE_NAME -> person.id.directoryPackage
                        else -> null
                    }
                }
            )
        }
    }
    logDropped(dropped)
    return cursor
}

/**
 * Serializes these groups into a [Cursor] according to [projection].
 *
 * Each group is written as one row per matched [Criterion], or as a single row with `null`
 * selection columns if it reports none. When [query] is non-null, criteria that do not appear in
 * [query] are dropped, which enforces the [Criterion] echo contract. Criterion types that have no
 * group selection type are also dropped.
 */
@JvmName("groupsToCursor")
internal fun List<Group>.toCursor(
    projection: Array<out String>? = null,
    query: GroupQuery? = null,
): Cursor {
    val columns = projection ?: DEFAULT_GROUP_PROJECTION
    val allowedCriteria = query?.toCriteria()?.toSet()
    var dropped = 0
    val cursor = MatrixCursor(columns, size)
    for (group in this) {
        val selections =
            group.matchedCriteria
                .filter { allowedCriteria == null || it in allowedCriteria }
                .mapNotNull { it.toGroupSelection() }
        dropped += group.matchedCriteria.size - selections.size
        for (selection in selections.ifEmpty { listOf(null) }) {
            cursor.addRow(
                columns.map { column ->
                    when (column) {
                        Groups.LOOKUP_KEY -> group.id.lookupKey
                        Groups.DISPLAY_NAME -> group.displayName
                        PeopleContractCompat.QUERY_ARG_SELECTION_TYPE -> selection?.type
                        PeopleContractCompat.QUERY_ARG_SELECTION_VALUE -> selection?.value
                        Directory.PACKAGE_NAME -> group.id.directoryPackage
                        else -> null
                    }
                }
            )
        }
    }
    logDropped(dropped)
    return cursor
}

/**
 * Parses this [Cursor], formatted by [PeopleContractCompat.Persons], into [Person] records.
 *
 * Rows that share a [PersonId] are merged into a single [Person] whose [Person.matchedCriteria] is
 * the union of every row's criterion. Non-criterion fields are taken from the first row for that
 * person, and persons are returned in the order they first appear. Rows whose selection type is not
 * recognized still contribute the person, but no criterion.
 */
internal fun Cursor.toPersons(): List<Person> {
    val lookupKeyIndex = getColumnIndexOrThrow(Persons.LOOKUP_KEY)
    val packageIndex = getColumnIndexOrThrow(Directory.PACKAGE_NAME)
    val displayNameIndex = getColumnIndexOrThrow(Persons.DISPLAY_NAME)
    val photoUriIndex = getColumnIndexOrThrow(Persons.PHOTO_URI)
    val capabilityIndex = getColumnIndexOrThrow(Persons.CAPABILITY)
    val isKnownIndex = getColumnIndexOrThrow(Persons.IS_KNOWN)
    val selectionTypeIndex = getColumnIndexOrThrow(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE)
    val selectionValueIndex = getColumnIndexOrThrow(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)

    val firstRows = LinkedHashMap<PersonId, Person>()
    val criteria = HashMap<PersonId, MutableSet<Criterion>>()
    while (moveToNext()) {
        val lookupKey = getStringOrNull(lookupKeyIndex)?.takeIf { it.isNotBlank() } ?: continue
        val id = PersonId(lookupKey, getStringOrNull(packageIndex).orEmpty())
        firstRows.getOrPut(id) {
            Person(
                id = id,
                displayName = getStringOrNull(displayNameIndex),
                photoUri = getStringOrNull(photoUriIndex)?.toUri(),
                capabilities =
                    capabilitiesFromReachabilityBitmask(getIntOrNull(capabilityIndex) ?: 0),
                isKnown = (getIntOrNull(isKnownIndex) ?: 0) != 0,
                matchedCriteria = emptySet(),
            )
        }
        val personCriteria = criteria.getOrPut(id) { mutableSetOf() }
        selectionOrNull(selectionTypeIndex, selectionValueIndex)
            ?.toPersonCriterion()
            ?.let(personCriteria::add)
    }
    return firstRows.values.map { person ->
        Person(
            id = person.id,
            displayName = person.displayName,
            photoUri = person.photoUri,
            capabilities = person.capabilities,
            isKnown = person.isKnown,
            matchedCriteria = criteria.getValue(person.id),
        )
    }
}

/**
 * Parses this [Cursor], formatted by [PeopleContractCompat.Groups], into [Group] records.
 *
 * Rows that share a [GroupId] are merged the same way as [toPersons].
 */
internal fun Cursor.toGroups(): List<Group> {
    val lookupKeyIndex = getColumnIndexOrThrow(Groups.LOOKUP_KEY)
    val packageIndex = getColumnIndexOrThrow(Directory.PACKAGE_NAME)
    val displayNameIndex = getColumnIndexOrThrow(Groups.DISPLAY_NAME)
    val selectionTypeIndex = getColumnIndexOrThrow(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE)
    val selectionValueIndex = getColumnIndexOrThrow(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)

    val displayNames = LinkedHashMap<GroupId, String?>()
    val criteria = HashMap<GroupId, MutableSet<Criterion>>()
    while (moveToNext()) {
        val lookupKey = getStringOrNull(lookupKeyIndex)?.takeIf { it.isNotBlank() } ?: continue
        val id = GroupId(lookupKey, getStringOrNull(packageIndex).orEmpty())
        if (id !in displayNames) displayNames[id] = getStringOrNull(displayNameIndex)
        val groupCriteria = criteria.getOrPut(id) { mutableSetOf() }
        selectionOrNull(selectionTypeIndex, selectionValueIndex)
            ?.toGroupCriterion()
            ?.let(groupCriteria::add)
    }
    return displayNames.map { (id, displayName) ->
        Group(id = id, displayName = displayName, matchedCriteria = criteria.getValue(id))
    }
}

/** Parses this [Cursor], formatted by [PeopleContractCompat.Directory], into directory records. */
internal fun Cursor.toDirectories(): List<DirectoryInfo> {
    val packageIndex = getColumnIndexOrThrow(Directory.PACKAGE_NAME)
    val authorityIndex = getColumnIndexOrThrow(Directory.DIRECTORY_AUTHORITY)
    return buildList {
        while (moveToNext()) {
            val packageName = getStringOrNull(packageIndex)?.takeIf { it.isNotBlank() }
            val authority = getStringOrNull(authorityIndex)?.takeIf { it.isNotBlank() }
            if (packageName != null && authority != null) {
                add(DirectoryInfo(packageName = packageName, authority = authority))
            }
        }
    }
}

/**
 * Returns the [Selection] in the current row's selection columns, or `null` if either column is
 * `null`.
 */
private fun Cursor.selectionOrNull(typeIndex: Int, valueIndex: Int): Selection? {
    val type = getStringOrNull(typeIndex) ?: return null
    val value = getStringOrNull(valueIndex) ?: return null
    return Selection(type, value)
}

/** Logs that [count] matched criteria were dropped from a result cursor, if any were. */
private fun logDropped(count: Int) {
    if (count > 0) {
        Log.w(TAG, "Dropped $count matched criteria that are not present in the query")
    }
}
