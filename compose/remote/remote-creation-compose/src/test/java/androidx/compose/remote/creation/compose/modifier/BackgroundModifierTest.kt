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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackgroundModifierTest {
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var fakeBuffer: TestRemoteComposeBuffer

    private class MyRemoteComposeWriterAndroid(
        profile: Profile,
        buffer: RemoteComposeBuffer,
        vararg tags: RemoteComposeWriter.HTag,
    ) : RemoteComposeWriterAndroid(profile, buffer, *tags)

    @Before
    fun setUp() {
        fakeBuffer = TestRemoteComposeBuffer()

        val platform = androidx.compose.remote.creation.platform.AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)
    }

    @Test
    fun testConstantColorBackground() {
        val color = Color.Red
        val modifier = RemoteModifier.background(color)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }

        // It should serialize to a SolidBackgroundModifier (which writes addModifierBackground)
        // Since Color.Red has red=1, green=0, blue=0, alpha=1
        assertThat(fakeBuffer.calls).containsExactly("addModifierBackground(1.0, 0.0, 0.0, 1.0, 0)")
    }

    @Test
    fun testConstantRemoteColorBackground() {
        val color = Color.Blue.rc
        val modifier = RemoteModifier.background(color)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }

        // Since it has constant value, it should also serialize to SolidBackgroundModifier.
        // Color.Blue has red=0, green=0, blue=1, alpha=1
        assertThat(fakeBuffer.calls).containsExactly("addModifierBackground(0.0, 0.0, 1.0, 1.0, 0)")
    }

    @Test
    fun testDynamicNamedRemoteColorBackground() {
        // Named color represents a dynamic theme token
        val color = RemoteColor.createNamedRemoteColor("color.primary", Color.Green)
        val modifier = RemoteModifier.background(color)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }

        // Since it is dynamic, it should serialize to DynamicSolidBackgroundModifier (which writes
        // addDynamicModifierBackground).
        // The color ID will be allocated in the document. We can verify it writes
        // addDynamicModifierBackground.
        assertThat(fakeBuffer.calls.last()).startsWith("addDynamicModifierBackground")
    }
}
