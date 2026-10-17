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
import androidx.core.os.BundleCompat
import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.PeopleInteraction
import androidx.people.PersonId
import androidx.people.groupQuery
import androidx.people.personQuery
import androidx.people.testing.FakePlatformPeopleProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PlatformPeopleBackendTest {

    private val context = RuntimeEnvironment.getApplication()
    private val backend = PlatformPeopleBackend(context)

    @Test
    fun isAvailable_isTrue() {
        assertThat(backend.isAvailable).isTrue()
    }

    private fun setupPlatformPeopleProvider(
        block: FakePlatformPeopleProvider.() -> Unit = {}
    ): FakePlatformPeopleProvider = FakePlatformPeopleProvider.install(context, block)

    @Test
    fun createIfSupported_withoutThePlatformProvider_returnsNull() = runTest {
        val result =
            PlatformPeopleBackend.createIfSupported(context, StandardTestDispatcher(testScheduler))

        assertThat(result).isNull()
    }

    @Test
    fun createIfSupported_belowAndroid37_2_returnsNullEvenWithThePlatformProviderInstalled() =
        runTest {
            setupPlatformPeopleProvider()

            val result =
                PlatformPeopleBackend.createIfSupported(
                    context,
                    StandardTestDispatcher(testScheduler),
                )

            assertThat(result).isNull()
        }

    @Test
    fun createInteractionRequest_withoutThePlatformProvider_returnsNull() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val interaction =
            PeopleInteraction(PersonId("user_123", "com.example.voip"), Capability.VOICE)

        val result = backend.createInteractionRequest(interaction)

        assertThat(result).isNull()
    }

    @Test
    fun createInteractionRequest_withMultipleCriteria_throwsIllegalArgumentException() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val target =
            personQuery("com.example.voip") {
                addPhones("+16505550100")
                addEmails("alice@example.com")
            }

        val result = runCatching {
            backend.createInteractionRequest(PeopleInteraction(target, Capability.VOICE))
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun notifyChange_forPersonQueryWithoutThePlatformProvider_returnsFalse() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = personQuery("com.example.voip") { addPhones("+16505550100") }

        val result = backend.notifyChange(query)

        assertThat(result).isFalse()
    }

    @Test
    fun notifyChange_forGroupQueryWithoutThePlatformProvider_returnsFalse() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = groupQuery("com.example.chat") { addLookupKeys("g1") }

        val result = backend.notifyChange(query)

        assertThat(result).isFalse()
    }

    @Test
    fun requestRefresh_withoutThePlatformProvider_returnsFalse() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))

        val result = backend.requestRefresh()

        assertThat(result).isFalse()
    }

    @Test
    fun notifyChange_forPersonQuery_sendsTheQueryAndItsDependencyUri() = runTest {
        val provider = setupPlatformPeopleProvider()
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = personQuery("com.example.voip") { addPhones("+16505550100") }

        val result = backend.notifyChange(query)

        assertThat(result).isTrue()
        val extras = provider.lastRefreshExtras!!
        assertThat(
                BundleCompat.getParcelable(
                    extras,
                    PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI,
                    Uri::class.java,
                )
            )
            .isEqualTo(PeopleContractCompat.Persons.CONTENT_URI)
        assertThat(extras.toPersonQueries()).containsExactly(query)
    }

    @Test
    fun requestRefresh_sendsNoQuery() = runTest {
        val provider = setupPlatformPeopleProvider()
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))

        val result = backend.requestRefresh()

        assertThat(result).isTrue()
        assertThat(provider.lastRefreshExtras).isNull()
    }

    @Test
    fun queryPersons_withoutThePlatformProvider_returnsEmpty() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = personQuery("com.example.voip") { addPhones("+1650") }

        val result = backend.queryPersons(listOf(query))

        assertThat(result).isEmpty()
    }

    @Test
    fun queryPersons_samePersonMatchedByTwoInputs_mergesIntoOnePerson() = runTest {
        setupPlatformPeopleProvider {
            addPerson(
                id = PersonId("lookup_1", "com.example.voip"),
                displayName = "Alice",
                isKnown = true,
                matchedCriteria =
                    setOf(
                        Criterion.Phone("+16505550100"),
                        Criterion.Email("alice@example.com"),
                    ),
            )
        }
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query =
            personQuery("com.example.voip") {
                addPhones("+16505550100")
                addEmails("alice@example.com")
            }

        val result = backend.queryPersons(listOf(query))

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(PersonId("lookup_1", "com.example.voip"))
        assertThat(result[0].matchedCriteria)
            .containsExactly(
                Criterion.Phone("+16505550100"),
                Criterion.Email("alice@example.com"),
            )
    }

    @Test
    fun parsePersonsCursor_readsEveryColumn() {
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
                "com.example.voip",
                "Alice",
                "https://example.com/alice.jpg",
                PeopleContractCompat.Capability.VOICE_REACHABILITY or
                    PeopleContractCompat.Capability.VIDEO_REACHABILITY,
                1,
                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                "+16505550100",
            )
        )
        cursor.addRow(
            arrayOf<Any?>(
                "lookup_2",
                "com.example.chat",
                "Bob",
                null,
                PeopleContractCompat.Capability.TEXT_REACHABILITY,
                0,
                PeopleContractCompat.Persons.Query.SELECTION_TYPE_EMAIL,
                "bob@example.com",
            )
        )

        val result = cursor.toPersons()

        assertThat(result).hasSize(2)
        val alice = result[0]
        assertThat(alice.id.lookupKey).isEqualTo("lookup_1")
        assertThat(alice.id.directoryPackage).isEqualTo("com.example.voip")
        assertThat(alice.displayName).isEqualTo("Alice")
        assertThat(alice.photoUri).isEqualTo(Uri.parse("https://example.com/alice.jpg"))
        assertThat(alice.capabilities).containsExactly(Capability.VOICE, Capability.VIDEO)
        assertThat(alice.isKnown).isTrue()
        assertThat(alice.matchedCriteria).containsExactly(Criterion.Phone("+16505550100"))

        val bob = result[1]
        assertThat(bob.photoUri).isNull()
        assertThat(bob.capabilities).containsExactly(Capability.MESSAGE)
        assertThat(bob.isKnown).isFalse()
        assertThat(bob.matchedCriteria).containsExactly(Criterion.Email("bob@example.com"))
    }

    @Test
    fun parsePersonsCursor_skipsRowsWithoutALookupKey() {
        val cursor =
            personsCursor(
                mapOf(PeopleContractCompat.Directory.PACKAGE_NAME to "com.example.voip"),
                mapOf(
                    PeopleContractCompat.Persons.LOOKUP_KEY to "",
                    PeopleContractCompat.Directory.PACKAGE_NAME to "com.example.voip",
                ),
                mapOf(
                    PeopleContractCompat.Persons.LOOKUP_KEY to "lookup_1",
                    PeopleContractCompat.Directory.PACKAGE_NAME to "com.example.voip",
                ),
            )

        val result = cursor.toPersons()

        assertThat(result).hasSize(1)
        assertThat(result[0].id.lookupKey).isEqualTo("lookup_1")
        assertThat(result[0].capabilities).isEmpty()
        assertThat(result[0].matchedCriteria).isEmpty()
    }

    @Test
    fun parsePersonsCursor_dropsAnUnrecognizedSelectionTypeButKeepsThePerson() {
        val cursor =
            personsCursor(
                mapOf(
                    PeopleContractCompat.Persons.LOOKUP_KEY to "lookup_1",
                    PeopleContractCompat.QUERY_ARG_SELECTION_TYPE to "selection-type-future",
                    PeopleContractCompat.QUERY_ARG_SELECTION_VALUE to "value",
                )
            )

        val result = cursor.toPersons()

        assertThat(result).hasSize(1)
        assertThat(result[0].matchedCriteria).isEmpty()
    }

    @Test
    fun parsePersonsCursor_ignoresUnknownCapabilityBits() {
        val cursor =
            personsCursor(
                mapOf(
                    PeopleContractCompat.Persons.LOOKUP_KEY to "lookup_1",
                    PeopleContractCompat.Persons.CAPABILITY to
                        (PeopleContractCompat.Capability.VOICE_REACHABILITY or (1 shl 20)),
                )
            )

        val result = cursor.toPersons()

        assertThat(result[0].capabilities).containsExactly(Capability.VOICE)
    }

    @Test
    fun parsePersonsCursor_missingAContractColumn_throws() {
        val cursor = MatrixCursor(arrayOf(PeopleContractCompat.Persons.LOOKUP_KEY))

        assertThrows(IllegalArgumentException::class.java) {
            cursor.toPersons()
        }
    }

    @Test
    fun parseGroupsCursor_readsEveryColumn() {
        val cursor =
            MatrixCursor(
                arrayOf(
                    PeopleContractCompat.Groups.LOOKUP_KEY,
                    PeopleContractCompat.Directory.PACKAGE_NAME,
                    PeopleContractCompat.Groups.DISPLAY_NAME,
                    PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                    PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                )
            )
        cursor.addRow(
            arrayOf<Any?>(
                "grp_1",
                "com.example.chat",
                "Project Alpha",
                PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY,
                "grp_1",
            )
        )

        val result = cursor.toGroups()

        assertThat(result).hasSize(1)
        assertThat(result[0].id.lookupKey).isEqualTo("grp_1")
        assertThat(result[0].id.directoryPackage).isEqualTo("com.example.chat")
        assertThat(result[0].displayName).isEqualTo("Project Alpha")
        assertThat(result[0].matchedCriteria).containsExactly(Criterion.GroupLookupKey("grp_1"))
    }

    @Test
    fun parseDirectoriesCursor_skipsIncompleteRows() {
        val cursor =
            MatrixCursor(
                arrayOf(
                    PeopleContractCompat.Directory.PACKAGE_NAME,
                    PeopleContractCompat.Directory.DIRECTORY_AUTHORITY,
                )
            )
        cursor.addRow(arrayOf<Any?>("com.example.voip", "com.example.voip.people"))
        cursor.addRow(arrayOf<Any?>("com.example.chat", "com.example.chat.people"))
        cursor.addRow(arrayOf<Any?>("com.example.broken", null))

        val result = cursor.toDirectories()

        assertThat(result.map { it.packageName })
            .containsExactly("com.example.voip", "com.example.chat")
            .inOrder()
    }

    private fun personsCursor(vararg rows: Map<String, Any?>): MatrixCursor {
        val columns = DEFAULT_PERSON_PROJECTION
        return MatrixCursor(columns).apply {
            rows.forEach { row -> addRow(columns.map { row[it] }) }
        }
    }
}
