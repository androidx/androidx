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

package androidx.camera.camera2.pipe.integration.compat

import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.Logger
import javax.inject.Inject

/**
 * Helper for accessing features in [StreamConfigurationMap] in a backwards compatible fashion.
 *
 * @param map [StreamConfigurationMap] class to wrap workarounds when output sizes are retrieved.
 * @param outputSizesCorrector [OutputSizesCorrector] class to perform correction on sizes.
 */
@CameraScope
public class StreamConfigurationMapCompat
@Inject
constructor(map: StreamConfigurationMap?, private val outputSizesCorrector: OutputSizesCorrector) {
    private val tag = "StreamConfigurationMapCompat"
    private val cachedFormatOutputSizes = mutableMapOf<Int, Array<Size>>()
    private val cachedFormatHighResolutionOutputSizes = mutableMapOf<Int, Array<Size>?>()
    private val cachedClassOutputSizes = mutableMapOf<Class<*>, Array<Size>>()
    private var impl: StreamConfigurationMapCompatImpl

    init {
        impl =
            if (Build.VERSION.SDK_INT >= 23) {
                StreamConfigurationMapCompatApi23Impl(map)
            } else {
                StreamConfigurationMapCompatBaseImpl(map)
            }
    }

    /**
     * Get the image format output formats in this stream configuration.
     *
     * All image formats returned by this function will be defined in either ImageFormat or in
     * PixelFormat.
     *
     * @return an array of integer format
     * @see [ImageFormat]
     * @see [PixelFormat]
     */
    public fun getOutputFormats(): Array<Int>? {
        return impl.getOutputFormats()
    }

    /**
     * Get a list of sizes compatible with the requested image `format`.
     *
     * Output sizes related quirks will be applied onto the returned sizes list.
     *
     * @param format an image format from [ImageFormat] or [PixelFormat]
     * @return an array of supported sizes, or `null` if the `format` is not a supported output
     */
    public fun getOutputSizes(format: Int): Array<Size>? {
        if (cachedFormatOutputSizes.contains(format)) {
            return cachedFormatOutputSizes[format]?.clone()
        }

        val outputSizes = impl.getOutputSizes(format)

        if (outputSizes.isNullOrEmpty()) {
            Logger.w(tag, "Retrieved output sizes array is null or empty for format $format")
            return outputSizes
        }

        outputSizesCorrector.applyQuirks(outputSizes, format).let {
            cachedFormatOutputSizes[format] = it
            return it.clone()
        }
    }

    /**
     * Get a list of sizes compatible with `klass` to use as an output.
     *
     * Output sizes related quirks will be applied onto the returned sizes list.
     *
     * @param klass a non-`null` [Class] object reference
     * @return an array of supported sizes for [ImageFormat#PRIVATE] format, or `null` if the
     *   `klass` is not a supported output.
     * @throws NullPointerException if `klass` was `null`
     */
    public fun <T> getOutputSizes(klass: Class<T>): Array<Size>? {
        if (cachedClassOutputSizes.contains(klass)) {
            return cachedClassOutputSizes[klass]?.clone()
        }

        val outputSizes = impl.getOutputSizes(klass)

        if (outputSizes.isNullOrEmpty()) {
            Logger.w(tag, "Retrieved output sizes array is null or empty for class $klass")
            return outputSizes
        }

        outputSizesCorrector.applyQuirks(outputSizes, klass).let {
            cachedClassOutputSizes[klass] = it
            return it.clone()
        }
    }

    /**
     * Get a list of supported high resolution sizes, which cannot operate at full
     * [CameraMetadata#REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE] rate.
     *
     * Output sizes related quirks will be applied onto the returned sizes list.
     *
     * @param format an image format from [ImageFormat] or [PixelFormat]
     * @return an array of supported sizes, or `null` if the `format` is not a supported output
     */
    public fun getHighResolutionOutputSizes(format: Int): Array<Size>? {
        if (cachedFormatHighResolutionOutputSizes.contains(format)) {
            return cachedFormatHighResolutionOutputSizes[format]?.clone()
        }

        var outputSizes = impl.getHighResolutionOutputSizes(format)

        // High resolution output sizes can be null.
        if (!outputSizes.isNullOrEmpty()) {
            outputSizes = outputSizesCorrector.applyQuirks(outputSizes, format)
        }

        cachedFormatHighResolutionOutputSizes[format] = outputSizes
        return outputSizes?.clone()
    }

    public fun getOutputMinFrameDuration(format: Int, size: Size?): Long? {
        return impl.getOutputMinFrameDuration(format, size)
    }

    /** Returns the [StreamConfigurationMap] represented by this object. */
    public fun toStreamConfigurationMap(): StreamConfigurationMap? {
        return impl.unwrap()
    }

    internal interface StreamConfigurationMapCompatImpl {
        fun getOutputFormats(): Array<Int>?

        fun getOutputSizes(format: Int): Array<Size>?

        fun <T> getOutputSizes(klass: Class<T>): Array<Size>?

        fun getHighResolutionOutputSizes(format: Int): Array<Size>?

        fun getOutputMinFrameDuration(format: Int, size: Size?): Long?

        /** Returns the underlying [StreamConfigurationMap] instance. */
        fun unwrap(): StreamConfigurationMap?
    }
}
