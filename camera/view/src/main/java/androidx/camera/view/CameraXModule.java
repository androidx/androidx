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
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.UiThread;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraOrientationUtil;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.ImageCaptureUseCase.OnImageCapturedListener;
import androidx.camera.core.ImageCaptureUseCase.OnImageSavedListener;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.VideoCaptureUseCase;
import androidx.camera.core.VideoCaptureUseCase.OnVideoSavedListener;
import androidx.camera.core.VideoCaptureUseCaseConfiguration;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCaseConfiguration;
import androidx.camera.view.CameraView.CaptureMode;
import androidx.camera.view.CameraView.Quality;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** CameraX use case operation built on @{link androidx.camera.core}. */
final class CameraXModule {
    public static final String TAG = "CameraXModule";

    private static final int MAX_VIEW_DIMENSION = 2000;
    private static final float UNITY_ZOOM_SCALE = 1f;
    private static final float ZOOM_NOT_SUPPORTED = UNITY_ZOOM_SCALE;
    private static final Rational ASPECT_RATIO_16_9 = new Rational(16, 9);
    private static final Rational ASPECT_RATIO_4_3 = new Rational(4, 3);

    private final CameraManager cameraManager;
    private final ViewFinderUseCaseConfiguration.Builder viewFinderConfigBuilder;
    private final VideoCaptureUseCaseConfiguration.Builder videoCaptureConfigBuilder;
    private final ImageCaptureUseCaseConfiguration.Builder imageCaptureConfigBuilder;
    private final CameraView cameraView;
    final AtomicBoolean videoIsRecording = new AtomicBoolean(false);
    private CameraView.Quality quality = CameraView.Quality.HIGH;
    private CameraView.CaptureMode captureMode = CaptureMode.IMAGE;
    private long maxVideoDuration = CameraView.INDEFINITE_VIDEO_DURATION;
    private long maxVideoSize = CameraView.INDEFINITE_VIDEO_SIZE;
    private FlashMode flash = FlashMode.OFF;
    @Nullable
    private ImageCaptureUseCase imageCaptureUseCase;
    @Nullable
    private VideoCaptureUseCase videoCaptureUseCase;
    @Nullable
    ViewFinderUseCase viewFinderUseCase;
    @Nullable
    LifecycleOwner currentLifecycle;
    private final LifecycleObserver currentLifecycleObserver =
            new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(LifecycleOwner owner) {
                    if (owner == currentLifecycle) {
                        clearCurrentLifecycle();
                        viewFinderUseCase.removeViewFinderOutputListener();
                    }
                }
            };
    @Nullable
    private LifecycleOwner newLifecycle;
    private float zoomLevel = UNITY_ZOOM_SCALE;
    @Nullable
    private Rect cropRegion;
    @Nullable
    private CameraX.LensFacing cameraLensFacing = LensFacing.BACK;

    public CameraXModule(CameraView view) {
        this.cameraView = view;

        cameraManager = (CameraManager) view.getContext().getSystemService(Context.CAMERA_SERVICE);

        viewFinderConfigBuilder =
                new ViewFinderUseCaseConfiguration.Builder().setTargetName("ViewFinder");

        imageCaptureConfigBuilder =
                new ImageCaptureUseCaseConfiguration.Builder().setTargetName("ImageCapture");

        videoCaptureConfigBuilder =
                new VideoCaptureUseCaseConfiguration.Builder().setTargetName("VideoCapture");
    }

    /**
     * Rescales view rectangle with dimensions in [-1000, 1000] to a corresponding rectangle in the
     * sensor coordinate frame.
     */
    private static Rect rescaleViewRectToSensorRect(Rect view, Rect sensor) {
        // Scale width and height.
        int newWidth = Math.round(view.width() * sensor.width() / (float) MAX_VIEW_DIMENSION);
        int newHeight = Math.round(view.height() * sensor.height() / (float) MAX_VIEW_DIMENSION);

        // Scale top/left corner.
        int halfViewDimension = MAX_VIEW_DIMENSION / 2;
        int leftOffset =
                Math.round(
                        (view.left + halfViewDimension)
                                * sensor.width()
                                / (float) MAX_VIEW_DIMENSION)
                        + sensor.left;
        int topOffset =
                Math.round(
                        (view.top + halfViewDimension)
                                * sensor.height()
                                / (float) MAX_VIEW_DIMENSION)
                        + sensor.top;

        // Now, produce the scaled rect.
        Rect scaled = new Rect();
        scaled.left = leftOffset;
        scaled.top = topOffset;
        scaled.right = scaled.left + newWidth;
        scaled.bottom = scaled.top + newHeight;
        return scaled;
    }

    @RequiresPermission(permission.CAMERA)
    public void bindToLifecycle(LifecycleOwner lifecycleOwner) {
        newLifecycle = lifecycleOwner;

        if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
            bindToLifecycleAfterViewMeasured();
        }
    }

    @RequiresPermission(permission.CAMERA)
    void bindToLifecycleAfterViewMeasured() {
        if (newLifecycle == null) {
            return;
        }

        clearCurrentLifecycle();
        currentLifecycle = newLifecycle;
        newLifecycle = null;
        if (currentLifecycle.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            currentLifecycle = null;
            throw new IllegalArgumentException("Cannot bind to lifecycle in a destroyed state.");
        }

        int cameraOrientation;
        try {
            String cameraId;
            Set<LensFacing> available = getAvailableCameraLensFacing();

            if (available.isEmpty()) {
                Log.w(TAG, "Unable to bindToLifeCycle since no cameras available");
                cameraLensFacing = null;
            }

            // Ensure the current camera exists, or default to another camera
            if (cameraLensFacing != null && !available.contains(cameraLensFacing)) {
                Log.w(TAG, "Camera does not exist with direction " + cameraLensFacing);

                // Default to the first available camera direction
                cameraLensFacing = available.iterator().next();

                Log.w(TAG, "Defaulting to primary camera with direction " + cameraLensFacing);
            }

            // Do not attempt to create use cases for a null cameraLensFacing. This could occur if
            // the
            // user explicitly sets the LensFacing to null, or if we determined there
            // were no available cameras, which should be logged in the logic above.
            if (cameraLensFacing == null) {
                return;
            }

            cameraId = CameraX.getCameraWithLensFacing(cameraLensFacing);
            if (cameraId == null) {
                return;
            }
            CameraInfo cameraInfo = CameraX.getCameraInfo(cameraId);
            cameraOrientation = cameraInfo.getSensorRotationDegrees();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to bind to lifecycle.", e);
        }

        // Set the preferred aspect ratio as 4:3 if it is IMAGE only mode. Set the preferred aspect
        // ratio as 16:9 if it is VIDEO or MIXED mode. Then, it will be WYSIWYG when the view finder
        // is
        // in CENTER_INSIDE mode.
        if (getCaptureMode() == CaptureMode.IMAGE) {
            imageCaptureConfigBuilder.setTargetAspectRatio(ASPECT_RATIO_4_3);
            viewFinderConfigBuilder.setTargetAspectRatio(ASPECT_RATIO_4_3);
        } else {
            imageCaptureConfigBuilder.setTargetAspectRatio(ASPECT_RATIO_16_9);
            viewFinderConfigBuilder.setTargetAspectRatio(ASPECT_RATIO_16_9);
        }

        imageCaptureConfigBuilder.setTargetRotation(getDisplaySurfaceRotation());
        imageCaptureConfigBuilder.setLensFacing(cameraLensFacing);
        imageCaptureUseCase = new ImageCaptureUseCase(imageCaptureConfigBuilder.build());

        videoCaptureConfigBuilder.setTargetRotation(getDisplaySurfaceRotation());
        videoCaptureConfigBuilder.setLensFacing(cameraLensFacing);
        videoCaptureUseCase = new VideoCaptureUseCase(videoCaptureConfigBuilder.build());
        viewFinderConfigBuilder.setLensFacing(cameraLensFacing);

        int relativeCameraOrientation = getRelativeCameraOrientation(false);

        if (relativeCameraOrientation == 90 || relativeCameraOrientation == 270) {
            viewFinderConfigBuilder.setTargetResolution(
                    new Size(getMeasuredHeight(), getMeasuredWidth()));
        } else {
            viewFinderConfigBuilder.setTargetResolution(
                    new Size(getMeasuredWidth(), getMeasuredHeight()));
        }

        viewFinderUseCase = new ViewFinderUseCase(viewFinderConfigBuilder.build());
        viewFinderUseCase.setOnViewFinderOutputUpdateListener(
                output -> {
                    boolean needReverse = cameraOrientation != 0 && cameraOrientation != 180;
                    int textureWidth =
                            needReverse
                                    ? output.getTextureSize().getHeight()
                                    : output.getTextureSize().getWidth();
                    int textureHeight =
                            needReverse
                                    ? output.getTextureSize().getWidth()
                                    : output.getTextureSize().getHeight();
                    onViewfinderSourceDimensUpdated(textureWidth, textureHeight);
                    setSurfaceTexture(output.getSurfaceTexture());
                });

        if (getCaptureMode() == CaptureMode.IMAGE) {
            CameraX.bindToLifecycle(currentLifecycle, imageCaptureUseCase, viewFinderUseCase);
        } else if (getCaptureMode() == CaptureMode.VIDEO) {
            CameraX.bindToLifecycle(currentLifecycle, videoCaptureUseCase, viewFinderUseCase);
        } else {
            CameraX.bindToLifecycle(
                    currentLifecycle, imageCaptureUseCase, videoCaptureUseCase, viewFinderUseCase);
        }
        setZoomLevel(zoomLevel);
        currentLifecycle.getLifecycle().addObserver(currentLifecycleObserver);
        // Enable flash setting in ImageCaptureUseCase after use cases are created and binded.
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

    public void takePicture(OnImageCapturedListener listener) {
        if (imageCaptureUseCase == null) {
            return;
        }

        if (getCaptureMode() == CaptureMode.VIDEO) {
            throw new IllegalStateException("Can not take picture under VIDEO capture mode.");
        }

        if (listener == null) {
            throw new IllegalArgumentException("OnImageCapturedListener should not be empty");
        }

        imageCaptureUseCase.takePicture(listener);
    }

    public void takePicture(File saveLocation, OnImageSavedListener listener) {
        if (imageCaptureUseCase == null) {
            return;
        }

        if (getCaptureMode() == CaptureMode.VIDEO) {
            throw new IllegalStateException("Can not take picture under VIDEO capture mode.");
        }

        if (listener == null) {
            throw new IllegalArgumentException("OnImageSavedListener should not be empty");
        }

        ImageCaptureUseCase.Metadata metadata = new ImageCaptureUseCase.Metadata();
        metadata.isReversedHorizontal = cameraLensFacing == LensFacing.FRONT;
        imageCaptureUseCase.takePicture(saveLocation, listener, metadata);
    }

    public void startRecording(File file, OnVideoSavedListener listener) {
        if (videoCaptureUseCase == null) {
            return;
        }

        if (getCaptureMode() == CaptureMode.IMAGE) {
            throw new IllegalStateException("Can not record video under IMAGE capture mode.");
        }

        if (listener == null) {
            throw new IllegalArgumentException("OnVideoSavedListener should not be empty");
        }

        videoIsRecording.set(true);
        videoCaptureUseCase.startRecording(
                file,
                new VideoCaptureUseCase.OnVideoSavedListener() {
                    @Override
                    public void onVideoSaved(File savedFile) {
                        videoIsRecording.set(false);
                        listener.onVideoSaved(savedFile);
                    }

                    @Override
                    public void onError(
                            VideoCaptureUseCase.UseCaseError useCaseError,
                            String message,
                            @Nullable Throwable cause) {
                        videoIsRecording.set(false);
                        Log.e(TAG, message, cause);
                        listener.onError(useCaseError, message, cause);
                    }
                });
    }

    public void stopRecording() {
        if (videoCaptureUseCase == null) {
            return;
        }

        videoCaptureUseCase.stopRecording();
    }

    public boolean isRecording() {
        return videoIsRecording.get();
    }

    // TODO(b/124269166): Rethink how we can handle permissions here.
    @SuppressLint("MissingPermission")
    public void setCameraByLensFacing(@Nullable LensFacing lensFacing) {
        // Setting same lens facing is a no-op, so check for that first
        if (cameraLensFacing != lensFacing) {
            // If we're not bound to a lifecycle, just update the camera that will be opened when we
            // attach to a lifecycle.
            cameraLensFacing = lensFacing;

            if (currentLifecycle != null) {
                // Re-bind to lifecycle with new camera
                bindToLifecycle(currentLifecycle);
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
        return cameraLensFacing;
    }

    public void toggleCamera() {
        // TODO(b/124269166): Rethink how we can handle permissions here.
        @SuppressLint("MissingPermission")
        Set<LensFacing> availableCameraLensFacing = getAvailableCameraLensFacing();

        if (availableCameraLensFacing.isEmpty()) {
            return;
        }

        if (cameraLensFacing == null) {
            setCameraByLensFacing(availableCameraLensFacing.iterator().next());
            return;
        }

        if (cameraLensFacing == LensFacing.BACK
                && availableCameraLensFacing.contains(LensFacing.FRONT)) {
            setCameraByLensFacing(LensFacing.FRONT);
            return;
        }

        if (cameraLensFacing == LensFacing.FRONT
                && availableCameraLensFacing.contains(LensFacing.BACK)) {
            setCameraByLensFacing(LensFacing.BACK);
            return;
        }
    }

    public void focus(Rect focus, Rect metering) {
        if (viewFinderUseCase == null) {
            // Nothing to focus on since we don't yet have a viewfinder
            return;
        }

        Rect rescaledFocus;
        Rect rescaledMetering;
        try {
            Rect sensorRegion;
            if (cropRegion != null) {
                sensorRegion = cropRegion;
            } else {
                sensorRegion = getSensorSize(getActiveCamera());
            }
            rescaledFocus = rescaleViewRectToSensorRect(focus, sensorRegion);
            rescaledMetering = rescaleViewRectToSensorRect(metering, sensorRegion);
        } catch (Exception e) {
            Log.e(TAG, "Failed to rescale the focus and metering rectangles.", e);
            return;
        }

        viewFinderUseCase.focus(rescaledFocus, rescaledMetering);
    }

    public float getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(float zoomLevel) {
        // Set the zoom level in case it is set before binding to a lifecycle
        this.zoomLevel = zoomLevel;

        if (viewFinderUseCase == null) {
            // Nothing to zoom on yet since we don't have a viewfinder. Defer calculating crop
            // region.
            return;
        }

        Rect sensorSize;
        try {
            sensorSize = getSensorSize(getActiveCamera());
            if (sensorSize == null) {
                Log.e(TAG, "Failed to get the sensor size.");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get the sensor size.", e);
            return;
        }

        float minZoom = getMinZoomLevel();
        float maxZoom = getMaxZoomLevel();

        if (this.zoomLevel < minZoom) {
            Log.e(TAG, "Requested zoom level is less than minimum zoom level.");
        }
        if (this.zoomLevel > maxZoom) {
            Log.e(TAG, "Requested zoom level is greater than maximum zoom level.");
        }
        this.zoomLevel = Math.max(minZoom, Math.min(maxZoom, this.zoomLevel));

        float zoomScaleFactor =
                (maxZoom == minZoom) ? minZoom : (this.zoomLevel - minZoom) / (maxZoom - minZoom);
        int minWidth = Math.round(sensorSize.width() / maxZoom);
        int minHeight = Math.round(sensorSize.height() / maxZoom);
        int diffWidth = sensorSize.width() - minWidth;
        int diffHeight = sensorSize.height() - minHeight;
        float cropWidth = diffWidth * zoomScaleFactor;
        float cropHeight = diffHeight * zoomScaleFactor;

        Rect cropRegion =
                new Rect(
                        /*left=*/ (int) Math.ceil(cropWidth / 2 - 0.5f),
                        /*top=*/ (int) Math.ceil(cropHeight / 2 - 0.5f),
                        /*right=*/ (int) Math.floor(sensorSize.width() - cropWidth / 2 + 0.5f),
                        /*bottom=*/ (int) Math.floor(sensorSize.height() - cropHeight / 2 + 0.5f));

        if (cropRegion.width() < 50 || cropRegion.height() < 50) {
            Log.e(TAG, "Crop region is too small to compute 3A stats, so ignoring further zoom.");
            return;
        }
        this.cropRegion = cropRegion;

        viewFinderUseCase.zoom(cropRegion);
    }

    public float getMinZoomLevel() {
        return UNITY_ZOOM_SCALE;
    }

    public float getMaxZoomLevel() {
        try {
            CameraCharacteristics characteristics =
                    cameraManager.getCameraCharacteristics(getActiveCamera());
            Float maxZoom =
                    characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            if (maxZoom == null) {
                return ZOOM_NOT_SUPPORTED;
            }
            if (maxZoom == ZOOM_NOT_SUPPORTED) {
                return ZOOM_NOT_SUPPORTED;
            }
            return maxZoom;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get SCALER_AVAILABLE_MAX_DIGITAL_ZOOM.", e);
        }
        return ZOOM_NOT_SUPPORTED;
    }

    public boolean isZoomSupported() {
        return getMaxZoomLevel() != ZOOM_NOT_SUPPORTED;
    }

    // TODO(b/124269166): Rethink how we can handle permissions here.
    @SuppressLint("MissingPermission")
    private void rebindToLifecycle() {
        if (currentLifecycle != null) {
            bindToLifecycle(currentLifecycle);
        }
    }

    int getRelativeCameraOrientation(boolean compensateForMirroring) {
        int rotationDegrees;
        try {
            String cameraId = CameraX.getCameraWithLensFacing(getLensFacing());
            CameraInfo cameraInfo = CameraX.getCameraInfo(cameraId);
            rotationDegrees = cameraInfo.getSensorRotationDegrees(getDisplaySurfaceRotation());
            if (compensateForMirroring) {
                rotationDegrees = (360 - rotationDegrees) % 360;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to query camera", e);
            rotationDegrees = 0;
        }

        return rotationDegrees;
    }

    public CameraView.Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        if (quality != Quality.HIGH) {
            throw new UnsupportedOperationException("Only supported Quality is HIGH");
        }
        this.quality = quality;
    }

    public void invalidateView() {
        transformPreview();
        updateViewInfo();
    }

    void clearCurrentLifecycle() {
        if (currentLifecycle != null) {
            // Remove previous use cases
            CameraX.unbind(imageCaptureUseCase, videoCaptureUseCase, viewFinderUseCase);
        }

        currentLifecycle = null;
    }

    private Rect getSensorSize(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        return characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
    }

    String getActiveCamera() throws CameraInfoUnavailableException {
        return CameraX.getCameraWithLensFacing(cameraLensFacing);
    }

    @UiThread
    private void transformPreview() {
        int viewfinderWidth = getViewFinderWidth();
        int viewfinderHeight = getViewFinderHeight();
        int displayOrientation = getDisplayRotationDegrees();

        Matrix matrix = new Matrix();

        // Apply rotation of the display
        int rotation = -displayOrientation;

        int px = (int) Math.round(viewfinderWidth / 2d);
        int py = (int) Math.round(viewfinderHeight / 2d);

        matrix.postRotate(rotation, px, py);

        if (displayOrientation == 90 || displayOrientation == 270) {
            // Swap width and height
            float xScale = viewfinderWidth / (float) viewfinderHeight;
            float yScale = viewfinderHeight / (float) viewfinderWidth;

            matrix.postScale(xScale, yScale, px, py);
        }

        setTransform(matrix);
    }

    // Update view related information used in use cases
    private void updateViewInfo() {
        if (imageCaptureUseCase != null) {
            imageCaptureUseCase.setTargetAspectRatio(new Rational(getWidth(), getHeight()));
            imageCaptureUseCase.setTargetRotation(getDisplaySurfaceRotation());
        }

        if (videoCaptureUseCase != null) {
            videoCaptureUseCase.setTargetRotation(getDisplaySurfaceRotation());
        }
    }

    @RequiresPermission(permission.CAMERA)
    private Set<LensFacing> getAvailableCameraLensFacing() {
        // Start with all camera directions
        Set<LensFacing> available = new LinkedHashSet<>(Arrays.asList(LensFacing.values()));

        // If we're bound to a lifecycle, remove unavailable cameras
        if (currentLifecycle != null) {
            if (!hasCameraWithLensFacing(LensFacing.BACK)) {
                available.remove(LensFacing.BACK);
            }

            if (!hasCameraWithLensFacing(LensFacing.FRONT)) {
                available.remove(LensFacing.FRONT);
            }
        }

        return available;
    }

    public FlashMode getFlash() {
        return flash;
    }

    public void setFlash(FlashMode flash) {
        this.flash = flash;

        if (imageCaptureUseCase == null) {
            // Do nothing if there is no imageCaptureUseCase
            return;
        }

        imageCaptureUseCase.setFlashMode(flash);
    }

    public void enableTorch(boolean torch) {
        if (viewFinderUseCase == null) {
            return;
        }
        viewFinderUseCase.enableTorch(torch);
    }

    public boolean isTorchOn() {
        if (viewFinderUseCase == null) {
            return false;
        }
        return viewFinderUseCase.isTorchOn();
    }

    public Context getContext() {
        return cameraView.getContext();
    }

    public int getWidth() {
        return cameraView.getWidth();
    }

    public int getHeight() {
        return cameraView.getHeight();
    }

    public int getDisplayRotationDegrees() {
        return CameraOrientationUtil.surfaceRotationToDegrees(getDisplaySurfaceRotation());
    }

    protected int getDisplaySurfaceRotation() {
        return cameraView.getDisplaySurfaceRotation();
    }

    public void setSurfaceTexture(SurfaceTexture st) {
        cameraView.setSurfaceTexture(st);
    }

    private int getViewFinderWidth() {
        return cameraView.getViewFinderWidth();
    }

    private int getViewFinderHeight() {
        return cameraView.getViewFinderHeight();
    }

    private int getMeasuredWidth() {
        return cameraView.getMeasuredWidth();
    }

    private int getMeasuredHeight() {
        return cameraView.getMeasuredHeight();
    }

    void setTransform(final Matrix matrix) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            cameraView.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            setTransform(matrix);
                        }
                    });
        } else {
            cameraView.setTransform(matrix);
        }
    }

    /**
     * Notify the view that the source dimensions have changed.
     *
     * <p>This will allow the view to layout the viewfinder to display the correct aspect ratio.
     *
     * @param width  width of camera source buffers.
     * @param height height of camera source buffers.
     */
    private void onViewfinderSourceDimensUpdated(int width, int height) {
        cameraView.onViewfinderSourceDimensUpdated(width, height);
    }

    public CameraView.CaptureMode getCaptureMode() {
        return captureMode;
    }

    public void setCaptureMode(CameraView.CaptureMode captureMode) {
        this.captureMode = captureMode;
        rebindToLifecycle();
    }

    public long getMaxVideoDuration() {
        return maxVideoDuration;
    }

    public void setMaxVideoDuration(long duration) {
        maxVideoDuration = duration;
    }

    public long getMaxVideoSize() {
        return maxVideoSize;
    }

    public void setMaxVideoSize(long size) {
        maxVideoSize = size;
    }

    public boolean isPaused() {
        return false;
    }
}
