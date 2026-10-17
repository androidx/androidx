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

import android.content.Intent
import android.os.Bundle
import androidx.people.internal.platform.PeopleContractCompat
import androidx.people.internal.platform.toInteractionQueryArgs
import androidx.people.internal.platform.toQueryArgs
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PeopleInteractionTest {

    @Test
    fun parse_validPersonInteraction_returnsParsedInteraction() {
        val personId = PersonId("alice_123", "com.example.voip")
        val intent =
            Intent(PeopleInteraction.ACTION_START_INTERACTION).apply {
                putExtra(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_TYPE,
                    PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE,
                )
                putExtra(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_QUERY_ARGS,
                    personQuery(personId.directoryPackage) { addLookupKeys(personId.lookupKey) }
                        .toInteractionQueryArgs(),
                )
            }

        val interaction = PeopleInteraction.parse(intent)

        assertThat(interaction).isNotNull()
        assertThat(interaction!!.targetPersonId).isEqualTo(personId)
        assertThat(interaction.targetGroupId).isNull()
        assertThat(interaction.capability).isEqualTo(Capability.VOICE)
    }

    @Test
    fun parse_validGroupInteraction_returnsParsedInteraction() {
        val groupId = GroupId("group_xyz", "com.example.chat")
        val intent =
            Intent(PeopleInteraction.ACTION_START_INTERACTION).apply {
                putExtra(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_TYPE,
                    PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_MESSAGE,
                )
                putExtra(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_QUERY_ARGS,
                    personQuery(groupId.directoryPackage) { addGroupLookupKeys(groupId.lookupKey) }
                        .toInteractionQueryArgs(),
                )
            }

        val interaction = PeopleInteraction.parse(intent)

        assertThat(interaction).isNotNull()
        assertThat(interaction!!.targetGroupId).isEqualTo(groupId)
        assertThat(interaction.targetPersonId).isNull()
        assertThat(interaction.capability).isEqualTo(Capability.MESSAGE)
    }

    @Test
    fun parse_wrongAction_returnsNull() {
        val personId = PersonId("alice_123", "com.example.voip")
        val intent =
            Intent("com.example.CUSTOM_ACTION").apply {
                putExtra(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_TYPE,
                    PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE,
                )
                putExtra(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_QUERY_ARGS,
                    personQuery(personId.directoryPackage) { addLookupKeys(personId.lookupKey) }
                        .toInteractionQueryArgs(),
                )
            }

        val interaction = PeopleInteraction.parse(intent)

        assertThat(interaction).isNull()
    }

    @Test
    fun parse_missingExtras_returnsNull() {
        val intent = Intent(PeopleInteraction.ACTION_START_INTERACTION)

        val interaction = PeopleInteraction.parse(intent)

        assertThat(interaction).isNull()
    }

    @Test
    fun parse_criteriaBasedPersonInteraction_returnsParsedInteractionWithQuery() {
        val query = personQuery("com.example.voip") { addPhones("+15551234567") }
        val queryArgs =
            Bundle().apply {
                putAll(query.toQueryArgs())
                putParcelable(
                    PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI,
                    PeopleContractCompat.Persons.CONTENT_URI,
                )
            }
        val intent =
            Intent(PeopleInteraction.ACTION_START_INTERACTION).apply {
                putExtra(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_TYPE,
                    PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE,
                )
                putExtra(PeopleContractCompat.Intents.EXTRA_PEOPLE_QUERY_ARGS, queryArgs)
            }

        val interaction = PeopleInteraction.parse(intent)

        assertThat(interaction).isNotNull()
        assertThat(interaction!!.targetPersonId).isNull()
        assertThat(interaction.targetGroupId).isNull()
        assertThat(interaction.targetQuery).isEqualTo(query)
        assertThat(interaction.capability).isEqualTo(Capability.VOICE)
    }

    @Test
    fun constructor_withPersonQueryContainingLookupKey_populatesTargetPersonId() {
        val query =
            personQuery("com.example.voip") {
                addLookupKeys("alice_123")
                addPhones("+15551234567")
            }

        val interaction = PeopleInteraction(query, Capability.VOICE)

        assertThat(interaction.targetPersonId).isEqualTo(PersonId("alice_123", "com.example.voip"))
        assertThat(interaction.targetQuery).isEqualTo(query)
    }

    @Test
    fun constructor_withPersonQueryWithoutLookupKey_leavesTargetPersonIdNull() {
        val query = personQuery("com.example.voip") { addPhones("+15551234567") }

        val interaction = PeopleInteraction(query, Capability.VOICE)

        assertThat(interaction.targetPersonId).isNull()
        assertThat(interaction.targetQuery).isEqualTo(query)
    }

    @Test
    fun constructor_withGroupId_populatesTargetGroupIdAndGeneratesQuery() {
        val groupId = GroupId("g1", "com.example.chat")

        val interaction = PeopleInteraction(groupId, Capability.MESSAGE)

        assertThat(interaction.targetGroupId).isEqualTo(groupId)
        assertThat(interaction.targetPersonId).isNull()
        assertThat(interaction.targetQuery.groupLookupKeys).containsExactly("g1")
        assertThat(interaction.targetQuery.directoryPackage).isEqualTo("com.example.chat")
    }
}
