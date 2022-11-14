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

import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.camera.view.CameraController.OutputSize.UNASSIGNED_ASPECT_RATIO;
import static androidx.core.content.ContextCompat.getMainExecutor;

import static java.util.Collections.emptyList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.hardware.camera2.CaptureResult;
import android.os.Build;
import android.util.Size;
import android.view.Display;

import androidx.annotation.DoNotInline;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.InitializationException;
import androidx.camera.core.Logger;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileDescriptorOutputOptions;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.OutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.transform.OutputTransform;
import androidx.camera.view.video.AudioConfig;
import androidx.camera.view.video.ExperimentalVideo;
import androidx.core.content.PermissionChecker;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * The abstract base camera controller class.
 *
 * <p> This a high level controller that provides most of the CameraX core features
 * in a single class. It handles camera initialization, creates and configures {@link UseCase}s.
 * It also listens to device motion sensor and set the target rotation for the use cases.
 *
 * <p> The controller is required to be used with a {@link PreviewView}. {@link PreviewView}
 * provides the UI elements to display camera preview. The layout of the {@link PreviewView} is
 * used to set the crop rect so the output from other use cases matches the preview display in a
 * WYSIWYG way. The controller also listens to {@link PreviewView}'s touch events to handle
 * tap-to-focus and pinch-to-zoom features.
 *
 * <p> This class provides features of 4 {@link UseCase}s: {@link Preview}, {@link ImageCapture},
 * {@link ImageAnalysis} and an experimental video capture. {@link Preview} is required and always
 * enabled. {@link ImageCapture} and {@link ImageAnalysis} are enabled by default. The video
 * capture feature is experimental. It's disabled by default because it might conflict with other
 * use cases, especially on lower end devices. It might be necessary to disable {@link ImageCapture}
 * and/or {@link ImageAnalysis} before the video capture feature can be enabled. Disabling/enabling
 * {@link UseCase}s freezes the preview for a short period of time. To avoid the glitch, the
 * {@link UseCase}s need to be enabled/disabled before the controller is set on {@link PreviewView}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class CameraController {

    private static final String TAG = "CameraController";

    // Externally visible error messages.
    private static final String CAMERA_NOT_INITIALIZED = "Camera not initialized.";
    private static final String PREVIEW_VIEW_NOT_ATTACHED =
            "PreviewView not attached to CameraController.";
    private static final String CAMERA_NOT_ATTACHED = "Use cases not attached to camera.";
    private static final String IMAGE_CAPTURE_DISABLED = "ImageCapture disabled.";
    private static final String VIDEO_CAPTURE_DISABLED = "VideoCapture disabled.";
    private static final String VIDEO_RECORDING_UNFINISHED = "Recording video. Only one recording"
            + " can be active at a time.";

    // Auto focus is 1/6 of the area.
    private static final float AF_SIZE = 1.0f / 6.0f;
    private static final float AE_SIZE = AF_SIZE * 1.5f;

    /**
     * {@link ImageAnalysis.Analyzer} option for returning {@link PreviewView} coordinates.
     *
     * <p>When the {@link ImageAnalysis.Analyzer} is configured with this option, it will receive a
     * {@link Matrix} that will receive a value that represents the transformation from camera
     * sensor to the {@link PreviewView}, which can be used for highlighting detected result in
     * {@link PreviewView}. For example, laying over a bounding box on top of the detected face.
     *
     * <p>Note this option only works if the {@link ImageAnalysis.Analyzer} is set via
     * {@link CameraController#setImageAnalysisAnalyzer}. It will not be effective when used with
     * camera-core directly.
     *
     * @see ImageAnalysis.Analyzer
     */
    public static final int COORDINATE_SYSTEM_VIEW_REFERENCED = 1;

    /**
     * States for tap-to-focus feature.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {TAP_TO_FOCUS_NOT_STARTED, TAP_TO_FOCUS_STARTED, TAP_TO_FOCUS_FOCUSED,
            TAP_TO_FOCUS_NOT_FOCUSED, TAP_TO_FOCUS_FAILED})
    public @interface TapToFocusStates {
    }

    /**
     * No tap-to-focus action has been started by the end user.
     */
    public static final int TAP_TO_FOCUS_NOT_STARTED = 0;

    /**
     * A tap-to-focus action has started but not completed. The app also gets notified with this
     * state if a new action happens before the previous one could finish.
     */
    public static final int TAP_TO_FOCUS_STARTED = 1;

    /**
     * The previous tap-to-focus action was completed successfully and the camera is focused.
     */
    public static final int TAP_TO_FOCUS_FOCUSED = 2;

    /**
     * The previous tap-to-focus action was completed successfully but the camera is still
     * unfocused, similar to the {@link CaptureResult#CONTROL_AF_STATE_NOT_FOCUSED_LOCKED} state.
     * The end user might be able to get a better result by trying again with different camera
     * distances and/or lighting.
     */
    public static final int TAP_TO_FOCUS_NOT_FOCUSED = 3;

    /**
     * The previous tap-to-focus action was failed to complete. This is usually due to device
     * limitations.
     */
    public static final int TAP_TO_FOCUS_FAILED = 4;

    /**
     * Bitmask options to enable/disable use cases.
     *
     * @hide
     */
    @OptIn(markerClass = ExperimentalVideo.class)
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(flag = true, value = {IMAGE_CAPTURE, IMAGE_ANALYSIS, VIDEO_CAPTURE})
    public @interface UseCases {
    }

    /**
     * Bitmask option to enable {@link ImageCapture}. In {@link #setEnabledUseCases}, if
     * (enabledUseCases & IMAGE_CAPTURE) != 0, then controller will enable image capture features.
     */
    public static final int IMAGE_CAPTURE = 1;
    /**
     * Bitmask option to enable {@link ImageAnalysis}. In {@link #setEnabledUseCases}, if
     * (enabledUseCases & IMAGE_ANALYSIS) != 0, then controller will enable image analysis features.
     */
    public static final int IMAGE_ANALYSIS = 1 << 1;
    /**
     * Bitmask option to enable video capture use case. In {@link #setEnabledUseCases}, if
     * (enabledUseCases & VIDEO_CAPTURE) != 0, then controller will enable video capture features.
     */
    @ExperimentalVideo
    public static final int VIDEO_CAPTURE = 1 << 2;

    CameraSelector mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    // By default, ImageCapture and ImageAnalysis are enabled. VideoCapture is disabled.
    private int mEnabledUseCases = IMAGE_CAPTURE | IMAGE_ANALYSIS;

    // CameraController and PreviewView hold reference to each other. The 2-way link is managed
    // by PreviewView.
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @NonNull
    Preview mPreview;

    @Nullable
    OutputSize mPreviewTargetSize;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @NonNull
    ImageCapture mImageCapture;

    @Nullable
    OutputSize mImageCaptureTargetSize;

    @Nullable
    Executor mImageCaptureIoExecutor;

    @Nullable
    private Executor mAnalysisExecutor;

    @Nullable
    private Executor mAnalysisBackgroundExecutor;

    @Nullable
    private ImageAnalysis.Analyzer mAnalysisAnalyzer;

    @NonNull
    ImageAnalysis mImageAnalysis;

    @Nullable
    OutputSize mImageAnalysisTargetSize;

    @NonNull
    VideoCapture<Recorder> mVideoCapture;

    @Nullable
    Recording mActiveRecording = null;

    @NonNull
    Map<Consumer<VideoRecordEvent>, Recording> mRecordingMap = new HashMap<>();

    @Nullable
    Quality mVideoCaptureQuality;

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

    private final RotationProvider mRotationProvider;

    @VisibleForTesting
    @NonNull
    final RotationProvider.Listener mDeviceRotationListener;

    private boolean mPinchToZoomEnabled = true;
    private boolean mTapToFocusEnabled = true;

    private final ForwardingLiveData<ZoomState> mZoomState = new ForwardingLiveData<>();
    private final ForwardingLiveData<Integer> mTorchState = new ForwardingLiveData<>();
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final MutableLiveData<Integer> mTapToFocusState = new MutableLiveData<>(
            TAP_TO_FOCUS_NOT_STARTED);

    @NonNull
    private List<CameraEffect> mEffects = emptyList();

    private final Context mAppContext;

    @NonNull
    private final ListenableFuture<Void> mInitializationFuture;

    CameraController(@NonNull Context context) {
        mAppContext = getApplicationContext(context);
        mPreview = new Preview.Builder().build();
        mImageCapture = new ImageCapture.Builder().build();
        mImageAnalysis = new ImageAnalysis.Builder().build();
        mVideoCapture = createNewVideoCapture();

        // Wait for camera to be initialized before binding use cases.
        mInitializationFuture = Futures.transform(
                ProcessCameraProvider.getInstance(mAppContext),
                provider -> {
                    mCameraProvider = provider;
                    startCameraAndTrackStates();
                    return null;
                }, mainThreadExecutor());

        // Listen for device rotation changes and set target rotation for non-preview use cases.
        // The output of non-preview use cases need to be corrected in fixed landscape/portrait
        // mode.
        mRotationProvider = new RotationProvider(mAppContext);
        mDeviceRotationListener = rotation -> {
            mImageAnalysis.setTargetRotation(rotation);
            mImageCapture.setTargetRotation(rotation);
            mVideoCapture.setTargetRotation(rotation);
        };
    }

    private static Recorder generateVideoCaptureRecorder(Quality videoQuality) {
        Recorder.Builder builder = new Recorder.Builder();
        if (videoQuality != null) {
            builder.setQualitySelector(QualitySelector.from(
                    videoQuality,
                    FallbackStrategy.lowerQualityOrHigherThan(videoQuality)
            ));
        }

        return builder.build();
    }

    /**
     * Gets the application context and preserves the attribution tag.
     *
     * TODO(b/185272953): instrument test getting attribution tag once the view artifact depends
     * on a core version that has the fix.
     */
    private static Context getApplicationContext(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String attributeTag = Api30Impl.getAttributionTag(context);

            if (attributeTag != null) {
                return Api30Impl.createAttributionContext(applicationContext, attributeTag);
            }
        }

        return applicationContext;
    }

    /**
     * Gets a {@link ListenableFuture} that completes when camera initialization completes.
     *
     * <p> This future may fail with an {@link InitializationException} and associated cause that
     * can be retrieved by {@link Throwable#getCause()}. The cause will be a
     * {@link CameraUnavailableException} if it fails to access any camera during initialization.
     *
     * <p> In the rare case that the future fails with {@link CameraUnavailableException}, the
     * camera will become unusable. This could happen for various reasons, for example hardware
     * failure or the camera being held by another process. If the failure is temporary, killing
     * and restarting the app might fix the issue.
     *
     * <p> The initialization also try to bind use cases before completing the
     * {@link ListenableFuture}. The {@link ListenableFuture} will complete successfully
     * regardless of whether the use cases are ready to be bound, e.g. it will complete
     * successfully even if the controller is not set on a {@link PreviewView}. However the
     * {@link ListenableFuture} will fail if the enabled use cases are not supported by the
     * current camera.
     *
     * @see ProcessCameraProvider#getInstance
     */
    @NonNull
    public ListenableFuture<Void> getInitializationFuture() {
        return mInitializationFuture;
    }

    /**
     * Implemented by children to refresh after {@link UseCase} is changed.
     */
    @Nullable
    abstract Camera startCamera();

    private boolean isCameraInitialized() {
        return mCameraProvider != null;
    }

    private boolean isPreviewViewAttached() {
        return mSurfaceProvider != null && mViewPort != null && mPreviewDisplay != null;
    }

    private boolean isCameraAttached() {
        return mCamera != null;
    }

    /**
     * Enables or disables use cases.
     *
     * <p> Use cases need to be enabled before they can be used. By default, {@link #IMAGE_CAPTURE}
     * and {@link #IMAGE_ANALYSIS} are enabled, and {@link #VIDEO_CAPTURE} is disabled. This is
     * necessary because {@link #VIDEO_CAPTURE} is an experimental feature that might not work
     * with other use cases, especially on lower end devices. When that happens, this method will
     * fail with an {@link IllegalStateException}.
     *
     * <p> To make sure {@link #VIDEO_CAPTURE} works, {@link #IMAGE_CAPTURE} and
     * {@link #IMAGE_ANALYSIS} needs to be disabled when enabling {@link #VIDEO_CAPTURE}. For
     * example:
     *
     * <pre><code>
     * // By default, image capture is enabled. Taking picture works.
     * controller.takePicture(...);
     *
     * // Switch to video capture to shoot video.
     * controller.setEnabledUseCases(VIDEO_CAPTURE);
     * controller.startRecording(...);
     *
     * // Switch back to image capture and image analysis before taking another picture.
     * controller.setEnabledUseCases(IMAGE_CAPTURE|IMAGE_ANALYSIS);
     * controller.takePicture(...);
     *
     * </code></pre>
     *
     * @param enabledUseCases one or more of the following use cases, bitwise-OR-ed together:
     *                        {@link #IMAGE_CAPTURE}, {@link #IMAGE_ANALYSIS} and/or
     *                        {@link #VIDEO_CAPTURE}.
     * @throws IllegalStateException If the current camera selector is unable to resolve a
     *                               camera to be used for the enabled use cases.
     * @see UseCase
     * @see ImageCapture
     * @see ImageAnalysis
     */
    @MainThread
    @OptIn(markerClass = ExperimentalVideo.class)
    public void setEnabledUseCases(@UseCases int enabledUseCases) {
        checkMainThread();
        if (enabledUseCases == mEnabledUseCases) {
            return;
        }
        int oldEnabledUseCases = mEnabledUseCases;
        mEnabledUseCases = enabledUseCases;
        if (!isVideoCaptureEnabled() && isRecording()) {
            stopRecording();
        }
        startCameraAndTrackStates(() -> mEnabledUseCases = oldEnabledUseCases);
    }

    /**
     * Checks if the given use case mask is enabled.
     *
     * @param useCaseMask One of the {@link #IMAGE_CAPTURE}, {@link #IMAGE_ANALYSIS} or
     *                    {@link #VIDEO_CAPTURE}
     * @return true if the use case is enabled.
     */
    private boolean isUseCaseEnabled(int useCaseMask) {
        return (mEnabledUseCases & useCaseMask) != 0;
    }

    /**
     * Sets the target aspect ratio or target resolution based on {@link OutputSize}.
     */
    private void setTargetOutputSize(@NonNull ImageOutputConfig.Builder<?> builder,
            @Nullable OutputSize outputSize) {
        if (outputSize == null) {
            return;
        }
        if (outputSize.getResolution() != null) {
            builder.setTargetResolution(outputSize.getResolution());
        } else if (outputSize.getAspectRatio() != UNASSIGNED_ASPECT_RATIO) {
            builder.setTargetAspectRatio(outputSize.getAspectRatio());
        } else {
            Logger.e(TAG, "Invalid target surface size. " + outputSize);
        }
    }

    /**
     * Checks if two {@link OutputSize} are equal.
     */
    private boolean isOutputSizeEqual(
            @Nullable OutputSize currentSize,
            @Nullable OutputSize newSize) {
        if (currentSize == newSize) {
            return true;
        }
        return currentSize != null && currentSize.equals(newSize);
    }

    // ------------------
    // Preview use case.
    // ------------------

    /**
     * Internal API used by {@link PreviewView} to notify changes.
     */
    @SuppressLint({"MissingPermission", "WrongConstant"})
    @MainThread
    void attachPreviewSurface(@NonNull Preview.SurfaceProvider surfaceProvider,
            @NonNull ViewPort viewPort, @NonNull Display display) {
        checkMainThread();
        if (mSurfaceProvider != surfaceProvider) {
            mSurfaceProvider = surfaceProvider;
            mPreview.setSurfaceProvider(surfaceProvider);
        }
        mViewPort = viewPort;
        mPreviewDisplay = display;
        startListeningToRotationEvents();
        startCameraAndTrackStates();
    }

    /**
     * Clear {@link PreviewView} to remove the UI reference.
     */
    @MainThread
    void clearPreviewSurface() {
        checkMainThread();
        if (mCameraProvider != null) {
            // Preview is required. Unbind everything if Preview is down.
            mCameraProvider.unbind(mPreview, mImageCapture, mImageAnalysis, mVideoCapture);
        }
        mPreview.setSurfaceProvider(null);
        mCamera = null;
        mSurfaceProvider = null;
        mViewPort = null;
        mPreviewDisplay = null;
        stopListeningToRotationEvents();
    }

    private void startListeningToRotationEvents() {
        mRotationProvider.addListener(mainThreadExecutor(),
                mDeviceRotationListener);
    }

    private void stopListeningToRotationEvents() {
        mRotationProvider.removeListener(mDeviceRotationListener);
    }

    /**
     * Sets the intended output size for {@link Preview}.
     *
     * <p> The value is used as a hint when determining the resolution and aspect ratio of the
     * preview. The actual output may differ from the requested value due to device constraints.
     *
     * <p> When set to null, the output will be based on the default config of {@link Preview}.
     *
     * <p> Changing the value will reconfigure the camera which will cause additional latency.
     * To avoid this, set the value before controller is bound to lifecycle.
     *
     * @param targetSize the intended output size for {@link Preview}.
     * @see Preview.Builder#setTargetAspectRatio(int)
     * @see Preview.Builder#setTargetResolution(Size)
     */
    @MainThread
    public void setPreviewTargetSize(@Nullable OutputSize targetSize) {
        checkMainThread();
        if (isOutputSizeEqual(mPreviewTargetSize, targetSize)) {
            return;
        }
        mPreviewTargetSize = targetSize;
        unbindPreviewAndRecreate();
        startCameraAndTrackStates();
    }

    /**
     * Returns the intended output size for {@link Preview} set by
     * {@link #setPreviewTargetSize(OutputSize)}, or null if not set.
     */
    @MainThread
    @Nullable
    public OutputSize getPreviewTargetSize() {
        checkMainThread();
        return mPreviewTargetSize;
    }

    /**
     * Unbinds {@link Preview} and recreates with the latest parameters.
     */
    private void unbindPreviewAndRecreate() {
        if (isCameraInitialized()) {
            mCameraProvider.unbind(mPreview);
        }
        Preview.Builder builder = new Preview.Builder();
        setTargetOutputSize(builder, mPreviewTargetSize);
        mPreview = builder.build();
    }

    // ----------------------
    // ImageCapture UseCase.
    // ----------------------

    /**
     * Checks if {@link ImageCapture} is enabled.
     *
     * <p> {@link ImageCapture} is enabled by default. It has to be enabled before
     * {@link #takePicture} can be called.
     *
     * @see ImageCapture
     */
    @MainThread
    public boolean isImageCaptureEnabled() {
        checkMainThread();
        return isUseCaseEnabled(IMAGE_CAPTURE);
    }

    /**
     * Gets the flash mode for {@link ImageCapture}.
     *
     * @return the flashMode. Value is {@link ImageCapture#FLASH_MODE_AUTO},
     * {@link ImageCapture#FLASH_MODE_ON}, or {@link ImageCapture#FLASH_MODE_OFF}.
     * @see ImageCapture
     */
    @MainThread
    @ImageCapture.FlashMode
    public int getImageCaptureFlashMode() {
        checkMainThread();
        return mImageCapture.getFlashMode();
    }

    /**
     * Sets the flash mode for {@link ImageCapture}.
     *
     * <p>If not set, the flash mode will default to {@link ImageCapture#FLASH_MODE_OFF}.
     *
     * @param flashMode the flash mode for {@link ImageCapture}.
     */
    @MainThread
    public void setImageCaptureFlashMode(@ImageCapture.FlashMode int flashMode) {
        checkMainThread();
        mImageCapture.setFlashMode(flashMode);
    }

    /**
     * Captures a new still image and saves to a file along with application specified metadata.
     *
     * <p> The callback will be called only once for every invocation of this method.
     *
     * <p> By default, the saved image is mirrored to match the output of the preview if front
     * camera is used. To override this behavior, the app needs to explicitly set the flag to
     * {@code false} using {@link ImageCapture.Metadata#setReversedHorizontal} and
     * {@link ImageCapture.OutputFileOptions.Builder#setMetadata}.
     *
     * @param outputFileOptions  Options to store the newly captured image.
     * @param executor           The executor in which the callback methods will be run.
     * @param imageSavedCallback Callback to be called for the newly captured image.
     * @see ImageCapture#takePicture(
     *ImageCapture.OutputFileOptions, Executor, ImageCapture.OnImageSavedCallback)
     */
    @MainThread
    public void takePicture(
            @NonNull ImageCapture.OutputFileOptions outputFileOptions,
            @NonNull Executor executor,
            @NonNull ImageCapture.OnImageSavedCallback imageSavedCallback) {
        checkMainThread();
        Preconditions.checkState(isCameraInitialized(), CAMERA_NOT_INITIALIZED);
        Preconditions.checkState(isImageCaptureEnabled(), IMAGE_CAPTURE_DISABLED);

        updateMirroringFlagInOutputFileOptions(outputFileOptions);
        mImageCapture.takePicture(outputFileOptions, executor, imageSavedCallback);
    }

    /**
     * Update {@link ImageCapture.OutputFileOptions} based on config.
     *
     * <p> Mirror the output image if front camera is used and if the flag is not set explicitly by
     * the app.
     *
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void updateMirroringFlagInOutputFileOptions(
            @NonNull ImageCapture.OutputFileOptions outputFileOptions) {
        if (mCameraSelector.getLensFacing() != null
                && !outputFileOptions.getMetadata().isReversedHorizontalSet()) {
            outputFileOptions.getMetadata().setReversedHorizontal(
                    mCameraSelector.getLensFacing() == CameraSelector.LENS_FACING_FRONT);
        }
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
            @NonNull Executor executor,
            @NonNull ImageCapture.OnImageCapturedCallback callback) {
        checkMainThread();
        Preconditions.checkState(isCameraInitialized(), CAMERA_NOT_INITIALIZED);
        Preconditions.checkState(isImageCaptureEnabled(), IMAGE_CAPTURE_DISABLED);

        mImageCapture.takePicture(executor, callback);
    }

    /**
     * Sets the image capture mode.
     *
     * <p>Valid capture modes are {@link ImageCapture.CaptureMode#CAPTURE_MODE_MINIMIZE_LATENCY},
     * which prioritizes latency over image quality, or
     * {@link ImageCapture.CaptureMode#CAPTURE_MODE_MAXIMIZE_QUALITY},
     * which prioritizes image quality over latency.
     *
     * <p> Changing the value will reconfigure the camera which will cause additional latency.
     * To avoid this, set the value before controller is bound to lifecycle.
     *
     * @param captureMode the requested image capture mode.
     */
    @MainThread
    public void setImageCaptureMode(@ImageCapture.CaptureMode int captureMode) {
        checkMainThread();
        if (mImageCapture.getCaptureMode() == captureMode) {
            return;
        }
        unbindImageCaptureAndRecreate(captureMode);
        startCameraAndTrackStates();
    }

    /**
     * Returns the image capture mode.
     *
     * @see ImageCapture#getCaptureMode()
     */
    @MainThread
    public int getImageCaptureMode() {
        checkMainThread();
        return mImageCapture.getCaptureMode();
    }

    /**
     * Sets the intended image size for {@link ImageCapture}.
     *
     * <p> The value is used as a hint when determining the resolution and aspect ratio of
     * the captured image. The actual output may differ from the requested value due to device
     * constraints.
     *
     * <p> When set to null, the output will be based on the default config of {@link ImageCapture}.
     *
     * <p> Changing the value will reconfigure the camera which will cause additional latency.
     * To avoid this, set the value before controller is bound to lifecycle.
     *
     * @param targetSize the intended image size for {@link ImageCapture}.
     */
    @MainThread
    public void setImageCaptureTargetSize(@Nullable OutputSize targetSize) {
        checkMainThread();
        if (isOutputSizeEqual(mImageCaptureTargetSize, targetSize)) {
            return;
        }
        mImageCaptureTargetSize = targetSize;
        unbindImageCaptureAndRecreate(getImageCaptureMode());
        startCameraAndTrackStates();
    }

    /**
     * Returns the intended output size for {@link ImageCapture} set by
     * {@link #setImageCaptureTargetSize(OutputSize)}, or null if not set.
     */
    @MainThread
    @Nullable
    public OutputSize getImageCaptureTargetSize() {
        checkMainThread();
        return mImageCaptureTargetSize;
    }

    /**
     * Sets the default executor that will be used for {@link ImageCapture} IO tasks.
     *
     * <p> This executor will be used for any IO tasks specifically for {@link ImageCapture},
     * such as {@link #takePicture(ImageCapture.OutputFileOptions, Executor,
     * ImageCapture.OnImageSavedCallback)}. If no executor is set, then a default Executor
     * specifically for IO will be used instead.
     *
     * <p> Changing the value will reconfigure the camera which will cause additional latency.
     * To avoid this, set the value before controller is bound to lifecycle.
     *
     * @param executor The executor which will be used for IO tasks.
     */
    @MainThread
    public void setImageCaptureIoExecutor(@Nullable Executor executor) {
        checkMainThread();
        if (mImageCaptureIoExecutor == executor) {
            return;
        }
        mImageCaptureIoExecutor = executor;
        unbindImageCaptureAndRecreate(mImageCapture.getCaptureMode());
        startCameraAndTrackStates();
    }

    /**
     * Gets the default executor for {@link ImageCapture} IO tasks.
     */
    @MainThread
    @Nullable
    public Executor getImageCaptureIoExecutor() {
        checkMainThread();
        return mImageCaptureIoExecutor;
    }

    /**
     * Unbinds {@link ImageCapture} and recreates with the latest parameters.
     */
    private void unbindImageCaptureAndRecreate(int imageCaptureMode) {
        if (isCameraInitialized()) {
            mCameraProvider.unbind(mImageCapture);
        }
        ImageCapture.Builder builder = new ImageCapture.Builder().setCaptureMode(imageCaptureMode);
        setTargetOutputSize(builder, mImageCaptureTargetSize);
        if (mImageCaptureIoExecutor != null) {
            builder.setIoExecutor(mImageCaptureIoExecutor);
        }
        mImageCapture = builder.build();
    }

    // -----------------
    // Image analysis
    // -----------------

    /**
     * Checks if {@link ImageAnalysis} is enabled.
     *
     * @see ImageAnalysis
     */
    @MainThread
    public boolean isImageAnalysisEnabled() {
        checkMainThread();
        return isUseCaseEnabled(IMAGE_ANALYSIS);
    }

    /**
     * Sets an analyzer to receive and analyze images.
     *
     * <p>Applications can process or copy the image by implementing the
     * {@link ImageAnalysis.Analyzer}. The image needs to be closed by calling
     * {@link ImageProxy#close()} when the analyzing is done.
     *
     * <p>Setting an analyzer function replaces any previous analyzer. Only one analyzer can be
     * set at any time.
     *
     * <p> If the {@link ImageAnalysis.Analyzer#getDefaultTargetResolution()} returns a non-null
     * value, calling this method will reconfigure the camera which might cause additional
     * latency. To avoid this, set the value before controller is bound to the lifecycle.
     *
     * @param executor The executor in which the
     *                 {@link ImageAnalysis.Analyzer#analyze(ImageProxy)} will be run.
     * @param analyzer of the images.
     * @see ImageAnalysis#setAnalyzer(Executor, ImageAnalysis.Analyzer)
     */
    @MainThread
    public void setImageAnalysisAnalyzer(@NonNull Executor executor,
            @NonNull ImageAnalysis.Analyzer analyzer) {
        checkMainThread();
        if (mAnalysisAnalyzer == analyzer && mAnalysisExecutor == executor) {
            return;
        }
        ImageAnalysis.Analyzer oldAnalyzer = mAnalysisAnalyzer;
        mAnalysisExecutor = executor;
        mAnalysisAnalyzer = analyzer;
        mImageAnalysis.setAnalyzer(executor, analyzer);
        restartCameraIfAnalyzerResolutionChanged(oldAnalyzer, analyzer);
    }

    /**
     * Removes a previously set analyzer.
     *
     * <p>This will stop data from streaming to the {@link ImageAnalysis}.
     *
     * <p> If the current {@link ImageAnalysis.Analyzer#getDefaultTargetResolution()} returns
     * non-null value, calling this method will reconfigure the camera which might cause additional
     * latency. To avoid this, call this method when the lifecycle is not active.
     *
     * @see ImageAnalysis#clearAnalyzer().
     */
    @MainThread
    public void clearImageAnalysisAnalyzer() {
        checkMainThread();
        ImageAnalysis.Analyzer oldAnalyzer = mAnalysisAnalyzer;
        mAnalysisExecutor = null;
        mAnalysisAnalyzer = null;
        mImageAnalysis.clearAnalyzer();
        restartCameraIfAnalyzerResolutionChanged(oldAnalyzer, null);
    }

    private void restartCameraIfAnalyzerResolutionChanged(
            @Nullable ImageAnalysis.Analyzer oldAnalyzer,
            @Nullable ImageAnalysis.Analyzer newAnalyzer) {
        Size oldResolution = oldAnalyzer == null ? null :
                oldAnalyzer.getDefaultTargetResolution();
        Size newResolution = newAnalyzer == null ? null :
                newAnalyzer.getDefaultTargetResolution();
        if (!Objects.equals(oldResolution, newResolution)) {
            // Rebind ImageAnalysis to reconfigure target resolution.
            unbindImageAnalysisAndRecreate(mImageAnalysis.getBackpressureStrategy(),
                    mImageAnalysis.getImageQueueDepth());
            startCameraAndTrackStates();
        }
    }

    /**
     * Returns the mode with which images are acquired.
     *
     * <p> If not set, it defaults to {@link ImageAnalysis#STRATEGY_KEEP_ONLY_LATEST}.
     *
     * @return The backpressure strategy applied to the image producer.
     * @see ImageAnalysis.Builder#getBackpressureStrategy()
     */
    @MainThread
    @ImageAnalysis.BackpressureStrategy
    public int getImageAnalysisBackpressureStrategy() {
        checkMainThread();
        return mImageAnalysis.getBackpressureStrategy();
    }

    /**
     * Sets the backpressure strategy to apply to the image producer to deal with scenarios
     * where images may be produced faster than they can be analyzed.
     *
     * <p>The available values are {@link ImageAnalysis#STRATEGY_BLOCK_PRODUCER} and
     * {@link ImageAnalysis#STRATEGY_KEEP_ONLY_LATEST}. If not set, the backpressure strategy
     * will default to {@link ImageAnalysis#STRATEGY_KEEP_ONLY_LATEST}.
     *
     * <p> Changing the value will reconfigure the camera which will cause additional latency. To
     * avoid this, set the value before controller is bound to lifecycle.
     *
     * @param strategy The strategy to use.
     * @see ImageAnalysis.Builder#setBackpressureStrategy(int)
     */
    @MainThread
    public void setImageAnalysisBackpressureStrategy(
            @ImageAnalysis.BackpressureStrategy int strategy) {
        checkMainThread();
        if (mImageAnalysis.getBackpressureStrategy() == strategy) {
            return;
        }

        unbindImageAnalysisAndRecreate(strategy, mImageAnalysis.getImageQueueDepth());
        startCameraAndTrackStates();
    }

    /**
     * Sets the image queue depth of {@link ImageAnalysis}.
     *
     * <p> This sets the number of images available in parallel to {@link ImageAnalysis.Analyzer}
     * . The value is only used if the backpressure strategy is
     * {@link ImageAnalysis.BackpressureStrategy#STRATEGY_BLOCK_PRODUCER}.
     *
     * <p> Changing the value will reconfigure the camera which will cause additional latency. To
     * avoid this, set the value before controller is bound to lifecycle.
     *
     * @param depth The total number of images available.
     * @see ImageAnalysis.Builder#setImageQueueDepth(int)
     */
    @MainThread
    public void setImageAnalysisImageQueueDepth(int depth) {
        checkMainThread();
        if (mImageAnalysis.getImageQueueDepth() == depth) {
            return;
        }
        unbindImageAnalysisAndRecreate(mImageAnalysis.getBackpressureStrategy(), depth);
        startCameraAndTrackStates();
    }

    /**
     * Gets the image queue depth of {@link ImageAnalysis}.
     *
     * @see ImageAnalysis#getImageQueueDepth()
     */
    @MainThread
    public int getImageAnalysisImageQueueDepth() {
        checkMainThread();
        return mImageAnalysis.getImageQueueDepth();
    }

    /**
     * Sets the intended output size for {@link ImageAnalysis}.
     *
     * <p> The value is used as a hint when determining the resolution and aspect ratio of
     * the output buffer. The actual output may differ from the requested value due to device
     * constraints.
     *
     * <p> When set to null, the output will be based on the default config of
     * {@link ImageAnalysis}.
     *
     * <p> Changing the value will reconfigure the camera which will cause additional latency.
     * To avoid this, set the value before controller is bound to lifecycle.
     *
     * @param targetSize the intended output size for {@link ImageAnalysis}.
     * @see ImageAnalysis.Builder#setTargetAspectRatio(int)
     * @see ImageAnalysis.Builder#setTargetResolution(Size)
     */
    @MainThread
    public void setImageAnalysisTargetSize(@Nullable OutputSize targetSize) {
        checkMainThread();
        if (isOutputSizeEqual(mImageAnalysisTargetSize, targetSize)) {
            return;
        }
        mImageAnalysisTargetSize = targetSize;
        unbindImageAnalysisAndRecreate(
                mImageAnalysis.getBackpressureStrategy(),
                mImageAnalysis.getImageQueueDepth());
        startCameraAndTrackStates();
    }

    /**
     * Returns the intended output size for {@link ImageAnalysis} set by
     * {@link #setImageAnalysisTargetSize(OutputSize)}, or null if not set.
     */
    @MainThread
    @Nullable
    public OutputSize getImageAnalysisTargetSize() {
        checkMainThread();
        return mImageAnalysisTargetSize;
    }

    /**
     * Sets the executor that will be used for {@link ImageAnalysis} background tasks.
     *
     * <p>If not set, the background executor will default to an automatically generated
     * {@link Executor}.
     *
     * <p> Changing the value will reconfigure the camera, which will cause additional latency. To
     * avoid this, set the value before controller is bound to lifecycle.
     *
     * @param executor the executor for {@link ImageAnalysis} background tasks.
     * @see ImageAnalysis.Builder#setBackgroundExecutor(Executor)
     */
    @MainThread
    public void setImageAnalysisBackgroundExecutor(@Nullable Executor executor) {
        checkMainThread();
        if (mAnalysisBackgroundExecutor == executor) {
            return;
        }
        mAnalysisBackgroundExecutor = executor;
        unbindImageAnalysisAndRecreate(mImageAnalysis.getBackpressureStrategy(),
                mImageAnalysis.getImageQueueDepth());
        startCameraAndTrackStates();
    }

    /**
     * Gets the default executor for {@link ImageAnalysis} background tasks.
     *
     * @see ImageAnalysis.Builder#setBackgroundExecutor(Executor)
     */
    @MainThread
    @Nullable
    public Executor getImageAnalysisBackgroundExecutor() {
        checkMainThread();
        return mAnalysisBackgroundExecutor;
    }

    /**
     * Unbinds {@link ImageAnalysis} and recreates with the latest parameters.
     */
    @MainThread
    private void unbindImageAnalysisAndRecreate(int strategy, int imageQueueDepth) {
        checkMainThread();
        if (isCameraInitialized()) {
            mCameraProvider.unbind(mImageAnalysis);
        }
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder()
                .setBackpressureStrategy(strategy)
                .setImageQueueDepth(imageQueueDepth);
        setTargetOutputSize(builder, mImageAnalysisTargetSize);
        if (mAnalysisBackgroundExecutor != null) {
            builder.setBackgroundExecutor(mAnalysisBackgroundExecutor);
        }
        mImageAnalysis = builder.build();
        if (mAnalysisExecutor != null && mAnalysisAnalyzer != null) {
            mImageAnalysis.setAnalyzer(mAnalysisExecutor, mAnalysisAnalyzer);
        }
    }

    @OptIn(markerClass = {TransformExperimental.class})
    @MainThread
    void updatePreviewViewTransform(@Nullable OutputTransform outputTransform) {
        checkMainThread();
        if (mAnalysisAnalyzer == null) {
            return;
        }
        if (outputTransform == null) {
            mAnalysisAnalyzer.updateTransform(null);
        } else if (mAnalysisAnalyzer.getTargetCoordinateSystem()
                == COORDINATE_SYSTEM_VIEW_REFERENCED) {
            mAnalysisAnalyzer.updateTransform(outputTransform.getMatrix());
        }
    }

    // -----------------
    // Video capture
    // -----------------

    /**
     * Checks if video capture is enabled.
     *
     * <p> Video capture is disabled by default. It has to be enabled before
     * {@link #startRecording} can be called.
     */
    @ExperimentalVideo
    @MainThread
    public boolean isVideoCaptureEnabled() {
        checkMainThread();
        return isUseCaseEnabled(VIDEO_CAPTURE);
    }

    /**
     * Takes a video to a given file.
     *
     * <p> Only a single recording can be active at a time, so if {@link #isRecording()} is true,
     * this will throw an {@link IllegalStateException}.
     *
     * <p> Upon successfully starting the recording, a {@link VideoRecordEvent.Start} event will
     * be the first event sent to the provided event listener.
     *
     * <p> If errors occur while starting the recording, a {@link VideoRecordEvent.Finalize} event
     * will be the first event sent to the provided listener, and information about the error can
     * be found in that event's {@link VideoRecordEvent.Finalize#getError()} method.
     *
     * <p> Recording with audio requires the {@link android.Manifest.permission#RECORD_AUDIO}
     * permission; without it, starting a recording will fail with a {@link SecurityException}.
     *
     * @param outputOptions the options to store the newly captured video.
     * @param audioConfig the configuration of audio.
     * @param executor the executor that the event listener will be run on.
     * @param listener the event listener to handle video record events.
     * @return a {@link Recording} that provides controls for new active recordings.
     * @throws IllegalStateException if there is an unfinished active recording.
     * @throws SecurityException if the audio config specifies audio should be enabled but the
     * {@link android.Manifest.permission#RECORD_AUDIO} permission is denied.
     */
    @SuppressLint("MissingPermission")
    @ExperimentalVideo
    @MainThread
    @NonNull
    public Recording startRecording(
            @NonNull FileOutputOptions outputOptions,
            @NonNull AudioConfig audioConfig,
            @NonNull Executor executor,
            @NonNull Consumer<VideoRecordEvent> listener) {
        return startRecordingInternal(outputOptions, audioConfig, executor, listener);
    }

    /**
     * Takes a video to a given file descriptor.
     *
     * <p> Currently, file descriptors as output destinations are not supported on pre-Android O
     * (API 26) devices.
     *
     * <p> Only a single recording can be active at a time, so if {@link #isRecording()} is true,
     * this will throw an {@link IllegalStateException}.
     *
     * <p> Upon successfully starting the recording, a {@link VideoRecordEvent.Start} event will
     * be the first event sent to the provided event listener.
     *
     * <p> If errors occur while starting the recording, a {@link VideoRecordEvent.Finalize} event
     * will be the first event sent to the provided listener, and information about the error can
     * be found in that event's {@link VideoRecordEvent.Finalize#getError()} method.
     *
     * <p> Recording with audio requires the {@link android.Manifest.permission#RECORD_AUDIO}
     * permission; without it, starting a recording will fail with a {@link SecurityException}.
     *
     * @param outputOptions the options to store the newly captured video.
     * @param audioConfig the configuration of audio.
     * @param executor the executor that the event listener will be run on.
     * @param listener the event listener to handle video record events.
     * @return a {@link Recording} that provides controls for new active recordings.
     * @throws IllegalStateException if there is an unfinished active recording.
     * @throws SecurityException if the audio config specifies audio should be enabled but the
     * {@link android.Manifest.permission#RECORD_AUDIO} permission is denied.
     */
    @SuppressLint("MissingPermission")
    @ExperimentalVideo
    @RequiresApi(26)
    @MainThread
    @NonNull
    public Recording startRecording(
            @NonNull FileDescriptorOutputOptions outputOptions,
            @NonNull AudioConfig audioConfig,
            @NonNull Executor executor,
            @NonNull Consumer<VideoRecordEvent> listener) {
        return startRecordingInternal(outputOptions, audioConfig, executor, listener);
    }

    /**
     * Takes a video to MediaStore.
     *
     * <p> Only a single recording can be active at a time, so if {@link #isRecording()} is true,
     * this will throw an {@link IllegalStateException}.
     *
     * <p> Upon successfully starting the recording, a {@link VideoRecordEvent.Start} event will
     * be the first event sent to the provided event listener.
     *
     * <p> If errors occur while starting the recording, a {@link VideoRecordEvent.Finalize} event
     * will be the first event sent to the provided listener, and information about the error can
     * be found in that event's {@link VideoRecordEvent.Finalize#getError()} method.
     *
     * <p> Recording with audio requires the {@link android.Manifest.permission#RECORD_AUDIO}
     * permission; without it, starting a recording will fail with a {@link SecurityException}.
     *
     * @param outputOptions the options to store the newly captured video.
     * @param audioConfig the configuration of audio.
     * @param executor the executor that the event listener will be run on.
     * @param listener the event listener to handle video record events.
     * @return a {@link Recording} that provides controls for new active recordings.
     * @throws IllegalStateException if there is an unfinished active recording.
     * @throws SecurityException if the audio config specifies audio should be enabled but the
     * {@link android.Manifest.permission#RECORD_AUDIO} permission is denied.
     */
    @SuppressLint("MissingPermission")
    @ExperimentalVideo
    @MainThread
    @NonNull
    public Recording startRecording(
            @NonNull MediaStoreOutputOptions outputOptions,
            @NonNull AudioConfig audioConfig,
            @NonNull Executor executor,
            @NonNull Consumer<VideoRecordEvent> listener) {
        return startRecordingInternal(outputOptions, audioConfig, executor, listener);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @ExperimentalVideo
    @MainThread
    private Recording startRecordingInternal(
            @NonNull OutputOptions outputOptions,
            @NonNull AudioConfig audioConfig,
            @NonNull Executor executor,
            @NonNull Consumer<VideoRecordEvent> listener) {
        checkMainThread();
        Preconditions.checkState(isCameraInitialized(), CAMERA_NOT_INITIALIZED);
        Preconditions.checkState(isVideoCaptureEnabled(), VIDEO_CAPTURE_DISABLED);
        Preconditions.checkState(!isRecording(), VIDEO_RECORDING_UNFINISHED);

        Consumer<VideoRecordEvent> wrappedListener =
                wrapListenerToDeactivateRecordingOnFinalized(listener);
        PendingRecording pendingRecording = prepareRecording(outputOptions);
        boolean isAudioEnabled = audioConfig.getAudioEnabled();
        if (isAudioEnabled) {
            checkAudioPermissionGranted();
            pendingRecording.withAudioEnabled();
        }
        Recording recording = pendingRecording.start(executor, wrappedListener);
        setActiveRecording(recording, wrappedListener);

        return recording;
    }

    private void checkAudioPermissionGranted() {
        int permissionState = PermissionChecker.checkSelfPermission(mAppContext,
                Manifest.permission.RECORD_AUDIO);
        if (permissionState == PermissionChecker.PERMISSION_DENIED) {
            throw new SecurityException("Attempted to start recording with audio, but "
                    + "application does not have RECORD_AUDIO permission granted.");
        }
    }

    /**
     * Generates a {@link PendingRecording} instance for starting a recording.
     *
     * <p> This method handles {@code prepareRecording()} methods for different output formats,
     * and makes {@link #startRecordingInternal(OutputOptions, AudioConfig, Executor, Consumer)}
     * only handle the general flow.
     *
     * <p> This method uses the parent class {@link OutputOptions} as the parameter. On the other
     * hand, the public {@code startRecording()} is overloaded with subclasses. The reason is to
     * enforce compile-time check for API levels.
     */
    @ExperimentalVideo
    @MainThread
    private PendingRecording prepareRecording(@NonNull OutputOptions options) {
        Recorder recorder = mVideoCapture.getOutput();
        if (options instanceof FileOutputOptions) {
            return recorder.prepareRecording(mAppContext, (FileOutputOptions) options);
        } else if (options instanceof FileDescriptorOutputOptions) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                throw new UnsupportedOperationException(
                        "File descriptors are not supported on pre-Android O (API 26) devices."
                );
            }
            return recorder.prepareRecording(mAppContext, (FileDescriptorOutputOptions) options);
        } else if (options instanceof MediaStoreOutputOptions) {
            return recorder.prepareRecording(mAppContext, (MediaStoreOutputOptions) options);
        } else {
            throw new IllegalArgumentException("Unsupported OutputOptions type.");
        }
    }

    @ExperimentalVideo
    private Consumer<VideoRecordEvent> wrapListenerToDeactivateRecordingOnFinalized(
            @NonNull final Consumer<VideoRecordEvent> listener) {
        final Executor mainExecutor = getMainExecutor(mAppContext);

        return new Consumer<VideoRecordEvent>() {
            @Override
            public void accept(VideoRecordEvent videoRecordEvent) {
                if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                    if (!Threads.isMainThread()) {
                        // Post on main thread to ensure thread safety.
                        mainExecutor.execute(() -> deactivateRecordingByListener(this));
                    } else {
                        deactivateRecordingByListener(this);
                    }
                }
                listener.accept(videoRecordEvent);
            }
        };
    }

    @ExperimentalVideo
    @MainThread
    void deactivateRecordingByListener(@NonNull Consumer<VideoRecordEvent> listener) {
        Recording recording = mRecordingMap.remove(listener);
        if (recording != null) {
            deactivateRecording(recording);
        }
    }

    /**
     * Clears the active video recording reference if the recording to be deactivated matches.
     */
    @ExperimentalVideo
    @MainThread
    private void deactivateRecording(@NonNull Recording recording) {
        if (mActiveRecording == recording) {
            mActiveRecording = null;
        }
    }

    @ExperimentalVideo
    @MainThread
    private void setActiveRecording(
            @NonNull Recording recording,
            @NonNull Consumer<VideoRecordEvent> listener) {
        mRecordingMap.put(listener, recording);
        mActiveRecording = recording;
    }

    /**
     * Stops an in-progress video recording.
     *
     * <p> Once the current recording has been stopped, the next recording can be started.
     *
     * <p> If the recording completes successfully, a {@link VideoRecordEvent.Finalize} event with
     * {@link VideoRecordEvent.Finalize#ERROR_NONE} will be sent to the provided listener.
     */
    @ExperimentalVideo
    @MainThread
    private void stopRecording() {
        checkMainThread();

        if (mActiveRecording != null) {
            mActiveRecording.stop();
            deactivateRecording(mActiveRecording);
        }
    }

    /**
     * Returns whether there is an in-progress video recording.
     */
    @ExperimentalVideo
    @MainThread
    public boolean isRecording() {
        checkMainThread();
        return mActiveRecording != null && !mActiveRecording.isClosed();
    }

    /**
     * Sets the intended video quality for {@code VideoCapture}.
     *
     * <p> The value is used as a hint when determining the resolution of the video.
     * The actual output may differ from the requested value due to device constraints.
     * The {@link FallbackStrategy#lowerQualityOrHigherThan(Quality)} fallback strategy
     * will be applied when the quality is not supported.
     *
     * <p> When set to null, the output will be based on the default config of {@link
     * Recorder#DEFAULT_QUALITY_SELECTOR}.
     *
     * <p> Changing the value will reconfigure the camera which will cause video
     * capture to stop. To avoid this, set the value before controller is bound to
     * lifecycle.
     *
     * @param targetQuality the intended video quality for {@code VideoCapture}.
     */
    @ExperimentalVideo
    @MainThread
    public void setVideoCaptureTargetQuality(@Nullable Quality targetQuality) {
        checkMainThread();
        if (targetQuality == mVideoCaptureQuality) {
            return;
        }
        mVideoCaptureQuality = targetQuality;
        startCameraAndTrackStates();
    }

    /**
     * Returns the intended quality for {@code VideoCapture} set by
     * {@link #setVideoCaptureTargetQuality(Quality)}, or null if not set.
     */
    @ExperimentalVideo
    @MainThread
    @Nullable
    public Quality getVideoCaptureTargetQuality() {
        checkMainThread();
        return mVideoCaptureQuality;
    }

    private VideoCapture<Recorder> createNewVideoCapture() {
        return VideoCapture.withOutput(generateVideoCaptureRecorder(mVideoCaptureQuality));
    }

    // -----------------
    // Camera control
    // -----------------

    /**
     * Sets the {@link CameraSelector}.
     *
     * <p> Calling this method with a {@link CameraSelector} that resolves to a different camera
     * will change the camera being used by the controller. If camera initialization is complete,
     * the controller will immediately rebind use cases with the new {@link CameraSelector};
     * otherwise, the new {@link CameraSelector} will be used when the camera becomes ready.
     *
     * <p>The default value is {@link CameraSelector#DEFAULT_BACK_CAMERA}.
     *
     * @throws IllegalStateException If the provided camera selector is unable to resolve a
     *                               camera to be used for the enabled use cases.
     * @see CameraSelector
     */
    @MainThread
    public void setCameraSelector(@NonNull CameraSelector cameraSelector) {
        checkMainThread();
        if (mCameraSelector == cameraSelector) {
            return;
        }

        CameraSelector oldCameraSelector = mCameraSelector;
        mCameraSelector = cameraSelector;

        if (mCameraProvider == null) {
            return;
        }
        mCameraProvider.unbind(mPreview, mImageCapture, mImageAnalysis, mVideoCapture);
        startCameraAndTrackStates(() -> mCameraSelector = oldCameraSelector);
    }

    /**
     * Checks if the given {@link CameraSelector} can be resolved to a camera.
     *
     * <p> Use this method to check if the device has the given camera.
     *
     * <p> Only call this method after camera is initialized. e.g. after the
     * {@link ListenableFuture} from {@link #getInitializationFuture()} is finished. Calling it
     * prematurely throws {@link IllegalStateException}. Example:
     *
     * <pre><code>
     * controller.getInitializationFuture().addListener(() -> {
     *     if (controller.hasCamera(cameraSelector)) {
     *         controller.setCameraSelector(cameraSelector);
     *     } else {
     *         // Update UI if the camera is not available.
     *     }
     *     // Attach PreviewView after we know the camera is available.
     *     previewView.setController(controller);
     * }, ContextCompat.getMainExecutor(requireContext()));
     * </code></pre>
     *
     * @return true if the {@link CameraSelector} can be resolved to a camera.
     * @throws IllegalStateException if the camera is not initialized.
     */
    @MainThread
    public boolean hasCamera(@NonNull CameraSelector cameraSelector) {
        checkMainThread();
        Preconditions.checkNotNull(cameraSelector);

        if (mCameraProvider == null) {
            throw new IllegalStateException("Camera not initialized. Please wait for "
                    + "the initialization future to finish. See #getInitializationFuture().");
        }

        try {
            return mCameraProvider.hasCamera(cameraSelector);
        } catch (CameraInfoUnavailableException e) {
            Logger.w(TAG, "Failed to check camera availability", e);
            return false;
        }
    }

    /**
     * Gets the {@link CameraSelector}.
     *
     * <p>The default value is{@link CameraSelector#DEFAULT_BACK_CAMERA}.
     *
     * @see CameraSelector
     */
    @NonNull
    @MainThread
    public CameraSelector getCameraSelector() {
        checkMainThread();
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
        checkMainThread();
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
        checkMainThread();
        mPinchToZoomEnabled = enabled;
    }

    /**
     * Called by {@link PreviewView} for a pinch-to-zoom event.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    void onPinchToZoom(float pinchToZoomScale) {
        if (!isCameraAttached()) {
            Logger.w(TAG, CAMERA_NOT_ATTACHED);
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
        if (!isCameraAttached()) {
            Logger.w(TAG, CAMERA_NOT_ATTACHED);
            return;
        }
        if (!mTapToFocusEnabled) {
            Logger.d(TAG, "Tap to focus disabled. ");
            return;
        }
        Logger.d(TAG, "Tap to focus started: " + x + ", " + y);
        mTapToFocusState.postValue(TAP_TO_FOCUS_STARTED);
        MeteringPoint afPoint = meteringPointFactory.createPoint(x, y, AF_SIZE);
        MeteringPoint aePoint = meteringPointFactory.createPoint(x, y, AE_SIZE);
        FocusMeteringAction focusMeteringAction = new FocusMeteringAction
                .Builder(afPoint, FocusMeteringAction.FLAG_AF)
                .addPoint(aePoint, FocusMeteringAction.FLAG_AE)
                .build();
        Futures.addCallback(mCamera.getCameraControl().startFocusAndMetering(focusMeteringAction),
                new FutureCallback<FocusMeteringResult>() {

                    @Override
                    public void onSuccess(@Nullable FocusMeteringResult result) {
                        if (result == null) {
                            return;
                        }
                        Logger.d(TAG, "Tap to focus onSuccess: " + result.isFocusSuccessful());
                        mTapToFocusState.postValue(result.isFocusSuccessful()
                                ? TAP_TO_FOCUS_FOCUSED : TAP_TO_FOCUS_NOT_FOCUSED);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        if (t instanceof CameraControl.OperationCanceledException) {
                            Logger.d(TAG, "Tap-to-focus is canceled by new action.");
                            return;
                        }
                        Logger.d(TAG, "Tap to focus failed.", t);
                        mTapToFocusState.postValue(TAP_TO_FOCUS_FAILED);
                    }
                }, CameraXExecutors.directExecutor());
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
        checkMainThread();
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
        checkMainThread();
        mTapToFocusEnabled = enabled;
    }

    /**
     * Returns a {@link LiveData} with the latest tap-to-focus state.
     *
     * <p> When tap-to-focus feature is enabled, the {@link LiveData} will receive updates of
     * focusing states. This happens when the end user taps on {@link PreviewView}, and then again
     * when focusing is finished either successfully or unsuccessfully. The following table
     * displays the states the {@link LiveData} can be in, and the possible transitions between
     * them.
     *
     * <table>
     * <tr>
     *     <th>State</th>
     *     <th>Transition cause</th>
     *     <th>New State</th>
     * </tr>
     * <tr>
     *     <td>TAP_TO_FOCUS_NOT_STARTED</td>
     *     <td>User taps on {@link PreviewView}</td>
     *     <td>TAP_TO_FOCUS_STARTED</td>
     * </tr>
     * <tr>
     *     <td>TAP_TO_FOCUS_SUCCESSFUL</td>
     *     <td>User taps on {@link PreviewView}</td>
     *     <td>TAP_TO_FOCUS_STARTED</td>
     * </tr>
     * <tr>
     *     <td>TAP_TO_FOCUS_UNSUCCESSFUL</td>
     *     <td>User taps on {@link PreviewView}</td>
     *     <td>TAP_TO_FOCUS_STARTED</td>
     * </tr>
     * <tr>
     *     <td>TAP_TO_FOCUS_FAILED</td>
     *     <td>User taps on {@link PreviewView}</td>
     *     <td>TAP_TO_FOCUS_STARTED</td>
     * </tr>
     * <tr>
     *     <td rowspan="3">TAP_TO_FOCUS_STARTED</td>
     *     <td>Focusing succeeded</td>
     *     <td>TAP_TO_FOCUS_SUCCESSFUL</td>
     * </tr>
     * <tr>
     *     <td>Focusing failed due to lighting and/or camera distance</td>
     *     <td>TAP_TO_FOCUS_UNSUCCESSFUL</td>
     * </tr>
     * <tr>
     *     <td>Focusing failed due to device constraints</td>
     *     <td>TAP_TO_FOCUS_FAILED</td>
     * </tr>
     * </table>
     *
     * @see #setTapToFocusEnabled(boolean)
     * @see CameraControl#startFocusAndMetering(FocusMeteringAction)
     */
    @MainThread
    @NonNull
    public LiveData<Integer> getTapToFocusState() {
        checkMainThread();
        return mTapToFocusState;
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
    @NonNull
    @MainThread
    public LiveData<ZoomState> getZoomState() {
        checkMainThread();
        return mZoomState;
    }

    /**
     * Gets the {@link CameraInfo} of the currently attached camera.
     *
     * <p> For info available directly through CameraController as well as {@link CameraInfo},
     * it's recommended to use the ones with CameraController, e.g. {@link #getTorchState()} v.s.
     * {@link CameraInfo#getTorchState()}. {@link CameraInfo} is a lower-layer API and may
     * require more steps to achieve the same effect, and will not maintain values when switching
     * between cameras.
     *
     * @return the {@link CameraInfo} of the current camera. Returns null if camera is not ready.
     * @see Camera#getCameraInfo()
     */
    @Nullable
    @MainThread
    public CameraInfo getCameraInfo() {
        checkMainThread();
        return mCamera == null ? null : mCamera.getCameraInfo();
    }

    /**
     * Gets the {@link CameraControl} of the currently attached camera.
     *
     * <p> For controls available directly through CameraController as well as
     * {@link CameraControl}, it's recommended to use the ones with CameraController, e.g.
     * {@link #setLinearZoom(float)} v.s. {@link CameraControl#setLinearZoom(float)}.
     * CameraControl is a lower-layer API and may require more steps to achieve the same effect,
     * and will not maintain control values when switching between cameras.
     *
     * @return the {@link CameraControl} of the current camera. Returns null if camera is not ready.
     * @see Camera#getCameraControl()
     */
    @Nullable
    @MainThread
    public CameraControl getCameraControl() {
        checkMainThread();
        return mCamera == null ? null : mCamera.getCameraControl();
    }

    /**
     * Sets current zoom by ratio.
     *
     * <p>Valid zoom values range from {@link ZoomState#getMinZoomRatio()} to
     * {@link ZoomState#getMaxZoomRatio()}.
     *
     * <p> No-ops if the camera is not ready. The {@link ListenableFuture} completes successfully
     * in this case.
     *
     * @param zoomRatio The requested zoom ratio.
     * @return a {@link ListenableFuture} which is finished when camera is set to the given ratio.
     * It fails with {@link CameraControl.OperationCanceledException} if there is newer value
     * being set or camera is closed. If the ratio is out of range, it fails with
     * {@link IllegalArgumentException}. Cancellation of this future is a no-op.
     * @see #getZoomState()
     * @see CameraControl#setZoomRatio(float)
     */
    @NonNull
    @MainThread
    public ListenableFuture<Void> setZoomRatio(float zoomRatio) {
        checkMainThread();
        if (!isCameraAttached()) {
            Logger.w(TAG, CAMERA_NOT_ATTACHED);
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
     * <p> No-ops if the camera is not ready. The {@link ListenableFuture} completes successfully
     * in this case.
     *
     * @return a {@link ListenableFuture} which is finished when camera is set to the given ratio.
     * It fails with {@link CameraControl.OperationCanceledException} if there is newer value
     * being set or camera is closed. If the ratio is out of range, it fails with
     * {@link IllegalArgumentException}. Cancellation of this future is a no-op.
     * @see CameraControl#setLinearZoom(float)
     */
    @NonNull
    @MainThread
    public ListenableFuture<Void> setLinearZoom(@FloatRange(from = 0f, to = 1f) float linearZoom) {
        checkMainThread();
        if (!isCameraAttached()) {
            Logger.w(TAG, CAMERA_NOT_ATTACHED);
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
    @NonNull
    @MainThread
    public LiveData<Integer> getTorchState() {
        checkMainThread();
        return mTorchState;
    }

    /**
     * Enable the torch or disable the torch.
     *
     * <p> No-ops if the camera is not ready. The {@link ListenableFuture} completes successfully
     * in this case.
     *
     * @param torchEnabled true to turn on the torch, false to turn it off.
     * @return A {@link ListenableFuture} which is successful when the torch was changed to the
     * value specified. It fails when it is unable to change the torch state. Cancellation of
     * this future is a no-op.
     * @see CameraControl#enableTorch(boolean)
     */
    @NonNull
    @MainThread
    public ListenableFuture<Void> enableTorch(boolean torchEnabled) {
        checkMainThread();
        if (!isCameraAttached()) {
            Logger.w(TAG, CAMERA_NOT_ATTACHED);
            return Futures.immediateFuture(null);
        }
        return mCamera.getCameraControl().enableTorch(torchEnabled);
    }

    // ------------------------
    // Effects and extensions
    // ------------------------

    /**
     * Sets post-processing effects.
     *
     * @param effects the effects applied to camera output.
     * @hide
     * @see UseCaseGroup.Builder#addEffect
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setEffects(@NonNull List<CameraEffect> effects) {
        if (Objects.equals(mEffects, effects)) {
            // Same effect. No change needed.
            return;
        }
        if (mCameraProvider != null) {
            // Unbind to make sure the pipelines will be recreated.
            mCameraProvider.unbindAll();
        }
        mEffects = effects;
        startCameraAndTrackStates();
    }

    // ------------------------------
    // Binding to lifecycle
    // ------------------------------

    /**
     * Binds use cases, gets a new {@link Camera} instance and tracks the state of the camera.
     */
    void startCameraAndTrackStates() {
        startCameraAndTrackStates(null);
    }

    /**
     * @param restoreStateRunnable runnable to restore the controller to the previous good state if
     *                             the binding fails.
     * @throws IllegalStateException if binding fails.
     */
    void startCameraAndTrackStates(@Nullable Runnable restoreStateRunnable) {
        try {
            mCamera = startCamera();
        } catch (IllegalArgumentException exception) {
            if (restoreStateRunnable != null) {
                restoreStateRunnable.run();
            }
            // Catches the core exception and throw a more readable one.
            String errorMessage =
                    "The selected camera does not support the enabled use cases. Please "
                            + "disable use case and/or select a different camera. e.g. "
                            + "#setVideoCaptureEnabled(false)";
            throw new IllegalStateException(errorMessage, exception);
        }
        if (!isCameraAttached()) {
            Logger.d(TAG, CAMERA_NOT_ATTACHED);
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
     *
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(markerClass = {ExperimentalVideo.class})
    protected UseCaseGroup createUseCaseGroup() {
        if (!isCameraInitialized()) {
            Logger.d(TAG, CAMERA_NOT_INITIALIZED);
            return null;
        }
        if (!isPreviewViewAttached()) {
            // Preview is required. Return early if preview Surface is not ready.
            Logger.d(TAG, PREVIEW_VIEW_NOT_ATTACHED);
            return null;
        }

        UseCaseGroup.Builder builder = new UseCaseGroup.Builder().addUseCase(mPreview);

        if (isImageCaptureEnabled()) {
            builder.addUseCase(mImageCapture);
        } else {
            mCameraProvider.unbind(mImageCapture);
        }

        if (isImageAnalysisEnabled()) {
            builder.addUseCase(mImageAnalysis);
        } else {
            mCameraProvider.unbind(mImageAnalysis);
        }

        // TODO: revert aosp/2280599 to reuse VideoCapture when VideoCapture supports reuse.
        mCameraProvider.unbind(mVideoCapture);
        if (isVideoCaptureEnabled()) {
            mVideoCapture = createNewVideoCapture();
            builder.addUseCase(mVideoCapture);
        }

        builder.setViewPort(mViewPort);
        for (CameraEffect effect : mEffects) {
            builder.addEffect(effect);
        }
        return builder.build();
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
     */
    @RequiresApi(30)
    private static class Api30Impl {

        private Api30Impl() {
        }

        @DoNotInline
        @NonNull
        static Context createAttributionContext(@NonNull Context context,
                @Nullable String attributeTag) {
            return context.createAttributionContext(attributeTag);
        }

        @DoNotInline
        @Nullable
        static String getAttributionTag(@NonNull Context context) {
            return context.getAttributionTag();
        }
    }

    /**
     * Represents the output size of a {@link UseCase}.
     *
     * <p> This class is a preferred output size to be used with {@link CameraController}. The
     * preferred output size can be based on either resolution or aspect ratio, but not both.
     *
     * @see #setImageAnalysisTargetSize(OutputSize)
     * @see #setPreviewTargetSize(OutputSize)
     * @see #setImageCaptureTargetSize(OutputSize)
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class OutputSize {

        /**
         * A value that represents the aspect ratio is not assigned.
         */
        public static final int UNASSIGNED_ASPECT_RATIO = -1;

        /**
         * Possible value for {@link #getAspectRatio()}
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {UNASSIGNED_ASPECT_RATIO, AspectRatio.RATIO_4_3, AspectRatio.RATIO_16_9})
        public @interface OutputAspectRatio {
        }

        @OutputAspectRatio
        private final int mAspectRatio;

        @Nullable
        private final Size mResolution;

        /**
         * Creates a {@link OutputSize} that is based on aspect ratio.
         *
         * @see Preview.Builder#setTargetAspectRatio(int)
         * @see ImageAnalysis.Builder#setTargetAspectRatio(int)
         */
        public OutputSize(@AspectRatio.Ratio int aspectRatio) {
            Preconditions.checkArgument(aspectRatio != UNASSIGNED_ASPECT_RATIO);
            mAspectRatio = aspectRatio;
            mResolution = null;
        }

        /**
         * Creates a {@link OutputSize} that is based on resolution.
         *
         * @see Preview.Builder#setTargetResolution(Size)
         * @see ImageAnalysis.Builder#setTargetResolution(Size)
         */
        public OutputSize(@NonNull Size resolution) {
            Preconditions.checkNotNull(resolution);
            mAspectRatio = UNASSIGNED_ASPECT_RATIO;
            mResolution = resolution;
        }

        /**
         * Gets the value of aspect ratio.
         *
         * @return {@link #UNASSIGNED_ASPECT_RATIO} if the size is not based on aspect ratio.
         */
        @OutputAspectRatio
        public int getAspectRatio() {
            return mAspectRatio;
        }

        /**
         * Gets the value of resolution.
         *
         * @return null if the size is not based on resolution.
         */
        @Nullable
        public Size getResolution() {
            return mResolution;
        }

        @NonNull
        @Override
        public String toString() {
            return "aspect ratio: " + mAspectRatio + " resolution: " + mResolution;
        }
    }
}
