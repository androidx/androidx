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
import android.net.Uri
import android.os.Bundle
import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.Group
import androidx.people.GroupId
import androidx.people.GroupMatch
import androidx.people.PeopleDirectoryContentProvider.Companion.directoryGroupResult
import androidx.people.PeopleDirectoryContentProvider.Companion.directoryPersonResult
import androidx.people.Person
import androidx.people.PersonId
import androidx.people.PersonMatch
import androidx.people.PersonQuery
import androidx.people.groupQuery
import androidx.people.personQuery
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class CursorsTest {

    private val phone1 = Criterion.Phone("+1")
    private val phone2 = Criterion.Phone("+2")
    private val email = Criterion.Email("a@example.com")

    @Test
    fun toPersonMatches_rowsForSamePerson_mergeCriteria() {
        val cursor =
            personsCursor(
                personRow("p1", PKG, "Alice", PHONE, "+1"),
                personRow("p1", PKG, "Alice", EMAIL, "a@example.com"),
            )

        val match = cursor.toPersonMatches().single()
        assertThat(match.person.id).isEqualTo(PersonId("p1", PKG))
        assertThat(match.criteria).containsExactly(phone1, email).inOrder()
    }

    @Test
    fun toPersonMatches_sameLookupKeyDifferentDirectories_staySeparate() {
        val cursor =
            personsCursor(
                personRow("p1", PKG, "Alice", PHONE, "+1"),
                personRow("p1", OTHER_PKG, "Alice", PHONE, "+1"),
            )

        val matches = cursor.toPersonMatches()

        assertThat(matches.map { it.person.id })
            .containsExactly(PersonId("p1", PKG), PersonId("p1", OTHER_PKG))
    }

    @Test
    fun toPersonMatches_preservesFirstSeenOrder() {
        val cursor =
            personsCursor(
                personRow("p2", PKG, "Bob", PHONE, "+2"),
                personRow("p1", PKG, "Alice", PHONE, "+1"),
                personRow("p2", PKG, "Bob", EMAIL, "b@example.com"),
            )

        val matches = cursor.toPersonMatches().toList()

        assertThat(matches.map { it.person.id.lookupKey }).containsExactly("p2", "p1").inOrder()
        assertThat(matches[0].criteria)
            .containsExactly(phone2, Criterion.Email("b@example.com"))
            .inOrder()
    }

    @Test
    fun unhandledPersonQuery_readsTheCursorExtras() {
        val unhandled = personQuery {
            directory(PKG) { addEmails("a@example.com") }
            directory(OTHER_PKG) { addPhones("+1") }
        }
        val cursor =
            personsCursor(personRow("p1", PKG, "Alice", PHONE, "+1")).apply {
                extras = unhandledExtras(unhandled)
            }

        assertThat(cursor.unhandledPersonQuery()).isEqualTo(unhandled)
    }

    @Test
    fun unhandledPersonQuery_withoutExtras_isEmpty() {
        assertThat(personsCursor().unhandledPersonQuery().isEmpty()).isTrue()
    }

    @Test
    fun toPersonMatches_emptyCursor_returnsNoMatches() {
        assertThat(personsCursor().toPersonMatches()).isEmpty()
    }

    @Test
    fun personResult_cursorRoundTrip_preservesMatchesAndUnhandled() {
        val alice = person()
        val bob = person(lookupKey = "p2", name = "Bob")
        val criteria =
            setOf(phone1, email, Criterion.PersonLookupKey("p1"), Criterion.GroupLookupKey("g1"))
        val original = directoryPersonResult {
            addMatch(alice, criteria)
            addMatch(bob, phone2)
            addUnhandled(Criterion.Phone("+3"))
        }
        val query =
            personQuery(PKG) {
                addPhones("+1", "+2", "+3")
                addEmails("a@example.com")
                addLookupKeys("p1")
                addGroupLookupKeys("g1")
            }

        val cursor = original.toCursor(PKG, projection = null, query.criteriaFor(PKG))

        assertThat(cursor.toPersonMatches())
            .containsExactly(PersonMatch(alice, criteria), PersonMatch(bob, setOf(phone2)))
        assertThat(cursor.unhandledPersonQuery()).isEqualTo(personQuery(PKG) { addPhones("+3") })
    }

    @Test
    fun personResult_toCursor_writesAskedMatchesAndUnhandled() {
        val original = directoryPersonResult {
            addMatch(person(), phone1)
            addUnhandled(email)
        }

        val cursor = original.toCursor(PKG, projection = null, askedCriteria = setOf(phone1, email))

        assertThat(cursor.count).isEqualTo(1)
        assertThat(
                cursor.extras
                    .getBundle(PeopleContractCompat.EXTRA_UNHANDLED_QUERIES)
                    .personCriteriaByPackage()
            )
            .containsExactly(PKG, setOf(email))
    }

    @Test
    fun personResult_toCursor_withoutUnhandled_writesNoUnhandledExtra() {
        val cursor = directoryPersonResult {
            addMatch(person(), phone1)
        }
            .toCursor(PKG, projection = null, askedCriteria = setOf(phone1))

        assertThat(cursor.extras.containsKey(PeopleContractCompat.EXTRA_UNHANDLED_QUERIES))
            .isFalse()
    }

    @Test
    fun toGroupMatches_rowsForSameGroup_mergeCriteria() {
        val cursor =
            MatrixCursor(DEFAULT_GROUP_PROJECTION).apply {
                addRow(arrayOf("g1", "Family", GROUP_LOOKUP_KEY, "g1", PKG))
                addRow(arrayOf("g1", "Family", GROUP_LOOKUP_KEY, "G1", PKG))
            }

        assertThat(cursor.toGroupMatches())
            .containsExactly(
                GroupMatch(
                    Group(id = GroupId("g1", PKG), displayName = "Family"),
                    setOf(Criterion.GroupLookupKey("g1"), Criterion.GroupLookupKey("G1")),
                )
            )
    }

    @Test
    fun groupResult_cursorRoundTrip_preservesMatchesAndUnhandled() {
        val g1 = Criterion.GroupLookupKey("g1")
        val g2 = Criterion.GroupLookupKey("g2")
        val family = Group(id = GroupId("g1", PKG), displayName = "Family")
        val original = directoryGroupResult {
            addMatch(family, g1)
            addUnhandled(g2)
        }
        val query = groupQuery(PKG) { addLookupKeys("g1", "g2") }

        val cursor = original.toCursor(PKG, projection = null, query.criteriaFor(PKG))

        assertThat(cursor.toGroupMatches()).containsExactly(GroupMatch(family, setOf(g1)))
        assertThat(cursor.unhandledGroupQuery()).isEqualTo(groupQuery(PKG) { addLookupKeys("g2") })
    }

    @Test
    fun toPersonMatches_readsEveryColumn() {
        val cursor =
            MatrixCursor(
                arrayOf(
                    PeopleContractCompat.Persons.LOOKUP_KEY,
                    PeopleContractCompat.Directory.PACKAGE_NAME,
                    PeopleContractCompat.Persons.DISPLAY_NAME,
                    PeopleContractCompat.Persons.PHOTO_URI,
                    PeopleContractCompat.Persons.CAPABILITY,
                    PeopleContractCompat.Persons.IS_KNOWN,
                    PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                    PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                )
            )
        cursor.addRow(
            arrayOf<Any?>(
                "lookup_1",
                PKG,
                "Alice",
                "https://example.com/alice.jpg",
                PeopleContractCompat.Capability.VOICE_REACHABILITY or
                    PeopleContractCompat.Capability.VIDEO_REACHABILITY,
                1,
                PHONE,
                "+1",
            )
        )
        cursor.addRow(
            arrayOf<Any?>(
                "lookup_2",
                OTHER_PKG,
                "Bob",
                null,
                PeopleContractCompat.Capability.TEXT_REACHABILITY,
                0,
                EMAIL,
                "bob@example.com",
            )
        )

        assertThat(cursor.toPersonMatches())
            .containsExactly(
                PersonMatch(
                    Person(
                        id = PersonId("lookup_1", PKG),
                        displayName = "Alice",
                        photoUri = Uri.parse("https://example.com/alice.jpg"),
                        capabilities = setOf(Capability.VOICE, Capability.VIDEO),
                        isKnown = true,
                    ),
                    setOf(phone1),
                ),
                PersonMatch(
                    Person(
                        id = PersonId("lookup_2", OTHER_PKG),
                        displayName = "Bob",
                        photoUri = null,
                        capabilities = setOf(Capability.MESSAGE),
                        isKnown = false,
                    ),
                    setOf(Criterion.Email("bob@example.com")),
                ),
            )
    }

    @Test
    fun toPersonMatches_missingAContractColumn_throws() {
        val cursor = MatrixCursor(arrayOf(PeopleContractCompat.Persons.LOOKUP_KEY))

        assertThrows(IllegalArgumentException::class.java) { cursor.toPersonMatches() }
    }

    @Test
    fun toDirectories_readsEveryRow() {
        val cursor =
            MatrixCursor(
                arrayOf(
                    PeopleContractCompat.Directory.PACKAGE_NAME,
                    PeopleContractCompat.Directory.DIRECTORY_AUTHORITY,
                )
            )
        cursor.addRow(arrayOf<Any?>("com.example.voip", "com.example.voip.people"))
        cursor.addRow(arrayOf<Any?>("com.example.chat", "com.example.chat.people"))

        val result = cursor.toDirectories()

        assertThat(result.map { it.packageName })
            .containsExactly("com.example.voip", "com.example.chat")
            .inOrder()
    }

    private fun person(lookupKey: String = "p1", name: String = "Alice") =
        Person(
            id = PersonId(lookupKey, PKG),
            displayName = name,
            photoUri = null,
            capabilities = setOf(Capability.MESSAGE),
            isKnown = true,
        )

    /** Cursor extras reporting [unhandled] as unhandled, built from the public query DSL. */
    private fun unhandledExtras(unhandled: PersonQuery) =
        Bundle().apply {
            putBundle(
                PeopleContractCompat.EXTRA_UNHANDLED_QUERIES,
                unhandled.directoryQueries.toPersonQueryArgs(),
            )
        }

    private fun personsCursor(vararg rows: Array<Any?>) =
        MatrixCursor(DEFAULT_PERSON_PROJECTION).apply { rows.forEach { addRow(it) } }

    private fun personRow(
        lookupKey: String?,
        pkg: String,
        name: String,
        selectionType: String?,
        selectionValue: String?,
    ): Array<Any?> = arrayOf(lookupKey, name, null, 0, 1, selectionType, selectionValue, pkg)

    private companion object {
        const val PKG = "com.example.app"
        const val OTHER_PKG = "com.other.app"
        const val PHONE = PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE
        const val EMAIL = PeopleContractCompat.Persons.Query.SELECTION_TYPE_EMAIL
        const val GROUP_LOOKUP_KEY = PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY
    }
}
