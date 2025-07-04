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

package androidx.camera.video.internal.config

import android.os.Build
import android.util.Range
import android.util.Rational
import androidx.camera.video.AudioSpec.CHANNEL_COUNT_MONO
import androidx.camera.video.AudioSpec.SAMPLE_RATE_RANGE_AUTO
import androidx.camera.video.AudioSpec.SOURCE_FORMAT_PCM_16BIT
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AudioConfigUtilTest {

    @Test
    fun resolveSampleRates_targetEncodeSampleRateRange_auto_noRatio() {
        val targetEncodeSampleRateRange = SAMPLE_RATE_RANGE_AUTO
        val initialTargetEncodeSampleRate = 24000
        val captureToEncodeRatio: Rational? = null

        val result =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRateRange,
                initialTargetEncodeSampleRate,
                CHANNEL_COUNT_MONO,
                SOURCE_FORMAT_PCM_16BIT,
                captureToEncodeRatio,
            )

        assertThat(result.captureRate).isEqualTo(24000)
        assertThat(result.encodeRate).isEqualTo(24000)
    }

    @Test
    fun resolveSampleRates_targetEncodeSampleRateRange_auto_withRatio() {
        val targetEncodeSampleRateRange = SAMPLE_RATE_RANGE_AUTO
        val initialTargetEncodeSampleRate = 24000
        val captureToEncodeRatio = Rational(2, 1)

        val result =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRateRange,
                initialTargetEncodeSampleRate,
                CHANNEL_COUNT_MONO,
                SOURCE_FORMAT_PCM_16BIT,
                captureToEncodeRatio,
            )

        assertThat(result.captureRate).isEqualTo(48000)
        assertThat(result.encodeRate).isEqualTo(24000)
    }

    @Test
    fun resolveSampleRates_targetEncodeSampleRateRange_specific_noRatio() {
        val targetEncodeSampleRateRange = Range(22050, 24000)
        val initialTargetEncodeSampleRate = 24000
        val captureToEncodeRatio: Rational? = null

        val result =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRateRange,
                initialTargetEncodeSampleRate,
                CHANNEL_COUNT_MONO,
                SOURCE_FORMAT_PCM_16BIT,
                captureToEncodeRatio,
            )

        assertThat(result.captureRate).isEqualTo(24000)
        assertThat(result.encodeRate).isEqualTo(24000)
    }

    @Test
    fun resolveSampleRates_targetEncodeSampleRateRange_specific_withRatio() {
        val targetEncodeSampleRateRange = Range(22050, 24000)
        val initialTargetEncodeSampleRate = 24000
        val captureToEncodeRatio = Rational(2, 1)

        val result =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRateRange,
                initialTargetEncodeSampleRate,
                CHANNEL_COUNT_MONO,
                SOURCE_FORMAT_PCM_16BIT,
                captureToEncodeRatio,
            )

        assertThat(result.captureRate).isEqualTo(48000)
        assertThat(result.encodeRate).isEqualTo(24000)
    }

    @Test
    fun resolveSampleRates_targetEncodeSampleRateRange_clamping_noRatio() {
        val targetEncodeSampleRateRange = Range(22050, 22050)
        val initialTargetEncodeSampleRate = 24000
        val captureToEncodeRatio: Rational? = null

        val result =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRateRange,
                initialTargetEncodeSampleRate,
                CHANNEL_COUNT_MONO,
                SOURCE_FORMAT_PCM_16BIT,
                captureToEncodeRatio,
            )

        assertThat(result.captureRate).isEqualTo(22050)
        assertThat(result.encodeRate).isEqualTo(22050)
    }

    @Test
    fun resolveSampleRates_targetEncodeSampleRateRange_clamping_withRatio() {
        val targetEncodeSampleRateRange = Range(22050, 22050)
        val initialTargetEncodeSampleRate = 24000
        val captureToEncodeRatio = Rational(2, 1)

        val result =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRateRange,
                initialTargetEncodeSampleRate,
                CHANNEL_COUNT_MONO,
                SOURCE_FORMAT_PCM_16BIT,
                captureToEncodeRatio,
            )

        assertThat(result.captureRate).isEqualTo(44100)
        assertThat(result.encodeRate).isEqualTo(22050)
    }
}
