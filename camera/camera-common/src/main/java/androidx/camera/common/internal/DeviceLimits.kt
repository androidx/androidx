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

package androidx.camera.common.internal

import android.util.Size
import androidx.camera.common.ImageFormat

/** An immutable snapshot of hardware limits. */
public abstract class DeviceLimits internal constructor() {

    /**
     * The maximum preview size.
     *
     * This parameter is mandatory and must be a valid size of non-zero area.
     */
    public abstract val maxPreviewSize: Size

    /**
     * The maximum record size.
     *
     * This parameter is mandatory and must be a valid size of non-zero area.
     */
    public abstract val maxRecordSize: Size

    /**
     * The maximum output sizes map.
     *
     * This parameter is mandatory and must not be empty.
     */
    public abstract val maxOutputSizes: Map<@ImageFormat Int, Size>

    /**
     * The maximum 4:3 output sizes map.
     *
     * This parameter is optional and defaults to an empty map.
     */
    public abstract val maxOutputSizes4by3: Map<@ImageFormat Int, Size>

    /**
     * The maximum 16:9 output sizes map.
     *
     * This parameter is optional and defaults to an empty map.
     */
    public abstract val maxOutputSizes16by9: Map<@ImageFormat Int, Size>

    /**
     * The maximum ultra-high resolution output sizes map.
     *
     * This parameter is optional and defaults to an empty map.
     */
    public abstract val maxUltraOutputSizes: Map<@ImageFormat Int, Size>

    public companion object {
        /**
         * Kotlin DSL entry point for creating [DeviceLimits].
         *
         * @throws IllegalArgumentException if any mandatory parameters are missing or invalid, or
         *   if aspect-ratio specific maximums conflict with global maximums.
         */
        @JvmStatic
        @JvmSynthetic
        public inline fun build(block: MutableDeviceLimits.() -> Unit): DeviceLimits {
            return MutableDeviceLimits.create().apply(block).build()
        }
    }
}

/** Mutable version of DeviceLimits, allowing property assignments. */
public abstract class MutableDeviceLimits internal constructor() : DeviceLimits() {
    public abstract override var maxPreviewSize: Size
    public abstract override var maxRecordSize: Size
    public abstract override var maxOutputSizes: Map<@ImageFormat Int, Size>
    public abstract override var maxOutputSizes4by3: Map<@ImageFormat Int, Size>
    public abstract override var maxOutputSizes16by9: Map<@ImageFormat Int, Size>
    public abstract override var maxUltraOutputSizes: Map<@ImageFormat Int, Size>

    public abstract fun build(): DeviceLimits

    public companion object {
        @JvmStatic public fun create(): MutableDeviceLimits = DeviceLimitsImpl()
    }
}

/** Internal implementation of [DeviceLimits] and [MutableDeviceLimits]. */
internal data class DeviceLimitsImpl(
    override var maxPreviewSize: Size = Size(0, 0),
    override var maxRecordSize: Size = Size(0, 0),
    override var maxOutputSizes: Map<@ImageFormat Int, Size> = emptyMap(),
    override var maxOutputSizes4by3: Map<@ImageFormat Int, Size> = emptyMap(),
    override var maxOutputSizes16by9: Map<@ImageFormat Int, Size> = emptyMap(),
    override var maxUltraOutputSizes: Map<@ImageFormat Int, Size> = emptyMap(),
) : MutableDeviceLimits() {

    override fun build(): DeviceLimits {
        val previewSize = maxPreviewSize
        val recordSize = maxRecordSize
        val outputSizes = maxOutputSizes
        val outputSizes4by3 = maxOutputSizes4by3
        val outputSizes16by9 = maxOutputSizes16by9

        require(previewSize.width > 0 && previewSize.height > 0) {
            "maxPreviewSize must be a valid non-zero size"
        }
        require(recordSize.width > 0 && recordSize.height > 0) {
            "maxRecordSize must be a valid non-zero size"
        }
        require(outputSizes.isNotEmpty()) { "maxOutputSizes must not be empty" }

        fun requirePositiveArea(map: Map<@ImageFormat Int, Size>, name: String) {
            for ((format, size) in map) {
                require(size.width > 0 && size.height > 0) {
                    "$name[$format] must be a valid non-zero area"
                }
            }
        }

        requirePositiveArea(outputSizes, "maxOutputSizes")
        requirePositiveArea(outputSizes4by3, "maxOutputSizes4by3")
        requirePositiveArea(outputSizes16by9, "maxOutputSizes16by9")
        requirePositiveArea(maxUltraOutputSizes, "maxUltraOutputSizes")

        // Ensure aspect-ratio specific maximums don't exceed the global maximum for each format.
        // Note: maxUltraOutputSizes is intentionally excluded from this check because it represents
        // high-resolution modes where the output size explicitly exceeds the standard global
        // maximums.
        fun validateMaxSizeMaps(ratioSpecificMaxSizes: Map<@ImageFormat Int, Size>, name: String) {
            for ((format, size) in ratioSpecificMaxSizes) {
                val max = outputSizes[format]
                requireNotNull(max) {
                    "$name contains format $format which is missing from maxOutputSizes"
                }
                require(size.width <= max.width && size.height <= max.height) {
                    "$name[$format] ($size) exceeds maxOutputSizes[$format] ($max)"
                }
            }
        }
        validateMaxSizeMaps(outputSizes4by3, "maxOutputSizes4by3")
        validateMaxSizeMaps(outputSizes16by9, "maxOutputSizes16by9")

        return this.copy()
    }
}
