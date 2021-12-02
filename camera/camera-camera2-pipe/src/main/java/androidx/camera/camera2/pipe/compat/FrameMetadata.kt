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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.ArrayMap
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.RequestMetadata

/**
 * An implementation of [FrameMetadata] that retrieves values from a [CaptureResult] object
 */
@Suppress("SyntheticAccessor") // Using an inline class generates a synthetic constructor
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class AndroidFrameMetadata constructor(
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

    override fun unwrap(): CaptureResult? = null
}

/**
 * A version of [FrameMetadata] that can override (fix) metadata.
 */
internal class CorrectedFrameMetadata(
    private var frameMetadata: FrameMetadata,
    override var extraMetadata: Map<*, Any?>
) : FrameMetadata {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Metadata.Key<T>): T? =
        extraMetadata[key] as T? ?: frameMetadata[key]

    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = get(key) ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureResult.Key<T>): T? =
        extraMetadata[key] as T? ?: frameMetadata[key]

    override fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T = get(key) ?: default

    override val camera: CameraId
        get() = frameMetadata.camera

    override val frameNumber: FrameNumber
        get() = frameMetadata.frameNumber

    override fun unwrap(): CaptureResult? = frameMetadata.unwrap()
}

/**
 * An implementation of [FrameInfo] that retrieves values from a [TotalCaptureResult] object.
 */
@Suppress("SyntheticAccessor") // Using an inline class generates a synthetic constructor
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class AndroidFrameInfo(
    private val totalCaptureResult: TotalCaptureResult,
    override val camera: CameraId,
    override val requestMetadata: RequestMetadata
) : FrameInfo {

    private val result = AndroidFrameMetadata(
        totalCaptureResult,
        camera
    )
    private val physicalResults: Map<CameraId, FrameMetadata>

    init {
        // Metadata for physical cameras was introduced in Android P so that it can be used to
        // determine state of the physical lens and sensor in a multi-camera configuration.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val physicalResults = Api28Compat.getPhysicalCaptureResults(totalCaptureResult)
            if (physicalResults != null && physicalResults.isNotEmpty()) {
                val map = ArrayMap<CameraId, AndroidFrameMetadata>(physicalResults.size)
                for (entry in physicalResults) {
                    val physicalCamera = CameraId(entry.key)
                    map[physicalCamera] =
                        AndroidFrameMetadata(
                            entry.value,
                            physicalCamera
                        )
                }
                this.physicalResults = map
            } else {
                this.physicalResults = emptyMap()
            }
        } else {
            physicalResults = emptyMap()
        }
    }

    override val metadata: FrameMetadata
        get() = result

    override fun get(camera: CameraId): FrameMetadata? = physicalResults[camera]

    override val frameNumber: FrameNumber
        get() = result.frameNumber

    override fun unwrap(): TotalCaptureResult? = totalCaptureResult
}
