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

@file:JvmName("PeopleInteractions")

package androidx.people

import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.content.IntentCompat
import androidx.people.internal.directoryPersonQuery
import androidx.people.internal.platform.PeopleContractCompat
import androidx.people.internal.platform.capabilityFromInteractionType
import androidx.people.internal.platform.personCriteriaByPackage

/**
 * A request to start a voice call, video call, or messaging session in one People Directory
 * application, with the recipient identified by [targetCriteria].
 *
 * Client applications build one with [peopleInteraction] and pass it to
 * `PeopleDirectoryClient.createInteractionRequest`. When the user starts the interaction, the
 * system launches the directory application's Activity matching [ACTION_START_INTERACTION], which
 * uses [parse] in `Activity.onCreate` or `Activity.onNewIntent` to read the request:
 * ```kotlin
 * val interaction = PeopleInteraction.parse(intent) ?: return
 * val recipient = interaction.targetPersonId ?: findRecipient(interaction.targetCriteria)
 * when (interaction.capability) {
 *     Capability.VOICE -> startVoiceCall(recipient)
 *     Capability.VIDEO -> startVideoCall(recipient)
 *     Capability.MESSAGE -> openChat(recipient)
 *     else -> finish() // A capability added in a newer release.
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PeopleInteraction
internal constructor(
    /** Package name of the People Directory application that handles the interaction. */
    public val directoryPackage: String,
    targetCriteria: Set<Criterion>,
    /** The requested mode of communication. */
    public val capability: Capability,
) {
    /**
     * The criteria identifying the recipient, for example a [Criterion.PersonLookupKey], a
     * [Criterion.GroupLookupKey], or phone numbers and email addresses. Never empty.
     */
    public val targetCriteria: Set<Criterion> = targetCriteria.toSet()

    init {
        require(directoryPackage.isNotEmpty()) { "A directory package must not be empty." }
        require(this.targetCriteria.isNotEmpty()) {
            "An interaction needs at least one target criterion."
        }
    }

    /**
     * The target person, from the first [Criterion.PersonLookupKey] in [targetCriteria], or `null`
     * if there is none. In that case inspect [targetCriteria] directly.
     */
    public val targetPersonId: PersonId?
        get() =
            targetCriteria
                .firstNotNullOfOrNull { it as? Criterion.PersonLookupKey }
                ?.let { PersonId(it.key, directoryPackage) }

    /**
     * The target group, from the first [Criterion.GroupLookupKey] in [targetCriteria], or `null` if
     * there is none.
     */
    public val targetGroupId: GroupId?
        get() =
            targetCriteria
                .firstNotNullOfOrNull { it as? Criterion.GroupLookupKey }
                ?.let { GroupId(it.key, directoryPackage) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PeopleInteraction) return false
        return directoryPackage == other.directoryPackage &&
            targetCriteria == other.targetCriteria &&
            capability == other.capability
    }

    override fun hashCode(): Int {
        var result = directoryPackage.hashCode()
        result = 31 * result + targetCriteria.hashCode()
        result = 31 * result + capability.hashCode()
        return result
    }

    override fun toString(): String =
        "PeopleInteraction(directoryPackage=$directoryPackage, targetCriteria=$targetCriteria, " +
            "capability=$capability)"

    public companion object {
        /**
         * Activity action initiating a communication session with a person or group in a People
         * Directory application.
         *
         * Declare an `<intent-filter>` with this action on the Activity that handles incoming
         * interaction requests:
         * ```xml
         * <activity android:name=".InteractionActivity" android:exported="true">
         *     <intent-filter>
         *         <action android:name="android.provider.action.START_PEOPLE_INTERACTION" />
         *         <category android:name="android.intent.category.DEFAULT" />
         *     </intent-filter>
         * </activity>
         * ```
         */
        @Suppress("ActionValue")
        public const val ACTION_START_INTERACTION: String =
            PeopleContractCompat.Intents.ACTION_START_PEOPLE_INTERACTION

        /**
         * Parses an incoming [Intent] delivered with [ACTION_START_INTERACTION].
         *
         * @param intent The intent delivered to the Activity.
         * @return The parsed [PeopleInteraction], or `null` if the intent is not a valid people
         *   interaction request, including when it does not address exactly one People Directory
         *   application.
         */
        @JvmStatic
        public fun parse(intent: Intent): PeopleInteraction? {
            if (intent.action != ACTION_START_INTERACTION) return null
            val interactionType =
                intent.getStringExtra(PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_TYPE)
                    ?: return null
            val capability = capabilityFromInteractionType(interactionType) ?: return null

            val queryArgs =
                IntentCompat.getParcelableExtra(
                    intent,
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_QUERY_ARGS,
                    Bundle::class.java,
                ) ?: return null

            // An interaction targets exactly one People Directory application.
            val (directoryPackage, criteria) =
                queryArgs.personCriteriaByPackage().entries.singleOrNull() ?: return null
            return PeopleInteraction(directoryPackage, criteria, capability)
        }
    }
}

/**
 * Builds a [PeopleInteraction] with the person [personId].
 *
 * @param personId The person to communicate with.
 * @param capability The requested mode of communication.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun peopleInteraction(personId: PersonId, capability: Capability): PeopleInteraction =
    PeopleInteraction(
        personId.directoryPackage,
        setOf(Criterion.PersonLookupKey(personId.lookupKey)),
        capability,
    )

/**
 * Builds a [PeopleInteraction] with the group [groupId].
 *
 * @param groupId The group to communicate with.
 * @param capability The requested mode of communication.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun peopleInteraction(groupId: GroupId, capability: Capability): PeopleInteraction =
    PeopleInteraction(
        groupId.directoryPackage,
        setOf(Criterion.GroupLookupKey(groupId.lookupKey)),
        capability,
    )

/**
 * Builds a [PeopleInteraction] with the recipient identified by the criteria in [block], in the
 * People Directory application [directoryPackage].
 *
 * ```
 * peopleInteraction("com.example.voip", Capability.VOICE) { addPhones("+16505550100") }
 * ```
 *
 * @param directoryPackage Package name of the People Directory application that handles the
 *   interaction.
 * @param capability The requested mode of communication.
 * @param block adds the criteria identifying the recipient.
 * @throws IllegalArgumentException if [directoryPackage] is empty or [block] adds no criterion.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun peopleInteraction(
    directoryPackage: String,
    capability: Capability,
    block: PersonCriteriaScope.() -> Unit,
): PeopleInteraction =
    PeopleInteraction(
        directoryPackage,
        directoryPersonQuery(block).criteria,
        capability,
    )
