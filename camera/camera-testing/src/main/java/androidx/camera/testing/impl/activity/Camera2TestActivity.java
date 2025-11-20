/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.activity;

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

import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.testing.R;
import androidx.core.util.Preconditions;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** An activity which opens the camera via Camera2 API for testing. */
public class Camera2TestActivity extends Activity {

    private static final String TAG = "Camera2TestActivity";
    private static final int FRAMES_UNTIL_VIEW_IS_READY = 5;
    private static final Size GUARANTEED_RESOLUTION = new Size(640, 480);
    public static final String EXTRA_CAMERA_ID = "androidx.camera.cameraId";
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 500;

    final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    @Nullable CameraDevice mCameraDevice;
    @Nullable CameraCaptureSession mCaptureSession;
    @Nullable Handler mBackgroundHandler;
    CaptureRequest.@Nullable Builder mPreviewRequestBuilder;
    private TextureView mTextureView;
    private @Nullable String mCameraId;
    private @Nullable HandlerThread mBackgroundThread;
    private int mFailedRetries = 0;
    private final AtomicInteger mFrameCount = new AtomicInteger(0);
    private final AtomicBoolean mIsOpening = new AtomicBoolean(false);
    private final AtomicBoolean mIsHandlingFailure = new AtomicBoolean(false);

    @VisibleForTesting
    public final CountingIdlingResource completionIdlingResource =
            new CountingIdlingResource("ActivityCompletion");

    @VisibleForTesting
    public final CountingIdlingResource previewStartedIdlingResource =
            new CountingIdlingResource("PreviewStarted");


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
                    if (mFrameCount.incrementAndGet() >= FRAMES_UNTIL_VIEW_IS_READY) {
                        if (!previewStartedIdlingResource.isIdleNow()) {
                            previewStartedIdlingResource.decrement();
                        }
                        if (!completionIdlingResource.isIdleNow()) {
                            completionIdlingResource.decrement();
                        }
                    }
                }
            };

    private final CameraDevice.StateCallback mDeviceStateCallback =
            new DeviceStateCallbackImpl();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_main);

        mTextureView = findViewById(R.id.textureView);
        mCameraId = getIntent().getStringExtra(EXTRA_CAMERA_ID);
        if (TextUtils.isEmpty(mCameraId)) {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                String[] cameraIds = manager.getCameraIdList();
                if (cameraIds.length > 0) {
                    mCameraId = cameraIds[0];
                }
            } catch (CameraAccessException e) {
                Logger.e(TAG, "Cannot get camera id list.", e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        completionIdlingResource.increment();
        previewStartedIdlingResource.increment();

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
    void openCamera() {
        mFailedRetries = 0;
        tryOpenCamera();
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    void tryOpenCamera() {
        mIsHandlingFailure.set(false);
        if (TextUtils.isEmpty(mCameraId)) {
            Logger.e(TAG, "Camera ID is empty. Cannot open camera.");
            if (!completionIdlingResource.isIdleNow()) {
                completionIdlingResource.decrement();
            }
            return;
        }
        Logger.d(TAG, "Attempting to open camera: " + mCameraId);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Logger.d(TAG, "Camera open already in progress. Skipping redundant retry.");
                return;
            }

            if (mCameraDevice != null) {
                Logger.d(TAG, "Camera was opened while we waited for the lock. Skipping.");
                mCameraOpenCloseLock.release(); // We acquired the lock, so we must release it.
                return;
            }

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            mIsOpening.set(true);
            manager.openCamera(mCameraId, mDeviceStateCallback, mBackgroundHandler);

        } catch (InterruptedException e) {
            // This is the new, correct handling for interruption.
            Logger.w(TAG, "Thread was interrupted while waiting for camera lock.", e);
            // Preserve the interrupted status for higher-level code.
            Thread.currentThread().interrupt();
        } catch (CameraAccessException | SecurityException e) {
            Logger.e(TAG, "Failed to open camera synchronously", e);
            mIsOpening.set(false);
            mCameraOpenCloseLock.release();
            handleCameraFailure(null);
        }
    }

    void closeCamera() {
        boolean lockAcquired = false;
        try {
            lockAcquired = mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
            if (lockAcquired) {
                if (null != mCaptureSession) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            } else {
                Logger.w(TAG, "Timed out trying to acquire lock for closing camera.");
            }
        } catch (InterruptedException e) {
            Logger.w(TAG, "Interrupted while trying to lock camera closing.", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lockAcquired) {
                mCameraOpenCloseLock.release();
            }
        }
    }

    /* createCaptureSession */
    @SuppressWarnings("deprecation")
    void createCameraPreviewSession() {
        mFrameCount.set(0);
        Preconditions.checkNotNull(mCameraDevice);
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            Preconditions.checkNotNull(texture);
            texture.setDefaultBufferSize(GUARANTEED_RESOLUTION.getWidth(),
                    GUARANTEED_RESOLUTION.getHeight());
            Surface surface = new Surface(texture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new SessionStateCallbackImpl(), mBackgroundHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            Logger.w(TAG, "Failed to create capture session. Camera was likely closed.", e);
            handleCameraFailure(mCameraDevice);
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join(500);
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                Logger.e(TAG, "Failed to stop background thread.", e);
            }
        }
    }

    private void handleCameraFailure(@Nullable CameraDevice cameraDevice) {
        if (!mIsHandlingFailure.compareAndSet(false, true)) {
            Logger.d(TAG, "Failure already being handled. Ignoring duplicate call.");
            return;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
        }
        mCameraDevice = null;

        if (mFailedRetries < MAX_RETRIES) {
            mFailedRetries++;
            Logger.d(TAG, "Scheduling retry to open camera. Attempt " + (mFailedRetries + 1));
            if (mBackgroundHandler != null) {
                mBackgroundHandler.postDelayed(this::tryOpenCamera, RETRY_DELAY_MS);
            }
        } else {
            Logger.e(TAG, "Failed to open camera after " + MAX_RETRIES + " retries.");
            if (!completionIdlingResource.isIdleNow()) {
                completionIdlingResource.decrement();
            }
        }
    }

    final class DeviceStateCallbackImpl extends CameraDevice.StateCallback {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Logger.d(TAG, "Camera onOpened: " + cameraDevice.getId());
            if (mIsOpening.compareAndSet(true, false)) {
                mCameraOpenCloseLock.release();
            }
            mIsHandlingFailure.set(false);
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Logger.w(TAG, "Camera onDisconnected: " + cameraDevice.getId());
            if (mIsOpening.compareAndSet(true, false)) {
                mCameraOpenCloseLock.release();
            }
            handleCameraFailure(cameraDevice);
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Logger.e(TAG, "Camera onError: " + cameraDevice.getId() + ", error: " + error);
            if (mIsOpening.compareAndSet(true, false)) {
                mCameraOpenCloseLock.release();
            }
            handleCameraFailure(cameraDevice);
        }
    }

    final class SessionStateCallbackImpl extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            if (null == mCameraDevice) return;
            mCaptureSession = cameraCaptureSession;
            try {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        new CameraCaptureSession.CaptureCallback() {}, mBackgroundHandler);
                Logger.d(TAG, "Camera session configured successfully.");
            } catch (CameraAccessException | IllegalStateException e) {
                Logger.w(TAG, "Failed to start preview, treating as camera failure.", e);
                handleCameraFailure(mCameraDevice);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Logger.e(TAG, "Camera session configuration failed, treating as camera failure.");
            handleCameraFailure(mCameraDevice);
        }
    }
}
