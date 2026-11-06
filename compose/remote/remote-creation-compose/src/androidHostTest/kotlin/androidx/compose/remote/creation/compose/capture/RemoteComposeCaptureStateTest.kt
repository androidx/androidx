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

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteComposeCaptureStateTest {

    private val platform = AndroidxRcPlatformServices()
    private val displayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f)
    private val profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            0,
            platform,
            { dInfo, p, callback -> RemoteComposeWriterAndroid(dInfo, null, p, callback) },
        )

    private fun createWriter(): RemoteComposeWriterAndroid =
        RemoteComposeWriterAndroid(100, 100, "", platform)

    @Test
    fun commonConstructors_initializeFieldsCorrectly() {
        val customDensity = RemoteDensity(2f.rf, 1.5f.rf)
        val state1 =
            RemoteComposeCaptureState(
                creationDisplayInfo = displayInfo,
                profile = profile,
                writerCallback = null,
                remoteDensity = customDensity,
                layoutDirection = RemoteLayoutDirection.Rtl,
            )
        assertThat(state1.creationDisplayInfo).isSameInstanceAs(displayInfo)
        assertThat(state1.profile).isSameInstanceAs(profile)
        assertThat(state1.remoteDensity).isEqualTo(customDensity)
        assertThat(state1.layoutDirection).isEqualTo(RemoteLayoutDirection.Rtl)
        assertThat(state1.densityBehavior).isEqualTo(displayInfo.densityBehavior)
        assertThat(state1.platform).isSameInstanceAs(platform)

        val state2 =
            RemoteComposeCaptureState(
                creationDisplayInfo = displayInfo,
                contentDescription = "test",
                profile = profile,
            )
        assertThat(state2.creationDisplayInfo).isSameInstanceAs(displayInfo)
        assertThat(state2.layoutDirection).isEqualTo(RemoteLayoutDirection.Ltr)

        val writer = createWriter()
        val state3 =
            RemoteComposeCaptureState(
                creationDisplayInfo = displayInfo,
                profile = profile,
                writer = writer,
            )
        assertThat(state3.document).isSameInstanceAs(writer)
    }

    @Test
    fun creationState_extensionProperties() {
        val state: RemoteComposeCreationState =
            RemoteComposeCreationState(
                creationDisplayInfo = displayInfo,
                profile = profile,
                writerCallback = null,
            )
        assertThat(state.creationDisplayInfo).isSameInstanceAs(displayInfo)
        assertThat(state.profile).isSameInstanceAs(profile)
        assertThat(state.platform).isSameInstanceAs(platform)
    }

    @Test
    fun noRemoteCompose() {
        val noRemote = NoRemoteCompose()
        assertThat(noRemote.creationDisplayInfo.width).isEqualTo(1)
        assertThat(noRemote.creationDisplayInfo.height).isEqualTo(1)
        assertThat(noRemote.layoutDirection).isEqualTo(RemoteLayoutDirection.Ltr)
    }
}
