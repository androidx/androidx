/*
 * Copyright 2024 The Android Open Source Project
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

import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.RequestMetadata
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

private val fakeFrameNumbers = atomic(0L)

internal fun nextFakeFrameNumber(): FrameNumber = FrameNumber(fakeFrameNumbers.incrementAndGet())

/** Utility class for interacting with objects require specific [CaptureResult] metadata */
class FakeFrameMetadata(
    private val resultMetadata: Map<CaptureResult.Key<*>, Any?> = emptyMap(),
    extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    override val camera: CameraId = nextFakeCameraId(),
    override val frameNumber: FrameNumber = nextFakeFrameNumber(),
    override val extraMetadata: Map<*, Any?> = emptyMap<Any, Any>()
) : FakeMetadata(extraResultMetadata), FrameMetadata {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureResult.Key<T>): T? =
        extraMetadata[key] as T? ?: resultMetadata[key] as T?

    override fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T = get(key) ?: default

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = null

    override fun toString(): String =
        "FakeFrameMetadata(camera: ${camera.value}, frameNumber: ${frameNumber.value})"
}

/** Utility class for interacting with objects require specific [TotalCaptureResult] metadata */
class FakeFrameInfo(
    override val metadata: FrameMetadata = FakeFrameMetadata(),
    override val requestMetadata: RequestMetadata = FakeRequestMetadata(),
    private val physicalMetadata: Map<CameraId, FrameMetadata> = emptyMap()
) : FrameInfo {
    override fun get(camera: CameraId): FrameMetadata? = physicalMetadata[camera]

    override val camera: CameraId
        get() = metadata.camera

    override val frameNumber: FrameNumber
        get() = metadata.frameNumber

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = null

    override fun toString(): String =
        "FakeFrameInfo(camera: ${camera.value}, frameNumber: ${frameNumber.value})"
}
