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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import android.media.MediaPlayer
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.testing.internal.FakeMediaPlayerExtensionsWrapper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class FakeMediaPlayerExtensionsWrapperTest {
    private lateinit var fakeWrapper: FakeMediaPlayerExtensionsWrapper

    @Before
    fun setUp() {
        fakeWrapper = FakeMediaPlayerExtensionsWrapper()
    }

    @Test
    fun setPointSourceParams_storesParams() {
        val mediaPlayer = MediaPlayer()
        val entity = FakeEntity()
        val params = PointSourceParams()
        check(fakeWrapper.paramsWithEntity[mediaPlayer] == null)

        fakeWrapper.setPointSourceParams(mediaPlayer, params, entity)

        assertThat(fakeWrapper.paramsWithEntity[mediaPlayer]!!.first).isEqualTo(params)
        assertThat(fakeWrapper.paramsWithEntity[mediaPlayer]!!.second).isEqualTo(entity)
    }

    @Test
    fun setSoundFieldAttributes_storesAttributes() {
        val mediaPlayer = MediaPlayer()
        val attributes = SoundFieldAttributes(1)
        check(fakeWrapper.soundFieldAttributes[mediaPlayer] == null)

        fakeWrapper.setSoundFieldAttributes(mediaPlayer, attributes)

        assertThat(fakeWrapper.soundFieldAttributes[mediaPlayer]).isEqualTo(attributes)
    }

    @Test
    fun setParamsAndAttributes_areMutuallyExclusive() {
        val mediaPlayer = MediaPlayer()
        val entity = FakeEntity()
        val params = PointSourceParams()
        val attributes = SoundFieldAttributes(1)

        // 1. Set Point Source, verify data exists and attributes is empty.
        fakeWrapper.setPointSourceParams(mediaPlayer, params, entity)
        assertThat(fakeWrapper.paramsWithEntity[mediaPlayer]).isNotNull()
        assertThat(fakeWrapper.soundFieldAttributes[mediaPlayer]).isNull()

        // 2. Set Sound Field, verify Point Source is removed.
        fakeWrapper.setSoundFieldAttributes(mediaPlayer, attributes)
        assertThat(fakeWrapper.soundFieldAttributes[mediaPlayer]).isNotNull()
        assertThat(fakeWrapper.paramsWithEntity[mediaPlayer]).isNull()

        // 3. Set back to Point Source, verify Sound Field is removed.
        fakeWrapper.setPointSourceParams(mediaPlayer, params, entity)
        assertThat(fakeWrapper.paramsWithEntity[mediaPlayer]).isNotNull()
        assertThat(fakeWrapper.soundFieldAttributes[mediaPlayer]).isNull()
    }
}
