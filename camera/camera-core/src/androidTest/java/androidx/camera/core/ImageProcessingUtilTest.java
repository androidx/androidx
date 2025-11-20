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

import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_NV21;
import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888;
import static androidx.camera.core.ImageProcessingUtil.convertJpegBytesToImage;
import static androidx.camera.core.ImageProcessingUtil.rotateYUV;
import static androidx.camera.core.ImageProcessingUtil.rotateYUVAndConvertToNV21;
import static androidx.camera.core.ImageProcessingUtil.writeJpegBytesToSurface;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA;
import static androidx.camera.testing.impl.IgnoreProblematicDeviceRule.Companion;
import static androidx.camera.testing.impl.ImageProxyUtil.createYUV420ImagePlanes;
import static androidx.camera.testing.impl.ImageProxyUtil.getDefaultYuvFormatPlaneDataType;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assume.assumeFalse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.ImageWriter;
import android.os.Build;

import androidx.annotation.IntRange;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.testing.impl.ImageProxyUtil;
import androidx.camera.testing.impl.TestImageUtil;
import androidx.camera.testing.impl.fakes.FakeImageInfo;
import androidx.camera.testing.impl.fakes.FakeImageProxy;
import androidx.core.math.MathUtils;
import androidx.core.util.Preconditions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Unit test for {@link ImageProcessingUtil}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ImageProcessingUtilTest {

    private static final int WIDTH = 8;
    private static final int HEIGHT = 4;
    private static final int MAX_IMAGES = 4;
    private static final int JPEG_ENCODE_ERROR_TOLERANCE = 3;

    private static final int PADDING_BYTES = 16;

    private ByteBuffer mRgbConvertedBuffer;
    private ByteBuffer mYRotatedBuffer;
    private ByteBuffer mURotatedBuffer;
    private ByteBuffer mVRotatedBuffer;
    private ByteBuffer mNV21YDelegatedByteBuffer;
    private ByteBuffer mNV21UVDelegatedByteBuffer;
    private static final int[] YUV_WHITE_STUDIO_SWING_BT601 = {/*y=*/235, /*u=*/128, /*v=*/128};
    private static final int[] YUV_BLACK_STUDIO_SWING_BT601 = {/*y=*/16, /*u=*/128, /*v=*/128};
    private static final int[] YUV_BLUE_STUDIO_SWING_BT601 = {/*y=*/16, /*u=*/240, /*v=*/128};
    private static final int[] YUV_RED_STUDIO_SWING_BT601 = {/*y=*/16, /*u=*/128, /*v=*/240};

    private FakeImageProxy mYUVImageProxy;
    @ImageProxyUtil.YuvFormatPlaneDataType
    private int mYUVImageProxyPlaneDataType;
    private SafeCloseImageReaderProxy mRGBImageReaderProxy;
    private SafeCloseImageReaderProxy mRotatedRGBImageReaderProxy;
    private SafeCloseImageReaderProxy mRotatedYUVImageReaderProxy;
    private SafeCloseImageReaderProxy mJpegImageReaderProxy;

    @Before
    public void setUp() {
        mYUVImageProxyPlaneDataType = getDefaultYuvFormatPlaneDataType(RESOLUTION_VGA.getWidth(),
                RESOLUTION_VGA.getHeight());
        createTestResources(WIDTH, HEIGHT, 90);
    }

    @After
    public void tearDown() {
        closeTestResources();
    }

    private void createTestResources(int width, int height, int rotation) {
        mYUVImageProxy = TestImageUtil.createYuvFakeImageProxy(new FakeImageInfo(), width, height,
                mYUVImageProxyPlaneDataType, true);
        mYUVImageProxy.setWidth(width);
        mYUVImageProxy.setHeight(height);
        mYUVImageProxy.setFormat(ImageFormat.YUV_420_888);

        boolean flipWh = rotation % 180 != 0;

        // rgb image reader proxy should not be mocked for JNI native code
        mRGBImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        width,
                        height,
                        PixelFormat.RGBA_8888,
                        MAX_IMAGES));

        // rotated image reader proxy with width and height flipped
        mRotatedRGBImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        flipWh ? height : width,
                        flipWh ? width : height,
                        PixelFormat.RGBA_8888,
                        MAX_IMAGES));

        mRotatedYUVImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        flipWh ? height : width,
                        flipWh ? width : height,
                        ImageFormat.YUV_420_888,
                        MAX_IMAGES));

        mJpegImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        flipWh ? height : width,
                        flipWh ? width : height,
                        ImageFormat.JPEG,
                        MAX_IMAGES));

        mRgbConvertedBuffer = ByteBuffer.allocateDirect(width * height * 4);
        mYRotatedBuffer = ByteBuffer.allocateDirect(width * height);
        mURotatedBuffer = ByteBuffer.allocateDirect(width * height / 2);
        mVRotatedBuffer = ByteBuffer.allocateDirect(width * height / 2);
        mNV21YDelegatedByteBuffer = ByteBuffer.allocateDirect(width * height);
        mNV21UVDelegatedByteBuffer = ByteBuffer.allocateDirect(width * height / 2);
    }

    private void closeTestResources() {
        mRGBImageReaderProxy.safeClose();
        mRotatedRGBImageReaderProxy.safeClose();
        mRotatedYUVImageReaderProxy.safeClose();
        mJpegImageReaderProxy.safeClose();
    }

    @Test
    public void writeJpeg_returnsTheSameImage() {
        // Arrange: create a JPEG image with solid color.
        byte[] inputBytes = createJpegBytesWithSolidColor(Color.RED);

        // Act: acquire image and get the bytes.
        ImageProxy imageProxy = convertJpegBytesToImage(mJpegImageReaderProxy, inputBytes);
        assertThat(imageProxy).isNotNull();
        ByteBuffer byteBuffer = imageProxy.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] outputBytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(outputBytes);

        // Assert: the color and the dimension of the restored image.
        Bitmap bitmap = BitmapFactory.decodeByteArray(outputBytes, 0, outputBytes.length);
        assertThat(bitmap.getWidth()).isEqualTo(WIDTH);
        assertThat(bitmap.getHeight()).isEqualTo(HEIGHT);
        assertBitmapColor(bitmap, Color.RED, JPEG_ENCODE_ERROR_TOLERANCE);
        imageProxy.close();
    }

    @Test
    public void writeJpegToSurface_returnsTheSameImage() {
        // Arrange: create a JPEG image with solid color.
        byte[] inputBytes = createJpegBytesWithSolidColor(Color.RED);

        // Act: acquire image and get the bytes.
        writeJpegBytesToSurface(mJpegImageReaderProxy.getSurface(), inputBytes);

        final ImageProxy imageProxy = mJpegImageReaderProxy.acquireLatestImage();
        assertThat(imageProxy).isNotNull();
        ByteBuffer byteBuffer = imageProxy.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] outputBytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(outputBytes);

        // Assert: the color and the dimension of the restored image.
        Bitmap bitmap = BitmapFactory.decodeByteArray(outputBytes, 0, outputBytes.length);
        assertThat(bitmap.getWidth()).isEqualTo(WIDTH);
        assertThat(bitmap.getHeight()).isEqualTo(HEIGHT);
        assertBitmapColor(bitmap, Color.RED, JPEG_ENCODE_ERROR_TOLERANCE);
        imageProxy.close();
    }

    @Test
    public void convertYuvToJpegBytesIntoSurface_sizeAndRotationAreCorrect() throws IOException {
        final int expectedRotation = 270;

        // Act: convert it into JPEG and write into the surface.
        ImageProcessingUtil.convertYuvToJpegBytesIntoSurface(mYUVImageProxy,
                100, expectedRotation, mJpegImageReaderProxy.getSurface());

        final ImageProxy imageProxy = mJpegImageReaderProxy.acquireLatestImage();
        assertThat(imageProxy).isNotNull();
        ByteBuffer byteBuffer = imageProxy.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] outputBytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(outputBytes);

        // Assert: the format is JPEG and can decode,  the size is correct, the rotation in Exif
        // is correct.
        assertThat(imageProxy.getFormat()).isEqualTo(ImageFormat.JPEG);
        Bitmap bitmap = BitmapFactory.decodeByteArray(outputBytes, 0, outputBytes.length);
        assertThat(bitmap.getWidth()).isEqualTo(WIDTH);
        assertThat(bitmap.getHeight()).isEqualTo(HEIGHT);
        Exif exif = Exif.createFromImageProxy(imageProxy);
        assertThat(exif.getRotation()).isEqualTo(expectedRotation);
        imageProxy.close();
    }

    /**
     * Returns JPEG bytes of a image with the given color.
     */
    private byte[] createJpegBytesWithSolidColor(int color) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        // Draw a solid color
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(color);
        // Encode to JPEG and return.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    @Test
    public void convertYUVToRGB_withNV12InputImageProxy() {
        // Act.
        ImageProxy rgbImageProxy = ImageProcessingUtil.convertYUVToRGB(
                mYUVImageProxy,
                mRGBImageReaderProxy,
                mRgbConvertedBuffer,
                /*rotation=*/0,
                /*onePixelShiftRequested=*/false);

        // Assert.
        assertThat(rgbImageProxy.getFormat()).isEqualTo(PixelFormat.RGBA_8888);
        assertThat(rgbImageProxy.getPlanes().length).isEqualTo(1);
        rgbImageProxy.close();
    }

    @Test
    public void convertYUVToRGBWhenUnsupportedYUVFormat() {
        mYUVImageProxy.close();
        int pixelStrideY = 1;
        int unsupportedPixelStrideUV = 3;
        mYUVImageProxy = TestImageUtil.createYuvFakeImageProxy(new FakeImageInfo(), WIDTH, HEIGHT,
                pixelStrideY, unsupportedPixelStrideUV, false, true);
        mYUVImageProxy.setWidth(WIDTH);
        mYUVImageProxy.setHeight(HEIGHT);
        mYUVImageProxy.setFormat(ImageFormat.YUV_420_888);

        // Act.
        ImageProxy rgbImageProxy = ImageProcessingUtil.convertYUVToRGB(
                mYUVImageProxy,
                mRGBImageReaderProxy,
                mRgbConvertedBuffer,
                /*rotation=*/0,
                /*onePixelShiftRequested=*/false);

        // Assert.
        assertThat(rgbImageProxy.getFormat()).isEqualTo(PixelFormat.RGBA_8888);
        assertThat(rgbImageProxy.getPlanes().length).isEqualTo(1);

        // Verifies the color value diff between the input image and the decoded output image proxy.
        Bitmap inputDecodedBitmap = ImageUtil.createBitmapFromImageProxy(mYUVImageProxy);
        Bitmap outputBitmap = ImageUtil.createBitmapFromImageProxy(rgbImageProxy);
        assertThat(TestImageUtil.getAverageDiff(inputDecodedBitmap, outputBitmap))
                .isEqualTo(0);

        rgbImageProxy.close();
    }

    @Test
    public void applyPixelShiftForYUVWhenOnePixelShiftEnabled() {
        // Arrange.
        mYUVImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                mYUVImageProxyPlaneDataType,
                /*incrementValue=*/true));

        // Assert.
        assertThat(mYUVImageProxy.getPlanes()[0].getBuffer().get(0)).isEqualTo(1);
        assertThat(mYUVImageProxy.getPlanes()[1].getBuffer().get(0)).isEqualTo(1);
        assertThat(mYUVImageProxy.getPlanes()[2].getBuffer().get(0)).isEqualTo(1);

        // Act.
        boolean result = ImageProcessingUtil.applyPixelShiftForYUV(mYUVImageProxy);

        // Assert.
        assertThat(result).isTrue();
        assertThat(mYUVImageProxy.getPlanes()[0].getBuffer().get(0)).isEqualTo(2);
        assertThat(mYUVImageProxy.getPlanes()[1].getBuffer().get(0)).isEqualTo(2);
        assertThat(mYUVImageProxy.getPlanes()[2].getBuffer().get(0)).isEqualTo(2);
    }

    @Test
    public void closeYUVImageProxyWhenRGBImageProxyClosed() {
        // Act.
        ImageProxy rgbImageProxy = ImageProcessingUtil.convertYUVToRGB(
                mYUVImageProxy,
                mRGBImageReaderProxy,
                mRgbConvertedBuffer,
                /*rotation=*/0,
                /*onePixelShiftRequested=*/false);

        // Assert.
        assertThat(rgbImageProxy.getFormat()).isEqualTo(PixelFormat.RGBA_8888);
        assertThat(rgbImageProxy.getPlanes().length).isEqualTo(1);
        assertThat(mYUVImageProxy.isClosed()).isFalse();

        rgbImageProxy.close();

        assertThat(mYUVImageProxy.isClosed()).isTrue();
    }

    @Test
    public void rotateRGB_imageRotated() {
        // Act.
        ImageProxy rgbImageProxy = ImageProcessingUtil.convertYUVToRGB(
                mYUVImageProxy,
                mRotatedRGBImageReaderProxy,
                mRgbConvertedBuffer,
                /*rotation=*/90,
                /*onePixelShiftRequested=*/false);

        // Assert.
        assertThat(rgbImageProxy.getFormat()).isEqualTo(PixelFormat.RGBA_8888);
        assertThat(rgbImageProxy.getPlanes().length).isEqualTo(1);
        assertThat(rgbImageProxy.getWidth()).isEqualTo(HEIGHT);
        assertThat(rgbImageProxy.getHeight()).isEqualTo(WIDTH);
        rgbImageProxy.close();
    }

    @Test
    public void rotateYUV_imageRotated_0() {
        rotateYUV_imageRotated(OUTPUT_IMAGE_FORMAT_YUV_420_888, 0, true);
    }

    @Test
    public void rotateYUV_imageRotated_90() {
        rotateYUV_imageRotated(OUTPUT_IMAGE_FORMAT_YUV_420_888, 90, false);
    }

    @Test
    public void rotateYUV_imageRotated_180() {
        rotateYUV_imageRotated(OUTPUT_IMAGE_FORMAT_YUV_420_888, 180, false);
    }

    @Test
    public void rotateYUV_imageRotated_270() {
        rotateYUV_imageRotated(OUTPUT_IMAGE_FORMAT_YUV_420_888, 270, false);
    }

    @Test
    public void rotateYUV_imageRotated_0_outputNV21() {
        rotateYUV_imageRotated(OUTPUT_IMAGE_FORMAT_NV21, 0,
                ImageProcessingUtil.isNV21FormatImage(mYUVImageProxy));
    }

    @Test
    public void rotateYUV_imageRotated_90_outputNV21() {
        rotateYUV_imageRotated(OUTPUT_IMAGE_FORMAT_NV21, 90, false);
    }

    @Test
    public void rotateYUV_imageRotated_180_outputNV21() {
        rotateYUV_imageRotated(OUTPUT_IMAGE_FORMAT_NV21, 180, false);
    }

    @Test
    public void rotateYUV_imageRotated_270_outputNV21() {
        rotateYUV_imageRotated(OUTPUT_IMAGE_FORMAT_NV21, 270, false);
    }

    private void rotateYUV_imageRotated(
            int outputImageFormat,
            int rotation,
            boolean outputShouldBeNull) {
        // Pixel2 API28 emulator has problem to run the test
        assumeFalse(Companion.isPixel2Api28Emulator());
        // Arrange.
        int width = 64;
        int height = 32;
        closeTestResources();
        createTestResources(width, height, rotation);

        // Act.
        ImageProxy yuvImageProxy = null;

        if (outputImageFormat == OUTPUT_IMAGE_FORMAT_YUV_420_888 && Build.VERSION.SDK_INT >= 23) {
            yuvImageProxy = rotateYUV(
                    mYUVImageProxy,
                    mRotatedYUVImageReaderProxy,
                    ImageWriter.newInstance(
                            mRotatedYUVImageReaderProxy.getSurface(),
                            mRotatedYUVImageReaderProxy.getMaxImages()),
                    mYRotatedBuffer,
                    mURotatedBuffer,
                    mVRotatedBuffer,
                    /*rotation=*/rotation);
        } else if (outputImageFormat == OUTPUT_IMAGE_FORMAT_NV21) {
            yuvImageProxy = rotateYUVAndConvertToNV21(
                    mYUVImageProxy,
                    mYRotatedBuffer,
                    mURotatedBuffer,
                    mVRotatedBuffer,
                    mNV21YDelegatedByteBuffer,
                    mNV21UVDelegatedByteBuffer,
                    /*rotation=*/rotation);
        }

        // Assert.
        if (outputShouldBeNull) {
            assertThat(yuvImageProxy).isNull();
            return;
        }

        boolean flipWh = rotation % 180 != 0;
        assertThat(yuvImageProxy).isNotNull();
        assertThat(yuvImageProxy.getFormat()).isEqualTo(ImageFormat.YUV_420_888);
        assertThat(yuvImageProxy.getPlanes().length).isEqualTo(3);
        assertThat(yuvImageProxy.getWidth()).isEqualTo(flipWh ? height : width);
        assertThat(yuvImageProxy.getHeight()).isEqualTo(flipWh ? width : height);

        if (outputImageFormat == OUTPUT_IMAGE_FORMAT_NV21) {
            assertThat(ImageProcessingUtil.isNV21FormatImage(yuvImageProxy)).isTrue();
        }

        // Verifies the color value diff between the rotated input image and the decoded output
        // image proxy.
        Bitmap inputDecodedBitmap = ImageUtil.createBitmapFromImageProxy(mYUVImageProxy);
        Bitmap inputRotatedBitmap = TestImageUtil.rotateBitmap(inputDecodedBitmap, rotation);
        Bitmap outputBitmap = ImageUtil.createBitmapFromImageProxy(yuvImageProxy);
        assertThat(TestImageUtil.getAverageDiff(inputRotatedBitmap, outputBitmap))
                .isEqualTo(0);

        yuvImageProxy.close();
    }

    @Test
    public void yuvToRGBUsesBT601FullSwing() {
        // Check that studio swing YUV colors do not scale to full range RGB colors
        // White
        int referenceColorRgb = yuvBt601FullSwingToRGB(
                YUV_WHITE_STUDIO_SWING_BT601[0],
                YUV_WHITE_STUDIO_SWING_BT601[1],
                YUV_WHITE_STUDIO_SWING_BT601[2]);
        assertSolidYUVColorConvertedToRGBMatchesReferenceRGB(YUV_WHITE_STUDIO_SWING_BT601,
                referenceColorRgb);

        // Black
        referenceColorRgb = yuvBt601FullSwingToRGB(
                YUV_BLACK_STUDIO_SWING_BT601[0],
                YUV_BLACK_STUDIO_SWING_BT601[1],
                YUV_BLACK_STUDIO_SWING_BT601[2]);
        assertSolidYUVColorConvertedToRGBMatchesReferenceRGB(YUV_BLACK_STUDIO_SWING_BT601,
                referenceColorRgb);

        // Blue
        referenceColorRgb = yuvBt601FullSwingToRGB(
                YUV_BLUE_STUDIO_SWING_BT601[0],
                YUV_BLUE_STUDIO_SWING_BT601[1],
                YUV_BLUE_STUDIO_SWING_BT601[2]);
        assertSolidYUVColorConvertedToRGBMatchesReferenceRGB(YUV_BLUE_STUDIO_SWING_BT601,
                referenceColorRgb);

        // Red
        referenceColorRgb = yuvBt601FullSwingToRGB(
                YUV_RED_STUDIO_SWING_BT601[0],
                YUV_RED_STUDIO_SWING_BT601[1],
                YUV_RED_STUDIO_SWING_BT601[2]);
        assertSolidYUVColorConvertedToRGBMatchesReferenceRGB(YUV_RED_STUDIO_SWING_BT601,
                referenceColorRgb);
    }

    @Test
    public void canCopyBetweenBitmapAndByteBufferWithDifferentStrides() {

        // Create bitmap with a solid color
        Bitmap bitmap1 = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        bitmap1.eraseColor(Color.YELLOW);

        int bufferStride = bitmap1.getRowBytes() + PADDING_BYTES;

        // Same size bitmap with a different color
        Bitmap bitmap2 = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        bitmap2.eraseColor(Color.BLUE);

        ByteBuffer bytebuffer = ByteBuffer.allocateDirect(bufferStride * bitmap1.getHeight());

        // Copy bitmap1 into bytebuffer
        ImageProcessingUtil.copyBitmapToByteBuffer(bitmap1, bytebuffer, bufferStride);

        // Copy bytebuffer into bitmap2
        ImageProcessingUtil.copyByteBufferToBitmap(bitmap2, bytebuffer, bufferStride);

        // Assert second bitmap now has the same color as first bitmap
        assertBitmapColor(bitmap2, Color.YELLOW, 0);
    }

    private void assertSolidYUVColorConvertedToRGBMatchesReferenceRGB(int[] yuvColor,
            int referenceColorRgb) {
        ImageProxy yuvImageProxy = createYuvImageProxyWithPlanes();

        fillYuvImageProxyWithYUVColor(yuvImageProxy, yuvColor[0], yuvColor[1], yuvColor[2]);

        try (ImageProxy rgbImageProxy = ImageProcessingUtil.convertYUVToRGB(
                yuvImageProxy,
                mRGBImageReaderProxy,
                mRgbConvertedBuffer,
                /*rotation=*/0,
                /*onePixelShiftRequested=*/false)) {
            assertRGBImageProxyColor(rgbImageProxy, referenceColorRgb);
        }
    }

    private static void fillYuvImageProxyWithYUVColor(@NonNull ImageProxy imageProxy,
            @IntRange(from = 0, to = 255) int y,
            @IntRange(from = 0, to = 255) int u,
            @IntRange(from = 0, to = 255) int v) {
        Preconditions.checkArgumentInRange(y, 0, 255, "y");
        Preconditions.checkArgumentInRange(u, 0, 255, "u");
        Preconditions.checkArgumentInRange(v, 0, 255, "v");

        // Fill in planes
        // Y plane
        fillPlane(imageProxy.getPlanes()[0], (byte) y);
        // U plane
        fillPlane(imageProxy.getPlanes()[1], (byte) u);
        // V plane
        fillPlane(imageProxy.getPlanes()[2], (byte) v);
    }

    private static void fillPlane(ImageProxy.@NonNull PlaneProxy plane, byte value) {
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        // Ignore row stride here, we don't need to be efficient, so we'll fill the padding also.
        buffer.rewind();
        while (buffer.hasRemaining()) {
            int nextPosition = Math.min(buffer.capacity(), buffer.position() + pixelStride);
            buffer.put(value);
            buffer.position(nextPosition);
        }
    }

    private @NonNull ImageProxy createYuvImageProxyWithPlanes() {
        FakeImageProxy yuvImageProxy = new FakeImageProxy(new FakeImageInfo());
        yuvImageProxy.setWidth(WIDTH);
        yuvImageProxy.setHeight(HEIGHT);
        yuvImageProxy.setFormat(ImageFormat.YUV_420_888);

        yuvImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                mYUVImageProxyPlaneDataType,
                /*incrementValue=*/false));

        return yuvImageProxy;
    }

    private static void assertRGBImageProxyColor(ImageProxy rgbImageProxy,
            int referenceColorRgb) {
        // Convert to Bitmap
        ImageProxy.PlaneProxy pixelPlane = Preconditions.checkNotNull(rgbImageProxy).getPlanes()[0];
        IntBuffer rgbPixelBuf = pixelPlane.getBuffer().asIntBuffer();
        int rgbBufLength = rgbPixelBuf.capacity();
        int[] rgbPixelArray = new int[rgbBufLength];
        rgbPixelBuf.get(rgbPixelArray);

        // the output array is in the order of ABGR (LITTLE ENDIAN) if BIG_ENDIAN is not set
        for (int i = 0; i < rgbBufLength; i++) {
            int alpha = (rgbPixelArray[i] >> 24) & 0xff;
            int blue = (rgbPixelArray[i] >> 16) & 0xff;
            int green = (rgbPixelArray[i] >> 8) & 0xff;
            int red = (rgbPixelArray[i] >> 0) & 0xff;
            rgbPixelArray[i] =
                    (alpha & 0xff) << 24 | (red & 0xff) << 16 | (green & 0xff) << 8 | (blue & 0xff);
        }

        Bitmap rgbBitmap = Bitmap.createBitmap(rgbPixelArray, 0,
                pixelPlane.getRowStride() / pixelPlane.getPixelStride(),
                WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);

        assertBitmapColor(rgbBitmap, referenceColorRgb, 0);
    }

    /**
     * Asserts that the given {@link Bitmap} is almost the given color.
     *
     * <p>Loops through all the pixels and verify that they are the given color. The color is
     * assumed to be correct if the difference between each pixel's color and the provided color
     * is at most {@code perChannelTolerance}.
     */
    private static void assertBitmapColor(Bitmap bitmap, int color, int perChannelTolerance) {
        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                int pixelColor = bitmap.getPixel(i, j);

                // Compare the R, G and B of the pixel to the given color.
                for (int shift = 16; shift > 0; shift -= 8) {
                    int pixelRgb = (pixelColor >> shift) & 0xFF;
                    int rgb = (color >> shift) & 0xFF;
                    assertWithMessage(String.format("Color from bitmap (#%08X) does not match "
                                    + "reference color (#%08X) at col %d, row %d [tolerance: %d]",
                            pixelColor, color, i, j, perChannelTolerance)
                    ).that((double) pixelRgb).isWithin(perChannelTolerance).of(rgb);
                }
            }
        }
    }

    private static int yuvBt601FullSwingToRGB(
            @IntRange(from = 0, to = 255) int y,
            @IntRange(from = 0, to = 255) int u,
            @IntRange(from = 0, to = 255) int v) {
        // Shift u and v to the range [-128, 127]
        int _u = u - 128;
        int _v = v - 128;
        int r = (int) MathUtils.clamp((y + 0.000000 * _u + 1.402000 * _v), 0, 255);
        int g = (int) MathUtils.clamp((y - 0.344136 * _u - 0.714136 * _v), 0, 255);
        int b = (int) MathUtils.clamp((y + 1.772000 * _u + 0.000000 * _v), 0, 255);

        return Color.rgb(r, g, b);
    }
}
