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

import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraColorSpaceProfiles
import androidx.camera.camera2.pipe.CameraStreamConfigurationMap
import androidx.camera.camera2.pipe.StreamFormat
import kotlin.reflect.KClass

/**
 * Implementation of the color space profile interface using Camera2 library.
 *
 * @see CameraColorSpaceProfiles
 */
internal class Camera2StreamConfigurationMap(
    private val streamConfigurationMap: StreamConfigurationMap
) : CameraStreamConfigurationMap {
    override fun getOutputFormats(): List<StreamFormat> {
        return streamConfigurationMap.outputFormats.map { StreamFormat(it) }
    }

    override fun getValidOutputFormatsForInput(inputFormat: StreamFormat): List<StreamFormat> {
        return streamConfigurationMap.getValidOutputFormatsForInput(inputFormat.value).map {
            StreamFormat(it)
        }
    }

    override fun getInputFormats(): List<StreamFormat> {
        return streamConfigurationMap.inputFormats.map { StreamFormat(it) }
    }

    override fun getInputSizes(format: StreamFormat): List<Size> {
        return streamConfigurationMap.getInputSizes(format.value)?.toList<Size>() ?: emptyList()
    }

    override fun isOutputSupportedFor(format: StreamFormat): Boolean {
        return streamConfigurationMap.isOutputSupportedFor(format.value)
    }

    override fun <T> isOutputSupportedFor(klass: Class<T>): Boolean {
        return StreamConfigurationMap.isOutputSupportedFor(klass)
    }

    override fun isOutputSupportedFor(surface: Surface): Boolean {
        return streamConfigurationMap.isOutputSupportedFor(surface)
    }

    override fun <T> getOutputSizes(klass: Class<T>): List<Size> {
        return streamConfigurationMap.getOutputSizes(klass)?.toList() ?: emptyList()
    }

    override fun getOutputSizes(format: StreamFormat): List<Size> {
        return streamConfigurationMap.getOutputSizes(format.value)?.toList() ?: emptyList()
    }

    override fun getHighSpeedVideoSizes(): List<Size> {
        return streamConfigurationMap.highSpeedVideoSizes.toList()
    }

    override fun getHighSpeedVideoFpsRangesFor(size: Size): List<Range<Int>> {
        return streamConfigurationMap.getHighSpeedVideoFpsRangesFor(size)?.toList() ?: emptyList()
    }

    override fun getHighSpeedVideoFpsRanges(): List<Range<Int>> {
        return streamConfigurationMap.highSpeedVideoFpsRanges.toList()
    }

    override fun getHighSpeedVideoSizesFor(fpsRange: Range<Int>): List<Size> {
        return streamConfigurationMap.getHighSpeedVideoSizesFor(fpsRange).toList()
    }

    override fun getHighResolutionOutputSizes(format: StreamFormat): List<Size> {
        return streamConfigurationMap.getHighResolutionOutputSizes(format.value)?.toList()
            ?: emptyList()
    }

    override fun getOutputMinFrameDuration(format: StreamFormat, size: Size): Long {
        return streamConfigurationMap.getOutputMinFrameDuration(format.value, size)
    }

    override fun <T> getOutputMinFrameDuration(klass: Class<T>, size: Size): Long {
        return streamConfigurationMap.getOutputMinFrameDuration(klass, size)
    }

    override fun getOutputStallDuration(format: StreamFormat, size: Size): Long {
        return streamConfigurationMap.getOutputStallDuration(format.value, size)
    }

    override fun <T> getOutputStallDuration(klass: Class<T>, size: Size): Long {
        return streamConfigurationMap.getOutputStallDuration(klass, size)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return when (type) {
            StreamConfigurationMap::class -> streamConfigurationMap as T
            Camera2StreamConfigurationMap::class -> this as T
            else -> null
        }
    }

    override fun toString(): String {
        return streamConfigurationMap.toString()
    }
}
