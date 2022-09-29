/*
 * Copyright 2022 The Android Open Source Project
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

import android.util.Range

class FakeVideoEncoderInfo(
    var _supportedWidths: Range<Int> = Range.create(0, Integer.MAX_VALUE),
    var _supportedHeights: Range<Int> = Range.create(0, Integer.MAX_VALUE),
    var _widthAlignment: Int = 2,
    var _heightAlignment: Int = 2,
) : FakeEncoderInfo(), VideoEncoderInfo {

    override fun getSupportedWidths(): Range<Int> {
        return _supportedWidths
    }

    override fun getSupportedHeights(): Range<Int> {
        return _supportedHeights
    }

    override fun getSupportedWidthsFor(height: Int): Range<Int> {
        return _supportedWidths
    }

    override fun getSupportedHeightsFor(width: Int): Range<Int> {
        return _supportedHeights
    }

    override fun getWidthAlignment(): Int {
       return _widthAlignment
    }

    override fun getHeightAlignment(): Int {
        return _heightAlignment
    }
}