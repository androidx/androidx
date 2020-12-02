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

import static androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageAnalysis.BackpressureStrategy;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ImageAnalysisTest {
    private static final Size GUARANTEED_RESOLUTION = new Size(640, 480);
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final Object mAnalysisResultLock = new Object();
    @GuardedBy("mAnalysisResultLock")
    private Set<ImageProperties> mAnalysisResults;
    private Analyzer mAnalyzer;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Semaphore mAnalysisResultsSemaphore;
    private CameraSelector mCameraSelector;
    private Context mContext;
    private CameraUseCaseAdapter mCamera;

    @Rule
    public TestRule mCameraRule = CameraUtil.grantCameraPermissionAndPreTest();

    @Before
    public void setUp() {
        synchronized (mAnalysisResultLock) {
            mAnalysisResults = new HashSet<>();
        }
        mAnalysisResultsSemaphore = new Semaphore(/*permits=*/ 0);
        mAnalyzer =
                (image) -> {
                    synchronized (mAnalysisResultLock) {
                        mAnalysisResults.add(new ImageProperties(image,
                                image.getImageInfo().getRotationDegrees()));
                    }
                    mAnalysisResultsSemaphore.release();
                    image.close();
                };
        mContext = ApplicationProvider.getApplicationContext();
        CameraXConfig config = Camera2Config.defaultConfig();

        CameraX.initialize(mContext, config);

        mHandlerThread = new HandlerThread("AnalysisThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCameraSelector = new CameraSelector.Builder().requireLensFacing(
                CameraSelector.LENS_FACING_BACK).build();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (mCamera != null) {
            mInstrumentation.runOnMainSync(() ->
                    //TODO: The removeUseCases() call might be removed after clarifying the
                    // abortCaptures() issue in b/162314023.
                    mCamera.removeUseCases(mCamera.getUseCases())
            );
        }

        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);

        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
    }

    @Test
    public void canSupportGuaranteedSizeFront()
            throws InterruptedException, CameraInfoUnavailableException {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        assumeTrue(!CameraUtil.requiresCorrectedAspectRatio(CameraSelector.LENS_FACING_FRONT));

        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // the exactly matching result size 640x480 can be selected if the device supports it.
        Integer sensorOrientation = CameraUtil.getSensorOrientation(
                CameraSelector.LENS_FACING_FRONT);
        boolean isRotateNeeded = (sensorOrientation % 180) != 0;
        ImageAnalysis useCase = new ImageAnalysis.Builder().setTargetResolution(
                GUARANTEED_RESOLUTION).setTargetRotation(
                isRotateNeeded ? Surface.ROTATION_90 : Surface.ROTATION_0).build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_FRONT_CAMERA, useCase);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);
        assertThat(mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();

        synchronized (mAnalysisResultLock) {
            // Check the analyzed image exactly matches 640x480 size. This test can also check
            // whether the guaranteed resolution 640x480 is really supported for YUV_420_888
            // format on the devices when running the test.
            assertThat(GUARANTEED_RESOLUTION).isEqualTo(
                    mAnalysisResults.iterator().next().mResolution);
        }
    }

    @Test
    public void canSupportGuaranteedSizeBack()
            throws InterruptedException, CameraInfoUnavailableException {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));
        assumeTrue(!CameraUtil.requiresCorrectedAspectRatio(CameraSelector.LENS_FACING_BACK));

        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // the exactly matching result size 640x480 can be selected if the device supports it.
        Integer sensorOrientation = CameraUtil.getSensorOrientation(
                CameraSelector.LENS_FACING_BACK);
        boolean isRotateNeeded = (sensorOrientation % 180) != 0;
        ImageAnalysis useCase = new ImageAnalysis.Builder().setTargetResolution(
                GUARANTEED_RESOLUTION).setTargetRotation(
                isRotateNeeded ? Surface.ROTATION_90 : Surface.ROTATION_0).build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_BACK_CAMERA, useCase);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);

        assertThat(mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();

        synchronized (mAnalysisResultLock) {
            // Check the analyzed image exactly matches 640x480 size. This test can also check
            // whether the guaranteed resolution 640x480 is really supported for YUV_420_888
            // format on the devices when running the test.
            assertThat(GUARANTEED_RESOLUTION).isEqualTo(
                    mAnalysisResults.iterator().next().mResolution);
        }
    }

    @Test
    public void analyzesImages_withKEEP_ONLY_LATEST_whenCameraIsOpen()
            throws InterruptedException {
        analyzerAnalyzesImagesWithStrategy(STRATEGY_KEEP_ONLY_LATEST);
    }

    @Test
    public void analyzesImages_withBLOCK_PRODUCER_whenCameraIsOpen()
            throws InterruptedException {
        analyzerAnalyzesImagesWithStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER);
    }

    private void analyzerAnalyzesImagesWithStrategy(@BackpressureStrategy int backpressureStrategy)
            throws InterruptedException {
        ImageAnalysis useCase = new ImageAnalysis.Builder().setBackpressureStrategy(
                backpressureStrategy).build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);

        mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS);

        synchronized (mAnalysisResultLock) {
            assertThat(mAnalysisResults).isNotEmpty();
        }
    }

    @Test
    public void analyzerDoesNotAnalyzeImages_whenCameraIsNotOpen() throws InterruptedException {
        ImageAnalysis useCase = new ImageAnalysis.Builder().build();
        // Bind but do not start lifecycle
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);
        mCamera.detachUseCases();

        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);
        // Keep the lifecycle in an inactive state.
        // Wait a little while for frames to be analyzed.
        mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS);

        // No frames should have been analyzed.
        synchronized (mAnalysisResultLock) {
            assertThat(mAnalysisResults).isEmpty();
        }
    }

    @Test
    public void canObtainDefaultBackpressureStrategy() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        assertThat(imageAnalysis.getBackpressureStrategy()).isEqualTo(STRATEGY_KEEP_ONLY_LATEST);
    }

    @Test
    public void canObtainDefaultImageQueueDepth() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        // Should not be less than 1
        assertThat(imageAnalysis.getImageQueueDepth()).isAtLeast(1);
    }

    @Test
    public void defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() {
        ImageAnalysis useCase = new ImageAnalysis.Builder().build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);
        ImageOutputConfig config = (ImageOutputConfig) useCase.getCurrentConfig();
        assertThat(config.getTargetAspectRatio()).isEqualTo(AspectRatio.RATIO_4_3);
    }

    @Test
    public void defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));
        ImageAnalysis useCase = new ImageAnalysis.Builder().setTargetResolution(
                GUARANTEED_RESOLUTION).build();

        assertThat(useCase.getCurrentConfig().containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)).isFalse();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_BACK_CAMERA, useCase);

        assertThat(useCase.getCurrentConfig().containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)).isFalse();
    }

    @Test
    public void targetRotationCanBeUpdatedAfterUseCaseIsCreated() {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetRotation(Surface.ROTATION_0).build();
        imageAnalysis.setTargetRotation(Surface.ROTATION_90);

        assertThat(imageAnalysis.getTargetRotation()).isEqualTo(Surface.ROTATION_90);
    }

    @Test
    public void targetResolutionIsUpdatedAfterTargetRotationIsUpdated() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(
                GUARANTEED_RESOLUTION).setTargetRotation(Surface.ROTATION_0).build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageAnalysis);

        // Updates target rotation from ROTATION_0 to ROTATION_90.
        imageAnalysis.setTargetRotation(Surface.ROTATION_90);

        ImageOutputConfig newConfig = (ImageOutputConfig) imageAnalysis.getCurrentConfig();
        Size expectedTargetResolution = new Size(GUARANTEED_RESOLUTION.getHeight(),
                GUARANTEED_RESOLUTION.getWidth());

        // Expected targetResolution will be reversed from original target resolution.
        assertThat(newConfig.getTargetResolution().equals(expectedTargetResolution)).isTrue();
    }

    // TODO(b/162298517): change the test to be deterministic instead of depend upon timing.
    @Test
    public void analyzerSetMultipleTimesInKeepOnlyLatestMode() throws Exception {
        ImageAnalysis useCase = new ImageAnalysis.Builder().setBackpressureStrategy(
                STRATEGY_KEEP_ONLY_LATEST).build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);

        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);
        mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS);

        Analyzer slowAnalyzer = image -> {
            try {
                Thread.sleep(200);
                image.close();
            } catch (Exception e) {
            }
        };
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), slowAnalyzer);

        Thread.sleep(100);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), slowAnalyzer);

        Thread.sleep(100);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), slowAnalyzer);

        Thread.sleep(100);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), slowAnalyzer);

        Thread.sleep(100);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), slowAnalyzer);

        Thread.sleep(100);
    }

    @Test
    public void useCaseConfigCanBeReset_afterUnbind() {
        final ImageAnalysis useCase = new ImageAnalysis.Builder().build();
        UseCaseConfig<?> initialConfig = useCase.getCurrentConfig();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);

        mInstrumentation.runOnMainSync(() -> {
            mCamera.removeUseCases(Collections.singleton(useCase));
        });

        UseCaseConfig<?> configAfterUnbinding = useCase.getCurrentConfig();
        assertThat(initialConfig.equals(configAfterUnbinding)).isTrue();
    }

    @Test
    public void targetRotationIsRetained_whenUseCaseIsReused() {
        ImageAnalysis useCase = new ImageAnalysis.Builder().build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);

        // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
        // use it to do the test.
        useCase.setTargetRotation(Surface.ROTATION_180);

        mInstrumentation.runOnMainSync(() -> {
            // Check the target rotation is kept when the use case is unbound.
            mCamera.removeUseCases(Collections.singleton(useCase));
            assertThat(useCase.getTargetRotation()).isEqualTo(Surface.ROTATION_180);
        });

        // Check the target rotation is kept when the use case is rebound to the
        // lifecycle.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);
        assertThat(useCase.getTargetRotation()).isEqualTo(Surface.ROTATION_180);
    }

    @Test
    public void useCaseCanBeReusedInSameCamera() throws InterruptedException {
        ImageAnalysis useCase = new ImageAnalysis.Builder().build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);

        assertThat(mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();

        mInstrumentation.runOnMainSync(() -> {
            mCamera.removeUseCases(Collections.singleton(useCase));
        });

        mAnalysisResultsSemaphore = new Semaphore(/*permits=*/ 0);
        // Rebind the use case to the same camera.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);

        assertThat(mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void useCaseCanBeReusedInDifferentCamera() throws InterruptedException {
        ImageAnalysis useCase = new ImageAnalysis.Builder().build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_BACK_CAMERA, useCase);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);

        assertThat(mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();

        mInstrumentation.runOnMainSync(() -> {
            mCamera.removeUseCases(Collections.singleton(useCase));
        });

        mAnalysisResultsSemaphore = new Semaphore(/*permits=*/ 0);
        // Rebind the use case to different camera.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_FRONT_CAMERA, useCase);

        assertThat(mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void returnValidTargetRotation_afterUseCaseIsCreated() {
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        assertThat(imageCapture.getTargetRotation()).isNotEqualTo(
                ImageOutputConfig.INVALID_ROTATION);
    }

    @Test
    public void returnCorrectTargetRotation_afterUseCaseIsAttached() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetRotation(
                Surface.ROTATION_180).build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageAnalysis);
        assertThat(imageAnalysis.getTargetRotation()).isEqualTo(Surface.ROTATION_180);
    }

    private static class ImageProperties {
        final Size mResolution;
        final int mFormat;
        final long mTimestamp;
        final int mRotationDegrees;

        ImageProperties(ImageProxy image, int rotationDegrees) {
            this.mResolution = new Size(image.getWidth(), image.getHeight());
            this.mFormat = image.getFormat();
            this.mTimestamp = image.getImageInfo().getTimestamp();
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
