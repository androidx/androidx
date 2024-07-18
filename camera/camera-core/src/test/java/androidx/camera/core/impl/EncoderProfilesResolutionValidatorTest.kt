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

package androidx.camera.core.impl

import android.os.Build
import android.util.Size
import androidx.camera.core.impl.quirk.ProfileResolutionQuirk
import androidx.camera.testing.impl.EncoderProfilesUtil
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class EncoderProfilesResolutionValidatorTest {

    @Test
    fun noQuirk_alwaysValid() {
        val validator = EncoderProfilesResolutionValidator(null)

        assertThat(validator.hasValidVideoResolution(EncoderProfilesUtil.PROFILES_2160P)).isTrue()
        assertThat(validator.hasValidVideoResolution(EncoderProfilesUtil.PROFILES_720P)).isTrue()
    }

    @Test
    fun hasQuirk_shouldCheckSupportedResolutions() {
        val quirk = createFakeProfileResolutionQuirk(
            supportedResolution = arrayOf(EncoderProfilesUtil.RESOLUTION_2160P)
        )
        val validator = EncoderProfilesResolutionValidator(listOf(quirk))

        assertThat(validator.hasValidVideoResolution(EncoderProfilesUtil.PROFILES_2160P)).isTrue()
        assertThat(validator.hasValidVideoResolution(EncoderProfilesUtil.PROFILES_720P)).isFalse()
    }

    @Test
    fun nullProfile_notValid() {
        val quirk = createFakeProfileResolutionQuirk(
            supportedResolution = arrayOf(EncoderProfilesUtil.RESOLUTION_2160P)
        )
        val validator = EncoderProfilesResolutionValidator(listOf(quirk))

        assertThat(validator.hasValidVideoResolution(null)).isFalse()
    }

    private fun createFakeProfileResolutionQuirk(
        supportedResolution: Array<Size> = emptyArray()
    ): ProfileResolutionQuirk {
        return FakeQuirk(supportedResolution)
    }

    class FakeQuirk(private val supportedResolutions: Array<Size>) : ProfileResolutionQuirk {

        override fun getSupportedResolutions(): MutableList<Size> {
            return supportedResolutions.toMutableList()
        }
    }
}
