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

import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_NV21;
import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888;
import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888;
import static androidx.camera.core.ImageAnalysisAbstractAnalyzer.getAdditionalTransformMatrixAppliedByProcessor;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA;
import static androidx.camera.testing.impl.ImageProxyUtil.createYUV420ImagePlanes;
import static androidx.camera.testing.impl.ImageProxyUtil.getDefaultYuvFormatPlaneDataType;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import android.graphics.Matrix;
import android.graphics.Rect;

import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.impl.fakes.FakeImageInfo;
import androidx.camera.testing.impl.fakes.FakeImageProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Android test for image analysis analyzer.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ImageAnalysisAbstractAnalyzerTest {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 4;
    private static final int MAX_IMAGES = 4;

    private FakeImageAnalysisAnalyzer mImageAnalysisAbstractAnalyzer;
    private FakeImageProxy mImageProxy;
    private FakeImageProxy mSecondImageProxy;
    private ImageAnalysis.Analyzer mAnalyzer;
    private SafeCloseImageReaderProxy mYUVImageReaderProxy;
    private SafeCloseImageReaderProxy mRGBImageReaderProxy;
    private SafeCloseImageReaderProxy mRotatedYUVImageReaderProxy;
    private SafeCloseImageReaderProxy mRotatedRGBImageReaderProxy;
    private Matrix mSensorToBufferMatrix;

    @Before
    public void setup() {
        int yuvFormatPlaneDataType = getDefaultYuvFormatPlaneDataType(RESOLUTION_VGA.getWidth(),
                RESOLUTION_VGA.getHeight());
        mImageProxy = new FakeImageProxy(new FakeImageInfo());
        mImageProxy.setWidth(WIDTH);
        mImageProxy.setHeight(HEIGHT);
        mImageProxy.setFormat(YUV_420_888);
        mImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                yuvFormatPlaneDataType,
                /*incrementValue=*/true));

        mSecondImageProxy = new FakeImageProxy(new FakeImageInfo());
        mSecondImageProxy.setWidth(WIDTH);
        mSecondImageProxy.setHeight(HEIGHT);
        mSecondImageProxy.setFormat(YUV_420_888);
        mSecondImageProxy.setPlanes(createYUV420ImagePlanes(
                WIDTH,
                HEIGHT,
                yuvFormatPlaneDataType,
                /*incrementValue=*/true));

        mYUVImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        WIDTH,
                        HEIGHT,
                        YUV_420_888,
                        MAX_IMAGES));

        // rotated yuv image reader proxy with 90 degree
        mRotatedYUVImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        mYUVImageReaderProxy.getHeight(),
                        mYUVImageReaderProxy.getWidth(),
                        YUV_420_888,
                        mYUVImageReaderProxy.getMaxImages()));

        // rgb image reader proxy should not be mocked for JNI native code
        mRGBImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        mYUVImageReaderProxy.getWidth(),
                        mYUVImageReaderProxy.getHeight(),
                        RGBA_8888,
                        mYUVImageReaderProxy.getMaxImages()));

        // rotated rgb image reader proxy with 90 degree
        mRotatedRGBImageReaderProxy = new SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                        mYUVImageReaderProxy.getHeight(),
                        mYUVImageReaderProxy.getWidth(),
                        RGBA_8888,
                        mYUVImageReaderProxy.getMaxImages()));

        mImageAnalysisAbstractAnalyzer = new FakeImageAnalysisAnalyzer(
                new ImageAnalysisNonBlockingAnalyzer(
                CameraXExecutors.directExecutor()));

        mSensorToBufferMatrix = new Matrix();
        mSensorToBufferMatrix.setTranslate(20.0f, 10.0f);
        mSensorToBufferMatrix.setScale(0.4f, 0.6f, 0.0f, 0.0f);

        mAnalyzer = mock(ImageAnalysis.Analyzer.class);
        mImageAnalysisAbstractAnalyzer.setAnalyzer(CameraXExecutors.mainThreadExecutor(),
                mAnalyzer);
        mImageAnalysisAbstractAnalyzer.attach();
    }

    @Test
    public void analysisRunWhenOutputImage_YUV_420_888()
            throws ExecutionException, InterruptedException {
        analysisRunWhenOutputImage(OUTPUT_IMAGE_FORMAT_YUV_420_888, mYUVImageReaderProxy, 3,
                YUV_420_888);
    }

    @Test
    public void analysisRunWhenOutputImage_RGBA_8888()
            throws ExecutionException, InterruptedException {
        analysisRunWhenOutputImage(OUTPUT_IMAGE_FORMAT_RGBA_8888, mRGBImageReaderProxy, 1,
                RGBA_8888);
    }

    @Test
    public void analysisRunWhenOutputImage_NV21()
            throws ExecutionException, InterruptedException {
        analysisRunWhenOutputImage(OUTPUT_IMAGE_FORMAT_NV21, null, 3, YUV_420_888);
    }

    private void analysisRunWhenOutputImage(
            int outputFormat,
            @Nullable SafeCloseImageReaderProxy processedImageReaderProxy,
            int expectedPlaneCount,
            int expectedFormat)
            throws ExecutionException, InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(outputFormat);
        if (processedImageReaderProxy != null) {
            mImageAnalysisAbstractAnalyzer.setProcessedImageReaderProxy(processedImageReaderProxy);
        }

        // Act.
        ListenableFuture<Void> result =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mImageProxy);
        result.get();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer).analyze(imageProxyArgumentCaptor.capture());
        assertThat(imageProxyArgumentCaptor.getValue().getFormat()).isEqualTo(expectedFormat);
        assertThat(imageProxyArgumentCaptor.getValue().getPlanes().length).isEqualTo(
                expectedPlaneCount);
        assertThat(imageProxyArgumentCaptor.getValue().getCropRect()).isEqualTo(
                new Rect(0, 0, WIDTH, HEIGHT));
        if (outputFormat == OUTPUT_IMAGE_FORMAT_NV21) {
            assertThat(ImageProcessingUtil.isNV21FormatImage(
                    imageProxyArgumentCaptor.getValue())).isTrue();
        }
    }

    @Test
    public void analysisRunWhenRotateYUVMinSdk23() throws ExecutionException, InterruptedException {
        analysisRunWhenRotate(OUTPUT_IMAGE_FORMAT_YUV_420_888, mRotatedYUVImageReaderProxy);

        assertThat(mImageAnalysisAbstractAnalyzer.getRGBConverterBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getYRotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getURotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getVRotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21YDelegatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21UVDelegatedBuffer()).isNull();
    }

    @Test
    public void analysisRunWhenRotate_RGBA_8888() throws ExecutionException, InterruptedException {
        analysisRunWhenRotate(OUTPUT_IMAGE_FORMAT_RGBA_8888, mRotatedRGBImageReaderProxy);

        assertThat(mImageAnalysisAbstractAnalyzer.getRGBConverterBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getYRotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getURotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getVRotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21YDelegatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21UVDelegatedBuffer()).isNull();
    }

    @Test
    public void analysisRunWhenRotate_NV21() throws ExecutionException, InterruptedException {
        analysisRunWhenRotate(OUTPUT_IMAGE_FORMAT_NV21, null);

        assertThat(mImageAnalysisAbstractAnalyzer.getRGBConverterBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getYRotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getURotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getVRotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21YDelegatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21UVDelegatedBuffer()).isNotNull();
    }

    private void analysisRunWhenRotate(int outputFormat,
            @Nullable SafeCloseImageReaderProxy processedImageReaderProxy)
            throws ExecutionException, InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(outputFormat);
        if (processedImageReaderProxy != null) {
            mImageAnalysisAbstractAnalyzer.setProcessedImageReaderProxy(
                    processedImageReaderProxy);
        }
        mImageAnalysisAbstractAnalyzer.setOutputImageRotationEnabled(true);
        mImageAnalysisAbstractAnalyzer.setSensorToBufferTransformMatrix(mSensorToBufferMatrix);
        mImageAnalysisAbstractAnalyzer.setRelativeRotation(/*rotation=*/90);

        Matrix original = new Matrix(mSensorToBufferMatrix);

        // Act.
        ListenableFuture<Void> result =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mImageProxy);
        result.get();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer).analyze(imageProxyArgumentCaptor.capture());

        Matrix target = new Matrix();
        target.setConcat(original, getAdditionalTransformMatrixAppliedByProcessor(
                WIDTH, HEIGHT, HEIGHT, WIDTH, 90));
        assertThat(imageProxyArgumentCaptor.getValue().getImageInfo()
                .getSensorToBufferTransformMatrix()).isEqualTo(target);
        assertThat(imageProxyArgumentCaptor.getValue().getCropRect())
                .isEqualTo(new Rect(0, 0, HEIGHT, WIDTH));
        if (outputFormat == OUTPUT_IMAGE_FORMAT_NV21) {
            assertThat(ImageProcessingUtil.isNV21FormatImage(
                    imageProxyArgumentCaptor.getValue())).isTrue();
        }
    }

    @Test
    public void analysisRunWhenSetTargetRotationMultipleTimes_YUV_420_888()
            throws ExecutionException, InterruptedException {
        analysisRunWhenSetTargetRotationMultipleTimes(OUTPUT_IMAGE_FORMAT_YUV_420_888,
                mRotatedYUVImageReaderProxy);
    }

    @Test
    public void analysisRunWhenSetTargetRotationMultipleTimes_RGBA_8888()
            throws ExecutionException, InterruptedException {
        analysisRunWhenSetTargetRotationMultipleTimes(OUTPUT_IMAGE_FORMAT_RGBA_8888,
                mRotatedRGBImageReaderProxy);
    }

    @Test
    public void analysisRunWhenSetTargetRotationMultipleTimes_NV21()
            throws ExecutionException, InterruptedException {
        analysisRunWhenSetTargetRotationMultipleTimes(OUTPUT_IMAGE_FORMAT_NV21, null);
    }

    private void analysisRunWhenSetTargetRotationMultipleTimes(int outputFormat,
            @Nullable SafeCloseImageReaderProxy processedImageReaderProxy)
            throws ExecutionException, InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(outputFormat);
        if (processedImageReaderProxy != null) {
            mImageAnalysisAbstractAnalyzer.setProcessedImageReaderProxy(processedImageReaderProxy);
        }
        mImageAnalysisAbstractAnalyzer.setOutputImageRotationEnabled(true);
        mImageAnalysisAbstractAnalyzer.setSensorToBufferTransformMatrix(mSensorToBufferMatrix);
        mImageAnalysisAbstractAnalyzer.setRelativeRotation(/*rotation=*/90);

        Matrix original = new Matrix(mSensorToBufferMatrix);

        // Act.
        ListenableFuture<Void> result1 =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mImageProxy);
        result1.get();

        reset(mAnalyzer);

        // Act.
        mImageAnalysisAbstractAnalyzer.setRelativeRotation(/*rotation=*/180);
        ListenableFuture<Void> result2 =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mSecondImageProxy);
        result2.get();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer).analyze(imageProxyArgumentCaptor.capture());

        // Verify that additional transform matrix will only be applied to original matrix once.
        Matrix target = new Matrix();
        target.setConcat(original, getAdditionalTransformMatrixAppliedByProcessor(
                WIDTH, HEIGHT, WIDTH, HEIGHT, 180));
        assertThat(imageProxyArgumentCaptor.getValue().getImageInfo()
                .getSensorToBufferTransformMatrix()).isEqualTo(target);
        if (outputFormat == OUTPUT_IMAGE_FORMAT_NV21) {
            assertThat(ImageProcessingUtil.isNV21FormatImage(
                    imageProxyArgumentCaptor.getValue())).isTrue();
        }
    }

    @Test
    public void analysisRunWhenNoRotate_RGBA_8888() throws ExecutionException,
            InterruptedException {
        analysisRunWhenNoRotateYUV(OUTPUT_IMAGE_FORMAT_RGBA_8888, mRotatedRGBImageReaderProxy);

        assertThat(mImageAnalysisAbstractAnalyzer.getRGBConverterBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getYRotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getURotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getVRotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21YDelegatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21UVDelegatedBuffer()).isNull();
    }

    @Test
    public void analysisRunWhenNoRotate_YUV_420_888() throws ExecutionException,
            InterruptedException {
        analysisRunWhenNoRotateYUV(OUTPUT_IMAGE_FORMAT_YUV_420_888, mRotatedRGBImageReaderProxy);

        assertThat(mImageAnalysisAbstractAnalyzer.getRGBConverterBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getYRotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getURotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getVRotatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21YDelegatedBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21UVDelegatedBuffer()).isNull();
    }

    @Test
    public void analysisRunWhenNoRotate_NV21() throws ExecutionException,
            InterruptedException {
        analysisRunWhenNoRotateYUV(OUTPUT_IMAGE_FORMAT_NV21, null);

        assertThat(mImageAnalysisAbstractAnalyzer.getRGBConverterBuffer()).isNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getYRotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getURotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getVRotatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21YDelegatedBuffer()).isNotNull();
        assertThat(mImageAnalysisAbstractAnalyzer.getNV21UVDelegatedBuffer()).isNotNull();
    }

    private void analysisRunWhenNoRotateYUV(int outputFormat,
            @Nullable SafeCloseImageReaderProxy processedImageReaderProxy)
            throws ExecutionException, InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(outputFormat);
        if (processedImageReaderProxy != null) {
            mImageAnalysisAbstractAnalyzer.setProcessedImageReaderProxy(
                    mRotatedRGBImageReaderProxy);
        }
        mImageAnalysisAbstractAnalyzer.setOutputImageRotationEnabled(false);
        mImageAnalysisAbstractAnalyzer.setSensorToBufferTransformMatrix(mSensorToBufferMatrix);
        mImageAnalysisAbstractAnalyzer.setRelativeRotation(/*rotation=*/270);

        // Act.
        ListenableFuture<Void> result =
                mImageAnalysisAbstractAnalyzer.analyzeImage(mImageProxy);
        result.get();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer).analyze(imageProxyArgumentCaptor.capture());

        if (outputFormat == OUTPUT_IMAGE_FORMAT_NV21) {
            assertThat(ImageProcessingUtil.isNV21FormatImage(
                    imageProxyArgumentCaptor.getValue())).isTrue();
        }
    }

    @Test
    public void applyPixelShiftForYUVWhenOnePixelShiftEnabled() throws ExecutionException,
            InterruptedException {
        // Arrange.
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888);
        mImageAnalysisAbstractAnalyzer.setProcessedImageReaderProxy(mRGBImageReaderProxy);
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
        mImageAnalysisAbstractAnalyzer.setProcessedImageReaderProxy(mRGBImageReaderProxy);
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

        @Override
        @Nullable ImageProxy acquireImage(@NonNull ImageReaderProxy imageReaderProxy) {
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
                ImageAnalysis.@Nullable Analyzer subscribedAnalyzer) {
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
        void setProcessedImageReaderProxy(
                @NonNull SafeCloseImageReaderProxy processedImageReaderProxy) {
            mImageAnalysisNonBlockingAnalyzer.setProcessedImageReaderProxy(
                    processedImageReaderProxy);
        }

        @Override
        ListenableFuture<Void> analyzeImage(@NonNull ImageProxy imageProxy) {
            return mImageAnalysisNonBlockingAnalyzer.analyzeImage(imageProxy);
        }

        @Override
        void setOnePixelShiftEnabled(boolean onePixelShiftEnabled) {
            mImageAnalysisNonBlockingAnalyzer.setOnePixelShiftEnabled(onePixelShiftEnabled);
        }

        @Override
        void setOutputImageRotationEnabled(boolean outputImageRotationEnabled) {
            mImageAnalysisNonBlockingAnalyzer.setOutputImageRotationEnabled(
                    outputImageRotationEnabled);
        }

        @Override
        void setSensorToBufferTransformMatrix(@NonNull Matrix sensorToBufferTransformMatrix) {
            mImageAnalysisNonBlockingAnalyzer.setSensorToBufferTransformMatrix(
                    sensorToBufferTransformMatrix);
        }

        @Override
        void setRelativeRotation(int relativeRotation) {
            mImageAnalysisNonBlockingAnalyzer.setRelativeRotation(relativeRotation);
        }

        @Nullable ByteBuffer getRGBConverterBuffer() {
            return mImageAnalysisNonBlockingAnalyzer.mRGBConvertedBuffer;
        }

        @Nullable ByteBuffer getYRotatedBuffer() {
            return mImageAnalysisNonBlockingAnalyzer.mYRotatedBuffer;
        }

        @Nullable ByteBuffer getURotatedBuffer() {
            return mImageAnalysisNonBlockingAnalyzer.mURotatedBuffer;
        }

        @Nullable ByteBuffer getVRotatedBuffer() {
            return mImageAnalysisNonBlockingAnalyzer.mVRotatedBuffer;
        }

        @Nullable ByteBuffer getNV21YDelegatedBuffer() {
            return mImageAnalysisNonBlockingAnalyzer.mNV21YDelegatedBuffer;
        }

        @Nullable ByteBuffer getNV21UVDelegatedBuffer() {
            return mImageAnalysisNonBlockingAnalyzer.mNV21UVDelegatedBuffer;
        }
    }
}
