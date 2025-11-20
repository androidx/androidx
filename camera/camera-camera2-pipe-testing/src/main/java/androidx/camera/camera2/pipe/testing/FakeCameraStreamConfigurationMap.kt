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

package androidx.camera.camera2.pipe.testing

import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraStreamConfigurationMap
import androidx.camera.camera2.pipe.StreamFormat
import kotlin.reflect.KClass

/**
 * A fake implementation of [CameraStreamConfigurationMap] for testing.
 *
 * This class enables a developer to specify a table of input and output formats, along with the
 * properties associated with them.
 *
 * Note: Some of these methods reference "Class" based output formats. This fake object forwards
 * these queries to the corresponding [StreamFormat.PRIVATE] format queries.
 *
 * @param outputTable Ordered table of entries that define output formats, sizes, and properties.
 * @param inputTable Ordered table of entries used for camera input (Reprocessing).
 * @param outputFormatsForInputFormats Map of outputs (list) allowed for a given input (the key)
 * @param outputClassTypes Set of types that can be queried through this fake object. These queries
 *   are redirected to equivalent functions using [StreamFormat.PRIVATE] queries instead.
 */
public class FakeCameraStreamConfigurationMap(
    private val outputTable: List<OutputTableEntry>,
    private val inputTable: List<InputTableEntry> = emptyList(),
    private val outputFormatsForInputFormats: Map<StreamFormat, List<StreamFormat>> = emptyMap(),
    private val outputClassTypes: Set<Class<*>> = emptySet(),
) : CameraStreamConfigurationMap {
    override fun getOutputFormats(): List<StreamFormat> = outputTable.map { it.format }.distinct()

    override fun getValidOutputFormatsForInput(inputFormat: StreamFormat): List<StreamFormat> =
        outputFormatsForInputFormats[inputFormat] ?: emptyList()

    override fun getInputFormats(): List<StreamFormat> = inputTable.map { it.format }.distinct()

    override fun getInputSizes(format: StreamFormat): List<Size> =
        inputTable.filter { it.format == format }.map { it.size }.distinct()

    override fun isOutputSupportedFor(format: StreamFormat): Boolean =
        outputTable.any { it.format == format }

    override fun <T> isOutputSupportedFor(klass: Class<T>): Boolean =
        outputClassTypes.contains(klass)

    override fun isOutputSupportedFor(surface: Surface): Boolean {
        throw UnsupportedOperationException("Fake cannot determine surface type.")
    }

    override fun <T> getOutputSizes(klass: Class<T>): List<Size> =
        getOutputSizes(StreamFormat.PRIVATE)

    override fun getOutputSizes(format: StreamFormat): List<Size> =
        outputTable.filter { it.format == format && !it.isHighRes }.map { it.size }.distinct()

    override fun getHighSpeedVideoSizes(): List<Size> =
        outputTable.filter { it.highSpeedFpsRange != null }.map { it.size }.distinct()

    override fun getHighSpeedVideoFpsRangesFor(size: Size): List<Range<Int>> =
        outputTable
            .filter { it.highSpeedFpsRange != null && it.size == size }
            .mapNotNull { it.highSpeedFpsRange }
            .distinct()

    override fun getHighSpeedVideoFpsRanges(): List<Range<Int>> =
        outputTable.mapNotNull { it.highSpeedFpsRange }.distinct()

    override fun getHighSpeedVideoSizesFor(fpsRange: Range<Int>): List<Size> =
        outputTable.filter { it.highSpeedFpsRange == fpsRange }.map { it.size }.distinct()

    override fun getHighResolutionOutputSizes(format: StreamFormat): List<Size> =
        outputTable.filter { it.format == format && it.isHighRes }.map { it.size }.distinct()

    override fun getOutputMinFrameDuration(format: StreamFormat, size: Size): Long =
        outputTable.single { it.format == format && it.size == size && !it.isHighRes }.minDuration

    override fun <T> getOutputMinFrameDuration(klass: Class<T>, size: Size): Long {
        check(outputClassTypes.contains(klass))
        return getOutputMinFrameDuration(StreamFormat.PRIVATE, size)
    }

    override fun getOutputStallDuration(format: StreamFormat, size: Size): Long =
        outputTable.single { it.format == format && it.size == size && !it.isHighRes }.stallDuration

    override fun <T> getOutputStallDuration(klass: Class<T>, size: Size): Long {
        check(outputClassTypes.contains(klass))
        return getOutputStallDuration(StreamFormat.PRIVATE, size)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return when (type) {
            FakeCameraStreamConfigurationMap::class -> this as T
            else -> null
        }
    }

    /**
     * Defines a single entry in the [FakeCameraStreamConfigurationMap] table for testing purposes.
     *
     * @param isHighRes defines if this entry is classified as a high resolution size.
     * @param minDuration defines the minimum duration between frames when using this output.
     * @param stallDuration defines the extra amount of time the camera may "stall" for when using
     *   this output
     * @param highSpeedFpsRange If defined, will classify this format/size combination as a high
     *   speed output format with the available fps range.
     */
    public data class OutputTableEntry(
        internal val format: StreamFormat,
        internal val size: Size,
        internal val isHighRes: Boolean = false,
        internal val minDuration: Long = 0,
        internal val stallDuration: Long = 0,
        internal val highSpeedFpsRange: Range<Int>? = null,
    )

    /**
     * Defines a single entry for a valid input format for testing purposes.
     *
     * @param format the [StreamFormat] of the input type.
     * @param size the associated [Size] of the input type.
     */
    public data class InputTableEntry(internal val format: StreamFormat, internal val size: Size)
}
