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

package androidx.camera.view;

import static androidx.camera.view.AccelerometerRotationListener.INVALID_SURFACE_ROTATION;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalUseCaseGroup;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.ViewPort;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The abstract base camera controller class.
 *
 * <p> The controller is a high level API manages the entire CameraX stack. This base class is
 * responsible for 1) initializing camera stack and 2) creating use cases based on user inputs.
 * Subclass this class to bind the use cases to camera.
 */
abstract class CameraController {

    private static final String TAG = "CameraController";

    CameraSelector mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    private static final String CAMERA_NOT_READY = "Camera is not ready.";

    private static final String IMAGE_CAPTURE_DISABLED_ERR_MSG = "ImageCapture disabled.";
    private static final String VIDEO_CAPTURE_DISABLED_ERR_MSG = "VideoCapture disabled.";

    // Auto focus is 1/6 of the area.
    private static final float AF_SIZE = 1.0f / 6.0f;
    private static final float AE_SIZE = AF_SIZE * 1.5f;

    // CameraController and PreviewView hold reference to each other. The 2-way link is managed
    // by PreviewView.
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    Preview mPreview;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    ImageCapture mImageCapture;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @ImageCapture.FlashMode
    int mFlashMode = ImageCapture.FLASH_MODE_OFF;

    // ImageCapture is enabled by default.
    private boolean mImageCaptureEnabled = true;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    VideoCapture mVideoCapture;

    // VideoCapture is disabled by default.
    private boolean mVideoCaptureEnabled = false;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @NonNull
    final AtomicBoolean mVideoIsRecording = new AtomicBoolean(false);

    // The latest bound camera.
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    Camera mCamera;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    ProcessCameraProvider mCameraProvider;


    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    ViewPort mViewPort;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    Preview.SurfaceProvider mSurfaceProvider;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    Display mPreviewDisplay;

    @NonNull
    private final AccelerometerRotationListener mAccelerometerRotationListener;

    @Nullable
    private final DisplayChangeListener mDisplayChangeListener;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    int mAccelerometerRotation = INVALID_SURFACE_ROTATION;
    int mDisplayRotation = INVALID_SURFACE_ROTATION;

    private boolean mPinchToZoomEnabled = true;
    private boolean mTapToFocusEnabled = true;

    private final ForwardingLiveData<ZoomState> mZoomState = new ForwardingLiveData<>();
    private final ForwardingLiveData<Integer> mTorchState = new ForwardingLiveData<>();

    private final Context mAppContext;

    CameraController(@NonNull Context context) {
        mAppContext = context.getApplicationContext();
        // Wait for camera to be initialized before binding use cases.
        Futures.addCallback(
                ProcessCameraProvider.getInstance(mAppContext),
                new FutureCallback<ProcessCameraProvider>() {

                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(@Nullable ProcessCameraProvider provider) {
                        mPreview = new Preview.Builder().build();
                        mPreview.setSurfaceProvider(mSurfaceProvider);
                        mImageCapture = new ImageCapture.Builder().setFlashMode(
                                mFlashMode).build();
                        mVideoCapture = new VideoCapture.Builder().build();
                        mCameraProvider = provider;
                        updateImageAndVideoRotationIfCameraIsReady();
                        updatePreviewRotationIfCameraIsReady();
                        startCameraAndTrackStates();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // TODO(b/148791439): fail gracefully and notify caller.
                        throw new RuntimeException("CameraX failed to initialize.", t);
                    }

                }, CameraXExecutors.mainThreadExecutor());

        // Listen to display rotation and set target rotation for Preview.
        mDisplayChangeListener = new DisplayChangeListener();

        // Listen to accelerometer reading and set target rotation for ImageCapture and
        // VideoCapture.
        mAccelerometerRotationListener = new AccelerometerRotationListener(mAppContext) {
            @Override
            public void onRotationChanged(int rotation) {
                mAccelerometerRotation = rotation;
                updateImageAndVideoRotationIfCameraIsReady();
            }
        };
    }

    /**
     * Implemented by children to refresh after {@link UseCase} is changed.
     */
    @Nullable
    abstract Camera startCamera();

    // ------------------
    // Preview use case.
    // ------------------

