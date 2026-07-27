/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range
import android.util.Size
import android.view.Surface
import java.lang.Class

internal class AndroidStreamConfigurationMap(
    private val streamConfigurationMap: StreamConfigurationMap,
    private val cameraId: CameraId,
) : StreamConfigurationMapWrapper {
    override fun getOutputFormats(): List<Int> {
        return streamConfigurationMap.outputFormats?.toList() ?: emptyList()
    }

    override fun getValidOutputFormatsForInput(inputFormat: Int): List<Int> {
        return streamConfigurationMap.getValidOutputFormatsForInput(inputFormat)?.toList()
            ?: emptyList()
    }

    override fun getInputFormats(): List<Int> {
        return streamConfigurationMap.inputFormats?.toList() ?: emptyList()
    }

    override fun getInputSizes(format: Int): List<Size> {
        return streamConfigurationMap.getInputSizes(format)?.toList() ?: emptyList()
    }

    override fun isOutputSupportedFor(format: Int): Boolean {
        return streamConfigurationMap.isOutputSupportedFor(format)
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

    override fun getOutputSizes(format: Int): List<Size> {
        return streamConfigurationMap.getOutputSizes(format)?.toList() ?: emptyList()
    }

    override fun getHighSpeedVideoSizes(): List<Size> {
        return streamConfigurationMap.highSpeedVideoSizes?.toList() ?: emptyList()
    }

    override fun getHighSpeedVideoFpsRangesFor(size: Size): List<Range<Int>> {
        return streamConfigurationMap.getHighSpeedVideoFpsRangesFor(size)?.toList() ?: emptyList()
    }

    override fun getHighSpeedVideoFpsRanges(): List<Range<Int>> {
        return streamConfigurationMap.highSpeedVideoFpsRanges?.toList() ?: emptyList()
    }

    override fun getHighSpeedVideoSizesFor(fpsRange: Range<Int>): List<Size> {
        return streamConfigurationMap.getHighSpeedVideoSizesFor(fpsRange)?.toList() ?: emptyList()
    }

    override fun getHighResolutionOutputSizes(format: Int): List<Size> {
        return streamConfigurationMap.getHighResolutionOutputSizes(format)?.toList() ?: emptyList()
    }

    override fun getOutputMinFrameDuration(format: Int, size: Size): Long {
        return streamConfigurationMap.getOutputMinFrameDuration(format, size)
    }

    override fun <T> getOutputMinFrameDuration(klass: Class<T>, size: Size): Long {
        return streamConfigurationMap.getOutputMinFrameDuration(klass, size)
    }

    override fun getOutputStallDuration(format: Int, size: Size): Long {
        return streamConfigurationMap.getOutputStallDuration(format, size)
    }

    override fun <T> getOutputStallDuration(klass: Class<T>, size: Size): Long {
        return streamConfigurationMap.getOutputStallDuration(klass, size)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? {
        return when {
            type.isInstance(streamConfigurationMap) -> streamConfigurationMap as T
            type.isInstance(this) -> this as T
            else -> null
        }
    }

    override fun toString(): String {
        return "AndroidStreamConfigurationMap(cameraId=$cameraId)"
    }
}
