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

package androidx.camera.camera2.compat

import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range
import android.util.Size
import androidx.camera.core.Logger

private const val TAG = "StreamConfigurationMapCompatBaseImpl"

internal open class StreamConfigurationMapCompatBaseImpl(
    val streamConfigurationMap: StreamConfigurationMap?
) : StreamConfigurationMapCompat.StreamConfigurationMapCompatImpl {

    override fun getOutputFormats(): Array<Int>? {
        // b/361590210: try-catch to workaround the NullPointerException issue when using
        // StreamConfigurationMap provided by Robolectric.
        val outputFormats =
            try {
                streamConfigurationMap?.outputFormats
            } catch (e: NullPointerException) {
                Logger.w(TAG, "Failed to get output formats from StreamConfigurationMap", e)
                null
            } catch (e: IllegalArgumentException) {
                Logger.w(TAG, "Failed to get output formats from StreamConfigurationMap", e)
                null
            }

        return outputFormats?.toTypedArray()
    }

    override fun getOutputSizes(format: Int): Array<Size>? {
        return streamConfigurationMap?.getOutputSizes(format)
    }

    override fun <T> getOutputSizes(klass: Class<T>): Array<Size>? {
        return streamConfigurationMap?.getOutputSizes(klass) ?: emptyArray()
    }

    override fun getHighResolutionOutputSizes(format: Int): Array<Size>? {
        return streamConfigurationMap?.getHighResolutionOutputSizes(format)
    }

    override fun getHighSpeedVideoFpsRanges(): Array<Range<Int>>? {
        return streamConfigurationMap?.getHighSpeedVideoFpsRanges()
    }

    override fun getHighSpeedVideoFpsRangesFor(size: Size): Array<Range<Int>>? {
        return streamConfigurationMap?.getHighSpeedVideoFpsRangesFor(size)
    }

    override fun getHighSpeedVideoSizes(): Array<Size>? {
        return streamConfigurationMap?.getHighSpeedVideoSizes()
    }

    override fun getHighSpeedVideoSizesFor(fpsRange: Range<Int>): Array<Size>? {
        return streamConfigurationMap?.getHighSpeedVideoSizesFor(fpsRange)
    }

    override fun getOutputMinFrameDuration(format: Int, size: Size): Long {
        return streamConfigurationMap?.getOutputMinFrameDuration(format, size) ?: 0L
    }

    override fun unwrap(): StreamConfigurationMap? {
        return streamConfigurationMap
    }
}
