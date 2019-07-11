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
import static org.mockito.Mockito.inOrder;
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
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CaptureSession.State;
import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CameraCaptureCallbacks;
import androidx.camera.core.CameraCaptureResult;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeferrableSurface;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.MutableOptionsBundle;
import androidx.camera.core.SessionConfig;
import androidx.camera.testing.CameraUtil;
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

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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

    private CameraDevice mCameraDevice;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setup() throws CameraAccessException, InterruptedException,
            AssumptionViolatedException {
        assumeTrue(CameraUtil.deviceHasCamera());
        mTestParameters0 = new CaptureSessionTestParameters("mTestParameters0");
        mTestParameters1 = new CaptureSessionTestParameters("mTestParameters1");
        mCameraDevice = CameraUtil.getCameraDevice();
    }

    @After
    public void tearDown() {
        if (mCameraDevice != null) {
            mTestParameters0.tearDown();
            mTestParameters1.tearDown();
            CameraUtil.releaseCameraDevice(mCameraDevice);
        }
    }

    @Test
    public void setCaptureSessionSucceed() {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);

        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        assertThat(captureSession.getSessionConfig()).isEqualTo(mTestParameters0.mSessionConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void setCaptureSessionOnClosedSession_throwsException() {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        SessionConfig newSessionConfig = mTestParameters0.mSessionConfig;

        captureSession.close();

        // Should throw IllegalStateException
        captureSession.setSessionConfig(newSessionConfig);
    }

    @Test
    public void openCaptureSessionSucceed() throws CameraAccessException, InterruptedException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        mTestParameters0.waitForData();

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        // StateCallback.onConfigured() should be called to signal the session is configured.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onConfigured(any(CameraCaptureSession.class));

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mSessionCameraCaptureCallback, timeout(3000).atLeastOnce())
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void openCaptureSessionWithOptionOverride()
            throws CameraAccessException, InterruptedException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        mTestParameters0.waitForData();

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
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.close();

        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void releaseUnopenedSession() throws ExecutionException, InterruptedException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        ListenableFuture<Void> releaseFuture = captureSession.release();

        // Wait for release
        releaseFuture.get();

        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);
    }

    @Test
    public void closeOpenedSession()
            throws CameraAccessException, InterruptedException, ExecutionException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        captureSession.close();

        // Session should be in closed state immediately after calling close() on an
        // opening/opened session.
        assertThat(captureSession.getState()).isEqualTo(State.CLOSED);

        // Release the session to clean up for next test
        ListenableFuture<Void> releaseFuture = captureSession.release();

        // Wait for release to finish
        releaseFuture.get();
    }

    @Test
    public void releaseOpenedSession()
            throws CameraAccessException, InterruptedException, ExecutionException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);
        ListenableFuture<Void> releaseFuture = captureSession.release();

        // Wait for release
        releaseFuture.get();
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

        // StateCallback.onClosed() should be called to signal the session is closed.
        verify(mTestParameters0.mSessionStateCallback, times(1))
                .onClosed(any(CameraCaptureSession.class));
    }

    @Test
    public void openSecondSession() throws CameraAccessException, InterruptedException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        // First session is opened
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);
        captureSession.close();

        // Open second session, which should cause first one to be released
        CaptureSession captureSession1 = new CaptureSession(mTestParameters1.mHandler);
        captureSession1.setSessionConfig(mTestParameters1.mSessionConfig);
        captureSession1.open(mTestParameters1.mSessionConfig, mCameraDevice);

        mTestParameters1.waitForData();

        assertThat(captureSession1.getState()).isEqualTo(State.OPENED);
        assertThat(captureSession.getState()).isEqualTo(State.RELEASED);

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
    public void issueCaptureRequest() throws CameraAccessException, InterruptedException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        mTestParameters0.waitForData();

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        mTestParameters0.waitForCameraCaptureCallback();

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void issueCaptureRequestAppendAndOverrideRepeatingOptions()
            throws CameraAccessException, InterruptedException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        mTestParameters0.waitForData();

        assertThat(captureSession.getState()).isEqualTo(State.OPENED);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));

        mTestParameters0.waitForCameraCaptureCallback();

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
    public void issueCaptureRequestAcrossCaptureSessions()
            throws CameraAccessException, InterruptedException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        captureSession.close();
        CaptureSession captureSession2 = new CaptureSession(mTestParameters0.mHandler);
        captureSession2.setSessionConfig(captureSession.getSessionConfig());
        if (!captureSession.getCaptureConfigs().isEmpty()) {
            captureSession2.issueCaptureRequests(captureSession.getCaptureConfigs());
        }
        captureSession2.open(mTestParameters0.mSessionConfig, mCameraDevice);

        mTestParameters0.waitForCameraCaptureCallback();

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void issueCaptureRequestBeforeCaptureSessionOpened()
            throws CameraAccessException, InterruptedException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        mTestParameters0.waitForCameraCaptureCallback();

        // CameraCaptureCallback.onCaptureCompleted() should be called to signal a capture attempt.
        verify(mTestParameters0.mCameraCaptureCallback, timeout(3000).times(1))
                .onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test(expected = IllegalStateException.class)
    public void issueCaptureRequestOnClosedSession_throwsException() {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);

        captureSession.close();

        // Should throw IllegalStateException
        captureSession.issueCaptureRequests(
                Collections.singletonList(mTestParameters0.mCaptureConfig));
    }

    @Test
    public void surfaceOnDetachedListenerIsCalledWhenSessionIsClose()
            throws CameraAccessException, InterruptedException, ExecutionException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        mTestParameters0.waitForData();

        DeferrableSurface.OnSurfaceDetachedListener listener =
                Mockito.mock(DeferrableSurface.OnSurfaceDetachedListener.class);
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        mTestParameters0.mDeferrableSurface.setOnSurfaceDetachedListener(executor, listener);

        ListenableFuture<Void> releaseFuture = captureSession.release();

        // Wait for release
        releaseFuture.get();

        Mockito.verify(listener, times(1)).onSurfaceDetached();
    }

    @Test
    public void cameraEventCallbackInvokedInOrder() throws CameraAccessException {
        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

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
    public void cameraEventCallback_requestKeysIssuedSuccessfully() throws CameraAccessException {
        ArgumentCaptor<CameraCaptureResult> captureResultCaptor = ArgumentCaptor.forClass(
                CameraCaptureResult.class);

        CaptureSession captureSession = new CaptureSession(mTestParameters0.mHandler);
        captureSession.setSessionConfig(mTestParameters0.mSessionConfig);

        // Open the capture session and verify the onEnableSession callback would be invoked
        // but onDisableSession callback not.
        captureSession.open(mTestParameters0.mSessionConfig, mCameraDevice);

        // Verify the request options in onEnableSession.
        verify(mTestParameters0.mTestCameraEventCallback.mEnableCallback,
                timeout(3000)).onCaptureCompleted(captureResultCaptor.capture());
        CameraCaptureResult result1 = captureResultCaptor.getValue();
        assertThat(result1).isInstanceOf(Camera2CameraCaptureResult.class);
        CaptureResult captureResult1 = ((Camera2CameraCaptureResult) result1).getCaptureResult();
        assertThat(captureResult1.getRequest().get(CaptureRequest.CONTROL_EFFECT_MODE)).isEqualTo(
                CaptureRequest.CONTROL_EFFECT_MODE_NEGATIVE);
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
        assertThat(captureResult2.getRequest().get(CaptureRequest.CONTROL_EFFECT_MODE)).isEqualTo(
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);
        // The onEnableSession should not been invoked in close().
        verify(mTestParameters0.mTestCameraEventCallback.mEnableCallback,
                never()).onCaptureCompleted(any(CameraCaptureResult.class));
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
            return getCaptureConfig(CaptureRequest.CONTROL_EFFECT_MODE,
                    CaptureRequest.CONTROL_EFFECT_MODE_MONO, null);
        }

        @Override
        public CaptureConfig onEnableSession() {
            return getCaptureConfig(CaptureRequest.CONTROL_EFFECT_MODE,
                    CaptureRequest.CONTROL_EFFECT_MODE_NEGATIVE, mEnableCallback);
        }

        @Override
        public CaptureConfig onRepeating() {
            return getCaptureConfig(CaptureRequest.CONTROL_EFFECT_MODE,
                    CaptureRequest.CONTROL_EFFECT_MODE_SOLARIZE, null);
        }

        @Override
        public CaptureConfig onDisableSession() {
            return getCaptureConfig(CaptureRequest.CONTROL_EFFECT_MODE,
                    CaptureRequest.CONTROL_EFFECT_MODE_SEPIA, mDisableCallback);
        }
    }

    private static CaptureConfig getCaptureConfig(CaptureRequest.Key key, int effectValue,
            CameraCaptureCallback callback) {
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        Camera2Config.Builder camera2ConfigurationBuilder =
                new Camera2Config.Builder();
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
            mHandler = new Handler(mHandlerThread.getLooper());

            mImageReader =
                    ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, /*maxImages*/ 2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);

            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            mDeferrableSurface = new ImmediateSurface(mImageReader.getSurface());
            builder.addSurface(mDeferrableSurface);
            builder.addSessionStateCallback(mSessionStateCallback);
            builder.addRepeatingCameraCaptureCallback(mSessionCameraCaptureCallback);

            MutableOptionsBundle testCallbackConfig = MutableOptionsBundle.create();
            testCallbackConfig.insertOption(Camera2Config.CAMERA_EVENT_CALLBACK_OPTION,
                    new CameraEventCallbacks(mTestCameraEventCallback));
            builder.addImplementationOptions(testCallbackConfig);

            MutableOptionsBundle mockCameraEventCallbackConfig = MutableOptionsBundle.create();
            mockCameraEventCallbackConfig.insertOption(Camera2Config.CAMERA_EVENT_CALLBACK_OPTION,
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

            Camera2Config.Builder camera2ConfigBuilder = new Camera2Config.Builder();

            // Add capture request options for CameraEventCallbacks
            CameraEventCallback cameraEventCallback = new CameraEventCallback() {
                @Override
                public CaptureConfig onRepeating() {
                    CaptureConfig.Builder builder = new CaptureConfig.Builder();
                    builder.addImplementationOptions(
                            new Camera2Config.Builder()
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
            new Camera2Config.Extender(camera2ConfigBuilder)
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
            captureConfigBuilder.addImplementationOptions(new Camera2Config.Builder()
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
        void waitForData() throws InterruptedException {
            mDataLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS, TimeUnit.SECONDS);
        }

        void waitForCameraCaptureCallback() throws InterruptedException {
            mCameraCaptureCallbackLatch.await(TIME_TO_WAIT_FOR_DATA_SECONDS, TimeUnit.SECONDS);
        }

        /** Clean up resources. */
        void tearDown() {
            mImageReader.close();
            mHandlerThread.quitSafely();
        }
    }
}
