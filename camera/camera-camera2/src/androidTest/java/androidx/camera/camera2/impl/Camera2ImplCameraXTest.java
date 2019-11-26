/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;

import androidx.annotation.NonNull;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.camera2.ExperimentalCamera2Interop;
import androidx.camera.camera2.impl.util.SemaphoreReleasingCamera2Callbacks.DeviceStateCallback;
import androidx.camera.camera2.impl.util.SemaphoreReleasingCamera2Callbacks.SessionCaptureCallback;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.LensFacing;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains tests for {@link androidx.camera.core.CameraX} which require an actual implementation to
 * run.
 */

@FlakyTest
@LargeTest
@RunWith(AndroidJUnit4.class)
@UseExperimental(markerClass = ExperimentalCamera2Interop.class)
public final class Camera2ImplCameraXTest {
    @LensFacing
    private static final int DEFAULT_LENS_FACING = LensFacing.BACK;
    private static final CameraSelector DEFAULT_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(DEFAULT_LENS_FACING).build();
    private final MutableLiveData<Long> mAnalysisResult = new MutableLiveData<>();
    private final MutableLiveData<Long> mAnalysisResult2 = new MutableLiveData<>();
    private final ImageAnalysis.Analyzer mImageAnalyzer =
            new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy image, int rotationDegrees) {
                    mAnalysisResult.postValue(image.getImageInfo().getTimestamp());

                    image.close();
                }
            };
    private final ImageAnalysis.Analyzer mImageAnalyzer2 =
            (image, rotationDegrees) -> {
                mAnalysisResult2.postValue(image.getImageInfo().getTimestamp());
                image.close();
            };
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private CameraDevice.StateCallback mDeviceStateCallback;
    private FakeLifecycleOwner mLifecycle;

    private static Observer<Long> createCountIncrementingObserver(final AtomicLong counter) {
        return new Observer<Long>() {
            @Override
            public void onChanged(Long value) {
                counter.incrementAndGet();
            }
        };
    }

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, Camera2AppConfig.create(context));
        mLifecycle = new FakeLifecycleOwner();

        mDeviceStateCallback = mock(CameraDevice.StateCallback.class);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);
        }
        CameraX.shutdown().get();
    }

    @Test
    public void lifecycleResume_opensCameraAndStreamsFrames() {
        Observer<Long> mockObserver = mock(Observer.class);
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
                new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
                ImageAnalysis useCase = builder.build();

                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mImageAnalyzer);
                mAnalysisResult.observe(mLifecycle, mockObserver);

                mLifecycle.startAndResume();
            }
        });
        verify(mockObserver, timeout(5000).atLeast(10)).onChanged(any());
    }

    @Test
    public void removedUseCase_doesNotStreamWhenLifecycleResumes() throws NullPointerException,
            CameraAccessException, CameraInfoUnavailableException {
        // Legacy device would not support two ImageAnalysis use cases combination.
        int hardwareLevelValue;
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(
                        CameraX.getCameraWithLensFacing(DEFAULT_LENS_FACING));
        hardwareLevelValue = cameraCharacteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        assumeTrue(
                hardwareLevelValue != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);

        Observer<Long> mockObserver = mock(Observer.class);
        Observer<Long> mockObserver2 = mock(Observer.class);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
                new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
                ImageAnalysis useCase = builder.build();

                ImageAnalysis useCase2 = new ImageAnalysis.Builder().build();

                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase, useCase2);

                useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mImageAnalyzer);
                useCase2.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mImageAnalyzer2);
                mAnalysisResult.observe(mLifecycle, mockObserver);
                mAnalysisResult2.observe(mLifecycle, mockObserver2);

                CameraX.unbind(useCase);

                mLifecycle.startAndResume();
            }
        });

        // Let second ImageAnalysis get some images. This shows that the first ImageAnalysis has
        // not observed any images, even though the camera has started to stream.
        verify(mockObserver2, timeout(3000).atLeast(3)).onChanged(any());
        verify(mockObserver, never()).onChanged(any());
    }

    @Test
    public void lifecyclePause_closesCameraAndStopsStreamingFrames() throws InterruptedException {
        final AtomicLong observedCount = new AtomicLong(0);
        final SessionCaptureCallback sessionCaptureCallback = new SessionCaptureCallback();
        final DeviceStateCallback deviceStateCallback = new DeviceStateCallback();
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ImageAnalysis.Builder configBuilder = new ImageAnalysis.Builder();
                new Camera2Interop.Extender<>(configBuilder)
                        .setDeviceStateCallback(deviceStateCallback)
                        .setSessionCaptureCallback(sessionCaptureCallback);
                ImageAnalysis useCase = configBuilder.build();
                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mImageAnalyzer);
                mAnalysisResult.observe(mLifecycle, createCountIncrementingObserver(observedCount));

                mLifecycle.startAndResume();
            }
        });

        // Wait a little bit for the camera to open and stream frames.
        sessionCaptureCallback.waitForOnCaptureCompleted(5);

        mInstrumentation.runOnMainSync(new Runnable() {
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
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mImageAnalyzer);
                mLifecycle.startAndResume();
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));
    }

    @Test
    public void bind_opensCamera_withOutAnalyzer() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                mLifecycle.startAndResume();
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));
    }

    @Test
    public void bind_opensCamera_noActiveUseCase_sessionIsConfigured() {
        CameraCaptureSession.StateCallback mockSessionStateCallback = mock(
                CameraCaptureSession.StateCallback.class);

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback)
                .setSessionStateCallback(mockSessionStateCallback);

        ImageAnalysis useCase = builder.build();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                mLifecycle.startAndResume();
            }
        });

        // When no analyzer is set, there will be no active surface for repeating request
        // CaptureSession#mSessionConfig will be null. Thus we wait until capture session
        // onConfigured to see if it causes any issue.
        verify(mockSessionStateCallback, timeout(3000)).onConfigured(
                any(CameraCaptureSession.class));
    }

    @Test
    public void bind_unbind_loopWithOutAnalyzer() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        mLifecycle.startAndResume();

        for (int i = 0; i < 2; i++) {
            CameraDevice.StateCallback callback = mock(CameraDevice.StateCallback.class);
            new Camera2Interop.Extender<>(builder).setDeviceStateCallback(callback);
            ImageAnalysis useCase = builder.build();

            mInstrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                }
            });

            verify(callback, timeout(5000)).onOpened(any(CameraDevice.class));

            mInstrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    CameraX.unbind(useCase);
                }
            });

            verify(callback, timeout(3000)).onClosed(any(CameraDevice.class));
        }
    }

    @Test
    public void bind_unbind_loopWithAnalyzer() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        mLifecycle.startAndResume();

        for (int i = 0; i < 2; i++) {
            CameraDevice.StateCallback callback = mock(CameraDevice.StateCallback.class);
            new Camera2Interop.Extender<>(builder).setDeviceStateCallback(callback);
            ImageAnalysis useCase = builder.build();

            mInstrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                    useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mImageAnalyzer);
                }
            });

            verify(callback, timeout(5000)).onOpened(any(CameraDevice.class));

            mInstrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    CameraX.unbind(useCase);
                }
            });

            verify(callback, timeout(3000)).onClosed(any(CameraDevice.class));
        }
    }

    @Test
    public void unbindAll_closesAllCameras() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                mLifecycle.startAndResume();
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.unbindAll();
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
    }

    @Test
    public void unbindAllAssociatedUseCase_closesCamera() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);
                mLifecycle.startAndResume();
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.unbind(useCase);
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
    }

    @Test
    public void unbindPartialAssociatedUseCase_doesNotCloseCamera() throws InterruptedException {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase0 = builder.build();

        ImageCapture useCase1 = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAXIMIZE_QUALITY)
                .build();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase0, useCase1);
                mLifecycle.startAndResume();
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.unbind(useCase1);
            }
        });

        Thread.sleep(3000);

        verify(mDeviceStateCallback, never()).onClosed(any(CameraDevice.class));
    }

    @Test
    public void unbindAllAssociatedUseCaseInParts_ClosesCamera() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase0 = builder.build();

        ImageCapture useCase1 = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAXIMIZE_QUALITY)
                .build();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase0, useCase1);
                mLifecycle.startAndResume();
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.unbind(useCase0);
                CameraX.unbind(useCase1);
            }
        });

        verify(mDeviceStateCallback, timeout(3000).times(1)).onClosed(any(CameraDevice.class));
    }

    @Test
    @UiThreadTest
    public void cameraInfo_getCameraInfoFromCamera() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        ImageAnalysis useCase = builder.build();

        Camera camera = CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);

        assertThat(camera.getCameraInfo()).isInstanceOf(CameraInfo.class);
    }

    @Test
    @UiThreadTest
    public void cameraControl_getCameraControlFromCamera() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        ImageAnalysis useCase = builder.build();

        Camera camera = CameraX.bindToLifecycle(mLifecycle, DEFAULT_SELECTOR, useCase);

        assertThat(camera.getCameraControl()).isInstanceOf(CameraControl.class);
    }
}
