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

package androidx.camera.video.internal.workaround

import android.media.CamcorderProfile.QUALITY_480P
import android.os.Build
import androidx.arch.core.util.Function
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy
import androidx.camera.core.impl.Quirks
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.DEFAULT_DURATION
import androidx.camera.testing.impl.EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeVideoProfileProxy
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.internal.compat.quirk.ExtraSupportedQualityQuirk
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
class QualityAddedEncoderProfilesProviderTest {

    @Test
    fun canSupportExtraQuality() {
        // Arrange.
        val baseProvider = FakeEncoderProfilesProvider.Builder().build()
        val encoderProfiles = ImmutableEncoderProfilesProxy.create(
            DEFAULT_DURATION,
            DEFAULT_OUTPUT_FORMAT,
            listOf(createFakeAudioProfileProxy()),
            listOf(createFakeVideoProfileProxy(RESOLUTION_480P.width, RESOLUTION_480P.height)),
        )
        val quirks = Quirks(listOf(FakeQuirk(mapOf(QUALITY_480P to encoderProfiles))))
        val cameraInfo = FakeCameraInfoInternal()
        val encoderInfo = FakeVideoEncoderInfo()

        // Act.
        val provider = QualityAddedEncoderProfilesProvider(baseProvider, quirks, cameraInfo) {
            encoderInfo
        }

        // Assert.
        assertThat(provider.getAll(QUALITY_480P)).isNotNull()
    }

    private class FakeQuirk(private val qualityToEncoderProfiles: Map<Int, EncoderProfilesProxy>) :
        ExtraSupportedQualityQuirk() {

        override fun getExtraEncoderProfiles(
            cameraInfo: CameraInfoInternal,
            encoderProfilesProvider: EncoderProfilesProvider,
            videoEncoderInfoFinder: Function<VideoEncoderConfig, VideoEncoderInfo>
        ) = qualityToEncoderProfiles
    }
}
