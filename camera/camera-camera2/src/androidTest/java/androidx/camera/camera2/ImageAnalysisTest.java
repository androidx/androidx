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

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageAnalysis.BackpressureStrategy;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ImageAnalysisTest {
    private static final Size GUARANTEED_RESOLUTION = new Size(640, 480);
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final ImageAnalysisConfig mDefaultConfig = ImageAnalysis.DEFAULT_CONFIG.getConfig(null);
    private final Object mAnalysisResultLock = new Object();
    @GuardedBy("mAnalysisResultLock")
    private Set<ImageProperties> mAnalysisResults;
    private Analyzer mAnalyzer;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Semaphore mAnalysisResultsSemaphore;
    private CameraSelector mCameraSelector;
    private FakeLifecycleOwner mLifecycleOwner;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
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
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig config = Camera2Config.defaultConfig();

        CameraX.initialize(context, config);

        mHandlerThread = new HandlerThread("AnalysisThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCameraSelector = new CameraSelector.Builder().requireLensFacing(
                CameraSelector.LENS_FACING_BACK).build();
        mLifecycleOwner = new FakeLifecycleOwner();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);
        }

        CameraX.shutdown().get();

        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
    }

    @Test
    public void canSupportGuaranteedSize()
            throws InterruptedException, CameraInfoUnavailableException {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        for (int i = 0; i <= 1; i++) {
            final int lensFacing = i;
            if (!CameraUtil.hasCameraWithLensFacing(lensFacing)) {
                continue;
            }

            // Checks camera device sensor degrees to set correct target rotation value to make sure
            // the exactly matching result size 640x480 can be selected if the device supports it.
            Integer sensorOrientation = CameraUtil.getSensorOrientation(
                    CameraSelector.LENS_FACING_BACK);
            boolean isRotateNeeded = (sensorOrientation % 180) != 0;
            ImageAnalysis useCase = new ImageAnalysis.Builder().setTargetResolution(
                    GUARANTEED_RESOLUTION).setTargetRotation(
                    isRotateNeeded ? Surface.ROTATION_90 : Surface.ROTATION_0).build();
            mInstrumentation.runOnMainSync(() -> {
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(
                        lensFacing).build();
                CameraX.bindToLifecycle(mLifecycleOwner, cameraSelector, useCase);
                mLifecycleOwner.startAndResume();
                useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);
            });

            assertThat(mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();

            synchronized (mAnalysisResultLock) {
                // Check the analyzed image exactly matches 640x480 size. This test can also check
                // whether the guaranteed resolution 640x480 is really supported for YUV_420_888
                // format on the devices when running the test.
                assertThat(GUARANTEED_RESOLUTION).isEqualTo(
                        mAnalysisResults.iterator().next().mResolution);
            }

            // Reset the environment to run test for the other lens facing camera device.
            mInstrumentation.runOnMainSync(() -> {
                CameraX.unbindAll();
                mLifecycleOwner.pauseAndStop();
            });
        }
    }

    @Test
    public void analyzesImages_withKEEP_ONLY_LATEST_whenCameraIsOpen()
            throws InterruptedException {
        analyzerAnalyzesImagesWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);
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
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, useCase);
            mLifecycleOwner.startAndResume();
            useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mAnalyzer);
        });

        mAnalysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS);

        synchronized (mAnalysisResultLock) {
            assertThat(mAnalysisResults).isNotEmpty();
        }
    }

    @Test
    @UiThreadTest
    public void analyzerDoesNotAnalyzeImages_whenCameraIsNotOpen() throws InterruptedException {
        ImageAnalysis useCase = new ImageAnalysis.Builder().build();
        // Bind but do not start lifecycle
        CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, useCase);
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
    public void defaultsIncludeBackpressureStrategy() {
        ImageAnalysisConfig defaultConfig = ImageAnalysis.DEFAULT_CONFIG.getConfig(null);

        // Will throw if strategy does not exist
        @BackpressureStrategy int strategy = defaultConfig.getBackpressureStrategy();

        // Should not be null
        assertThat(strategy).isNotNull();
    }

    @Test
    public void defaultsIncludeImageQueueDepth() {
        ImageAnalysisConfig defaultConfig = ImageAnalysis.DEFAULT_CONFIG.getConfig(null);

        // Will throw if depth does not exist
        int depth = defaultConfig.getImageQueueDepth();

        // Should not be less than 1
        assertThat(depth).isAtLeast(1);
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
        // setTargetResolution will also set corresponding value to targetAspectRatioCustom.
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(
                GUARANTEED_RESOLUTION).setTargetRotation(Surface.ROTATION_0).build();

        // Updates target rotation from ROTATION_0 to ROTATION_90.
        imageAnalysis.setTargetRotation(Surface.ROTATION_90);

        ImageOutputConfig newConfig = (ImageOutputConfig) imageAnalysis.getUseCaseConfig();
        Size expectedTargetResolution = new Size(GUARANTEED_RESOLUTION.getHeight(),
                GUARANTEED_RESOLUTION.getWidth());
        Rational expectedTargetAspectRatioCustom = new Rational(GUARANTEED_RESOLUTION.getHeight(),
                GUARANTEED_RESOLUTION.getWidth());

        // Expected targetResolution and targetAspectRatioCustom will be reversed from original
        // target resolution.
        assertThat(newConfig.getTargetResolution().equals(expectedTargetResolution)).isTrue();
        assertThat(newConfig.getTargetAspectRatioCustom().equals(
                expectedTargetAspectRatioCustom)).isTrue();
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
