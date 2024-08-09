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

package androidx.camera.camera2.pipe.media

import android.hardware.camera2.MultiResolutionImageReader
import android.media.ImageReader
import android.view.Surface
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.UnsafeWrapper

/** Simplified wrapper for [ImageReader]-like classes. */
public interface ImageReaderWrapper : UnsafeWrapper, AutoCloseable {
    /**
     * Get a Surface that can be used to produce images for this ImageReader.
     *
     * @see [ImageReader.getSurface]
     */
    public val surface: Surface

    /**
     * Get the maximum number of images that can be produced before stalling or throwing exceptions.
     *
     * @see [ImageReader.acquireNextImage]
     * @see [ImageReader.acquireLatestImage]
     */
    public val capacity: Int

    /**
     * Set the [OnImageListener]. Setting additional listeners will override the previous listener.]
     */
    public fun setOnImageListener(onImageListener: OnImageListener)

    /**
     * Discard free buffers from the internal memory pool.
     *
     * @see [ImageReader.discardFreeBuffers]
     * @see [MultiResolutionImageReader.flush]
     */
    public fun flush()

    /**
     * The OnNextImageListener adapts the standard [ImageReader.OnImageAvailableListener] to push
     * images into a consumer. This consumer is responsible for processing and/or closing images
     * when they are no longer needed.
     */
    public fun interface OnImageListener {
        /**
         * Handle the next [ImageWrapper] from an [ImageReaderWrapper]. Implementations are
         * responsible for closing images when they are no longer in use.
         *
         * [ImageWrapper.timestamp] is not guaranteed to be in order when used with a multi-sensor
         * camera system, but should *usually* be in order
         */
        public fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper)
    }
}
