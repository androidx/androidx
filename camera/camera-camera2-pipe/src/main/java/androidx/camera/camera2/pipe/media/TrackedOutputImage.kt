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
import kotlinx.atomicfu.atomic

/**
 * Immutable value class to encode external use count (bits 1..31) and closed state (bit 0) into a
 * single primitive int. This allows the state transitions can be performed atomically without
 * locks.
 */
@JvmInline
internal value class TrackedOutputImageState(val value: Int = 0) {
    val externalUseCount: Int
        get() = value ushr 1

    val isClosed: Boolean
        get() = (value and 1) != 0

    val isEvictable: Boolean
        get() = value == 0

    fun withClosed(): TrackedOutputImageState = TrackedOutputImageState(value or 1)

    fun withIncrementedUse(count: Int = 1): TrackedOutputImageState =
        TrackedOutputImageState(value + (count shl 1))

    fun withDecrementedUse(): TrackedOutputImageState = TrackedOutputImageState(value - 2)
}

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

    // TrackedOutputImageState to track external use count and closed state.
    // Initial state is 0: closed = false, externalUseCount = 0, isEvictable = true.
    private val state = atomic(TrackedOutputImageState(0).value)

    init {
        // Note - right now we are tracking the evictable bytes for Image(s), we should also
        // consider keeping track of the count of evictable image(s) for a particular stream
        // irrespective of the byte size of the image.
        if (bytesPerImage > 0) {
            // Acquire memory budget for this image.
            memoryEstimator.incrementUsage(bytesPerImage)
        }
        // A newly created image with 0 external uses and not closed is evictable.
        onEvictableStateChanged(isEvictable = true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when (type) {
            TrackedOutputImage::class.java -> this as T?
            OutputImage::class.java -> this as T?
            ImageWrapper::class.java -> this as T?
            else -> image.unwrapAs(type)
        }

    fun incrementExternalUse() = updateState { it.withIncrementedUse(1) }

    fun decrementExternalUse() = updateState { it.withDecrementedUse() }

    fun addExternalUse(count: Int) {
        if (count == 0) {
            return
        }
        updateState { it.withIncrementedUse(count) }
    }

    private inline fun updateState(
        transform: (TrackedOutputImageState) -> TrackedOutputImageState
    ) {
        while (true) {
            val currentStateValue = state.value
            val currentState = TrackedOutputImageState(currentStateValue)
            val nextState = transform(currentState)

            if (currentState == nextState) {
                return
            }

            if (state.compareAndSet(currentStateValue, nextState.value)) {
                if (currentState.isEvictable != nextState.isEvictable) {
                    onEvictableStateChanged(nextState.isEvictable)
                }
                return
            }
        }
    }

    private fun onEvictableStateChanged(isEvictable: Boolean) {
        if (isEvictable) {
            if (bytesPerImage > 0) {
                memoryEstimator.updateEvictable(bytesPerImage)
            }
        } else {
            if (bytesPerImage > 0) {
                memoryEstimator.updateEvictable(-bytesPerImage)
            }
        }
    }

    override fun close() {
        var becameClosed = false
        updateState { currentState ->
            if (currentState.isClosed) {
                becameClosed = false
                currentState
            } else {
                becameClosed = true
                currentState.withClosed()
            }
        }
        if (becameClosed) {
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
