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

package androidx.camera.camera2.pipe

import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.annotation.RestrictTo

/** A compatibility wrapper for [android.hardware.camera2.params.StreamConfigurationMap]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraStreamConfigurationMap : UnsafeWrapper {
    /** @see android.hardware.camera2.params.StreamConfigurationMap.getOutputFormats */
    public fun getOutputFormats(): List<StreamFormat>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getValidOutputFormatsForInput */
    public fun getValidOutputFormatsForInput(inputFormat: StreamFormat): List<StreamFormat>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getInputFormats */
    public fun getInputFormats(): List<StreamFormat>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getInputSizes */
    public fun getInputSizes(format: StreamFormat): List<Size>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.isOutputSupportedFor */
    public fun isOutputSupportedFor(format: StreamFormat): Boolean

    /**
     * Converting this method to non-static for testability.
     *
     * @see android.hardware.camera2.params.StreamConfigurationMap.isOutputSupportedFor
     */
    public fun <T> isOutputSupportedFor(klass: Class<T>): Boolean

    /** @see android.hardware.camera2.params.StreamConfigurationMap.isOutputSupportedFor */
    public fun isOutputSupportedFor(surface: Surface): Boolean

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getOutputSizes */
    public fun <T> getOutputSizes(klass: Class<T>): List<Size>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getOutputSizes */
    public fun getOutputSizes(format: StreamFormat): List<Size>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getHighSpeedVideoSizes */
    public fun getHighSpeedVideoSizes(): List<Size>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getHighSpeedVideoFpsRangesFor */
    public fun getHighSpeedVideoFpsRangesFor(size: Size): List<Range<Int>>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getHighSpeedVideoFpsRanges */
    public fun getHighSpeedVideoFpsRanges(): List<Range<Int>>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getHighSpeedVideoSizesFor */
    public fun getHighSpeedVideoSizesFor(fpsRange: Range<Int>): List<Size>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getHighResolutionOutputSizes */
    public fun getHighResolutionOutputSizes(format: StreamFormat): List<Size>

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getOutputMinFrameDuration */
    public fun getOutputMinFrameDuration(format: StreamFormat, size: Size): Long

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getOutputMinFrameDuration */
    public fun <T> getOutputMinFrameDuration(klass: Class<T>, size: Size): Long

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getOutputStallDuration */
    public fun getOutputStallDuration(format: StreamFormat, size: Size): Long

    /** @see android.hardware.camera2.params.StreamConfigurationMap.getOutputStallDuration */
    public fun <T> getOutputStallDuration(klass: Class<T>, size: Size): Long
}
