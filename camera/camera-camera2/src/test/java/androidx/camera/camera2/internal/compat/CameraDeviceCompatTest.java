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

package androidx.camera.camera2.internal.compat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Build;
import android.os.Handler;

import androidx.camera.camera2.internal.compat.params.InputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP,
        instrumentedPackages = { "androidx.camera.camera2.internal.compat.params" })
public final class CameraDeviceCompatTest {

    private static final int NUM_OUTPUTS = 3;

    private CameraDevice mCameraDevice;
    private List<OutputConfigurationCompat> mOutputs;
    private Handler mMockHandler = mock(Handler.class);

    @Before
    public void setUp() {
        mCameraDevice = mock(CameraDevice.class);
        mOutputs = new ArrayList<>(NUM_OUTPUTS);
        for (int i = 0; i < NUM_OUTPUTS; ++i) {
            mOutputs.add(mock(OutputConfigurationCompat.class));
        }
    }

    @Test
    @Config(maxSdk = 23)
    @SuppressWarnings("unchecked")
    public void createCaptureSession_createsSession_withBaseMethod()
            throws CameraAccessException, CameraAccessExceptionCompat {
        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                mOutputs,
                mock(Executor.class),
                mock(CameraCaptureSession.StateCallback.class));

        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice,
                mMockHandler);
        deviceCompat.createCaptureSession(sessionConfig);

        verify(mCameraDevice, times(1)).createCaptureSession(
                any(List.class),
                any(CameraCaptureSession.StateCallback.class),
                eq(mMockHandler));
    }

    @Test
    @Config(minSdk = 24, maxSdk = 27)
    @SuppressWarnings("unchecked")
    public void createCaptureSession_createsSession_byOutputConfiguration()
            throws CameraAccessException, CameraAccessExceptionCompat {
        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                mOutputs,
                mock(Executor.class),
                mock(CameraCaptureSession.StateCallback.class));

        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice);
        deviceCompat.createCaptureSession(sessionConfig);

        verify(mCameraDevice, times(1)).createCaptureSessionByOutputConfigurations(
                any(List.class),
                any(CameraCaptureSession.StateCallback.class),
                any(Handler.class));
    }

    @Test
    @Config(minSdk = 28)
    public void createCaptureSession_createsSession_bySessionConfiguration()
            throws CameraAccessException, CameraAccessExceptionCompat {
        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                mOutputs,
                mock(Executor.class),
                mock(CameraCaptureSession.StateCallback.class));

        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice);
        deviceCompat.createCaptureSession(sessionConfig);

        verify(mCameraDevice, times(1)).createCaptureSession(
                any(SessionConfiguration.class));
    }

    @Test(expected = IllegalArgumentException.class)
    @Config(maxSdk = 22)
    public void createCaptureSession_throwsForReprocessableSession()
            throws CameraAccessExceptionCompat {
        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                mOutputs,
                mock(Executor.class),
                mock(CameraCaptureSession.StateCallback.class));

        // Setting an InputConfiguration will mark the session as reprocessable
        sessionConfig.setInputConfiguration(mock(InputConfigurationCompat.class));

        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice);
        deviceCompat.createCaptureSession(sessionConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    @Config(maxSdk = 22)
    public void createCaptureSession_throwsForHighSpeedSession()
            throws CameraAccessExceptionCompat {
        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_HIGH_SPEED,
                mOutputs,
                mock(Executor.class),
                mock(CameraCaptureSession.StateCallback.class));
        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice);
        deviceCompat.createCaptureSession(sessionConfig);
    }

    @Test
    @Config(minSdk = 23, maxSdk = 23)
    @SuppressWarnings("unchecked")
    public void createCaptureSession_createsReprocessableSession()
            throws CameraAccessException, CameraAccessExceptionCompat {
        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                mOutputs,
                mock(Executor.class),
                mock(CameraCaptureSession.StateCallback.class));

        // Setting an InputConfiguration will mark the session as reprocessable
        sessionConfig.setInputConfiguration(Objects.requireNonNull(
                InputConfigurationCompat.wrap(mock(InputConfiguration.class))));

        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice);
        deviceCompat.createCaptureSession(sessionConfig);

        verify(mCameraDevice, times(1)).createReprocessableCaptureSession(
                any(InputConfiguration.class),
                any(List.class),
                any(CameraCaptureSession.StateCallback.class),
                any(Handler.class));
    }

    @Test
    @Config(minSdk = 24, maxSdk = 27)
    @SuppressWarnings("unchecked")
    public void createCaptureSession_createsReprocessableSession_byConfiguration()
            throws CameraAccessException, CameraAccessExceptionCompat {
        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                mOutputs,
                mock(Executor.class),
                mock(CameraCaptureSession.StateCallback.class));

        // Setting an InputConfiguration will mark the session as reprocessable
        sessionConfig.setInputConfiguration(Objects.requireNonNull(
                InputConfigurationCompat.wrap(mock(InputConfiguration.class))));

        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice);
        deviceCompat.createCaptureSession(sessionConfig);

        verify(mCameraDevice, times(1)).createReprocessableCaptureSessionByConfigurations(
                any(InputConfiguration.class),
                any(List.class),
                any(CameraCaptureSession.StateCallback.class),
                any(Handler.class));
    }

    @Test
    @Config(minSdk = 23, maxSdk = 27)
    @SuppressWarnings("unchecked")
    public void createCaptureSession_createsHighSpeedSession()
            throws CameraAccessException, CameraAccessExceptionCompat {
        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_HIGH_SPEED,
                mOutputs,
                mock(Executor.class),
                mock(CameraCaptureSession.StateCallback.class));

        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice);
        deviceCompat.createCaptureSession(sessionConfig);

        verify(mCameraDevice, times(1)).createConstrainedHighSpeedCaptureSession(
                any(List.class),
                any(CameraCaptureSession.StateCallback.class),
                any(Handler.class));
    }
}
