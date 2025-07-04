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

package androidx.xr.runtime.testing

import android.media.MediaPlayer
import androidx.xr.runtime.internal.PointSourceParams
import androidx.xr.runtime.internal.SoundFieldAttributes
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
        val params = PointSourceParams(FakeEntity())
        check(fakeWrapper.pointSourceParams[mediaPlayer] == null)

        fakeWrapper.setPointSourceParams(mediaPlayer, params)

        assertThat(fakeWrapper.pointSourceParams[mediaPlayer]).isEqualTo(params)
    }

    @Test
    fun setSoundFieldAttributes_storesAttributes() {
        val mediaPlayer = MediaPlayer()
        val attributes = SoundFieldAttributes(1)
        check(fakeWrapper.soundFieldAttributes[mediaPlayer] == null)

        fakeWrapper.setSoundFieldAttributes(mediaPlayer, attributes)

        assertThat(fakeWrapper.soundFieldAttributes[mediaPlayer]).isEqualTo(attributes)
    }
}
