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

package androidx.camera.camera2.pipe

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import androidx.camera.camera2.pipe.impl.Debug
import java.util.concurrent.ConcurrentHashMap

/**
 * A map-like interface used to describe or interact with metadata from CameraPipe and Camera2.
 *
 * These interfaces are designed to wrap native camera2 metadata objects in a way that allows
 * additional values to be passed back internally computed values, state, or control values.
 *
 * These interfaces are read-only.
 */
interface Metadata {
    operator fun <T> get(key: Key<T>): T?

    fun <T> getOrDefault(key: Key<T>, default: T): T
    fun <T> getChecked(key: Key<T>): T

    /**
     * Metadata keys provide values or controls that are provided or computed by CameraPipe.
     */
    class Key<T> private constructor(private val name: String) {
        companion object {
            @JvmStatic
            internal val keys: ConcurrentHashMap<String, Key<*>> = ConcurrentHashMap()

            /**
             * This will create a new Key instance, and will check to see that the key has not been
             * previously created somewhere else.
             */
            internal fun <T> create(name: String): Key<T> {
                val key = Key<T>(name)
                Debug.checkNull(keys.putIfAbsent(name, key)) { "$name is already defined!" }
                return key
            }
        }

        override fun toString(): String {
            return name
        }
    }
}

/**
 * CameraMetadata is a wrapper around [CameraCharacteristics].
 */
interface CameraMetadata : Metadata, UnsafeWrapper<CameraCharacteristics> {
    operator fun <T> get(key: CameraCharacteristics.Key<T>): T?

    fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T
    fun <T> getChecked(key: CameraCharacteristics.Key<T>): T

    val camera: CameraId
}

/**
 * RequestMetadata is a wrapper around [CaptureRequest].
 */
interface RequestMetadata : Metadata, UnsafeWrapper<CaptureRequest> {
    operator fun <T> get(key: CaptureRequest.Key<T>): T?

    fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T
    fun <T> getChecked(key: CaptureRequest.Key<T>): T
}

/**
 * ResultMetadata is a wrapper around [CaptureResult].
 */
interface ResultMetadata : Metadata, UnsafeWrapper<CaptureResult> {
    operator fun <T> get(key: CaptureResult.Key<T>): T?

    fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T
    fun <T> getChecked(key: CaptureResult.Key<T>): T

    val camera: CameraId
    val request: RequestMetadata
}
