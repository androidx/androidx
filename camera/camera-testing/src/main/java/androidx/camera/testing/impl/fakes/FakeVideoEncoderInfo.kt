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

package androidx.camera.testing.impl.fakes

import android.util.Range
import androidx.camera.video.internal.encoder.VideoEncoderInfo

public class FakeVideoEncoderInfo(
    @JvmField public var canSwapWidthHeight: Boolean = true,
    @JvmField public var supportedWidths: Range<Int> = Range.create(0, Integer.MAX_VALUE),
    @JvmField public var supportedHeights: Range<Int> = Range.create(0, Integer.MAX_VALUE),
    @JvmField public var widthAlignment: Int = 2,
    @JvmField public var heightAlignment: Int = 2,
    @JvmField public var supportedBitrateRange: Range<Int> = Range(1, Int.MAX_VALUE)
) : FakeEncoderInfo(), VideoEncoderInfo {

    override fun canSwapWidthHeight(): Boolean {
        return canSwapWidthHeight
    }

    override fun isSizeSupported(width: Int, height: Int): Boolean =
        supportedWidths.contains(width) &&
            supportedHeights.contains(height) &&
            width.mod(widthAlignment) == 0 &&
            height.mod(heightAlignment) == 0

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
