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

package androidx.camera.camera2.pipe.media

import android.media.ImageReader
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.media.AndroidImageReader.Companion.IMAGEREADER_MAX_CAPACITY
import androidx.camera.camera2.pipe.media.ImageReaderImageSource.Companion.IMAGE_SOURCE_CAPACITY
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

internal class ImageReaderImageSources @Inject constructor(private val threads: Threads) :
    ImageSources {
    override fun createImageSource(
        cameraStream: CameraStream,
        imageSourceConfig: ImageSourceConfig
    ): ImageSource {
        return create(
            cameraStream,
            imageSourceConfig.capacity,
            imageSourceConfig.usageFlags,
            imageSourceConfig.defaultDataSpace,
            imageSourceConfig.defaultHardwareBufferFormat
        )
    }

    fun create(
        cameraStream: CameraStream,
        capacity: Int,
        usageFlags: Long?,
        defaultDataSpace: Int?,
        defaultHardwareBufferFormat: Int?
    ): ImageSource {
        require(cameraStream.outputs.isNotEmpty()) { "$cameraStream must have outputs." }
        require(capacity > 0) { "Capacity ($capacity) must be > 0" }
        require(capacity <= IMAGE_SOURCE_CAPACITY) {
            "Capacity for creating new ImageReaderImageSources is restricted to " +
                "$IMAGE_SOURCE_CAPACITY. Android has undocumented internal limits that can vary " +
                "per device."
        }

        val handlerProvider = { threads.camera2Handler }
        val executorProvider = { threads.lightweightExecutor }

        // Increase the internal capacity of the ImageReader so that the final capacity of the
        // ImageSource matches the requested capacity.
        //
        // As an example, if the consumer requests "40", the ImageReader will be created with
        // a capacity of "42", which will allow the consumer to hold exactly 40 images without
        // stalling the camera pipeline.
        val imageReaderCapacity = capacity + IMAGE_SOURCE_CAPACITY

        if (cameraStream.outputs.size == 1) {
            val output = cameraStream.outputs.single()
            val handler = handlerProvider()
            val imageReader =
                AndroidImageReader.create(
                    output.size.width,
                    output.size.height,
                    output.format.value,
                    imageReaderCapacity,
                    usageFlags,
                    defaultDataSpace,
                    defaultHardwareBufferFormat,
                    cameraStream.id,
                    output.id,
                    handler
                )
            return ImageReaderImageSource.create(imageReader)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (usageFlags != null) {
                Log.warn {
                    "Ignoring usageFlags ($usageFlags) " +
                        "for $cameraStream. MultiResolutionImageReader does not support " +
                        "setting usage flags."
                }
            }
            if (defaultDataSpace != null) {
                Log.warn {
                    "Ignoring DataSpace ($defaultDataSpace) " +
                        "for $cameraStream. MultiResolutionImageReader does not support " +
                        "setting the default DataSpace."
                }
            }
            if (defaultHardwareBufferFormat != null) {
                Log.warn {
                    "Ignoring HardwareBufferFormat ($defaultHardwareBufferFormat) " +
                        "for $cameraStream. MultiResolutionImageReader does not support " +
                        "setting the default HardwareBufferFormat."
                }
            }
            val imageReader =
                AndroidMultiResolutionImageReader.create(cameraStream, capacity, executorProvider())
            return ImageReaderImageSource.create(imageReader)
        }

        // If we reach this point, it's likely the user asked for MultiResolutionImageReader
        // but it was not possible to create it due to the SDK the code is running on.
        throw IllegalStateException("Failed to create an ImageSource for $cameraStream!")
    }
}

