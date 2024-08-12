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

package androidx.camera.camera2.pipe

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.util.Size
import androidx.annotation.RestrictTo

/**
 * [CameraExtensionMetadata] is a compatibility wrapper around [CameraExtensionCharacteristics].
 *
 * Applications should, in most situations, prefer using this interface to unwrapping and using the
 * underlying [CameraExtensionCharacteristics] object directly. Implementation(s) of this interface
 * provide compatibility guarantees and performance improvements over using
 * [CameraExtensionCharacteristics] directly. This allows code to get reasonable behavior for all
 * properties across all OS levels and makes behavior that depends on [CameraExtensionMetadata]
 * easier to test and reason about.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CameraExtensionMetadata : Metadata, UnsafeWrapper {
    operator fun <T> get(key: CameraCharacteristics.Key<T>): T?

    fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T

    val camera: CameraId
    val cameraExtension: Int

    val isRedacted: Boolean
    val isPostviewSupported: Boolean
    val isCaptureProgressSupported: Boolean

    val keys: Set<CameraCharacteristics.Key<*>>
    val requestKeys: Set<CaptureRequest.Key<*>>
    val resultKeys: Set<CaptureResult.Key<*>>

    /** Get output sizes that can be used for high-quality capture requests. */
    fun getOutputSizes(imageFormat: Int): Set<Size>

    /** Get output sizes that can be used for repeating preview requests. */
    fun getOutputSizes(klass: Class<*>): Set<Size>

    /** Get sizes that may be used for the postview stream. */
    fun getPostviewSizes(captureSize: Size, format: Int): Set<Size>
}
