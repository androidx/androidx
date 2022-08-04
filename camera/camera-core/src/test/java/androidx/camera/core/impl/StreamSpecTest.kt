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
import android.util.Range
import android.util.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StreamSpecTest {

    @Test
    fun canRetrieveResolution() {
        val streamSpec = StreamSpec.builder(TEST_RESOLUTION).build()

        assertThat(streamSpec.resolution).isEqualTo(TEST_RESOLUTION)
    }

    @Test
    fun defaultExpectedFrameRateRangeIsUnspecified() {
        val streamSpec = StreamSpec.builder(TEST_RESOLUTION).build()

        assertThat(streamSpec.expectedFrameRateRange)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
    }

    @Test
    fun canRetrieveExpectedFrameRateRange() {
        val streamSpec = StreamSpec.builder(TEST_RESOLUTION)
            .setExpectedFrameRateRange(TEST_EXPECTED_FRAME_RATE_RANGE)
            .build()

        assertThat(streamSpec.expectedFrameRateRange).isEqualTo(TEST_EXPECTED_FRAME_RATE_RANGE)
    }

    companion object {
        private val TEST_RESOLUTION = Size(640, 480)
        private val TEST_EXPECTED_FRAME_RATE_RANGE = Range(30, 30)
    }
}