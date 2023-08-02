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

@file:RequiresApi(21)

package androidx.camera.core.impl

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.quirk.ProfileResolutionQuirk
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private val PAIR_2160 = Pair(QUALITY_2160P, PROFILES_2160P)
private val PAIR_1080 = Pair(QUALITY_1080P, PROFILES_1080P)
private val PAIR_720 = Pair(QUALITY_720P, PROFILES_720P)
private val PAIR_480 = Pair(QUALITY_480P, PROFILES_480P)

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ResolutionValidatedEncoderProfilesProviderTest {

    private val defaultProvider = createFakeEncoderProfilesProvider(
        arrayOf(PAIR_2160, PAIR_1080, PAIR_720, PAIR_480)
    )

    @Test
    fun hasNoProfile_canNotGetProfiles() {
        val quirks = createQuirksWithProfileResolutionQuirk(
            supportedResolution = arrayOf(RESOLUTION_1080P)
        )
        val emptyProvider = createFakeEncoderProfilesProvider()
        val provider = ResolutionValidatedEncoderProfilesProvider(emptyProvider, quirks)

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
    fun hasQuirk_canOnlyGetSupportedProfiles() {
        val quirks = createQuirksWithProfileResolutionQuirk(
            supportedResolution = arrayOf(RESOLUTION_1080P)
        )
        val provider = ResolutionValidatedEncoderProfilesProvider(defaultProvider, quirks)

        assertThat(provider.hasProfile(QUALITY_2160P)).isFalse()
        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        assertThat(provider.hasProfile(QUALITY_720P)).isFalse()
        assertThat(provider.hasProfile(QUALITY_480P)).isFalse()
        assertThat(provider.getAll(QUALITY_2160P)).isNull()
        assertThat(provider.getAll(QUALITY_1080P)).isNotNull()
        assertThat(provider.getAll(QUALITY_720P)).isNull()
        assertThat(provider.getAll(QUALITY_480P)).isNull()
    }

    @Test
    fun hasNoQuirk_canGetProfiles() {
        val quirks = Quirks(emptyList())
        val provider = ResolutionValidatedEncoderProfilesProvider(defaultProvider, quirks)

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
        qualityToProfilesPairs: Array<Pair<Int, EncoderProfilesProxy>> = emptyArray()
    ): EncoderProfilesProvider {
        return FakeEncoderProfilesProvider.Builder().also { builder ->
            for (pair in qualityToProfilesPairs) {
                builder.add(pair.first, pair.second)
            }
        }.build()
    }

    private fun createQuirksWithProfileResolutionQuirk(
        supportedResolution: Array<Size> = emptyArray()
    ): Quirks {
        return Quirks(listOf(FakeQuirk(supportedResolution)))
    }

    class FakeQuirk(private val supportedResolutions: Array<Size>) : ProfileResolutionQuirk {

        override fun getSupportedResolutions(): MutableList<Size> {
            return supportedResolutions.toMutableList()
        }
    }
}
