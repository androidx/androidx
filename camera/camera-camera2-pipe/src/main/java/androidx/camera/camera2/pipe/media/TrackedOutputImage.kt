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

package androidx.camera.camera2.pipe.media

import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.OutputImage.Companion.toLogString
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/**
 * Base image type for images coming out of [ImageReaderImageSource]. It additionally tracks the
 * external usage of this image and updates the evictable math at appropriate state changes.
 */
internal class TrackedOutputImage(
    private val imageReaderImageSource: ImageReaderImageSource,
    private val image: ImageWrapper,
    override val streamId: StreamId,
    override val outputId: OutputId,
    private val memoryEstimator: MemoryEstimator,
) : ImageWrapper by image, OutputImage {
    val bytesPerImage =
        StreamFormat.bytesPerImage(StreamFormat(image.format), image.width, image.height)

    private val closed = atomic(false)
    private val externalUseCount = atomic(0)
    private val isEvictable = atomic(false)

    init {
        // Note - right now we are tracking the evictable bytes for Image(s), we should also
        // consider keeping track of the count of evictable image(s) for a particular stream
        // irrespective of the byte size of the image.
        if (bytesPerImage > 0) {
            // Acquire memory budget for this image.
            memoryEstimator.incrementUsage(bytesPerImage)
        }
        // Evaluate evictable state on creation
        updateEvictableState()
    }

    @Suppress("UNCHECKED_CAST")
    @Deprecated("Use the reified unwrapAs<T>() extension function instead")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? = unwrapAs(type.java)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when (type) {
            TrackedOutputImage::class.java -> this as T?
            OutputImage::class.java -> this as T?
            ImageWrapper::class.java -> this as T?
            else -> image.unwrapAs(type)
        }

    fun incrementExternalUse() {
        externalUseCount.incrementAndGet()
        updateEvictableState()
    }

    fun decrementExternalUse() {
        externalUseCount.decrementAndGet()
        updateEvictableState()
    }

    fun addExternalUse(count: Int) {
        if (count == 0) {
            return
        }
        externalUseCount.getAndAdd(count)
        updateEvictableState()
    }

    private fun updateEvictableState() {
        while (true) {
            // Compute if it is evictable based on the current external use count and closed state.
            val shouldBeEvictable = externalUseCount.value == 0 && !closed.value
            val currentlyEvictable = isEvictable.value
            if (shouldBeEvictable == currentlyEvictable) {
                return
            }

            // Attempt to update the evictable state.
            if (
                isEvictable.compareAndSet(expect = currentlyEvictable, update = shouldBeEvictable)
            ) {
                if (bytesPerImage > 0) {
                    if (shouldBeEvictable) {
                        memoryEstimator.updateEvictable(bytesPerImage)
                    } else {
                        memoryEstimator.updateEvictable(-bytesPerImage)
                    }
                }
                return
            }
            // Try again if the update was not successful due to any concurrent updates.
        }
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            // Try to update the evictable math.
            updateEvictableState()

            // Close underlying image exactly once, and close it *before* decrementImageCount
            // to ensure the imageCount does not get out of sync.
            imageReaderImageSource.closeAndDecrementImageCount(image)

            // Release the memory budget back to the pool.
            if (bytesPerImage > 0) {
                memoryEstimator.decrementUsage(bytesPerImage)
            }
        }
    }

    protected fun finalize() {
        // https://kotlinlang.org/docs/java-interop.html#finalize
        // Wrapper images that are no longer reachable should be closed to avoid memory leaks.
        close()
    }

    override fun toString(): String = this.toLogString()
}
