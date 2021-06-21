/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.impl.advanced;

import android.annotation.SuppressLint;

/**
 * A interface to receive and process the upcoming next available Image.
 *
 * <p>Implemented by OEM.
 */
@SuppressLint("UnknownNullness")
public interface ImageProcessorImpl {
    /**
     * The reference count will be decremented when this method returns. If an extension wants
     * to hold onto the image it should increment the reference count in this method and
     * decrement it when the image is no longer needed.
     *
     * <p>If OEM is not closing(decrement) the image fast enough, the imageReference passed
     * in this method might contain null image meaning that the Image was closed to prevent
     * preview from stalling.
     *
     * @param outputConfigId the id of {@link Camera2OutputConfigImpl} which identifies
     *                       corresponding Surface
     * @param timestampNs    the timestamp in nanoseconds associated with this image
     * @param imageReference A reference to the {@link android.media.Image} which might contain
     *                       null if OEM close(decrement) the image too slowly
     * @param physicalCameraId used to distinguish which physical camera id the image comes from
     *                         when the output configuration is
     *                         MultiResolutionImageReaderOutputConfigImpl. It is also set if
     *                         physicalCameraId is set in other Camera2OutputConfigImpl types.
     *
     */
    void onNextImageAvailable(
            int outputConfigId,
            long timestampNs,
            ImageReferenceImpl imageReference,
            String physicalCameraId
            );
}
