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

package androidx.camera.camera2.internal;

import static androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.core.ImageCapture.FLASH_MODE_ON;

import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A Camera2 implementation for CameraControlInternal interface
 */
final class Camera2CameraControl implements CameraControlInternal {
    private static final String TAG = "Camera2CameraControl";
    @VisibleForTesting
    final CameraControlSessionCallback mSessionCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @CameraExecutor
    final Executor mExecutor;
    private final CameraCharacteristics mCameraCharacteristics;
    private final ControlUpdateCallback mControlUpdateCallback;
    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    volatile Rational mPreviewAspectRatio = null;
    private final FocusMeteringControl mFocusMeteringControl;
    private final ZoomControl mZoomControl;
    private final TorchControl mTorchControl;
    private final AeFpsRange mAeFpsRange;
    // use volatile modifier to make these variables in sync in all threads.
    private volatile boolean mIsTorchOn = false;
    @ImageCapture.FlashMode
    private volatile int mFlashMode = FLASH_MODE_OFF;

    //******************** Should only be accessed by executor *****************************//
    private Rect mCropRect = null;
    private final CameraCaptureCallbackSet mCameraCaptureCallbackSet =
            new CameraCaptureCallbackSet();
    //**************************************************************************************//

    /**
     * Constructor for a Camera2CameraControl.
     *
     * <p>All {@code controlUpdateListener} invocations will be on the provided {@code executor}.
     *
     * <p>All tasks scheduled by {@code scheduler} will be immediately executed by {@code executor}.
     *
     * @param cameraCharacteristics Characteristics for the camera being controlled.
     * @param scheduler             Scheduler used for scheduling tasks in the future.
     * @param executor              Camera executor for synchronizing and offloading all commands.
     * @param controlUpdateCallback Listener which will be notified of control changes.
     */
    Camera2CameraControl(@NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull ScheduledExecutorService scheduler,
            @NonNull @CameraExecutor Executor executor,
            @NonNull ControlUpdateCallback controlUpdateCallback) {
        mCameraCharacteristics = cameraCharacteristics;
        mControlUpdateCallback = controlUpdateCallback;
        mExecutor = executor;
        mSessionCallback = new CameraControlSessionCallback(mExecutor);
        mSessionConfigBuilder.setTemplateType(getDefaultTemplate());
        mSessionConfigBuilder.addRepeatingCameraCaptureCallback(
                CaptureCallbackContainer.create(mSessionCallback));
        // Adding a callback via SessionConfigBuilder requires a expensive updateSessionConfig
        // call. mCameraCaptureCallbackset is for enabling dynamically add/remove
        // CameraCaptureCallback efficiently.
        mSessionConfigBuilder.addRepeatingCameraCaptureCallback(mCameraCaptureCallbackSet);

        mFocusMeteringControl = new FocusMeteringControl(this, scheduler, mExecutor);
        mZoomControl = new ZoomControl(this, mCameraCharacteristics);
        mTorchControl = new TorchControl(this, mCameraCharacteristics);
        mAeFpsRange = new AeFpsRange(mCameraCharacteristics);

        // Initialize the session config
        mExecutor.execute(this::updateSessionConfig);
    }

    @NonNull
    public ZoomControl getZoomControl() {
        return mZoomControl;
    }

    @NonNull
    public TorchControl getTorchControl() {
        return mTorchControl;
    }

    /**
     * Set current active state. Set active if it is ready to trigger camera control operation.
     *
     * <p>Most operations during inactive state do nothing. Some states are reset to default
     * once it is changed to inactive state.
     */
    void setActive(boolean isActive) {
        mFocusMeteringControl.setActive(isActive);
        mZoomControl.setActive(isActive);
        mTorchControl.setActive(isActive);
    }

    @WorkerThread
    public void setPreviewAspectRatio(@Nullable Rational previewAspectRatio) {
        mPreviewAspectRatio = previewAspectRatio;
    }

    /**
     * Sets a {@link CaptureRequest.Builder} to get the default capture request parameters in order
     * to compare the 3A regions in CaptureResult in FocusMeteringControl.
     */
    public void setDefaultRequestBuilder(@NonNull CaptureRequest.Builder builder) {
        mFocusMeteringControl.setDefaultRequestBuilder(builder);
    }

