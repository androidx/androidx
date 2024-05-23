/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.imagecapture;

import static android.graphics.ImageFormat.JPEG;

import static androidx.camera.core.ImageProcessingUtil.convertJpegBytesToImage;
import static androidx.camera.core.ImageReaderProxys.createIsolatedReader;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.SafeCloseImageReaderProxy;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

/**
 * Converts JPEG bytes to {@link ImageProxy}.
 */
public class JpegBytes2Image implements Operation<Packet<byte[]>, Packet<ImageProxy>> {

    private static final int MAX_IMAGES = 2;

    @NonNull
    @Override
    public Packet<ImageProxy> apply(@NonNull Packet<byte[]> packet) throws ImageCaptureException {
        // TODO: loosen the restriction in ImageCapture#enforceSoftwareJpegConstraints() to enable
        //  the YUV code path on API level <26.
        SafeCloseImageReaderProxy jpegImageReaderProxy = new SafeCloseImageReaderProxy(
                createIsolatedReader(
                        packet.getSize().getWidth(),
                        packet.getSize().getHeight(),
                        JPEG,
                        MAX_IMAGES));
        ImageProxy imageProxy = convertJpegBytesToImage(jpegImageReaderProxy, packet.getData());
        jpegImageReaderProxy.safeClose();
        return Packet.of(
                requireNonNull(imageProxy),
                requireNonNull(packet.getExif()),
                packet.getCropRect(),
                packet.getRotationDegrees(), packet.getSensorToBufferTransform(),
                packet.getCameraCaptureResult());
    }
}
