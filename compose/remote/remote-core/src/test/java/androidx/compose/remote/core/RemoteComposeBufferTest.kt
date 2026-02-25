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

package androidx.compose.remote.core

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class RemoteComposeBufferTest {
    private lateinit var rcPlatform: RcPlatformServices

    @Before
    fun setUp() {
        rcPlatform = mock<RcPlatformServices>()
    }

    @Test
    fun initCoreDocumentFromBuffer_withExperimentalFeatures() {
        val rcProfile =
            Profile(
                /* apiLevel= */ 7,
                /* operationProfiles= */ RcProfiles.PROFILE_ANDROIDX or
                    RcProfiles.PROFILE_EXPERIMENTAL,
                /* platform= */ rcPlatform,
            )
            /* factory= */ { creationDisplayInfo, profile, _ ->
                RemoteComposeWriter(creationDisplayInfo, null, profile)
            }

        val writer =
            RemoteComposeWriter(
                rcProfile,
                RemoteComposeBuffer(rcProfile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 188),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, rcProfile.operationsProfiles),
            )

        val hello = writer.textCreateId("Hello")

        writer.root {
            // A CORE_TEXT component that is experimental and in api 7
            writer.startTextComponent(
                /*modifier=*/ RecordingModifier(),
                /*textId=*/ hello,
                /*color=*/ 0xFFD0BCFF.toInt(),
                /*colorId=*/ -1,
                /*fontSize-*/ 10f,
                /*minFontSize=*/ 10f,
                /*maxFontSize=*/ 15f,
                /*fontStyle=*/ 0,
                /*fontWeight=*/ 500f,
                /*fontFamily=*/ null,
                /*textAlign=*/ 0,
                /*overflow*/ 0,
                /*maxLines=*/ 1,
                /*letterSpacing=*/ 0f,
                /*lineHeightAdd-*/ 0f,
                /*lineHeightMultiplier=*/ 1f,
                /*lineBreakStrategy=*/ 0,
                /*hyphenationFrequency=*/ 0,
                /*justificationMode=*/ 0,
                /*underline=*/ false,
                /*strikethrough=*/ false,
                /*fontAxis=*/ null,
                /*fontAxisValues*/ null,
                /*autosize=*/ true,
                /*flags=*/ 0,
            )
            writer.endTextComponent()
        }

        // no crash; can read correct api level from buffer and init the core doc.
        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }
        assertThat(coreDoc.mBuffer.mApiLevel).isEqualTo(7)
        assertThat(coreDoc.mHeader?.profiles ?: 0).isEqualTo(rcProfile.operationsProfiles)
    }
}
