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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class VideoSpecTest {

    @Test
    fun newBuilder_containsCorrectDefaults() {
        val videoSpec = VideoSpec.builder().build()

        assertThat(videoSpec.qualitySelector).isEqualTo(VideoSpec.QUALITY_SELECTOR_AUTO)
        assertThat(videoSpec.bitrate).isEqualTo(VideoSpec.BITRATE_RANGE_AUTO)
        assertThat(videoSpec.frameRate).isEqualTo(VideoSpec.FRAME_RATE_RANGE_AUTO)
        assertThat(videoSpec.aspectRatio).isEqualTo(VideoSpec.ASPECT_RATIO_AUTO)
    }
}