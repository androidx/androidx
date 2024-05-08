/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.camera.core.ImageProcessingUtil.convertYUVToRGB;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageReaderProxys;
import androidx.camera.core.SafeCloseImageReaderProxy;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

import java.nio.ByteBuffer;

/**
 * Convert an {@link ImageProxy} to a {@link Bitmap}.
 *
 * <p>An {@link ImageCaptureException} will be thrown if the conversion failed.
 * Currently it supports only {@link ImageFormat#YUV_420_888} and
 * {@link ImageFormat#JPEG} image. {@link IllegalArgumentException} will be thrown if the input
 * image format is not supported.
 */
public class Image2Bitmap implements
        Operation<Packet<ImageProxy>, Bitmap> {
    @NonNull
    @Override
    public Bitmap apply(@NonNull Packet<ImageProxy> imageProxyPacket)
            throws ImageCaptureException {
        Bitmap result;
        SafeCloseImageReaderProxy rgbImageReader = null;
        try {
            if (imageProxyPacket.getFormat() == ImageFormat.YUV_420_888) {
                ImageProxy yuvImage = imageProxyPacket.getData();
                boolean needFlip = (imageProxyPacket.getRotationDegrees() % 180) != 0;
                int tempImageReaderWidth = needFlip ? yuvImage.getHeight() : yuvImage.getWidth();
                int tempImageReaderHeight = needFlip ? yuvImage.getWidth() : yuvImage.getHeight();

                // TODO(b/313548792): remove the usage of ImageReader by creating a version of
                //  convertYUVToBitmap that also rotates the output.
                rgbImageReader = new SafeCloseImageReaderProxy(
                        ImageReaderProxys.createIsolatedReader(
                                tempImageReaderWidth, tempImageReaderHeight,
                                PixelFormat.RGBA_8888, 2)
                );

                ByteBuffer rgbConvertedBuffer = ByteBuffer.allocateDirect(
                        yuvImage.getWidth() * yuvImage.getHeight() * 4);
                ImageProxy imageProxyRGB = convertYUVToRGB(
                        yuvImage,
                        rgbImageReader,
                        rgbConvertedBuffer,
                        imageProxyPacket.getRotationDegrees(),
                        /* onePixelShiftEnabled */false);
                yuvImage.close();
                if (imageProxyRGB == null) {
                    throw new ImageCaptureException(ERROR_UNKNOWN, "Can't covert YUV to RGB", null);
                }
                Bitmap bitmap = ImageUtil.createBitmapFromImageProxy(imageProxyRGB);
                imageProxyRGB.close();
                result = bitmap;
            } else if (imageProxyPacket.getFormat() == ImageFormat.JPEG) {
                ImageProxy jpegImage = imageProxyPacket.getData();
                Bitmap bitmap = ImageUtil.createBitmapFromImageProxy(jpegImage);
                jpegImage.close();
                result = ImageUtil.rotateBitmap(bitmap, imageProxyPacket.getRotationDegrees());
            } else {
                throw new IllegalArgumentException("Invalid postview image format : "
                        + imageProxyPacket.getFormat());
            }
            return result;
        } catch (UnsupportedOperationException e) {
            String format = imageProxyPacket.getFormat() == ImageFormat.YUV_420_888
                    ? "YUV" : "JPEG";
            throw new ImageCaptureException(ERROR_UNKNOWN,
                    "Can't convert " + format + " to bitmap", e);
        } finally {
            if (rgbImageReader != null) {
                rgbImageReader.close();
            }
        }
    }
}
