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

import static androidx.camera.core.ImageProcessingUtil.rotateYUV;
import static androidx.camera.testing.ImageProxyUtil.createYUV420ImagePlanes;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.ImageWriter;

import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

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

    private ByteBuffer mRgbConvertedBuffer;
    private ByteBuffer mYRotatedBuffer;
    private ByteBuffer mURotatedBuffer;
    private ByteBuffer mVRotatedBuffer;

    private FakeImageProxy mYUVImageProxy;
    private SafeCloseImageReaderProxy mRGBImageReaderProxy;
    private SafeCloseImageReaderProxy mRotatedRGBImageReaderProxy;
    private SafeCloseImageReaderProxy mRotatedYUVImageReaderProxy;

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
}