    /**
     * Internal API used by {@link PreviewView} to notify changes.
     */
    @SuppressLint({"MissingPermission", "WrongConstant"})
    @MainThread
    @UseExperimental(markerClass = ExperimentalUseCaseGroup.class)
    void attachPreviewSurface(@NonNull Preview.SurfaceProvider surfaceProvider,
            @NonNull ViewPort viewPort, @NonNull Display display) {
        Threads.checkMainThread();
        if (mSurfaceProvider != surfaceProvider) {
            // Avoid setting provider unnecessarily which restarts Preview pipeline.
            mSurfaceProvider = surfaceProvider;
            if (mPreview != null) {
                mPreview.setSurfaceProvider(surfaceProvider);
            }
        }
        mViewPort = viewPort;
        mPreviewDisplay = display;
        // Update default rotation value with display rotation.
        if (mAccelerometerRotation == INVALID_SURFACE_ROTATION) {
            mAccelerometerRotation = display.getRotation();
            updateImageAndVideoRotationIfCameraIsReady();
        }
        if (mDisplayRotation == INVALID_SURFACE_ROTATION) {
            mDisplayRotation = display.getRotation();
            updatePreviewRotationIfCameraIsReady();
        }
        startListeningToRotationEvents();
        startCameraAndTrackStates();
    }

    /**
     * Clear {@link PreviewView} to remove the UI reference.
     */
    @MainThread
    void clearPreviewSurface() {
        Threads.checkMainThread();
        if (mCameraProvider != null) {
            // Preview is required. Unbind everything if Preview is down.
            mCameraProvider.unbindAll();
        }
        if (mPreview != null) {
            mPreview.setSurfaceProvider(null);
        }
        mCamera = null;
        mSurfaceProvider = null;
        mViewPort = null;
        mPreviewDisplay = null;
        stopListeningToRotationEvents();
    }

    private void startListeningToRotationEvents() {
        getDisplayManager().registerDisplayListener(mDisplayChangeListener,
                new Handler(Looper.getMainLooper()));
        if (mAccelerometerRotationListener.canDetectOrientation()) {
            mAccelerometerRotationListener.enable();
        }
    }

    private void stopListeningToRotationEvents() {
        getDisplayManager().unregisterDisplayListener(mDisplayChangeListener);
        mAccelerometerRotationListener.disable();
    }

    private DisplayManager getDisplayManager() {
        return (DisplayManager) mAppContext.getSystemService(Context.DISPLAY_SERVICE);
    }

    // ----------------------
    // ImageCapture UseCase.
    // ----------------------

    /**
     * Checks if {@link ImageCapture} is enabled.
     *
     * @see ImageCapture
     */
    @MainThread
    public boolean isImageCaptureEnabled() {
        Threads.checkMainThread();
        return mImageCaptureEnabled;
    }

    /**
     * Enables or disables {@link ImageCapture}.
     *
     * @see ImageCapture
     */
    @MainThread
    public void setImageCaptureEnabled(boolean imageCaptureEnabled) {
        Threads.checkMainThread();
        mImageCaptureEnabled = imageCaptureEnabled;
        if (mCameraProvider != null && !imageCaptureEnabled) {
            mCameraProvider.unbind(mImageCapture);
        }
        startCameraAndTrackStates();
    }

    /**
     * Gets the flash mode for {@link ImageCapture}.
     *
     * @return the flashMode. Value is {@link ImageCapture.FlashMode##FLASH_MODE_AUTO},
     * {@link ImageCapture.FlashMode##FLASH_MODE_ON}, or
     * {@link ImageCapture.FlashMode##FLASH_MODE_OFF}.
     * @see ImageCapture.FlashMode
     */
    @ImageCapture.FlashMode
    public int getImageCaptureFlashMode() {
        return mFlashMode;
    }

    /**
     * Sets the flash mode for {@link ImageCapture}.
     *
     * <p>If not set, the flash mode will default to {@link ImageCapture.FlashMode#FLASH_MODE_OFF}.
     *
     * @param flashMode the {@link ImageCapture.FlashMode} for {@link ImageCapture}.
     * @see ImageCapture.FlashMode
     */
    public void setImageCaptureFlashMode(@ImageCapture.FlashMode int flashMode) {
        Threads.checkMainThread();
        mFlashMode = flashMode;
        if (mImageCapture == null) {
            // Camera is not ready.
            return;
        }
        mImageCapture.setFlashMode(flashMode);
        startCameraAndTrackStates();
    }

