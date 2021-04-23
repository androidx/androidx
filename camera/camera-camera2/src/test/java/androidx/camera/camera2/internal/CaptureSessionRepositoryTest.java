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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CaptureSessionRepositoryTest {
    private static final int NUM_OUTPUTS = 3;

    private CameraCaptureSession mMockCaptureSession;
    private SynchronizedCaptureSession.StateCallback mMockStateCallback;
    private List<OutputConfigurationCompat> mOutputs;
    private SynchronizedCaptureSessionBaseImpl mCaptureSessionCompatBase;
    private CaptureSessionRepository mCaptureSessionRepository;
    private ScheduledExecutorService mScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    @Before
    public void setUp() {
        mCaptureSessionRepository =
                new CaptureSessionRepository(CameraXExecutors.mainThreadExecutor());

        mCaptureSessionCompatBase =
                new SynchronizedCaptureSessionBaseImpl(mCaptureSessionRepository,
                        CameraXExecutors.mainThreadExecutor(), mScheduledExecutorService,
                        mock(Handler.class));

        mMockCaptureSession = mock(CameraCaptureSession.class);
        mMockStateCallback = mock(SynchronizedCaptureSession.StateCallback.class);
        mOutputs = new ArrayList<>(NUM_OUTPUTS);
        for (int i = 0; i < NUM_OUTPUTS; ++i) {
            mOutputs.add(mock(OutputConfigurationCompat.class));
        }
    }

    @Test
    public void onClosedSessionRemoveFromRepository() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mCaptureSessionCompatBase.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mCaptureSessionCompatBase.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        // Simulate the CameraCaptureSession is configured.
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);

        mCaptureSessionCompatBase.close();

        // Simulate the CameraCaptureSession is closed, so the CaptureSessionWrapper should be
        // removed from the Repository.
        sessionConfigurationCompat.getStateCallback().onClosed(mMockCaptureSession);
        assertThat(mCaptureSessionRepository.getCreatingCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getClosingCaptureSession().size()).isEqualTo(0);
    }

    @Test
    public void closingSessionInRepository() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mCaptureSessionCompatBase.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mCaptureSessionCompatBase.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        // Simulate the CameraCaptureSession is configured.
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);

        mCaptureSessionCompatBase.close();


        assertThat(mCaptureSessionRepository.getCreatingCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
        // The closing CaptureSessionWrapper should be in the removing list of the Repository.
        assertThat(mCaptureSessionRepository.getClosingCaptureSession().size()).isEqualTo(1);
    }

    @Test
    public void onConfiguredSessionInRepository() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mCaptureSessionCompatBase.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mCaptureSessionCompatBase.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        assertThat(mCaptureSessionRepository.getCreatingCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);
        assertThat(mCaptureSessionRepository.getClosingCaptureSession().size()).isEqualTo(0);
    }

    @Test
    public void openingSessionInRepository() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mCaptureSessionCompatBase.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mCaptureSessionCompatBase.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));

        assertThat(mCaptureSessionRepository.getCreatingCaptureSessions().size()).isEqualTo(1);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getClosingCaptureSession().size()).isEqualTo(0);
    }

    @Test
    public void onConfigureFailSessionInRepository() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mCaptureSessionCompatBase.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mCaptureSessionCompatBase.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));

        sessionConfigurationCompat.getStateCallback().onConfigureFailed(mMockCaptureSession);

        assertThat(mCaptureSessionRepository.getCreatingCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getClosingCaptureSession().size()).isEqualTo(0);
    }

    @Test
    public void onErrorSessionInRepository() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mCaptureSessionCompatBase.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mCaptureSessionCompatBase.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));

        // Simulate the CameraCaptureSession is configured.
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(1);

        mCaptureSessionRepository.getCameraStateCallback().onError(mock(CameraDevice.class),
                anyInt());

        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mCaptureSessionRepository.getCreatingCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getCaptureSessions().size()).isEqualTo(0);
        assertThat(mCaptureSessionRepository.getClosingCaptureSession().size()).isEqualTo(0);
        // Ensure the onClosed() is called to prevent from CaptureSession leak.
        verify(mMockStateCallback).onClosed(mCaptureSessionCompatBase);
    }
}
