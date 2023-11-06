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

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.os.Build
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.Quirks
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.video.Quality
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.internal.compat.quirk.VideoQualityQuirk
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class QualityValidatedEncoderProfilesProviderTest {

    private val defaultProvider = createFakeEncoderProfilesProvider(
        mapOf(
            QUALITY_2160P to PROFILES_2160P,
            QUALITY_1080P to PROFILES_1080P,
            QUALITY_720P to PROFILES_720P,
            QUALITY_480P to PROFILES_480P
        )
    )
    private val cameraInfo = FakeCameraInfoInternal()

    @Test
    fun hasNoProfile_canNotGetProfiles() {
        val quirks = createFakeQuirks(unsupportedQualities = emptySet())
        val emptyProvider = createFakeEncoderProfilesProvider()
        val provider = QualityValidatedEncoderProfilesProvider(emptyProvider, cameraInfo, quirks)

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
    fun hasQuirk_canNotGetUnsupportedProfiles() {
        val quirks = createFakeQuirks(
            unsupportedQualities = setOf(UHD, HD) // 2160P, 720P
        )
        val provider = QualityValidatedEncoderProfilesProvider(defaultProvider, cameraInfo, quirks)

        assertThat(provider.hasProfile(QUALITY_2160P)).isFalse()
        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        assertThat(provider.hasProfile(QUALITY_720P)).isFalse()
        assertThat(provider.hasProfile(QUALITY_480P)).isTrue()
        assertThat(provider.getAll(QUALITY_2160P)).isNull()
        assertThat(provider.getAll(QUALITY_1080P)).isNotNull()
        assertThat(provider.getAll(QUALITY_720P)).isNull()
        assertThat(provider.getAll(QUALITY_480P)).isNotNull()
    }

    @Test
    fun hasQuirk_canGetUnsupportedProfiles_whenCanBeWorkaround() {
        val quirks = createFakeQuirks(
            unsupportedQualities = setOf(UHD, HD), // 2160P, 720P
            canBeWorkaround = true
        )
        val provider = QualityValidatedEncoderProfilesProvider(defaultProvider, cameraInfo, quirks)

        assertThat(provider.hasProfile(QUALITY_2160P)).isTrue()
        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        assertThat(provider.hasProfile(QUALITY_720P)).isTrue()
        assertThat(provider.hasProfile(QUALITY_480P)).isTrue()
        assertThat(provider.getAll(QUALITY_2160P)).isNotNull()
        assertThat(provider.getAll(QUALITY_1080P)).isNotNull()
        assertThat(provider.getAll(QUALITY_720P)).isNotNull()
        assertThat(provider.getAll(QUALITY_480P)).isNotNull()
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

    private fun createFakeQuirks(
        unsupportedQualities: Set<Quality> = emptySet(),
        canBeWorkaround: Boolean = false
    ): Quirks {
        return Quirks(listOf(FakeQuirk(unsupportedQualities, canBeWorkaround)))
    }

    class FakeQuirk(
        private val unsupportedQualities: Set<Quality> = emptySet(),
        private val canBeWorkaround: Boolean = false
    ) :
        VideoQualityQuirk {

        override fun isProblematicVideoQuality(
            cameraInfo: CameraInfoInternal,
            quality: Quality
        ): Boolean {
            return unsupportedQualities.contains(quality)
        }

        override fun workaroundBySurfaceProcessing(): Boolean {
            return canBeWorkaround
        }
    }
}
