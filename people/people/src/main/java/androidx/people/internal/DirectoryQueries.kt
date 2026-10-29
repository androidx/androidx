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

package androidx.people.internal

import androidx.people.Criterion
import androidx.people.GroupCriteriaScope
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.PersonCriteriaScope

/**
 * Adds [values] to these criteria.
 *
 * @throws IllegalArgumentException if [values] is empty, naming the missing [kind] of value.
 */
internal fun MutableSet<Criterion>.addNonEmpty(values: List<Criterion>, kind: String) {
    require(values.isNotEmpty()) { "At least one $kind is required." }
    addAll(values)
}

/**
 * Builds a [DirectoryPersonQuery] from the criteria added in [block].
 *
 * @throws IllegalArgumentException if [block] adds no criterion.
 */
internal fun directoryPersonQuery(block: PersonCriteriaScope.() -> Unit): DirectoryPersonQuery =
    PersonCriteriaScope().apply(block).buildDirectoryQuery()

/**
 * Builds a [DirectoryGroupQuery] from the criteria added in [block].
 *
 * @throws IllegalArgumentException if [block] adds no criterion.
 */
internal fun directoryGroupQuery(block: GroupCriteriaScope.() -> Unit): DirectoryGroupQuery =
    GroupCriteriaScope().apply(block).buildDirectoryQuery()
