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

import androidx.camera.core.AppConfiguration;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.BaseUseCase.StateChangeListener;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraUtil;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageAnalysisUseCase;
import androidx.camera.core.ImageAnalysisUseCase.Analyzer;
import androidx.camera.core.ImageAnalysisUseCase.ImageReaderMode;
import androidx.camera.core.ImageAnalysisUseCaseConfiguration;
import androidx.camera.core.ImageProxy;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

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

@RunWith(AndroidJUnit4.class)
public final class ImageAnalysisUseCaseAndroidTest {
    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);
    private final ImageAnalysisUseCaseConfiguration defaultConfiguration =
            ImageAnalysisUseCase.DEFAULT_CONFIG.getConfiguration();
    private final StateChangeListener mockListener = Mockito.mock(StateChangeListener.class);
    private final Analyzer mockAnalyzer = Mockito.mock(Analyzer.class);
    private Set<ImageProperties> analysisResults;
    private Analyzer analyzer;
    private BaseCamera camera;
    private HandlerThread handlerThread;
    private Handler handler;
    private Semaphore analysisResultsSemaphore;
    private String cameraId;

    @Before
    public void setUp() {
        analysisResults = new HashSet<>();
        analysisResultsSemaphore = new Semaphore(/*permits=*/ 0);
        analyzer =
                (image, rotationDegrees) -> {
                    analysisResults.add(new ImageProperties(image, rotationDegrees));
                    analysisResultsSemaphore.release();
                };
        Context context = ApplicationProvider.getApplicationContext();
        AppConfiguration config = Camera2AppConfiguration.create(context);
        CameraFactory cameraFactory = config.getCameraFactory(/*valueIfMissing=*/ null);
        try {
            cameraId = cameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + LensFacing.BACK, e);
        }
        camera = cameraFactory.getCamera(cameraId);

        CameraX.init(context, config);

        handlerThread = new HandlerThread("AnalysisThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @After
    public void tearDown() {
        handlerThread.quitSafely();
        camera.release();
    }

    @Test
    public void analyzerCanBeSetAndRetrieved() {
        ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);

        Analyzer initialAnalyzer = useCase.getAnalyzer();

        useCase.setAnalyzer(mockAnalyzer);

        Analyzer retrievedAnalyzer = useCase.getAnalyzer();

        // The observer is bound to the lifecycle.
        assertThat(initialAnalyzer).isNull();
        assertThat(retrievedAnalyzer).isSameAs(mockAnalyzer);
    }

    @Test
    public void becomesActive_whenHasAnalyzer() {
        ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.addStateChangeListener(mockListener);

        useCase.setAnalyzer(mockAnalyzer);

        verify(mockListener, times(1)).onUseCaseActive(useCase);
    }

    @Test
    public void becomesInactive_whenNoAnalyzer() {
        ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.addStateChangeListener(mockListener);
        useCase.setAnalyzer(mockAnalyzer);
        useCase.removeAnalyzer();

        verify(mockListener, times(1)).onUseCaseInactive(useCase);
    }

    @Test(timeout = 5000)
    public void analyzerAnalyzesImages_whenCameraIsOpen()
            throws InterruptedException, CameraInfoUnavailableException {
        final int imageFormat = ImageFormat.YUV_420_888;
        ImageAnalysisUseCaseConfiguration configuration =
                new ImageAnalysisUseCaseConfiguration.Builder().setCallbackHandler(handler).build();
        ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configuration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase);
        useCase.setAnalyzer(analyzer);

        int sensorRotation = CameraX.getCameraInfo(cameraId).getSensorRotationDegrees();
        // The frames should have properties which match the configuration.
        for (ImageProperties properties : analysisResults) {
            assertThat(properties.resolution).isEqualTo(DEFAULT_RESOLUTION);
            assertThat(properties.format).isEqualTo(imageFormat);
            assertThat(properties.rotationDegrees).isEqualTo(sensorRotation);
        }
    }

    @Test
    public void analyzerDoesNotAnalyzeImages_whenCameraIsNotOpen() throws InterruptedException {
        ImageAnalysisUseCaseConfiguration configuration =
                new ImageAnalysisUseCaseConfiguration.Builder().setCallbackHandler(handler).build();
        ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configuration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.setAnalyzer(analyzer);
        // Keep the lifecycle in an inactive state.
        // Wait a little while for frames to be analyzed.
        analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS);

        // No frames should have been analyzed.
        assertThat(analysisResults).isEmpty();
    }

    @Test(timeout = 5000)
    public void updateSessionConfigurationWithSuggestedResolution() throws InterruptedException {
        final int imageFormat = ImageFormat.YUV_420_888;
        final Size[] sizes = {new Size(1280, 720), new Size(640, 480)};

        ImageAnalysisUseCaseConfiguration configuration =
                new ImageAnalysisUseCaseConfiguration.Builder().setCallbackHandler(handler).build();
        ImageAnalysisUseCase useCase = new ImageAnalysisUseCase(configuration);
        useCase.setAnalyzer(analyzer);

        for (Size size : sizes) {
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            suggestedResolutionMap.put(cameraId, size);
            useCase.updateSuggestedResolution(suggestedResolutionMap);
            CameraUtil.openCameraWithUseCase(camera, useCase);

            // Clear previous results
            analysisResults.clear();
            // Wait a little while for frames to be analyzed.
            analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS);

            // The frames should have properties which match the configuration.
            for (ImageProperties properties : analysisResults) {
                assertThat(properties.resolution).isEqualTo(size);
                assertThat(properties.format).isEqualTo(imageFormat);
            }

            // Detach use case from camera device to run next resolution setting
            CameraUtil.detachUseCaseFromCamera(camera, useCase);
        }
    }

    @Test
    public void defaultsIncludeImageReaderMode() {
        ImageAnalysisUseCaseConfiguration defaultConfig =
                ImageAnalysisUseCase.DEFAULT_CONFIG.getConfiguration();

        // Will throw if mode does not exist
        ImageReaderMode mode = defaultConfig.getImageReaderMode();

        // Should not be null
        assertThat(mode).isNotNull();
    }

    @Test
    public void defaultsIncludeImageQueueDepth() {
        ImageAnalysisUseCaseConfiguration defaultConfig =
                ImageAnalysisUseCase.DEFAULT_CONFIG.getConfiguration();

        // Will throw if depth does not exist
        int depth = defaultConfig.getImageQueueDepth();

        // Should not be less than 1
        assertThat(depth).isAtLeast(1);
    }

    private static class ImageProperties {
        final Size resolution;
        final int format;
        final long timestamp;
        final int rotationDegrees;

        ImageProperties(ImageProxy image, int rotationDegrees) {
            this.resolution = new Size(image.getWidth(), image.getHeight());
            this.format = image.getFormat();
            this.timestamp = image.getTimestamp();
            this.rotationDegrees = rotationDegrees;
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
            return resolution.equals(otherProperties.resolution)
                    && format == otherProperties.format
                    && otherProperties.timestamp == timestamp
                    && otherProperties.rotationDegrees == rotationDegrees;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + resolution.getWidth();
            hash = 31 * hash + resolution.getHeight();
            hash = 31 * hash + format;
            hash = 31 * hash + (int) timestamp;
            hash = 31 * hash + rotationDegrees;
            return hash;
        }
    }
}
