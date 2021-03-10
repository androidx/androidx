/*
 * Copyright 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SuppressWarnings({"deprecation", "unchecked"})
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class SynchronizedCaptureSessionBaseTest {

    private static final int NUM_OUTPUTS = 3;

    private CameraCaptureSession mMockCaptureSession;
    private SynchronizedCaptureSession.StateCallback mMockStateCallback;
    private List<OutputConfigurationCompat> mOutputs;
    private SynchronizedCaptureSessionBaseImpl mSyncCaptureSessionBaseImpl;
    private CaptureSessionRepository mMockCaptureSessionRepository;
    private ScheduledExecutorService mScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    @Before
    public void setUp() {
        mMockCaptureSessionRepository = mock(CaptureSessionRepository.class);

        mSyncCaptureSessionBaseImpl =
                new SynchronizedCaptureSessionBaseImpl(mMockCaptureSessionRepository,
                        CameraXExecutors.directExecutor(), mScheduledExecutorService,
                        mock(Handler.class));

        mMockCaptureSession = mock(CameraCaptureSession.class);
        mMockStateCallback = mock(SynchronizedCaptureSession.StateCallback.class);
        mOutputs = new ArrayList<>(NUM_OUTPUTS);
        for (int i = 0; i < NUM_OUTPUTS; ++i) {
            mOutputs.add(mock(OutputConfigurationCompat.class));
        }
    }

    @Test
    public void callbackShouldWork_onReady() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        sessionConfigurationCompat.getStateCallback().onReady(mMockCaptureSession);

        verify(mMockStateCallback).onReady(any(SynchronizedCaptureSession.class));
    }

    @Test
    public void callbackShouldWork_onActive() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        sessionConfigurationCompat.getStateCallback().onActive(mMockCaptureSession);

        verify(mMockStateCallback).onActive(any(SynchronizedCaptureSession.class));
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.O)
    public void callbackShouldWork_onCaptureQueueEmpty() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        sessionConfigurationCompat.getStateCallback().onCaptureQueueEmpty(mMockCaptureSession);

        verify(mMockStateCallback).onCaptureQueueEmpty(any(SynchronizedCaptureSession.class));
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void callbackShouldWork_onSurfacePrepared() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        sessionConfigurationCompat.getStateCallback().onSurfacePrepared(mMockCaptureSession,
                mock(Surface.class));

        verify(mMockStateCallback).onSurfacePrepared(any(SynchronizedCaptureSession.class),
                any(Surface.class));
    }

    @Test
    public void callbackShouldWork_onConfigured() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        verify(mMockStateCallback).onConfigured(any(SynchronizedCaptureSession.class));
        verify(mMockCaptureSessionRepository, times(1)).onCaptureSessionCreated(
                any(SynchronizedCaptureSession.class));
    }

    @Test
    public void callbackShouldWork_onConfigureFailed() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigureFailed(mMockCaptureSession);

        verify(mMockStateCallback).onConfigureFailed(any(SynchronizedCaptureSession.class));
    }

    @Test
    public void callbackShouldWork_onClosed() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);
        sessionConfigurationCompat.getStateCallback().onClosed(mMockCaptureSession);

        verify(mMockStateCallback).onClosed(any(SynchronizedCaptureSession.class));
        verify(mMockCaptureSessionRepository, times(1)).onCaptureSessionClosed(
                any(SynchronizedCaptureSession.class));
    }

    @Test
    public void callClose_onSessionFinished_shouldBeCalled() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);
        mSyncCaptureSessionBaseImpl.close();

        verify(mMockStateCallback).onSessionFinished(
                any(SynchronizedCaptureSession.class));
    }

    @Test
    public void callbackShouldWork_onSessionFinished() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);
        sessionConfigurationCompat.getStateCallback().onClosed(mMockCaptureSession);

        verify(mMockStateCallback).onSessionFinished(
                any(SynchronizedCaptureSession.class));
    }

    @Test
    public void shouldForwardAfterOnConfigured() throws CameraAccessException {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mSyncCaptureSessionBaseImpl.captureSingleRequest(mock(CaptureRequest.class),
                mock(CameraCaptureSession.CaptureCallback.class));
    }

    @Test
    public void forwardBeforeOnConfigured_captureBurstRequests() throws CameraAccessException {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mSyncCaptureSessionBaseImpl.captureBurstRequests(mock(List.class),
                mock(CameraCaptureSession.CaptureCallback.class));
    }

    @Test
    public void forwardBeforeOnConfigured_setRepeatingBurstRequests()
            throws CameraAccessException {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mSyncCaptureSessionBaseImpl.setRepeatingBurstRequests(mock(List.class),
                mock(CameraCaptureSession.CaptureCallback.class));
    }

    @Test
    public void forwardBeforeOnConfigured_setSingleRepeatingRequest()
            throws CameraAccessException {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mSyncCaptureSessionBaseImpl.setSingleRepeatingRequest(mock(CaptureRequest.class),
                mock(CameraCaptureSession.CaptureCallback.class));
    }

    @Test
    public void forwardBeforeOnConfigured_abortCaptures()
            throws CameraAccessException {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mSyncCaptureSessionBaseImpl.abortCaptures();
    }

    @Test
    public void forwardBeforeOnConfigured_stopRepeating()
            throws CameraAccessException {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mSyncCaptureSessionBaseImpl.stopRepeating();
    }

    @Test
    public void forwardBeforeOnConfigured_getDevice() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSyncCaptureSessionBaseImpl.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSyncCaptureSessionBaseImpl.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mSyncCaptureSessionBaseImpl.getDevice();
    }

    @Test(expected = NullPointerException.class)
    public void notForwardBeforeOnConfigured_captureSingleRequest() throws CameraAccessException {
        mSyncCaptureSessionBaseImpl.captureSingleRequest(mock(CaptureRequest.class),
                mock(CameraCaptureSession.CaptureCallback.class));
    }

    @Test(expected = NullPointerException.class)
    public void notForwardBeforeOnConfigured_captureBurstRequests() throws CameraAccessException {
        mSyncCaptureSessionBaseImpl.captureBurstRequests(mock(List.class),
                mock(CameraCaptureSession.CaptureCallback.class));
    }

    @Test(expected = NullPointerException.class)
    public void notForwardBeforeOnConfigured_setRepeatingBurstRequests()
            throws CameraAccessException {
        mSyncCaptureSessionBaseImpl.setRepeatingBurstRequests(mock(List.class),
                mock(CameraCaptureSession.CaptureCallback.class));
    }

    @Test(expected = NullPointerException.class)
    public void notForwardBeforeOnConfigured_setSingleRepeatingRequest()
            throws CameraAccessException {
        mSyncCaptureSessionBaseImpl.setSingleRepeatingRequest(mock(CaptureRequest.class),
                mock(CameraCaptureSession.CaptureCallback.class));
    }

    @Test(expected = NullPointerException.class)
    public void notForwardBeforeOnConfigured_abortCaptures()
            throws CameraAccessException {
        mSyncCaptureSessionBaseImpl.abortCaptures();
    }

    @Test(expected = NullPointerException.class)
    public void notForwardBeforeOnConfigured_stopRepeating()
            throws CameraAccessException {
        mSyncCaptureSessionBaseImpl.stopRepeating();
    }

    @Test(expected = NullPointerException.class)
    public void notForwardBeforeOnConfigured_getDevice() {
        mSyncCaptureSessionBaseImpl.getDevice();
    }

}
