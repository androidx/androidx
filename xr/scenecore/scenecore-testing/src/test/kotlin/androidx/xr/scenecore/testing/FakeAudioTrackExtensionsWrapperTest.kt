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

import android.media.AudioTrack
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeAudioTrackExtensionsWrapperTest {

    private val fakeWrapper = FakeAudioTrackExtensionsWrapper()

    @Test
    fun getSpatialSourceType_returnsByPassByDefault() {
        val track = AudioTrack.Builder().build()
        check(fakeWrapper.getSpatialSourceType(track) == SpatializerConstants.SOURCE_TYPE_BYPASS)
    }

    @Test
    fun setPointSourceParams_defaultSetsAndGetsCorrectly() {
        val track = AudioTrack.Builder().build()
        check(fakeWrapper.getPointSourceParams(track) == null)

        val entity = FakeEntity()
        val params = PointSourceParams()
        // Uses default spatial source type SOURCE_TYPE_BYPASS.
        fakeWrapper.setPointSourceParams(track, params, entity)

        assertThat(fakeWrapper.getPointSourceParams(track)).isEqualTo(params)
    }

    @Test
    fun setPointSourceParams_setsPointSourceAndGetsCorrectly() {
        val track = AudioTrack.Builder().build()
        check(fakeWrapper.getPointSourceParams(track) == null)

        fakeWrapper.spatialSourceTypeMap =
            mutableMapOf(track to SpatializerConstants.SOURCE_TYPE_POINT_SOURCE)
        val entity = FakeEntity()
        val params = PointSourceParams()
        // Uses spatial source type SOURCE_TYPE_POINT_SOURCE.
        fakeWrapper.setPointSourceParams(track, params, entity)

        assertThat(fakeWrapper.getPointSourceParams(track)).isEqualTo(params)
    }

    @Test
    fun setPointSourceParams_doesNotSetIfSoundFieldType() {
        val track = AudioTrack.Builder().build()
        check(fakeWrapper.getPointSourceParams(track) == null)

        fakeWrapper.spatialSourceTypeMap =
            mutableMapOf(track to SpatializerConstants.SOURCE_TYPE_SOUND_FIELD)
        val params = PointSourceParams()
        // Uses spatial source type SOURCE_TYPE_SOUND_FIELD.
        fakeWrapper.setPointSourceParams(track, params, null)

        assertThat(fakeWrapper.getPointSourceParams(track)).isNull()
    }

    @Test
    fun soundFieldAttributesMap_setsAndGetsCorrectly() {
        val track = AudioTrack.Builder().build()
        check(fakeWrapper.getSoundFieldAttributes(track) == null)

        val attributes = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER)
        fakeWrapper.setSoundFieldAttributes(track, attributes)

        assertThat(fakeWrapper.getSoundFieldAttributes(track)).isEqualTo(attributes)
    }

    @Test
    fun setSoundFieldAttributes_setsCorrectly() {
        val builder = AudioTrack.Builder()
        assertThat(
                fakeWrapper.setSoundFieldAttributes(
                    builder,
                    SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER),
                )
            )
            .isEqualTo(builder)
    }
}
