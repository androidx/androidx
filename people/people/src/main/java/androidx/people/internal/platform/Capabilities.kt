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

import androidx.people.Capability
import androidx.people.internal.platform.PeopleContractCompat.Intents

/**
 * Converts these capabilities into an AOSP reachability bitmask. Capabilities with no reachability
 * flag are ignored.
 */
internal fun Iterable<Capability>.toReachabilityBitmask(): Int =
    fold(0) { mask, capability ->
        mask or
            when (capability) {
                Capability.VOICE -> PeopleContractCompat.Capability.VOICE_REACHABILITY
                Capability.VIDEO -> PeopleContractCompat.Capability.VIDEO_REACHABILITY
                Capability.MESSAGE -> PeopleContractCompat.Capability.TEXT_REACHABILITY
                else -> 0
            }
    }

/**
 * Converts an AOSP reachability [bitmask] into a set of [Capability]. Unrecognized bits are
 * ignored.
 */
internal fun capabilitiesFromReachabilityBitmask(bitmask: Int): Set<Capability> = buildSet {
    if (bitmask and PeopleContractCompat.Capability.VOICE_REACHABILITY != 0) add(Capability.VOICE)
    if (bitmask and PeopleContractCompat.Capability.VIDEO_REACHABILITY != 0) add(Capability.VIDEO)
    if (bitmask and PeopleContractCompat.Capability.TEXT_REACHABILITY != 0) add(Capability.MESSAGE)
}

/**
 * Converts this capability to its AOSP interaction type string.
 *
 * @throws IllegalArgumentException if this capability has no interaction type.
 */
internal fun Capability.toInteractionType(): String =
    when (this) {
        Capability.VOICE -> Intents.PEOPLE_INTERACTION_TYPE_VOICE
        Capability.VIDEO -> Intents.PEOPLE_INTERACTION_TYPE_VIDEO
        Capability.MESSAGE -> Intents.PEOPLE_INTERACTION_TYPE_MESSAGE
        else -> throw IllegalArgumentException("Unsupported capability: $this")
    }

/** Converts an AOSP [interactionType] string into its [Capability], or `null` if unrecognized. */
internal fun capabilityFromInteractionType(interactionType: String): Capability? =
    when (interactionType) {
        Intents.PEOPLE_INTERACTION_TYPE_VOICE -> Capability.VOICE
        Intents.PEOPLE_INTERACTION_TYPE_VIDEO -> Capability.VIDEO
        Intents.PEOPLE_INTERACTION_TYPE_MESSAGE -> Capability.MESSAGE
        else -> null
    }
