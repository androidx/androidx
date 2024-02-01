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

package androidx.camera.video.internal.encoder

import android.os.Build
import android.util.Range
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SwappedVideoEncoderInfoTest {

    @Test
    fun canNotSwapWidthHeight_throwException() {
        val videoEncoder = FakeVideoEncoderInfo(
            canSwapWidthHeight = false
        )

        assertThrows(IllegalArgumentException::class.java) {
            SwappedVideoEncoderInfo(videoEncoder)
        }
    }

    @Test
    fun swapWidthHeight() {
        val anyLength = 10
        val widths = Range.create(2, 400)
        val heights = Range.create(4, 300)
        val videoEncoderInfo = FakeVideoEncoderInfo(
            supportedWidths = widths,
            supportedHeights = heights,
            widthAlignment = 2,
            heightAlignment = 4,
        )

        val swappedVideoEncoderInfo = SwappedVideoEncoderInfo(videoEncoderInfo)

        assertThat(swappedVideoEncoderInfo.supportedWidths).isEqualTo(heights)
        assertThat(swappedVideoEncoderInfo.supportedHeights).isEqualTo(widths)
        assertThat(swappedVideoEncoderInfo.getSupportedWidthsFor(anyLength)).isEqualTo(heights)
        assertThat(swappedVideoEncoderInfo.getSupportedHeightsFor(anyLength)).isEqualTo(widths)
        assertThat(swappedVideoEncoderInfo.widthAlignment).isEqualTo(4)
        assertThat(swappedVideoEncoderInfo.heightAlignment).isEqualTo(2)
    }
}
