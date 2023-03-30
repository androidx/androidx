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
import static androidx.camera.core.impl.utils.Exif.createFromImageProxy;
import static androidx.camera.core.impl.utils.TransformUtils.getRectToRect;
import static androidx.camera.core.impl.utils.TransformUtils.is90or270;
import static androidx.camera.core.impl.utils.TransformUtils.within360;
import static androidx.core.util.Preconditions.checkNotNull;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.internal.CameraCaptureResultImageInfo;
import androidx.camera.core.internal.compat.quirk.ImageCaptureRotationOptionQuirk;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

import java.io.IOException;

/**
 * Converts {@link ProcessingNode} input to a {@link Packet}.
 *
 * <p>This is we fix the metadata of the image, such as rotation and crop rect.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class ProcessingInput2Packet implements
        Operation<ProcessingNode.InputPacket, Packet<ImageProxy>> {

    @NonNull
    @Override
    public Packet<ImageProxy> apply(@NonNull ProcessingNode.InputPacket inputPacket)
            throws ImageCaptureException {
        ImageProxy image = inputPacket.getImageProxy();
        ProcessingRequest request = inputPacket.getProcessingRequest();

        // Extracts Exif data from JPEG.
        Exif exif = null;
        if (image.getFormat() == JPEG) {
            try {
                exif = createFromImageProxy(image);
                // Rewind the buffer after reading.
                image.getPlanes()[0].getBuffer().rewind();
            } catch (IOException e) {
                throw new ImageCaptureException(ERROR_FILE_IO, "Failed to extract EXIF data.", e);
            }
        }
        if (EXIF_ROTATION_AVAILABILITY.shouldUseExifOrientation(image)) {
            checkNotNull(exif, "JPEG image must have exif.");
            return createPacketWithHalRotation(request, exif, image);
        }
        return createPacket(request, exif, image);
    }

    private static Packet<ImageProxy> createPacket(@NonNull ProcessingRequest request,
            @Nullable Exif exif, @NonNull ImageProxy image) {
        return Packet.of(image, exif, request.getCropRect(), request.getRotationDegrees(),
                request.getSensorToBufferTransform(), getCameraCaptureResult(image));
    }

    /**
     * Creates {@link Packet} with possible HAL rotation.
     *
     * <p>When {@link CaptureRequest#JPEG_ORIENTATION} is set, it's possible that the HAL might
     * rotate the image in memory. We need to update the metadata to match the rotated image.
     *
     * <p>This method is based on the assumptions that: 1) the image width/height always match
     * the Surface size, and 2) the exif rotation is correct. Anything else, e.g. the Exif
     * width/height, cannot be trusted.
     *
     * <p>If the Exif rotation is incorrect, we need to add the device to
     * {@link ImageCaptureRotationOptionQuirk} and disable this code path.
     */
    private static Packet<ImageProxy> createPacketWithHalRotation(
            @NonNull ProcessingRequest request, @NonNull Exif exif, @NonNull ImageProxy image) {
        Size surfaceSize = new Size(image.getWidth(), image.getHeight());

        // Clock-wise rotation performed by the HAL.
        int halRotationDegrees = request.getRotationDegrees() - exif.getRotation();

        Size imageSize = getRotatedSize(halRotationDegrees, surfaceSize);

        // The transformation performed by the HAL.
        Matrix halTransform = getRectToRect(
                new RectF(0, 0, surfaceSize.getWidth(), surfaceSize.getHeight()),
                new RectF(0, 0, imageSize.getWidth(), imageSize.getHeight()),
                halRotationDegrees);

        return Packet.of(image, exif, imageSize,
                getUpdatedCropRect(request.getCropRect(), halTransform), exif.getRotation(),
                getUpdatedTransform(request.getSensorToBufferTransform(), halTransform),
                getCameraCaptureResult(image));
    }

    private static CameraCaptureResult getCameraCaptureResult(@NonNull ImageProxy image) {
        return ((CameraCaptureResultImageInfo) image.getImageInfo()).getCameraCaptureResult();
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
        rectF.sort();
        Rect rect = new Rect();
        rectF.round(rect);
        return rect;
    }

    private static Size getRotatedSize(int rotationDegrees, Size size) {
        return is90or270(within360(rotationDegrees))
                ? new Size(/*width=*/size.getHeight(), /*height=*/size.getWidth()) :
                size;
    }
}
