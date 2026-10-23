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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.RemoteComponentCacheKey
import androidx.compose.remote.creation.compose.util.MyRemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteComponentTest {
    private val fakeBuffer = TestRemoteComposeBuffer()
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var remoteCanvas: RemoteCanvas
    private lateinit var drawScope: RemoteDrawScope

    @Before
    fun setUp() {
        val platform = AndroidxRcPlatformServices()
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                _ ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }
        creationState =
            RemoteComposeCreationState(
                RemoteCreationDisplayInfo(100, 50, 160, 1f),
                null,
                profile,
            )
        remoteCanvas = RemoteCanvas(creationState)
        drawScope = RemoteDrawScope(remoteCanvas)
    }

    @Test
    fun componentOffsetProperties_emitComponentValuesAndCacheKeys() {
        val component = drawScope.remote.component
        val componentId = creationState.document.buffer.lastComponentId

        val x = component.x
        val y = component.y
        val rootX = component.rootX
        val rootY = component.rootY

        assertThat(x.cacheKey).isEqualTo(RemoteComponentCacheKey(componentId, "x"))
        assertThat(y.cacheKey).isEqualTo(RemoteComponentCacheKey(componentId, "y"))
        assertThat(rootX.cacheKey).isEqualTo(RemoteComponentCacheKey(componentId, "rootX"))
        assertThat(rootY.cacheKey).isEqualTo(RemoteComponentCacheKey(componentId, "rootY"))

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addComponentValue(42, ${ComponentValue.POS_X})",
                "addComponentValue(43, ${ComponentValue.POS_Y})",
                "addComponentValue(44, ${ComponentValue.POS_ROOT_X})",
                "addComponentValue(45, ${ComponentValue.POS_ROOT_Y})",
            )
            .inOrder()
    }
}