    @NonNull
    @Override
    public ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        return mFocusMeteringControl.startFocusAndMetering(action, mPreviewAspectRatio);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelFocusAndMetering() {
        return mFocusMeteringControl.cancelFocusAndMetering();
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setZoomRatio(float ratio) {
        return mZoomControl.setZoomRatio(ratio);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        return mZoomControl.setLinearZoom(linearZoom);
    }

    /** {@inheritDoc} */
    @Override
    public void setCropRegion(@Nullable final Rect crop) {
        mExecutor.execute(() -> setCropRegionInternal(crop));
    }

    @ImageCapture.FlashMode
    @Override
    public int getFlashMode() {
        return mFlashMode;
    }

    /** {@inheritDoc} */
    @Override
    public void setFlashMode(@ImageCapture.FlashMode int flashMode) {
        // update mFlashMode immediately so that following getFlashMode() returns correct value.
        mFlashMode = flashMode;

        mExecutor.execute(this::updateSessionConfig);
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public ListenableFuture<Void> enableTorch(final boolean torch) {
        return mTorchControl.enableTorch(torch);
    }

    /**
     * Issues a {@link CaptureRequest#CONTROL_AF_TRIGGER_START} request to start auto focus scan.
     *
     * @return a {@link ListenableFuture} which completes when the request is completed.
     * Cancelling the ListenableFuture is a no-op.
     */
    @Override
    @NonNull
    public ListenableFuture<CameraCaptureResult> triggerAf() {
        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                completer -> {
                    mExecutor.execute(() -> mFocusMeteringControl.triggerAf(completer));
                    return "triggerAf";
                }));
    }

