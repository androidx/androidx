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
class FakeImageReader
private constructor(
    private val format: StreamFormat,
    override val capacity: Int,
    override val surface: Surface,
    private val streamId: StreamId,
    private val outputs: Map<OutputId, Size>
) : ImageReaderWrapper {
    private val closed = atomic(false)
    private val onImageListener = atomic<ImageReaderWrapper.OnImageListener?>(null)

    val isClosed: Boolean
        get() = closed.value

    /**
     * Simulate an image at a specific [timestamp]. The timebase for an imageReader is undefined.
     */
    fun simulateImage(timestamp: Long): FakeImage {
        val outputId = outputs.keys.single()
        return simulateImage(outputId, timestamp)
    }

    /**
     * Simulate an image using a specific [outputId] and [timestamp]. The timebase for an
     * imageReader is undefined.
     */
    fun simulateImage(outputId: OutputId, timestamp: Long): FakeImage {
        val size =
            checkNotNull(outputs[outputId]) {
                "Unexpected $outputId! Available outputs are $outputs"
            }
        val image = FakeImage(size.width, size.height, format.value, timestamp)
        simulateImage(outputId, image)
        return image
    }

    /**
     * Simulate an image using a specific [ImageWrapper] for the given outputId. The size must
     * match.
     */
    fun simulateImage(outputId: OutputId, image: ImageWrapper) {
        val size =
            checkNotNull(outputs[outputId]) {
                "Unexpected $outputId! Available outputs are $outputs"
            }
        check(image.width == size.width)
        check(image.height == size.height)
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

    companion object {

        /** Create a [FakeImageReader] that can simulate images. */
        fun create(
            format: StreamFormat,
            streamId: StreamId,
            outputId: OutputId,
            size: Size,
            capacity: Int
        ): FakeImageReader = create(format, streamId, mapOf(outputId to size), capacity)

        /** Create a [FakeImageReader] that can simulate different sized images. */
        fun create(
            format: StreamFormat,
            streamId: StreamId,
            outputIdMap: Map<OutputId, Size>,
            capacity: Int
        ): FakeImageReader {

            // Find smallest by areas to pick the default surface size. This matches the behavior of
            // MultiResolutionImageReader.
            val smallestOutput = outputIdMap.values.minBy { it.width * it.height }
            val surface = FakeSurfaces.create(smallestOutput)
            return FakeImageReader(format, capacity, surface, streamId, outputIdMap)
        }
    }
}

class FakeOnImageListener : ImageReaderWrapper.OnImageListener {
    val onImageEvents = mutableListOf<OnImageEvent>()

    override fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
        onImageEvents.add(OnImageEvent(streamId, outputId, image))
    }

    data class OnImageEvent(
        val streamId: StreamId,
        val outputId: OutputId,
        val image: ImageWrapper
    )
}
