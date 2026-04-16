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

package androidx.camera.camera2.adapter

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraExtensionMetadata
import androidx.camera.core.impl.CameraExtensionCapabilities

/** Adapter for [CameraExtensionCapabilities] that wraps [CameraExtensionMetadata]. */
@RequiresApi(Build.VERSION_CODES.S)
internal class CameraExtensionCapabilitiesAdapter(
    private val extensionMetadata: CameraExtensionMetadata
) : CameraExtensionCapabilities {
    override fun <T> get(key: Any): T? {
        if (key is CameraCharacteristics.Key<*>) {
            @Suppress("UNCHECKED_CAST")
            return extensionMetadata[key as CameraCharacteristics.Key<T>]
        }
        return null
    }

    override fun isPostviewSupported(): Boolean = extensionMetadata.isPostviewSupported

    override fun isCaptureProcessProgressSupported(): Boolean =
        extensionMetadata.isCaptureProgressSupported

    override fun getOutputSizes(format: Int): Set<Size> = extensionMetadata.getOutputSizes(format)

    override fun getOutputSizes(klass: Class<*>): Set<Size> =
        extensionMetadata.getOutputSizes(klass)

    override fun getPostviewSizes(captureSize: Size, format: Int): Set<Size> =
        extensionMetadata.getPostviewSizes(captureSize, format)

    override fun getEstimatedCaptureLatencyRangeMillis(
        captureSize: Size,
        format: Int,
    ): Range<Long>? = extensionMetadata.getEstimatedCaptureLatencyRangeMillis(captureSize, format)

    override fun getAvailableCaptureRequestKeys(): Set<Any> =
        @Suppress("UNCHECKED_CAST") extensionMetadata.requestKeys as Set<Any>

    override fun getAvailableCaptureResultKeys(): Set<Any> =
        @Suppress("UNCHECKED_CAST") extensionMetadata.resultKeys as Set<Any>

    override fun getAvailableCharacteristicsKeyValues(): List<Pair<Any, Any>> {
        val result = mutableListOf<Pair<Any, Any>>()
        for (key in extensionMetadata.keys) {
            val value = extensionMetadata[key]
            if (value != null) {
                result.add(key to value)
            }
        }
        return result
    }
}
