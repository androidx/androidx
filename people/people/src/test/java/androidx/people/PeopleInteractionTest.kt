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
import androidx.people.internal.platform.toPersonQueryArgs
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
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
            interactionIntent(
                PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE,
                peopleInteraction(personId, Capability.VOICE).toInteractionQueryArgs(),
            )

        val interaction = PeopleInteraction.parse(intent)

        assertThat(interaction).isNotNull()
        assertThat(interaction!!.targetPersonId).isEqualTo(personId)
        assertThat(interaction.targetGroupId).isNull()
        assertThat(interaction.directoryPackage).isEqualTo("com.example.voip")
        assertThat(interaction.targetCriteria)
            .containsExactly(Criterion.PersonLookupKey("alice_123"))
        assertThat(interaction.capability).isEqualTo(Capability.VOICE)
    }

    @Test
    fun parse_validGroupInteraction_returnsParsedInteraction() {
        val groupId = GroupId("group_xyz", "com.example.chat")
        val intent =
            interactionIntent(
                PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_MESSAGE,
                peopleInteraction(groupId, Capability.MESSAGE).toInteractionQueryArgs(),
            )

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
            interactionIntent(
                PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE,
                peopleInteraction(personId, Capability.VOICE).toInteractionQueryArgs(),
                action = "com.example.CUSTOM_ACTION",
            )

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
    fun parse_unknownInteractionType_returnsNull() {
        val intent =
            interactionIntent(
                "unknown",
                peopleInteraction(PersonId("p1", "com.example.voip"), Capability.VOICE)
                    .toInteractionQueryArgs(),
            )

        assertThat(PeopleInteraction.parse(intent)).isNull()
    }

    @Test
    fun parse_queryArgsWithoutCriteria_returnsNull() {
        val intent =
            interactionIntent(PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE, Bundle())

        assertThat(PeopleInteraction.parse(intent)).isNull()
    }

    @Test
    fun parse_criterionTarget_returnsParsedInteraction() {
        val expected =
            peopleInteraction("com.example.voip", Capability.VOICE) { addPhones("+15551234567") }
        val intent =
            interactionIntent(
                PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE,
                queryArgsFor(personQuery("com.example.voip") { addPhones("+15551234567") }),
            )

        val interaction = PeopleInteraction.parse(intent)

        assertThat(interaction).isEqualTo(expected)
        assertThat(interaction!!.targetPersonId).isNull()
        assertThat(interaction.targetGroupId).isNull()
    }

    @Test
    fun parse_roundTripsEveryTargetType() {
        val targets: List<PersonCriteriaScope.() -> Unit> =
            listOf(
                { addPhones("+15551234567") },
                { addEmails("alice@example.com") },
                { addLookupKeys("p1") },
                { addGroupLookupKeys("g1") },
            )
        for (target in targets) {
            val interaction = peopleInteraction("com.example.voip", Capability.VIDEO, target)
            val intent =
                interactionIntent(
                    PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VIDEO,
                    interaction.toInteractionQueryArgs(),
                )

            assertThat(PeopleInteraction.parse(intent)).isEqualTo(interaction)
        }
    }

    @Test
    fun parse_multipleCriteria_roundTripsEveryCriterion() {
        val interaction =
            peopleInteraction("com.example.voip", Capability.VOICE) {
                addLookupKeys("p1")
                addPhones("+15551234567")
                addEmails("alice@example.com")
                addGroupLookupKeys("g1")
            }
        val intent =
            interactionIntent(
                PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE,
                interaction.toInteractionQueryArgs(),
            )

        val parsed = PeopleInteraction.parse(intent)

        assertThat(parsed).isEqualTo(interaction)
        assertThat(parsed!!.targetPersonId).isEqualTo(PersonId("p1", "com.example.voip"))
        assertThat(parsed.targetGroupId).isEqualTo(GroupId("g1", "com.example.voip"))
    }

    @Test
    fun parse_multiplePackages_returnsNull() {
        val queryArgs =
            queryArgsFor(
                personQuery {
                    directory("com.example.voip") {
                        addPhones("+1")
                        addEmails("a@b")
                    }
                    directory("com.example.chat") { addPhones("+2") }
                }
            )
        val intent =
            interactionIntent(PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VOICE, queryArgs)

        val parsed = PeopleInteraction.parse(intent)

        assertThat(parsed).isNull()
    }

    @Test
    fun constructor_withPersonId_targetsItsLookupKey() {
        val personId = PersonId("alice_123", "com.example.voip")

        val interaction = peopleInteraction(personId, Capability.VOICE)

        assertThat(interaction.directoryPackage).isEqualTo("com.example.voip")
        assertThat(interaction.targetCriteria)
            .containsExactly(Criterion.PersonLookupKey("alice_123"))
        assertThat(interaction.targetPersonId).isEqualTo(personId)
        assertThat(interaction.targetGroupId).isNull()
    }

    @Test
    fun constructor_withGroupId_targetsItsLookupKey() {
        val groupId = GroupId("g1", "com.example.chat")

        val interaction = peopleInteraction(groupId, Capability.MESSAGE)

        assertThat(interaction.directoryPackage).isEqualTo("com.example.chat")
        assertThat(interaction.targetCriteria).containsExactly(Criterion.GroupLookupKey("g1"))
        assertThat(interaction.targetGroupId).isEqualTo(groupId)
        assertThat(interaction.targetPersonId).isNull()
    }

    @Test
    fun constructor_withCriterionTarget_hasNoTargetIds() {
        val interaction =
            peopleInteraction("com.example.voip", Capability.VOICE) { addPhones("+15551234567") }

        assertThat(interaction.targetPersonId).isNull()
        assertThat(interaction.targetGroupId).isNull()
    }

    @Test
    fun equals_comparesEveryProperty() {
        fun interaction(directoryPackage: String, capability: Capability) =
            peopleInteraction(directoryPackage, capability) { addPhones("+1") }
        val a = interaction("com.example.voip", Capability.VOICE)

        assertThat(a).isEqualTo(interaction("com.example.voip", Capability.VOICE))
        assertThat(a.hashCode())
            .isEqualTo(interaction("com.example.voip", Capability.VOICE).hashCode())
        assertThat(a).isNotEqualTo(interaction("com.example.voip", Capability.VIDEO))
        assertThat(a).isNotEqualTo(interaction("com.example.chat", Capability.VOICE))
        assertThat(a)
            .isNotEqualTo(
                peopleInteraction("com.example.voip", Capability.VOICE) { addPhones("+2") }
            )
    }

    /** Serializes [query] the way the system People Provider forwards it in an intent. */
    private fun queryArgsFor(query: PersonQuery): Bundle =
        query.directoryQueries.toPersonQueryArgs()

    private fun interactionIntent(
        interactionType: String,
        queryArgs: Bundle,
        action: String = PeopleInteraction.ACTION_START_INTERACTION,
    ): Intent =
        Intent(action).apply {
            putExtra(PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_TYPE, interactionType)
            putExtra(PeopleContractCompat.Intents.EXTRA_PEOPLE_QUERY_ARGS, queryArgs)
        }

    @Test
    fun peopleInteraction_withCriteriaBlock_collectsCriteriaInOrder() {
        val interaction =
            peopleInteraction("com.example.voip", Capability.VOICE) {
                addPhones("+16505550100")
                addLookupKeys("user_1")
            }

        assertThat(interaction.directoryPackage).isEqualTo("com.example.voip")
        assertThat(interaction.capability).isEqualTo(Capability.VOICE)
        assertThat(interaction.targetCriteria)
            .containsExactly(Criterion.Phone("+16505550100"), Criterion.PersonLookupKey("user_1"))
            .inOrder()
        assertThat(interaction.targetPersonId).isEqualTo(PersonId("user_1", "com.example.voip"))
    }

    @Test
    fun peopleInteraction_withCriteriaBlock_equalsIdOverload() {
        assertThat(peopleInteraction("com.example.voip", Capability.VIDEO) { addLookupKeys("u") })
            .isEqualTo(peopleInteraction(PersonId("u", "com.example.voip"), Capability.VIDEO))
    }

    @Test
    fun peopleInteraction_withEmptyBlock_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            peopleInteraction("com.example.voip", Capability.VOICE) {}
        }
    }

    @Test
    fun peopleInteraction_withEmptyPackage_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            peopleInteraction("", Capability.VOICE) { addPhones("+16505550100") }
        }
    }
}
