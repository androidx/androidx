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

import androidx.people.Capability
import androidx.people.GroupId
import androidx.people.PersonId
import androidx.people.groupQuery
import androidx.people.peopleInteraction
import androidx.people.personQuery
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UnavailablePeopleBackendTest {

    private val backend = UnavailablePeopleBackend

    @Test
    fun isAvailable_isFalse() {
        assertThat(backend.isAvailable).isFalse()
    }

    @Test
    fun queryDirectories_returnsEmptyList() = runTest {
        val result = backend.queryDirectories()

        assertThat(result).isEmpty()
    }

    @Test
    fun observeDirectories_emitsEmptyList() = runTest {
        val result = backend.observeDirectories().first()

        assertThat(result).isEmpty()
    }

    @Test
    fun queryPersons_reportsEverythingUnhandled() = runTest {
        val query = personQuery("com.example.voip") { addPhones("+16505550100") }

        val result = backend.queryPersons(query)

        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledQuery).isEqualTo(query)
    }

    @Test
    fun observePersons_reportsEverythingUnhandled() = runTest {
        val query = personQuery("com.example.voip") { addPhones("+16505550100") }

        val result = backend.observePersons(query).first()

        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledQuery).isEqualTo(query)
    }

    @Test
    fun queryGroups_reportsEverythingUnhandled() = runTest {
        val query = groupQuery("com.example.chat") { addLookupKeys("g1") }

        val result = backend.queryGroups(query)

        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledQuery).isEqualTo(query)
    }

    @Test
    fun observeGroups_reportsEverythingUnhandled() = runTest {
        val query = groupQuery("com.example.chat") { addLookupKeys("g1") }

        val result = backend.observeGroups(query).first()

        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledQuery).isEqualTo(query)
    }

    @Test
    fun createInteractionRequest_withPersonId_returnsNull() = runTest {
        val interaction =
            peopleInteraction(PersonId("user_1", "com.example.voip"), Capability.VOICE)

        val result = backend.createInteractionRequest(interaction)

        assertThat(result).isNull()
    }

    @Test
    fun createInteractionRequest_withGroupId_returnsNull() = runTest {
        val interaction = peopleInteraction(GroupId("g1", "com.example.chat"), Capability.MESSAGE)

        val result = backend.createInteractionRequest(interaction)

        assertThat(result).isNull()
    }

    @Test
    fun createInteractionRequest_withMultipleCriteria_returnsNull() = runTest {
        val interaction =
            peopleInteraction("com.example.voip", Capability.VOICE) {
                addPhones("+16505550100")
                addEmails("alice@example.com")
            }

        val result = backend.createInteractionRequest(interaction)

        assertThat(result).isNull()
    }

    @Test
    fun notifyChange_personQuery_returnsFalse() = runTest {
        val query = directoryPersonQuery { addPhones("+16505550100") }

        val result = backend.notifyChange(query)

        assertThat(result).isFalse()
    }

    @Test
    fun notifyChange_groupQuery_returnsFalse() = runTest {
        val query = directoryGroupQuery { addLookupKeys("g1") }

        val result = backend.notifyChange(query)

        assertThat(result).isFalse()
    }

    @Test
    fun requestRefresh_returnsFalse() = runTest {
        val result = backend.requestRefresh()

        assertThat(result).isFalse()
    }
}
