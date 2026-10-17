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
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CapabilitiesTest {

    @Test
    fun toInteractionType_convertsEachCapabilityToPlatformContractString() {
        assertThat(Capability.VOICE.toInteractionType())
            .isEqualTo(Intents.PEOPLE_INTERACTION_TYPE_VOICE)
        assertThat(Capability.VIDEO.toInteractionType())
            .isEqualTo(Intents.PEOPLE_INTERACTION_TYPE_VIDEO)
        assertThat(Capability.MESSAGE.toInteractionType())
            .isEqualTo(Intents.PEOPLE_INTERACTION_TYPE_MESSAGE)
    }

    @Test
    fun capabilityFromInteractionType_convertsPlatformContractStringsToCapabilities() {
        assertThat(capabilityFromInteractionType(Intents.PEOPLE_INTERACTION_TYPE_VOICE))
            .isEqualTo(Capability.VOICE)
        assertThat(capabilityFromInteractionType(Intents.PEOPLE_INTERACTION_TYPE_VIDEO))
            .isEqualTo(Capability.VIDEO)
        assertThat(capabilityFromInteractionType(Intents.PEOPLE_INTERACTION_TYPE_MESSAGE))
            .isEqualTo(Capability.MESSAGE)
    }

    @Test
    fun capabilityFromInteractionType_withUnknownString_returnsNull() {
        assertThat(capabilityFromInteractionType("UNKNOWN")).isNull()
    }

    @Test
    fun reachabilityBitmask_emptySet_roundTrips() {
        val capabilities = emptySet<Capability>()

        val result = capabilitiesFromReachabilityBitmask(capabilities.toReachabilityBitmask())

        assertThat(result).isEqualTo(capabilities)
    }

    @Test
    fun reachabilityBitmask_multipleCapabilities_roundTrips() {
        val capabilities = setOf(Capability.VOICE, Capability.VIDEO)

        val result = capabilitiesFromReachabilityBitmask(capabilities.toReachabilityBitmask())

        assertThat(result).isEqualTo(capabilities)
    }

    @Test
    fun reachabilityBitmask_allCapabilities_roundTrips() {
        val capabilities = setOf(Capability.VOICE, Capability.VIDEO, Capability.MESSAGE)

        val result = capabilitiesFromReachabilityBitmask(capabilities.toReachabilityBitmask())

        assertThat(result).isEqualTo(capabilities)
    }

    @Test
    fun capabilitiesFromReachabilityBitmask_ignoresUnknownBits() {
        val bitmask = PeopleContractCompat.Capability.VOICE_REACHABILITY or (1 shl 20)

        assertThat(capabilitiesFromReachabilityBitmask(bitmask)).containsExactly(Capability.VOICE)
    }
}