/** An ImageReaderImageSource implements an [ImageSource] using an [ImageReader] */
public class ImageReaderImageSource(
    private val imageReader: ImageReaderWrapper,
    private val maxImages: Int,
) : ImageSource {
    public companion object {
        private const val IMAGE_SOURCE_CAPACITY_MARGIN = 2
        public const val IMAGE_SOURCE_CAPACITY: Int =
            IMAGEREADER_MAX_CAPACITY - IMAGE_SOURCE_CAPACITY_MARGIN

        public fun create(imageReader: ImageReaderWrapper): ImageSource {
            // Reduce the maxImages of the ImageSource relative to the ImageReader to ensure there
            // is enough headroom to avoid acquiring too many images that could otherwise stall the
            // camera or trigger IllegalStateExceptions from the underlying ImageReader.
            val maxImages = imageReader.capacity - IMAGE_SOURCE_CAPACITY_MARGIN
            return ImageReaderImageSource(imageReader, maxImages)
        }
    }

    private val state = atomic(State.ACTIVE)
    private val listener = atomic<ImageSourceListener?>(null)
    private val imageCount = atomic(0)

    override val surface: Surface = imageReader.surface

    init {
        imageReader.setOnImageListener(::onImage)
    }

    override fun setListener(listener: ImageSourceListener) {
        this.listener.value = listener
    }

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = imageReader.unwrapAs(type)

    override fun close() {
        // If this is the first time this is invoked, update the state from ACTIVE to CLOSING and
        // flush unused images from the pool. This does *not* immediately close the underlying
        // ImageReader unless there are no outstanding images.
        if (state.compareAndSet(expect = State.ACTIVE, update = State.CLOSING)) {
            flushOrCloseIfEmpty()
        }
    }

    override fun toString(): String = "ImageSource($imageReader)"

    private fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
        // Always increment the imageCount before acquireNextImage
        val currentImageCount = imageCount.incrementAndGet()

        val outputListener = listener.value
        if (outputListener == null) {
            // If there is nowhere to send the image, close it and decrement the imageCount.
            closeAndDecrementImageCount(image)
            return
        }

        if (currentImageCount > maxImages || state.value != State.ACTIVE) {
            // If there are too many images that are currently being held or the ImageSource is in
            // a CLOSING or CLOSED state: close the image, decrement the imageCount, and let the
            // outputListener know that an image was received but that it was dropped (by passing
            // null for the image).
            val outputTimestamp = image.timestamp
            closeAndDecrementImageCount(image)
            outputListener.onImage(streamId, outputId, outputTimestamp, null)
            return
        }

        // Wrap and track the image, and pass it along to the outputListener, which is now
        // responsible for closing the image when it is done with it.
        outputListener.onImage(
            streamId,
            outputId,
            image.timestamp,
            TrackedOutputImage(image, streamId, outputId)
        )
    }

    internal fun closeAndDecrementImageCount(image: ImageWrapper) {
        // This must called *exactly* once for each image that is closed.
        image.close()
        imageCount.decrementAndGet()
        if (state.value != State.ACTIVE) {
            flushOrCloseIfEmpty()
        }
    }

    private fun flushOrCloseIfEmpty() {
        // Assumption: This method is ONLY called if the current state is CLOSING or CLOSED.
        if (state.value == State.CLOSED) {
            return
        }

        // If the imageCount is zero, or has just reached zero, update the state to CLOSED and call
        // close on the imageReader exactly once.
        if (imageCount.value == 0) {
            if (state.compareAndSet(State.CLOSING, State.CLOSED)) {
                imageReader.close()
            }
            return
        }

        // If we reach this point, this ImageSource is CLOSING. Actively flush and discard free
        // buffers to reduce memory usage as individual images are closed.
        imageReader.flush()
    }

    private inner class TrackedOutputImage(
        private val image: ImageWrapper,
        override val streamId: StreamId,
        override val outputId: OutputId
    ) : ImageWrapper by image, OutputImage {
        private val closed = atomic(false)

        override fun close() {
            if (closed.compareAndSet(expect = false, update = true)) {
                // Close underlying image exactly once, and close it *before* decrementImageCount
                // to ensure the imageCount does not get out of sync.
                closeAndDecrementImageCount(image)
            }
        }

        protected fun finalize() {
            // https://kotlinlang.org/docs/java-interop.html#finalize
            // Wrapper images that are no longer reachable should be closed to avoid memory leaks.
            close()
        }
    }

    private enum class State {
        ACTIVE,
        CLOSING,
        CLOSED
    }
}
