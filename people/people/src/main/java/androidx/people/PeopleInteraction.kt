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
import androidx.annotation.RestrictTo
import androidx.core.content.IntentCompat
import androidx.people.internal.platform.PeopleContractCompat
import androidx.people.internal.platform.capabilityFromInteractionType
import androidx.people.internal.platform.toPersonQuery

/**
 * Represents an incoming interaction request delivered to a People Directory application's
 * Activity.
 *
 * When a user initiates a voice call, video call, or message with a contact or group through the
 * People Directory (e.g. from the Dialer or Contacts app), the system launches the directory
 * application's Activity matching [ACTION_START_INTERACTION].
 *
 * Use [parse] in `Activity.onCreate` or `Activity.onNewIntent` to extract the target [PersonId],
 * [GroupId], or criteria-based contact information (e.g. phone or email in [targetQuery]) and the
 * requested [Capability]:
 * ```kotlin
 * val interaction = PeopleInteraction.parse(intent) ?: return
 * val targetId = interaction.targetPersonId
 * val targetPhone = interaction.targetQuery.phones.firstOrNull()
 * when (interaction.capability) {
 *     Capability.VOICE -> startVoiceCall(targetId, targetPhone)
 *     Capability.VIDEO -> startVideoCall(targetId, targetPhone)
 *     Capability.MESSAGE -> openChat(targetId, targetPhone)
 *     else -> finish() // A capability added in a newer release.
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PeopleInteraction(
    /** The target query criteria identifying the person or group. */
    public val targetQuery: PersonQuery,
    /** The requested mode of communication. */
    public val capability: Capability,
) {
    /**
     * Constructs a [PeopleInteraction] targeting a known person identity.
     *
     * @param targetPersonId The target person identity to communicate with.
     * @param capability The requested mode of communication.
     */
    public constructor(
        targetPersonId: PersonId,
        capability: Capability,
    ) : this(
        targetQuery =
            personQuery(targetPersonId.directoryPackage) {
                addLookupKeys(targetPersonId.lookupKey)
            },
        capability = capability,
    )

    /**
     * Constructs a [PeopleInteraction] targeting a known group identity.
     *
     * @param targetGroupId The target group identity to communicate with.
     * @param capability The requested mode of communication.
     */
    public constructor(
        targetGroupId: GroupId,
        capability: Capability,
    ) : this(
        targetQuery =
            personQuery(targetGroupId.directoryPackage) {
                addGroupLookupKeys(targetGroupId.lookupKey)
            },
        capability = capability,
    )

    /**
     * The target person identity to communicate with, or `null` if this interaction targets a group
     * or was initiated via other query criteria such as a phone number or email address. In the
     * latter case, inspect [targetQuery] directly (for example, [PersonQuery.phones] or
     * [PersonQuery.emails]).
     */
    public val targetPersonId: PersonId?
        get() =
            targetQuery.lookupKeys.firstOrNull()?.let {
                PersonId(
                    lookupKey = it,
                    directoryPackage = targetQuery.directoryPackage,
                )
            }

    /**
     * The target group identity to communicate with, or `null` if this interaction targets an
     * individual person or was initiated without a group lookup key. Inspect [targetQuery] directly
     * for criteria-based queries.
     */
    public val targetGroupId: GroupId?
        get() =
            targetQuery.groupLookupKeys.firstOrNull()?.let {
                GroupId(
                    lookupKey = it,
                    directoryPackage = targetQuery.directoryPackage,
                )
            }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PeopleInteraction) return false
        return targetQuery == other.targetQuery && capability == other.capability
    }

    override fun hashCode(): Int {
        var result = targetQuery.hashCode()
        result = 31 * result + capability.hashCode()
        return result
    }

    override fun toString(): String =
        "PeopleInteraction(targetQuery=$targetQuery, capability=$capability)"

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
         *   interaction request.
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

            val personQuery = queryArgs.toPersonQuery() ?: return null
            return PeopleInteraction(targetQuery = personQuery, capability = capability)
        }
    }
}
