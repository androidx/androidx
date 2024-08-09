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
import android.util.Size
import androidx.camera.camera2.pipe.CameraExtensionMetadata
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Metadata
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

private val fakeCameraIds = atomic(0)

internal fun nextFakeCameraId(): CameraId =
    CameraId("FakeCamera-${fakeCameraIds.incrementAndGet()}")

/** Utility class for interacting with objects that require pre-populated Metadata. */
open class FakeMetadata(private val metadata: Map<Metadata.Key<*>, Any?> = emptyMap()) : Metadata {
    companion object {
        @JvmField val TEST_KEY: Metadata.Key<Int> = Metadata.Key.create("test.key")

        @JvmField val TEST_KEY_ABSENT: Metadata.Key<Int> = Metadata.Key.create("test.key.absent")
    }

    override fun <T> get(key: Metadata.Key<T>): T? = metadata[key] as T?

    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T {
        val value = metadata[key]
        return if (value == null) default else value as T
    }
}

/** Utility class for interacting with objects require specific [CameraCharacteristics] metadata. */
class FakeCameraMetadata(
    private val characteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    cameraId: CameraId = nextFakeCameraId(),
    override val keys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    override val requestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    override val resultKeys: Set<CaptureResult.Key<*>> = emptySet(),
    override val sessionKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    val physicalMetadata: Map<CameraId, CameraMetadata> = emptyMap(),
    override val physicalRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    private val extensions: Map<Int, FakeCameraExtensionMetadata> = emptyMap(),
) : FakeMetadata(metadata), CameraMetadata {

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? = characteristics[key] as T?

    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
        get(key) ?: default

    override val camera: CameraId = cameraId
    override val isRedacted: Boolean = false

    override val physicalCameraIds: Set<CameraId> = physicalMetadata.keys
    override val supportedExtensions: Set<Int>
        get() = extensions.keys

    override suspend fun getPhysicalMetadata(cameraId: CameraId): CameraMetadata =
        physicalMetadata[cameraId]!!

    override fun awaitPhysicalMetadata(cameraId: CameraId): CameraMetadata =
        physicalMetadata[cameraId]!!

    override suspend fun getExtensionMetadata(extension: Int): CameraExtensionMetadata {
        return extensions[extension]!!
    }

    override fun awaitExtensionMetadata(extension: Int): CameraExtensionMetadata {
        return extensions[extension]!!
    }

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = null

    override fun toString(): String = "FakeCameraMetadata(camera: ${camera.value})"
}

/** Utility class for interacting with objects require [CameraExtensionMetadata] */
class FakeCameraExtensionMetadata(
    override val camera: CameraId,
    override val cameraExtension: Int,
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    private val characteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
    override val requestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    override val resultKeys: Set<CaptureResult.Key<*>> = emptySet(),
    private val captureOutputSizes: Map<Int, Set<Size>> = emptyMap(),
    private val previewOutputSizes: Map<Class<*>, Set<Size>> = emptyMap(),
    private val postviewSizes: Map<Int, Map<Size, Set<Size>>> = emptyMap(),
    override val isRedacted: Boolean = false,
    override val isPostviewSupported: Boolean = false,
    override val isCaptureProgressSupported: Boolean = false
) : FakeMetadata(metadata), CameraExtensionMetadata {
    override fun getOutputSizes(imageFormat: Int): Set<Size> {
        return captureOutputSizes[imageFormat] ?: emptySet()
    }

    override fun getOutputSizes(klass: Class<*>): Set<Size> {
        return previewOutputSizes[klass] ?: emptySet()
    }

    override fun getPostviewSizes(captureSize: Size, format: Int): Set<Size> {
        return postviewSizes[format]?.get(captureSize) ?: emptySet()
    }

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? = characteristics[key] as T?

    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
        get(key) ?: default

    override val keys: Set<CameraCharacteristics.Key<*>>
        get() = characteristics.keys

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = null

    override fun toString(): String =
        "FakeCameraExtensionMetadata(camera: ${camera.value}, extension: $cameraExtension)"
}
