/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureResult
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.LatestFrameMetadata
import androidx.camera.camera2.pipe.Metadata

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LatestFrameMetadataImpl(
    val captureResultKeys: Array<CaptureResult.Key<*>>,
    val captureResultValues: Array<Any?>,
    val captureResultFrameNumbers: Array<FrameNumber?>,
    val rawMetadataKeys: Array<Metadata.Key<*>>,
    val metadataValues: Array<Any?>,
    val metadataFrameNumbers: Array<FrameNumber?>,
) : LatestFrameMetadata {

    override val keys: List<CaptureResult.Key<*>>
        get() = captureResultKeys.asList()

    override val metadataKeys: List<Metadata.Key<*>>
        get() = rawMetadataKeys.asList()

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Metadata.Key<T>): T? {
        for (i in rawMetadataKeys.indices) {
            if (rawMetadataKeys[i] == key) {
                return metadataValues[i] as T?
            }
        }
        return null
    }

    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = get(key) ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureResult.Key<T>): T? {
        for (i in captureResultKeys.indices) {
            if (captureResultKeys[i] == key) {
                return captureResultValues[i] as T?
            }
        }
        return null
    }

    override fun <T> getOrDefault(key: CaptureResult.Key<T>, default: T): T = get(key) ?: default

    override fun getFrameNumber(key: CaptureResult.Key<*>): FrameNumber? {
        for (i in captureResultKeys.indices) {
            if (captureResultKeys[i] == key) {
                return captureResultFrameNumbers[i]
            }
        }
        return null
    }

    override fun getFrameNumber(key: Metadata.Key<*>): FrameNumber? {
        for (i in rawMetadataKeys.indices) {
            if (rawMetadataKeys[i] == key) {
                return metadataFrameNumbers[i]
            }
        }
        return null
    }
}
