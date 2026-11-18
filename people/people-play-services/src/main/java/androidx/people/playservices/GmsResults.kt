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
import androidx.people.DirectoryInfo
import androidx.people.GroupId
import androidx.people.GroupMatch
import androidx.people.GroupQuery
import androidx.people.PersonId
import androidx.people.PersonMatch
import androidx.people.PersonQuery
import androidx.people.group
import androidx.people.groupQuery
import androidx.people.internal.failedGroupQueryResultOf
import androidx.people.internal.failedPersonQueryResultOf
import androidx.people.person
import androidx.people.personQuery
import com.google.android.gms.people.peopleprovider.PeopleContractCompat
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Groups
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Persons
import com.google.android.gms.people.peopleprovider.PeopleProviderDirectory
import com.google.android.gms.people.peopleprovider.PeopleProviderGroup
import com.google.android.gms.people.peopleprovider.PeopleProviderPerson

// Converts Google Play services People Provider results into androidx.people models.

internal fun PeopleProviderDirectory.toDirectoryInfo(): DirectoryInfo =
    DirectoryInfo(packageName = packageName, authority = authority)

/**
 * Returns the matches in these records.
 *
 * Google Play services returns one record per matched selection, as the platform People Provider
 * returns one cursor row per matched selection, so the records of a person are combined into one
 * [PersonMatch]. The person's other fields come from its first record.
 */
internal fun List<PeopleProviderPerson>.toPersonMatches(): Set<PersonMatch> = groupBy {
    PersonId(directoryPackage = it.packageName, lookupKey = it.lookupKey)
}
    .mapTo(mutableSetOf()) { (id, records) ->
        val first = records.first()
        val person =
            person(id) {
                displayName = first.displayName
                photoUri = first.photoUri
                capabilities = capabilitiesOf(first.capabilities)
                isKnown = first.isKnown
            }
        PersonMatch(person, records.mapTo(mutableSetOf()) { it.matchedCriterion() })
    }

/** Returns the matches in these records, combined the same way as [toPersonMatches]. */
internal fun List<PeopleProviderGroup>.toGroupMatches(): Set<GroupMatch> = groupBy {
    GroupId(directoryPackage = it.packageName, lookupKey = it.lookupKey)
}
    .mapTo(mutableSetOf()) { (id, records) ->
        val group = group(id) { displayName = records.first().displayName }
        GroupMatch(group, records.mapTo(mutableSetOf()) { it.matchedCriterion() })
    }

/**
 * Returns the part of this query addressed to [directoryPackages], for example the People Directory
 * applications that Google Play services could not reach. Packages that this query does not address
 * are ignored.
 */
internal fun PersonQuery.forDirectories(directoryPackages: Collection<String>): PersonQuery {
    val criteriaByPackage =
        directoryPackages.associateWith { criteriaFor(it) }.filterValues { it.isNotEmpty() }
    // personQuery needs at least one directory, so an empty query comes from a result that has no
    // matches.
    if (criteriaByPackage.isEmpty()) return failedPersonQueryResultOf(this).matchedQuery
    return personQuery {
        for ((directoryPackage, criteria) in criteriaByPackage) {
            directory(directoryPackage) {
                for (criterion in criteria) {
                    when (criterion) {
                        is Criterion.Phone -> addPhones(criterion.number)
                        is Criterion.Email -> addEmails(criterion.address)
                        is Criterion.PersonLookupKey -> addLookupKeys(criterion.key)
                        is Criterion.GroupLookupKey -> addGroupLookupKeys(criterion.key)
                        else -> throw IllegalStateException("Unknown criterion $criterion")
                    }
                }
            }
        }
    }
}

/**
 * Returns the part of this query addressed to [directoryPackages], as the [PersonQuery] overload.
 */
internal fun GroupQuery.forDirectories(directoryPackages: Collection<String>): GroupQuery {
    val criteriaByPackage =
        directoryPackages.associateWith { criteriaFor(it) }.filterValues { it.isNotEmpty() }
    if (criteriaByPackage.isEmpty()) return failedGroupQueryResultOf(this).matchedQuery
    return groupQuery {
        for ((directoryPackage, criteria) in criteriaByPackage) {
            directory(directoryPackage) {
                for (criterion in criteria) {
                    when (criterion) {
                        is Criterion.GroupLookupKey -> addLookupKeys(criterion.key)
                        else -> throw IllegalStateException("Unknown criterion $criterion")
                    }
                }
            }
        }
    }
}

/** Returns the capabilities in a People Provider reachability bitmask. */
private fun capabilitiesOf(bitmask: Int): Set<Capability> = buildSet {
    if (bitmask and PeopleContractCompat.Capability.VOICE_REACHABILITY != 0) add(Capability.VOICE)
    if (bitmask and PeopleContractCompat.Capability.VIDEO_REACHABILITY != 0) add(Capability.VIDEO)
    if (bitmask and PeopleContractCompat.Capability.TEXT_REACHABILITY != 0) add(Capability.MESSAGE)
}

/** Returns the criterion this record matched. */
private fun PeopleProviderPerson.matchedCriterion(): Criterion {
    val value = checkNotNull(matchedSelectionValue)
    return when (matchedSelectionType) {
        Persons.Query.SELECTION_TYPE_PHONE -> Criterion.Phone(value)
        Persons.Query.SELECTION_TYPE_EMAIL -> Criterion.Email(value)
        Persons.Query.SELECTION_TYPE_LOOKUP_KEY -> Criterion.PersonLookupKey(value)
        Persons.Query.SELECTION_TYPE_GROUP_LOOKUP_KEY -> Criterion.GroupLookupKey(value)
        else -> throw IllegalStateException("Unknown selection type $matchedSelectionType")
    }
}

/** Returns the criterion this record matched. */
private fun PeopleProviderGroup.matchedCriterion(): Criterion =
    when (matchedSelectionType) {
        Groups.Query.SELECTION_TYPE_LOOKUP_KEY ->
            Criterion.GroupLookupKey(checkNotNull(matchedSelectionValue))
        else -> throw IllegalStateException("Unknown selection type $matchedSelectionType")
    }
