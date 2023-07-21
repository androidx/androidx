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

package androidx.camera.testing.fakes

import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.video.internal.encoder.VideoEncoderInfo

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FakeVideoEncoderInfo(
    @JvmField var supportedWidths: Range<Int> = Range.create(0, Integer.MAX_VALUE),
    @JvmField var supportedHeights: Range<Int> = Range.create(0, Integer.MAX_VALUE),
    @JvmField var widthAlignment: Int = 2,
    @JvmField var heightAlignment: Int = 2,
    @JvmField var supportedBitrateRange: Range<Int> = Range(1, Int.MAX_VALUE)
) : FakeEncoderInfo(), VideoEncoderInfo {
    override fun isSizeSupported(width: Int, height: Int) =
        supportedWidths.contains(width) && supportedHeights.contains(height) &&
            width.mod(widthAlignment) == 0 && height.mod(heightAlignment) == 0

    override fun getSupportedWidths(): Range<Int> {
        return supportedWidths
    }

    override fun getSupportedHeights(): Range<Int> {
        return supportedHeights
    }

    override fun getSupportedWidthsFor(height: Int): Range<Int> {
        return supportedWidths
    }

    override fun getSupportedHeightsFor(width: Int): Range<Int> {
        return supportedHeights
    }

    override fun getWidthAlignment(): Int {
        return widthAlignment
    }

    override fun getHeightAlignment(): Int {
        return heightAlignment
    }

    override fun getSupportedBitrateRange(): Range<Int> {
        return supportedBitrateRange
    }
}