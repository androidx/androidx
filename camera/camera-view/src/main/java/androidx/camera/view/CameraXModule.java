/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.view;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.UiThread;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraOrientationUtil;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.OnImageCapturedCallback;
import androidx.camera.core.ImageCapture.OnImageSavedCallback;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.LensFacing;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCapture.OnVideoSavedCallback;
import androidx.camera.core.VideoCaptureConfig;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.view.CameraView.CaptureMode;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** CameraX use case operation built on @{link androidx.camera.core}. */
final class CameraXModule {
    public static final String TAG = "CameraXModule";

    private static final int MAX_VIEW_DIMENSION = 2000;
    private static final float UNITY_ZOOM_SCALE = 1f;
    private static final float ZOOM_NOT_SUPPORTED = UNITY_ZOOM_SCALE;
    private static final Rational ASPECT_RATIO_16_9 = new Rational(16, 9);
    private static final Rational ASPECT_RATIO_4_3 = new Rational(4, 3);
    private static final Rational ASPECT_RATIO_9_16 = new Rational(9, 16);
    private static final Rational ASPECT_RATIO_3_4 = new Rational(3, 4);

    private final PreviewConfig.Builder mPreviewConfigBuilder;
    private final VideoCaptureConfig.Builder mVideoCaptureConfigBuilder;
    private final ImageCaptureConfig.Builder mImageCaptureConfigBuilder;
    private final CameraView mCameraView;
    final AtomicBoolean mVideoIsRecording = new AtomicBoolean(false);
    private CameraView.CaptureMode mCaptureMode = CaptureMode.IMAGE;
    private long mMaxVideoDuration = CameraView.INDEFINITE_VIDEO_DURATION;
    private long mMaxVideoSize = CameraView.INDEFINITE_VIDEO_SIZE;
    private FlashMode mFlash = FlashMode.OFF;
    @Nullable
    private ImageCapture mImageCapture;
    @Nullable
    private VideoCapture mVideoCapture;
    @Nullable
    Preview mPreview;
    @Nullable
    LifecycleOwner mCurrentLifecycle;
    private final LifecycleObserver mCurrentLifecycleObserver =
            new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                public void onDestroy(LifecycleOwner owner) {
                    if (owner == mCurrentLifecycle) {
                        clearCurrentLifecycle();
                        mPreview.setPreviewSurfaceCallback(null);
                    }
                }
            };
    @Nullable
    private LifecycleOwner mNewLifecycle;
    @Nullable
    private LensFacing mCameraLensFacing = LensFacing.BACK;

    CameraXModule(CameraView view) {
        this.mCameraView = view;

        mPreviewConfigBuilder = new PreviewConfig.Builder().setTargetName("Preview");

        mImageCaptureConfigBuilder =
                new ImageCaptureConfig.Builder().setTargetName("ImageCapture");

        mVideoCaptureConfigBuilder =
                new VideoCaptureConfig.Builder().setTargetName("VideoCapture");
    }

    @RequiresPermission(permission.CAMERA)
    public void bindToLifecycle(LifecycleOwner lifecycleOwner) {
        mNewLifecycle = lifecycleOwner;

        if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
            bindToLifecycleAfterViewMeasured();
        }
    }

    @RequiresPermission(permission.CAMERA)
    void bindToLifecycleAfterViewMeasured() {
        if (mNewLifecycle == null) {
            return;
        }

        clearCurrentLifecycle();
        mCurrentLifecycle = mNewLifecycle;
        mNewLifecycle = null;
        if (mCurrentLifecycle.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            mCurrentLifecycle = null;
            throw new IllegalArgumentException("Cannot bind to lifecycle in a destroyed state.");
        }

        final int cameraOrientation;
        try {
            Set<LensFacing> available = getAvailableCameraLensFacing();

            if (available.isEmpty()) {
                Log.w(TAG, "Unable to bindToLifeCycle since no cameras available");
                mCameraLensFacing = null;
            }

            // Ensure the current camera exists, or default to another camera
            if (mCameraLensFacing != null && !available.contains(mCameraLensFacing)) {
                Log.w(TAG, "Camera does not exist with direction " + mCameraLensFacing);

                // Default to the first available camera direction
                mCameraLensFacing = available.iterator().next();

                Log.w(TAG, "Defaulting to primary camera with direction " + mCameraLensFacing);
            }

            // Do not attempt to create use cases for a null cameraLensFacing. This could occur if
            // the
            // user explicitly sets the LensFacing to null, or if we determined there
            // were no available cameras, which should be logged in the logic above.
            if (mCameraLensFacing == null) {
                return;
            }
            CameraInfo cameraInfo = CameraX.getCameraInfo(getLensFacing());
            cameraOrientation = cameraInfo.getSensorRotationDegrees();
        } catch (CameraInfoUnavailableException e) {
            throw new IllegalStateException("Unable to get Camera Info.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to bind to lifecycle.", e);
        }

        // Set the preferred aspect ratio as 4:3 if it is IMAGE only mode. Set the preferred aspect
        // ratio as 16:9 if it is VIDEO or MIXED mode. Then, it will be WYSIWYG when the view finder
        // is in CENTER_INSIDE mode.

        boolean isDisplayPortrait = getDisplayRotationDegrees() == 0
                || getDisplayRotationDegrees() == 180;

        Rational targetAspectRatio;
        if (getCaptureMode() == CaptureMode.IMAGE) {
            mImageCaptureConfigBuilder.setTargetAspectRatio(AspectRatio.RATIO_4_3);
            targetAspectRatio = isDisplayPortrait ? ASPECT_RATIO_3_4 : ASPECT_RATIO_4_3;
        } else {
            mImageCaptureConfigBuilder.setTargetAspectRatio(AspectRatio.RATIO_16_9);
            targetAspectRatio = isDisplayPortrait ? ASPECT_RATIO_9_16 : ASPECT_RATIO_16_9;
        }

        mImageCaptureConfigBuilder.setTargetRotation(getDisplaySurfaceRotation());
        mImageCaptureConfigBuilder.setLensFacing(mCameraLensFacing);
        mImageCapture = new ImageCapture(mImageCaptureConfigBuilder.build());

        mVideoCaptureConfigBuilder.setTargetRotation(getDisplaySurfaceRotation());
        mVideoCaptureConfigBuilder.setLensFacing(mCameraLensFacing);
        mVideoCapture = new VideoCapture(mVideoCaptureConfigBuilder.build());
        mPreviewConfigBuilder.setLensFacing(mCameraLensFacing);

        // Adjusts the preview resolution according to the view size and the target aspect ratio.
        int height = (int) (getMeasuredWidth() / targetAspectRatio.floatValue());
        mPreviewConfigBuilder.setTargetResolution(new Size(getMeasuredWidth(), height));

        mPreview = new Preview(mPreviewConfigBuilder.build());
        mPreview.setPreviewSurfaceCallback(new Preview.PreviewSurfaceCallback() {
            // Thread safe because it only accessed on the default executor, which is UI thread.
            Map<Surface, SurfaceTexture> mSurfaceTextureMap = new HashMap<>();

            @NonNull
            @Override
            public ListenableFuture<Surface> createSurfaceFuture(@NonNull Size resolution,
                    int imageFormat) {
                boolean needReverse = cameraOrientation != 0 && cameraOrientation != 180;
                int textureWidth =
                        needReverse
                                ? resolution.getHeight()
                                : resolution.getWidth();
                int textureHeight =
                        needReverse
                                ? resolution.getWidth()
                                : resolution.getHeight();
                CameraXModule.this.onPreviewSourceDimensUpdated(textureWidth,
                        textureHeight);
                // Create SurfaceTexture and Surface.
                SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                surfaceTexture.setDefaultBufferSize(resolution.getWidth(),
                        resolution.getHeight());
                surfaceTexture.detachFromGLContext();
                CameraXModule.this.setSurfaceTexture(surfaceTexture);
                Surface surface = new Surface(surfaceTexture);
                mSurfaceTextureMap.put(surface, surfaceTexture);
                return Futures.immediateFuture(surface);
            }

            @Override
            public void onSafeToRelease(@NonNull ListenableFuture<Surface> surfaceFuture) {
                try {
                    Surface surface = surfaceFuture.get();
                    surface.release();
                    SurfaceTexture surfaceTexture = mSurfaceTextureMap.get(surface);
                    if (surfaceTexture != null) {
                        surfaceTexture.release();
                        mSurfaceTextureMap.remove(surface);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Failed to release Surface", e);
                }
            }
        });

        if (getCaptureMode() == CaptureMode.IMAGE) {
            CameraX.bindToLifecycle(mCurrentLifecycle, mImageCapture, mPreview);
        } else if (getCaptureMode() == CaptureMode.VIDEO) {
            CameraX.bindToLifecycle(mCurrentLifecycle, mVideoCapture, mPreview);
        } else {
            CameraX.bindToLifecycle(mCurrentLifecycle, mImageCapture, mVideoCapture, mPreview);
        }
        setZoomRatio(UNITY_ZOOM_SCALE);
        mCurrentLifecycle.getLifecycle().addObserver(mCurrentLifecycleObserver);
        // Enable flash setting in ImageCapture after use cases are created and binded.
        setFlash(getFlash());
    }

    public void open() {
        throw new UnsupportedOperationException(
                "Explicit open/close of camera not yet supported. Use bindtoLifecycle() instead.");
    }

    public void close() {
        throw new UnsupportedOperationException(
                "Explicit open/close of camera not yet supported. Use bindtoLifecycle() instead.");
    }

    public void takePicture(Executor executor, OnImageCapturedCallback callback) {
        if (mImageCapture == null) {
            return;
        }

        if (getCaptureMode() == CaptureMode.VIDEO) {
            throw new IllegalStateException("Can not take picture under VIDEO capture mode.");
        }

        if (callback == null) {
            throw new IllegalArgumentException("OnImageCapturedCallback should not be empty");
        }

        mImageCapture.takePicture(executor, callback);
    }

    public void takePicture(File saveLocation, Executor executor, OnImageSavedCallback callback) {
        if (mImageCapture == null) {
            return;
        }

        if (getCaptureMode() == CaptureMode.VIDEO) {
            throw new IllegalStateException("Can not take picture under VIDEO capture mode.");
        }

        if (callback == null) {
            throw new IllegalArgumentException("OnImageSavedCallback should not be empty");
        }

        ImageCapture.Metadata metadata = new ImageCapture.Metadata();
        metadata.isReversedHorizontal = mCameraLensFacing == LensFacing.FRONT;
        mImageCapture.takePicture(saveLocation, metadata, executor, callback);
    }

    public void startRecording(File file, Executor executor, final OnVideoSavedCallback callback) {
        if (mVideoCapture == null) {
            return;
        }

        if (getCaptureMode() == CaptureMode.IMAGE) {
            throw new IllegalStateException("Can not record video under IMAGE capture mode.");
        }

        if (callback == null) {
            throw new IllegalArgumentException("OnVideoSavedCallback should not be empty");
        }

        mVideoIsRecording.set(true);
        mVideoCapture.startRecording(
                file,
                executor,
                new VideoCapture.OnVideoSavedCallback() {
                    @Override
                    public void onVideoSaved(@NonNull File savedFile) {
                        mVideoIsRecording.set(false);
                        callback.onVideoSaved(savedFile);
                    }

                    @Override
                    public void onError(
                            @NonNull VideoCapture.VideoCaptureError videoCaptureError,
                            @NonNull String message,
                            @Nullable Throwable cause) {
                        mVideoIsRecording.set(false);
                        Log.e(TAG, message, cause);
                        callback.onError(videoCaptureError, message, cause);
                    }
                });
    }

    public void stopRecording() {
        if (mVideoCapture == null) {
            return;
        }

        mVideoCapture.stopRecording();
    }

    public boolean isRecording() {
        return mVideoIsRecording.get();
    }

    // TODO(b/124269166): Rethink how we can handle permissions here.
    @SuppressLint("MissingPermission")
    public void setCameraLensFacing(@Nullable LensFacing lensFacing) {
        // Setting same lens facing is a no-op, so check for that first
        if (mCameraLensFacing != lensFacing) {
            // If we're not bound to a lifecycle, just update the camera that will be opened when we
            // attach to a lifecycle.
            mCameraLensFacing = lensFacing;

            if (mCurrentLifecycle != null) {
                // Re-bind to lifecycle with new camera
                bindToLifecycle(mCurrentLifecycle);
            }
        }
    }

    @RequiresPermission(permission.CAMERA)
    public boolean hasCameraWithLensFacing(LensFacing lensFacing) {
        String cameraId;
        try {
            cameraId = CameraX.getCameraWithLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to query lens facing.", e);
        }

        return cameraId != null;
    }

    @Nullable
    public LensFacing getLensFacing() {
        return mCameraLensFacing;
    }

    public void toggleCamera() {
        // TODO(b/124269166): Rethink how we can handle permissions here.
        @SuppressLint("MissingPermission")
        Set<LensFacing> availableCameraLensFacing = getAvailableCameraLensFacing();

        if (availableCameraLensFacing.isEmpty()) {
            return;
        }

        if (mCameraLensFacing == null) {
            setCameraLensFacing(availableCameraLensFacing.iterator().next());
            return;
        }

        if (mCameraLensFacing == LensFacing.BACK
                && availableCameraLensFacing.contains(LensFacing.FRONT)) {
            setCameraLensFacing(LensFacing.FRONT);
            return;
        }

        if (mCameraLensFacing == LensFacing.FRONT
                && availableCameraLensFacing.contains(LensFacing.BACK)) {
            setCameraLensFacing(LensFacing.BACK);
            return;
        }
    }

    public float getZoomRatio() {
        try {
            return CameraX.getCameraInfo(mCameraLensFacing).getZoomRatio().getValue();
        } catch (CameraInfoUnavailableException e) {
            return UNITY_ZOOM_SCALE;
        }
    }

    public void setZoomRatio(float zoomRatio) {
        try {
            CameraX.getCameraControl(mCameraLensFacing).setZoomRatio(zoomRatio);
        } catch (CameraInfoUnavailableException e) {
            Log.e(TAG, "Failed to set zoom ratio", e);
        }
    }

    public float getMinZoomRatio() {
        try {
            return CameraX.getCameraInfo(mCameraLensFacing).getMinZoomRatio().getValue();
        } catch (CameraInfoUnavailableException e) {
            return UNITY_ZOOM_SCALE;
        }
    }

    public float getMaxZoomRatio() {
        try {
            return CameraX.getCameraInfo(mCameraLensFacing).getMaxZoomRatio().getValue();
        } catch (CameraInfoUnavailableException e) {
            return ZOOM_NOT_SUPPORTED;
        }
    }

    public boolean isZoomSupported() {
        return getMaxZoomRatio() != ZOOM_NOT_SUPPORTED;
    }

    // TODO(b/124269166): Rethink how we can handle permissions here.
    @SuppressLint("MissingPermission")
    private void rebindToLifecycle() {
        if (mCurrentLifecycle != null) {
            bindToLifecycle(mCurrentLifecycle);
        }
    }

    int getRelativeCameraOrientation(boolean compensateForMirroring) {
        int rotationDegrees = 0;
        try {
            CameraInfo cameraInfo = CameraX.getCameraInfo(getLensFacing());
            rotationDegrees = cameraInfo.getSensorRotationDegrees(getDisplaySurfaceRotation());
            if (compensateForMirroring) {
                rotationDegrees = (360 - rotationDegrees) % 360;
            }
        } catch (CameraInfoUnavailableException e) {
            Log.e(TAG, "Failed to get CameraInfo", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to query camera", e);
        }

        return rotationDegrees;
    }

    public void invalidateView() {
        transformPreview();
        updateViewInfo();
    }

    void clearCurrentLifecycle() {
        if (mCurrentLifecycle != null) {
            // Remove previous use cases
            CameraX.unbind(mImageCapture, mVideoCapture, mPreview);
        }

        mCurrentLifecycle = null;
    }

    @UiThread
    private void transformPreview() {
        int previewWidth = getPreviewWidth();
        int previewHeight = getPreviewHeight();
        int displayOrientation = getDisplayRotationDegrees();

        Matrix matrix = new Matrix();

        // Apply rotation of the display
        int rotation = -displayOrientation;

        int px = (int) Math.round(previewWidth / 2d);
        int py = (int) Math.round(previewHeight / 2d);

        matrix.postRotate(rotation, px, py);

        if (displayOrientation == 90 || displayOrientation == 270) {
            // Swap width and height
            float xScale = previewWidth / (float) previewHeight;
            float yScale = previewHeight / (float) previewWidth;

            matrix.postScale(xScale, yScale, px, py);
        }

        setTransform(matrix);
    }

    // Update view related information used in use cases
    private void updateViewInfo() {
        if (mImageCapture != null) {
            mImageCapture.setTargetAspectRatioCustom(new Rational(getWidth(), getHeight()));
            mImageCapture.setTargetRotation(getDisplaySurfaceRotation());
        }

        if (mVideoCapture != null) {
            mVideoCapture.setTargetRotation(getDisplaySurfaceRotation());
        }
    }

    @RequiresPermission(permission.CAMERA)
    private Set<LensFacing> getAvailableCameraLensFacing() {
        // Start with all camera directions
        Set<LensFacing> available = new LinkedHashSet<>(Arrays.asList(LensFacing.values()));

        // If we're bound to a lifecycle, remove unavailable cameras
        if (mCurrentLifecycle != null) {
            if (!hasCameraWithLensFacing(LensFacing.BACK)) {
                available.remove(LensFacing.BACK);
            }

            if (!hasCameraWithLensFacing(LensFacing.FRONT)) {
                available.remove(LensFacing.FRONT);
            }
        }

        return available;
    }

    @NonNull
    public FlashMode getFlash() {
        return mFlash;
    }

    public void setFlash(@NonNull FlashMode flash) {
        this.mFlash = flash;

        if (mImageCapture == null) {
            // Do nothing if there is no imageCapture
            return;
        }

        mImageCapture.setFlashMode(flash);
    }

    public void enableTorch(boolean torch) {
        if (mPreview == null) {
            return;
        }
        mPreview.enableTorch(torch);
    }

    public boolean isTorchOn() {
        if (mPreview == null) {
            return false;
        }
        return mPreview.isTorchOn();
    }

    public Context getContext() {
        return mCameraView.getContext();
    }

    public int getWidth() {
        return mCameraView.getWidth();
    }

    public int getHeight() {
        return mCameraView.getHeight();
    }

    public int getDisplayRotationDegrees() {
        return CameraOrientationUtil.surfaceRotationToDegrees(getDisplaySurfaceRotation());
    }

    protected int getDisplaySurfaceRotation() {
        return mCameraView.getDisplaySurfaceRotation();
    }

    public void setSurfaceTexture(SurfaceTexture st) {
        mCameraView.setSurfaceTexture(st);
    }

    private int getPreviewWidth() {
        return mCameraView.getPreviewWidth();
    }

    private int getPreviewHeight() {
        return mCameraView.getPreviewHeight();
    }

    private int getMeasuredWidth() {
        return mCameraView.getMeasuredWidth();
    }

    private int getMeasuredHeight() {
        return mCameraView.getMeasuredHeight();
    }

    void setTransform(final Matrix matrix) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mCameraView.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            setTransform(matrix);
                        }
                    });
        } else {
            mCameraView.setTransform(matrix);
        }
    }

    /**
     * Notify the view that the source dimensions have changed.
     *
     * <p>This will allow the view to layout the preview to display the correct aspect ratio.
     *
     * @param width  width of camera source buffers.
     * @param height height of camera source buffers.
     */
    void onPreviewSourceDimensUpdated(int width, int height) {
        mCameraView.onPreviewSourceDimensUpdated(width, height);
    }

    @NonNull
    public CameraView.CaptureMode getCaptureMode() {
        return mCaptureMode;
    }

    public void setCaptureMode(@NonNull CameraView.CaptureMode captureMode) {
        this.mCaptureMode = captureMode;
        rebindToLifecycle();
    }

    public long getMaxVideoDuration() {
        return mMaxVideoDuration;
    }

    public void setMaxVideoDuration(long duration) {
        mMaxVideoDuration = duration;
    }

    public long getMaxVideoSize() {
        return mMaxVideoSize;
    }

    public void setMaxVideoSize(long size) {
        mMaxVideoSize = size;
    }

    public boolean isPaused() {
        return false;
    }
}
