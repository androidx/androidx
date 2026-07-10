/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.SystemInfo
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.utilities.IntMap
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteComposeWriterTest {
    private lateinit var rcPlatform: RcPlatformServices
    private lateinit var writer: RemoteComposeWriter
    private lateinit var profile: Profile

    val creationDisplayInfo = CreationDisplayInfo(450, 450, (2f * 160).toInt())

    @Before
    fun setUp() {
        rcPlatform = RcPlatformServices.None
        profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, rcPlatform) {
                creationDisplayInfo,
                profile,
                _ ->
                RemoteComposeWriter(
                    profile,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        writer = profile.create(creationDisplayInfo, "test")
    }

    @Test
    fun createTextFromFloat_deduplicates_calls() {
        val initialBufferSize = writer.bufferSize()
        val id1 = writer.createTextFromFloat(1.0f, 2, 2, 0)
        val sizeAfterFirstCall = writer.bufferSize()

        // Second call with same arguments, this should not write anything to the buffer.
        val id2 = writer.createTextFromFloat(1.0f, 2, 2, 0)
        val sizeAfterSecondCall = writer.bufferSize()

        assertThat(id1).isEqualTo(id2)
        assertThat(sizeAfterFirstCall).isEqualTo(sizeAfterSecondCall)
    }

    @Test
    fun testConstructorWithTagsSetsFields() {
        assertThat(writer.mPlatform).isEqualTo(profile.platform)
        assertThat(writer.mApiLevel).isEqualTo(profile.apiLevel)
    }

    @Test
    fun testConstructorWithoutTagsSetsFields() {
        writer = RemoteComposeWriter(creationDisplayInfo, null, profile)

        assertThat(writer.mPlatform).isEqualTo(profile.platform)
        assertThat(writer.mApiLevel).isEqualTo(profile.apiLevel)
    }

    private fun parseDocument(writer: RemoteComposeWriter): CoreDocument {
        val bytes = writer.encodeToByteArray()
        val readBuffer = RemoteComposeBuffer()
        readBuffer.buffer.setSystemInfo(SystemInfo(writer.apiLevel, CoreDocument.PROFILE))
        readBuffer.buffer.reset(bytes.size)
        for (b in bytes) {
            readBuffer.buffer.writeByte(b.toInt())
        }
        readBuffer.buffer.setIndex(0)
        val document = CoreDocument()
        document.initFromBuffer(readBuffer)
        return document
    }

    private fun getHeaderProperties(doc: CoreDocument): IntMap<Any>? {
        val headerField = CoreDocument::class.java.getDeclaredField("mHeader")
        headerField.isAccessible = true
        val header = headerField.get(doc) as Header? ?: return null

        val propertiesField = Header::class.java.getDeclaredField("mProperties")
        propertiesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return propertiesField.get(header) as IntMap<Any>?
    }

    private fun createProfile(apiLevel: Int): Profile {
        return Profile(apiLevel, RcProfiles.PROFILE_ANDROIDX, rcPlatform) {
            creationDisplayInfo,
            profile,
            _ ->
            RemoteComposeWriter(
                profile,
                RemoteComposeBuffer(),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
            )
        }
    }

    @Test
    fun testDensityBehavior_v8_supportsAnyBehavior() {
        val p = createProfile(8)

        // Legacy
        val infoLegacy = CreationDisplayInfo(100, 100, 160, CoreDocument.DENSITY_BEHAVIOR_LEGACY)
        val wLegacy = RemoteComposeWriter(infoLegacy, null, p)
        val docLegacy = parseDocument(wLegacy)
        assertThat(docLegacy.densityBehavior).isEqualTo(CoreDocument.DENSITY_BEHAVIOR_LEGACY)
        val propsLegacy = getHeaderProperties(docLegacy)
        assertThat(propsLegacy).isNotNull()
        assertThat(propsLegacy!!.get(Header.DOC_DENSITY_BEHAVIOR.toInt()))
            .isEqualTo(CoreDocument.DENSITY_BEHAVIOR_LEGACY)

        // Dp
        val infoDp = CreationDisplayInfo(100, 100, 160, CoreDocument.DENSITY_BEHAVIOR_DP)
        val wDp = RemoteComposeWriter(infoDp, null, p)
        val docDp = parseDocument(wDp)
        assertThat(docDp.densityBehavior).isEqualTo(CoreDocument.DENSITY_BEHAVIOR_DP)
        val propsDp = getHeaderProperties(docDp)
        assertThat(propsDp).isNotNull()
        assertThat(propsDp!!.get(Header.DOC_DENSITY_BEHAVIOR.toInt()))
            .isEqualTo(CoreDocument.DENSITY_BEHAVIOR_DP)

        // Pixels
        val infoPx = CreationDisplayInfo(100, 100, 160, CoreDocument.DENSITY_BEHAVIOR_PIXELS)
        val wPx = RemoteComposeWriter(infoPx, null, p)
        val docPx = parseDocument(wPx)
        assertThat(docPx.densityBehavior).isEqualTo(CoreDocument.DENSITY_BEHAVIOR_PIXELS)
        val propsPx = getHeaderProperties(docPx)
        assertThat(propsPx).isNotNull()
        assertThat(propsPx!!.get(Header.DOC_DENSITY_BEHAVIOR.toInt()))
            .isEqualTo(CoreDocument.DENSITY_BEHAVIOR_PIXELS)
    }

    @Test
    fun testDensityBehavior_v7_legacySucceedsAndNotWritten() {
        val p = createProfile(7)
        val info = CreationDisplayInfo(100, 100, 160, CoreDocument.DENSITY_BEHAVIOR_LEGACY)
        val w = RemoteComposeWriter(info, null, p)
        val doc = parseDocument(w)

        // Should default to legacy on player side
        assertThat(doc.densityBehavior).isEqualTo(CoreDocument.DENSITY_BEHAVIOR_LEGACY)

        // But the tag should NOT be in the header properties
        val props = getHeaderProperties(doc)
        if (props != null) {
            assertThat(props.get(Header.DOC_DENSITY_BEHAVIOR.toInt())).isNull()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDensityBehavior_v7_dpFails() {
        val p = createProfile(7)
        val info = CreationDisplayInfo(100, 100, 160, CoreDocument.DENSITY_BEHAVIOR_DP)
        RemoteComposeWriter(info, null, p)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDensityBehavior_v7_pixelsFails() {
        val p = createProfile(7)
        val info = CreationDisplayInfo(100, 100, 160, CoreDocument.DENSITY_BEHAVIOR_PIXELS)
        RemoteComposeWriter(info, null, p)
    }

    @Test
    fun testDensityBehavior_v6_legacySucceedsAndNotWritten() {
        val p = createProfile(6)
        val info = CreationDisplayInfo(100, 100, 160, CoreDocument.DENSITY_BEHAVIOR_LEGACY)
        val w = RemoteComposeWriter(info, null, p)
        val doc = parseDocument(w)

        // Should default to legacy on player side
        assertThat(doc.densityBehavior).isEqualTo(CoreDocument.DENSITY_BEHAVIOR_LEGACY)

        // V6 doesn't write map at all, so properties should be null (or at least no density
        // behavior)
        val props = getHeaderProperties(doc)
        if (props != null) {
            assertThat(props.get(Header.DOC_DENSITY_BEHAVIOR.toInt())).isNull()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDensityBehavior_v6_dpFails() {
        val p = createProfile(6)
        val info = CreationDisplayInfo(100, 100, 160, CoreDocument.DENSITY_BEHAVIOR_DP)
        RemoteComposeWriter(info, null, p)
    }

    @Test
    fun testConstructorWithProfileSerializesDocProfiles() {
        // Create a profile with a non-baseline operations profile (PROFILE_ANDROIDX)
        val testProfile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, rcPlatform) {
                _,
                profile,
                _ ->
                RemoteComposeWriter(profile)
            }

        // Construct the writer using the constructor under test, without passing DOC_PROFILES
        val testWriter =
            RemoteComposeWriter(
                testProfile,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            )

        // Encode to byte array
        val bytes = testWriter.encodeToByteArray()

        // Decode (round-trip) into a CoreDocument
        val inputStream = java.io.ByteArrayInputStream(bytes)
        val decodedBuffer = RemoteComposeBuffer.fromInputStream(inputStream)
        val decodedDoc = CoreDocument()
        decodedDoc.initFromBuffer(decodedBuffer)

        // Assert that the profile is correctly restored (not defaulted to 0)
        assertThat(decodedDoc.profileMask).isEqualTo(RcProfiles.PROFILE_ANDROIDX)
    }
}
