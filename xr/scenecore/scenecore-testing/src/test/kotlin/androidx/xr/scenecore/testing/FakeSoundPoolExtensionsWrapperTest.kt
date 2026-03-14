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

package androidx.xr.scenecore.testing

import android.media.SoundPool
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeSoundPoolExtensionsWrapperTest {
    private val testSoundId: Int = 0
    private val testVolume: Float = 0f
    private val testPriority: Int = 0
    private val testLoop: Int = 0
    private val testRate: Float = 0f

    private lateinit var fakeWrapper: FakeSoundPoolExtensionsWrapper

    @Before
    fun setUp() {
        fakeWrapper = FakeSoundPoolExtensionsWrapper()
    }

    @Test
    fun playWithPointSource_getsPointSourceResult() {
        val expected = 123

        val soundPool = SoundPool.Builder().build()
        val entity = FakeEntity()
        val rtParams = PointSourceParams()

        fakeWrapper.setPlayAsPointSourceResult(expected)

        val actual =
            fakeWrapper.play(
                soundPool,
                testSoundId,
                rtParams,
                entity,
                testVolume,
                testPriority,
                testLoop,
                testRate,
            )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun playWithSoundField_getsSoundFieldResult() {
        val expected = 312

        val soundPool = SoundPool.Builder().build()
        val attributes = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)

        fakeWrapper.setPlayAsSoundFieldResult(expected)

        val actual =
            fakeWrapper.play(
                soundPool,
                testSoundId,
                attributes,
                testVolume,
                testPriority,
                testLoop,
                testRate,
            )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSpatialSourceType_returnsSourceType() {
        val expected = SpatializerConstants.SOURCE_TYPE_POINT_SOURCE

        check(fakeWrapper.getSpatialSourceType(SoundPool.Builder().build(), 0) == 0)

        fakeWrapper.sourceType = expected

        val actual = fakeWrapper.getSpatialSourceType(SoundPool.Builder().build(), 0)

        assertThat(actual).isEqualTo(expected)
    }
}
