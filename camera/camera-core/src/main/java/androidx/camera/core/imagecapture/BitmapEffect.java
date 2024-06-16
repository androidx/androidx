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

import static androidx.camera.core.internal.utils.ImageUtil.createBitmapFromPlane;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProcessor;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.processing.ImageProcessorRequest;
import androidx.camera.core.processing.InternalImageProcessor;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

/**
 * Applies effect to a {@link Bitmap} and gets a {@link Bitmap} in return.
 *
 * <p>The output packet will inherited the input packet's metadata, such as the crop rect. The
 * metadata of the image returned from the effect is ignored.
 */
public class BitmapEffect implements Operation<Packet<Bitmap>, Packet<Bitmap>> {

    private final InternalImageProcessor mProcessor;

    BitmapEffect(InternalImageProcessor imageProcessor) {
        mProcessor = imageProcessor;
    }

    @NonNull
    @Override
    public Packet<Bitmap> apply(@NonNull Packet<Bitmap> packet) throws ImageCaptureException {
        // Process the frame.
        ImageProcessor.Response response = mProcessor.safeProcess(new ImageProcessorRequest(
                new RgbaImageProxy(packet),
                PixelFormat.RGBA_8888));

        // Restored it back to a Bitmap packet.
        ImageProxy imageOut = response.getOutputImage();
        Bitmap bitmapOut = createBitmapFromPlane(
                requireNonNull(imageOut).getPlanes(),
                imageOut.getWidth(),
                imageOut.getHeight());
        return Packet.of(
                bitmapOut,
                requireNonNull(packet.getExif()),
                packet.getCropRect(),
                packet.getRotationDegrees(),
                packet.getSensorToBufferTransform(),
                packet.getCameraCaptureResult()
        );
    }
}
