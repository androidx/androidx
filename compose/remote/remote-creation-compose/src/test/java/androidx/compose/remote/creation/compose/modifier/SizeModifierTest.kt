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
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.modifiers.HeightInModifier as CoreHeightInModifier
import androidx.compose.remote.creation.modifiers.HeightModifier as CoreHeightModifier
import androidx.compose.remote.creation.modifiers.WidthInModifier as CoreWidthInModifier
import androidx.compose.remote.creation.modifiers.WidthModifier as CoreWidthModifier
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

    @Test
    fun testDefaultMinSize_unconstrained() {
        val modifier = RemoteModifier.defaultMinSize(minWidth = 50.rdp, minHeight = 60.rdp)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(2)
        val widthElement = recordingModifier.list[0] as CoreWidthInModifier
        val heightElement = recordingModifier.list[1] as CoreHeightInModifier
        assertThat(widthElement.min).isEqualTo(50f)
        assertThat(heightElement.min).isEqualTo(60f)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addWidthInModifierOperation(50.0, ${Float.MAX_VALUE})",
                "addHeightInModifierOperation(60.0, ${Float.MAX_VALUE})",
            )
            .inOrder()
    }

    @Test
    fun testDefaultMinSize_withExplicitWidth() {
        val modifier =
            RemoteModifier.width(100.rdp).defaultMinSize(minWidth = 50.rdp, minHeight = 60.rdp)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(2)
        val widthElement = recordingModifier.list[0] as CoreWidthModifier
        val heightElement = recordingModifier.list[1] as CoreHeightInModifier
        assertThat(widthElement.type).isEqualTo(Type.EXACT_DP)
        assertThat(widthElement.value).isEqualTo(100f)
        assertThat(heightElement.min).isEqualTo(60f)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addWidthModifierOperation(${Type.EXACT_DP.ordinal}, 100.0)",
                "addHeightInModifierOperation(60.0, ${Float.MAX_VALUE})",
            )
            .inOrder()
    }

    @Test
    fun testDefaultMinSize_withExplicitHeight() {
        val modifier =
            RemoteModifier.height(20.rdp).defaultMinSize(minWidth = 50.rdp, minHeight = 60.rdp)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(2)
        val heightElement = recordingModifier.list[0] as CoreHeightModifier
        val widthElement = recordingModifier.list[1] as CoreWidthInModifier
        assertThat(heightElement.type).isEqualTo(Type.EXACT_DP)
        assertThat(heightElement.value).isEqualTo(20f)
        assertThat(widthElement.min).isEqualTo(50f)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addHeightModifierOperation(${Type.EXACT_DP.ordinal}, 20.0)",
                "addWidthInModifierOperation(50.0, ${Float.MAX_VALUE})",
            )
            .inOrder()
    }

    @Test
    fun testDefaultMinSize_withExplicitSize() {
        val modifier =
            RemoteModifier.size(30.rdp).defaultMinSize(minWidth = 50.rdp, minHeight = 60.rdp)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(2)
        val widthElement = recordingModifier.list[0] as CoreWidthModifier
        val heightElement = recordingModifier.list[1] as CoreHeightModifier
        assertThat(widthElement.type).isEqualTo(Type.EXACT_DP)
        assertThat(widthElement.value).isEqualTo(30f)
        assertThat(heightElement.type).isEqualTo(Type.EXACT_DP)
        assertThat(heightElement.value).isEqualTo(30f)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addWidthModifierOperation(${Type.EXACT_DP.ordinal}, 30.0)",
                "addHeightModifierOperation(${Type.EXACT_DP.ordinal}, 30.0)",
            )
            .inOrder()
    }

    @Test
    fun testDefaultMinSize_withFillMaxWidth() {
        val modifier =
            RemoteModifier.fillMaxWidth().defaultMinSize(minWidth = 50.rdp, minHeight = 60.rdp)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(2)
        val widthElement = recordingModifier.list[0] as CoreWidthModifier
        val heightElement = recordingModifier.list[1] as CoreHeightInModifier
        assertThat(widthElement.type).isEqualTo(Type.FILL)
        assertThat(widthElement.value).isEqualTo(1f)
        assertThat(heightElement.min).isEqualTo(60f)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addWidthModifierOperation(${Type.FILL.ordinal}, 1.0)",
                "addHeightInModifierOperation(60.0, ${Float.MAX_VALUE})",
            )
            .inOrder()
    }

    @Test
    fun testDefaultMinSize_appliedBeforeOtherModifiers() {
        val modifier =
            RemoteModifier.defaultMinSize(minWidth = 50.rdp, minHeight = 60.rdp).width(100.rdp)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.list).hasSize(3)
        val widthInElement = recordingModifier.list[0] as CoreWidthInModifier
        val heightInElement = recordingModifier.list[1] as CoreHeightInModifier
        val widthElement = recordingModifier.list[2] as CoreWidthModifier
        assertThat(widthInElement.min).isEqualTo(50f)
        assertThat(heightInElement.min).isEqualTo(60f)
        assertThat(widthElement.type).isEqualTo(Type.EXACT_DP)
        assertThat(widthElement.value).isEqualTo(100f)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addWidthInModifierOperation(50.0, ${Float.MAX_VALUE})",
                "addHeightInModifierOperation(60.0, ${Float.MAX_VALUE})",
                "addWidthModifierOperation(${Type.EXACT_DP.ordinal}, 100.0)",
            )
            .inOrder()
    }

    @Test
    fun testDefaultMinSize_appliedTwice() {
        val modifier =
            RemoteModifier.defaultMinSize(minWidth = 50.rdp, minHeight = 60.rdp)
                .defaultMinSize(minWidth = 80.rdp, minHeight = 90.rdp)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        // The second defaultMinSize is a no-op because width and height are already constrained by
        // the first defaultMinSize.
        assertThat(recordingModifier.list).hasSize(2)
        val widthElement = recordingModifier.list[0] as CoreWidthInModifier
        val heightElement = recordingModifier.list[1] as CoreHeightInModifier
        assertThat(widthElement.min).isEqualTo(50f)
        assertThat(heightElement.min).isEqualTo(60f)

        for (element in recordingModifier.list) {
            element.write(creationState.document)
        }
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addWidthInModifierOperation(50.0, ${Float.MAX_VALUE})",
                "addHeightInModifierOperation(60.0, ${Float.MAX_VALUE})",
            )
            .inOrder()
    }

    @Test
    fun testDefaultMinSize_appliedTwice_withPaddingAndBackground() {
        val modifier =
            RemoteModifier.defaultMinSize(minWidth = 40.rdp, minHeight = 40.rdp)
                .background(Color.Gray.rc)
                .padding(10.rdp)
                .defaultMinSize(minWidth = 70.rdp, minHeight = 70.rdp)
                .background(Color.Red.rc)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        // The second defaultMinSize is a no-op because width and height are already constrained by
        // the first defaultMinSize.
        val widthInElements = recordingModifier.list.filterIsInstance<CoreWidthInModifier>()
        val heightInElements = recordingModifier.list.filterIsInstance<CoreHeightInModifier>()
        assertThat(widthInElements).hasSize(1)
        assertThat(heightInElements).hasSize(1)
        assertThat(widthInElements[0].min).isEqualTo(40f)
        assertThat(heightInElements[0].min).isEqualTo(40f)
    }
}
