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
import static android.graphics.ImageFormat.YUV_420_888;

import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.camera.core.impl.utils.Exif.createFromInputStream;
import static androidx.camera.core.impl.utils.TransformUtils.updateSensorToBufferTransform;

import static java.util.Objects.requireNonNull;

import android.graphics.Rect;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.internal.compat.workaround.JpegMetadataCorrector;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

import com.google.auto.value.AutoValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Converts a {@link ImageProxy} to JPEG bytes.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class Image2JpegBytes implements Operation<Image2JpegBytes.In, Packet<byte[]>> {
    private final JpegMetadataCorrector mJpegMetadataCorrector;

    Image2JpegBytes(@NonNull Quirks quirks) {
        mJpegMetadataCorrector = new JpegMetadataCorrector(quirks);
    }

    @NonNull
    @Override
    public Packet<byte[]> apply(@NonNull Image2JpegBytes.In input) throws ImageCaptureException {
        try {
            int imageFormat = input.getPacket().getFormat();
            switch (imageFormat) {
                case JPEG:
                    return processJpegImage(input);
                case YUV_420_888:
                    return processYuvImage(input);
                default:
                    throw new IllegalArgumentException("Unexpected format: " + imageFormat);
            }
        } finally {
            input.getPacket().getData().close();
        }
    }

    private Packet<byte[]> processJpegImage(@NonNull Image2JpegBytes.In input) {
        Packet<ImageProxy> packet = input.getPacket();
        return Packet.of(
                mJpegMetadataCorrector.jpegImageToJpegByteArray(packet.getData()),
                requireNonNull(packet.getExif()),
                JPEG,
                packet.getSize(),
                packet.getCropRect(),
                packet.getRotationDegrees(),
                packet.getSensorToBufferTransform(),
                packet.getCameraCaptureResult());
    }

    private Packet<byte[]> processYuvImage(@NonNull Image2JpegBytes.In input)
            throws ImageCaptureException {
        Packet<ImageProxy> packet = input.getPacket();
        ImageProxy image = packet.getData();
        Rect cropRect = packet.getCropRect();

        byte[] jpegBytes;
        try {
            jpegBytes = ImageUtil.yuvImageToJpegByteArray(
                    image,
                    cropRect,
                    input.getJpegQuality(),
                    packet.getRotationDegrees());
        } catch (ImageUtil.CodecFailedException e) {
            throw new ImageCaptureException(ImageCapture.ERROR_FILE_IO,
                    "Failed to encode the image to JPEG.", e);
        }

        // Return bytes with a new format, size, and crop rect.
        return Packet.of(
                jpegBytes,
                extractExif(jpegBytes),
                JPEG,
                new Size(cropRect.width(), cropRect.height()),
                new Rect(0, 0, cropRect.width(), cropRect.height()),
                packet.getRotationDegrees(),
                updateSensorToBufferTransform(packet.getSensorToBufferTransform(), cropRect),
                packet.getCameraCaptureResult());
    }

    private static Exif extractExif(@NonNull byte[] jpegBytes) throws ImageCaptureException {
        try {
            return createFromInputStream(new ByteArrayInputStream(jpegBytes));
        } catch (IOException e) {
            throw new ImageCaptureException(ERROR_UNKNOWN,
                    "Failed to extract Exif from YUV-generated JPEG", e);
        }
    }

    @AutoValue
    abstract static class In {

        abstract Packet<ImageProxy> getPacket();

        abstract int getJpegQuality();

        @NonNull
        static In of(@NonNull Packet<ImageProxy> imagePacket, int jpegQuality) {
            return new AutoValue_Image2JpegBytes_In(imagePacket, jpegQuality);
        }
    }
}