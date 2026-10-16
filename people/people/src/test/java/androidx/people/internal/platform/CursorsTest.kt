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

import android.database.MatrixCursor
import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.Group
import androidx.people.GroupId
import androidx.people.Person
import androidx.people.PersonId
import androidx.people.personQuery
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class CursorsTest {

    @Test
    fun toPersons_rowsForSamePerson_mergeCriteria() {
        val cursor =
            personsCursor(
                personRow("p1", PKG, "Alice", PHONE, "+1"),
                personRow("p1", PKG, "Alice", EMAIL, "a@example.com"),
            )

        val result = cursor.toPersons()

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(PersonId("p1", PKG))
        assertThat(result[0].matchedCriteria)
            .containsExactly(Criterion.Phone("+1"), Criterion.Email("a@example.com"))
    }

    @Test
    fun toPersons_sameLookupKeyDifferentDirectories_staySeparate() {
        val cursor =
            personsCursor(
                personRow("p1", PKG, "Alice", PHONE, "+1"),
                personRow("p1", "com.other.app", "Alice", PHONE, "+1"),
            )

        val result = cursor.toPersons()

        assertThat(result.map { it.id })
            .containsExactly(PersonId("p1", PKG), PersonId("p1", "com.other.app"))
            .inOrder()
    }

    @Test
    fun toPersons_unknownSelectionType_dropsCriterionButKeepsPerson() {
        val cursor = personsCursor(personRow("p1", PKG, "Alice", "selection-type-future", "x"))

        val result = cursor.toPersons()

        assertThat(result).hasSize(1)
        assertThat(result[0].matchedCriteria).isEmpty()
    }

    @Test
    fun toPersons_nullSelection_yieldsEmptyCriteria() {
        val cursor = personsCursor(personRow("p1", PKG, "Alice", null, null))

        val result = cursor.toPersons()

        assertThat(result.single().matchedCriteria).isEmpty()
    }

    @Test
    fun toPersons_preservesFirstSeenOrder() {
        val cursor =
            personsCursor(
                personRow("p2", PKG, "Bob", PHONE, "+2"),
                personRow("p1", PKG, "Alice", PHONE, "+1"),
                personRow("p2", PKG, "Bob", EMAIL, "b@example.com"),
            )

        val result = cursor.toPersons()

        assertThat(result.map { it.id.lookupKey }).containsExactly("p2", "p1").inOrder()
        assertThat(result[0].matchedCriteria)
            .containsExactly(Criterion.Phone("+2"), Criterion.Email("b@example.com"))
    }

    @Test
    fun personsCursor_roundTrip_preservesPerson() {
        val person =
            Person(
                id = PersonId("p1", PKG),
                displayName = "Alice",
                photoUri = null,
                capabilities = setOf(Capability.MESSAGE),
                isKnown = true,
                matchedCriteria =
                    setOf(
                        Criterion.Phone("+1"),
                        Criterion.Email("a@example.com"),
                        Criterion.PersonLookupKey("p1"),
                        Criterion.GroupLookupKey("g1"),
                    ),
            )
        val query =
            personQuery(PKG) {
                addPhones("+1")
                addEmails("a@example.com")
                addLookupKeys("p1")
                addGroupLookupKeys("g1")
            }

        val result = listOf(person).toCursor(projection = null, query = query).toPersons()

        assertThat(result).containsExactly(person)
    }

    @Test
    fun toGroups_rowsForSameGroup_mergeCriteria() {
        val cursor =
            MatrixCursor(DEFAULT_GROUP_PROJECTION).apply {
                addRow(arrayOf("g1", "Family", GROUP_LOOKUP_KEY, "g1", PKG))
                addRow(arrayOf("g1", "Family", GROUP_LOOKUP_KEY, "G1", PKG))
                addRow(arrayOf("g1", "Family", "selection-type-future", "x", PKG))
            }

        val result = cursor.toGroups()

        assertThat(result)
            .containsExactly(
                Group(
                    id = GroupId("g1", PKG),
                    displayName = "Family",
                    matchedCriteria =
                        setOf(Criterion.GroupLookupKey("g1"), Criterion.GroupLookupKey("G1")),
                )
            )
    }

    private fun personsCursor(vararg rows: Array<Any?>) =
        MatrixCursor(DEFAULT_PERSON_PROJECTION).apply { rows.forEach { addRow(it) } }

    private fun personRow(
        lookupKey: String,
        pkg: String,
        name: String,
        selectionType: String?,
        selectionValue: String?,
    ): Array<Any?> = arrayOf(lookupKey, name, null, 0, 1, selectionType, selectionValue, pkg)

    private companion object {
        const val PKG = "com.example.app"
        const val PHONE = PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE
        const val EMAIL = PeopleContractCompat.Persons.Query.SELECTION_TYPE_EMAIL
        const val GROUP_LOOKUP_KEY = PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY
    }
}
