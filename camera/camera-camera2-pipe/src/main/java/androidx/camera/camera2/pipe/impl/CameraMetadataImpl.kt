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

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.impl.Timestamps.formatMs

/**
 * This implementation provides access to CameraCharacteristics and lazy caching of properties
 * that are either expensive to create and access, or that only exist on newer versions of the
 * OS.
 */
class CameraMetadataImpl constructor(
    override val camera: CameraId,
    override val isRedacted: Boolean,
    private val characteristics: CameraCharacteristics,
    private val metadata: Map<Metadata.Key<*>, Any?>
) : CameraMetadata {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Metadata.Key<T>): T? = metadata[key] as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T =
        metadata[key] as T? ?: default

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? = characteristics[key]
    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
        characteristics[key] ?: default

    override fun unwrap(): CameraCharacteristics? = characteristics

    override val keys: Set<CameraCharacteristics.Key<*>> get() = _keys.value
    override val requestKeys: Set<CaptureRequest.Key<*>> get() = _requestKeys.value
    override val resultKeys: Set<CaptureResult.Key<*>> get() = _resultKeys.value
    override val sessionKeys: Set<CaptureRequest.Key<*>> get() = _sessionKeys.value
    override val physicalCameraIds: Set<CameraId> get() = _physicalCameraIds.value
    override val physicalRequestKeys: Set<CaptureRequest.Key<*>>
        get() = _physicalRequestKeys.value

    override val streamMap: StreamConfigurationMap get() = _streamMap.value

    private val _keys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-${camera.value}#keys") {
                    @Suppress("UselessCallOnNotNull")
                    characteristics.keys.orEmpty().toSet()
                }
            } catch (ignored: AssertionError) {
                emptySet<CameraCharacteristics.Key<*>>()
            }
        }

    private val _requestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-${camera.value}#availableCaptureRequestKeys") {
                    @Suppress("UselessCallOnNotNull")
                    characteristics.availableCaptureRequestKeys.orEmpty().toSet()
                }
            } catch (ignored: AssertionError) {
                emptySet<CaptureRequest.Key<*>>()
            }
        }

    private val _resultKeys: Lazy<Set<CaptureResult.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-${camera.value}#availableCaptureResultKeys") {
                    @Suppress("UselessCallOnNotNull")
                    characteristics.availableCaptureResultKeys.orEmpty().toSet()
                }
            } catch (ignored: AssertionError) {
                emptySet<CaptureResult.Key<*>>()
            }
        }

    private val _physicalCameraIds: Lazy<Set<CameraId>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                try {
                    Debug.trace("Camera-${camera.value}#physicalCameraIds") {
                        @Suppress("UselessCallOnNotNull")
                        characteristics.physicalCameraIds.orEmpty().map { CameraId(it) }.toSet()
                    }
                } catch (ignored: AssertionError) {
                    emptySet<CameraId>()
                }
            }
        }

    private val _physicalRequestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                try {
                    Debug.trace("Camera-${camera.value}#availablePhysicalCameraRequestKeys") {
                        @Suppress("UselessCallOnNotNull")
                        characteristics.availablePhysicalCameraRequestKeys.orEmpty().toSet()
                    }
                } catch (ignored: AssertionError) {
                    emptySet<CaptureRequest.Key<*>>()
                }
            }
        }

    private val _sessionKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                try {
                    Debug.trace("Camera-${camera.value}#availableSessionKeys") {
                        @Suppress("UselessCallOnNotNull")
                        characteristics.availableSessionKeys.orEmpty().toSet()
                    }
                } catch (ignored: AssertionError) {
                    emptySet<CaptureRequest.Key<*>>()
                }
            }
        }

    private val _streamMap: Lazy<StreamConfigurationMap> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            val start = Timestamps.now()
            val result =
                Debug.trace("Camera-${camera.value}#SCALER_STREAM_CONFIGURATION_MAP") {
                    get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                }
            val duration = Timestamps.now() - start
            Log.info { "Loaded stream map for ($camera) in ${duration.formatMs()}" }

            result
        }
}