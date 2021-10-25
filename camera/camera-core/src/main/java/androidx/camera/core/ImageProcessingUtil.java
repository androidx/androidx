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

package androidx.camera.core;

import static androidx.camera.core.ImageProcessingUtil.Result.ERROR_CONVERSION;
import static androidx.camera.core.ImageProcessingUtil.Result.SUCCESS;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.internal.compat.ImageWriterCompat;

import java.nio.ByteBuffer;

/** Utility class to convert an {@link Image} from YUV to RGB. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ImageProcessingUtil {

    private static final String TAG = "ImageProcessingUtil";

    static {
        System.loadLibrary("image_processing_util_jni");
    }

    enum Result {
        UNKNOWN,
        SUCCESS,
        ERROR_CONVERSION,  // Native conversion error.
    }

    private ImageProcessingUtil() {
    }

    /**
     * Converts image proxy in YUV to RGB.
     *
     * Currently this config supports the devices which generated NV21, NV12, I420 YUV layout,
     * otherwise the input YUV layout will be converted to NV12 first and then to RGBA_8888 as a
     * fallback.
     *
     * @param imageProxy input image proxy in YUV.
     * @param rgbImageReaderProxy output image reader proxy in RGB.
     * @param rgbConvertedBuffer intermediate image buffer for format conversion.
     * @param rotationDegrees output image rotation degrees.
     * @param onePixelShiftEnabled true if one pixel shift should be applied, otherwise false.
     * @return output image proxy in RGB.
     */
    @Nullable
    public static ImageProxy convertYUVToRGB(
            @NonNull ImageProxy imageProxy,
            @NonNull ImageReaderProxy rgbImageReaderProxy,
            @NonNull ByteBuffer rgbConvertedBuffer,
            @IntRange(from = 0, to = 359) int rotationDegrees,
            boolean onePixelShiftEnabled) {
        if (!isSupportedYUVFormat(imageProxy)) {
            Logger.e(TAG, "Unsupported format for YUV to RGB");
            return null;
        }

        if (!isSupportedRotationDegrees(rotationDegrees)) {
            Logger.e(TAG, "Unsupported rotation degrees for rotate RGB");
            return null;
        }

        // Convert YUV To RGB and write data to surface
        Result result = convertYUVToRGBInternal(
                imageProxy,
                rgbImageReaderProxy.getSurface(),
                rgbConvertedBuffer,
                rotationDegrees,
                onePixelShiftEnabled);

        if (result == ERROR_CONVERSION) {
            Logger.e(TAG, "YUV to RGB conversion failure");
            return null;
        }

        // Retrieve ImageProxy in RGB
        final ImageProxy rgbImageProxy = rgbImageReaderProxy.acquireLatestImage();
        if (rgbImageProxy == null) {
            Logger.e(TAG, "YUV to RGB acquireLatestImage failure");
            return null;
        }

        // Close ImageProxy for the next image
        SingleCloseImageProxy wrappedRgbImageProxy = new SingleCloseImageProxy(rgbImageProxy);
        wrappedRgbImageProxy.addOnImageCloseListener(image -> {
            // Close YUV image proxy when RGB image proxy is closed by app.
            if (rgbImageProxy != null && imageProxy != null) {
                imageProxy.close();
            }
        });
        return wrappedRgbImageProxy;
    }

    /**
     * Applies one pixel shift workaround for YUV image
     *
     * @param imageProxy input image proxy in YUV.
     * @return true if one pixel shift is applied successfully, otherwise false.
     */
    public static boolean applyPixelShiftForYUV(@NonNull ImageProxy imageProxy) {
        if (!isSupportedYUVFormat(imageProxy)) {
            Logger.e(TAG, "Unsupported format for YUV to RGB");
            return false;
        }

        Result result = applyPixelShiftInternal(imageProxy);

        if (result == ERROR_CONVERSION) {
            Logger.e(TAG, "One pixel shift for YUV failure");
            return false;
        }
        return true;
    }

    /**
     * Rotates YUV image proxy.
     *
     * @param imageProxy input image proxy.
     * @param rotatedImageReaderProxy input image reader proxy.
     * @param rotatedImageWriter output image writer.
     * @param yRotatedBuffer intermediate image buffer for y plane rotation.
     * @param uRotatedBuffer intermediate image buffer for u plane rotation.
     * @param vRotatedBuffer intermediate image buffer for v plane rotation.
     * @param rotationDegrees output image rotation degrees.
     * @return rotated image proxy or null if rotation fails or format is not supported.
     */
    @Nullable
    public static ImageProxy rotateYUV(
            @NonNull ImageProxy imageProxy,
            @NonNull ImageReaderProxy rotatedImageReaderProxy,
            @NonNull ImageWriter rotatedImageWriter,
            @NonNull ByteBuffer yRotatedBuffer,
            @NonNull ByteBuffer uRotatedBuffer,
            @NonNull ByteBuffer vRotatedBuffer,
            @IntRange(from = 0, to = 359) int rotationDegrees) {
        if (!isSupportedYUVFormat(imageProxy)) {
            Logger.e(TAG, "Unsupported format for rotate YUV");
            return null;
        }

        if (!isSupportedRotationDegrees(rotationDegrees)) {
            Logger.e(TAG, "Unsupported rotation degrees for rotate YUV");
            return null;
        }

        Result result = ERROR_CONVERSION;

        // YUV rotation is checking non-zero rotation degrees in java layer to avoid unnecessary
        // overhead, while RGB rotation is checking in c++ layer.
        if (Build.VERSION.SDK_INT >= 23 && rotationDegrees > 0) {
            result = rotateYUVInternal(
                    imageProxy,
                    rotatedImageWriter,
                    yRotatedBuffer,
                    uRotatedBuffer,
                    vRotatedBuffer,
                    rotationDegrees);
        }

        if (result == ERROR_CONVERSION) {
            Logger.e(TAG, "rotate YUV failure");
            return null;
        }

        // Retrieve ImageProxy in rotated YUV
        ImageProxy rotatedImageProxy = rotatedImageReaderProxy.acquireLatestImage();
        if (rotatedImageProxy == null) {
            Logger.e(TAG, "YUV rotation acquireLatestImage failure");
            return null;
        }

        SingleCloseImageProxy wrappedRotatedImageProxy = new SingleCloseImageProxy(
                rotatedImageProxy);
        wrappedRotatedImageProxy.addOnImageCloseListener(image -> {
            // Close original YUV image proxy when rotated YUV image is closed by app.
            if (rotatedImageProxy != null && imageProxy != null) {
                imageProxy.close();
            }
        });

        return wrappedRotatedImageProxy;
    }

    private static boolean isSupportedYUVFormat(@NonNull ImageProxy imageProxy) {
        return imageProxy.getFormat() == ImageFormat.YUV_420_888
                && imageProxy.getPlanes().length == 3;
    }

    private static boolean isSupportedRotationDegrees(
            @IntRange(from = 0, to = 359) int rotationDegrees) {
        return rotationDegrees == 0
                || rotationDegrees == 90
                || rotationDegrees == 180
                || rotationDegrees == 270;
    }

    @NonNull
    private static Result convertYUVToRGBInternal(
            @NonNull ImageProxy imageProxy,
            @NonNull Surface surface,
            @NonNull ByteBuffer rgbConvertedBuffer,
            @ImageOutputConfig.RotationDegreesValue int rotation,
            boolean onePixelShiftEnabled) {
        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();
        int srcStrideY = imageProxy.getPlanes()[0].getRowStride();
        int srcStrideU = imageProxy.getPlanes()[1].getRowStride();
        int srcStrideV = imageProxy.getPlanes()[2].getRowStride();
        int srcPixelStrideY = imageProxy.getPlanes()[0].getPixelStride();
        int srcPixelStrideUV = imageProxy.getPlanes()[1].getPixelStride();

        int startOffsetY = onePixelShiftEnabled ? srcPixelStrideY : 0;
        int startOffsetU = onePixelShiftEnabled ? srcPixelStrideUV : 0;
        int startOffsetV = onePixelShiftEnabled ? srcPixelStrideUV : 0;

        int result = nativeConvertAndroid420ToABGR(
                imageProxy.getPlanes()[0].getBuffer(),
                srcStrideY,
                imageProxy.getPlanes()[1].getBuffer(),
                srcStrideU,
                imageProxy.getPlanes()[2].getBuffer(),
                srcStrideV,
                srcPixelStrideY,
                srcPixelStrideUV,
                surface,
                rgbConvertedBuffer,
                imageWidth,
                imageHeight,
                startOffsetY,
                startOffsetU,
                startOffsetV,
                rotation);
        if (result != 0) {
            return ERROR_CONVERSION;
        }
        return SUCCESS;
    }

    @NonNull
    private static Result applyPixelShiftInternal(@NonNull ImageProxy imageProxy) {
        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();
        int srcStrideY = imageProxy.getPlanes()[0].getRowStride();
        int srcStrideU = imageProxy.getPlanes()[1].getRowStride();
        int srcStrideV = imageProxy.getPlanes()[2].getRowStride();
        int srcPixelStrideY = imageProxy.getPlanes()[0].getPixelStride();
        int srcPixelStrideUV = imageProxy.getPlanes()[1].getPixelStride();

        int startOffsetY = srcPixelStrideY;
        int startOffsetU = srcPixelStrideUV;
        int startOffsetV = srcPixelStrideUV;

        int result = nativeShiftPixel(
                imageProxy.getPlanes()[0].getBuffer(),
                srcStrideY,
                imageProxy.getPlanes()[1].getBuffer(),
                srcStrideU,
                imageProxy.getPlanes()[2].getBuffer(),
                srcStrideV,
                srcPixelStrideY,
                srcPixelStrideUV,
                imageWidth,
                imageHeight,
                startOffsetY,
                startOffsetU,
                startOffsetV);
        if (result != 0) {
            return ERROR_CONVERSION;
        }
        return SUCCESS;
    }

    @RequiresApi(23)
    @Nullable
    private static Result rotateYUVInternal(
            @NonNull ImageProxy imageProxy,
            @NonNull ImageWriter rotatedImageWriter,
            @NonNull ByteBuffer yRotatedBuffer,
            @NonNull ByteBuffer uRotatedBuffer,
            @NonNull ByteBuffer vRotatedBuffer,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees) {
        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();
        int srcStrideY = imageProxy.getPlanes()[0].getRowStride();
        int srcStrideU = imageProxy.getPlanes()[1].getRowStride();
        int srcStrideV = imageProxy.getPlanes()[2].getRowStride();
        int srcPixelStrideUV = imageProxy.getPlanes()[1].getPixelStride();

        Image rotatedImage = ImageWriterCompat.dequeueInputImage(rotatedImageWriter);
        if (rotatedImage == null) {
            return ERROR_CONVERSION;
        }

        int result = nativeRotateYUV(
                imageProxy.getPlanes()[0].getBuffer(),
                srcStrideY,
                imageProxy.getPlanes()[1].getBuffer(),
                srcStrideU,
                imageProxy.getPlanes()[2].getBuffer(),
                srcStrideV,
                srcPixelStrideUV,
                rotatedImage.getPlanes()[0].getBuffer(),
                rotatedImage.getPlanes()[0].getRowStride(),
                rotatedImage.getPlanes()[0].getPixelStride(),
                rotatedImage.getPlanes()[1].getBuffer(),
                rotatedImage.getPlanes()[1].getRowStride(),
                rotatedImage.getPlanes()[1].getPixelStride(),
                rotatedImage.getPlanes()[2].getBuffer(),
                rotatedImage.getPlanes()[2].getRowStride(),
                rotatedImage.getPlanes()[2].getPixelStride(),
                yRotatedBuffer,
                uRotatedBuffer,
                vRotatedBuffer,
                imageWidth,
                imageHeight,
                rotationDegrees);

        if (result != 0) {
            return ERROR_CONVERSION;
        }

        ImageWriterCompat.queueInputImage(rotatedImageWriter, rotatedImage);
        return SUCCESS;
    }

    private static native int nativeConvertAndroid420ToABGR(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            @NonNull Surface surface,
            @NonNull ByteBuffer convertedByteBufferRGB,
            int width,
            int height,
            int startOffsetY,
            int startOffsetU,
            int startOffsetV,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees);

    private static native int nativeShiftPixel(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            int width,
            int height,
            int startOffsetY,
            int startOffsetU,
            int startOffsetV);

    private static native int nativeRotateYUV(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideUV,
            @NonNull ByteBuffer dstByteBufferY,
            int dstStrideY,
            int dstPixelStrideY,
            @NonNull ByteBuffer dstByteBufferU,
            int dstStrideU,
            int dstPixelStrideU,
            @NonNull ByteBuffer dstByteBufferV,
            int dstStrideV,
            int dstPixelStrideV,
            @NonNull ByteBuffer rotatedByteBufferY,
            @NonNull ByteBuffer rotatedByteBufferU,
            @NonNull ByteBuffer rotatedByteBufferV,
            int width,
            int height,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees);
}
