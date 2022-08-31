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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.processing.Packet;
import androidx.camera.core.processing.Processor;

import java.io.IOException;

/**
 * Processes a JPEG image and produces a cropped {@link Bitmap} output.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class JpegBytes2CroppedBitmap implements Processor<Packet<byte[]>, Packet<Bitmap>> {

    @NonNull
    @Override
    public Packet<Bitmap> process(@NonNull Packet<byte[]> packet) throws ImageCaptureException {
        Rect cropRect = packet.getCropRect();
        Bitmap bitmap = createCroppedBitmap(packet.getData(), cropRect);
        return Packet.of(
                bitmap,
                packet.getExif(),
                packet.getCameraCaptureResult(),
                ImageFormat.FLEX_RGBA_8888,
                new Size(bitmap.getWidth(), bitmap.getHeight()),
                new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                packet.getRotationDegrees(),
                createSensorToBufferTransform(packet.getSensorToBufferTransform(), cropRect));
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

    @NonNull
    private Matrix createSensorToBufferTransform(
            @NonNull Matrix original,
            @NonNull Rect cropRect) {
        Matrix matrix = new Matrix(original);
        matrix.postTranslate(-cropRect.left, -cropRect.top);
        return matrix;
    }
}
