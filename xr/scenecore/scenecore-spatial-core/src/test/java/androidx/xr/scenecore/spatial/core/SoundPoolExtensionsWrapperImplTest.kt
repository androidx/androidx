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

import android.media.SoundPool
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import androidx.xr.scenecore.runtime.SpatializerConstants
import androidx.xr.scenecore.spatial.core.SpatialCoreXrExtensionsHolderProvider.Companion.extensionsLegacy
import com.android.extensions.xr.media.ShadowSoundPoolExtensions
import com.android.extensions.xr.media.SoundPoolExtensions
import com.android.extensions.xr.media.XrSpatialAudioExtensions
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SoundPoolExtensionsWrapperImplTest {
    private val xrExtensions = extensionsLegacy
    private val spatialAudioExtensions: XrSpatialAudioExtensions =
        xrExtensions.xrSpatialAudioExtensions
    private val soundPoolExtensions: SoundPoolExtensions =
        spatialAudioExtensions.soundPoolExtensions

    @Test
    fun playWithPointSource_callsExtensionsPlayWithPointSource() {
        val expected = 123
        val fakeNode = xrExtensions.createNode()
        val entity = mock<AndroidXrEntity>()
        whenever(entity.getNode()).thenReturn(fakeNode)
        val rtParams = PointSourceParams()
        val soundPool = SoundPool.Builder().build()
        ShadowSoundPoolExtensions.extract(soundPoolExtensions).setPlayAsPointSourceResult(expected)
        val wrapper: SoundPoolExtensionsWrapper =
            SoundPoolExtensionsWrapperImpl(soundPoolExtensions)
        val actual =
            wrapper.play(
                soundPool,
                TEST_SOUND_ID,
                rtParams,
                entity,
                TEST_VOLUME,
                TEST_PRIORITY,
                TEST_LOOP,
                TEST_RATE,
            )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun playWithSoundField_callsExtensionsPlayWithSoundField() {
        val expected = 312
        val soundPool = SoundPool.Builder().build()
        ShadowSoundPoolExtensions.extract(soundPoolExtensions).setPlayAsSoundFieldResult(expected)
        val wrapper: SoundPoolExtensionsWrapper =
            SoundPoolExtensionsWrapperImpl(soundPoolExtensions)
        val attributes = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
        val actual =
            wrapper.play(
                soundPool,
                TEST_SOUND_ID,
                attributes,
                TEST_VOLUME,
                TEST_PRIORITY,
                TEST_LOOP,
                TEST_RATE,
            )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSpatialSourceType_returnsFromExtensions() {
        val expected = SpatializerConstants.SOURCE_TYPE_SOUND_FIELD
        val soundPool = SoundPool.Builder().build()
        ShadowSoundPoolExtensions.extract(soundPoolExtensions).setSourceType(expected)
        val wrapper: SoundPoolExtensionsWrapper =
            SoundPoolExtensionsWrapperImpl(soundPoolExtensions)
        val actualSourceType = wrapper.getSpatialSourceType(soundPool, /* streamId= */ 0)

        assertThat(actualSourceType).isEqualTo(SpatializerConstants.SOURCE_TYPE_SOUND_FIELD)
    }

    companion object {
        private const val TEST_SOUND_ID = 0
        private const val TEST_VOLUME = 0f
        private const val TEST_PRIORITY = 0
        private const val TEST_LOOP = 0
        private const val TEST_RATE = 0f
    }
}
