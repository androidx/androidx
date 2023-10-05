/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video.internal

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.EncoderProfiles.VideoProfile.HDR_HLG
import android.os.Build
import androidx.arch.core.util.Function
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class BackupHdrProfileEncoderProfilesProviderTest {

    private val defaultProvider = createFakeEncoderProfilesProvider(
        mapOf(
            QUALITY_2160P to PROFILES_2160P,
            QUALITY_1080P to PROFILES_1080P,
            QUALITY_720P to PROFILES_720P,
            QUALITY_480P to PROFILES_480P
        )
    )
    private val videoEncoderFinder =
        Function<VideoEncoderConfig, VideoEncoderInfo> { FakeVideoEncoderInfo() }

    @Test
    fun hasNoProfile_canNotGetProfiles() {
        val emptyProvider = createFakeEncoderProfilesProvider()
        val provider = BackupHdrProfileEncoderProfilesProvider(emptyProvider, videoEncoderFinder)

        assertThat(provider.hasProfile(QUALITY_2160P)).isFalse()
        assertThat(provider.hasProfile(QUALITY_1080P)).isFalse()
        assertThat(provider.hasProfile(QUALITY_720P)).isFalse()
        assertThat(provider.hasProfile(QUALITY_480P)).isFalse()
        assertThat(provider.getAll(QUALITY_2160P)).isNull()
        assertThat(provider.getAll(QUALITY_1080P)).isNull()
        assertThat(provider.getAll(QUALITY_720P)).isNull()
        assertThat(provider.getAll(QUALITY_480P)).isNull()
    }

    @Test
    fun hasProfile_backupHdrProfileAppended() {
        val qualities = listOf(QUALITY_2160P, QUALITY_1080P, QUALITY_720P, QUALITY_480P)
        for (quality in qualities) {
            val baseVideoProfilesSize = defaultProvider.getAll(quality)!!.videoProfiles.size
            val provider =
                BackupHdrProfileEncoderProfilesProvider(defaultProvider, videoEncoderFinder)

            assertThat(provider.hasProfile(quality)).isTrue()
            val videoProfiles = provider.getAll(quality)!!.videoProfiles
            assertThat(videoProfiles.size).isEqualTo(baseVideoProfilesSize + 1)
            assertThat(videoProfiles.last().hdrFormat).isEqualTo(HDR_HLG)
            assertThat(videoProfiles.last().bitDepth).isEqualTo(BIT_DEPTH_10)
        }
    }

    private fun createFakeEncoderProfilesProvider(
        qualityToProfilesMap: Map<Int, EncoderProfilesProxy> = emptyMap()
    ): EncoderProfilesProvider {
        return FakeEncoderProfilesProvider.Builder().also { builder ->
            for ((quality, profiles) in qualityToProfilesMap) {
                builder.add(quality, profiles)
            }
        }.build()
    }
}
