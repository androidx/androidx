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

import android.hardware.camera2.MultiResolutionImageReader
import android.media.ImageReader
import android.view.Surface
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.UnsafeWrapper

/**
 * An ImageSource produces images from a CameraStream via an [ImageSourceListener].
 *
 * This interface is an abstraction over [ImageReader] and [MultiResolutionImageReader] that is
 * designed to eliminate several subtle and difficult to avoid pitfalls that can occur when using
 * these classes directly.
 *
 * There are three common problems that occur when using an ImageReader with a camera:
 * 1. Closing the ImageReader before all outstanding Images are closed. This can lead to memory
 *    getting unexpectedly freed or overwritten, leading to corrupted outputs or difficult to
 *    diagnose crashes. Implementations are expected to avoid this problem by waiting to close the
 *    underlying ImageReader (after close has been called) until all images passed to the listener
 *    have also been closed.
 * 2. Acquiring too many images from an ImageReader (or failing to acquire images fast enough) can
 *    either lead to IllegalStateExceptions when attempting to read images out, or it can lead to
 *    stalling the Camera if images are not drained out fast enough. Implementations of this
 *    interface are expected to internally account for the number of outstanding images and to read
 *    and drop images that would otherwise exceed the maximum number of images.
 * 3. Using ImageReader.acquireLatestImage skips an arbitrary number of images. While this is
 *    primarily used to avoid stalling the camera, it also causes problems when attempting to
 *    associate metadata from the camera and can lead to stalls later on. Since this can be solved
 *    via #2, acquireLatestImage is not supported.
 *
 * Implementations are expected to be thread safe, and to associate each image with the [OutputId]
 * it is associated with.
 */
public interface ImageSource : UnsafeWrapper, AutoCloseable {
    /** The graphics surface that the Camera produces images into. */
    public val surface: Surface

    public fun setListener(listener: ImageSourceListener)
}

/** Listener for handling [ImageWrapper]s as they are produced. */
public fun interface ImageSourceListener {
    /**
     * Handle the next image from the [ImageSource]. Implementations *must* close the [image] when
     * they are done with it. Receiving a null [image] indicates the that an image was produced, but
     * that this image source is at capacity and that the image was dropped and closed to avoid
     * stalling the camera.
     */
    public fun onImage(
        streamId: StreamId,
        outputId: OutputId,
        outputTimestamp: Long,
        image: ImageWrapper?
    )
}

/** Provider for creating an [ImageSource] based on an [ImageSourceConfig] */
public fun interface ImageSources {
    public fun createImageSource(
        cameraStream: CameraStream,
        imageSourceConfig: ImageSourceConfig,
    ): ImageSource
}
