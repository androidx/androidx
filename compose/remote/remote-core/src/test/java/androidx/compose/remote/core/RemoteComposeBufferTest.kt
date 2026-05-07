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

import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.TextStyle
import androidx.compose.remote.core.operations.utilities.ImageScaling
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

    private lateinit var androidXProfile: Profile

    @Before
    fun setUp() {
        rcPlatform = mock<RcPlatformServices>()

        androidXProfile =
            Profile(
                /* apiLevel= */ 7,
                /* operationProfiles= */ RcProfiles.PROFILE_ANDROIDX or
                    RcProfiles.PROFILE_EXPERIMENTAL,
                /* platform= */ rcPlatform,
            )
            /* factory= */ { creationDisplayInfo, profile, _ ->
                RemoteComposeWriter(creationDisplayInfo, null, profile)
            }
    }

    @Test
    fun testTextStyleInBuffer() {
        val rcProfile =
            Profile(7, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL, rcPlatform)
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

        writer.addTextStyle(
            0xFFFF0000.toInt(),
            -1,
            30f,
            -1f,
            -1f,
            0,
            800f,
            null,
            CoreText.TEXT_ALIGN_CENTER,
            1,
            Int.MAX_VALUE,
            0f,
            0f,
            1f,
            0,
            0,
            0,
            false,
            false,
            null,
            null,
            false,
            -1,
        )

        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }
        val hasStyle = coreDoc.mOperations.any { it is TextStyle }
        assertThat(hasStyle).isTrue()
    }

    @Test
    fun initCoreDocumentFromBuffer_withExperimentalFeatures() {
        val rcProfile =
            Profile(7, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL, rcPlatform)
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
            // Use simplified startTextComponent
            writer.startTextComponent(
                RecordingModifier(),
                hello,
                -1, // textStyleId
                0, // flags
            )
            writer.endTextComponent()
        }

        // no crash; can read correct api level from buffer and init the core doc.
        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }
        assertThat(coreDoc.mBuffer.mApiLevel).isEqualTo(7)
        assertThat(coreDoc.mHeader?.profiles ?: 0).isEqualTo(rcProfile.operationsProfiles)
    }

    @Test
    fun imageComponent() {
        val writer =
            RemoteComposeWriter(
                androidXProfile,
                RemoteComposeBuffer(androidXProfile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 188),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, androidXProfile.operationsProfiles),
            )

        writer.root {
            val imageId = writer.addBitmapUrl("https://example.com/a.png")

            writer.image(
                /* modifier = */ RecordingModifier(),
                /* imageId = */ imageId,
                /* scaleType = */ ImageScaling.SCALE_FIT,
                /* alpha = */ 1f,
            )
        }

        // no crash; can read correct api level from buffer and init the core doc.
        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }

        val components = coreDoc.mRootLayoutComponent!!.mList
        assertThat(components).hasSize(2)

        val bitmapId = (components[0] as BitmapData).mImageId
        val imageLayout = components[1] as ImageLayout
        assertThat(imageLayout.componentId).isEqualTo(-3)
        assertThat(imageLayout.bitmapId).isEqualTo(bitmapId)
    }
}
