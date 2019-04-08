/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.ImageFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.AppConfig;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageAnalysis.ImageReaderMode;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.UseCase.StateChangeListener;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ImageAnalysisTest {
    // Use most supported resolution for different supported hardware level devices,
    // especially for legacy devices.
    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);
    private static final Size SECONDARY_RESOLUTION = new Size(320, 240);
    private final ImageAnalysisConfig mDefaultConfig = ImageAnalysis.DEFAULT_CONFIG.getConfig(null);
    private final StateChangeListener mMockListener = Mockito.mock(StateChangeListener.class);
    private final Analyzer mMockAnalyzer = Mockito.mock(Analyzer.class);
    private Set<ImageProperties> mAnalysisResults;
    private Analyzer mAnalyzer;
    private BaseCamera mCamera;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Semaphore mAnalysisResultsSemaphore;
    private String mCameraId;

    @Before
    public void setUp() {
        mAnalysisResults = new HashSet<>();
        mAnalysisResultsSemaphore = new Semaphore(/*permits=*/ 0);
        mAnalyzer =
                new Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        mAnalysisResults.add(new ImageProperties(image, rotationDegrees));
                        mAnalysisResultsSemaphore.release();
                    }
                };
        Context context = ApplicationProvider.getApplicationContext();
        AppConfig config = Camera2AppConfig.create(context);
        CameraFactory cameraFactory = config.getCameraFactory(/*valueIfMissing=*/ null);
        try {
            mCameraId = cameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + LensFacing.BACK, e);
        }
        mCamera = cameraFactory.getCamera(mCameraId);

        CameraX.init(context, config);

        mHandlerThread = new HandlerThread("AnalysisThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @After
    public void tearDown() {
        mHandlerThread.quitSafely();
        mCamera.release();
    }

    @Test
    public void analyzerCanBeSetAndRetrieved() {
        ImageAnalysis useCase = new ImageAnalysis(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);

        Analyzer initialAnalyzer = useCase.getAnalyzer();

        useCase.setAnalyzer(mMockAnalyzer);

        Analyzer retrievedAnalyzer = useCase.getAnalyzer();

        // The observer is bound to the lifecycle.
        assertThat(initialAnalyzer).isNull();
        assertThat(retrievedAnalyzer).isSameAs(mMockAnalyzer);
    }

    @Test
    public void becomesActive_whenHasAnalyzer() {
        ImageAnalysis useCase = new ImageAnalysis(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.addStateChangeListener(mMockListener);

        useCase.setAnalyzer(mMockAnalyzer);

        verify(mMockListener, times(1)).onUseCaseActive(useCase);
    }

    @Test
    public void becomesInactive_whenNoAnalyzer() {
        ImageAnalysis useCase = new ImageAnalysis(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.addStateChangeListener(mMockListener);
        useCase.setAnalyzer(mMockAnalyzer);
        useCase.removeAnalyzer();

        verify(mMockListener, times(1)).onUseCaseInactive(useCase);
    }

    @Test
    public void analyzerAnalyzesImages_whenCameraIsOpen()
            throws InterruptedException, CameraInfoUnavailableException {
        final int imageFormat = ImageFormat.YUV_420_888;
        ImageAnalysisConfig config =
                new ImageAnalysisConfig.Builder().setCallbackHandler(mHandler).build();
        ImageAnalysis useCase = new ImageAnalysis(config);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase);
        useCase.setAnalyzer(mAnalyzer);

        int sensorRotation = CameraX.getCameraInfo(mCameraId).getSensorRotationDegrees();
        // The frames should have properties which match the configuration.
        for (ImageProperties properties : mAnalysisResults) {
            assertThat(properties.mResolution).isEqualTo(DEFAULT_RESOLUTION);
            assertThat(properties.mFormat).isEqualTo(imageFormat);
            assertThat(properties.mRotationDegrees).isEqualTo(sensorRotation);
        }
    }

    @Test
    public void analyzerDoesNotAnalyzeImages_whenCameraIsNotOpen() throws InterruptedException {
        ImageAnalysisConfig config =
                new ImageAnalysisConfig.Builder().setCallbackHandler(mHandler).build();
        ImageAnalysis useCase = new ImageAnalysis(config);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.setAnalyzer(mAnalyzer);
        // Keep the lifecycle in an inactive state.
        // Wait a little while for frames to be analyzed.
        mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS);

        // No frames should have been analyzed.
        assertThat(mAnalysisResults).isEmpty();
    }

    @Test
    public void updateSessionConfigWithSuggestedResolution() throws InterruptedException {
        final int imageFormat = ImageFormat.YUV_420_888;
        final Size[] sizes = {SECONDARY_RESOLUTION, DEFAULT_RESOLUTION};

        ImageAnalysisConfig config =
                new ImageAnalysisConfig.Builder().setCallbackHandler(mHandler).build();
        ImageAnalysis useCase = new ImageAnalysis(config);
        useCase.setAnalyzer(mAnalyzer);

        for (Size size : sizes) {
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            suggestedResolutionMap.put(mCameraId, size);
            useCase.updateSuggestedResolution(suggestedResolutionMap);
            CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase);

            // Clear previous results
            mAnalysisResults.clear();
            // Wait a little while for frames to be analyzed.
            mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS);

            // The frames should have properties which match the configuration.
            for (ImageProperties properties : mAnalysisResults) {
                assertThat(properties.mResolution).isEqualTo(size);
                assertThat(properties.mFormat).isEqualTo(imageFormat);
            }

            // Detach use case from camera device to run next resolution setting
            CameraUtil.detachUseCaseFromCamera(mCamera, useCase);
        }
    }

    @Test
    public void defaultsIncludeImageReaderMode() {
        ImageAnalysisConfig defaultConfig = ImageAnalysis.DEFAULT_CONFIG.getConfig(null);

        // Will throw if mode does not exist
        ImageReaderMode mode = defaultConfig.getImageReaderMode();

        // Should not be null
        assertThat(mode).isNotNull();
    }

    @Test
    public void defaultsIncludeImageQueueDepth() {
        ImageAnalysisConfig defaultConfig = ImageAnalysis.DEFAULT_CONFIG.getConfig(null);

        // Will throw if depth does not exist
        int depth = defaultConfig.getImageQueueDepth();

        // Should not be less than 1
        assertThat(depth).isAtLeast(1);
    }

    private static class ImageProperties {
        final Size mResolution;
        final int mFormat;
        final long mTimestamp;
        final int mRotationDegrees;

        ImageProperties(ImageProxy image, int rotationDegrees) {
            this.mResolution = new Size(image.getWidth(), image.getHeight());
            this.mFormat = image.getFormat();
            this.mTimestamp = image.getTimestamp();
            this.mRotationDegrees = rotationDegrees;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null) {
                return false;
            }
            if (!(other instanceof ImageProperties)) {
                return false;
            }
            ImageProperties otherProperties = (ImageProperties) other;
            return mResolution.equals(otherProperties.mResolution)
                    && mFormat == otherProperties.mFormat
                    && otherProperties.mTimestamp == mTimestamp
                    && otherProperties.mRotationDegrees == mRotationDegrees;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + mResolution.getWidth();
            hash = 31 * hash + mResolution.getHeight();
            hash = 31 * hash + mFormat;
            hash = 31 * hash + (int) mTimestamp;
            hash = 31 * hash + mRotationDegrees;
            return hash;
        }
    }
}
