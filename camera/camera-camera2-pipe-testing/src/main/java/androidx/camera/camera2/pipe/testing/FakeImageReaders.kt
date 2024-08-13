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

import android.util.Size
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.ImageReaderWrapper

/**
 * Utility class for creating, tracking, and simulating [FakeImageReader]s. ImageReaders can be
 * retrieved based on [Surface] or by [StreamId], and supports both single and
 * MultiResolutionImageReader-like implementations.
 */
public class FakeImageReaders(private val fakeSurfaces: FakeSurfaces) {
    private val lock = Any()

    @GuardedBy("lock") private val fakeImageReaders = mutableListOf<FakeImageReader>()

    public operator fun get(surface: Surface): FakeImageReader? {
        return synchronized(lock) { fakeImageReaders.find { it.surface == surface } }
    }

    public operator fun get(streamId: StreamId): FakeImageReader? {
        return synchronized(lock) { fakeImageReaders.find { it.streamId == streamId } }
    }

    /** Create a [FakeImageReader] based on a single [CameraStream]. */
    public fun create(cameraStream: CameraStream, capacity: Int): FakeImageReader =
        create(
            cameraStream.outputs.first().format,
            cameraStream.id,
            cameraStream.outputs.associate { it.id to it.size },
            capacity
        )

    /** Create a [FakeImageReader] from its properties. */
    public fun create(
        format: StreamFormat,
        streamId: StreamId,
        outputIdMap: Map<OutputId, Size>,
        capacity: Int,
        fakeSurfaces: FakeSurfaces? = null
    ): FakeImageReader {
        check(this[streamId] == null) {
            "Cannot create multiple ImageReader(s) from the same $streamId!"
        }

        val fakeImageReader =
            FakeImageReader.create(format, streamId, outputIdMap, capacity, fakeSurfaces)
        synchronized(lock) { fakeImageReaders.add(fakeImageReader) }
        return fakeImageReader
    }

    /** Create a [FakeImageReader] based on a [CameraStream] and an [ImageSourceConfig]. */
    public fun create(
        cameraStream: CameraStream,
        imageSourceConfig: ImageSourceConfig
    ): ImageReaderWrapper =
        create(
            cameraStream.outputs.first().format,
            cameraStream.id,
            cameraStream.outputs.associate { it.id to it.size },
            imageSourceConfig.capacity,
            fakeSurfaces
        )

    /** [check] that all [FakeImageReader]s are closed. */
    public fun checkImageReadersClosed() {
        for (fakeImageReader in fakeImageReaders) {
            check(fakeImageReader.isClosed) { "Failed to close ImageReader: $fakeImageReader" }
        }
    }

    /** [check] that all images from all [FakeImageReader]s are closed. */
    public fun checkImagesClosed() {
        for (fakeImageReader in fakeImageReaders) {
            fakeImageReader.checkImagesClosed()
        }
    }
}
