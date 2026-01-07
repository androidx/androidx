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
import androidx.compose.remote.core.operations.Header
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
}
