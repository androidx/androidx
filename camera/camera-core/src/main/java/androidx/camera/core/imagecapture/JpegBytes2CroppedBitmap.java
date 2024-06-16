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

import static androidx.camera.core.ImageCapture.ERROR_FILE_IO;
import static androidx.camera.core.impl.utils.TransformUtils.updateSensorToBufferTransform;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

import java.io.IOException;

/**
 * Processes a JPEG image and produces a cropped {@link Bitmap} output.
 */
final class JpegBytes2CroppedBitmap implements Operation<Packet<byte[]>, Packet<Bitmap>> {

    @NonNull
    @Override
    public Packet<Bitmap> apply(@NonNull Packet<byte[]> packet) throws ImageCaptureException {
        Rect cropRect = packet.getCropRect();
        Bitmap bitmap = createCroppedBitmap(packet.getData(), cropRect);
        return Packet.of(
                bitmap,
                requireNonNull(packet.getExif()),
                new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                packet.getRotationDegrees(),
                updateSensorToBufferTransform(packet.getSensorToBufferTransform(), cropRect),
                packet.getCameraCaptureResult());
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private Bitmap createCroppedBitmap(@NonNull byte[] jpegBytes, @NonNull Rect cropRect)
            throws ImageCaptureException {
        BitmapRegionDecoder decoder;
        try {
            decoder = BitmapRegionDecoder.newInstance(
                    jpegBytes, 0, jpegBytes.length, false);
        } catch (IOException e) {
            throw new ImageCaptureException(ERROR_FILE_IO, "Failed to decode JPEG.", e);
        }
        return decoder.decodeRegion(cropRect, new BitmapFactory.Options());
    }
}
