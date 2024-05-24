/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.ArrayMap
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.core.Debug
import kotlin.reflect.KClass

/** An implementation of [FrameMetadata] that retrieves values from a [CaptureResult] object */
internal class AndroidFrameMetadata(
    private val captureResult: CaptureResult,
    override val camera: CameraId
) : FrameMetadata {
    override fun <T> get(key: Metadata.Key<T>): T? = null

    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = default

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureResult.Key<T>): T? =
        extraMetadata[key] as T? ?: captureResult[key]

    override fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T = get(key) ?: default

    override val frameNumber: FrameNumber
        get() = FrameNumber(captureResult.frameNumber)

    override val extraMetadata: Map<*, Any?> = emptyMap<Any, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CaptureResult::class -> captureResult as T
            TotalCaptureResult::class -> captureResult as? T
            else -> null
        }

    override fun toString(): String =
        "FrameMetadata(camera: $camera, frameNumber: ${captureResult.frameNumber})"
}

/** A version of [FrameMetadata] that can override (fix) metadata. */
internal class CorrectedFrameMetadata(
    private var frameMetadata: FrameMetadata,
    override var extraMetadata: Map<*, Any?>
) : FrameMetadata {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Metadata.Key<T>): T? = extraMetadata[key] as T? ?: frameMetadata[key]

    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = get(key) ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureResult.Key<T>): T? =
        extraMetadata[key] as T? ?: frameMetadata[key]

    override fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T = get(key) ?: default

    override val camera: CameraId
        get() = frameMetadata.camera

    override val frameNumber: FrameNumber
        get() = frameMetadata.frameNumber

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = frameMetadata.unwrapAs(type)
}

/** An implementation of [FrameInfo] that retrieves values from a [TotalCaptureResult] object. */
internal class AndroidFrameInfo(
    private val totalCaptureResult: TotalCaptureResult,
    override val camera: CameraId,
    override val requestMetadata: RequestMetadata
) : FrameInfo {

    private val result = AndroidFrameMetadata(totalCaptureResult, camera)
    private val physicalResults: Map<CameraId, FrameMetadata> =
        Debug.trace("physicalCaptureResults") {
            // Compute a Map<String, CaptureResult> by calling the appropriate compat method and
            // by treating everything as a CaptureResult. Internally, AndroidFrameMetadata will
            // unwrap the object as a TotalCaptureResult instead.
            val physicalResults =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Api31Compat.getPhysicalCameraTotalResults(totalCaptureResult)
                        as Map<String, CaptureResult>
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Android P only supports Map<String, CaptureResult>
                    Api28Compat.getPhysicalCaptureResults(totalCaptureResult)
                } else {
                    emptyMap()
                }

            // Wrap the results using AndroidFrameMetadata.
            if (!physicalResults.isNullOrEmpty()) {
                val map = ArrayMap<CameraId, AndroidFrameMetadata>(physicalResults.size)
                for (entry in physicalResults) {
                    val physicalCamera = CameraId(entry.key)
                    map[physicalCamera] = AndroidFrameMetadata(entry.value, physicalCamera)
                }
                return@trace map
            }
            emptyMap()
        }

    override val metadata: FrameMetadata
        get() = result

    override fun get(camera: CameraId): FrameMetadata? = physicalResults[camera]

    override val frameNumber: FrameNumber
        get() = result.frameNumber

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CaptureResult::class -> totalCaptureResult as T
            TotalCaptureResult::class -> totalCaptureResult as? T
            else -> null
        }

    override fun toString(): String =
        "FrameInfo(camera: ${result.camera}, frameNumber: ${result.frameNumber.value})"
}
