/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core.impl

import android.media.CamcorderProfile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@Suppress("DEPRECATION")
@SdkSuppress(minSdkVersion = 21)
public class CamcorderProfileProxyTest {

    @Test
    public fun createInstance() {
        // QUALITY_HIGH is guaranteed to be supported.
        assumeTrue(CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH))

        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
        val profileProxy = CamcorderProfileProxy.fromCamcorderProfile(profile)

        assertThat(profileProxy.duration).isEqualTo(profile.duration)
        assertThat(profileProxy.quality).isEqualTo(profile.quality)
        assertThat(profileProxy.fileFormat).isEqualTo(profile.fileFormat)
        assertThat(profileProxy.videoCodec).isEqualTo(profile.videoCodec)
        assertThat(profileProxy.videoBitRate).isEqualTo(profile.videoBitRate)
        assertThat(profileProxy.videoFrameRate).isEqualTo(profile.videoFrameRate)
        assertThat(profileProxy.videoFrameWidth).isEqualTo(profile.videoFrameWidth)
        assertThat(profileProxy.videoFrameHeight).isEqualTo(profile.videoFrameHeight)
        assertThat(profileProxy.audioCodec).isEqualTo(profile.audioCodec)
        assertThat(profileProxy.audioBitRate).isEqualTo(profile.audioBitRate)
        assertThat(profileProxy.audioSampleRate).isEqualTo(profile.audioSampleRate)
        assertThat(profileProxy.audioChannels).isEqualTo(profile.audioChannels)
    }
}
