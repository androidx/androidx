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

package androidx.people.client

import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.DirectoryInfo
import androidx.people.Group
import androidx.people.GroupId
import androidx.people.GroupMatch
import androidx.people.GroupQueryResult
import androidx.people.Person
import androidx.people.PersonId
import androidx.people.PersonMatch
import androidx.people.PersonQueryResult
import androidx.people.client.internal.FakePeopleBackend
import androidx.people.client.internal.PeopleDirectoryClientImpl
import androidx.people.groupQuery
import androidx.people.internal.groupQueryResultOf
import androidx.people.internal.personQueryResultOf
import androidx.people.peopleInteraction
import androidx.people.personQuery
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PeopleDirectoryClientTest {

    private fun clientFor(backend: FakePeopleBackend): PeopleDirectoryClient =
        PeopleDirectoryClientImpl {
            backend
        }

    @Test
    fun queryPersons_forwardsTheQueryAndReturnsTheBackendResult() = runTest {
        val backend = FakePeopleBackend(personResult = PERSON_RESULT)

        val result = clientFor(backend).queryPersons(PERSON_QUERY)

        assertThat(result).isSameInstanceAs(PERSON_RESULT)
        assertThat(backend.personQueries).containsExactly(PERSON_QUERY)
    }

    @Test
    fun observePersons_forwardsTheQueryAndEmitsTheBackendResult() = runTest {
        val backend = FakePeopleBackend(personResult = PERSON_RESULT)

        val result = clientFor(backend).observePersons(PERSON_QUERY).first()

        assertThat(result).isSameInstanceAs(PERSON_RESULT)
        assertThat(backend.personQueries).containsExactly(PERSON_QUERY)
    }

    @Test
    fun observePersons_resolvesTheBackendOnlyWhenCollected() = runTest {
        var resolutions = 0
        val client = PeopleDirectoryClientImpl {
            resolutions++
            FakePeopleBackend(personResult = PERSON_RESULT)
        }

        val flow = client.observePersons(PERSON_QUERY)
        val before = resolutions
        flow.first()

        assertThat(before).isEqualTo(0)
        assertThat(resolutions).isEqualTo(1)
    }

    @Test
    fun queryGroups_forwardsTheQueryAndReturnsTheBackendResult() = runTest {
        val backend = FakePeopleBackend(groupResult = GROUP_RESULT)

        val result = clientFor(backend).queryGroups(GROUP_QUERY)

        assertThat(result).isSameInstanceAs(GROUP_RESULT)
        assertThat(backend.groupQueries).containsExactly(GROUP_QUERY)
    }

    @Test
    fun observeGroups_forwardsTheQueryAndEmitsTheBackendResult() = runTest {
        val backend = FakePeopleBackend(groupResult = GROUP_RESULT)

        val result = clientFor(backend).observeGroups(GROUP_QUERY).first()

        assertThat(result).isSameInstanceAs(GROUP_RESULT)
        assertThat(backend.groupQueries).containsExactly(GROUP_QUERY)
    }

    @Test
    fun queryDirectories_forwardsToTheBackend() = runTest {
        val directory = DirectoryInfo("com.example.voip", "com.example.voip.people")
        val client = clientFor(FakePeopleBackend(directories = listOf(directory)))

        val result = client.queryDirectories()

        assertThat(result).containsExactly(directory)
    }

    @Test
    fun observeDirectories_forwardsToTheBackend() = runTest {
        val directory = DirectoryInfo("com.example.voip", "com.example.voip.people")
        val client = clientFor(FakePeopleBackend(directories = listOf(directory)))

        val result = client.observeDirectories().first()

        assertThat(result).containsExactly(directory)
    }

    @Test
    fun isAvailable_exposesBackendAvailability() = runTest {
        val available = clientFor(FakePeopleBackend(isAvailable = true))
        val unavailable = clientFor(FakePeopleBackend(isAvailable = false))

        assertThat(available.isAvailable()).isTrue()
        assertThat(unavailable.isAvailable()).isFalse()
    }

    @Test
    fun createInteractionRequest_forwardsTheInteraction() = runTest {
        val backend = FakePeopleBackend()
        val client = clientFor(backend)
        val interaction =
            peopleInteraction(PersonId("user_1", "com.example.voip"), Capability.VIDEO)

        client.createInteractionRequest(interaction)

        assertThat(backend.interactions).containsExactly(interaction)
    }

    @Test
    fun create_withoutAnyPeopleProvider_isNotAvailable() = runTest {
        // Robolectric runs below Android 37.2, and people-play-services is not on the classpath.
        val client = PeopleDirectoryClient.create(RuntimeEnvironment.getApplication())

        assertThat(client.isAvailable()).isFalse()
    }

    @Test
    fun create_withoutAnyPeopleProvider_reportsEverythingUnhandled() = runTest {
        val client = PeopleDirectoryClient.create(RuntimeEnvironment.getApplication())

        val persons = client.queryPersons(PERSON_QUERY)
        val groups = client.queryGroups(GROUP_QUERY)

        assertThat(persons.matches).isEmpty()
        assertThat(persons.matchedQuery.isEmpty()).isTrue()
        assertThat(persons.unmatchedQuery.isEmpty()).isTrue()
        assertThat(persons.unhandledQuery).isEqualTo(PERSON_QUERY)
        assertThat(groups.matches).isEmpty()
        assertThat(groups.unhandledQuery).isEqualTo(GROUP_QUERY)
    }

    @Test
    fun isAvailable_withoutAnyPeopleProvider_isFalse() = runTest {
        val result = PeopleDirectoryClient.isAvailable(RuntimeEnvironment.getApplication())

        assertThat(result).isFalse()
    }

    private companion object {
        val PERSON_QUERY = personQuery {
            directory("com.example.voip") { addPhones("+16505550100") }
            directory("com.example.chat") { addEmails("alice@example.com") }
        }
        val GROUP_QUERY = groupQuery("com.example.chat") { addLookupKeys("g1", "g2") }

        // The client is a pass-through, so the tests check that the backend's result is returned
        // as is. The results carry a match and an unhandled criterion, so that a client that
        // rebuilt or filtered them would be caught.
        val PERSON_RESULT: PersonQueryResult =
            personQueryResultOf(
                PERSON_QUERY,
                matches =
                    setOf(
                        PersonMatch(
                            Person(
                                id = PersonId("alice", "com.example.chat"),
                                displayName = "Alice",
                                photoUri = null,
                                capabilities = setOf(Capability.MESSAGE),
                                isKnown = true,
                            ),
                            setOf(Criterion.Email("alice@example.com")),
                        )
                    ),
                unhandledQuery = personQuery("com.example.voip") { addPhones("+16505550100") },
            )

        val GROUP_RESULT: GroupQueryResult =
            groupQueryResultOf(
                GROUP_QUERY,
                matches =
                    setOf(
                        GroupMatch(
                            Group(GroupId("g1", "com.example.chat"), "Family"),
                            setOf(Criterion.GroupLookupKey("g1")),
                        )
                    ),
                unhandledQuery = groupQuery("com.example.chat") { addLookupKeys("g2") },
            )
    }
}
