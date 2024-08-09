/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.ImageReaderWrapper
import androidx.camera.camera2.pipe.media.ImageWrapper
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/** Utility class for simulating [FakeImage] and testing code that uses an [ImageReaderWrapper]. */
public class FakeImageReader
private constructor(
    private val format: StreamFormat,
    override val capacity: Int,
    override val surface: Surface,
    public val streamId: StreamId,
    private val outputs: Map<OutputId, Size>
) : ImageReaderWrapper {
    private val debugId = debugIds.incrementAndGet()
    private val closed = atomic(false)
    private val onImageListener = atomic<ImageReaderWrapper.OnImageListener?>(null)

    private val lock = Any()
    private val _images = mutableListOf<FakeImage>()

    /** Retrieve a list of every image that has been created from this FakeImageReader. */
    public val images: List<FakeImage>
        get() = synchronized(lock) { _images.toMutableList() }

    public val isClosed: Boolean
        get() = closed.value

    /**
     * Simulate an image at a specific [imageTimestamp] for a particular (optional) [OutputId]. The
     * timebase for an imageReader is left undefined.
     */
    public fun simulateImage(imageTimestamp: Long, outputId: OutputId? = null): FakeImage {
        val output = outputId ?: outputs.keys.single()
        val size =
            checkNotNull(outputs[output]) { "Unexpected $output! Available outputs are $outputs" }
        val image = FakeImage(size.width, size.height, format.value, imageTimestamp)
        simulateImage(image, output)
        return image
    }

    /**
     * Simulate an image using a specific [ImageWrapper] for the given outputId. The size must
     * match.
     */
    public fun simulateImage(image: ImageWrapper, outputId: OutputId) {
        val size =
            checkNotNull(outputs[outputId]) {
                "Unexpected $outputId! Available outputs are $outputs"
            }
        check(image.width == size.width)
        check(image.height == size.height)

        synchronized(lock) {
            if (image is FakeImage) {
                _images.add(image)
            }
        }
        onImageListener.value?.onImage(streamId, outputId, image)
    }

    override fun setOnImageListener(onImageListener: ImageReaderWrapper.OnImageListener) {
        this.onImageListener.value = onImageListener
    }

    override fun flush() {
        // NoOp
    }

    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        // Fake objects cannot be unwrapped.
        return null
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            surface.release()
        }
    }

    override fun toString(): String = "FakeImageReader-$debugId"

    /** [check] that all images produced by this [FakeImageReader] have been closed. */
    public fun checkImagesClosed() {
        for ((i, fakeImage) in images.withIndex()) {
            check(fakeImage.isClosed) {
                "Failed to close image $i / ${images.size} $fakeImage from $this"
            }
        }
    }

    public companion object {
        private val debugIds = atomic(0)

        /** Create a [FakeImageReader] that can simulate images. */
        public fun create(
            format: StreamFormat,
            streamId: StreamId,
            outputId: OutputId,
            size: Size,
            capacity: Int,
            fakeSurfaces: FakeSurfaces? = null
        ): FakeImageReader =
            create(format, streamId, mapOf(outputId to size), capacity, fakeSurfaces)

        /** Create a [FakeImageReader] that can simulate different sized images. */
        public fun create(
            format: StreamFormat,
            streamId: StreamId,
            outputIdMap: Map<OutputId, Size>,
            capacity: Int,
            fakeSurfaces: FakeSurfaces? = null
        ): FakeImageReader {

            // Find smallest by areas to pick the default surface size. This matches the behavior of
            // MultiResolutionImageReader.
            val smallestOutput = outputIdMap.values.minBy { it.width * it.height }
            val surface =
                fakeSurfaces?.createFakeSurface(smallestOutput)
                    ?: FakeSurfaces.create(smallestOutput)
            return FakeImageReader(format, capacity, surface, streamId, outputIdMap)
        }
    }
}

public class FakeOnImageListener : ImageReaderWrapper.OnImageListener {
    public val onImageEvents: MutableList<OnImageEvent> = mutableListOf<OnImageEvent>()

    override fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
        onImageEvents.add(OnImageEvent(streamId, outputId, image))
    }

    public data class OnImageEvent(
        val streamId: StreamId,
        val outputId: OutputId,
        val image: ImageWrapper
    )
}
