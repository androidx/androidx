/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST;
import static androidx.camera.testing.impl.TestImageUtil.createYuvFakeImageProxy;
import static androidx.camera.testing.impl.TestImageUtil.getAverageDiff;
import static androidx.camera.testing.impl.TestImageUtil.rotateBitmap;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.impl.ImageProxyUtil;
import androidx.camera.testing.impl.fakes.FakeImageInfo;
import androidx.camera.testing.impl.fakes.FakeImageProxy;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ImageAnalysisDeviceTest {

    private final CameraInternal mMockCameraInternal = mock(CameraInternal.class);
    private final ImageAnalysis.Analyzer mMockAnalyzer = mock(ImageAnalysis.Analyzer.class);

    @Test
    @UiThreadTest
    public void becomesActive_whenHasAnalyzer() {
        when(mMockCameraInternal.getCameraInfoInternal()).thenReturn(new FakeCameraInfoInternal());
        ImageAnalysis useCase = new ImageAnalysis.Builder().setBackpressureStrategy(
                STRATEGY_KEEP_ONLY_LATEST).build();

        useCase.bindToCamera(mMockCameraInternal, null, null, null);

        useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mMockAnalyzer);

        verify(mMockCameraInternal, times(1)).onUseCaseActive(useCase);
    }

    @Test
    @UiThreadTest
    public void becomesInactive_whenNoAnalyzer() {
        when(mMockCameraInternal.getCameraInfoInternal()).thenReturn(new FakeCameraInfoInternal());
        ImageAnalysis useCase = new ImageAnalysis.Builder().setBackpressureStrategy(
                STRATEGY_KEEP_ONLY_LATEST).build();

        useCase.bindToCamera(mMockCameraInternal, null, null, null);
        useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mMockAnalyzer);
        useCase.clearAnalyzer();

        verify(mMockCameraInternal, times(1)).onUseCaseInactive(useCase);
    }

    @Test
    public void rotationWorksWhenChangingRotationFrom0To180()
            throws Exception {
        // 1. Create an ImageAnalysis UseCase with OutputRotationEnabled.
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageRotationEnabled(true)
                .setSessionOptionUnpacker((resolution2, config, builder) -> {
                })
                .setCaptureOptionUnpacker((config, builder) -> {
                })
                .build();

        // 2. Invoke bindToCamera with a FakeCamera that will make initial relative rotation be 0
        FakeCameraInfoInternal cameraInfoInternal = new FakeCameraInfoInternal("0", 0,
                CameraSelector.LENS_FACING_BACK);
        FakeCamera fakeCamera = new FakeCamera("0", null, cameraInfoInternal);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysis.bindToCamera(fakeCamera, null, null, null);
        });

        // 3. Invoke onSuggestedStreamSpecUpdated with proper resolution to create pipeline
        Size resolution = new Size(8, 4);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysis.onSuggestedStreamSpecUpdated(StreamSpec.builder(resolution).build(),
                    null);
        });

        // 4. Set an Analyzer to the ImageAnalysis
        imageAnalysis.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mMockAnalyzer);

        // 5. Extract the mImageAnalysisAbstractAnalyzer
        ImageAnalysisAbstractAnalyzer abstractAnalyzer =
                imageAnalysis.mImageAnalysisAbstractAnalyzer;

        FakeImageInfo fakeImageInfo = new FakeImageInfo();
        FakeImageProxy imageProxy = createYuvFakeImageProxy(fakeImageInfo, resolution.getWidth(),
                resolution.getHeight(), ImageProxyUtil.YUV_FORMAT_PLANE_DATA_TYPE_I420, true);

        // 6. Verify first analysis diff
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        Bitmap expectedBitmap = ImageUtil.createBitmapFromImageProxy(imageProxy);
        verifyAnalysis(abstractAnalyzer, imageProxy, expectedBitmap, imageProxyArgumentCaptor);

        // 7. Change target rotation of ImageAnalysis to 180
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysis.setTargetRotation(Surface.ROTATION_180);
        });

        // 8. Invoke analyzeImage with the color blocks ImageProxy again.
        FakeImageProxy imageProxy2 = createYuvFakeImageProxy(fakeImageInfo,
                resolution.getWidth(), resolution.getHeight(),
                ImageProxyUtil.YUV_FORMAT_PLANE_DATA_TYPE_I420, true);

        // 9. Verify second analysis diff
        Bitmap expectedBitmap2 = rotateBitmap(expectedBitmap, 180);
        verifyAnalysis(abstractAnalyzer, imageProxy2, expectedBitmap2, imageProxyArgumentCaptor);
    }

    private void verifyAnalysis(
            ImageAnalysisAbstractAnalyzer analyzer,
            ImageProxy imageToAnalyze,
            Bitmap expectedBitmap,
            ArgumentCaptor<ImageProxy> captor) throws Exception {
        analyzer.analyzeImage(imageToAnalyze).get(1, TimeUnit.SECONDS);
        verify(mMockAnalyzer).analyze(captor.capture());

        Bitmap resultBitmap = ImageUtil.createBitmapFromImageProxy(
                captor.getValue());

        assertThat(getAverageDiff(resultBitmap, expectedBitmap)).isEqualTo(0);
        reset(mMockAnalyzer);
    }
}
