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
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.document.RemoteDocumentWriter
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.lang.reflect.Proxy
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteStateDomainTest {

    private object HomeDomain : RemoteValue.Domain("HOME")

    @Test
    fun builtInAndCustomDomains_toStringAndPrefixed() {
        val home = RemoteValue.Domain("HOME")

        assertThat(RemoteValue.Domain.User.toString()).isEqualTo("USER")
        assertThat(RemoteValue.Domain.System.toString()).isEqualTo("SYSTEM")
        assertThat(RemoteValue.Domain.None.toString()).isEqualTo("")
        assertThat(home.toString()).isEqualTo("HOME")
        assertThat(HomeDomain.toString()).isEqualTo("HOME")

        assertThat(RemoteValue.Domain.User.prefixed("shutter.is_open"))
            .isEqualTo("USER:shutter.is_open")
        assertThat(RemoteValue.Domain.System.prefixed("heart_rate_bpm"))
            .isEqualTo("SYSTEM:heart_rate_bpm")
        assertThat(RemoteValue.Domain.None.prefixed("raw_value")).isEqualTo("raw_value")
        assertThat(home.prefixed("sun.elevation")).isEqualTo("HOME:sun.elevation")
    }

    @Test
    fun domainEqualityAndHashCode() {
        val home = RemoteValue.Domain("HOME")
        val homeDuplicate = RemoteValue.Domain("HOME")

        assertThat(RemoteValue.Domain("USER")).isEqualTo(RemoteValue.Domain.User)
        assertThat(RemoteValue.Domain("USER").hashCode())
            .isEqualTo(RemoteValue.Domain.User.hashCode())
        assertThat(RemoteValue.Domain("SYSTEM")).isEqualTo(RemoteValue.Domain.System)
        assertThat(RemoteValue.Domain(null)).isEqualTo(RemoteValue.Domain.None)
        assertThat(RemoteValue.Domain(null).hashCode())
            .isEqualTo(RemoteValue.Domain.None.hashCode())
        assertThat(home).isEqualTo(homeDuplicate)
        assertThat(home).isEqualTo(HomeDomain)
        assertThat(home.hashCode()).isEqualTo(HomeDomain.hashCode())
        assertThat(home).isNotEqualTo(RemoteValue.Domain.User)
        assertThat(home).isNotEqualTo(RemoteValue.Domain.System)
        assertThat(home).isNotEqualTo(RemoteValue.Domain.None)
    }

    @Test
    fun invalidDomainNamespace_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { RemoteValue.Domain("") }
        assertFailsWith<IllegalArgumentException> { RemoteValue.Domain("HOME:SUB") }
        assertFailsWith<IllegalArgumentException> { RemoteValue.Domain(":HOME") }
    }

    @Test
    fun namedStatesAcrossDomains_writeDistinctQualifiedVariables() {
        val home = RemoteValue.Domain("HOME")
        val rawBuffer = RemoteComposeBuffer().apply { header(100, 100, 1f, 0L) }
        var nextId = 42
        val testWriter =
            Proxy.newProxyInstance(
                RemoteDocumentWriter::class.java.classLoader,
                arrayOf(RemoteDocumentWriter::class.java),
            ) { _, method, args ->
                when {
                    method.name == "allocateId" -> nextId++
                    method.name == "setNamedVariable" -> {
                        rawBuffer.setNamedVariable(
                            args[0] as Int,
                            args[1] as String,
                            args[2] as Int,
                        )
                        Unit
                    }
                    method.name.startsWith("addNamedFloat") -> {
                        val id = nextId++
                        rawBuffer.setNamedVariable(id, args[0] as String, NamedVariable.FLOAT_TYPE)
                        FloatConstant.apply(rawBuffer.buffer, id, args[1] as Float)
                        id
                    }
                    method.name.startsWith("addNamedInt") -> {
                        val id = nextId++
                        rawBuffer.setNamedVariable(id, args[0] as String, NamedVariable.INT_TYPE)
                        rawBuffer.addInteger(id, args[1] as Int)
                        id
                    }
                    method.name.startsWith("addIntConstant") -> {
                        val id = nextId++
                        rawBuffer.addInteger(id, args[0] as Int)
                        id
                    }
                    method.name.startsWith("addNamedString") -> {
                        val id = nextId++
                        rawBuffer.setNamedVariable(id, args[0] as String, NamedVariable.STRING_TYPE)
                        rawBuffer.addText(id, args[1] as String)
                        id
                    }
                    else -> throw UnsupportedOperationException(method.name)
                }
            } as RemoteDocumentWriter
        val creationState =
            RemoteComposeCreationState().apply {
                document = testWriter
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
                domain = RemoteValue.Domain.None,
            )
        val sunAboveHorizon =
            RemoteBoolean.createNamedRemoteBoolean("sun.above_horizon", true, domain = home)
        val heartRate =
            RemoteInt.createNamedRemoteInt(
                "heart_rate_bpm",
                72,
                domain = RemoteValue.Domain.System,
            )
        val roomLabel = RemoteString.createNamedRemoteString("room.label", "Living Room")

        val cardTempId = creationState.getId(cardTemperature)
        val homeTempId = creationState.getId(homeOutdoorTemperature)
        val homeTempAgainId = creationState.getId(homeOutdoorTemperatureAgain)
        val rawTempId = creationState.getId(rawTemperature)
        val sunUpId = creationState.getId(sunAboveHorizon)
        val heartRateId = creationState.getId(heartRate)
        val roomLabelId = creationState.getId(roomLabel)

        assertThat(homeTempAgainId).isEqualTo(homeTempId)
        assertThat(setOf(cardTempId, homeTempId, rawTempId, sunUpId, heartRateId)).hasSize(5)
        assertThat(roomLabelId).isGreaterThan(0)

        val bytes = rawBuffer.buffer.cloneBytes()
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
