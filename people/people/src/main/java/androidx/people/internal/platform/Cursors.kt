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
import android.os.Bundle
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.people.Criterion
import androidx.people.DirectoryInfo
import androidx.people.Group
import androidx.people.GroupId
import androidx.people.GroupMatch
import androidx.people.GroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupResult
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonResult
import androidx.people.Person
import androidx.people.PersonId
import androidx.people.PersonMatch
import androidx.people.PersonQuery
import androidx.people.groupQueryOf
import androidx.people.internal.platform.PeopleContractCompat.Directory
import androidx.people.internal.platform.PeopleContractCompat.Groups
import androidx.people.internal.platform.PeopleContractCompat.Persons
import androidx.people.personQueryOf

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
 * Serializes this result of the People Directory application [directoryPackage] into a [Cursor]
 * according to [projection].
 *
 * Each match is written as one row per matched [Criterion]. Unhandled criteria are written to the
 * cursor extras under [PeopleContractCompat.EXTRA_UNHANDLED_QUERIES]. Criteria that are not in
 * [askedCriteria] are dropped, which enforces the [Criterion] echo contract, so a match left with
 * no criterion writes no row.
 */
internal fun DirectoryPersonResult.toCursor(
    directoryPackage: String,
    projection: Array<out String>?,
    askedCriteria: Set<Criterion>,
): Cursor {
    val columns = projection ?: DEFAULT_PERSON_PROJECTION
    val cursor = MatrixCursor(columns, matches.size)
    for (match in matches) {
        val person = match.person
        val selections =
            match.criteria.filter { it in askedCriteria }.map { it.toPersonSelection() }
        for (selection in selections) {
            cursor.addRow(
                columns.map { column ->
                    when (column) {
                        Persons.LOOKUP_KEY -> person.id.lookupKey
                        Persons.DISPLAY_NAME -> person.displayName
                        Persons.PHOTO_URI -> person.photoUri?.toString()
                        Persons.CAPABILITY -> person.capabilities.toReachabilityBitmask()
                        Persons.IS_KNOWN -> if (person.isKnown) 1 else 0
                        PeopleContractCompat.QUERY_ARG_SELECTION_TYPE -> selection.type
                        PeopleContractCompat.QUERY_ARG_SELECTION_VALUE -> selection.value
                        Directory.PACKAGE_NAME -> person.id.directoryPackage
                        else -> null
                    }
                }
            )
        }
    }
    val unhandled = unhandledCriteria.filterTo(mutableSetOf()) { it in askedCriteria }
    if (unhandled.isNotEmpty()) {
        cursor.extras = unhandledExtrasOf(personQueryArgsOf(mapOf(directoryPackage to unhandled)))
    }
    return cursor
}

/**
 * Serializes this result of the People Directory application [directoryPackage] into a [Cursor]
 * according to [projection], following the same rules as the person overload.
 */
internal fun DirectoryGroupResult.toCursor(
    directoryPackage: String,
    projection: Array<out String>?,
    askedCriteria: Set<Criterion>,
): Cursor {
    val columns = projection ?: DEFAULT_GROUP_PROJECTION
    val cursor = MatrixCursor(columns, matches.size)
    for (match in matches) {
        val group = match.group
        val selections = match.criteria.filter { it in askedCriteria }.map { it.toGroupSelection() }
        for (selection in selections) {
            cursor.addRow(
                columns.map { column ->
                    when (column) {
                        Groups.LOOKUP_KEY -> group.id.lookupKey
                        Groups.DISPLAY_NAME -> group.displayName
                        PeopleContractCompat.QUERY_ARG_SELECTION_TYPE -> selection.type
                        PeopleContractCompat.QUERY_ARG_SELECTION_VALUE -> selection.value
                        Directory.PACKAGE_NAME -> group.id.directoryPackage
                        else -> null
                    }
                }
            )
        }
    }
    val unhandled = unhandledCriteria.filterTo(mutableSetOf()) { it in askedCriteria }
    if (unhandled.isNotEmpty()) {
        cursor.extras = unhandledExtrasOf(groupQueryArgsOf(mapOf(directoryPackage to unhandled)))
    }
    return cursor
}

/**
 * Reads the matches in this [Cursor], formatted by [PeopleContractCompat.Persons].
 *
 * The cursor has one row per matched criterion, so the rows of a person are combined into one
 * [PersonMatch].
 */
