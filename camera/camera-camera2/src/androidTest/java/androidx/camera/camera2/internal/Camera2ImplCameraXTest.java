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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.internal.util.SemaphoreReleasingCamera2Callbacks;
import androidx.camera.camera2.internal.util.SemaphoreReleasingCamera2Callbacks.DeviceStateCallback;
import androidx.camera.camera2.internal.util.SemaphoreReleasingCamera2Callbacks.SessionCaptureCallback;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains tests for {@link androidx.camera.core.CameraX} which require an actual implementation to
 * run.
 */

@FlakyTest
@LargeTest
@RunWith(AndroidJUnit4.class)
@OptIn(markerClass = ExperimentalCamera2Interop.class)
@SdkSuppress(minSdkVersion = 21)
public final class Camera2ImplCameraXTest {
    @CameraSelector.LensFacing
    private static final int DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_BACK;
    private static final CameraSelector DEFAULT_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(DEFAULT_LENS_FACING).build();

    @Rule
    public TestRule mCameraRule = CameraUtil.grantCameraPermissionAndPreTest();

    private CameraDevice.StateCallback mDeviceStateCallback;
    private FakeLifecycleOwner mLifecycle;

    private Context mContext;
    private CameraUseCaseAdapter mCamera;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        CameraX.initialize(mContext, Camera2Config.defaultConfig());
        mLifecycle = new FakeLifecycleOwner();

