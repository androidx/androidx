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

package androidx.camera.video.internal.compat.quirk

import android.os.Build
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT709
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class MediaCodecDefaultDataSpaceQuirkTest {

    @Test
    fun getSuggestedDataSpace_forAffectedDevice() {
        ShadowBuild.setModel(MODEL_PIXEL_9_PRO)

        val isAffectedDevice = MediaCodecDefaultDataSpaceQuirk.load()
        assertThat(isAffectedDevice).isTrue()
        val dataSpace = MediaCodecDefaultDataSpaceQuirk().getSuggestedDataSpace()
        assertThat(dataSpace).isEqualTo(ENCODER_DATA_SPACE_BT709)
    }

    @Test
    fun getSuggestedDataSpace_forNonAffectedDevice() {
        ShadowBuild.setModel(MODEL_ANOTHER)

        val isAffectedDevice = MediaCodecDefaultDataSpaceQuirk.load()
        assertThat(isAffectedDevice).isFalse()
        val dataSpace = MediaCodecDefaultDataSpaceQuirk().getSuggestedDataSpace()
        assertThat(dataSpace).isEqualTo(ENCODER_DATA_SPACE_UNSPECIFIED)
    }

    companion object {
        private const val MODEL_PIXEL_9_PRO = "pixel 9 pro"
        private const val MODEL_ANOTHER = "other model"
    }
}
