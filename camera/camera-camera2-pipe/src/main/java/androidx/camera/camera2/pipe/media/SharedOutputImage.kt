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
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/**
 * A SharedOutputImage is an [OutputImage] that can be shared between multiple consumers.
 *
 * Use [acquire] or [acquireOrNull] to create a new shared reference to the image. Use
 * [setFinalizer] to get access to the underlying image once all outstanding references have been
 * closed.
 */
public interface SharedOutputImage : OutputImage {
    /**
     * Create a new [SharedOutputImage] copy that can be independently managed or closed. Throws an
     * exception if this reference is already closed.
     */
    public fun acquire(): SharedOutputImage

    /**
     * Create a new [SharedOutputImage] copy that can be independently managed or closed. Returns
     * null if this image has already been finalized.
     */
    public fun acquireOrNull(): SharedOutputImage?

    /**
     * Set a finalizer that is responsible for closing the underlying [OutputImage] when all
     * outstanding [SharedOutputImage] instances have been closed. If multiple finalizers are set,
     * the previous finalizer will receive [Finalizer.finalize] with null to indicate it will not
     * receive the [OutputImage].
     */
    public fun setFinalizer(finalizer: Finalizer<OutputImage>)

    public companion object {

        /** Create a new [SharedOutputImage] from an [OutputImage] */
        public fun from(image: OutputImage): SharedOutputImage {
            if (image is SharedOutputImage) {
                return image.acquire()
            }

            // This attempts to unwrap the OutputImage as a SharedOutputImage. This avoids
            // creating layers of reference counted objects.
            val shared = image.unwrapAs(SharedOutputImage::class)
            if (shared != null) {
                return shared.acquire()
            }

            // By default, create the SharedReference with a ClosingFinalizer, which will simply
            // close the underlying image when it is no longer in use.
            val sharedReference = SharedReference(image, ClosingFinalizer)
            return SharedOutputImageImpl(image, sharedReference)
        }

        private class SharedOutputImageImpl(
            private val outputImage: OutputImage,
            private val sharedReference: SharedReference<OutputImage>
        ) : OutputImage by outputImage, SharedOutputImage {
            private val closed = atomic(false)

            override fun acquire(): SharedOutputImage = checkNotNull(acquireOrNull())

            override fun acquireOrNull(): SharedOutputImage? {
                if (closed.value) {
                    return null
                }
                return sharedReference.acquireOrNull()?.let {
                    SharedOutputImageImpl(outputImage, sharedReference)
                }
            }

            override fun setFinalizer(finalizer: Finalizer<OutputImage>) {
                if (closed.value) {
                    finalizer.finalize(null)
                } else {
                    sharedReference.setFinalizer(finalizer)
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> unwrapAs(type: KClass<T>): T? {
                return if (closed.value) {
                    null
                } else {
                    when (type) {
                        SharedOutputImage::class -> this as T?
                        OutputImage::class -> this as T?
                        ImageWrapper::class -> this as T?

                        // WARNING: Do not allow shared images to be directly unwrapped as a
                        // android.media.Image to avoid circumventing the finalizer protection
                        // methods. This restriction may be removed in the future if there is a
                        // compelling use case.
                        Image::class ->
                            throw UnsupportedOperationException(
                                "Cannot unwrap $this as android.media.Image. Use setFinalizer" +
                                    "instead and close all outstanding references."
                            )
                        else -> null
                    }
                }
            }

            override fun close() {
                if (closed.compareAndSet(expect = false, update = true)) {
                    // WARNING: This method should NOT call super.close(), sharedReference is
                    // responsible for handling the lifetime of the underlying object.

                    // Decrement the shared reference once and only once.
                    sharedReference.decrement()
                }
            }
        }
    }
}
