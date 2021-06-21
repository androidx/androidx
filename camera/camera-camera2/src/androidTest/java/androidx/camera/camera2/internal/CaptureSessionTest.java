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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.camera2.internal.CaptureSession.State;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
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
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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
@SuppressWarnings("unchecked")
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class CaptureSessionTest {
    /** Thread for all asynchronous calls. */
    private static HandlerThread sHandlerThread;
    /** Handler for all asynchronous calls. */
    private Handler mHandler;
    /** Executor which delegates to Handler */
    private Executor mExecutor;
    /** Scheduled executor service which delegates to Handler */
    private ScheduledExecutorService mScheduledExecutor;

    private CaptureSessionTestParameters mTestParameters0;
    private CaptureSessionTestParameters mTestParameters1;

    private CameraUtil.CameraDeviceHolder mCameraDeviceHolder;

    private CaptureSessionRepository mCaptureSessionRepository;
    private SynchronizedCaptureSessionOpener.Builder mCaptureSessionOpenerBuilder;

    private final List<CaptureSession> mCaptureSessions = new ArrayList<>();

    @Rule
    public TestRule mUseCameraRule = CameraUtil.grantCameraPermissionAndPreTest();

    @BeforeClass
    public static void setUpClass() {
        sHandlerThread = new HandlerThread("CaptureSessionTest");
        sHandlerThread.start();
    }

    @AfterClass
    public static void tearDownClass() {
        if (sHandlerThread != null) {
            sHandlerThread.quitSafely();
        }
    }

    @Before
    public void setup() throws CameraAccessException, InterruptedException,
            AssumptionViolatedException, TimeoutException, ExecutionException {
        mTestParameters0 = new CaptureSessionTestParameters("mTestParameters0");
        mTestParameters1 = new CaptureSessionTestParameters("mTestParameters1");
        mHandler = new Handler(sHandlerThread.getLooper());

        mExecutor = CameraXExecutors.newHandlerExecutor(mHandler);
        mScheduledExecutor = CameraXExecutors.newHandlerExecutor(mHandler);

        mCaptureSessionRepository = new CaptureSessionRepository(mExecutor);

        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSessionOpener.Builder(mExecutor,
                mScheduledExecutor, mHandler, mCaptureSessionRepository,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);

        mCameraDeviceHolder = CameraUtil.getCameraDevice(
                mCaptureSessionRepository.getCameraStateCallback());
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        // Ensure all capture sessions are fully closed
        List<ListenableFuture<Void>> releaseFutures = new ArrayList<>();
        for (CaptureSession captureSession : mCaptureSessions) {
            releaseFutures.add(captureSession.release(/*abortInFlightCaptures=*/false));
        }
        mCaptureSessions.clear();
        Future<?> aggregateReleaseFuture = Futures.allAsList(releaseFutures);
        aggregateReleaseFuture.get(10L, TimeUnit.SECONDS);

        if (mCameraDeviceHolder != null) {
            CameraUtil.releaseCameraDevice(mCameraDeviceHolder);
            mTestParameters0.tearDown();
            mTestParameters1.tearDown();
        }
    }

    @Test
    public void setCaptureSessionSucceed() {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        assertThat(captureSession.getSessionConfig()).isEqualTo(mTestParameters0.mSessionConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void setCaptureSessionOnClosedSession_throwsException() {
        CaptureSession captureSession = createCaptureSession();

        SessionConfig newSessionConfig = mTestParameters0.mSessionConfig;

        captureSession.close();

        // Should throw IllegalStateException
        captureSession.setSessionConfig(newSessionConfig);
    }

    @Test
    public void openCaptureSessionSucceed() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

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
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        surface.close();

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onFailure(any(Throwable.class));
    }

    @Test
    public void captureSessionIncreasesSurfaceUseCountAfterOpen_andDecreasesAfterCameraIsClosed()
            throws InterruptedException, ExecutionException, TimeoutException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        int useCountBeforeOpen = surface.getUseCount();

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onSuccess(any());
        int useCountAfterOpen = surface.getUseCount();

        reset(mockFutureCallback);

        captureSession.close();
        Futures.addCallback(captureSession.release(false), mockFutureCallback,
                CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000)).onSuccess(any());

        // Release the CaptureSession will not wait for the CameraCaptureSession close, the use
        // count of the surface will be decreased after the camera is closed or the new
        // CaptureSession is created. Close the CameraDevice to verify the surface use count
        // will actually decrease.
        CameraUtil.releaseCameraDevice(mCameraDeviceHolder);

        int useCountAfterRelease = surface.getUseCount();

        assertThat(useCountAfterOpen).isGreaterThan(useCountBeforeOpen);
        assertThat(useCountAfterRelease).isEqualTo(useCountBeforeOpen);
    }

    @Test
    public void captureSessionSurfaceUseCount_decreaseAllAfterCameraClose()
            throws InterruptedException, ExecutionException, TimeoutException {

        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        int useCount0BeforeOpen = surface.getUseCount();
        CaptureSession captureSession = createSessionAndWaitOpened(mTestParameters0, 3000);
        int useCount0AfterOpen = surface.getUseCount();

        captureSession.release(false);

        DeferrableSurface surface1 = mTestParameters1.mSessionConfig.getSurfaces().get(0);
        int useCount1BeforeOpen = surface1.getUseCount();
        CaptureSession captureSession1 = createSessionAndWaitOpened(mTestParameters1, 3000);
        int useCount1AfterOpen = surface1.getUseCount();

        captureSession1.release(false);

        CameraUtil.releaseCameraDevice(mCameraDeviceHolder);

        assertThat(useCount0AfterOpen).isGreaterThan(useCount0BeforeOpen);
        assertThat(useCount1AfterOpen).isGreaterThan(useCount1BeforeOpen);

        assertThat(surface.getUseCount()).isEqualTo(0);
        assertThat(surface1.getUseCount()).isEqualTo(0);
    }

    @Test
    public void captureSessionSurfaceUseCount_decreaseAfterNewCaptureSessionConfigured() {
        DeferrableSurface surface = mTestParameters0.mSessionConfig.getSurfaces().get(0);
        int useCountBeforeOpen = surface.getUseCount();
        CaptureSession captureSession = createSessionAndWaitOpened(mTestParameters0, 3000);
        int useCountAfterOpen = surface.getUseCount();

        captureSession.release(false);

        createSessionAndWaitOpened(mTestParameters1, 3000);
        int useCountAfterNewCaptureSessionConfigured = surface.getUseCount();

        assertThat(useCountAfterOpen).isGreaterThan(useCountBeforeOpen);
        assertThat(useCountAfterNewCaptureSessionConfigured).isEqualTo(useCountBeforeOpen);
    }

    @NonNull
    private CaptureSession createSessionAndWaitOpened(
            @NonNull CaptureSessionTestParameters parameters, long waitTimeout) {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(parameters.mSessionConfig);
        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);
        Futures.addCallback(captureSession.open(parameters.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(waitTimeout)).onSuccess(any());

        return captureSession;
    }

    @Test
    public void openCaptureSessionWithOptionOverride() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

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
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.close();

        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void releaseUnopenedSession() {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.release(/*abortInFlightCaptures=*/false);

        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closeOpenedSession() throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        captureSession.close();

        // Session should be in closed state immediately after calling close() on an
        // opening/opened session.
        assertThat(captureSession.getState()).isEqualTo(State.CLOSED);
    }

    @Test
    public void releaseOpenedSession() throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        ListenableFuture<Void> releaseFuture = captureSession.release(
                /*abortInFlightCaptures=*/false);

        // Wait for release
        releaseFuture.get();
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

        // StateCallback.onClosed() should be called to signal the session is closed.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
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
        CaptureSession captureSession0 = createCaptureSession();
        captureSession0.setSessionConfig(mTestParameters0.mSessionConfig);

        // First session is opened
        Future<Void> openFuture0 = captureSession0.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        assertFutureCompletes(openFuture0, 5, TimeUnit.SECONDS);

        captureSession0.close();

        // Open second session, which should cause first one to be released
        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        Future<Void> openFuture1 = captureSession1.open(mTestParameters1.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
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
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

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
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

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
        CaptureSession captureSession0 = createCaptureSession();
        captureSession0.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession0.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
        captureSession0.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        captureSession0.close();

        // Most close the capture session before start a new one, some legacy devices or Android
        // API < M need to recreate the surface for the new CaptureSession.
        captureSession0.release(false);

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(captureSession0.getSessionConfig());
        if (!captureSession0.getCaptureConfigs().isEmpty()) {
            captureSession1.issueCaptureRequests(captureSession0.getCaptureConfigs());
        }
        captureSession1.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void issueCaptureRequestBeforeCaptureSessionOpened() throws InterruptedException {
        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForCameraCaptureCallback());

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test(expected = IllegalStateException.class)
    public void issueCaptureRequestOnClosedSession_throwsException() {
        CaptureSession captureSession = createCaptureSession();

        captureSession.close();

        // Should throw IllegalStateException
        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
    }

    @Test
    public void startStreamingAfterOpenCaptureSession()
            throws InterruptedException, ExecutionException {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForData());
        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        SynchronizedCaptureSession syncCaptureSession = captureSession.mSynchronizedCaptureSession;
        assertFutureCompletes(syncCaptureSession.getSynchronizedBlocker(
                SynchronizedCaptureSessionOpener.FEATURE_WAIT_FOR_REQUEST), 5,
                TimeUnit.SECONDS);

        verify(mTestParameters0.mCamera2CaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureStarted(any(CameraCaptureSession.class), any(CaptureRequest.class),
                        any(Long.class), any(Long.class));
    }

    @Test
    public void surfaceTerminationFutureIsCalledWhenSessionIsClose() throws InterruptedException {
        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSessionOpener.Builder(mExecutor,
                mScheduledExecutor, mHandler, mCaptureSessionRepository,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        assertTrue(mTestParameters0.waitForData());

        Runnable runnable = mock(Runnable.class);
        mTestParameters0.mDeferrableSurface.getTerminationFuture().addListener(runnable,
                CameraXExecutors.directExecutor());

        captureSession.release(/*abortInFlightCaptures=*/false);

        Mockito.verify(runnable, timeout(3000).times(1)).run();
    }

    @Test
    public void cameraEventCallbackInvokedInOrder() {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());
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
    public void cameraEventCallbackInvoked_assignDifferentSessionConfig() {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(new SessionConfig.Builder().build());
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        InOrder inOrder = inOrder(mTestParameters0.mMockCameraEventCallback);
        inOrder.verify(mTestParameters0.mMockCameraEventCallback, timeout(3000)).onPresetSession();
        inOrder.verify(mTestParameters0.mMockCameraEventCallback, timeout(3000)).onEnableSession();
        // Should not trigger repeating since the repeating SessionConfig is empty.
        verify(mTestParameters0.mMockCameraEventCallback, never()).onRepeating();

        captureSession.close();
        inOrder.verify(mTestParameters0.mMockCameraEventCallback, timeout(3000)).onDisableSession();

        verifyNoMoreInteractions(mTestParameters0.mMockCameraEventCallback);
    }

    @Test
    public void cameraEventCallback_requestKeysIssuedSuccessfully() {
        ArgumentCaptor<CameraCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                CameraCaptureResult.class);

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        // Open the capture session and verify the onEnableSession callback would be invoked
        // but onDisableSession callback not.
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

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
                CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM);
        // The onEnableSession should not been invoked in close().
        verify(mTestParameters0.mTestCameraEventCallback.mEnableCallback,
                never()).onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void closingCaptureSessionClosesDeferrableSurface()
            throws ExecutionException, InterruptedException {
        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSessionOpener.Builder(mExecutor,
                mScheduledExecutor, mHandler, mCaptureSessionRepository,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);

        CaptureSession captureSession = createCaptureSession();

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        Future<Void> openFuture = captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        assertFutureCompletes(openFuture, 5, TimeUnit.SECONDS);

        captureSession.release(false);

        // Verify the next CaptureSession should get an invalid DeferrableSurface.
        CaptureSession captureSession1 = createCaptureSession();
        ListenableFuture<Void> openFuture1 = captureSession1.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());

        FutureCallback<Void> futureCallback = Mockito.mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        Futures.addCallback(openFuture1, futureCallback,
                CameraXExecutors.directExecutor());
        verify(futureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());

        assertThat(throwableCaptor.getValue()).isInstanceOf(
                DeferrableSurface.SurfaceClosedException.class);
    }

    @Test
    public void openCaptureSessionTwice_theSecondaryCallShouldNoop() {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());

        // StateCallback.onConfigured() should be called to signal the session is configured.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(captureSession.getState()).isEqualTo(State.OPENED);
    }

    @Test
    public void releaseImmediateAfterOpenCaptureSession()
            throws ExecutionException, InterruptedException {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build());
        Future<Void> releaseFuture = captureSession.release(false);
        assertFutureCompletes(releaseFuture, 5, TimeUnit.SECONDS);

        // The captureSession state should change to RELEASED state
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    @SuppressWarnings("deprecation") /* AsyncTask */
    public void cancelOpenCaptureSessionListenableFuture_shouldNoop() {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ListenableFuture<Void> openingFuture = captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build());
        Futures.addCallback(openingFuture, mockFutureCallback,
                android.os.AsyncTask.THREAD_POOL_EXECUTOR);
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

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        FutureCallback<Void> mockFutureCallback = mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        Futures.addCallback(captureSession.open(mTestParameters0.mSessionConfig,
                mCameraDeviceHolder.get(), mCaptureSessionOpenerBuilder.build()),
                mockFutureCallback, CameraXExecutors.mainThreadExecutor());

        verify(mockFutureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());
        // The captureSession opening should callback onFailure when the DeferrableSurface is
        // already closed.
        assertThat(throwableCaptor.getValue()).isInstanceOf(
                DeferrableSurface.SurfaceClosedException.class);
    }

    private CaptureSession createCaptureSession() {
        CaptureSession captureSession = new CaptureSession();
        mCaptureSessions.add(captureSession);
        return captureSession;
    }

    @Test
    public void issueCaptureCancelledBeforeExecuting() {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        captureSession.cancelIssuedCaptureRequests();

        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCancelled();
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void cameraDisconnected_whenOpeningCaptureSessions_onClosedShouldBeCalled()
            throws CameraAccessException, InterruptedException, ExecutionException,
            TimeoutException {
        List<OutputConfigurationCompat> outputConfigList = new LinkedList<>();
        outputConfigList.add(
                new OutputConfigurationCompat(mTestParameters0.mImageReader.getSurface()));

        SynchronizedCaptureSessionOpener synchronizedCaptureSessionOpener =
                mCaptureSessionOpenerBuilder.build();

        SessionConfigurationCompat sessionConfigCompat =
                synchronizedCaptureSessionOpener.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        outputConfigList,
                        new SynchronizedCaptureSessionStateCallbacks.Adapter(
                                mTestParameters0.mSessionStateCallback));

        // Open the CameraCaptureSession without waiting for the onConfigured() callback.
        synchronizedCaptureSessionOpener.openCaptureSession(mCameraDeviceHolder.get(),
                sessionConfigCompat, mTestParameters0.mSessionConfig.getSurfaces());

        // Open the camera again to simulate the cameraDevice is disconnected
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CameraUtil.CameraDeviceHolder holder = CameraUtil.getCameraDevice(
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                });
        // Only verify the result when the camera can open successfully.
        assumeTrue(countDownLatch.await(3000, TimeUnit.MILLISECONDS));

        // The opened CaptureSession should be closed after the CameraDevice is disconnected.
        verify(mTestParameters0.mSessionStateCallback, timeout(5000)).onClosed(
                any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);

        CameraUtil.releaseCameraDevice(holder);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void cameraDisconnected_captureSessionsOnClosedShouldBeCalled_repeatingStarted()
            throws ExecutionException, InterruptedException, TimeoutException,
            CameraAccessException {
        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        assertTrue(mTestParameters0.waitForData());
        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        assertTrue(mTestParameters1.waitForData());

        // Open the camera again to simulate the cameraDevice is disconnected
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CameraUtil.CameraDeviceHolder holder = CameraUtil.getCameraDevice(
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                });

        // Only verify the result when the camera can open successfully.
        assumeTrue(countDownLatch.await(3000, TimeUnit.MILLISECONDS));

        // The opened CaptureSession should be closed after the CameraDevice is disconnected.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));
        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

        CameraUtil.releaseCameraDevice(holder);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void cameraDisconnected_captureSessionsOnClosedShouldBeCalled_withoutRepeating()
            throws CameraAccessException, InterruptedException, ExecutionException,
            TimeoutException {
        // The CameraCaptureSession will call close() automatically when CameraDevice is
        // disconnected, and the CameraCaptureSession should receive the onClosed() callback if
        // the CameraDevice status is idling.
        // In this test, we didn't start the repeating for the CaptureSession, the CameraDevice
        // status should be in idle statue when we trying to disconnect the CameraDevice. So the
        // CaptureSession can receive the onClosed() callback after the camera is disconnected.
        CaptureSession captureSession = createCaptureSession();
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // Open the camera again to simulate the cameraDevice is disconnected
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CameraUtil.CameraDeviceHolder holder = CameraUtil.getCameraDevice(
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                });
        // Only verify the result when the camera can open successfully.
        assumeTrue(countDownLatch.await(3000, TimeUnit.MILLISECONDS));

        // The opened CaptureSession should be closed after the CameraDevice is disconnected.
        verify(mTestParameters0.mSessionStateCallback, timeout(5000)).onClosed(
                any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);

        CameraUtil.releaseCameraDevice(holder);
    }

    @Test
    public void closePreviousOpeningCaptureSession_afterNewCaptureSessionCreated()
            throws ExecutionException, InterruptedException {

        List<OutputConfigurationCompat> outputConfigList = new LinkedList<>();
        outputConfigList.add(
                new OutputConfigurationCompat(mTestParameters0.mImageReader.getSurface()));

        SynchronizedCaptureSessionOpener synchronizedCaptureSessionOpener =
                mCaptureSessionOpenerBuilder.build();

        SessionConfigurationCompat sessionConfigCompat =
                synchronizedCaptureSessionOpener.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        outputConfigList,
                        new SynchronizedCaptureSessionStateCallbacks.Adapter(
                                mTestParameters0.mSessionStateCallback));

        // Open the CameraCaptureSession without waiting for the onConfigured() callback.
        synchronizedCaptureSessionOpener.openCaptureSession(mCameraDeviceHolder.get(),
                sessionConfigCompat, mTestParameters0.mSessionConfig.getSurfaces());

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The first capture sessions should be closed since the new CaptureSession is created.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
    }

    @Test
    public void closePreviousCaptureSession_afterNewCaptureSessionCreated()
            throws ExecutionException, InterruptedException {

        CaptureSession captureSession = createCaptureSession();
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        // Not close the first capture session before opening the next CaptureSession.

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The first capture sessions should be closed since the new CaptureSession is created.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closePreviousCaptureSession_afterNewCaptureSessionCreated_runningRepeating()
            throws ExecutionException, InterruptedException {

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        assertTrue(mTestParameters0.waitForData());

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The opened capture sessions should be closed.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closePreviousClosingCaptureSession_afterNewCaptureSessionCreated_runningRepeating()
            throws ExecutionException, InterruptedException, TimeoutException {

        CaptureSession captureSession = createCaptureSession();
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        assertTrue(mTestParameters0.waitForData());
        // Call close() before the creating the next CaptureSession.
        captureSession.release(false);

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The opened capture sessions should be closed.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));

        CameraUtil.releaseCameraDevice(mCameraDeviceHolder);

        // Close camera device should close all sessions.
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closePreviousClosingCaptureSession_afterNewCaptureSessionCreated()
            throws ExecutionException, InterruptedException {

        CaptureSession captureSession = createCaptureSession();
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();
        // Call close() before the creating the next CaptureSession.
        captureSession.release(false);

        CaptureSession captureSession1 = createCaptureSession();
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // The opened capture sessions should be closed.
        verify(mTestParameters0.mSessionStateCallback, timeout(3000).times(1))
                .onClosed(any(CameraCaptureSession.class));

        verify(mTestParameters1.mSessionStateCallback, timeout(3000).times(1))
                .onConfigured(any(CameraCaptureSession.class));
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void setSessionConfigWithoutSurface_shouldStopRepeating()
            throws ExecutionException, InterruptedException {
        // Create Surface
        ImageReader imageReader =
                ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, /*maxImages*/ 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireNextImage();
            if (image != null) {
                image.close();
            }
        }, mHandler);
        DeferrableSurface surface = new ImmediateSurface(imageReader.getSurface());

        // Prepare SessionConfig builder
        SessionConfig.Builder builder = new SessionConfig.Builder();
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        CameraCaptureSession.StateCallback stateCallback =
                Mockito.mock(CameraCaptureSession.StateCallback.class);
        builder.addSessionStateCallback(stateCallback);
        CameraCaptureCallback captureCallback =
                Mockito.mock(CameraCaptureCallback.class);
        builder.addRepeatingCameraCaptureCallback(captureCallback);

        // Create SessionConfig without Surface
        SessionConfig sessionConfigWithoutSurface = builder.build();

        // Create SessionConfig with Surface
        builder.addSurface(surface);
        SessionConfig sessionConfigWithSurface = builder.build();

        // Open CaptureSession
        CaptureSession captureSession = createCaptureSession();
        captureSession.open(sessionConfigWithSurface, mCameraDeviceHolder.get(),
                mCaptureSessionOpenerBuilder.build()).get();

        // Activate repeating request
        captureSession.setSessionConfig(sessionConfigWithSurface);
        verify(captureCallback, timeout(3000L).atLeast(3)).onCaptureCompleted(any());

        // Deactivate repeating request
        clearInvocations(stateCallback);
        captureSession.setSessionConfig(sessionConfigWithoutSurface);

        // Wait for #onReady which means there is no repeating request.
        verify(stateCallback, timeout(3000L)).onReady(any());

        // Clean up
        surface.close();
        surface.getTerminationFuture().addListener(() -> imageReader.close(),
                CameraXExecutors.directExecutor());
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
                    CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM, mDisableCallback);
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
        /** Latch to wait for first image data to appear. */
        private final CountDownLatch mDataLatch = new CountDownLatch(1);

        /** Latch to wait for camera capture callback to be invoked. */
        private final CountDownLatch mCameraCaptureCallbackLatch = new CountDownLatch(1);

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
    }
}
