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

package androidx.camera.video

import android.os.Build
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class AudioSpecTest {

    @Test
    fun newBuilder_containsCorrectDefaults() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )

        val audioSpec = AudioSpec.builder().build()

        assertThat(audioSpec.source).isEqualTo(AudioSpec.SOURCE_AUTO)
        assertThat(audioSpec.sourceFormat).isEqualTo(AudioSpec.SOURCE_FORMAT_AUTO)
        assertThat(audioSpec.bitrate).isEqualTo(AudioSpec.BITRATE_RANGE_AUTO)
        assertThat(audioSpec.channelCount).isEqualTo(AudioSpec.CHANNEL_COUNT_AUTO)
        assertThat(audioSpec.sampleRate).isEqualTo(AudioSpec.SAMPLE_RATE_RANGE_AUTO)
    }
}