    /**
     * Issues a {@link CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_START} request to start auto
     * exposure scan.
     *
     * @return a {@link ListenableFuture} which completes when the request is completed.
     * Cancelling the ListenableFuture is a no-op.
     */
    @Override
    @NonNull
    public ListenableFuture<CameraCaptureResult> triggerAePrecapture() {
        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                completer -> {
                    mExecutor.execute(() -> mFocusMeteringControl.triggerAePrecapture(completer));
                    return "triggerAePrecapture";
                }));
    }

    /**
     * Issues {@link CaptureRequest#CONTROL_AF_TRIGGER_CANCEL} or {@link
     * CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL} request to cancel auto focus or auto
     * exposure scan.
     */
    @Override
    public void cancelAfAeTrigger(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        mExecutor.execute(() -> mFocusMeteringControl.cancelAfAeTrigger(cancelAfTrigger,
                cancelAePrecaptureTrigger));
    }

    /** {@inheritDoc} */
    @Override
    public void submitCaptureRequests(@NonNull final List<CaptureConfig> captureConfigs) {
        mExecutor.execute(() -> submitCaptureRequestsInternal(captureConfigs));
    }

    int getDefaultTemplate() {
        return CameraDevice.TEMPLATE_PREVIEW;
    }

    @WorkerThread
    void updateSessionConfig() {
        mSessionConfigBuilder.setImplementationOptions(getSessionOptions());
        mControlUpdateCallback.onCameraControlUpdateSessionConfig(mSessionConfigBuilder.build());
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void setCropRegionInternal(final Rect crop) {
        mCropRect = crop;
        updateSessionConfig();
    }

    @WorkerThread
    @NonNull
    Rect getCropSensorRegion() {
        Rect cropRect = mCropRect;
        if (cropRect == null) {
            cropRect = getSensorRect();
        }
        return cropRect;
    }

    @Override
    @WorkerThread
    @NonNull
    public Rect getSensorRect() {
        return Preconditions.checkNotNull(
                mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE));
    }

    @WorkerThread
    void removeCaptureResultListener(@NonNull CaptureResultListener listener) {
        mSessionCallback.removeListener(listener);
    }

    @WorkerThread
    void addCaptureResultListener(@NonNull CaptureResultListener listener) {
        mSessionCallback.addListener(listener);
    }

    /** Adds a session {@link CameraCaptureCallback dynamically */
    void addSessionCameraCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback cameraCaptureCallback) {
        mExecutor.execute(()-> {
            mCameraCaptureCallbackSet.addCaptureCallback(executor, cameraCaptureCallback);
        });
    }

    /** Removes the {@link CameraCaptureCallback} that was added previously */
    void removeSessionCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
        mExecutor.execute(()-> {
            mCameraCaptureCallbackSet.removeCaptureCallback(cameraCaptureCallback);
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void enableTorchInternal(boolean torch) {
        mExecutor.execute(() -> {
            mIsTorchOn = torch;
            if (!torch) {
                // Send capture request with AE_MODE_ON + FLASH_MODE_OFF to turn off torch.
                CaptureConfig.Builder singleRequestBuilder = new CaptureConfig.Builder();
                singleRequestBuilder.setTemplateType(getDefaultTemplate());
                singleRequestBuilder.setUseRepeatingSurface(true);
                Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
                configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,
                        getSupportedAeMode(CaptureRequest.CONTROL_AE_MODE_ON));
                configBuilder.setCaptureRequestOption(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                singleRequestBuilder.addImplementationOptions(configBuilder.build());
                submitCaptureRequestsInternal(
                        Collections.singletonList(singleRequestBuilder.build()));
            }
            updateSessionConfig();
        });
    }


    @WorkerThread
    void submitCaptureRequestsInternal(final List<CaptureConfig> captureConfigs) {
        mControlUpdateCallback.onCameraControlCaptureRequests(captureConfigs);
    }

    /**
     * Gets session options by current status.
     *
     * <p>The session options are based on the current torch status, flash mode, focus area, crop
     * area, etc... They should be appended to the repeat request.
     */
    @VisibleForTesting
    @WorkerThread
    Config getSessionOptions() {
        Camera2ImplConfig.Builder builder = new Camera2ImplConfig.Builder();
        builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        // AF Mode is assigned in mFocusMeteringControl.
        mFocusMeteringControl.addFocusMeteringOptions(builder);

        mAeFpsRange.addAeFpsRangeOptions(builder);

        int aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
        if (mIsTorchOn) {
            builder.setCaptureRequestOption(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH);
        } else {
            switch (mFlashMode) {
                case FLASH_MODE_OFF:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
                    break;
                case FLASH_MODE_ON:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
                    break;
                case FLASH_MODE_AUTO:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
                    break;
            }
        }
        builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, getSupportedAeMode(aeMode));

        builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                getSupportedAwbMode(CaptureRequest.CONTROL_AWB_MODE_AUTO));

        if (mCropRect != null) {
            builder.setCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, mCropRect);
        }

        return builder.build();
    }

    /**
     * Returns a supported AF mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AF_MODE_CONTINUOUS_PICTURE}
     * 2) {@link CaptureRequest#CONTROL_AF_MODE_AUTO)}
     * 3) {@link CaptureRequest#CONTROL_AF_MODE_OFF}
     * </pre>
     */
    @WorkerThread
    int getSupportedAfMode(int preferredMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (modes == null) {
            return CaptureRequest.CONTROL_AF_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is CONTINUOUS_PICTURE > AUTO > OFF
        if (isModeInList(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, modes)) {
            return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        } else if (isModeInList(CaptureRequest.CONTROL_AF_MODE_AUTO, modes)) {
            return CaptureRequest.CONTROL_AF_MODE_AUTO;
        }

        return CaptureRequest.CONTROL_AF_MODE_OFF;
    }

    /**
     * Returns a supported AE mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AE_MODE_ON}
     * 2) {@link CaptureRequest#CONTROL_AE_MODE_OFF)}
     * </pre>
     */
    @WorkerThread
    private int getSupportedAeMode(int preferredMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);

        if (modes == null) {
            return CaptureRequest.CONTROL_AE_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is AE_ON > AE_OFF
        if (isModeInList(CaptureRequest.CONTROL_AE_MODE_ON, modes)) {
            return CaptureRequest.CONTROL_AE_MODE_ON;
        }

        return CaptureRequest.CONTROL_AE_MODE_OFF;
    }

    /**
     * Returns a supported AWB mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AWB_MODE_AUTO}
     * 2) {@link CaptureRequest#CONTROL_AWB_MODE_OFF)}
     * </pre>
     */
    @WorkerThread
    private int getSupportedAwbMode(int preferredMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);

        if (modes == null) {
            return CaptureRequest.CONTROL_AWB_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is AWB_AUTO > AWB_OFF
        if (isModeInList(CaptureRequest.CONTROL_AWB_MODE_AUTO, modes)) {
            return CaptureRequest.CONTROL_AWB_MODE_AUTO;
        }

        return CaptureRequest.CONTROL_AWB_MODE_OFF;
    }

    @WorkerThread
    private boolean isModeInList(int mode, int[] modeList) {
        for (int m : modeList) {
            if (mode == m) {
                return true;
            }
        }
        return false;
    }

    int getMaxAfRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        return count == null ? 0 : count;
    }

    int getMaxAeRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return count == null ? 0 : count;
    }

    int getMaxAwbRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB);
        return count == null ? 0 : count;
    }

    /** An interface to listen to camera capture results. */
    interface CaptureResultListener {
        /**
         * Callback to handle camera capture results.
         *
         * @param captureResult camera capture result.
         * @return true to finish listening, false to continue listening.
         */
        boolean onCaptureResult(@NonNull TotalCaptureResult captureResult);
    }

    static final class CameraControlSessionCallback extends CaptureCallback {

        /* synthetic accessor */final Set<CaptureResultListener> mResultListeners = new HashSet<>();
        @CameraExecutor
        private final Executor mExecutor;

        CameraControlSessionCallback(@NonNull @CameraExecutor Executor executor) {
            mExecutor = executor;
        }

        @WorkerThread
        void addListener(@NonNull CaptureResultListener listener) {
            mResultListeners.add(listener);
        }

        @WorkerThread
        void removeListener(@NonNull CaptureResultListener listener) {
            mResultListeners.remove(listener);
        }

        @Override
        public void onCaptureCompleted(
                @NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull final TotalCaptureResult result) {

            mExecutor.execute(() -> {
                Set<CaptureResultListener> removeSet = new HashSet<>();
                for (CaptureResultListener listener : mResultListeners) {
                    boolean isFinished = listener.onCaptureResult(result);
                    if (isFinished) {
                        removeSet.add(listener);
                    }
                }

                if (!removeSet.isEmpty()) {
                    mResultListeners.removeAll(removeSet);
                }
            });
        }
    }

    /**
     * A set of {@link CameraCaptureCallback}s which is capable of adding/removing callbacks
     * dynamically.
     */
    static final class CameraCaptureCallbackSet extends CameraCaptureCallback {
        Set<CameraCaptureCallback> mCallbacks = new HashSet<>();
        Map<CameraCaptureCallback, Executor> mCallbackExecutors = new ArrayMap<>();

        @ExecutedBy("mExecutor")
        void addCaptureCallback(@NonNull Executor executor,
                @NonNull CameraCaptureCallback callback) {
            mCallbacks.add(callback);
            mCallbackExecutors.put(callback, executor);
        }

        @ExecutedBy("mExecutor")
        void removeCaptureCallback(@NonNull CameraCaptureCallback callback) {
            mCallbacks.remove(callback);
            mCallbackExecutors.remove(callback);
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureCompleted(cameraCaptureResult);
                    });
                } catch (RejectedExecutionException e) {
                    Log.e(TAG, "Executor rejected to invoke onCaptureCompleted.", e);
                }
            }
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureFailed(@NonNull CameraCaptureFailure failure) {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureFailed(failure);
                    });
                } catch (RejectedExecutionException e) {
                    Log.e(TAG, "Executor rejected to invoke onCaptureFailed.", e);
                }
            }
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureCancelled() {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureCancelled();
                    });
                } catch (RejectedExecutionException e) {
                    Log.e(TAG, "Executor rejected to invoke onCaptureCancelled.", e);
                }
            }
        }
    }
}
