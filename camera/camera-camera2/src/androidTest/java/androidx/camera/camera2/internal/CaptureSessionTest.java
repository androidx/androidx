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

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.Manifest;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.camera2.internal.CaptureSession.State;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureCallbacks;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.testing.CameraUtil;
import androidx.core.os.HandlerCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests for {@link CaptureSession}. This requires an environment where a valid {@link
 * android.hardware.camera2.CameraDevice} can be opened since it is used to open a {@link
 * android.hardware.camera2.CaptureRequest}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class CaptureSessionTest {
    private CaptureSessionTestParameters mTestParameters0;
    private CaptureSessionTestParameters mTestParameters1;

    private CameraUtil.CameraDeviceHolder mCameraDeviceHolder;

    private final List<CaptureSession> mCaptureSessions = new ArrayList<>();

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setup() throws CameraAccessException, InterruptedException,
            AssumptionViolatedException, TimeoutException, ExecutionException {
        assumeTrue(CameraUtil.deviceHasCamera());
        mTestParameters0 = new CaptureSessionTestParameters("mTestParameters0");
        mTestParameters1 = new CaptureSessionTestParameters("mTestParameters1");
        mCameraDeviceHolder = CameraUtil.getCameraDevice();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        // Ensure all capture sessions are fully closed
        List<ListenableFuture<Void>> releaseFutures = new ArrayList<>();
        for (CaptureSession captureSession : mCaptureSessions) {
            releaseFutures.add(captureSession.release(/*abortInFlightCaptures=*/false));
        }
        mCaptureSessions.clear();
        Future<?> aggregateReleaseFuture = Futures.allAsList(releaseFutures);
        aggregateReleaseFuture.get();

        if (mCameraDeviceHolder != null) {
            CameraUtil.releaseCameraDevice(mCameraDeviceHolder);
            mTestParameters0.tearDown();
            mTestParameters1.tearDown();
        }
    }

    @Test
    public void setCaptureSessionSucceed() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        assertThat(captureSession.getSessionConfig()).isEqualTo(mTestParameters0.mSessionConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void setCaptureSessionOnClosedSession_throwsException() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        SessionConfig newSessionConfig = mTestParameters0.mSessionConfig;

        captureSession.close();

        // Should throw IllegalStateException
        captureSession.setSessionConfig(newSessionConfig);
    }

    @Test
    public void openCaptureSessionSucceed() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get()), mockFutureCallback,
                CameraXExecutors.mainThreadExecutor());

        assertTrue(mTestParameters0.waitForData());

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        verify(mockFutureCallback, times(1)).onSuccess(any());

        // StateCallback.onConfigured() should be called to signal the session is configured.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mSessionCameraCaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void openCaptureSessionWithClosedSurfaceFails() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        surface.close();

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get()), mockFutureCallback,
                CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onFailure(any(Throwable.class));
    }

    @Test
    public void captureSessionIncreasesSurfaceUseCountAfterOpen_andDecreasesAfterRelease() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        int useCountBeforeOpen = surface.getUseCount();

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get()), mockFutureCallback,
                CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onSuccess(any());
        int useCountAfterOpen = surface.getUseCount();

        reset(mockFutureCallback);

        captureSession.close();
        Futures.addCallback(captureSession.release(false), mockFutureCallback,
                CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onSuccess(any());
        int useCountAfterRelease = surface.getUseCount();

        assertThat(useCountAfterOpen).isGreaterThan(useCountBeforeOpen);
        assertThat(useCountAfterRelease).isEqualTo(useCountBeforeOpen);
    }

    @Test
    public void openCaptureSessionWithOptionOverride() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        assertTrue(mTestParameters0.waitForData());

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        // StateCallback.onConfigured() should be called to signal the session is configured.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));

        ArgumentCaptor<CameraCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                CameraCaptureResult.class);

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mSessionCameraCaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureCompleted(captureResultCaptor.capture());

        CameraCaptureResult cameraCaptureResult = captureResultCaptor.getValue();
        assertThat(cameraCaptureResult).isInstanceOf(Camera2CameraCaptureResult.class);

        CaptureResult captureResult =
                ((Camera2CameraCaptureResult) cameraCaptureResult).getCaptureResult();

        // From CameraEventCallbacks option
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_AF_MODE)).isEqualTo(
                CaptureRequest.CONTROL_AF_MODE_MACRO);
        assertThat(captureResult.getRequest().get(CaptureRequest.FLASH_MODE)).isEqualTo(
                CaptureRequest.FLASH_MODE_TORCH);

        // From SessionConfig option
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_AE_MODE)).isEqualTo(
                CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void closeUnopenedSession() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.close();

        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void releaseUnopenedSession() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.release(/*abortInFlightCaptures=*/false);

        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closeOpenedSession() throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get()).get();

        captureSession.close();

        // Session should be in closed state immediately after calling close() on an
        // opening/opened session.
        assertThat(captureSession.getState()).isEqualTo(State.CLOSED);
    }

    @Test
    public void releaseOpenedSession() throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get()).get();
        ListenableFuture<Void> releaseFuture = captureSession.release(
                /*abortInFlightCaptures=*/false);

        // Wait for release
        releaseFuture.get();
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

        // StateCallback.onClosed() should be called to signal the session is closed.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onClosed(any(CameraCaptureSession.class));
    }

    // Wait for future completion. The test fails if it timeouts.
    private <T> void assertFutureCompletes(Future<T> future, long timeout, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException {
        try {
            future.get(timeout, timeUnit);
            assertTrue(true);
        } catch (TimeoutException e) {
            fail("Future cannot complete in time");
        }
    }

    @Test
    public void openSecondSession() throws InterruptedException, ExecutionException {
        CaptureSession captureSession0 = createCaptureSession(mTestParameters0);
        captureSession0.setSessionConfig(mTestParameters0.mSessionConfig);

        // First session is opened
        Future<Void> openFuture0 = captureSession0.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get());
        assertFutureCompletes(openFuture0, 5, TimeUnit.SECONDS);

        captureSession0.close();

        // Open second session, which should cause first one to be released
        CaptureSession captureSession1 = createCaptureSession(mTestParameters1);
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        Future<Void> openFuture1 = captureSession1.open(mTestParameters1.mSessionConfig,
                mCameraDeviceHolder.get());
        assertFutureCompletes(openFuture1, 5, TimeUnit.SECONDS);


        assertTrue(mTestParameters1.waitForData());

        assertThat(captureSession1.getState()).isEqualTo(State.OPENED);
        assertThat(captureSession0.getState()).isEqualTo(State.RELEASED);

        // First session should have StateCallback.onConfigured(), onClosed() calls.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onClosed(any(CameraCaptureSession.class));

        // Second session should have StateCallback.onConfigured() call.
        verify(mTestParameters1.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));

        // Second session should have CameraCaptureCallback.onCaptureCompleted() call.
        verify(mTestParameters1.mSessionCameraCaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void issueCaptureRequest() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        assertTrue(mTestParameters0.waitForData());

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void issueCaptureRequestAppendAndOverrideRepeatingOptions() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        assertTrue(mTestParameters0.waitForData());

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        ArgumentCaptor<CameraCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                CameraCaptureResult.class);

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(captureResultCaptor.capture());

        CameraCaptureResult cameraCaptureResult = captureResultCaptor.getValue();
        assertThat(cameraCaptureResult).isInstanceOf(Camera2CameraCaptureResult.class);

        CaptureResult captureResult =
                ((Camera2CameraCaptureResult) cameraCaptureResult).getCaptureResult();

        // From CaptureConfig option
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_AF_MODE)).isEqualTo(
                CaptureRequest.CONTROL_AF_MODE_OFF);

        // From CameraEventCallbacks option
        assertThat(captureResult.getRequest().get(CaptureRequest.FLASH_MODE)).isEqualTo(
                CaptureRequest.FLASH_MODE_TORCH);

        // From SessionConfig option
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_AE_MODE)).isEqualTo(
                CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void issueCaptureRequestAcrossCaptureSessions() throws InterruptedException {
        CaptureSession captureSession0 = createCaptureSession(mTestParameters0);
        captureSession0.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession0.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
        captureSession0.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        captureSession0.close();
        CaptureSession captureSession1 = createCaptureSession(mTestParameters0);
        captureSession1.setSessionConfig(captureSession0.getSessionConfig());
        if (!captureSession0.getCaptureConfigs().isEmpty()) {
            captureSession1.issueCaptureRequests(captureSession0.getCaptureConfigs());
        }
        captureSession1.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void issueCaptureRequestBeforeCaptureSessionOpened() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test(expected = IllegalStateException.class)
    public void issueCaptureRequestOnClosedSession_throwsException() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.close();

        // Should throw IllegalStateException
        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
    }

    @Test
    public void startStreamingAfterOpenCaptureSession()
            throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        assertTrue(mTestParameters0.waitForData());
        assertThat(captureSession.getState()).isEqualTo(State.OPENED);
        assertFutureCompletes(captureSession.getStartStreamingFuture(), 5,
                TimeUnit.SECONDS);

        verify(mTestParameters0.mCamera2CaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureStarted(any(CameraCaptureSession.class), any(CaptureRequest.class),
                        any(Long.class), any(Long.class));
    }

    @Test
    public void surfaceTerminationFutureIsCalledWhenSessionIsClose()
            throws InterruptedException, ExecutionException {
        mTestParameters0.setCloseSurfaceOnSessionClose(true);
        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        assertTrue(mTestParameters0.waitForData());

        Runnable runnable = mock(Runnable.class);
        mTestParameters0.mDeferrableSurface.getTerminationFuture().addListener(runnable,
                CameraXExecutors.directExecutor());

        captureSession.release(/*abortInFlightCaptures=*/false);

        Mockito.verify(runnable, timeout(3000).times(1)).run();
    }

    @Test
    public void cameraEventCallbackInvokedInOrder() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        InOrder inOrder = inOrder(mTestParameters0.mMockCameraEventCallback);

        inOrder.verify(mTestParameters0.mMockCameraEventCallback, timeout(3000)).onPresetSession();
        inOrder.verify(mTestParameters0.mMockCameraEventCallback, timeout(3000)).onEnableSession();
        inOrder.verify(mTestParameters0.mMockCameraEventCallback, timeout(3000)).onRepeating();
        verify(mTestParameters0.mMockCameraEventCallback, never()).onDisableSession();

        verifyNoMoreInteractions(mTestParameters0.mMockCameraEventCallback);

        captureSession.close();
        verify(mTestParameters0.mMockCameraEventCallback, timeout(3000)).onDisableSession();

        verifyNoMoreInteractions(mTestParameters0.mMockCameraEventCallback);
    }

    @Test
    public void cameraEventCallback_requestKeysIssuedSuccessfully() {
        ArgumentCaptor<CameraCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                CameraCaptureResult.class);

        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        // Open the capture session and verify the onEnableSession callback would be invoked
        // but onDisableSession callback not.
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        // Verify the request options in onEnableSession.
        verify(mTestParameters0.mTestCameraEventCallback.mEnableCallback,
                timeout(3000)).onCaptureCompleted(captureResultCaptor.capture());
        CameraCaptureResult result1 = captureResultCaptor.getValue();
        assertThat(result1).isInstanceOf(Camera2CameraCaptureResult.class);
        CaptureResult captureResult1 = ((Camera2CameraCaptureResult) result1).getCaptureResult();
        assertThat(
                captureResult1.getRequest().get(CaptureRequest.CONTROL_CAPTURE_INTENT)).isEqualTo(
                CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);
        // The onDisableSession should not been invoked.
        verify(mTestParameters0.mTestCameraEventCallback.mDisableCallback,
                never()).onCaptureCompleted(any(CameraCaptureResult.class));

        reset(mTestParameters0.mTestCameraEventCallback.mEnableCallback);
        reset(mTestParameters0.mTestCameraEventCallback.mDisableCallback);

        // Close the capture session and verify the onDisableSession callback would be invoked
        // but onEnableSession callback not.
        captureSession.close();

        // Verify the request options in onDisableSession.
        verify(mTestParameters0.mTestCameraEventCallback.mDisableCallback,
                timeout(3000)).onCaptureCompleted(captureResultCaptor.capture());
        CameraCaptureResult result2 = captureResultCaptor.getValue();
        assertThat(result2).isInstanceOf(Camera2CameraCaptureResult.class);
        CaptureResult captureResult2 = ((Camera2CameraCaptureResult) result2).getCaptureResult();
        assertThat(
                captureResult2.getRequest().get(CaptureRequest.CONTROL_CAPTURE_INTENT)).isEqualTo(
                CaptureRequest.CONTROL_CAPTURE_INTENT_ZERO_SHUTTER_LAG);
        // The onEnableSession should not been invoked in close().
        verify(mTestParameters0.mTestCameraEventCallback.mEnableCallback,
                never()).onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void closingCaptureSessionClosesDeferrableSurface()
            throws ExecutionException, InterruptedException {
        mTestParameters0.setCloseSurfaceOnSessionClose(true);
        CaptureSession captureSession = createCaptureSession(mTestParameters0);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        Future<Void> openFuture = captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get());
        assertFutureCompletes(openFuture, 5, TimeUnit.SECONDS);

        captureSession.close();

        for (DeferrableSurface deferrableSurface : mTestParameters0.mSessionConfig.getSurfaces()) {
            ListenableFuture<Surface> surfaceListenableFuture = deferrableSurface.getSurface();

            FutureCallback<Surface> futureCallback = Mockito.mock(FutureCallback.class);
            ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

            Futures.addCallback(surfaceListenableFuture, futureCallback,
                    CameraXExecutors.directExecutor());
            verify(futureCallback, times(1)).onFailure(throwableCaptor.capture());

            assertThat(throwableCaptor.getValue()).isInstanceOf(
                    DeferrableSurface.SurfaceClosedException.class);
        }
    }

    @Test
    public void openCaptureSessionTwice_theSecondaryCallShouldNoop() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());

        // StateCallback.onConfigured() should be called to signal the session is configured.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(captureSession.getState()).isEqualTo(State.OPENED);
    }

    @Test
    public void releaseImmediateAfterOpenCaptureSession()
            throws ExecutionException, InterruptedException {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get());
        Future<Void> releaseFuture = captureSession.release(false);
        assertFutureCompletes(releaseFuture, 5, TimeUnit.SECONDS);

        // The captureSession state should change to RELEASED state
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void cancelOpenCaptureSessionListenableFuture_shouldNoop() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ListenableFuture<Void> openingFuture = captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get());
        Futures.addCallback(openingFuture, mockFutureCallback, AsyncTask.THREAD_POOL_EXECUTOR);
        openingFuture.cancel(true);

        // The captureSession opening should callback onFailure with a CancellationException.
        verify(mockFutureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(CancellationException.class);

        // The opening task should not propagate the cancellation to the internal
        // ListenableFuture task, so the captureSession should keep running. The
        // StateCallback.onConfigured() should be called and change the state to OPENED.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(captureSession.getState()).isEqualTo(State.OPENED);
    }

    @Test
    public void openCaptureSessionFailed_withClosedDeferrableSurface() {
        // Close the configured DeferrableSurface for testing.
        mTestParameters0.mDeferrableSurface.close();

        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get()), mockFutureCallback,
                CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());
        // The captureSession opening should callback onFailure when the DeferrableSurface is
        // already closed.
        assertThat(throwableCaptor.getValue()).isInstanceOf(
                DeferrableSurface.SurfaceClosedException.class);
    }

    private CaptureSession createCaptureSession(CaptureSessionTestParameters testParams) {
        CaptureSession captureSession = new CaptureSession(testParams.mExecutor,
                testParams.mHandler,
                testParams.mScheduledExecutor,
                testParams.getCloseSurfaceOnSessionClose());
        mCaptureSessions.add(captureSession);
        return captureSession;
    }

    @Test
    public void issueCaptureCancelledBeforeExecuting() {
        CaptureSession captureSession = createCaptureSession(mTestParameters0);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        captureSession.cancelIssuedCaptureRequests();

        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCancelled();
    }

    /**
     * A implementation to test {@link CameraEventCallback} on CaptureSession.
     */
    private static class TestCameraEventCallback extends CameraEventCallback {

        private final CameraCaptureCallback mEnableCallback = Mockito.mock(
                CameraCaptureCallback.class);
        private final CameraCaptureCallback mDisableCallback = Mockito.mock(
                CameraCaptureCallback.class);

        @Override
        public CaptureConfig onPresetSession() {
            return getCaptureConfig(CaptureRequest.CONTROL_CAPTURE_INTENT,
                    CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD, null);
        }

        @Override
        public CaptureConfig onEnableSession() {
            return getCaptureConfig(CaptureRequest.CONTROL_CAPTURE_INTENT,
                    CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW, mEnableCallback);
        }

        @Override
        public CaptureConfig onRepeating() {
            return getCaptureConfig(CaptureRequest.CONTROL_CAPTURE_INTENT,
                    CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW, null);
        }

        @Override
        public CaptureConfig onDisableSession() {
            return getCaptureConfig(CaptureRequest.CONTROL_CAPTURE_INTENT,
                    CaptureRequest.CONTROL_CAPTURE_INTENT_ZERO_SHUTTER_LAG, mDisableCallback);
        }
    }

    private static <T> CaptureConfig getCaptureConfig(CaptureRequest.Key<T> key, T effectValue,
            CameraCaptureCallback callback) {
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        Camera2ImplConfig.Builder camera2ConfigurationBuilder =
                new Camera2ImplConfig.Builder();
        camera2ConfigurationBuilder.setCaptureRequestOption(key, effectValue);
        captureConfigBuilder.addImplementationOptions(camera2ConfigurationBuilder.build());
        captureConfigBuilder.addCameraCaptureCallback(callback);
        return captureConfigBuilder.build();
    }

    /**
     * Collection of parameters required for setting a {@link CaptureSession} and wait for it to
     * produce data.
     */
    private static class CaptureSessionTestParameters {
        private static final int TIME_TO_WAIT_FOR_DATA_SECONDS = 3;
        /** Thread for all asynchronous calls. */
        private final HandlerThread mHandlerThread;
        /** Handler for all asynchronous calls. */
        private final Handler mHandler;
        /** Executor which delegates to Handler */
        private final Executor mExecutor;
        /** Scheduled executor service which delegates to Handler */
        private final ScheduledExecutorService mScheduledExecutor;
        /** Latch to wait for first image data to appear. */
        private final CountDownLatch mDataLatch = new CountDownLatch(1);

        /** Latch to wait for camera capture callback to be invoked. */
        private final CountDownLatch mCameraCaptureCallbackLatch = new CountDownLatch(1);

        private boolean mCloseSurfaceOnSessionClose = false;

        /** Image reader that unlocks the latch waiting for the first image data to appear. */
        private final OnImageAvailableListener mOnImageAvailableListener =
                new OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = reader.acquireNextImage();
                        if (image != null) {
                            image.close();
                            mDataLatch.countDown();
                        }
                    }
                };

        private final ImageReader mImageReader;
        private final SessionConfig mSessionConfig;
        private final CaptureConfig mCaptureConfig;

        private final TestCameraEventCallback mTestCameraEventCallback =
                new TestCameraEventCallback();
        private final CameraEventCallback mMockCameraEventCallback = Mockito.mock(
                CameraEventCallback.class);

        private final CameraCaptureSession.StateCallback mSessionStateCallback =
                Mockito.mock(CameraCaptureSession.StateCallback.class);
        private final CameraCaptureCallback mSessionCameraCaptureCallback =
                Mockito.mock(CameraCaptureCallback.class);
        private final CameraCaptureCallback mCameraCaptureCallback =
                Mockito.mock(CameraCaptureCallback.class);
        private final CameraCaptureSession.CaptureCallback mCamera2CaptureCallback =
                Mockito.mock(CameraCaptureSession.CaptureCallback.class);

        private final DeferrableSurface mDeferrableSurface;
        /**
         * A composite capture callback that dispatches callbacks to both mock and real callbacks.
         * The mock callback is used to verify the callback result. The real callback is used to
         * unlock the latch waiting.
         */
        private final CameraCaptureCallback mComboCameraCaptureCallback =
                CameraCaptureCallbacks.createComboCallback(
                        mCameraCaptureCallback,
                        new CameraCaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureResult result) {
                                mCameraCaptureCallbackLatch.countDown();
                            }
                        });

        CaptureSessionTestParameters(String name) {
            mHandlerThread = new HandlerThread(name);
            mHandlerThread.start();
            mHandler = HandlerCompat.createAsync(mHandlerThread.getLooper());

            mExecutor = CameraXExecutors.newHandlerExecutor(mHandler);
            mScheduledExecutor = CameraXExecutors.newHandlerExecutor(mHandler);

            mImageReader =
                    ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, /*maxImages*/ 2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);

            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            mDeferrableSurface = new ImmediateSurface(mImageReader.getSurface());
            builder.addSurface(mDeferrableSurface);
            builder.addSessionStateCallback(mSessionStateCallback);
            builder.addRepeatingCameraCaptureCallback(mSessionCameraCaptureCallback);
            builder.addRepeatingCameraCaptureCallback(
                    CaptureCallbackContainer.create(mCamera2CaptureCallback));

            MutableOptionsBundle testCallbackConfig = MutableOptionsBundle.create();
            testCallbackConfig.insertOption(Camera2ImplConfig.CAMERA_EVENT_CALLBACK_OPTION,
                    new CameraEventCallbacks(mTestCameraEventCallback));
            builder.addImplementationOptions(testCallbackConfig);

            MutableOptionsBundle mockCameraEventCallbackConfig = MutableOptionsBundle.create();
            mockCameraEventCallbackConfig.insertOption(
                    Camera2ImplConfig.CAMERA_EVENT_CALLBACK_OPTION,
                    new CameraEventCallbacks(mMockCameraEventCallback));
            builder.addImplementationOptions(mockCameraEventCallbackConfig);

            // Set capture request options
            // ==================================================================================
            // Priority | Component        | AF_MODE       | FLASH_MODE         | AE_MODE
            // ----------------------------------------------------------------------------------
            // P1 | CaptureConfig          | AF_MODE_OFF  |                     |
            // ----------------------------------------------------------------------------------
            // P2 | CameraEventCallbacks   | AF_MODE_MACRO | FLASH_MODE_TORCH   |
            // ----------------------------------------------------------------------------------
            // P3 | SessionConfig          | AF_MODE_AUTO  | FLASH_MODE_SINGLE  | AE_MODE_ON
            // ==================================================================================

            Camera2ImplConfig.Builder camera2ConfigBuilder = new Camera2ImplConfig.Builder();

            // Add capture request options for CameraEventCallbacks
            CameraEventCallback cameraEventCallback = new CameraEventCallback() {
                @Override
                public CaptureConfig onRepeating() {
                    CaptureConfig.Builder builder = new CaptureConfig.Builder();
                    builder.addImplementationOptions(
                            new Camera2ImplConfig.Builder()
                                    .setCaptureRequestOption(
                                            CaptureRequest.CONTROL_AF_MODE,
                                            CaptureRequest.CONTROL_AF_MODE_MACRO)
                                    .setCaptureRequestOption(
                                            CaptureRequest.FLASH_MODE,
                                            CaptureRequest.FLASH_MODE_TORCH)
                                    .build());
                    return builder.build();
                }
            };
            new Camera2ImplConfig.Extender<>(camera2ConfigBuilder)
                    .setCameraEventCallback(
                            new CameraEventCallbacks(cameraEventCallback));

            // Add capture request options for SessionConfig
            camera2ConfigBuilder
                    .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                    .setCaptureRequestOption(
                            CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                    .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            builder.addImplementationOptions(camera2ConfigBuilder.build());

            mSessionConfig = builder.build();

            CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
            captureConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            captureConfigBuilder.addSurface(mDeferrableSurface);
            captureConfigBuilder.addCameraCaptureCallback(mComboCameraCaptureCallback);

            // Add capture request options for CaptureConfig
            captureConfigBuilder.addImplementationOptions(new Camera2ImplConfig.Builder()
                    .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                    .build());

            mCaptureConfig = captureConfigBuilder.build();
        }

        /**
         * Wait for data to get produced by the session.
         *
         * @throws InterruptedException if data is not produced after a set amount of time
         */
        boolean waitForData() throws InterruptedException {
            return mDataLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS, TimeUnit.SECONDS);
        }

        boolean waitForCameraCaptureCallback() throws InterruptedException {
            return mCameraCaptureCallbackLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS,
                    TimeUnit.SECONDS);
        }

        /** Clean up resources. */
        void tearDown() {
            mDeferrableSurface.close();
            mImageReader.close();
            mHandlerThread.quitSafely();
        }

        void setCloseSurfaceOnSessionClose(boolean closeSurfaceOnSessionClose) {
            mCloseSurfaceOnSessionClose = closeSurfaceOnSessionClose;
        }

        boolean getCloseSurfaceOnSessionClose() {
            return mCloseSurfaceOnSessionClose;
        }
    }
}