    /**
     * Captures a new still image and saves to a file along with application specified metadata.
     *
     * <p>The callback will be called only once for every invocation of this method.
     *
     * @param outputFileOptions  Options to store the newly captured image.
     * @param executor           The executor in which the callback methods will be run.
     * @param imageSavedCallback Callback to be called for the newly captured image.
     * @see ImageCapture#takePicture(
     *ImageCapture.OutputFileOptions, Executor, ImageCapture.OnImageSavedCallback)
     */
    @MainThread
    public void takePicture(
            ImageCapture.OutputFileOptions outputFileOptions,
            Executor executor,
            ImageCapture.OnImageSavedCallback imageSavedCallback) {
        Threads.checkMainThread();
        if (mImageCapture == null) {
            imageSavedCallback.onError(new ImageCaptureException(ImageCapture.ERROR_UNKNOWN,
                    CAMERA_NOT_READY, null));
            return;
        }
        Preconditions.checkState(mImageCaptureEnabled, IMAGE_CAPTURE_DISABLED_ERR_MSG);

        // Mirror the image for front camera.
        if (mCameraSelector.getLensFacing() != null) {
            outputFileOptions.getMetadata().setReversedHorizontal(
                    mCameraSelector.getLensFacing() == CameraSelector.LENS_FACING_FRONT);
        }
        mImageCapture.takePicture(outputFileOptions, executor, imageSavedCallback);
    }

    /**
     * Captures a new still image for in memory access.
     *
     * <p>The listener is responsible for calling {@link ImageProxy#close()} on the returned image.
     *
     * @param executor The executor in which the callback methods will be run.
     * @param callback Callback to be invoked for the newly captured image
     * @see ImageCapture#takePicture(Executor, ImageCapture.OnImageCapturedCallback)
     */
    @MainThread
    public void takePicture(
            Executor executor,
            ImageCapture.OnImageCapturedCallback callback) {
        Threads.checkMainThread();
        if (mImageCapture == null) {
            callback.onError(new ImageCaptureException(ImageCapture.ERROR_UNKNOWN,
                    CAMERA_NOT_READY, null));
            return;
        }
        Preconditions.checkState(mImageCaptureEnabled, IMAGE_CAPTURE_DISABLED_ERR_MSG);
        mImageCapture.takePicture(executor, callback);
    }

    // -----------------
    // Video capture
    // -----------------

    /**
     * Checks if {@link VideoCapture} is use case.
     *
     * @see ImageCapture
     */
    @MainThread
    public boolean isVideoCaptureEnabled() {
        Threads.checkMainThread();
        return mVideoCaptureEnabled;
    }

    /**
     * Enables or disables {@link VideoCapture} use case.
     *
     * <p> Note that using both {@link #setVideoCaptureEnabled} and
     * {@link #setImageCaptureEnabled} simultaneously true may not work on lower end devices.
     *
     * @see ImageCapture
     */
    @MainThread
    public void setVideoCaptureEnabled(boolean videoCaptureEnabled) {
        Threads.checkMainThread();
        if (mVideoCaptureEnabled && !videoCaptureEnabled) {
            stopRecording();
        }
        mVideoCaptureEnabled = videoCaptureEnabled;
        if (mCameraProvider != null && !videoCaptureEnabled) {
            mCameraProvider.unbind(mVideoCapture);
        }
        startCameraAndTrackStates();
    }

