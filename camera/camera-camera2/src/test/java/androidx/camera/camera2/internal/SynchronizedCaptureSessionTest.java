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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class SynchronizedCaptureSessionTest {
    private static final int NUM_OUTPUTS = 3;

    private CameraCaptureSession mMockCaptureSession;
    private SynchronizedCaptureSession.StateCallback mMockStateCallback;
    private List<OutputConfigurationCompat> mOutputs;
    private CaptureSessionRepository mCaptureSessionRepository;
    private SynchronizedCaptureSessionOpener mSynchronizedCaptureSessionOpener;
    private SynchronizedCaptureSessionOpener.Builder mCaptureSessionOpenerBuilder;
    private ScheduledExecutorService mScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private List<DeferrableSurface> mFakeDeferrableSurfaces;
    private DeferrableSurface mDeferrableSurface1;
    private DeferrableSurface mDeferrableSurface2;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        mCaptureSessionRepository =
                new CaptureSessionRepository(android.os.AsyncTask.THREAD_POOL_EXECUTOR);

        mDeferrableSurface1 = mock(DeferrableSurface.class);
        mDeferrableSurface2 = mock(DeferrableSurface.class);

        mFakeDeferrableSurfaces = new ArrayList<>();
        mFakeDeferrableSurfaces.add(mDeferrableSurface1);
        mFakeDeferrableSurfaces.add(mDeferrableSurface2);

        Set<String> enabledFeature = new HashSet<>();
        enabledFeature.add(SynchronizedCaptureSessionOpener.FEATURE_FORCE_CLOSE);
        enabledFeature.add(SynchronizedCaptureSessionOpener.FEATURE_DEFERRABLE_SURFACE_CLOSE);

        mCaptureSessionOpenerBuilder = new SynchronizedCaptureSessionOpener.Builder(
                android.os.AsyncTask.SERIAL_EXECUTOR, mScheduledExecutorService,
                mock(Handler.class), mCaptureSessionRepository, -1);
        mSynchronizedCaptureSessionOpener = mCaptureSessionOpenerBuilder.build();

        mMockCaptureSession = mock(CameraCaptureSession.class);
        mMockStateCallback = mock(SynchronizedCaptureSession.StateCallback.class);
        mOutputs = new ArrayList<>(NUM_OUTPUTS);
        for (int i = 0; i < NUM_OUTPUTS; ++i) {
            mOutputs.add(mock(OutputConfigurationCompat.class));
        }
    }

    /**
     * Test for the camera capture session can successfully callback onClosed() when the new
     * capture session is configured.
     *
     * This test is for the workaround logic: "Callback the CameraCaptureSession
     * .StateCallback#onClosed() directly when a camera capture session is created."
     *
     * The workaround is now apply for Android API < 23, if we change the workaround rule, the
     * test should be changed accordingly.
     */
    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void onClosedShouldCalled_afterNewCaptureSessionConfigured() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSynchronizedCaptureSessionOpener.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        CameraCaptureSession mockCaptureSession1 = mock(CameraCaptureSession.class);
        SynchronizedCaptureSession.StateCallback mockStateCallback1 = mock(
                SynchronizedCaptureSession.StateCallback.class);
        SynchronizedCaptureSessionOpener captureSessionUtil1 =
                mCaptureSessionOpenerBuilder.build();
        SessionConfigurationCompat sessionConfigurationCompat1 =
                captureSessionUtil1.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mockStateCallback1);

        mSynchronizedCaptureSessionOpener.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);
        captureSessionUtil1.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat1.getStateCallback().onConfigured(mockCaptureSession1);

        verify(mMockStateCallback).onClosed(any(SynchronizedCaptureSession.class));
    }

    /**
     * The test should change if the camera capture session can successfully callback
     * onClosed() when camera device is disconnected.
     *
     * This test is for the workaround logic "Callback the CameraCaptureSession
     * .StateCallback#onClosed() directly when the camera device is disconnected."
     *
     * If we change the workaround logic, the test should be changed accordingly.
     */
    @Test
    public void onClosedShouldCalled_cameraDisconnect() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSynchronizedCaptureSessionOpener.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSynchronizedCaptureSessionOpener.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mCaptureSessionRepository.getCameraStateCallback().onDisconnected(mock(CameraDevice.class));

        verify(mMockStateCallback, timeout(3000)).onClosed(any(SynchronizedCaptureSession.class));
    }

    @Test
    public void onClosedShouldOnlyCalledOnce() {
        SessionConfigurationCompat sessionConfigurationCompat =
                mSynchronizedCaptureSessionOpener.createSessionConfigurationCompat(
                        SessionConfigurationCompat.SESSION_REGULAR,
                        mOutputs,
                        mMockStateCallback);

        mSynchronizedCaptureSessionOpener.openCaptureSession(mock(CameraDevice.class),
                sessionConfigurationCompat,
                Arrays.asList(new ImmediateSurface(mock(Surface.class))));
        sessionConfigurationCompat.getStateCallback().onConfigured(mMockCaptureSession);

        mCaptureSessionRepository.getCameraStateCallback().onDisconnected(mock(CameraDevice.class));

        sessionConfigurationCompat.getStateCallback().onClosed(mMockCaptureSession);

        verify(mMockStateCallback, timeout(3000).times(1)).onClosed(
                any(SynchronizedCaptureSession.class));
    }
}
