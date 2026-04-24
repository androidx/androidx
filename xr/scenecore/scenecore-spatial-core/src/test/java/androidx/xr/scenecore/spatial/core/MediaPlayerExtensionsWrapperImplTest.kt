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
package androidx.xr.scenecore.spatial.core

import android.media.MediaPlayer
import androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants
import androidx.xr.scenecore.spatial.core.SpatialCoreXrExtensionsHolderProvider.Companion.extensionsLegacy
import com.android.extensions.xr.media.MediaPlayerExtensions
import com.android.extensions.xr.media.ShadowMediaPlayerExtensions
import com.android.extensions.xr.media.SpatializerExtensions
import com.android.extensions.xr.media.XrSpatialAudioExtensions
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class MediaPlayerExtensionsWrapperImplTest {
    private val xrExtensions = extensionsLegacy
    private val spatialAudioExtensions: XrSpatialAudioExtensions =
        xrExtensions.xrSpatialAudioExtensions
    private val mediaPlayerExtensions: MediaPlayerExtensions =
        spatialAudioExtensions.mediaPlayerExtensions

    @Test
    fun setPointSourceParams_callsExtensionsSetPointSourceParams() {
        val mediaPlayer = MediaPlayer()
        val fakeNode = xrExtensions.createNode()
        val entity = mock<AndroidXrEntity>()
        whenever(entity.getNode()).thenReturn(fakeNode)
        val expectedRtParams = PointSourceParams()
        val wrapper: MediaPlayerExtensionsWrapper =
            MediaPlayerExtensionsWrapperImpl(mediaPlayerExtensions)
        wrapper.setPointSourceParams(mediaPlayer, expectedRtParams, entity)

        Truth.assertThat(
                ShadowMediaPlayerExtensions.extract(mediaPlayerExtensions).pointSourceParams.node
            )
            .isEqualTo(fakeNode)
    }

    @Test
    fun setSoundFieldAttr_callsExtensionsSetSoundFieldAttr() {
        val mediaPlayer = MediaPlayer()
        val expectedAmbisonicOrder = SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER
        val expectedRtAttr = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
        val wrapper: MediaPlayerExtensionsWrapper =
            MediaPlayerExtensionsWrapperImpl(mediaPlayerExtensions)
        wrapper.setSoundFieldAttributes(mediaPlayer, expectedRtAttr)

        Truth.assertThat(
                ShadowMediaPlayerExtensions.extract(mediaPlayerExtensions)
                    .soundFieldAttributes
                    .ambisonicsOrder
            )
            .isEqualTo(expectedAmbisonicOrder)
    }
}