    /**
     * Takes a video and calls the OnVideoSavedCallback when done.
     *
     * @param outputFileOptions Options to store the newly captured video.
     * @param executor          The executor in which the callback methods will be run.
     * @param callback          Callback which will receive success or failure.
     */
    @MainThread
    public void startRecording(VideoCapture.OutputFileOptions outputFileOptions,
            Executor executor, final VideoCapture.OnVideoSavedCallback callback) {
        Threads.checkMainThread();
        Preconditions.checkState(mVideoCaptureEnabled, VIDEO_CAPTURE_DISABLED_ERR_MSG);
        if (mVideoCapture == null) {
            callback.onError(ImageCapture.ERROR_UNKNOWN, CAMERA_NOT_READY, null);
            return;
        }
        mVideoCapture.startRecording(outputFileOptions, executor,
                new VideoCapture.OnVideoSavedCallback() {
                    @Override
                    public void onVideoSaved(
                            @NonNull VideoCapture.OutputFileResults outputFileResults) {
                        mVideoIsRecording.set(false);
                        callback.onVideoSaved(outputFileResults);
                    }

                    @Override
                    public void onError(int videoCaptureError, @NonNull String message,
                            @Nullable Throwable cause) {
                        mVideoIsRecording.set(false);
                        callback.onError(videoCaptureError, message, cause);
                    }
                });
        mVideoIsRecording.set(true);
    }

    /**
     * Stops a in progress video recording.
     */
    @MainThread
    public void stopRecording() {
        Threads.checkMainThread();
        if (mVideoIsRecording.get() && mVideoCapture != null) {
            mVideoCapture.stopRecording();
        }
    }

    /**
     * Returns whether there is a in progress video recording.
     */
    @MainThread
    public boolean isRecording() {
        Threads.checkMainThread();
        return mVideoIsRecording.get();
    }

    // -----------------
    // Camera control
    // -----------------

    /**
     * Sets the {@link CameraSelector}. The default value is
     * {@link CameraSelector#DEFAULT_BACK_CAMERA}.
     *
     * @see CameraSelector
     */
    public void setCameraSelector(@NonNull CameraSelector cameraSelector) {
        mCameraSelector = cameraSelector;
        if (mCameraProvider != null) {
            // Preview is required. Unbind everything if Preview is down.
            mCameraProvider.unbindAll();
        }
        startCameraAndTrackStates();
    }

    /**
     * Gets the {@link CameraSelector}.
     *
     * @see CameraSelector
     */
    public CameraSelector getCameraSelector() {
        return mCameraSelector;
    }

    /**
     * Returns whether pinch-to-zoom is enabled.
     *
     * <p> By default pinch-to-zoom is enabled.
     *
     * @return True if pinch-to-zoom is enabled.
     */
    @MainThread
    public boolean isPinchToZoomEnabled() {
        Threads.checkMainThread();
        return mPinchToZoomEnabled;
    }

    /**
     * Enables/disables pinch-to-zoom.
     *
     * <p>Once enabled, end user can pinch on the {@link PreviewView} to zoom in/out if the bound
     * camera supports zooming.
     *
     * @param enabled True to enable pinch-to-zoom.
     */
    @MainThread
    public void setPinchToZoomEnabled(boolean enabled) {
        Threads.checkMainThread();
        mPinchToZoomEnabled = enabled;
    }

    /**
     * Called by {@link PreviewView} for a pinch-to-zoom event.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    void onPinchToZoom(float pinchToZoomScale) {
        if (mCamera == null) {
            Logger.d(TAG, CAMERA_NOT_READY);
            return;
        }
        if (!mPinchToZoomEnabled) {
            Logger.d(TAG, "Pinch to zoom disabled.");
            return;
        }
        Logger.d(TAG, "Pinch to zoom with scale: " + pinchToZoomScale);

        ZoomState zoomState = getZoomState().getValue();
        if (zoomState == null) {
            return;
        }
        float clampedRatio = zoomState.getZoomRatio() * speedUpZoomBy2X(pinchToZoomScale);
        // Clamp the ratio with the zoom range.
        clampedRatio = Math.min(Math.max(clampedRatio, zoomState.getMinZoomRatio()),
                zoomState.getMaxZoomRatio());
        setZoomRatio(clampedRatio);
    }

    private float speedUpZoomBy2X(float scaleFactor) {
        if (scaleFactor > 1f) {
            return 1.0f + (scaleFactor - 1.0f) * 2;
        } else {
            return 1.0f - (1.0f - scaleFactor) * 2;
        }
    }

    /**
     * Called by {@link PreviewView} for a tap-to-focus event.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    void onTapToFocus(MeteringPointFactory meteringPointFactory, float x, float y) {
        if (mCamera == null) {
            Logger.w(TAG, CAMERA_NOT_READY);
            return;
        }
        if (!mTapToFocusEnabled) {
            Logger.d(TAG, "Tap to focus disabled. ");
            return;
        }
        Logger.d(TAG, "Tap to focus: " + x + ", " + y);
        MeteringPoint afPoint = meteringPointFactory.createPoint(x, y, AF_SIZE);
        MeteringPoint aePoint = meteringPointFactory.createPoint(x, y, AE_SIZE);
        mCamera.getCameraControl().startFocusAndMetering(new FocusMeteringAction
                .Builder(afPoint, FocusMeteringAction.FLAG_AF)
                .addPoint(aePoint, FocusMeteringAction.FLAG_AE)
                .build());
    }

    /**
     * Returns whether tap-to-focus is enabled.
     *
     * <p> By default tap-to-focus is enabled.
     *
     * @return True if tap-to-focus is enabled.
     */
    @MainThread
    public boolean isTapToFocusEnabled() {
        Threads.checkMainThread();
        return mTapToFocusEnabled;
    }

