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

package androidx.camera.testing.impl.fakes

import android.util.Range
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.CameraExtensionCapabilities

/** Fake implementation of [CameraExtensionCapabilities]. */
@RequiresApi(31)
public class FakeCameraExtensionCapabilities(
    private val isPostviewSupported: Boolean = false,
    private val isCaptureProcessProgressSupported: Boolean = false,
    private val outputSizesFormat: Map<Int, Set<Size>> = emptyMap(),
    private val outputSizesClass: Map<Class<*>, Set<Size>> = emptyMap(),
    private val postviewSizes: Map<CaptureConfig, Set<Size>> = emptyMap(),
    private val estimatedCaptureLatencyRangeMillis: Map<CaptureConfig, Range<Long>> = emptyMap(),
    private val availableCaptureRequestKeys: Set<Any> = emptySet(),
    private val availableCaptureResultKeys: Set<Any> = emptySet(),
    private val characteristicsKeys: Map<Any, Any?> = emptyMap(),
) : CameraExtensionCapabilities {
    /** Configuration for capture. */
    public data class CaptureConfig(val size: Size, val format: Int)

    override fun <T> get(key: Any): T? {
        @Suppress("UNCHECKED_CAST")
        return characteristicsKeys[key] as T?
    }

    override fun isPostviewSupported(): Boolean = isPostviewSupported

    override fun isCaptureProcessProgressSupported(): Boolean = isCaptureProcessProgressSupported

    override fun getOutputSizes(format: Int): Set<Size> = outputSizesFormat[format] ?: emptySet()

    override fun getOutputSizes(klass: Class<*>): Set<Size> = outputSizesClass[klass] ?: emptySet()

    override fun getPostviewSizes(captureSize: Size, format: Int): Set<Size> =
        postviewSizes[CaptureConfig(captureSize, format)] ?: emptySet()

    override fun getEstimatedCaptureLatencyRangeMillis(
        captureSize: Size,
        format: Int,
    ): Range<Long>? = estimatedCaptureLatencyRangeMillis[CaptureConfig(captureSize, format)]

    override fun getAvailableCaptureRequestKeys(): Set<Any> = availableCaptureRequestKeys

    override fun getAvailableCaptureResultKeys(): Set<Any> = availableCaptureResultKeys

    override fun getAvailableCharacteristicsKeyValues(): List<Pair<Any, Any>> {
        val result = mutableListOf<Pair<Any, Any>>()
        for ((key, value) in characteristicsKeys) {
            if (value != null) {
                result.add(key to value)
            }
        }
        return result
    }
}
