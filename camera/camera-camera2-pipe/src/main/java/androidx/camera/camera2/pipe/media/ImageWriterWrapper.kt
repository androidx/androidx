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

import android.media.ImageWriter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.InputStreamId
import androidx.camera.camera2.pipe.UnsafeWrapper

/**
 * Simplified wrapper for [ImageWriter]-like classes.
 */

@RequiresApi(Build.VERSION_CODES.M)
interface ImageWriterWrapper : UnsafeWrapper, AutoCloseable {

    /**
     * Get the ImageWriter format.
     * @see [ImageWriter.getFormat]
     */
    val format: Int

    /**
     * Get the maximum number of images that can be dequeued from the ImageWriter simultaneously.
     * @see [ImageWriter.getMaxImages]
     */
    val maxImages: Int

    /**
     * Queue an input Image back to ImageWriter for the downstream consumer to access.
     * @see [ImageWriter.queueInputImage]
     */
    fun queueInputImage(image: ImageWrapper)

    /**
     * Dequeue the next available input Image for the application to produce data into.
     * @see [ImageWriter.dequeueInputImage]
     */
    fun dequeueInputImage(): ImageWrapper

    /**
     * Set the [OnImageReleasedListener]. Setting additional listeners will override the previous listener.]
     */
    fun setOnImageReleasedListener(onImageReleasedListener: OnImageReleasedListener)

    /**
     * The OnImageListener adapts the standard [ImageWriter.OnImageReleasedListener] to retrieve
     * images returned to the ImageWriter.
     */
    fun interface OnImageReleasedListener {
        /**
         * Handle the [ImageWrapper] that has been released back to [ImageWriterWrapper].
         */
        fun onImageReleased(inputStreamId: InputStreamId)
    }

    interface Builder {
        fun build(): ImageWriterWrapper
    }
}
