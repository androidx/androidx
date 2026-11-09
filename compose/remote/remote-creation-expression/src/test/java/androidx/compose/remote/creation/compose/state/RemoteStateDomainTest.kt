/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteStateDomainTest {

    private object HomeDomain : RemoteState.Domain("HOME")

    @Test
    fun builtInAndCustomDomains_toStringAndPrefixed() {
        val home = RemoteState.Domain("HOME")

        assertThat(RemoteState.Domain.User.toString()).isEqualTo("USER")
        assertThat(RemoteState.Domain.System.toString()).isEqualTo("SYSTEM")
        assertThat(RemoteState.Domain.None.toString()).isEqualTo("")
        assertThat(home.toString()).isEqualTo("HOME")
        assertThat(HomeDomain.toString()).isEqualTo("HOME")

        assertThat(RemoteState.Domain.User.prefixed("shutter.is_open"))
            .isEqualTo("USER:shutter.is_open")
        assertThat(RemoteState.Domain.System.prefixed("heart_rate_bpm"))
            .isEqualTo("SYSTEM:heart_rate_bpm")
        assertThat(RemoteState.Domain.None.prefixed("raw_value")).isEqualTo("raw_value")
        assertThat(home.prefixed("sun.elevation")).isEqualTo("HOME:sun.elevation")
    }

    @Test
    fun domainEqualityAndHashCode() {
        val home = RemoteState.Domain("HOME")
        val homeDuplicate = RemoteState.Domain("HOME")

        assertThat(RemoteState.Domain("USER")).isEqualTo(RemoteState.Domain.User)
        assertThat(RemoteState.Domain("USER").hashCode())
            .isEqualTo(RemoteState.Domain.User.hashCode())
        assertThat(RemoteState.Domain("SYSTEM")).isEqualTo(RemoteState.Domain.System)
        assertThat(RemoteState.Domain(null)).isEqualTo(RemoteState.Domain.None)
        assertThat(RemoteState.Domain(null).hashCode())
            .isEqualTo(RemoteState.Domain.None.hashCode())
        assertThat(home).isEqualTo(homeDuplicate)
        assertThat(home).isEqualTo(HomeDomain)
        assertThat(home.hashCode()).isEqualTo(HomeDomain.hashCode())
        assertThat(home).isNotEqualTo(RemoteState.Domain.User)
        assertThat(home).isNotEqualTo(RemoteState.Domain.System)
        assertThat(home).isNotEqualTo(RemoteState.Domain.None)
    }

    @Test
    fun invalidDomainNamespace_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { RemoteState.Domain("") }
        assertFailsWith<IllegalArgumentException> { RemoteState.Domain("HOME:SUB") }
        assertFailsWith<IllegalArgumentException> { RemoteState.Domain(":HOME") }
    }

    @Test
    fun namedStatesAcrossDomains_writeDistinctQualifiedVariables() {
        val home = RemoteState.Domain("HOME")
        val creationState =
            RemoteComposeCreationState().apply {
                document = RemoteComposeWriter(RcPlatformServices.None)
                remoteDensity = RemoteDensity(1f.rf, 1f.rf)
            }

        // Same local name ("temperature") in User vs Home vs None does not collide, while two
        // reads of the same (domain, name) deduplicate to one variable ID.
        val cardTemperature = RemoteFloat.createNamedRemoteFloat("temperature", 21.5f)
        val homeOutdoorTemperature =
            RemoteFloat.createNamedRemoteFloat("temperature", 14.0f, domain = home)
        val homeOutdoorTemperatureAgain =
            RemoteFloat.createNamedRemoteFloat("temperature", 14.0f, domain = home)
        val rawTemperature =
            RemoteFloat.createNamedRemoteFloat(
                "temperature",
                20.0f,
                domain = RemoteState.Domain.None,
            )
        val sunAboveHorizon =
            RemoteBoolean.createNamedRemoteBoolean("sun.above_horizon", true, domain = home)
        val heartRate =
            RemoteInt.createNamedRemoteInt(
                "heart_rate_bpm",
                72,
                domain = RemoteState.Domain.System,
            )
        val roomLabel = RemoteString.createNamedRemoteString("room.label", "Living Room")

        val cardTempId = cardTemperature.getIdForCreationState(creationState)
        val homeTempId = homeOutdoorTemperature.getIdForCreationState(creationState)
        val homeTempAgainId = homeOutdoorTemperatureAgain.getIdForCreationState(creationState)
        val rawTempId = rawTemperature.getIdForCreationState(creationState)
        val sunUpId = sunAboveHorizon.getIdForCreationState(creationState)
        val heartRateId = heartRate.getIdForCreationState(creationState)
        val roomLabelId = roomLabel.getIdForCreationState(creationState)

        assertThat(homeTempAgainId).isEqualTo(homeTempId)
        assertThat(setOf(cardTempId, homeTempId, rawTempId, sunUpId, heartRateId)).hasSize(5)
        assertThat(roomLabelId).isGreaterThan(0)

        val bytes = creationState.document.buffer.buffer.cloneBytes()
        val decoded =
            CoreDocument().apply {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes)))
            }
        val declaredNames = decoded.operations.filterIsInstance<NamedVariable>().map { it.mVarName }

        assertThat(declaredNames)
            .containsExactly(
                "USER:temperature",
                "HOME:temperature",
                "temperature",
                "HOME:sun.above_horizon",
                "SYSTEM:heart_rate_bpm",
                "USER:room.label",
            )

        // A host can discover shared HOME dependencies directly from the document's declared names.
        val homePrefix = home.prefixed("")
        val sharedHomeNames = declaredNames.filter { it.startsWith(homePrefix) }
        assertThat(sharedHomeNames).containsExactly("HOME:temperature", "HOME:sun.above_horizon")
    }
}
