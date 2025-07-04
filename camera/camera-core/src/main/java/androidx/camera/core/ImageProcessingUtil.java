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

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.internal.compat.ImageWriterCompat;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Utility class to convert an {@link Image} from YUV to RGB.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ImageProcessingUtil {

    private static final String TAG = "ImageProcessingUtil";
    public static final String JNI_LIB_NAME = "image_processing_util_jni";
    private static int sImageCount = 0;

    static {
        System.loadLibrary(JNI_LIB_NAME);
    }

    enum Result {
        UNKNOWN,
        SUCCESS,
        ERROR_CONVERSION,  // Native conversion error.
    }

    private ImageProcessingUtil() {
    }

    /**
     * Wraps a JPEG byte array with an {@link Image}.
     *
     * <p>This methods wraps the given byte array with an {@link Image} via the help of the
     * given ImageReader. The image format of the ImageReader has to be JPEG, and the JPEG image
     * size has to match the size of the ImageReader.
     */
    public static @Nullable ImageProxy convertJpegBytesToImage(
            @NonNull ImageReaderProxy jpegImageReaderProxy,
            byte @NonNull [] jpegBytes) {
        Preconditions.checkArgument(jpegImageReaderProxy.getImageFormat() == ImageFormat.JPEG);
        Preconditions.checkNotNull(jpegBytes);

        Surface surface = jpegImageReaderProxy.getSurface();
        Preconditions.checkNotNull(surface);

        if (nativeWriteJpegToSurface(jpegBytes, surface) != 0) {
            Logger.e(TAG, "Failed to enqueue JPEG image.");
            return null;
        }

        final ImageProxy imageProxy = jpegImageReaderProxy.acquireLatestImage();
        if (imageProxy == null) {
            Logger.e(TAG, "Failed to get acquire JPEG image.");
        }
        return imageProxy;
    }


    /**
     * Copies information from a given Bitmap to the address of the ByteBuffer
     *
     * @param bitmap            source bitmap
     * @param byteBuffer        destination ByteBuffer
     * @param bufferStride      the stride of the ByteBuffer
     */
    public static void copyBitmapToByteBuffer(@NonNull Bitmap bitmap,
            @NonNull ByteBuffer byteBuffer, int bufferStride) {
        int bitmapStride = bitmap.getRowBytes();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        nativeCopyBetweenByteBufferAndBitmap(bitmap, byteBuffer, bitmapStride, bufferStride, width,
                height, false);
    }

    /**
     * Copies information from a ByteBuffer to the address of the Bitmap
     *
     * @param bitmap            destination Bitmap
     * @param byteBuffer        source ByteBuffer
     * @param bufferStride      the stride of the ByteBuffer
     *
     */
    public static void copyByteBufferToBitmap(@NonNull Bitmap bitmap,
            @NonNull ByteBuffer byteBuffer, int bufferStride) {
        int bitmapStride = bitmap.getRowBytes();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        nativeCopyBetweenByteBufferAndBitmap(bitmap, byteBuffer, bufferStride, bitmapStride, width,
                height, true);
    }

    /**
     * Writes a JPEG bytes data as an Image into the Surface. Returns true if it succeeds and false
     * otherwise.
     */
    public static boolean writeJpegBytesToSurface(
            @NonNull Surface surface,
            byte @NonNull [] jpegBytes) {
        Preconditions.checkNotNull(jpegBytes);
        Preconditions.checkNotNull(surface);

        if (nativeWriteJpegToSurface(jpegBytes, surface) != 0) {
            Logger.e(TAG, "Failed to enqueue JPEG image.");
            return false;
        }
        return true;
    }

    /**
     * Convert a YUV_420_888 Image to a JPEG bytes data as an Image into the Surface.
     *
     * <p>Returns true if it succeeds and false otherwise.
     */
    public static boolean convertYuvToJpegBytesIntoSurface(
            @NonNull Image image,
            @IntRange(from = 1, to = 100) int jpegQuality,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees,
            @NonNull Surface outputSurface) {
        return convertYuvToJpegBytesIntoSurface(new AndroidImageProxy(image), jpegQuality,
                rotationDegrees, outputSurface);
    }

        /**
         * Convert a YUV_420_888 ImageProxy to a JPEG bytes data as an Image into the Surface.
         *
         * <p>Returns true if it succeeds and false otherwise.
         */
    public static boolean convertYuvToJpegBytesIntoSurface(
            @NonNull ImageProxy imageProxy,
            @IntRange(from = 1, to = 100) int jpegQuality,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees,
            @NonNull Surface outputSurface) {
        try {
            byte[] jpegBytes =
                    ImageUtil.yuvImageToJpegByteArray(
                            imageProxy, null, jpegQuality, rotationDegrees);
            return writeJpegBytesToSurface(outputSurface,
                    jpegBytes);
        } catch (ImageUtil.CodecFailedException e) {
            Logger.e(TAG, "Failed to encode YUV to JPEG", e);
            return false;
        }
    }

    /**
     * Converts image proxy in YUV to RGB.
     *
     * Currently this config supports the devices which generated NV21, NV12, I420 YUV layout,
     * otherwise the input YUV layout will be converted to NV12 first and then to RGBA_8888 as a
     * fallback.
     *
     * @param imageProxy           input image proxy in YUV.
     * @param rgbImageReaderProxy  output image reader proxy in RGB.
     * @param rgbConvertedBuffer   intermediate image buffer for format conversion.
     * @param rotationDegrees      output image rotation degrees.
     * @param onePixelShiftEnabled true if one pixel shift should be applied, otherwise false.
     * @return output image proxy in RGB.
     */
    public static @Nullable ImageProxy convertYUVToRGB(
            @NonNull ImageProxy imageProxy,
            @NonNull ImageReaderProxy rgbImageReaderProxy,
            @Nullable ByteBuffer rgbConvertedBuffer,
            @IntRange(from = 0, to = 359) int rotationDegrees,
            boolean onePixelShiftEnabled) {
        if (!isSupportedYUVFormat(imageProxy)) {
            Logger.e(TAG, "Unsupported format for YUV to RGB");
            return null;
        }
        long startTimeMillis = System.currentTimeMillis();

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
        if (Log.isLoggable("MH", Log.DEBUG)) {
            // The log is used to profile the ImageProcessing performance and only shows in the
            // mobile harness tests.
            Logger.d(TAG, String.format(Locale.US,
                    "Image processing performance profiling, duration: [%d], image count: %d",
                    (System.currentTimeMillis() - startTimeMillis), sImageCount));
            sImageCount++;
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
     * Converts image proxy in YUV to {@link Bitmap}.
     *
     * <p> Different from {@link ImageProcessingUtil#convertYUVToRGB(
     * ImageProxy, ImageReaderProxy, ByteBuffer, int, boolean)}, this function converts to
     * {@link Bitmap} in RGBA directly. If input format is invalid,
     * {@link IllegalArgumentException} will be thrown. If the conversion to bitmap failed,
     * {@link UnsupportedOperationException} will be thrown.
     *
     * @param imageProxy input image proxy in YUV.
     * @return bitmap output bitmap in RGBA.
     */
    public static @NonNull Bitmap convertYUVToBitmap(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Input image format must be YUV_420_888");
        }

        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();
        int srcStrideY = imageProxy.getPlanes()[0].getRowStride();
        int srcStrideU = imageProxy.getPlanes()[1].getRowStride();
        int srcStrideV = imageProxy.getPlanes()[2].getRowStride();
        int srcPixelStrideY = imageProxy.getPlanes()[0].getPixelStride();
        int srcPixelStrideUV = imageProxy.getPlanes()[1].getPixelStride();

        Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(),
                imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
        int bitmapStride = bitmap.getRowBytes();

        int result = nativeConvertAndroid420ToBitmap(
                imageProxy.getPlanes()[0].getBuffer(),
                srcStrideY,
                imageProxy.getPlanes()[1].getBuffer(),
                srcStrideU,
                imageProxy.getPlanes()[2].getBuffer(),
                srcStrideV,
                srcPixelStrideY,
                srcPixelStrideUV,
                bitmap,
                bitmapStride,
                imageWidth,
                imageHeight);
        if (result != 0) {
            throw new UnsupportedOperationException("YUV to RGB conversion failed");
        }
        return bitmap;
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
     * @param imageProxy              input image proxy.
     * @param rotatedImageReaderProxy input image reader proxy.
     * @param rotatedImageWriter      output image writer.
     * @param yRotatedBuffer          intermediate image buffer for y plane rotation.
     * @param uRotatedBuffer          intermediate image buffer for u plane rotation.
     * @param vRotatedBuffer          intermediate image buffer for v plane rotation.
     * @param rotationDegrees         output image rotation degrees.
     * @return rotated image proxy or null if rotation fails or format is not supported.
     */
    public static @Nullable ImageProxy rotateYUV(
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


    /**
     * Rotates YUV image proxy and output the NV21 format image proxy with the delegated byte
     * buffers.
     *
     * @param imageProxy              input image proxy.
     * @param yRotatedBuffer          intermediate image buffer for y plane rotation.
     * @param uRotatedBuffer          intermediate image buffer for u plane rotation.
     * @param vRotatedBuffer          intermediate image buffer for v plane rotation.
     * @param nv21YDelegatedBuffer    delegated image buffer for y plane.
     * @param nv21UVDelegatedBuffer   delegated image buffer for u/v plane.
     * @param rotationDegrees         output image rotation degrees.
     * @return rotated image proxy or null if rotation fails or format is not supported.
     */
    public static @Nullable ImageProxy rotateYUVAndConvertToNV21(
            @NonNull ImageProxy imageProxy,
            @NonNull ByteBuffer yRotatedBuffer,
            @NonNull ByteBuffer uRotatedBuffer,
            @NonNull ByteBuffer vRotatedBuffer,
            @NonNull ByteBuffer nv21YDelegatedBuffer,
            @NonNull ByteBuffer nv21UVDelegatedBuffer,
            @IntRange(from = 0, to = 359) int rotationDegrees) {
        if (!isSupportedYUVFormat(imageProxy)) {
            Logger.e(TAG, "Unsupported format for rotate YUV");
            return null;
        }

        if (!isSupportedRotationDegrees(rotationDegrees)) {
            Logger.e(TAG, "Unsupported rotation degrees for rotate YUV");
            return null;
        }

        // If both rotation and format conversion processing are unnecessary, directly return here.
        if (rotationDegrees == 0 && isNV21FormatImage(imageProxy)) {
            return null;
        }

        int rotatedWidth =
                (rotationDegrees % 180 == 0) ? imageProxy.getWidth() : imageProxy.getHeight();
        int rotatedHeight =
                (rotationDegrees % 180 == 0) ? imageProxy.getHeight() : imageProxy.getWidth();

        ByteBuffer position1ChildByteBuffer = nativeNewDirectByteBuffer(
                nv21UVDelegatedBuffer, 1, nv21UVDelegatedBuffer.capacity());

        int result = nativeRotateYUV(
                imageProxy.getPlanes()[0].getBuffer(),
                imageProxy.getPlanes()[0].getRowStride(),
                imageProxy.getPlanes()[1].getBuffer(),
                imageProxy.getPlanes()[1].getRowStride(),
                imageProxy.getPlanes()[2].getBuffer(),
                imageProxy.getPlanes()[2].getRowStride(),
                imageProxy.getPlanes()[2].getPixelStride(),
                nv21YDelegatedBuffer,
                rotatedWidth,
                1,
                position1ChildByteBuffer,
                rotatedWidth,
                2,
                nv21UVDelegatedBuffer,
                rotatedWidth,
                2,
                yRotatedBuffer,
                uRotatedBuffer,
                vRotatedBuffer,
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                rotationDegrees);

        if (result != 0) {
            Logger.e(TAG, "rotate YUV failure");
            return null;
        }

        // Wraps to NV21ImageProxy to make sure that the returned v plane position is in front of u
        // plane position.
        return new SingleCloseImageProxy(
                new NV21ImageProxy(imageProxy,
                        nv21YDelegatedBuffer,
                        position1ChildByteBuffer,
                        nv21UVDelegatedBuffer,
                        rotatedWidth,
                        rotatedHeight,
                        rotationDegrees));
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

    private static @NonNull Result convertYUVToRGBInternal(
            @NonNull ImageProxy imageProxy,
            @NonNull Surface surface,
            @Nullable ByteBuffer rgbConvertedBuffer,
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

    private static @NonNull Result applyPixelShiftInternal(@NonNull ImageProxy imageProxy) {
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
    private static @Nullable Result rotateYUVInternal(
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

    /**
     * Checks whether the image proxy data is formatted in NV21.
     */
    public static boolean isNV21FormatImage(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getPlanes().length != 3) {
            return false;
        }
        if (imageProxy.getPlanes()[1].getPixelStride() != 2) {
            return false;
        }
        return nativeGetYUVImageVUOff(
                imageProxy.getPlanes()[2].getBuffer(),
                imageProxy.getPlanes()[1].getBuffer()) == -1;
    }

    /**
     * A wrapper to make sure that the returned v plane position (getPlanes()[2]) is in front of
     * u plane position (getPlanes()[1]). So that the following operations can correctly check
     * whether the format is NV12 or NV21 by the plane buffers' pointer positions.
     *
     * <p>The callers need to ensure that the v data is put in the plane with former position and v
     * data is put in the plane with the later position in the associated image proxy.
     */
    private static class NV21ImageProxy extends ForwardingImageProxy {
        private final ImageProxy.PlaneProxy[] mPlanes;
        private final int mWidth;
        private final int mHeight;

        NV21ImageProxy(@NonNull ImageProxy imageProxy,
                @NonNull ByteBuffer delegateBufferY,
                @NonNull ByteBuffer delegateBufferU,
                @NonNull ByteBuffer delegateBufferV,
                int width, int height,
                @IntRange(from = 0, to = 359) int rotatedRotationDegrees) {
            super(imageProxy);
            mPlanes = createPlanes(delegateBufferY, delegateBufferU, delegateBufferV, width);
            mWidth = width;
            mHeight = height;
        }

        @Override
        public int getHeight() {
            return mHeight;
        }

        @Override
        public int getWidth() {
            return mWidth;
        }

        @Override
        public ImageProxy.PlaneProxy @NonNull [] getPlanes() {
            return mPlanes;
        }

        private ImageProxy.PlaneProxy @NonNull [] createPlanes(
                @NonNull ByteBuffer delegateBufferY,
                @NonNull ByteBuffer delegateBufferU,
                @NonNull ByteBuffer delegateBufferV,
                int rowStride
        ) {
            ImageProxy.PlaneProxy[] planes = new ImageProxy.PlaneProxy[3];
            planes[0] = new ImageProxy.PlaneProxy() {
                @Override
                public int getRowStride() {
                    return rowStride;
                }

                @Override
                public int getPixelStride() {
                    return 1;
                }

                @Override
                public @NonNull ByteBuffer getBuffer() {
                    return delegateBufferY;
                }
            };
            planes[1] = new NV21PlaneProxy(delegateBufferU, rowStride);
            planes[2] = new NV21PlaneProxy(delegateBufferV, rowStride);

            return planes;
        }
    }

    private static class NV21PlaneProxy implements ImageProxy.PlaneProxy {
        private final ByteBuffer mByteBuffer;
        private final int mRowStride;

        NV21PlaneProxy(@NonNull ByteBuffer byteBuffer, int rowStride) {
            mByteBuffer = byteBuffer;
            mRowStride = rowStride;
        }

        @Override
        public int getRowStride() {
            return mRowStride;
        }

        @Override
        public int getPixelStride() {
            // Force return pixel stride value 2
            return 2;
        }

        @Override
        public @NonNull ByteBuffer getBuffer() {
            return mByteBuffer;
        }
    }

    private static native int nativeCopyBetweenByteBufferAndBitmap(Bitmap bitmap,
            ByteBuffer byteBuffer,
            int sourceStride, int destinationStride, int width, int height,
            boolean isCopyBufferToBitmap);


    private static native int nativeWriteJpegToSurface(byte @NonNull [] jpegArray,
            @NonNull Surface surface);

    private static native int nativeConvertAndroid420ToABGR(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            @Nullable Surface surface,
            @Nullable ByteBuffer convertedByteBufferRGB,
            int width,
            int height,
            int startOffsetY,
            int startOffsetU,
            int startOffsetV,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees);

    private static native int nativeConvertAndroid420ToBitmap(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            @NonNull Bitmap bitmap,
            int bitmapStride,
            int width,
            int height);

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

    /**
     * Checks the V and U planes' ByteBuffer start position pointer distance.
     *
     * <p>This can be used to determine whether the YUV image is NV12 or NV21 data type.
     */
    public static native int nativeGetYUVImageVUOff(
            @NonNull ByteBuffer srcByteBufferV,
            @NonNull ByteBuffer srcByteBufferU
    );

    /**
     * Creates ByteBuffer from the offset of the input byte buffer.
     */
    public static native @NonNull ByteBuffer nativeNewDirectByteBuffer(
            @NonNull ByteBuffer byteBuffer,
            int offset,
            int capacity
    );
}
