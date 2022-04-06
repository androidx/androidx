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

import static androidx.camera.core.ImageProcessingUtil.convertJpegBytesToImage;
import static androidx.camera.core.ImageProcessingUtil.rotateYUV;
import static androidx.camera.testing.ImageProxyUtil.createYUV420ImagePlanes;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.ImageWriter;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;
import androidx.core.math.MathUtils;
import androidx.core.util.Preconditions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Unit test for {@link ImageProcessingUtil}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class ImageProcessingUtilTest {

    private static final int WIDTH = 8;
    private static final int HEIGHT = 4;
    private static final int PIXEL_STRIDE_Y = 1;
    private static final int PIXEL_STRIDE_UV = 1;
    private static final int PIXEL_STRIDE_Y_UNSUPPORTED = 1;
    private static final int PIXEL_STRIDE_UV_UNSUPPORTED = 3;
    private static final int MAX_IMAGES = 4;
    private static final int JPEG_ENCODE_ERROR_TOLERANCE = 3;

    private ByteBuffer mRgbConvertedBuffer;
    private ByteBuffer mYRotatedBuffer;
    private ByteBuffer mURotatedBuffer;
    private ByteBuffer mVRotatedBuffer;
    private static final int[] YUV_WHITE_STUDIO_SWING_BT601 = {/*y=*/235, /*u=*/128, /*v=*/128};
    private static final int[] YUV_BLACK_STUDIO_SWING_BT601 = {/*y=*/16, /*u=*/128, /*v=*/128};
    private static final int[] YUV_BLUE_STUDIO_SWING_BT601 = {/*y=*/16, /*u=*/240, /*v=*/128};
    private static final int[] YUV_RED_STUDIO_SWING_BT601 = {/*y=*/16, /*u=*/128, /*v=*/240};

    private FakeImageProxy mYUVImageProxy;
    private SafeCloseImageReaderProxy mRGBImageReaderProxy;
    private SafeCloseImageReaderProxy mRotatedRGBImageReaderProxy;
    private SafeCloseImageReaderProxy mRotatedYUVImageReaderProxy;
    private SafeCloseImageReaderProxy mJpegImageReaderProxy;

    @Before
    public void setUp() {
        mYUVImageProxy = new FakeImageProxy(new FakeImageInfo());
        mYUVImageProxy.setWidth(WIDTH);
        mYUVImageProxy.setHeight(HEIGHT);
        mYUVImageProxy.setFormat(ImageFormat.YUV_420_888);

        // rgb image reader proxy should not be mocked for JNI native code
        mRGBImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        WIDTH,
                        HEIGHT,
                        PixelFormat.RGBA_8888,
                        MAX_IMAGES));

        // rotated image reader proxy with width and height flipped
        mRotatedRGBImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        HEIGHT,
                        WIDTH,
                        PixelFormat.RGBA_8888,
                        MAX_IMAGES));

        mRotatedYUVImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        HEIGHT,
                        WIDTH,
                        ImageFormat.YUV_420_888,
                        MAX_IMAGES));

        mJpegImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        WIDTH,
                        HEIGHT,
                        ImageFormat.JPEG,
                        MAX_IMAGES));

        mRgbConvertedBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4);
        mYRotatedBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT);
        mURotatedBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 2);
        mVRotatedBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 2);
    }

    @After
    public void tearDown() {
        mRGBImageReaderProxy.safeClose();
        mRotatedRGBImageReaderProxy.safeClose();
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
    public void convertYUVToRGBWhenNotFlipUV() {
        // Arrange.
        mYUVImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y,
                PIXEL_STRIDE_UV,
                /*flipUV=*/false,
                /*incrementValue=*/false));

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
    }

    @Test
    public void convertYUVToRGBWhenFlipUV() {
        // Arrange.
        mYUVImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y,
                PIXEL_STRIDE_UV,
                /*flipUV=*/true,
                /*incrementValue=*/false));

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
    }

    @Test
    public void convertYUVToRGBWhenUnsupportedYUVFormat() {
        // Arrange.
        mYUVImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y_UNSUPPORTED,
                PIXEL_STRIDE_UV_UNSUPPORTED,
                /*flipUV=*/true,
                /*incrementValue=*/false));

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
    }

    @Test
    public void applyPixelShiftForYUVWhenOnePixelShiftEnabled() {
        // Arrange.
        mYUVImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y,
                PIXEL_STRIDE_UV,
                /*flipUV=*/false,
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
        // Arrange.
        mYUVImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y,
                PIXEL_STRIDE_UV,
                /*flipUV=*/false,
                /*incrementValue=*/false));

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
        // Arrange.
        mYUVImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y,
                PIXEL_STRIDE_UV,
                /*flipUV=*/true,
                /*incrementValue=*/false));

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
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void rotateYUV_imageRotated() {
        // Arrange.
        mYUVImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y,
                PIXEL_STRIDE_UV,
                /*flipUV=*/true,
                /*incrementValue=*/false));

        // Act.
        ImageProxy yuvImageProxy = rotateYUV(
                mYUVImageProxy,
                mRotatedYUVImageReaderProxy,
                ImageWriter.newInstance(
                        mRotatedYUVImageReaderProxy.getSurface(),
                        mRotatedYUVImageReaderProxy.getMaxImages()),
                mYRotatedBuffer,
                mURotatedBuffer,
                mVRotatedBuffer,
                /*rotation=*/90);

        // Assert.
        assertThat(yuvImageProxy).isNotNull();
        assertThat(yuvImageProxy.getFormat()).isEqualTo(ImageFormat.YUV_420_888);
        assertThat(yuvImageProxy.getPlanes().length).isEqualTo(3);
        assertThat(yuvImageProxy.getWidth()).isEqualTo(HEIGHT);
        assertThat(yuvImageProxy.getHeight()).isEqualTo(WIDTH);
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

    private static void fillPlane(@NonNull ImageProxy.PlaneProxy plane, byte value) {
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

    @NonNull
    private ImageProxy createYuvImageProxyWithPlanes() {
        FakeImageProxy yuvImageProxy = new FakeImageProxy(new FakeImageInfo());
        yuvImageProxy.setWidth(WIDTH);
        yuvImageProxy.setHeight(HEIGHT);
        yuvImageProxy.setFormat(ImageFormat.YUV_420_888);

        yuvImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y,
                PIXEL_STRIDE_UV,
                /*flipUV=*/true,
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
