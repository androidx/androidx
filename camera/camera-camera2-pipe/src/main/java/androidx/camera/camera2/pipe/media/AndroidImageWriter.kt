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

import android.media.Image
import android.media.ImageWriter
import android.os.Build
import android.os.Handler
import android.view.Surface
import androidx.camera.camera2.pipe.InputStreamId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.compat.Api29Compat
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.media.AndroidImageReader.Companion.IMAGEREADER_MAX_CAPACITY
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/** Implements an [ImageWriterWrapper] using an [ImageWriter]. */
public class AndroidImageWriter
private constructor(
    private val imageWriter: ImageWriter,
    private val inputStreamId: InputStreamId,
) : ImageWriterWrapper, ImageWriter.OnImageReleasedListener {
    private val onImageReleasedListener = atomic<ImageWriterWrapper.OnImageReleasedListener?>(null)
    override val maxImages: Int = imageWriter.maxImages

    override val format: Int = imageWriter.format

    override fun queueInputImage(image: ImageWrapper): Boolean {
        return try {
            val unwrappedImage = image.unwrapAs(Image::class)
            if (unwrappedImage == null) {
                Log.warn { "Failed to unwrap image wrapper $image" }
                return false
            }
            imageWriter.queueInputImage(unwrappedImage)
            true
        } catch (e: Throwable) {
            Log.warn {
                "Failed to queue image to $this due to error ${e.message}. " +
                    "Ignoring failure and closing $image"
            }
            image.close()
            false
        }
    }

    override fun dequeueInputImage(): ImageWrapper {
        val image = imageWriter.dequeueInputImage()
        return AndroidImage(image)
    }

    override fun setOnImageReleasedListener(
        onImageReleasedListener: ImageWriterWrapper.OnImageReleasedListener
    ) {
        this.onImageReleasedListener.value = onImageReleasedListener
    }

    override fun onImageReleased(writer: ImageWriter?) {
        onImageReleasedListener.value?.onImageReleased(inputStreamId)
    }

    override fun close(): Unit = imageWriter.close()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            ImageWriter::class -> imageWriter as T?
            else -> null
        }

    override fun toString(): String {
        return "ImageWriter-${StreamFormat(imageWriter.format).name}-$inputStreamId"
    }

    public companion object {
        /**
         * Create and configure a new ImageWriter instance as an [ImageWriter].
         *
         * See [ImageWriter.newInstance] for details.
         */
        public fun create(
            surface: Surface,
            inputStreamId: InputStreamId,
            maxImages: Int,
            format: StreamFormat?,
            handler: Handler,
        ): ImageWriterWrapper {
            require(maxImages > 0) { "Max images ($maxImages) must be > 0" }
            require(maxImages <= IMAGEREADER_MAX_CAPACITY) {
                "Max images for ImageWriters is restricted to " +
                    "$IMAGEREADER_MAX_CAPACITY to prevent overloading downstream " +
                    "consumer components."
            }
            // Create and configure a new ImageWriter
            val imageWriter =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && format != null) {
                    Api29Compat.imageWriterNewInstance(surface, maxImages, format.value)
                } else {
                    if (format != null) {
                        Log.warn {
                            "Ignoring format ($format) for $inputStreamId. Android " +
                                "${Build.VERSION.SDK_INT} does not support creating ImageWriters " +
                                "with formats. This may lead to unexpected behaviors."
                        }
                    }
                    ImageWriter.newInstance(surface, maxImages)
                }

            val androidImageWriter = AndroidImageWriter(imageWriter, inputStreamId)
            imageWriter.setOnImageReleasedListener(androidImageWriter, handler)
            return androidImageWriter
        }
    }
}
