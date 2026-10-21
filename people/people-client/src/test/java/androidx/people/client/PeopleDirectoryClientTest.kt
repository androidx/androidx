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
import androidx.people.DirectoryInfo
import androidx.people.Group
import androidx.people.GroupId
import androidx.people.PeopleInteraction
import androidx.people.Person
import androidx.people.PersonId
import androidx.people.client.internal.FakePeopleBackend
import androidx.people.client.internal.PeopleDirectoryClientImpl
import androidx.people.groupQuery
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

    private val alice =
        Person(
            id = PersonId("user_1", "com.example.voip"),
            displayName = "Alice",
            photoUri = null,
            capabilities = emptySet(),
            isKnown = false,
            matchedCriteria = emptySet(),
        )
    private val family =
        Group(
            id = GroupId("g1", "com.example.chat"),
            displayName = "Family",
            matchedCriteria = emptySet(),
        )

    private fun clientFor(backend: FakePeopleBackend): PeopleDirectoryClient =
        PeopleDirectoryClientImpl {
            backend
        }

    @Test
    fun queryPersons_forwardsEveryQuery() = runTest {
        val backend = FakePeopleBackend(persons = listOf(alice))
        val client = clientFor(backend)
        val voip = personQuery("com.example.voip") { addPhones("+16505550100") }
        val chat = personQuery("com.example.chat") { addEmails("alice@example.com") }

        val result = client.queryPersons(voip, chat)

        assertThat(result).containsExactly(alice)
        assertThat(backend.personQueries).containsExactly(listOf(voip, chat))
    }

    @Test
    fun observePersons_forwardsEveryQuery() = runTest {
        val backend = FakePeopleBackend(persons = listOf(alice))
        val client = clientFor(backend)
        val query = personQuery("com.example.voip") { addPhones("+16505550100") }

        val result = client.observePersons(query).first()

        assertThat(result).containsExactly(alice)
        assertThat(backend.personQueries).containsExactly(listOf(query))
    }

    @Test
    fun observePersons_resolvesTheBackendOnlyWhenCollected() = runTest {
        var resolutions = 0
        val client = PeopleDirectoryClientImpl {
            resolutions++
            FakePeopleBackend(persons = listOf(alice))
        }
        val query = personQuery("com.example.voip") { addPhones("+16505550100") }

        val flow = client.observePersons(query)
        val before = resolutions
        flow.first()

        assertThat(before).isEqualTo(0)
        assertThat(resolutions).isEqualTo(1)
    }

    @Test
    fun queryGroups_forwardsEveryQuery() = runTest {
        val backend = FakePeopleBackend(groups = listOf(family))
        val client = clientFor(backend)
        val query = groupQuery("com.example.chat") { addLookupKeys("g1") }

        val result = client.queryGroups(query)

        assertThat(result).containsExactly(family)
        assertThat(backend.groupQueries).containsExactly(listOf(query))
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
    fun queryPersons_listOverload_forwardsQueries() = runTest {
        val backend = FakePeopleBackend(persons = listOf(alice))
        val client = clientFor(backend)
        val voip = personQuery("com.example.voip") { addPhones("+16505550100") }
        val chat = personQuery("com.example.chat") { addEmails("alice@example.com") }

        val result = client.queryPersons(listOf(voip, chat))

        assertThat(result).containsExactly(alice)
        assertThat(backend.personQueries).containsExactly(listOf(voip, chat))
    }

    @Test
    fun observePersons_listOverload_forwardsQueries() = runTest {
        val backend = FakePeopleBackend(persons = listOf(alice))
        val client = clientFor(backend)
        val query = personQuery("com.example.voip") { addPhones("+16505550100") }

        val result = client.observePersons(listOf(query)).first()

        assertThat(result).containsExactly(alice)
        assertThat(backend.personQueries).containsExactly(listOf(query))
    }

    @Test
    fun queryGroups_listOverload_forwardsQueries() = runTest {
        val backend = FakePeopleBackend(groups = listOf(family))
        val client = clientFor(backend)
        val query = groupQuery("com.example.chat") { addLookupKeys("g1") }

        val result = client.queryGroups(listOf(query))

        assertThat(result).containsExactly(family)
        assertThat(backend.groupQueries).containsExactly(listOf(query))
    }

    @Test
    fun observeGroups_listOverload_forwardsQueries() = runTest {
        val backend = FakePeopleBackend(groups = listOf(family))
        val client = clientFor(backend)
        val query = groupQuery("com.example.chat") { addLookupKeys("g1") }

        val result = client.observeGroups(listOf(query)).first()

        assertThat(result).containsExactly(family)
        assertThat(backend.groupQueries).containsExactly(listOf(query))
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
        val interaction = PeopleInteraction(alice.id, Capability.VIDEO)

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
    fun isAvailable_withoutAnyPeopleProvider_isFalse() = runTest {
        val result = PeopleDirectoryClient.isAvailable(RuntimeEnvironment.getApplication())

        assertThat(result).isFalse()
    }
}
