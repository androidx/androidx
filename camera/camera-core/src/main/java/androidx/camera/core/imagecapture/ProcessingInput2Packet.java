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

import static androidx.camera.core.ImageCapture.ERROR_FILE_IO;
import static androidx.camera.core.imagecapture.ImagePipeline.EXIF_ROTATION_AVAILABILITY;
import static androidx.camera.core.impl.utils.TransformUtils.getRectToRect;
import static androidx.camera.core.impl.utils.TransformUtils.is90or270;
import static androidx.camera.core.impl.utils.TransformUtils.within360;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.processing.Packet;
import androidx.camera.core.processing.Processor;

import java.io.IOException;

/**
 * Converts {@link ProcessingNode} input to a {@link Packet}.
 *
 * <p>This is we fix the metadata of the image, such as rotation and crop rect.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class ProcessingInput2Packet implements
        Processor<ProcessingNode.InputPacket, Packet<ImageProxy>> {

    @NonNull
    @Override
    public Packet<ImageProxy> process(@NonNull ProcessingNode.InputPacket inputPacket)
            throws ImageCaptureException {
        ImageProxy image = inputPacket.getImageProxy();
        ProcessingRequest request = inputPacket.getProcessingRequest();

        Exif exif;
        if (image.getFormat() == JPEG) {
            // Extracts Exif data from JPEG.
            try {
                exif = Exif.createFromImageProxy(image);
                // Rewind the buffer after reading.
                image.getPlanes()[0].getBuffer().rewind();
            } catch (IOException e) {
                throw new ImageCaptureException(ERROR_FILE_IO, "Failed to extract EXIF data.", e);
            }
        } else {
            // TODO: handle YUV image.
            throw new UnsupportedOperationException();
        }

        // Default metadata based on UseCase config.
        Rect cropRect = request.getCropRect();
        Matrix sensorToBuffer = request.getSensorToBufferTransform();
        int rotationDegrees = request.getRotationDegrees();

        // Update metadata if the rotation is sent to the HAL.
        if (EXIF_ROTATION_AVAILABILITY.shouldUseExifOrientation(image)) {
            // If the image's size does not match the Exif size, it might be a vendor bug.
            // Consider adding it to ImageCaptureRotationOptionQuirk.
            checkState(isSizeMatch(exif, image), "Exif size does not match image size.");

            Matrix halTransform = getHalTransform(request.getRotationDegrees(),
                    new Size(exif.getWidth(), exif.getHeight()), exif.getRotation());
            cropRect = getUpdatedCropRect(request.getCropRect(), halTransform);
            sensorToBuffer = getUpdatedTransform(
                    request.getSensorToBufferTransform(), halTransform);
            rotationDegrees = exif.getRotation();
        }

        return Packet.of(image, exif, cropRect, rotationDegrees, sensorToBuffer);
    }

    private static boolean isSizeMatch(@NonNull Exif exif, @NonNull ImageProxy image) {
        return exif.getWidth() == image.getWidth() && exif.getHeight() == image.getHeight();
    }

    /**
     * Updates sensorToSurface transformation.
     */
    @NonNull
    private static Matrix getUpdatedTransform(@NonNull Matrix sensorToSurface,
            @NonNull Matrix halTransform) {
        Matrix sensorToBuffer = new Matrix(sensorToSurface);
        sensorToBuffer.postConcat(halTransform);
        return sensorToBuffer;
    }

    /**
     * Transforms crop rect with the HAL transformation.
     */
    @NonNull
    private static Rect getUpdatedCropRect(@NonNull Rect cropRect, @NonNull Matrix halTransform) {
        RectF rectF = new RectF(cropRect);
        halTransform.mapRect(rectF);
        Rect rect = new Rect();
        rectF.round(rect);
        return rect;
    }

    /**
     * Calculates the transformation applied by the HAL.
     */
    @NonNull
    private static Matrix getHalTransform(
            @IntRange(from = 0, to = 359) int requestRotationDegrees,
            @NonNull Size imageSize,
            @IntRange(from = 0, to = 359) int exifRotationDegrees) {
        int halRotationDegrees = requestRotationDegrees - exifRotationDegrees;
        Size surfaceSize = is90or270(within360(halRotationDegrees))
                ? new Size(/*width=*/imageSize.getHeight(), /*height=*/imageSize.getWidth()) :
                imageSize;
        return getRectToRect(
                new RectF(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight()),
                new RectF(0, 0, imageSize.getWidth(), imageSize.getHeight()),
                halRotationDegrees);
    }
}
