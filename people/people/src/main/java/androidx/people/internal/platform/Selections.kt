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

import androidx.people.Criterion
import androidx.people.internal.platform.PeopleContractCompat.Groups
import androidx.people.internal.platform.PeopleContractCompat.Persons

/**
 * A `(selection type, value)` pair, as sent in a [PeopleContractCompat.QUERY_ARG_QUERIES] sub-query
 * and echoed back in the selection columns of a result cursor.
 */
internal data class Selection(val type: String, val value: String)

/**
 * Returns the [PeopleContractCompat.Persons] selection for this criterion.
 *
 * @throws IllegalArgumentException if the criterion type has no person selection type.
 */
internal fun Criterion.toPersonSelection(): Selection =
    when (this) {
        is Criterion.Phone -> Selection(Persons.Query.SELECTION_TYPE_PHONE, number)
        is Criterion.Email -> Selection(Persons.Query.SELECTION_TYPE_EMAIL, address)
        is Criterion.PersonLookupKey -> Selection(Persons.Query.SELECTION_TYPE_LOOKUP_KEY, key)
        is Criterion.GroupLookupKey -> Selection(Persons.Query.SELECTION_TYPE_GROUP_LOOKUP_KEY, key)
        else -> throw IllegalArgumentException("$this has no person selection type")
    }

/**
 * Returns the [Criterion] for this [PeopleContractCompat.Persons] selection, or `null` if its type
 * is not recognized.
 */
internal fun Selection.toPersonCriterion(): Criterion? =
    when (type) {
        Persons.Query.SELECTION_TYPE_PHONE -> Criterion.Phone(value)
        Persons.Query.SELECTION_TYPE_EMAIL -> Criterion.Email(value)
        Persons.Query.SELECTION_TYPE_LOOKUP_KEY -> Criterion.PersonLookupKey(value)
        Persons.Query.SELECTION_TYPE_GROUP_LOOKUP_KEY -> Criterion.GroupLookupKey(value)
        else -> null
    }

/**
 * Returns the [PeopleContractCompat.Groups] selection for this criterion.
 *
 * @throws IllegalArgumentException if the criterion type has no group selection type.
 */
internal fun Criterion.toGroupSelection(): Selection =
    when (this) {
        is Criterion.GroupLookupKey -> Selection(Groups.Query.SELECTION_TYPE_LOOKUP_KEY, key)
        else -> throw IllegalArgumentException("$this has no group selection type")
    }

/**
 * Returns the [Criterion] for this [PeopleContractCompat.Groups] selection, or `null` if its type
 * is not recognized.
 */
internal fun Selection.toGroupCriterion(): Criterion? =
    when (type) {
        Groups.Query.SELECTION_TYPE_LOOKUP_KEY -> Criterion.GroupLookupKey(value)
        else -> null
    }
