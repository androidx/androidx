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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.annotation.GuardedBy;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysis.Analyzer;
import androidx.camera.core.ImageAnalysis.BackpressureStrategy;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.UseCase.StateChangeCallback;
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
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ImageAnalysisTest {
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final ImageAnalysisConfig mDefaultConfig = ImageAnalysis.DEFAULT_CONFIG.getConfig(null);
    private final StateChangeCallback mMockCallback = Mockito.mock(StateChangeCallback.class);
    private final Analyzer mMockAnalyzer = Mockito.mock(Analyzer.class);
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
                (image, rotationDegrees) -> {
                    synchronized (mAnalysisResultLock) {
                        mAnalysisResults.add(new ImageProperties(image, rotationDegrees));
                    }
                    mAnalysisResultsSemaphore.release();
                    image.close();
                };
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig config = Camera2Config.defaultConfig(context);

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
    @UiThreadTest
    public void becomesActive_whenHasAnalyzer() {
        ImageAnalysis useCase = ImageAnalysis.Builder.fromConfig(mDefaultConfig).build();
        CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, useCase);
        mLifecycleOwner.startAndResume();

        useCase.addStateChangeCallback(mMockCallback);

        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mMockAnalyzer);

        verify(mMockCallback, times(1)).onUseCaseActive(useCase);
    }

    @Test
    @UiThreadTest
    public void becomesInactive_whenNoAnalyzer() {
        ImageAnalysis useCase = ImageAnalysis.Builder.fromConfig(mDefaultConfig).build();
        CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, useCase);
        mLifecycleOwner.startAndResume();
        useCase.addStateChangeCallback(mMockCallback);
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(mHandler), mMockAnalyzer);
        useCase.clearAnalyzer();

        verify(mMockCallback, times(1)).onUseCaseInactive(useCase);
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