    /**
     * Enables/disables tap-to-focus.
     *
     * <p>Once enabled, end user can tap on the {@link PreviewView} to set focus point.
     *
     * @param enabled True to enable tap-to-focus.
     */
    @MainThread
    public void setTapToFocusEnabled(boolean enabled) {
        Threads.checkMainThread();
        mTapToFocusEnabled = enabled;
    }

    /**
     * Returns a {@link LiveData} of {@link ZoomState}.
     *
     * <p>The LiveData will be updated whenever the set zoom state has been changed. This can
     * occur when the application updates the zoom via {@link #setZoomRatio(float)}
     * or {@link #setLinearZoom(float)}. The zoom state can also change anytime a
     * camera starts up, for example when {@link #setCameraSelector} is called.
     *
     * @see CameraInfo#getZoomState()
     */
    @MainThread
    public LiveData<ZoomState> getZoomState() {
        Threads.checkMainThread();
        return mZoomState;
    }

    /**
     * Sets current zoom by ratio.
     *
     * <p>Valid zoom values range from {@link ZoomState#getMinZoomRatio()} to
     * {@link ZoomState#getMaxZoomRatio()}.
     *
     * <p> No-ops if camera is not ready.
     *
     * @param zoomRatio The requested zoom ratio.
     * @return a {@link ListenableFuture} which is finished when camera is set to the given ratio.
     * It fails with {@link CameraControl.OperationCanceledException} if there is newer value
     * being set or camera is closed. If the ratio is out of range, it fails with
     * {@link IllegalArgumentException}. Cancellation of this future is a no-op.
     * @see #getZoomState()
     * @see CameraControl#setZoomRatio(float)
     */
    @MainThread
    public ListenableFuture<Void> setZoomRatio(float zoomRatio) {
        Threads.checkMainThread();
        if (mCamera == null) {
            Logger.w(TAG, CAMERA_NOT_READY);
            return Futures.immediateFuture(null);
        }
        return mCamera.getCameraControl().setZoomRatio(zoomRatio);
    }

