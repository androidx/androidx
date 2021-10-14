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

import static android.graphics.ImageFormat.YUV_420_888;
import static android.graphics.PixelFormat.RGBA_8888;

import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888;
import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888;
import static androidx.camera.testing.ImageProxyUtil.createYUV420ImagePlanes;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.PixelFormat;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Android test for image analysis analyzer.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class ImageAnalysisAbstractAnalyzerTest {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;
    private static final int PIXEL_STRIDE_Y = 1;
    private static final int PIXEL_STRIDE_UV = 1;

    private FakeImageAnalysisAnalyzer mImageAnalysisAbstractAnalyzer;
    private FakeImageProxy mImageProxy;
    private ImageAnalysis.Analyzer mAnalyzer;
    private ImageReaderProxy mYUVImageReaderProxy;
    private ImageReaderProxy mRGBImageReaderProxy;

    @Before
    public void setup() {
        mImageProxy = new FakeImageProxy(new FakeImageInfo());
        mImageProxy.setWidth(WIDTH);
        mImageProxy.setHeight(HEIGHT);
        mImageProxy.setFormat(YUV_420_888);
        mImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                PIXEL_STRIDE_Y,
                PIXEL_STRIDE_UV,
                /*flipUV=*/true,
                /*incrementValue=*/true));

        mYUVImageReaderProxy = mock(ImageReaderProxy.class);
        when(mYUVImageReaderProxy.getWidth()).thenReturn(WIDTH);
        when(mYUVImageReaderProxy.getHeight()).thenReturn(HEIGHT);
        when(mYUVImageReaderProxy.getImageFormat()).thenReturn(YUV_420_888);
        when(mYUVImageReaderProxy.getMaxImages()).thenReturn(4);
        when(mYUVImageReaderProxy.getSurface()).thenReturn(mock(Surface.class));

        // rgb image reader proxy should not be mocked for JNI native code
        mRGBImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        mYUVImageReaderProxy.getWidth(),
                        mYUVImageReaderProxy.getHeight(),
                        PixelFormat.RGBA_8888,
                        mYUVImageReaderProxy.getMaxImages()));

        mImageAnalysisAbstractAnalyzer = new FakeImageAnalysisAnalyzer(
                new ImageAnalysisNonBlockingAnalyzer(
                CameraXExecutors.directExecutor()));

        mAnalyzer = mock(ImageAnalysis.Analyzer.class);
        mImageAnalysisAbstractAnalyzer.setAnalyzer(CameraXExecutors.mainThreadExecutor(),
                mAnalyzer);
        mImageAnalysisAbstractAnalyzer.attach();
    }

    @Test
    public void analysisRunWhenOutputImageYUV() throws ExecutionException, InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888);
        mImageAnalysisAbstractAnalyzer.setRGBImageReaderProxy(mRGBImageReaderProxy);

        // Act.
        ListenableFuture<Void> result =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mImageProxy);
        result.get();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer).analyze(imageProxyArgumentCaptor.capture());
        assertThat(imageProxyArgumentCaptor.getValue().getFormat()).isEqualTo(YUV_420_888);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes().length).isEqualTo(3);
    }

    @Test
    public void analysisRunWhenOutputImageRGB() throws ExecutionException, InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888);
        mImageAnalysisAbstractAnalyzer.setRGBImageReaderProxy(mRGBImageReaderProxy);

        // Act.
        ListenableFuture<Void> result =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mImageProxy);
        result.get();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer).analyze(imageProxyArgumentCaptor.capture());
        assertThat(imageProxyArgumentCaptor.getValue().getFormat()).isEqualTo(RGBA_8888);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes().length).isEqualTo(1);
    }

    @Test
    public void applyPixelShiftForYUVWhenOnePixelShiftEnabled() throws ExecutionException,
            InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888);
        mImageAnalysisAbstractAnalyzer.setRGBImageReaderProxy(mRGBImageReaderProxy);
        mImageAnalysisAbstractAnalyzer.setOnePixelShiftEnabled(true);

        // Act.
        ListenableFuture<Void> result =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mImageProxy);
        result.get();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer).analyze(imageProxyArgumentCaptor.capture());
        assertThat(imageProxyArgumentCaptor.getValue().getFormat()).isEqualTo(YUV_420_888);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes().length).isEqualTo(3);

        assertThat(imageProxyArgumentCaptor.getValue().getPlanes()[0].getBuffer().get(0))
                .isEqualTo(2);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes()[1].getBuffer().get(0))
                .isEqualTo(2);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes()[2].getBuffer().get(0))
                .isEqualTo(2);
    }

    @Test
    public void notApplyPixelShiftForYUVWhenOnePixelShiftDisabled() throws ExecutionException,
            InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888);
        mImageAnalysisAbstractAnalyzer.setRGBImageReaderProxy(mRGBImageReaderProxy);
        mImageAnalysisAbstractAnalyzer.setOnePixelShiftEnabled(false);

        // Act.
        ListenableFuture<Void> result =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mImageProxy);
        result.get();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer).analyze(imageProxyArgumentCaptor.capture());
        assertThat(imageProxyArgumentCaptor.getValue().getFormat()).isEqualTo(YUV_420_888);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes().length).isEqualTo(3);

        assertThat(imageProxyArgumentCaptor.getValue().getPlanes()[0].getBuffer().get(0))
                .isEqualTo(1);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes()[1].getBuffer().get(0))
                .isEqualTo(1);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes()[2].getBuffer().get(0))
                .isEqualTo(1);
    }

    /**
     * Faked image analysis analyzer to verify YUV to RGB convert is working as expected or not.
     *
     * It purely delegates operations to {@link ImageAnalysisNonBlockingAnalyzer} because we cannot
     * spy the final class.
     */
    public static class FakeImageAnalysisAnalyzer extends ImageAnalysisAbstractAnalyzer {

        private final ImageAnalysisNonBlockingAnalyzer mImageAnalysisNonBlockingAnalyzer;

        public FakeImageAnalysisAnalyzer(
                ImageAnalysisNonBlockingAnalyzer imageAnalysisNonBlockingAnalyzer) {
            mImageAnalysisNonBlockingAnalyzer = imageAnalysisNonBlockingAnalyzer;
        }

        @Nullable
        @Override
        ImageProxy acquireImage(@NonNull ImageReaderProxy imageReaderProxy) {
            return mImageAnalysisNonBlockingAnalyzer.acquireImage(imageReaderProxy);
        }

        @Override
        void onValidImageAvailable(@NonNull ImageProxy imageProxy) {
            mImageAnalysisNonBlockingAnalyzer.onValidImageAvailable(imageProxy);
        }

        @Override
        void clearCache() {
            mImageAnalysisNonBlockingAnalyzer.clearCache();
        }

        @Override
        void setAnalyzer(@Nullable Executor userExecutor,
                @Nullable ImageAnalysis.Analyzer subscribedAnalyzer) {
            mImageAnalysisNonBlockingAnalyzer.setAnalyzer(userExecutor, subscribedAnalyzer);
        }

        @Override
        void attach() {
            mImageAnalysisNonBlockingAnalyzer.attach();
        }

        @Override
        void setOutputImageFormat(int outputImageFormat) {
            mImageAnalysisNonBlockingAnalyzer.setOutputImageFormat(outputImageFormat);
        }

        @Override
        void setRGBImageReaderProxy(@NonNull ImageReaderProxy rgbImageReaderProxy) {
            mImageAnalysisNonBlockingAnalyzer.setRGBImageReaderProxy(rgbImageReaderProxy);
        }

        @Override
        ListenableFuture<Void> analyzeImage(@NonNull ImageProxy imageProxy) {
            return mImageAnalysisNonBlockingAnalyzer.analyzeImage(imageProxy);
        }

        @Override
        void setOnePixelShiftEnabled(boolean onePixelShiftEnabled) {
            mImageAnalysisNonBlockingAnalyzer.setOnePixelShiftEnabled(onePixelShiftEnabled);
        }
    }
}
