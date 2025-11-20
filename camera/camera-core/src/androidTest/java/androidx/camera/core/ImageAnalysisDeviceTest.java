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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

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
}
