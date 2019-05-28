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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.camera.camera2.SemaphoreReleasingCamera2Callbacks.DeviceStateCallback;
import androidx.camera.camera2.SemaphoreReleasingCamera2Callbacks.SessionCaptureCallback;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains tests for {@link androidx.camera.core.CameraX} which require an actual implementation to
 * run.
 */
@FlakyTest
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class Camera2ImplCameraXTest {
    private static final LensFacing DEFAULT_LENS_FACING = LensFacing.BACK;
    private final MutableLiveData<Long> mAnalysisResult = new MutableLiveData<>();
    private final ImageAnalysis.Analyzer mImageAnalyzer =
            new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(ImageProxy image, int rotationDegrees) {
                    mAnalysisResult.postValue(image.getTimestamp());
                }
            };
    private FakeLifecycleOwner mLifecycle;
    private HandlerThread mHandlerThread;
    private Handler mMainThreadHandler;

    private CameraDevice.StateCallback mMockStateCallback;

    private static Observer<Long> createCountIncrementingObserver(final AtomicLong counter) {
        return new Observer<Long>() {
            @Override
            public void onChanged(Long value) {
                counter.incrementAndGet();
            }
        };
    }

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        CameraX.init(context, Camera2AppConfig.create(context));
        mLifecycle = new FakeLifecycleOwner();
        mHandlerThread = new HandlerThread("ErrorHandlerThread");
        mHandlerThread.start();
        mMainThreadHandler = new Handler(Looper.getMainLooper());
        mMockStateCallback = Mockito.mock(CameraDevice.StateCallback.class);
    }

    @After
    public void tearDown() throws InterruptedException {
        CameraX.unbindAll();
        mHandlerThread.quitSafely();

        // Wait some time for the cameras to close. We need the cameras to close to bring CameraX
        // back to the initial state.
        Thread.sleep(3000);
    }

    @Test
    public void lifecycleResume_opensCameraAndStreamsFrames() throws InterruptedException {
        final AtomicLong observedCount = new AtomicLong(0);
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ImageAnalysisConfig config =
                        new ImageAnalysisConfig.Builder()
                                .setLensFacing(DEFAULT_LENS_FACING)
                                .build();
                ImageAnalysis useCase = new ImageAnalysis(config);
                CameraX.bindToLifecycle(mLifecycle, useCase);

                useCase.setAnalyzer(mImageAnalyzer);
                mAnalysisResult.observe(mLifecycle, createCountIncrementingObserver(observedCount));

                mLifecycle.startAndResume();
            }
        });

        // Wait a little bit for the camera to open and stream frames.
        Thread.sleep(5000);

        // Some frames should have been observed.
        assertThat(observedCount.get()).isAtLeast(10L);
    }

    @Test
    public void removedUseCase_doesNotStreamWhenLifecycleResumes() throws InterruptedException {
        final AtomicLong observedCount = new AtomicLong(0);
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ImageAnalysisConfig config =
                        new ImageAnalysisConfig.Builder()
                                .setLensFacing(DEFAULT_LENS_FACING)
                                .build();
                ImageAnalysis useCase = new ImageAnalysis(config);
                CameraX.bindToLifecycle(mLifecycle, useCase);
                useCase.setAnalyzer(mImageAnalyzer);
                mAnalysisResult.observe(mLifecycle, createCountIncrementingObserver(observedCount));
                assertThat(observedCount.get()).isEqualTo(0);

                CameraX.unbind(useCase);

                mLifecycle.startAndResume();
            }
        });

        // Wait a little bit for the camera to open and stream frames.
        Thread.sleep(5000);

        // No frames should have been observed.
        assertThat(observedCount.get()).isEqualTo(0);
    }

    @Test
    public void lifecyclePause_closesCameraAndStopsStreamingFrames() throws InterruptedException {
        final AtomicLong observedCount = new AtomicLong(0);
        final SessionCaptureCallback sessionCaptureCallback = new SessionCaptureCallback();
        final DeviceStateCallback deviceStateCallback = new DeviceStateCallback();
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ImageAnalysisConfig.Builder configBuilder =
                        new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
                new Camera2Config.Extender(configBuilder)
                        .setDeviceStateCallback(deviceStateCallback)
                        .setSessionCaptureCallback(sessionCaptureCallback);
                ImageAnalysis useCase = new ImageAnalysis(configBuilder.build());
                CameraX.bindToLifecycle(mLifecycle, useCase);
                useCase.setAnalyzer(mImageAnalyzer);
                mAnalysisResult.observe(mLifecycle, createCountIncrementingObserver(observedCount));

                mLifecycle.startAndResume();
            }
        });

        // Wait a little bit for the camera to open and stream frames.
        sessionCaptureCallback.waitForOnCaptureCompleted(5);

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mLifecycle.pauseAndStop();
            }
        });

        // Wait a little bit for the camera to close.
        deviceStateCallback.waitForOnClosed(1);

        final Long firstObservedCount = observedCount.get();
        assertThat(firstObservedCount).isGreaterThan(1L);

        // Stay in idle state for a while.
        Thread.sleep(5000);

        // Additional frames should not be observed.
        final Long secondObservedCount = observedCount.get();
        assertThat(secondObservedCount).isEqualTo(firstObservedCount);
    }

    @Test
    public void bind_opensCamera() {
        ImageAnalysisConfig.Builder builder =
                new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
        new Camera2Config.Extender(builder).setDeviceStateCallback(mMockStateCallback);
        ImageAnalysisConfig config = builder.build();
        ImageAnalysis useCase = new ImageAnalysis(config);
        CameraX.bindToLifecycle(mLifecycle, useCase);
        useCase.setAnalyzer(mImageAnalyzer);
        mLifecycle.startAndResume();

        verify(mMockStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));
    }

    @Test
    public void bind_opensCamera_withOutAnalyzer() {
        ImageAnalysisConfig.Builder builder =
                new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
        new Camera2Config.Extender(builder).setDeviceStateCallback(mMockStateCallback);
        ImageAnalysisConfig config = builder.build();
        ImageAnalysis useCase = new ImageAnalysis(config);
        CameraX.bindToLifecycle(mLifecycle, useCase);
        mLifecycle.startAndResume();

        verify(mMockStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));
    }

    @Test
    public void bind_unbind_loopWithOutAnalyzer() {
        ImageAnalysisConfig.Builder builder =
                new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
        new Camera2Config.Extender(builder).setDeviceStateCallback(mMockStateCallback);
        mLifecycle.startAndResume();

        for (int i = 0; i < 2; i++) {
            ImageAnalysisConfig config = builder.build();
            ImageAnalysis useCase = new ImageAnalysis(config);
            CameraX.bindToLifecycle(mLifecycle, useCase);

            verify(mMockStateCallback, timeout(5000)).onOpened(any(CameraDevice.class));

            CameraX.unbind(useCase);

            verify(mMockStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
        }
    }

    @Test
    public void bind_unbind_loopWithAnalyzer() {
        ImageAnalysisConfig.Builder builder =
                new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
        new Camera2Config.Extender(builder).setDeviceStateCallback(mMockStateCallback);
        mLifecycle.startAndResume();

        for (int i = 0; i < 2; i++) {
            ImageAnalysisConfig config = builder.build();
            ImageAnalysis useCase = new ImageAnalysis(config);
            CameraX.bindToLifecycle(mLifecycle, useCase);
            useCase.setAnalyzer(mImageAnalyzer);

            verify(mMockStateCallback, timeout(5000)).onOpened(any(CameraDevice.class));

            CameraX.unbind(useCase);

            verify(mMockStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
        }
    }

    @Test
    public void unbindAll_closesAllCameras() {
        ImageAnalysisConfig.Builder builder =
                new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
        new Camera2Config.Extender(builder).setDeviceStateCallback(mMockStateCallback);
        ImageAnalysisConfig config = builder.build();
        ImageAnalysis useCase = new ImageAnalysis(config);
        CameraX.bindToLifecycle(mLifecycle, useCase);
        mLifecycle.startAndResume();

        verify(mMockStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        CameraX.unbindAll();

        verify(mMockStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
    }

    @Test
    public void unbindAllAssociatedUseCase_closesCamera() {
        ImageAnalysisConfig.Builder builder =
                new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
        new Camera2Config.Extender(builder).setDeviceStateCallback(mMockStateCallback);
        ImageAnalysisConfig config = builder.build();
        ImageAnalysis useCase = new ImageAnalysis(config);
        CameraX.bindToLifecycle(mLifecycle, useCase);
        mLifecycle.startAndResume();

        verify(mMockStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        CameraX.unbind(useCase);

        verify(mMockStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
    }

    @Test
    public void unbindPartialAssociatedUseCase_doesNotCloseCamera() throws InterruptedException {
        ImageAnalysisConfig.Builder builder =
                new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
        new Camera2Config.Extender(builder).setDeviceStateCallback(mMockStateCallback);
        ImageAnalysisConfig config0 = builder.build();
        ImageAnalysis useCase0 = new ImageAnalysis(config0);

        ImageCaptureConfig configuration =
                new ImageCaptureConfig.Builder()
                        .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                        .build();
        ImageCapture useCase1 = new ImageCapture(configuration);

        CameraX.bindToLifecycle(mLifecycle, useCase0, useCase1);
        mLifecycle.startAndResume();

        verify(mMockStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        CameraX.unbind(useCase1);

        Thread.sleep(3000);

        verify(mMockStateCallback, never()).onClosed(any(CameraDevice.class));
    }

    @Test
    public void unbindAllAssociatedUseCaseInParts_ClosesCamera() {
        ImageAnalysisConfig.Builder builder =
                new ImageAnalysisConfig.Builder().setLensFacing(DEFAULT_LENS_FACING);
        new Camera2Config.Extender(builder).setDeviceStateCallback(mMockStateCallback);
        ImageAnalysisConfig config0 = builder.build();
        ImageAnalysis useCase0 = new ImageAnalysis(config0);

        ImageCaptureConfig configuration =
                new ImageCaptureConfig.Builder()
                        .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                        .build();
        ImageCapture useCase1 = new ImageCapture(configuration);

        CameraX.bindToLifecycle(mLifecycle, useCase0, useCase1);

        mLifecycle.startAndResume();

        verify(mMockStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        CameraX.unbind(useCase0);
        CameraX.unbind(useCase1);

        verify(mMockStateCallback, timeout(3000).times(1)).onClosed(any(CameraDevice.class));
    }
}
