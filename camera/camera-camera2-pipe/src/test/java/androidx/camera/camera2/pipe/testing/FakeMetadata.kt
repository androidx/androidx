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
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.ResultMetadata

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

    override fun <T> getChecked(key: Metadata.Key<T>): T = (values[key] as T)!!
}

/**
 * Utility class for interacting with objects require specific [CameraCharacteristics] metadata.
 */
class FakeCameraMetadata(
    characteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap()
) : FakeMetadata(metadata), CameraMetadata {

    private val values = characteristics.toMap()

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? = values[key] as T?
    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T {
        val value = values[key]
        return if (value == null) default else value as T
    }

    override fun <T> getChecked(key: CameraCharacteristics.Key<T>): T = (values[key] as T)!!
    override val camera = CameraId("Fake")
    override fun unwrap(): CameraCharacteristics? {
        throw UnsupportedOperationException(
            "FakeCameraMetadata does not wrap CameraCharacteristics")
    }
}

/**
 * Utility class for interacting with objects require specific [CaptureRequest] metadata
 */
class FakeRequestMetadata(
    request: Map<CaptureRequest.Key<*>, Any?> = emptyMap(),
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap()
) : FakeMetadata(metadata), RequestMetadata {
    private val values = request.toMap()

    override fun <T> get(key: CaptureRequest.Key<T>): T? = values[key] as T?
    override fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T {
        val value = values[key]
        return if (value == null) default else value as T
    }

    override fun <T> getChecked(key: CaptureRequest.Key<T>): T = (values[key] as T)!!
    override fun unwrap(): CaptureRequest? {
        throw UnsupportedOperationException(
            "FakeCameraMetadata does not wrap CameraCharacteristics")
    }
}

/**
 * Utility class for interacting with objects require specific [CaptureResult] metadata
 */
class FakeResultMetadata(
    override val request: RequestMetadata,
    override val camera: CameraId = CameraId("Fake"),
    result: Map<CaptureResult.Key<*>, Any?> = emptyMap(),
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap()
) : FakeMetadata(metadata), ResultMetadata {

    private val values = result.toMap()

    override fun <T> get(key: CaptureResult.Key<T>): T? = values[key] as T?
    override fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T {
        val value = values[key]
        return if (value == null) default else value as T
    }

    override fun <T> getChecked(key: CaptureResult.Key<T>): T = (values[key] as T)!!

    override fun unwrap(): CaptureResult? {
        throw UnsupportedOperationException(
            "FakeCameraMetadata does not wrap CameraCharacteristics")
    }
}
