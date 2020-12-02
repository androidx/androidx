/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("UNCHECKED_CAST")

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.StreamConfigurationMap
import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.StreamId

/**
 * Utility class for interacting with objects that require pre-populated Metadata.
 */
open class FakeMetadata(metadata: Map<Metadata.Key<*>, Any?> = emptyMap()) : Metadata {
    companion object {
        val TEST_KEY = Metadata.Key.create<Int>("test.key")
        val TEST_KEY_ABSENT = Metadata.Key.create<Int>("test.key.absent")
    }

    private val values = metadata.toMap()

    override fun <T> get(key: Metadata.Key<T>): T? = values[key] as T?
    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T {
        val value = values[key]
        return if (value == null) default else value as T
    }
}

/**
 * Utility class for interacting with objects require specific [CameraCharacteristics] metadata.
 */
class FakeCameraMetadata(
    characteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    cameraId: CameraId = CameraId("Fake")
) : FakeMetadata(metadata), CameraMetadata {

    private val values = characteristics.toMap()

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? = values[key] as T?
    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
        get(key) ?: default

    override val camera = cameraId
    override val isRedacted = false
    override val keys: Set<CameraCharacteristics.Key<*>> = emptySet()
    override val requestKeys: Set<CaptureRequest.Key<*>> = emptySet()
    override val resultKeys: Set<CaptureResult.Key<*>> = emptySet()
    override val sessionKeys: Set<CaptureRequest.Key<*>> = emptySet()
    override val physicalCameraIds: Set<CameraId> = emptySet()
    override val physicalRequestKeys: Set<CaptureRequest.Key<*>> = emptySet()
    override val streamMap: StreamConfigurationMap
        get() = throw UnsupportedOperationException("StreamConfigurationMap is not available.")

    override fun unwrap(): CameraCharacteristics? {
        throw UnsupportedOperationException(
            "FakeCameraMetadata does not wrap CameraCharacteristics"
        )
    }
}

/**
 * Utility class for interacting with objects require specific [CaptureRequest] metadata
 */
class FakeRequestMetadata(
    private val requestParameters: Map<CaptureRequest.Key<*>, Any?> = emptyMap(),
    extraRequestParameters: Map<Metadata.Key<*>, Any?> = emptyMap(),
    override val template: RequestTemplate = RequestTemplate(0),
    override val streams: Map<StreamId, Surface> = mapOf(),
    override val repeating: Boolean = false,
    override val request: Request = Request(listOf()),
    override val requestNumber: RequestNumber = RequestNumber(4321)
) : FakeMetadata(request.extraRequestParameters.plus(extraRequestParameters)), RequestMetadata {

    override fun <T> get(key: CaptureRequest.Key<T>): T? = requestParameters[key] as T?
    override fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T = get(key) ?: default

    override fun unwrap(): CaptureRequest? {
        throw UnsupportedOperationException(
            "FakeCameraMetadata does not wrap a real CaptureRequest"
        )
    }
}

/**
 * Utility class for interacting with objects require specific [CaptureResult] metadata
 */
class FakeFrameMetadata(
    private val resultMetadata: Map<CaptureResult.Key<*>, Any?> = emptyMap(),
    extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    override val camera: CameraId = CameraId("Fake"),
    override val frameNumber: FrameNumber = FrameNumber(21),
    override val extraMetadata: Map<*, Any?> = emptyMap<Any, Any>()
) : FakeMetadata(extraResultMetadata), FrameMetadata {

    override fun <T> get(key: CaptureResult.Key<T>): T? =
        extraMetadata[key] as T? ?: resultMetadata[key] as T?

    override fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T = get(key) ?: default

    override fun unwrap(): CaptureResult? {
        throw UnsupportedOperationException(
            "FakeCameraMetadata does not wrap a real CaptureResult"
        )
    }
}

/**
 * Utility class for interacting with objects require specific [TotalCaptureResult] metadata
 */
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

    override fun unwrap(): TotalCaptureResult? {
        throw UnsupportedOperationException(
            "FakeFrameInfo does not wrap a real TotalCaptureResult object"
        )
    }
}
