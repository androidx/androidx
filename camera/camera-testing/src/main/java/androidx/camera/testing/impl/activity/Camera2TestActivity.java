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

import androidx.annotation.GuardedBy;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.testing.R;
import androidx.camera.testing.impl.ActivityScreenUtil;
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
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;
    private static final long ATTEMPT_TIMEOUT_MS = 5000;

    final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    final Object mResourcesLock = new Object();
    @GuardedBy("mResourcesLock")
    @Nullable CameraDevice mCameraDevice;
    @GuardedBy("mResourcesLock")
    @Nullable CameraCaptureSession mCaptureSession;
    @GuardedBy("mResourcesLock")
    @Nullable Surface mPreviewSurface;
    volatile @Nullable Handler mBackgroundHandler;
    CaptureRequest.@Nullable Builder mPreviewRequestBuilder;
    private TextureView mTextureView;
    private @Nullable String mCameraId;
    private @Nullable HandlerThread mBackgroundThread;
    private int mFailedRetries = 0;
    private final AtomicInteger mFrameCount = new AtomicInteger(0);
    private final AtomicInteger mAttemptToken = new AtomicInteger(0);
    private final AtomicBoolean mIsOpening = new AtomicBoolean(false);
    private final AtomicBoolean mIsHandlingFailure = new AtomicBoolean(false);

    private final Runnable mWatchdogRunnable = () -> {
        if (mFrameCount.get() < FRAMES_UNTIL_VIEW_IS_READY) {
            Logger.w(TAG, "Camera open/preview attempt timed out for token "
                    + mAttemptToken.get() + ". Retrying.");
            if (mIsOpening.compareAndSet(true, false)) {
                mCameraOpenCloseLock.release();
            }
            CameraDevice device;
            synchronized (mResourcesLock) {
                device = mCameraDevice;
            }
            handleCameraFailure(device);
        }
    };

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
                        Handler handler = mBackgroundHandler;
                        if (handler != null) {
                            handler.removeCallbacks(mWatchdogRunnable);
                        }
                        if (!previewStartedIdlingResource.isIdleNow()) {
                            previewStartedIdlingResource.decrement();
                        }
                        if (!completionIdlingResource.isIdleNow()) {
                            completionIdlingResource.decrement();
                        }
                    }
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityScreenUtil.turnScreenOnAndShowWhenLocked(this, true);
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

        mFrameCount.set(0);
        completionIdlingResource.increment();
        previewStartedIdlingResource.increment();

        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        if (mTextureView.isAvailable()) {
            openCamera();
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

            synchronized (mResourcesLock) {
                if (mCameraDevice != null) {
                    Logger.d(TAG, "Camera was opened while we waited for the lock. Skipping.");
                    mCameraOpenCloseLock.release();
                    return;
                }
            }

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            mFrameCount.set(0);
            mIsOpening.set(true);
            final int token = mAttemptToken.incrementAndGet();
            Handler handler = mBackgroundHandler;
            if (handler != null) {
                handler.removeCallbacks(mWatchdogRunnable);
                handler.postDelayed(mWatchdogRunnable, ATTEMPT_TIMEOUT_MS);
            }
            manager.openCamera(mCameraId, new DeviceStateCallbackImpl(token), handler);

        } catch (InterruptedException e) {
            // This is the new, correct handling for interruption.
            Logger.w(TAG, "Thread was interrupted while waiting for camera lock.", e);
            // Preserve the interrupted status for higher-level code.
            Thread.currentThread().interrupt();
        } catch (CameraAccessException | SecurityException | IllegalArgumentException e) {
            Logger.e(TAG, "Failed to open camera synchronously", e);
            mIsOpening.set(false);
            mCameraOpenCloseLock.release();
            handleCameraFailure(null);
        }
    }

    void closeCamera() {
        mAttemptToken.incrementAndGet();
        Handler handler = mBackgroundHandler;
        if (handler != null) {
            handler.removeCallbacks(mWatchdogRunnable);
        }
        boolean lockAcquired = false;
        try {
            lockAcquired = mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
            if (!lockAcquired) {
                Logger.w(TAG, "Timed out trying to acquire lock for closing camera.");
            }
            releaseCameraResources(null);
        } catch (InterruptedException e) {
            Logger.w(TAG, "Interrupted while trying to lock camera closing.", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lockAcquired || mIsOpening.compareAndSet(true, false)) {
                mCameraOpenCloseLock.release();
            }
        }
    }

    private void releaseCameraResources(@Nullable CameraDevice extraDeviceToClose) {
        CameraCaptureSession sessionToClose;
        CameraDevice deviceToClose;
        Surface surfaceToRelease;
        synchronized (mResourcesLock) {
            sessionToClose = mCaptureSession;
            mCaptureSession = null;
            deviceToClose = mCameraDevice;
            mCameraDevice = null;
            surfaceToRelease = mPreviewSurface;
            mPreviewSurface = null;
        }
        if (sessionToClose != null) {
            sessionToClose.close();
        }
        if (deviceToClose != null) {
            deviceToClose.close();
        }
        if (extraDeviceToClose != null && extraDeviceToClose != deviceToClose) {
            extraDeviceToClose.close();
        }
        if (surfaceToRelease != null) {
            surfaceToRelease.release();
        }
    }

    /* createCaptureSession */
    @SuppressWarnings("deprecation")
    void createCameraPreviewSession(int token) {
        mFrameCount.set(0);
        CameraDevice device;
        Surface previewSurface = null;
        Surface oldSurfaceToRelease = null;
        boolean failed = false;
        synchronized (mResourcesLock) {
            if (mAttemptToken.get() != token || mCameraDevice == null) {
                return;
            }
            device = mCameraDevice;
            try {
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                Preconditions.checkNotNull(texture);
                texture.setDefaultBufferSize(GUARANTEED_RESOLUTION.getWidth(),
                        GUARANTEED_RESOLUTION.getHeight());
                oldSurfaceToRelease = mPreviewSurface;
                mPreviewSurface = new Surface(texture);
                previewSurface = mPreviewSurface;
                mPreviewRequestBuilder = device.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(previewSurface);
            } catch (CameraAccessException | IllegalStateException e) {
                Logger.w(TAG, "Failed to prepare capture request. Camera was likely closed.", e);
                failed = true;
            }
        }
        if (oldSurfaceToRelease != null) {
            oldSurfaceToRelease.release();
        }
        if (failed) {
            handleCameraFailure(device);
            return;
        }
        try {
            device.createCaptureSession(Collections.singletonList(previewSurface),
                    new SessionStateCallbackImpl(token), mBackgroundHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            Logger.w(TAG, "Failed to create capture session. Camera was likely closed.", e);
            handleCameraFailure(device);
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
        mAttemptToken.incrementAndGet();
        Handler handler = mBackgroundHandler;
        if (handler != null) {
            handler.removeCallbacks(mWatchdogRunnable);
        }

        releaseCameraResources(cameraDevice);

        if (mFailedRetries < MAX_RETRIES) {
            mFailedRetries++;
            Logger.d(TAG, "Scheduling retry to open camera. Attempt " + (mFailedRetries + 1));
            if (handler != null) {
                handler.postDelayed(this::tryOpenCamera, RETRY_DELAY_MS);
            }
        } else {
            Logger.e(TAG, "Failed to open camera after " + MAX_RETRIES + " retries.");
            if (!completionIdlingResource.isIdleNow()) {
                completionIdlingResource.decrement();
            }
        }
    }

    final class DeviceStateCallbackImpl extends CameraDevice.StateCallback {
        private final int mToken;

        DeviceStateCallbackImpl(int token) {
            mToken = token;
        }

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            if (mIsOpening.compareAndSet(true, false)) {
                mCameraOpenCloseLock.release();
            }
            boolean isStale = false;
            synchronized (mResourcesLock) {
                if (mAttemptToken.get() != mToken) {
                    isStale = true;
                } else {
                    mCameraDevice = cameraDevice;
                }
            }
            if (isStale) {
                Logger.w(TAG, "Ignoring stale onOpened for token " + mToken);
                cameraDevice.close();
                return;
            }
            Logger.d(TAG, "Camera onOpened: " + cameraDevice.getId());
            mIsHandlingFailure.set(false);
            createCameraPreviewSession(mToken);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            if (mIsOpening.compareAndSet(true, false)) {
                mCameraOpenCloseLock.release();
            }
            if (mAttemptToken.get() != mToken) {
                cameraDevice.close();
                return;
            }
            Logger.w(TAG, "Camera onDisconnected: " + cameraDevice.getId());
            handleCameraFailure(cameraDevice);
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            if (mIsOpening.compareAndSet(true, false)) {
                mCameraOpenCloseLock.release();
            }
            if (mAttemptToken.get() != mToken) {
                cameraDevice.close();
                return;
            }
            Logger.e(TAG, "Camera onError: " + cameraDevice.getId() + ", error: " + error);
            handleCameraFailure(cameraDevice);
        }
    }

    final class SessionStateCallbackImpl extends CameraCaptureSession.StateCallback {
        private final int mToken;

        SessionStateCallbackImpl(int token) {
            mToken = token;
        }

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            CameraDevice device = null;
            boolean isStale = false;
            synchronized (mResourcesLock) {
                if (mAttemptToken.get() != mToken || mCameraDevice == null) {
                    isStale = true;
                } else {
                    device = mCameraDevice;
                    mCaptureSession = cameraCaptureSession;
                    try {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        Handler handler = mBackgroundHandler;
                        if (handler != null) {
                            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                                    new CameraCaptureSession.CaptureCallback() {}, handler);
                        }
                        Logger.d(TAG, "Camera session configured successfully.");
                        return;
                    } catch (CameraAccessException | IllegalStateException e) {
                        Logger.w(TAG, "Failed to start preview, treating as camera failure.", e);
                    }
                }
            }
            if (isStale) {
                cameraCaptureSession.close();
                return;
            }
            handleCameraFailure(device);
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            if (mAttemptToken.get() != mToken) {
                cameraCaptureSession.close();
                return;
            }
            Logger.e(TAG, "Camera session configuration failed, treating as camera failure.");
            CameraDevice device;
            synchronized (mResourcesLock) {
                device = mCameraDevice;
            }
            handleCameraFailure(device);
        }
    }
}
