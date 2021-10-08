/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.ExifData;

/** Metadata for an image. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ImageInfo {
    /**
     * Returns all tags stored in the metadata.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    TagBundle getTagBundle();

    /**
     * Returns the timestamp of the metadata.
     *
     * Details on the timestamp depend on the source providing the image, and the method providing
     * the image contains more documentation.
     *
     * @return the timestamp of the image
     */
    long getTimestamp();

    /**
     * Returns the rotation needed to transform the image to the correct orientation.
     *
     * <p> This is a clockwise rotation in degrees that needs to be applied to the image buffer.
     * Note that for images that are in {@link android.graphics.ImageFormat#JPEG} this value will
     * match the rotation defined in the EXIF.
     *
     * <p> The target rotation is set at the time the image capture was requested.
     *
     * <p> The correct orientation of the image is dependent upon the producer of the image. For
     * example, if the {@link ImageProxy} that contains this instance of ImageInfo is produced
     * by an {@link ImageCapture}, then the rotation will be determined by
     * {@link ImageCapture#setTargetRotation(int)} or
     * {@link ImageCapture.Builder#setTargetRotation(int)}.
     *
     * @return The rotation in degrees which will be a value in {0, 90, 180, 270}.
     * @see ImageCapture#setTargetRotation(int)
     * @see ImageCapture.Builder#setTargetRotation(int)
     * @see ImageAnalysis#setTargetRotation(int)
     * @see ImageAnalysis.Builder#setTargetRotation(int)
     */
    // TODO(b/122806727) Need to correctly set EXIF in JPEG images
    int getRotationDegrees();

    /**
     * Adds any stored EXIF information in this ImageInfo into the provided ExifData builder.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void populateExifData(@NonNull ExifData.Builder exifBuilder);
}
