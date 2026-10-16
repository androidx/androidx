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
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import androidx.people.internal.platform.PeopleContractCompat
import androidx.people.internal.platform.toQueryArgs
import com.google.common.truth.Truth.assertThat
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
        directory = TestPeopleDirectory()
        directory.attachInfo(
            RuntimeEnvironment.getApplication(),
            ProviderInfo().apply { authority = "com.example.app.people" },
        )
    }

    @Test
    fun getType_forPersonsUri_returnsPersonsContentType() {
        val uri = Uri.parse("content://com.example.app.people/persons")

        val result = directory.getType(uri)

        assertThat(result).isEqualTo(PeopleContractCompat.Persons.CONTENT_TYPE)
    }

    @Test
    fun getType_forGroupsUri_returnsGroupsContentType() {
        val uri = Uri.parse("content://com.example.app.people/groups")

        val result = directory.getType(uri)

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
        val multiAuthority = TestPeopleDirectory()
        val providerInfo =
            ProviderInfo().apply { authority = "com.example.app.people;com.example.app.alt" }

        multiAuthority.attachInfo(RuntimeEnvironment.getApplication(), providerInfo)

        assertThat(multiAuthority.getType(Uri.parse("content://com.example.app.alt/persons")))
            .isEqualTo(PeopleContractCompat.Persons.CONTENT_TYPE)
    }

    @Test
    fun queryPersons_serializesEveryColumn() {
        val uri = Uri.parse("content://com.example.app.people/persons")
        val query = personQuery("com.example.app") { addPhones("+15551234567") }

        val cursor = directory.query(uri, null, query.toQueryArgs(), null)

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
                .isEqualTo("+15551234567")
        }
        assertThat(directory.lastPersonQuery).isEqualTo(query)
    }

    @Test
    fun queryPersons_customProjection_onlyReturnsRequestedColumns() {
        val uri = Uri.parse("content://com.example.app.people/persons")
        val query = personQuery("com.example.app") { addPhones("+15551234567") }

        val cursor =
            directory.query(
                uri,
                arrayOf(PeopleContractCompat.Persons.DISPLAY_NAME),
                query.toQueryArgs(),
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
        val uri = Uri.parse("content://com.example.app.people/groups")
        val query = groupQuery("com.example.app") { addLookupKeys("g1") }

        val cursor = directory.query(uri, null, query.toQueryArgs(), null)

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
        assertThat(directory.lastGroupQuery).isEqualTo(query)
    }

    @Test
    fun queryPersons_multipleCriteria_writesOneRowPerCriterion() {
        directory.persons =
            listOf(alice(setOf(Criterion.Phone("+15551234567"), Criterion.Email("a@example.com"))))
        val query =
            personQuery("com.example.app") {
                addPhones("+15551234567")
                addEmails("a@example.com")
            }

        val cursor = directory.query(PERSONS_URI, null, query.toQueryArgs(), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(2)
            val rows = buildList {
                while (it.moveToNext()) {
                    add(
                        it.string(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE) to
                            it.string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)
                    )
                }
            }
            assertThat(rows)
                .containsExactly(
                    PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE to "+15551234567",
                    PeopleContractCompat.Persons.Query.SELECTION_TYPE_EMAIL to "a@example.com",
                )
        }
    }

    @Test
    fun queryPersons_noCriteria_writesSingleRowWithNullSelection() {
        directory.persons = listOf(alice(emptySet()))
        val query = personQuery("com.example.app") { addPhones("+15551234567") }

        val cursor = directory.query(PERSONS_URI, null, query.toQueryArgs(), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_TYPE)).isNull()
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)).isNull()
        }
    }

    @Test
    fun queryPersons_criterionNotInQuery_isDropped() {
        directory.persons =
            listOf(alice(setOf(Criterion.Phone("+15551234567"), Criterion.Phone("(555) 123-4567"))))
        val query = personQuery("com.example.app") { addPhones("+15551234567") }

        val cursor = directory.query(PERSONS_URI, null, query.toQueryArgs(), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE))
                .isEqualTo("+15551234567")
        }
    }

    @Test
    fun queryGroups_criterionWithoutGroupSelectionType_isDropped() {
        directory.groups =
            listOf(family(setOf(Criterion.GroupLookupKey("g1"), Criterion.Phone("+15551234567"))))
        val query = groupQuery("com.example.app") { addLookupKeys("g1") }

        val cursor = directory.query(GROUPS_URI, null, query.toQueryArgs(), null)

        assertThat(cursor).isNotNull()
        cursor!!.use {
            assertThat(it.count).isEqualTo(1)
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.string(PeopleContractCompat.QUERY_ARG_SELECTION_VALUE)).isEqualTo("g1")
        }
    }

    @Test
    fun query_criterionWithoutDirectoryPackage_isDropped() {
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
                                arrayOf("+15551234567"),
                            )
                        }
                    ),
                )
            }

        directory
            .query(Uri.parse("content://com.example.app.people/persons"), null, queryArgs, null)
            ?.close()

        assertThat(directory.lastPersonQuery).isNull()
    }

    @Test
    fun query_topLevelSelectionArgs_areNotTreatedAsACriterion() {
        val queryArgs =
            Bundle().apply {
                putString(
                    PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                    PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                )
                putStringArray(
                    PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                    arrayOf("+15551234567"),
                )
                putString(PeopleContractCompat.QUERY_ARG_PACKAGE, "com.example.app")
            }

        directory
            .query(Uri.parse("content://com.example.app.people/persons"), null, queryArgs, null)
            ?.close()

        assertThat(directory.lastPersonQuery).isNull()
    }

    @Test
    fun legacyQueryOverload_throwsUnsupportedOperationException() {
        val uri = Uri.parse("content://com.example.app.people/persons")

        assertThrows(UnsupportedOperationException::class.java) {
            directory.query(uri, null, "display_name = ?", arrayOf("Alice"), "display_name ASC")
        }
    }

    @Test
    fun notifyChange_forPersonQueryWithoutAPeopleProvider_returnsFalse() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val query = personQuery("com.example.app") { addPhones("+15551234567") }

        val result = PeopleDirectoryContentProvider.notifyChange(context, query)

        assertThat(result).isFalse()
    }

    @Test
    fun notifyChange_forGroupQueryWithoutAPeopleProvider_returnsFalse() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val query = groupQuery("com.example.app") { addLookupKeys("g1") }

        val result = PeopleDirectoryContentProvider.notifyChange(context, query)

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
        val uri = Uri.parse("content://com.example.app.people/persons")

        assertThrows(UnsupportedOperationException::class.java) {
            directory.insert(uri, null)
        }
    }

    @Test
    fun delete_throwsUnsupportedOperationException() {
        val uri = Uri.parse("content://com.example.app.people/persons")

        assertThrows(UnsupportedOperationException::class.java) {
            directory.delete(uri, null, null)
        }
    }

    @Test
    fun update_throwsUnsupportedOperationException() {
        val uri = Uri.parse("content://com.example.app.people/persons")

        assertThrows(UnsupportedOperationException::class.java) {
            directory.update(uri, null, null, null)
        }
    }

    private fun android.database.Cursor.string(column: String): String? =
        getString(getColumnIndexOrThrow(column))

    private fun android.database.Cursor.int(column: String): Int =
        getInt(getColumnIndexOrThrow(column))

    private companion object {
        val PERSONS_URI: Uri = Uri.parse("content://com.example.app.people/persons")
        val GROUPS_URI: Uri = Uri.parse("content://com.example.app.people/groups")

        fun alice(criteria: Set<Criterion>) =
            Person(
                id = PersonId(lookupKey = "p1", directoryPackage = "com.example.app"),
                displayName = "Alice",
                photoUri = null,
                capabilities = setOf(Capability.VOICE, Capability.VIDEO),
                isKnown = true,
                matchedCriteria = criteria,
            )

        fun family(criteria: Set<Criterion>) =
            Group(
                id = GroupId(lookupKey = "g1", directoryPackage = "com.example.app"),
                displayName = "Family",
                matchedCriteria = criteria,
            )
    }

    class TestPeopleDirectory : PeopleDirectoryContentProvider() {
        var lastPersonQuery: PersonQuery? = null
        var lastGroupQuery: GroupQuery? = null
        var persons: List<Person> = listOf(alice(setOf(Criterion.Phone("+15551234567"))))
        var groups: List<Group> = listOf(family(setOf(Criterion.GroupLookupKey("g1"))))

        override fun queryPersons(
            query: PersonQuery,
            cancellationSignal: CancellationSignal?,
        ): List<Person> {
            lastPersonQuery = query
            return persons
        }

        override fun queryGroups(
            query: GroupQuery,
            cancellationSignal: CancellationSignal?,
        ): List<Group> {
            lastGroupQuery = query
            return groups
        }
    }
}