        mDeviceStateCallback = mock(CameraDevice.StateCallback.class);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        if (mCamera != null) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    //TODO: The removeUseCases() call might be removed after clarifying the
                    // abortCaptures() issue in b/162314023.
                    mCamera.removeUseCases(mCamera.getUseCases())
            );
        }

        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void lifecycleResume_opensCameraAndStreamsFrames() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase);
        WaitingAnalyzer waitingAnalyzer = new WaitingAnalyzer(10);
        useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), waitingAnalyzer);

        assertThat(waitingAnalyzer.waitForCount(5000, TimeUnit.MILLISECONDS)).isTrue();
    }

    @Test
    public void removedUseCase_doesNotStreamWhenLifecycleResumes() throws NullPointerException,
            CameraAccessException, CameraInfoUnavailableException {
        // Legacy device would not support two ImageAnalysis use cases combination.
        int hardwareLevelValue;
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraCharacteristics(DEFAULT_LENS_FACING);
        hardwareLevelValue = cameraCharacteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        assumeTrue(
                hardwareLevelValue != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);


        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        ImageAnalysis useCase2 = new ImageAnalysis.Builder().build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase,
                useCase2);

        CountingAnalyzer countingAnalyzer = new CountingAnalyzer();
        WaitingAnalyzer waitingAnalyzer = new WaitingAnalyzer(3);

        useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), countingAnalyzer);
        useCase2.setAnalyzer(CameraXExecutors.mainThreadExecutor(), waitingAnalyzer);

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                mCamera.removeUseCases(Arrays.asList(useCase))
        );

        mLifecycle.startAndResume();

        // Let second ImageAnalysis get some images. This shows that the first ImageAnalysis has
        // not observed any images, even though the camera has started to stream.
        waitingAnalyzer.waitForCount(3000, TimeUnit.MILLISECONDS);
        assertThat(countingAnalyzer.getCount()).isEqualTo(0);
    }

    @Test
    public void detach_closesCameraAndStopsStreamingFrames() throws InterruptedException {
        final SessionCaptureCallback sessionCaptureCallback = new SessionCaptureCallback();
        final DeviceStateCallback deviceStateCallback = new DeviceStateCallback();

        ImageAnalysis.Builder configBuilder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(configBuilder)
                .setDeviceStateCallback(deviceStateCallback)
                .setSessionCaptureCallback(sessionCaptureCallback);
        ImageAnalysis useCase = configBuilder.build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase);
        CountingAnalyzer countingAnalyzer = new CountingAnalyzer();
        useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), countingAnalyzer);

        mLifecycle.startAndResume();

        // Wait a little bit for the camera to open and stream frames.
        sessionCaptureCallback.waitForOnCaptureCompleted(5);

        mCamera.detachUseCases();

        // Wait a little bit for the camera to close.
        deviceStateCallback.waitForOnClosed(1);

        final int firstObservedCount = countingAnalyzer.getCount();
        assertThat(firstObservedCount).isGreaterThan(1);

        // Stay in idle state for a while.
        Thread.sleep(5000);

        // Additional frames should not be observed.
        final int secondObservedCount = countingAnalyzer.getCount();
        assertThat(secondObservedCount).isEqualTo(firstObservedCount);
    }

    @Test
    public void detachAndAttachInShortTime_theCaptureSessionShouldOpenAndCloseCorrectly()
            throws InterruptedException {
        // To test the CaptureSession should open/close correctly or we might fail to close the
        // camera device due to the wrong capture session state.
        //
        // This test simulate to rotate the device and switch the camera.
        // Test contains the following steps:
        // (1) Bind use case to lifecycle and wait for the stream is opened.
        // (2) Set the lifeCycle pause and resume to simulate the app was closed and then reopened.
        // Similar to rotate the device.
        // (3) Set the lifeCycle pause and resume again to simulate the device was rotated back
        // to the original position.
        // (4) Switch the camera and verify the target camera is opened successfully.


        // Make sure the test environment have front/back camera.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        SemaphoreReleasingCamera2Callbacks.SessionStateCallback sessionStateCallback =
                new SemaphoreReleasingCamera2Callbacks.SessionStateCallback();
        final CountDownLatch lock = new CountDownLatch(5);
        final CameraCaptureSession.CaptureCallback callback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        lock.countDown();
                    }
                };

        ImageAnalysis.Builder configBuilder0 = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(configBuilder0).setSessionCaptureCallback(
                callback).setSessionStateCallback(sessionStateCallback);
        ImageAnalysis useCase0 = configBuilder0.build();
        CameraSelector selectorBack = new CameraSelector.Builder().requireLensFacing(
                CameraSelector.LENS_FACING_BACK).build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, selectorBack, useCase0);

        mLifecycle.startAndResume();

        // Wait a little bit for the camera to open and stream frames.
        lock.await(3000, TimeUnit.MILLISECONDS);

        // Pause/Resume the lifeCycle twice to simulate the test step (2) and (3).
        for (int i = 0; i < 2; i++) {
            // Attach/Detach the useCases to create and close the capture session. the new
            // camera capture session will be opened immediately after a previous
            // CaptureSession is opened, and the previous CaptureSession will going to close
            // right after the new CaptureSession is opened.
            mCamera.detachUseCases();
            mCamera.attachUseCases();
            // Wait for the capture session is configured.
            assertTrue(sessionStateCallback.waitForOnConfigured(1));
        }

        // Detach all useCase and switch to another camera to verify the camera close flow.
        mCamera.detachUseCases();

        // The camera switch only succeeds after all the exist CaptureSession was
        // closed successfully.
        ImageAnalysis.Builder configBuilder1 = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(configBuilder1).setDeviceStateCallback(
                mDeviceStateCallback);
        ImageAnalysis useCase1 = configBuilder1.build();
        CameraSelector selectorFront = new CameraSelector.Builder().requireLensFacing(
                CameraSelector.LENS_FACING_FRONT).build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, selectorFront, useCase1);

        // The front camera should open successfully. If the test fail, the CameraX might
        // in wrong internal state, and the CameraX#shutdown() might stuck.
        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));
    }

    @Test
    public void bind_opensCamera() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase);

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));
    }

    @Test
    public void bind_opensCamera_withOutAnalyzer() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase);

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

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase);

        // When no analyzer is set, there will be no active surface for repeating request
        // CaptureSession#mSessionConfig will be null. Thus we wait until capture session
        // onConfigured to see if it causes any issue.
        verify(mockSessionStateCallback, timeout(3000)).onConfigured(
                any(CameraCaptureSession.class));
    }

    @Test
    public void attach_detach_loopWithOutAnalyzer() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        mLifecycle.startAndResume();

        for (int i = 0; i < 2; i++) {
            CameraDevice.StateCallback callback = mock(CameraDevice.StateCallback.class);
            new Camera2Interop.Extender<>(builder).setDeviceStateCallback(callback);
            ImageAnalysis useCase = builder.build();

            mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase);

            verify(callback, timeout(5000)).onOpened(any(CameraDevice.class));

            mCamera.detachUseCases();

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

            mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase);

            verify(callback, timeout(5000)).onOpened(any(CameraDevice.class));

            mCamera.detachUseCases();

            verify(callback, timeout(3000)).onClosed(any(CameraDevice.class));
        }
    }

    @Test
    public void removeAllAssociatedUseCase_closesCamera() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase = builder.build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase);

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                mCamera.removeUseCases(Collections.singletonList(useCase))
        );
        verify(mDeviceStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
    }

    @Test
    public void removePartialAssociatedUseCase_doesNotCloseCamera() throws InterruptedException {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase0 = builder.build();

        ImageCapture useCase1 = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase0,
                useCase1);

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                mCamera.removeUseCases(Collections.singletonList(useCase1))
        );
        Thread.sleep(3000);

        verify(mDeviceStateCallback, never()).onClosed(any(CameraDevice.class));
    }

    @Test
    public void removeAllAssociatedUseCaseInParts_ClosesCamera() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(mDeviceStateCallback);
        ImageAnalysis useCase0 = builder.build();

        ImageCapture useCase1 = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, DEFAULT_SELECTOR, useCase0,
                useCase1);

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                    mCamera.removeUseCases(Collections.singleton(useCase0));
                    mCamera.removeUseCases(Collections.singleton(useCase1));
                }
        );

        verify(mDeviceStateCallback, timeout(3000).times(1)).onClosed(any(CameraDevice.class));
    }

    @Test
    public void sequentialBindUnbindUseCases_closeCamera() throws
            CameraUseCaseAdapter.CameraException {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        ImageCapture.Builder builder1 = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);
        new Camera2Interop.Extender<>(builder1).setDeviceStateCallback(mDeviceStateCallback);
        ImageCapture imageCapture = builder1.build();

        mCamera = CameraUtil.createCameraUseCaseAdapter(mContext, DEFAULT_SELECTOR);

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                mCamera.addUseCases(Collections.singleton(imageCapture));
                mCamera.addUseCases(Collections.singleton(imageAnalysis));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException(e);
            }
        });

        verify(mDeviceStateCallback, timeout(3000)).onOpened(any(CameraDevice.class));


        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mCamera.removeUseCases(Collections.singleton(imageCapture));
            mCamera.removeUseCases(Collections.singleton(imageAnalysis));
        });

        verify(mDeviceStateCallback, timeout(3000)).onClosed(any(CameraDevice.class));
    }

    private class CountingAnalyzer implements ImageAnalysis.Analyzer {
        AtomicInteger mCount = new AtomicInteger(0);

        @Override
        public void analyze(@NonNull ImageProxy image) {
            mCount.incrementAndGet();
            image.close();
        }

        // Should not be used for exact value
        int getCount() {
            return mCount.get();
        }
    }

    private class WaitingAnalyzer implements ImageAnalysis.Analyzer {
        private final CountDownLatch mCountDownLatch;
        WaitingAnalyzer(int waitCount) {
            mCountDownLatch = new CountDownLatch(waitCount);
        }

        @Override
        public void analyze(@NonNull ImageProxy image) {
            mCountDownLatch.countDown();
            image.close();
        }

        boolean waitForCount(long timeout, TimeUnit unit) {
            try {
                return mCountDownLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                // If it is interrupted then it is a failure
                return false;
            }
        }
    }
}
