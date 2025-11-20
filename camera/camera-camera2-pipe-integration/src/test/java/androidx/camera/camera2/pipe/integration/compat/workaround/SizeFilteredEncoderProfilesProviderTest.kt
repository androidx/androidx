/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_LOW
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class SizeFilteredEncoderProfilesProviderTest {

    private val profilesProvider =
        FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, PROFILES_2160P)
            .add(QUALITY_2160P, PROFILES_2160P)
            .add(QUALITY_1080P, PROFILES_1080P)
            .add(QUALITY_720P, PROFILES_720P)
            .add(QUALITY_480P, PROFILES_480P)
            .add(QUALITY_LOW, PROFILES_480P)
            .build()

    private val supportedSizes = listOf(RESOLUTION_1080P, RESOLUTION_720P)

    private val sizeFilteredProvider =
        SizeFilteredEncoderProfilesProvider(profilesProvider, supportedSizes)

    @Test
    fun quality_shouldBeFilteredBySupportedSizes() {
        assertThat(sizeFilteredProvider.hasProfile(QUALITY_2160P)).isFalse()
        assertThat(sizeFilteredProvider.getAll(QUALITY_2160P)).isNull()

        assertThat(sizeFilteredProvider.hasProfile(QUALITY_2160P)).isFalse()
        assertThat(sizeFilteredProvider.getAll(QUALITY_2160P)).isNull()
        assertThat(sizeFilteredProvider.hasProfile(QUALITY_1080P)).isTrue()
        assertThat(sizeFilteredProvider.getAll(QUALITY_1080P)).isSameInstanceAs(PROFILES_1080P)
        assertThat(sizeFilteredProvider.hasProfile(QUALITY_720P)).isTrue()
        assertThat(sizeFilteredProvider.getAll(QUALITY_720P)).isSameInstanceAs(PROFILES_720P)
        assertThat(sizeFilteredProvider.hasProfile(QUALITY_480P)).isFalse()
        assertThat(sizeFilteredProvider.getAll(QUALITY_480P)).isNull()
    }

    @Test
    fun qualityHighLow_shouldMapToCorrectProfiles() {
        assertThat(sizeFilteredProvider.hasProfile(QUALITY_HIGH)).isTrue()
        assertThat(sizeFilteredProvider.getAll(QUALITY_HIGH)).isSameInstanceAs(PROFILES_1080P)

        assertThat(sizeFilteredProvider.hasProfile(QUALITY_LOW)).isTrue()
        assertThat(sizeFilteredProvider.getAll(QUALITY_LOW)).isSameInstanceAs(PROFILES_720P)
    }
}