internal fun Cursor.toPersonMatches(): Set<PersonMatch> {
    val lookupKeyIndex = getColumnIndexOrThrow(Persons.LOOKUP_KEY)
    val packageIndex = getColumnIndexOrThrow(Directory.PACKAGE_NAME)
    val displayNameIndex = getColumnIndexOrThrow(Persons.DISPLAY_NAME)
    val photoUriIndex = getColumnIndexOrThrow(Persons.PHOTO_URI)
    val capabilityIndex = getColumnIndexOrThrow(Persons.CAPABILITY)
    val isKnownIndex = getColumnIndexOrThrow(Persons.IS_KNOWN)
    val rows = readRows {
        val person =
            Person(
                id = PersonId(getString(lookupKeyIndex), getString(packageIndex)),
                displayName = getStringOrNull(displayNameIndex),
                photoUri = getStringOrNull(photoUriIndex)?.toUri(),
                capabilities = capabilitiesFromReachabilityBitmask(getInt(capabilityIndex)),
                isKnown = getInt(isKnownIndex) != 0,
            )
        PersonMatch(person, setOf(checkNotNull(readSelection().toPersonCriterion())))
    }
    return rows
        .groupBy { it.person.id }
        .values
        .mapTo(mutableSetOf()) { personRows ->
            PersonMatch(
                personRows.first().person,
                personRows.flatMapTo(mutableSetOf()) { it.criteria },
            )
        }
}

/**
 * Reads the matches in this [Cursor], formatted by [PeopleContractCompat.Groups], following the
 * same rules as [toPersonMatches].
 */
internal fun Cursor.toGroupMatches(): Set<GroupMatch> {
    val lookupKeyIndex = getColumnIndexOrThrow(Groups.LOOKUP_KEY)
    val packageIndex = getColumnIndexOrThrow(Directory.PACKAGE_NAME)
    val displayNameIndex = getColumnIndexOrThrow(Groups.DISPLAY_NAME)
    val rows = readRows {
        val group =
            Group(
                id = GroupId(getString(lookupKeyIndex), getString(packageIndex)),
                displayName = getStringOrNull(displayNameIndex),
            )
        GroupMatch(group, setOf(checkNotNull(readSelection().toGroupCriterion())))
    }
    return rows
        .groupBy { it.group.id }
        .values
        .mapTo(mutableSetOf()) { groupRows ->
            GroupMatch(groupRows.first().group, groupRows.flatMapTo(mutableSetOf()) { it.criteria })
        }
}

/** Returns the unhandled person criteria in this cursor's extras, as a [PersonQuery]. */
internal fun Cursor.unhandledPersonQuery(): PersonQuery =
    personQueryOf(unhandledExtra().personCriteriaByPackage())

/** Returns the unhandled group criteria in this cursor's extras, as a [GroupQuery]. */
internal fun Cursor.unhandledGroupQuery(): GroupQuery =
    groupQueryOf(unhandledExtra().groupCriteriaByPackage())

/** Parses this [Cursor], formatted by [PeopleContractCompat.Directory], into directory records. */
internal fun Cursor.toDirectories(): List<DirectoryInfo> {
    val packageIndex = getColumnIndexOrThrow(Directory.PACKAGE_NAME)
    val authorityIndex = getColumnIndexOrThrow(Directory.DIRECTORY_AUTHORITY)
    return readRows { DirectoryInfo(getString(packageIndex), getString(authorityIndex)) }
}

/** Reads every row of this cursor with [readRow]. */
private inline fun <T> Cursor.readRows(readRow: Cursor.() -> T): List<T> = buildList {
    while (moveToNext()) add(readRow())
}

/** Reads the current row's selection columns. */
private fun Cursor.readSelection(): Selection =
    Selection(
        getString(getColumnIndexOrThrow(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE)),
        getString(getColumnIndexOrThrow(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)),
    )

/**
 * Returns cursor extras carrying [unhandledQueryArgs] under
 * [PeopleContractCompat.EXTRA_UNHANDLED_QUERIES].
 */
private fun unhandledExtrasOf(unhandledQueryArgs: Bundle): Bundle =
    Bundle().apply { putBundle(PeopleContractCompat.EXTRA_UNHANDLED_QUERIES, unhandledQueryArgs) }

/** Returns the unhandled criteria query arguments in this cursor's extras, or `null`. */
private fun Cursor.unhandledExtra(): Bundle? =
    extras?.getBundle(PeopleContractCompat.EXTRA_UNHANDLED_QUERIES)
