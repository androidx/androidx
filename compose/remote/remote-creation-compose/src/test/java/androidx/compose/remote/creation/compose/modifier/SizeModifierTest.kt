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
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.modifiers.HeightModifier as CoreHeightModifier
import androidx.compose.remote.creation.modifiers.WidthModifier as CoreWidthModifier
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SizeModifierTest {
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
    fun testWrapContentWidth() {
        val modifier = RemoteModifier.wrapContentWidth()
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0]
        assertThat(element).isInstanceOf(CoreWidthModifier::class.java)
        val widthModifier = element as CoreWidthModifier
        assertThat(widthModifier.type).isEqualTo(Type.WRAP)

        element.write(creationState.document)
        assertThat(fakeBuffer.calls)
            .containsExactly("addWidthModifierOperation(${Type.WRAP.ordinal}, 1.0)")
    }

    @Test
    fun testWrapContentHeight() {
        val modifier = RemoteModifier.wrapContentHeight()
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(1)
        val element = recordingModifier.list[0]
        assertThat(element).isInstanceOf(CoreHeightModifier::class.java)
        val heightModifier = element as CoreHeightModifier
        assertThat(heightModifier.type).isEqualTo(Type.WRAP)

        element.write(creationState.document)
        assertThat(fakeBuffer.calls)
            .containsExactly("addHeightModifierOperation(${Type.WRAP.ordinal}, 1.0)")
    }

    @Test
    fun testWrapContentSize() {
        val modifier = RemoteModifier.wrapContentSize()
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(2)
        val widthElement = recordingModifier.list[0] as CoreWidthModifier
        val heightElement = recordingModifier.list[1] as CoreHeightModifier
        assertThat(widthElement.type).isEqualTo(Type.WRAP)
        assertThat(heightElement.type).isEqualTo(Type.WRAP)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addWidthModifierOperation(${Type.WRAP.ordinal}, 1.0)",
                "addHeightModifierOperation(${Type.WRAP.ordinal}, 1.0)",
            )
            .inOrder()
    }
}
