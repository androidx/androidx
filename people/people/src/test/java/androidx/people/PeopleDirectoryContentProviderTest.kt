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

package androidx.people

import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import androidx.people.PeopleDirectoryContentProvider.Companion.directoryGroupResult
import androidx.people.PeopleDirectoryContentProvider.Companion.directoryPersonResult
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupResult
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonResult
import androidx.people.internal.directoryGroupQuery
import androidx.people.internal.directoryPersonQuery
import androidx.people.internal.platform.PeopleContractCompat
import androidx.people.internal.platform.groupCriteriaByPackage
import androidx.people.internal.platform.personCriteriaByPackage
import androidx.people.internal.platform.toGroupQueryArgs
import androidx.people.internal.platform.toPersonQueryArgs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PeopleDirectoryContentProviderTest {

    private lateinit var directory: TestPeopleDirectory

    @Before
    fun setUp() {
        directory =
            TestPeopleDirectory().apply {
                personResult = defaultPersonResult()
                groupResult = defaultGroupResult()
            }
        directory.attachInfo(
            RuntimeEnvironment.getApplication(),
            ProviderInfo().apply { authority = "com.example.app.people" },
        )
    }

    @Test
    fun getType_forPersonsUri_returnsPersonsContentType() {
        val result = directory.getType(PERSONS_URI)

        assertThat(result).isEqualTo(PeopleContractCompat.Persons.CONTENT_TYPE)
    }

    @Test
    fun getType_forGroupsUri_returnsGroupsContentType() {
        val result = directory.getType(GROUPS_URI)

        assertThat(result).isEqualTo(PeopleContractCompat.Groups.CONTENT_TYPE)
    }

    @Test
    fun getType_unknownPath_returnsNull() {
        val uri = Uri.parse("content://com.example.app.people/other")

        val result = directory.getType(uri)

        assertThat(result).isNull()
    }

    @Test
    fun attachInfo_registersEverySemicolonSeparatedAuthority() {
        val multiAuthority =
            TestPeopleDirectory().apply {
                personResult = defaultPersonResult()
                groupResult = defaultGroupResult()
            }
        val providerInfo =
            ProviderInfo().apply { authority = "com.example.app.people;com.example.app.alt" }

        multiAuthority.attachInfo(RuntimeEnvironment.getApplication(), providerInfo)

        assertThat(multiAuthority.getType(Uri.parse("content://com.example.app.alt/persons")))
            .isEqualTo(PeopleContractCompat.Persons.CONTENT_TYPE)
    }

    @Test
    fun queryPersons_serializesEveryColumn() {
        val query = personQuery(packageName()) { addPhones(PHONE.number) }

        val cursor = directory.query(PERSONS_URI, null, queryArgsFor(query), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.Persons.LOOKUP_KEY)).isEqualTo("p1")
            assertThat(it.string(PeopleContractCompat.Directory.PACKAGE_NAME))
                .isEqualTo("com.example.app")
            assertThat(it.string(PeopleContractCompat.Persons.DISPLAY_NAME)).isEqualTo("Alice")
            assertThat(it.int(PeopleContractCompat.Persons.CAPABILITY))
                .isEqualTo(
                    PeopleContractCompat.Capability.VOICE_REACHABILITY or
                        PeopleContractCompat.Capability.VIDEO_REACHABILITY
                )
            assertThat(it.int(PeopleContractCompat.Persons.IS_KNOWN)).isEqualTo(1)
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE))
                .isEqualTo(PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE)
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE))
                .isEqualTo(PHONE.number)
        }
        assertThat(directory.lastPersonQuery?.criteria).isEqualTo(query.criteriaFor(packageName()))
    }

    @Test
    fun queryPersons_customProjection_onlyReturnsRequestedColumns() {
        val query = personQuery(packageName()) { addPhones(PHONE.number) }

        val cursor =
            directory.query(
                PERSONS_URI,
                arrayOf(PeopleContractCompat.Persons.DISPLAY_NAME),
                queryArgsFor(query),
                null,
            )

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.columnNames.toList())
                .containsExactly(PeopleContractCompat.Persons.DISPLAY_NAME)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.Persons.DISPLAY_NAME)).isEqualTo("Alice")
        }
    }

    @Test
    fun queryGroups_serializesEveryColumn() {
        val query = groupQuery(packageName()) { addLookupKeys("g1") }

        val cursor = directory.query(GROUPS_URI, null, queryArgsFor(query), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.Groups.LOOKUP_KEY)).isEqualTo("g1")
            assertThat(it.string(PeopleContractCompat.Directory.PACKAGE_NAME))
                .isEqualTo("com.example.app")
            assertThat(it.string(PeopleContractCompat.Groups.DISPLAY_NAME)).isEqualTo("Family")
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE))
                .isEqualTo(PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY)
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)).isEqualTo("g1")
        }
        assertThat(directory.lastGroupQuery?.criteria).isEqualTo(query.criteriaFor(packageName()))
    }

    @Test
    fun queryPersons_multipleCriteria_writesOneRowPerCriterion() {
        directory.personResult = directoryPersonResult { addMatch(ALICE, setOf(PHONE, EMAIL)) }
        val query =
            personQuery(packageName()) {
                addPhones(PHONE.number)
                addEmails(EMAIL.address)
            }

        val cursor = directory.query(PERSONS_URI, null, queryArgsFor(query), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(2)
            assertThat(it.selections())
                .containsExactly(
                    PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE to PHONE.number,
                    PeopleContractCompat.Persons.Query.SELECTION_TYPE_EMAIL to EMAIL.address,
                )
        }
    }

    @Test
    fun addMatch_withoutCriteria_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            directoryPersonResult { addMatch(ALICE, emptyList()) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            directoryGroupResult {
                addMatch(Group(GroupId("g1", "com.example.voip"), null), emptyList())
            }
        }
    }

    @Test
    fun queryPersons_matchWhoseCriteriaAreAllNotInQuery_writesNoRow() {
        directory.personResult = directoryPersonResult {
            addMatch(ALICE, Criterion.Phone("(555) 123-4567"))
            addMatch(BOB, PHONE)
        }
        val query = personQuery(packageName()) { addPhones(PHONE.number) }

        val cursor = directory.query(PERSONS_URI, null, queryArgsFor(query), null)

        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.Persons.LOOKUP_KEY)).isEqualTo("p2")
        }
    }

    @Test
    fun queryPersons_answersOnlyTheSubQueryForItsOwnPackage() {
        val query = personQuery {
            directory("com.other.app") { addEmails(EMAIL.address) }
            directory(packageName()) { addPhones(PHONE.number) }
        }

        directory.query(PERSONS_URI, null, queryArgsFor(query), null)?.close()

        assertThat(directory.lastPersonQuery?.criteria).containsExactly(PHONE)
    }

    @Test
    fun queryPersons_withoutASubQueryForItsOwnPackage_throws() {
        val query = personQuery("com.other.app") { addPhones(PHONE.number) }

        assertThrows(IllegalArgumentException::class.java) {
            directory.query(PERSONS_URI, null, queryArgsFor(query), null)
        }
        assertThat(directory.lastPersonQuery).isNull()
    }

    @Test
    fun queryPersons_withoutQueryArgs_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            directory.query(PERSONS_URI, null, null, null)
        }
    }

    @Test
    fun queryGroups_answersOnlyTheSubQueryForItsOwnPackage() {
        val query = groupQuery {
            directory("com.other.app") { addLookupKeys("other") }
            directory(packageName()) { addLookupKeys("g1") }
        }

        directory.query(GROUPS_URI, null, queryArgsFor(query), null)?.close()

        assertThat(directory.lastGroupQuery?.criteria)
            .containsExactly(Criterion.GroupLookupKey("g1"))
    }

    @Test
    fun queryGroups_withoutASubQueryForItsOwnPackage_throws() {
        val query = groupQuery("com.other.app") { addLookupKeys("g1") }

        assertThrows(IllegalArgumentException::class.java) {
            directory.query(GROUPS_URI, null, queryArgsFor(query), null)
        }
        assertThat(directory.lastGroupQuery).isNull()
    }

    @Test
    fun queryPersons_criterionNotInQuery_isDropped() {
        directory.personResult = directoryPersonResult {
            addMatch(ALICE, setOf(PHONE, Criterion.Phone("(555) 123-4567")))
        }
        val query = personQuery(packageName()) { addPhones(PHONE.number) }

        val cursor = directory.query(PERSONS_URI, null, queryArgsFor(query), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE))
                .isEqualTo(PHONE.number)
        }
    }

    @Test
    fun queryPersons_unhandledCriteria_areWrittenToCursorExtras() {
        directory.personResult = directoryPersonResult {
            addMatch(ALICE, PHONE)
            addUnhandled(EMAIL, LOOKUP_KEY)
        }
        val query =
            personQuery(packageName()) {
                addPhones(PHONE.number)
                addEmails(EMAIL.address)
                addLookupKeys(LOOKUP_KEY.key)
            }

        val cursor = directory.query(PERSONS_URI, null, queryArgsFor(query), null)

        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.unhandledCriteria())
                .containsExactly(packageName(), setOf(EMAIL, LOOKUP_KEY))
        }
    }

    @Test
    fun queryPersons_unhandledCriterionNotInQuery_isDropped() {
        directory.personResult = directoryPersonResult {
            addUnhandled(EMAIL, Criterion.Email("not-asked@example.com"))
        }
        val query =
            personQuery(packageName()) {
                addPhones(PHONE.number)
                addEmails(EMAIL.address)
            }

        val cursor = directory.query(PERSONS_URI, null, queryArgsFor(query), null)

        cursor!!.use {
            assertThat(it.unhandledCriteria()).containsExactly(packageName(), setOf(EMAIL))
        }
    }

    @Test
    fun queryPersons_onlyUnhandledCriteriaNotInQuery_writesNoExtras() {
        directory.personResult = directoryPersonResult {
            addUnhandled(Criterion.Email("not-asked@example.com"))
        }
        val query = personQuery(packageName()) { addPhones(PHONE.number) }

        val cursor = directory.query(PERSONS_URI, null, queryArgsFor(query), null)

        cursor!!.use {
            assertThat(it.count).isEqualTo(0)
            assertThat(it.unhandledCriteria()).isEmpty()
        }
    }

    @Test
    fun queryPersons_criterionNeitherMatchedNorUnhandled_isAbsentFromRowsAndExtras() {
        val other = Criterion.Phone("+15559999999")
        directory.personResult = directoryPersonResult {
            addMatch(ALICE, PHONE)
            addUnhandled(EMAIL)
        }
        val query =
            personQuery(packageName()) {
                addPhones(PHONE.number, other.number)
                addEmails(EMAIL.address)
            }

        val cursor = directory.query(PERSONS_URI, null, queryArgsFor(query), null)

        cursor!!.use {
            assertThat(it.selections().map { (_, value) -> value }).containsExactly(PHONE.number)
            assertThat(it.unhandledCriteria()).containsExactly(packageName(), setOf(EMAIL))
        }
    }

    @Test
    fun queryGroups_criterionWithoutGroupSelectionType_isDropped() {
        directory.groupResult = directoryGroupResult {
            addMatch(FAMILY, setOf(Criterion.GroupLookupKey("g1"), PHONE))
        }
        val query = groupQuery(packageName()) { addLookupKeys("g1") }

        val cursor = directory.query(GROUPS_URI, null, queryArgsFor(query), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)).isEqualTo("g1")
        }
    }

    @Test
    fun queryGroups_unhandledCriteria_areWrittenToCursorExtras() {
        val g2 = Criterion.GroupLookupKey("g2")
        directory.groupResult = directoryGroupResult {
            addMatch(FAMILY, Criterion.GroupLookupKey("g1"))
            addUnhandled(g2, Criterion.GroupLookupKey("not-asked"))
        }
        val query = groupQuery(packageName()) { addLookupKeys("g1", "g2") }

        val cursor = directory.query(GROUPS_URI, null, queryArgsFor(query), null)

        cursor!!.use {
            val extra = it.extras.getBundle(PeopleContractCompat.EXTRA_UNHANDLED_QUERIES)
            assertThat(extra).isNotNull()
            assertThat(extra.groupCriteriaByPackage()).containsExactly(packageName(), setOf(g2))
        }
    }

    @Test
    fun query_criterionWithoutDirectoryPackage_throws() {
        val queryArgs =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf(PHONE.number),
                            )
                        }
                    ),
                )
            }

        assertThrows(IllegalArgumentException::class.java) {
            directory.query(PERSONS_URI, null, queryArgs, null)
        }
        assertThat(directory.lastPersonQuery).isNull()
    }

    @Test
    fun query_unknownPath_returnsNull() {
        val uri = Uri.parse("content://com.example.app.people/other")

        val cursor = directory.query(uri, null, Bundle(), null)

        assertThat(cursor).isNull()
    }

    @Test
    fun query_topLevelSelectionArgsOnly_throws() {
        val queryArgs =
            Bundle().apply {
                putString(
                    PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                    PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                )
                putStringArray(
                    PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                    arrayOf(PHONE.number),
                )
                putString(PeopleContractCompat.QUERY_ARG_PACKAGE, "com.example.app")
            }

        assertThrows(IllegalArgumentException::class.java) {
            directory.query(PERSONS_URI, null, queryArgs, null)
        }
        assertThat(directory.lastPersonQuery).isNull()
    }

    @Test
    fun legacyQueryOverload_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            directory.query(
                PERSONS_URI,
                null,
                "display_name = ?",
                arrayOf("Alice"),
                "display_name ASC",
            )
        }
    }

    @Test
    fun notifyPersonsChanged_withoutAPeopleProvider_returnsFalse() = runTest {
        val context = RuntimeEnvironment.getApplication()

        val result =
            PeopleDirectoryContentProvider.notifyPersonsChanged(context) { addPhones(PHONE.number) }

        assertThat(result).isFalse()
    }

    @Test
    fun notifyPersonsChanged_withoutCriteria_throws() = runTest {
        val context = RuntimeEnvironment.getApplication()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { PeopleDirectoryContentProvider.notifyPersonsChanged(context) {} }
        }
    }

    @Test
    fun notifyGroupsChanged_withoutCriteria_throws() = runTest {
        val context = RuntimeEnvironment.getApplication()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { PeopleDirectoryContentProvider.notifyGroupsChanged(context) {} }
        }
    }

    @Test
    fun notifyGroupsChanged_withoutAPeopleProvider_returnsFalse() = runTest {
        val context = RuntimeEnvironment.getApplication()

        val result =
            PeopleDirectoryContentProvider.notifyGroupsChanged(context) { addLookupKeys("g1") }

        assertThat(result).isFalse()
    }

    @Test
    fun requestRefresh_withoutAPeopleProvider_returnsFalse() = runTest {
        val context = RuntimeEnvironment.getApplication()

        val result = PeopleDirectoryContentProvider.requestRefresh(context)

        assertThat(result).isFalse()
    }

    @Test
    fun insert_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            directory.insert(PERSONS_URI, null)
        }
    }

    @Test
    fun delete_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            directory.delete(PERSONS_URI, null, null)
        }
    }

    @Test
    fun update_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            directory.update(PERSONS_URI, null, null, null)
        }
    }

    @Test
    fun directoryPersonResult_empty_hasNoMatchesOrUnhandled() {
        val result = directoryPersonResult {}

        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledCriteria).isEmpty()
    }

    @Test
    fun directoryPersonResult_addMatchesOverloads_reportEveryMatch() {
        val result = directoryPersonResult {
            addMatches(PersonMatch(ALICE, setOf(PHONE)))
            addMatches(listOf(PersonMatch(BOB, setOf(EMAIL))))
        }

        assertThat(result.matches)
            .containsExactly(PersonMatch(ALICE, setOf(PHONE)), PersonMatch(BOB, setOf(EMAIL)))
    }

    @Test
    fun directoryPersonResult_addMatchWithEmptyCriteria_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            directoryPersonResult { addMatch(ALICE, emptySet()) }
        }
    }

    @Test
    fun directoryPersonResult_equalResults_areEqual() {
        val a = directoryPersonResult {
            addMatch(ALICE, PHONE)
            addUnhandled(EMAIL)
        }
        val b = directoryPersonResult {
            addUnhandled(EMAIL)
            addMatches(PersonMatch(ALICE, setOf(PHONE)))
        }

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a).isNotEqualTo(directoryPersonResult { addMatch(ALICE, PHONE) })
    }

    @Test
    fun directoryGroupResult_reportsMatchesAndUnhandledAsGiven() {
        val g1 = Criterion.GroupLookupKey("g1")
        val g2 = Criterion.GroupLookupKey("g2")
        val g3 = Criterion.GroupLookupKey("g3")
        val g4 = Criterion.GroupLookupKey("g4")
        val friends = Group(GroupId("g3", FAMILY.id.directoryPackage), "Friends")

        val result = directoryGroupResult {
            addMatch(FAMILY, g1, g2)
            addMatches(listOf(GroupMatch(friends, setOf(g3))))
            addUnhandled(listOf(g4))
        }

        assertThat(result.matches)
            .containsExactly(GroupMatch(FAMILY, setOf(g1, g2)), GroupMatch(friends, setOf(g3)))
        assertThat(result.unhandledCriteria).containsExactly(g4)
    }

    @Test
    fun directoryGroupResult_addMatchWithEmptyCriteria_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            directoryGroupResult { addMatch(FAMILY, emptySet()) }
        }
    }

    @Test
    fun directoryPersonQuery_equalityAndToString_areByCriteria() {
        val a = directoryPersonQuery { addPhones("+1") }
        val b = DirectoryPersonQuery(setOf(Criterion.Phone("+1")))

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a).isNotEqualTo(directoryPersonQuery { addPhones("+2") })
        assertThat(a.toString())
            .isEqualTo("DirectoryPersonQuery(criteria=[Criterion.Phone(number=+1)])")
    }

    @Test
    fun directoryGroupQuery_equalityAndToString_areByCriteria() {
        val a = directoryGroupQuery { addLookupKeys("g1") }
        val b = DirectoryGroupQuery(setOf(Criterion.GroupLookupKey("g1")))

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a.toString())
            .isEqualTo("DirectoryGroupQuery(criteria=[Criterion.GroupLookupKey(key=g1)])")
    }

    private fun person(lookupKey: String, name: String, directoryPackage: String = PKG) =
        Person(
            id = PersonId(lookupKey, directoryPackage),
            displayName = name,
            photoUri = null,
            capabilities = emptySet(),
            isKnown = true,
        )

    private fun defaultPersonResult(): DirectoryPersonResult = directoryPersonResult {
        addMatch(ALICE, PHONE)
    }

    private fun defaultGroupResult(): DirectoryGroupResult = directoryGroupResult {
        addMatch(FAMILY, Criterion.GroupLookupKey("g1"))
    }

    /** Serializes [query] the way the system People Provider forwards it to a directory. */
    private fun queryArgsFor(query: PersonQuery): Bundle =
        query.directoryQueries.toPersonQueryArgs()

    private fun queryArgsFor(query: GroupQuery): Bundle = query.directoryQueries.toGroupQueryArgs()

    private fun packageName(): String = RuntimeEnvironment.getApplication().packageName

    private fun Cursor.string(column: String): String? = getString(getColumnIndexOrThrow(column))

    private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

    private fun Cursor.selections(): List<Pair<String?, String?>> = buildList {
        moveToPosition(-1)
        while (moveToNext()) {
            add(
                string(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE) to
                    string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)
            )
        }
    }

    private fun Cursor.unhandledCriteria(): Map<String, Set<Criterion>> =
        extras.getBundle(PeopleContractCompat.EXTRA_UNHANDLED_QUERIES).personCriteriaByPackage()

    private companion object {
        val PERSONS_URI: Uri = Uri.parse("content://com.example.app.people/persons")
        val GROUPS_URI: Uri = Uri.parse("content://com.example.app.people/groups")

        const val PKG = "com.example.app"

        val PHONE = Criterion.Phone("+15551234567")
        val EMAIL = Criterion.Email("a@example.com")
        val LOOKUP_KEY = Criterion.PersonLookupKey("p1")

        val BOB =
            Person(
                id = PersonId(lookupKey = "p2", directoryPackage = "com.example.app"),
                displayName = "Bob",
                photoUri = null,
                capabilities = emptySet(),
                isKnown = false,
            )

        val ALICE =
            Person(
                id = PersonId(lookupKey = "p1", directoryPackage = "com.example.app"),
                displayName = "Alice",
                photoUri = null,
                capabilities = setOf(Capability.VOICE, Capability.VIDEO),
                isKnown = true,
            )

        val FAMILY =
            Group(
                id = GroupId(lookupKey = "g1", directoryPackage = "com.example.app"),
                displayName = "Family",
            )
    }

    /** A People Directory that returns the results it was given and records the last queries. */
    class TestPeopleDirectory : PeopleDirectoryContentProvider() {
        var personResult: DirectoryPersonResult = directoryPersonResult {}
        var groupResult: DirectoryGroupResult = directoryGroupResult {}
        var lastPersonQuery: DirectoryPersonQuery? = null
        var lastGroupQuery: DirectoryGroupQuery? = null

        override fun queryPersons(
            query: DirectoryPersonQuery,
            cancellationSignal: CancellationSignal?,
        ): DirectoryPersonResult {
            lastPersonQuery = query
            return personResult
        }

        override fun queryGroups(
            query: DirectoryGroupQuery,
            cancellationSignal: CancellationSignal?,
        ): DirectoryGroupResult {
            lastGroupQuery = query
            return groupResult
        }
    }
}
