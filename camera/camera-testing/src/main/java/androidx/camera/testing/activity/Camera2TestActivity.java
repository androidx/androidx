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

package androidx.camera.testing.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.testing.R;
import androidx.core.util.Preconditions;
import androidx.test.espresso.idling.CountingIdlingResource;

import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** An activity which opens the camera via Camera2 API for testing. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Camera2TestActivity extends Activity {

    private static final String TAG = "Camera2TestActivity";
    private static final int FRAMES_UNTIL_VIEW_IS_READY = 5;
    private static final Size GUARANTEED_RESOLUTION = new Size(640, 480);
    public static final String EXTRA_CAMERA_ID = "androidx.camera.cameraId";

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    @Nullable
    CameraDevice mCameraDevice;
    @Nullable
    CameraCaptureSession mCaptureSession;
    @Nullable
    Handler mBackgroundHandler;
    @Nullable
    CaptureRequest.Builder mPreviewRequestBuilder;
    private TextureView mTextureView;
    @Nullable
    private String mCameraId;
    @Nullable
    private HandlerThread mBackgroundThread;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {

                @SuppressLint("MissingPermission")
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture texture, int width,
                        int height) {
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture texture, int width,
                        int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {
                    // Wait until surface texture receives enough updates.
                    if (!mPreviewReady.isIdleNow()) {
                        mPreviewReady.decrement();
                    }
                }

            };

    private final CameraDevice.StateCallback mDeviceStateCallback =
            new DeviceStateCallbackImpl();

    @VisibleForTesting
    public final CountingIdlingResource mPreviewReady = new CountingIdlingResource("PreviewReady");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_main);
        mTextureView = findViewById(R.id.textureView);
        mCameraId = getIntent().getStringExtra(EXTRA_CAMERA_ID);
        if (TextUtils.isEmpty(mCameraId)) {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            // Use the first camera available.
            String[] cameraIds = new String[0];
            try {
                cameraIds = manager.getCameraIdList();
            } catch (CameraAccessException e) {
                Logger.d(TAG, "Cannot find default camera id");
            }
            if (cameraIds.length > 0) {
                mCameraId = cameraIds[0];
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        for (int i = 0; i < FRAMES_UNTIL_VIEW_IS_READY; i++) {
            mPreviewReady.increment();
        }

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @SuppressWarnings("CatchAndPrintStackTrace")
    void openCamera() {
        if (TextUtils.isEmpty(mCameraId)) {
            Logger.d(TAG, "Cannot open the camera");
            return;
        }
        Logger.d(TAG, "Opening camera: " + mCameraId);

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    /* createCaptureSession */
    @SuppressWarnings({"deprecation", "CatchAndPrintStackTrace"})
    void createCameraPreviewSession() {
        Preconditions.checkNotNull(mCameraDevice);
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();

            // We configure the size of default buffer to be the size of the guaranteed supported
            // resolution, which is 640x480.
            texture.setDefaultBufferSize(GUARANTEED_RESOLUTION.getWidth(),
                    GUARANTEED_RESOLUTION.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new SessionStateCallbackImpl(), null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    @SuppressWarnings("CatchAndPrintStackTrace")
    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
        }
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    final class DeviceStateCallbackImpl extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Logger.d(TAG, "Camera onOpened: " + cameraDevice);
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Logger.d(TAG, "Camera onDisconnected: " + cameraDevice);
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Logger.d(TAG, "Camera onError: " + cameraDevice);
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    final class SessionStateCallbackImpl extends CameraCaptureSession.StateCallback {

        @Override
        @SuppressWarnings("CatchAndPrintStackTrace")
        public void onConfigured(
                @NonNull CameraCaptureSession cameraCaptureSession) {
            // The camera is already closed
            if (null == mCameraDevice) {
                return;
            }

            // When the session is ready, we start displaying the preview.
            mCaptureSession = cameraCaptureSession;
            try {
                // Auto focus should be continuous for camera preview.
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                // Finally, we start displaying the camera preview.
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        new CameraCaptureSession.CaptureCallback() {

                        },
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(
                @NonNull CameraCaptureSession cameraCaptureSession) {
        }
    }
}