    /**
     * Sets current zoom by a linear zoom value ranging from 0f to 1.0f.
     *
     * LinearZoom 0f represents the minimum zoom while linearZoom 1.0f represents the maximum
     * zoom. The advantage of linearZoom is that it ensures the field of view (FOV) varies
     * linearly with the linearZoom value, for use with slider UI elements (while
     * {@link #setZoomRatio(float)} works well for pinch-zoom gestures).
     *
     * <p> No-ops if camera is not ready.
     *
     * @return a {@link ListenableFuture} which is finished when camera is set to the given ratio.
     * It fails with {@link CameraControl.OperationCanceledException} if there is newer value
     * being set or camera is closed. If the ratio is out of range, it fails with
     * {@link IllegalArgumentException}. Cancellation of this future is a no-op.
     * @see CameraControl#setLinearZoom(float)
     */
    @MainThread
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        Threads.checkMainThread();
        if (mCamera == null) {
            Logger.w(TAG, CAMERA_NOT_READY);
            return Futures.immediateFuture(null);
        }
        return mCamera.getCameraControl().setLinearZoom(linearZoom);
    }

    /**
     * Returns a {@link LiveData} of current {@link TorchState}.
     *
     * <p>The torch can be turned on and off via {@link #enableTorch(boolean)} which
     * will trigger the change event to the returned {@link LiveData}.
     *
     * @return a {@link LiveData} containing current torch state.
     * @see CameraInfo#getTorchState()
     */
    @MainThread
    public LiveData<Integer> getTorchState() {
        Threads.checkMainThread();
        return mTorchState;
    }

    /**
     * Enable the torch or disable the torch.
     *
     * <p> No-ops if camera is not ready.
     *
     * @param torchEnabled true to turn on the torch, false to turn it off.
     * @return A {@link ListenableFuture} which is successful when the torch was changed to the
     * value specified. It fails when it is unable to change the torch state. Cancellation of
     * this future is a no-op.
     * @see CameraControl#enableTorch(boolean)
     */
    @MainThread
    public ListenableFuture<Void> enableTorch(boolean torchEnabled) {
        Threads.checkMainThread();
        if (mCamera == null) {
            Logger.w(TAG, CAMERA_NOT_READY);
            return Futures.immediateFuture(null);
        }
        return mCamera.getCameraControl().enableTorch(torchEnabled);
    }

    // TODO(b/148791439): Give user a way to tell if the camera provider is ready.

    /**
     * Binds use cases, gets a new {@link Camera} instance and tracks the state of the camera.
     */
    void startCameraAndTrackStates() {
        mCamera = startCamera();
        if (mCamera == null) {
            return;
        }
        mZoomState.setSource(mCamera.getCameraInfo().getZoomState());
        mTorchState.setSource(mCamera.getCameraInfo().getTorchState());
    }

    /**
     * Creates {@link UseCaseGroup} from all the use cases.
     *
     * <p> Preview is required. If it is null, then controller is not ready. Return null and ignore
     * other use cases.
     */
    @UseExperimental(markerClass = ExperimentalUseCaseGroup.class)
    protected UseCaseGroup createUseCaseGroup() {
        if (mCameraProvider == null || mPreview == null) {
            Logger.d(TAG, CAMERA_NOT_READY);
            return null;
        }
        if (mSurfaceProvider == null || mViewPort == null) {
            // Preview is required. Return early if preview Surface is not ready.
            Logger.d(TAG, "Preview is not ready.");
            return null;
        }

        UseCaseGroup.Builder builder = new UseCaseGroup.Builder().addUseCase(mPreview);

        if (mImageCaptureEnabled && mImageCapture != null) {
            builder.addUseCase(mImageCapture);
        }
        if (mVideoCaptureEnabled && mVideoCapture != null) {
            builder.addUseCase(mVideoCapture);
        }

        builder.setViewPort(mViewPort);
        return builder.build();
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @SuppressLint("WrongConstant")
    void updateImageAndVideoRotationIfCameraIsReady() {
        if (mAccelerometerRotation != INVALID_SURFACE_ROTATION && mImageCapture != null
                && mVideoCapture != null) {
            mImageCapture.setTargetRotation(mAccelerometerRotation);
            mVideoCapture.setTargetRotation(mAccelerometerRotation);
        }

    }

    @SuppressLint("WrongConstant")
    @UseExperimental(markerClass = ExperimentalUseCaseGroup.class)
    void updatePreviewRotationIfCameraIsReady() {
        if (mDisplayRotation != INVALID_SURFACE_ROTATION && mPreview != null) {
            mPreview.setTargetRotation(mDisplayRotation);
        }
    }

    /**
     * Listener for display changes.
     *
     * <p>When the device is rotated 180° from side to side, the activity is not
     * destroyed and recreated, thus {@link #attachPreviewSurface} will not be invoked. This
     * class is necessary to make sure preview's target rotation is the display rotation when
     * that happens.
     */
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    class DisplayChangeListener implements DisplayManager.DisplayListener {

        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onDisplayChanged(int displayId) {
            if (mPreviewDisplay != null && mPreview != null
                    && mPreviewDisplay.getDisplayId() == displayId) {
                mDisplayRotation = mPreviewDisplay.getRotation();
                updatePreviewRotationIfCameraIsReady();
            }
        }
    }
}
