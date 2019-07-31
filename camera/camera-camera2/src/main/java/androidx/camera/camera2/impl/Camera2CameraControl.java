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

package androidx.camera.camera2.impl;

import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.Config;
import androidx.camera.core.FlashMode;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A Camera2 implementation for CameraControlInternal interface
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Camera2CameraControl implements CameraControlInternal {
    private static final String TAG = "Camera2CameraControl";
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final CameraControlSessionCallback mSessionCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mExecutor;
    private final CameraCharacteristics mCameraCharacteristics;
    private final ControlUpdateListener mControlUpdateListener;
    private final ScheduledExecutorService mScheduler;
    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    volatile Rational mPreviewAspectRatio = null;
    @VisibleForTesting
    final FocusMeteringControl mFocusMeteringControl;
    // use volatile modifier to make these variables in sync in all threads.
    private volatile boolean mIsTorchOn = false;
    private volatile FlashMode mFlashMode = FlashMode.OFF;

    //******************** Should only be accessed by executor *****************************//
    private Rect mCropRect = null;
    //**************************************************************************************//


    public Camera2CameraControl(@NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull ControlUpdateListener controlUpdateListener,
            @NonNull ScheduledExecutorService scheduler, @NonNull Executor executor) {
        mCameraCharacteristics = cameraCharacteristics;
        mControlUpdateListener = controlUpdateListener;
        if (CameraXExecutors.isSequentialExecutor(executor)) {
            mExecutor = executor;
        } else {
            mExecutor = CameraXExecutors.newSequentialExecutor(executor);
        }
        mScheduler = scheduler;
        mSessionCallback = new CameraControlSessionCallback(mExecutor);
        mSessionConfigBuilder.setTemplateType(getDefaultTemplate());
        mSessionConfigBuilder.addRepeatingCameraCaptureCallback(
                CaptureCallbackContainer.create(mSessionCallback));

        mFocusMeteringControl = new FocusMeteringControl(this, mExecutor, mScheduler);

        // Initialize the session config
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                updateSessionConfig();
            }
        });
    }

    public void setPreviewAspectRatio(@Nullable Rational previewAspectRatio) {
        mPreviewAspectRatio = previewAspectRatio;
    }

    @Override
    public void startFocusAndMetering(@NonNull FocusMeteringAction action) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mFocusMeteringControl.startFocusAndMetering(action, mPreviewAspectRatio);
            }
        });
    }

    @Override
    public void cancelFocusAndMetering() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mFocusMeteringControl.cancelFocusAndMetering();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void setCropRegion(@Nullable final Rect crop) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                setCropRegionInternal(crop);
            }
        });
    }

    @NonNull
    @Override
    public FlashMode getFlashMode() {
        return mFlashMode;
    }

    /** {@inheritDoc} */
    @Override
    public void setFlashMode(@NonNull FlashMode flashMode) {
        // update mFlashMode immediately so that following getFlashMode() returns correct value.
        mFlashMode = flashMode;

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                updateSessionConfig();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void enableTorch(final boolean torch) {
        // update isTorchOn immediately so that following isTorchOn() returns correct value.
        mIsTorchOn = torch;

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                enableTorchInternal(torch);
            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public boolean isTorchOn() {
        return mIsTorchOn;
    }

    /**
     * Issues a {@link CaptureRequest#CONTROL_AF_TRIGGER_START} request to start auto focus scan.
     */
    @Override
    public void triggerAf() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mFocusMeteringControl.triggerAf();
            }
        });
    }

    /**
     * Issues a {@link CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_START} request to start auto
     * exposure scan.
     */
    @Override
    public void triggerAePrecapture() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mFocusMeteringControl.triggerAePrecapture();
            }
        });
    }

    /**
     * Issues {@link CaptureRequest#CONTROL_AF_TRIGGER_CANCEL} or {@link
     * CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL} request to cancel auto focus or auto
     * exposure scan.
     */
    @Override
    public void cancelAfAeTrigger(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mFocusMeteringControl.cancelAfAeTrigger(cancelAfTrigger,
                        cancelAePrecaptureTrigger);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void submitCaptureRequests(@NonNull final List<CaptureConfig> captureConfigs) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                submitCaptureRequestsInternal(captureConfigs);
            }
        });
    }

    /**
     * Creates a CaptureConfig.Builder contains shared options.
     *
     * @return a {@link CaptureConfig.Builder} contains shared options.
     */
    private CaptureConfig.Builder createCaptureBuilderWithSharedOptions() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.addImplementationOptions(getSharedOptions());
        return builder;
    }

    @WorkerThread
    private int getDefaultTemplate() {
        return CameraDevice.TEMPLATE_PREVIEW;
    }

    @WorkerThread
    private void notifyCaptureRequests(final List<CaptureConfig> captureConfigs) {
        mControlUpdateListener.onCameraControlCaptureRequests(captureConfigs);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void updateSessionConfig() {
        mSessionConfigBuilder.setImplementationOptions(getSharedOptions());
        mControlUpdateListener.onCameraControlUpdateSessionConfig(mSessionConfigBuilder.build());
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

    @WorkerThread
    @NonNull
    Rect getSensorRect() {
        return mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
    }

    @WorkerThread
    void removeCaptureResultListener(@NonNull CaptureResultListener listener) {
        mSessionCallback.removeListener(listener);
    }

    @WorkerThread
    void addCaptureResultListener(@NonNull CaptureResultListener listener) {
        mSessionCallback.addListener(listener);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void enableTorchInternal(boolean torch) {
        if (!torch) {
            // Send capture request with AE_MODE_ON + FLASH_MODE_OFF to turn off torch.
            CaptureConfig.Builder singleRequestBuilder = createCaptureBuilderWithSharedOptions();
            singleRequestBuilder.setTemplateType(getDefaultTemplate());
            singleRequestBuilder.setUseRepeatingSurface(true);
            Camera2Config.Builder configBuilder = new Camera2Config.Builder();
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,
                    getSupportedAeMode(CaptureRequest.CONTROL_AE_MODE_ON));
            configBuilder.setCaptureRequestOption(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF);
            singleRequestBuilder.addImplementationOptions(configBuilder.build());
            notifyCaptureRequests(Collections.singletonList(singleRequestBuilder.build()));
        }
        updateSessionConfig();
    }


    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void submitCaptureRequestsInternal(final List<CaptureConfig> captureConfigs) {
        List<CaptureConfig> captureConfigsWithImpl = new ArrayList<>();
        for (CaptureConfig captureConfig : captureConfigs) {
            CaptureConfig.Builder builder = CaptureConfig.Builder.from(captureConfig);
            // Always override options by shared options for the capture request from outside.
            builder.addImplementationOptions(getSharedOptions());
            captureConfigsWithImpl.add(builder.build());
        }
        notifyCaptureRequests(captureConfigsWithImpl);
    }

    /**
     * Gets shared options by current status.
     *
     * <p>The shared options are based on the current torch status, flash mode, focus area, crop
     * area, etc... They should be appended to the repeat request and each single capture request.
     */
    @VisibleForTesting
    @WorkerThread
    Config getSharedOptions() {
        Camera2Config.Builder builder = new Camera2Config.Builder();
        builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        // AF Mode is assigned in mFocusMeteringControl.
        mFocusMeteringControl.addFocusMeteringOptions(builder);

        int aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
        if (mIsTorchOn) {
            builder.setCaptureRequestOption(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH);
        } else {
            switch (mFlashMode) {
                case OFF:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
                    break;
                case ON:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
                    break;
                case AUTO:
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
        private final Executor mExecutor;

        CameraControlSessionCallback(@NonNull Executor executor) {
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

            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
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
                }
            });
        }
    }
}
