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

package androidx.camera.common.testing

import android.graphics.ImageFormat as GraphicsImageFormat
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.common.ImageFormat
import androidx.camera.common.ImageFormats
import androidx.camera.common.StreamConfigurationMapWrapper
import java.lang.Class
import java.util.LinkedHashMap

// Camera2 DURATION_20FPS_NS threshold (50ms) definition:
// https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/hardware/camera2/params/StreamConfigurationMap.java;l=2283
private const val DURATION_20FPS_NS = 50000000L

/**
 * A fake implementation of [StreamConfigurationMapWrapper] for testing.
 *
 * This class enables a developer to specify a table of input and output formats, along with the
 * properties associated with them.
 *
 * ### Example
 *
 * ```kotlin
 * val fakeMap = FakeStreamConfigurationMap(
 *     outputsTable = linkedMapOf(
 *         OutputKey(ImageFormat.YUV_420_888, Size(1920, 1080)) to OutputValues(),
 *         OutputKey(ImageFormat.JPEG, Size(1920, 1080)) to OutputValues(
 *             stallDuration = 200000000L // 200ms stall duration for JPEG
 *         )
 *     )
 * )
 * ```
 *
 * @param outputsTable The map of supported outputs, mapping [OutputKey] to [OutputValues]. Must be
 *   a [LinkedHashMap] to preserve insertion order. Used by output-related queries (e.g.,
 *   [getOutputFormats], [getOutputSizes], [getOutputMinFrameDuration]).
 * @param inputTable The list of supported inputs. Used by [getInputFormats] and [getInputSizes].
 * @param outputFormatsForInputFormats Mapping of input formats to their valid output formats. Used
 *   by [getValidOutputFormatsForInput].
 * @param outputClassTypes Set of class types that are supported as outputs. Used by
 *   [isOutputSupportedFor] (class-based queries) and [getOutputMinFrameDuration] (class-based
 *   queries). Class-based queries internally map to [GraphicsImageFormat.PRIVATE].
 */
public class FakeStreamConfigurationMap
@JvmOverloads
constructor(
    outputsTable: LinkedHashMap<OutputKey, OutputValues>,
    private val inputTable: List<InputTableEntry> = emptyList(),
    private val outputFormatsForInputFormats: Map<@ImageFormat Int, List<@ImageFormat Int>> =
        emptyMap(),
    private val outputClassTypes: Set<Class<*>> = emptySet(),
) : StreamConfigurationMapWrapper {

    private val outputsTable: LinkedHashMap<OutputKey, OutputValues> = LinkedHashMap(outputsTable)

    private fun isHighRes(values: OutputValues): Boolean {
        return values.minDuration > DURATION_20FPS_NS
    }

    override fun getOutputFormats(): List<Int> = outputsTable.keys.map { it.format }.distinct()

    override fun getValidOutputFormatsForInput(inputFormat: Int): List<Int> =
        outputFormatsForInputFormats[inputFormat] ?: emptyList()

    override fun getInputFormats(): List<Int> = inputTable.map { it.format }.distinct()

    override fun getInputSizes(format: Int): List<Size> =
        inputTable.filter { it.format == format }.map { it.size }.distinct()

    override fun isOutputSupportedFor(format: Int): Boolean =
        outputsTable.keys.any { it.format == format }

    override fun <T> isOutputSupportedFor(klass: Class<T>): Boolean =
        outputClassTypes.contains(klass)

    override fun isOutputSupportedFor(surface: Surface): Boolean {
        throw UnsupportedOperationException("Fake cannot determine surface type.")
    }

    override fun <T> getOutputSizes(klass: Class<T>): List<Size> =
        getOutputSizes(GraphicsImageFormat.PRIVATE)

    override fun getOutputSizes(format: Int): List<Size> =
        outputsTable.filter { it.key.format == format && !isHighRes(it.value) }.map { it.key.size }

    override fun getHighSpeedVideoSizes(): List<Size> =
        outputsTable.filter { it.value.highSpeedFpsRanges.isNotEmpty() }.map { it.key.size }

    override fun getHighSpeedVideoFpsRangesFor(size: Size): List<Range<Int>> =
        outputsTable
            .filter { it.key.size == size }
            .flatMap { it.value.highSpeedFpsRanges }
            .distinct()

    override fun getHighSpeedVideoFpsRanges(): List<Range<Int>> =
        outputsTable.flatMap { it.value.highSpeedFpsRanges }.distinct()

    override fun getHighSpeedVideoSizesFor(fpsRange: Range<Int>): List<Size> =
        outputsTable.filter { it.value.highSpeedFpsRanges.contains(fpsRange) }.map { it.key.size }

    override fun getHighResolutionOutputSizes(format: Int): List<Size> =
        outputsTable.filter { it.key.format == format && isHighRes(it.value) }.map { it.key.size }

    override fun getOutputMinFrameDuration(format: Int, size: Size): Long {
        val values =
            outputsTable[OutputKey(format, size)]
                ?: throw NoSuchElementException("No entry found for format $format and size $size")
        return values.minDuration
    }

    override fun <T> getOutputMinFrameDuration(klass: Class<T>, size: Size): Long {
        check(outputClassTypes.contains(klass))
        return getOutputMinFrameDuration(GraphicsImageFormat.PRIVATE, size)
    }

    override fun getOutputStallDuration(format: Int, size: Size): Long {
        val values =
            outputsTable[OutputKey(format, size)]
                ?: throw NoSuchElementException("No entry found for format $format and size $size")
        return values.stallDuration
    }

    override fun <T> getOutputStallDuration(klass: Class<T>, size: Size): Long {
        check(outputClassTypes.contains(klass))
        return getOutputStallDuration(GraphicsImageFormat.PRIVATE, size)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        if (type.isInstance(this)) this as T else null

    /** Key used to identify an output configuration in [FakeStreamConfigurationMap]. */
    public class OutputKey(@ImageFormat public val format: Int, public val size: Size) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is OutputKey) return false
            return format == other.format && size == other.size
        }

        override fun hashCode(): Int {
            var result = format
            result = 31 * result + size.hashCode()
            return result
        }

        override fun toString(): String {
            return "OutputKey(format=${ImageFormats.name(format)}, size=$size)"
        }
    }

    /** Values associated with an output configuration in [FakeStreamConfigurationMap]. */
    public class OutputValues
    @JvmOverloads
    constructor(
        public val minDuration: Long = 0L,
        public val stallDuration: Long = 0L,
        public val highSpeedFpsRanges: List<Range<Int>> = emptyList(),
    ) {
        override fun toString(): String {
            return "OutputValues(minDuration=$minDuration, " +
                "stallDuration=$stallDuration, " +
                "highSpeedFpsRanges=$highSpeedFpsRanges)"
        }
    }

    /** Defines a single entry for a valid input format for testing purposes. */
    public class InputTableEntry(@ImageFormat public val format: Int, public val size: Size) {
        override fun toString(): String {
            return "InputTableEntry(format=${ImageFormats.name(format)}, size=$size)"
        }
    }
}
