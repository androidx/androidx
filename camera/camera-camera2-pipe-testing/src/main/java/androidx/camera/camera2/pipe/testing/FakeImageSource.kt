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
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.ImageReaderImageSource
import androidx.camera.camera2.pipe.media.ImageSource
import kotlinx.atomicfu.atomic

public class FakeImageSource
private constructor(
    private val fakeImageReader: FakeImageReader,
    private val imageSource: ImageSource
) : ImageSource by imageSource {
    private val debugId = debugIds.incrementAndGet()
    private val closed = atomic<Boolean>(false)
    public val isClosed: Boolean
        get() = closed.value

    public val streamId: StreamId
        get() = fakeImageReader.streamId

    /** Retrieve a list of every image that has been created from this FakeImageSource. */
    public val images: List<FakeImage>
        get() = fakeImageReader.images

    public fun simulateImage(timestamp: Long, outputId: OutputId? = null): FakeImage {
        return fakeImageReader.simulateImage(timestamp, outputId)
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            imageSource.close()
        }
    }

    override fun toString(): String = "FakeImageSource-$debugId"

    public companion object {
        private val debugIds = atomic(0)

        public fun create(
            streamFormat: StreamFormat,
            streamId: StreamId,
            outputs: Map<OutputId, Size>,
            capacity: Int,
            fakeImageReaders: FakeImageReaders
        ): FakeImageSource {
            val fakeImageReader = fakeImageReaders.create(streamFormat, streamId, outputs, capacity)

            val imageReaderImageSource = ImageReaderImageSource.create(fakeImageReader)
            return FakeImageSource(fakeImageReader, imageReaderImageSource)
        }
    }
}
