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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.testing.CameraUtil;
import androidx.core.os.HandlerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.Semaphore;


/**
 * Tests some of the methods of {@link CameraDeviceCompat} on device.
 *
 * <p>These need to run on device since they rely on native implementation details of the
 * {@link CameraDevice} class on some API levels.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public final class CameraDeviceCompatDeviceTest {

    private final Semaphore mOpenCloseSemaphore = new Semaphore(0);
    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();
    private CameraDevice mCameraDevice;
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            mOpenCloseSemaphore.release();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            mOpenCloseSemaphore.release();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraDevice = camera;
            mOpenCloseSemaphore.release();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            mCameraDevice = null;
            mOpenCloseSemaphore.release();
        }
    };
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private HandlerThread mCompatHandlerThread;
    private Handler mCompatHandler;

    @Before
    public void setUp() throws CameraAccessException, InterruptedException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        String[] cameraIds = cameraManager.getCameraIdList();
        Assume.assumeTrue("No cameras found on device.", cameraIds.length > 0);
        String cameraId = cameraIds[0];

        mCompatHandlerThread = new HandlerThread("DispatchThread");
        mCompatHandlerThread.start();
        mCompatHandler = HandlerCompat.createAsync(mCompatHandlerThread.getLooper());

        cameraManager.openCamera(cameraId, mStateCallback, mCompatHandler);
        mOpenCloseSemaphore.acquire();

        if (mCameraDevice == null) {
            throw new AssertionError("Unable to open camera.");
        }

        StreamConfigurationMap streamConfigurationMap = cameraManager.getCameraCharacteristics(
                cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] validSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);

        Assume.assumeTrue("No valid sizes available for SurfaceTexture.", validSizes.length > 0);

        mSurfaceTexture = new SurfaceTexture(0);
        mSurfaceTexture.setDefaultBufferSize(validSizes[0].getWidth(), validSizes[0].getHeight());

        mSurface = new Surface(mSurfaceTexture);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mOpenCloseSemaphore.acquire();
        }

        if (mSurface != null) {
            mSurface.release();
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }

        if (mCompatHandlerThread != null) {
            mCompatHandlerThread.quitSafely();
        }
    }

    @Test
    @SuppressWarnings("deprecation") /* AsyncTask */
    public void canConfigureCaptureSession() throws CameraAccessExceptionCompat {
        OutputConfigurationCompat outputConfig = new OutputConfigurationCompat(mSurface);

        CameraCaptureSession.StateCallback stateCallback =
                mock(CameraCaptureSession.StateCallback.class);

        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                Collections.singletonList(outputConfig), android.os.AsyncTask.THREAD_POOL_EXECUTOR,
                stateCallback);

        CameraDeviceCompat deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(mCameraDevice,
                mCompatHandler);
        try {
            deviceCompat.createCaptureSession(sessionConfig);
        } catch (CameraAccessExceptionCompat e) {
            // If the camera has been disconnected during the test (likely due to another process
            // stealing the camera), then we will skip the test.
            Assume.assumeTrue("Camera disconnected during test.",
                    e.getReason() != CameraAccessException.CAMERA_DISCONNECTED);

            // This is not an error we expect should reasonably happen. Rethrow the exception.
            throw e;
        }

        verify(stateCallback, timeout(3000)).onConfigured(any(CameraCaptureSession.class));
    }

}
