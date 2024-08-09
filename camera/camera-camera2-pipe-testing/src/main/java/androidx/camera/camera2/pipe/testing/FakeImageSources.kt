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

import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.ImageSource
import androidx.camera.camera2.pipe.media.ImageSources

/**
 * Utility class for creating, tracking, and simulating [FakeImageSource]s. [FakeImageSource](s) can
 * be retrieved based on [Surface] or by [StreamId], and supports both single and
 * MultiResolutionImageReader-like implementations.
 */
public class FakeImageSources(private val fakeImageReaders: FakeImageReaders) : ImageSources {
    private val lock = Any()

    @GuardedBy("lock") private val fakeImageSources = mutableListOf<FakeImageSource>()

    public operator fun get(surface: Surface): FakeImageSource? {
        return synchronized(lock) { fakeImageSources.find { it.surface == surface } }
    }

    public operator fun get(streamId: StreamId): FakeImageSource? {
        return synchronized(lock) { fakeImageSources.find { it.streamId == streamId } }
    }

    override fun createImageSource(
        cameraStream: CameraStream,
        imageSourceConfig: ImageSourceConfig
    ): ImageSource {
        check(this[cameraStream.id] == null) {
            "Cannot create multiple ImageSource(s) from the same $cameraStream!"
        }
        val fakeImageSource =
            FakeImageSource.create(
                cameraStream.outputs.first().format,
                cameraStream.id,
                cameraStream.outputs.associate { it.id to it.size },
                imageSourceConfig.capacity,
                fakeImageReaders
            )
        synchronized(lock) { fakeImageSources.add(fakeImageSource) }
        return fakeImageSource
    }

    /** [check] that all [FakeImageSource]s are closed. */
    public fun checkImageSourcesClosed(): Unit =
        synchronized(lock) {
            for (fakeImageSource in fakeImageSources) {
                check(fakeImageSource.isClosed) { "Failed to close ImageSource!: $fakeImageSource" }
            }
        }

    /** [check] that all images from all [FakeImageReader]s are closed. */
    public fun checkImagesClosed(): Unit =
        synchronized(lock) {
            for ((i, fakeImageSource) in fakeImageSources.withIndex()) {
                for ((j, fakeImage) in fakeImageSource.images.withIndex()) {
                    check(fakeImage.isClosed) {
                        "Failed to close $fakeImage ($j / " +
                            "${fakeImageSource.images.size}) from $fakeImageSource " +
                            "($i / ${fakeImageSources.size})"
                    }
                }
            }
        }
}
