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

package androidx.camera.camera2.impl.compat;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.AsyncTask;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.impl.compat.params.SessionConfigurationCompat;
import androidx.camera.core.impl.utils.MainThreadAsyncHandler;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Tests some of the methods of {@link CameraDeviceCompat} on device.
 *
 * <p>These need to run on device since they rely on native implementation details of the
 * {@link CameraDevice} class on some API levels.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CameraDeviceCompatDeviceTest {

    private final Semaphore mOpenCloseSemaphore = new Semaphore(0);
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);
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

    @Before
    public void setUp() throws CameraAccessException, InterruptedException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        String[] cameraIds = cameraManager.getCameraIdList();
        Assume.assumeTrue("No cameras found on device.", cameraIds.length > 0);
        String cameraId = cameraIds[0];

        cameraManager.openCamera(cameraId, mStateCallback, MainThreadAsyncHandler.getInstance());
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
    }

    // This test should not run on the main thread since it will block the main thread and
    // deadlock on API <= 28.
    @Test
    public void canConfigureCaptureSession() throws CameraAccessException, InterruptedException {
        OutputConfigurationCompat outputConfig = new OutputConfigurationCompat(mSurface);

        final Semaphore configureSemaphore = new Semaphore(0);
        final AtomicBoolean configureSucceeded = new AtomicBoolean(false);
        CameraCaptureSession.StateCallback stateCallback =
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        configureSucceeded.set(true);
                        configureSemaphore.release();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        configureSucceeded.set(false);
                        configureSemaphore.release();
                    }
                };

        SessionConfigurationCompat sessionConfig = new SessionConfigurationCompat(
                SessionConfigurationCompat.SESSION_REGULAR,
                Collections.singletonList(outputConfig), AsyncTask.THREAD_POOL_EXECUTOR,
                stateCallback);

        CameraDeviceCompat.createCaptureSession(mCameraDevice, sessionConfig);
        configureSemaphore.acquire();

        assertThat(configureSucceeded.get()).isTrue();
    }

}
