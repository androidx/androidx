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
import kotlinx.atomicfu.atomic

private val fakeCameraIds = atomic(0)
internal fun nextFakeCameraId(): CameraId =
    CameraId("FakeCamera-${fakeCameraIds.incrementAndGet()}")

private val fakeRequestNumbers = atomic(0L)
internal fun nextFakeRequestNumber(): RequestNumber =
    RequestNumber(fakeRequestNumbers.incrementAndGet())

private val fakeFrameNumbers = atomic(0L)
internal fun nextFakeFrameNumber(): FrameNumber = FrameNumber(fakeFrameNumbers.incrementAndGet())

/**
 * Utility class for interacting with objects that require pre-populated Metadata.
 */
public open class FakeMetadata(
    private val metadata: Map<Metadata.Key<*>, Any?> = emptyMap()
) : Metadata {
    public companion object {
        @JvmField
        public val TEST_KEY: Metadata.Key<Int> = Metadata.Key.create("test.key")

        @JvmField
        public val TEST_KEY_ABSENT: Metadata.Key<Int> = Metadata.Key.create("test.key.absent")
    }

    public override fun <T> get(key: Metadata.Key<T>): T? = metadata[key] as T?
    public override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T {
        val value = metadata[key]
        return if (value == null) default else value as T
    }
}

/**
 * Utility class for interacting with objects require specific [CameraCharacteristics] metadata.
 */
public class FakeCameraMetadata(
    private val characteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    cameraId: CameraId = nextFakeCameraId(),
    override val keys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    override val requestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    override val resultKeys: Set<CaptureResult.Key<*>> = emptySet(),
    override val sessionKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    public val physicalMetadata: Map<CameraId, CameraMetadata> = emptyMap(),
    override val physicalRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
) : FakeMetadata(metadata), CameraMetadata {

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? = characteristics[key] as T?
    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
        get(key) ?: default

    public override val camera: CameraId = cameraId
    public override val isRedacted: Boolean = false

    override val physicalCameraIds: Set<CameraId> = physicalMetadata.keys
    override suspend fun getPhysicalMetadata(cameraId: CameraId): CameraMetadata =
        physicalMetadata[cameraId]!!

    override fun awaitPhysicalMetadata(cameraId: CameraId): CameraMetadata =
        physicalMetadata[cameraId]!!

    /** @throws UnsupportedOperationException */
    override fun unwrap(): CameraCharacteristics? {
        throw UnsupportedOperationException(
            "FakeCameraMetadata does not wrap CameraCharacteristics"
        )
    }
}

/**
 * Utility class for interacting with objects require specific [CaptureRequest] metadata.
 */
public class FakeRequestMetadata(
    private val requestParameters: Map<CaptureRequest.Key<*>, Any?> = emptyMap(),
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    override val template: RequestTemplate = RequestTemplate(0),
    override val streams: Map<StreamId, Surface> = mapOf(),
    override val repeating: Boolean = false,
    override val request: Request = Request(listOf()),
    override val requestNumber: RequestNumber = nextFakeRequestNumber()
) : FakeMetadata(request.extras.plus(metadata)), RequestMetadata {

    override fun <T> get(key: CaptureRequest.Key<T>): T? = requestParameters[key] as T?
    override fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T = get(key) ?: default

    /** @throws UnsupportedOperationException */
    override fun unwrap(): CaptureRequest? {
        throw UnsupportedOperationException(
            "FakeRequestMetadata does not wrap a real CaptureRequest"
        )
    }
}

/**
 * Utility class for interacting with objects require specific [CaptureResult] metadata
 */
public class FakeFrameMetadata(
    private val resultMetadata: Map<CaptureResult.Key<*>, Any?> = emptyMap(),
    extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    override val camera: CameraId = nextFakeCameraId(),
    override val frameNumber: FrameNumber = nextFakeFrameNumber(),
    override val extraMetadata: Map<*, Any?> = emptyMap<Any, Any>()
) : FakeMetadata(extraResultMetadata), FrameMetadata {

    override fun <T> get(key: CaptureResult.Key<T>): T? =
        extraMetadata[key] as T? ?: resultMetadata[key] as T?

    override fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T = get(key) ?: default

    /** @throws UnsupportedOperationException */
    override fun unwrap(): CaptureResult? {
        throw UnsupportedOperationException(
            "FakeFrameMetadata does not wrap a real CaptureResult"
        )
    }
}

/**
 * Utility class for interacting with objects require specific [TotalCaptureResult] metadata
 */
@Suppress("SyntheticAccessor") // Using an inline class generates a synthetic constructor
public class FakeFrameInfo(
    override val metadata: FrameMetadata = FakeFrameMetadata(),
    override val requestMetadata: RequestMetadata = FakeRequestMetadata(),
    private val physicalMetadata: Map<CameraId, FrameMetadata> = emptyMap()
) : FrameInfo {
    override fun get(camera: CameraId): FrameMetadata? = physicalMetadata[camera]

    override val camera: CameraId
        get() = metadata.camera

    override val frameNumber: FrameNumber
        get() = metadata.frameNumber

    /** @throws UnsupportedOperationException */
    override fun unwrap(): TotalCaptureResult? {
        throw UnsupportedOperationException(
            "FakeFrameInfo does not wrap a real TotalCaptureResult object!"
        )
    }
}
