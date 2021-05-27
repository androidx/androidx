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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraControl;
import androidx.camera.core.ExposureState;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Implementation of Exposure compensation control.
 *
 * <p>It is intended to be used within {@link Camera2CameraControlImpl} to implement the
 * functionality of {@link Camera2CameraControlImpl#setExposureCompensationIndex(int)}.
 *
 * <p>To wait for the exposure setting reach to the new requested target, it calls
 * {@link Camera2CameraControlImpl#addCaptureResultListener(
 * Camera2CameraControlImpl.CaptureResultListener)} to monitor the capture result.
 *
 * <p>The {@link Camera2CameraControlImpl#setExposureCompensationIndex(int)} can only allow to
 * run one task at the same time, it will cancel the incomplete task if a new task is requested.
 * The task will fails with {@link CameraControl.OperationCanceledException} if the camera is
 * closed.
 */
public class ExposureControl {

    private static final int DEFAULT_EXPOSURE_COMPENSATION = 0;

    @NonNull
    private final Camera2CameraControlImpl mCameraControl;

    @NonNull
    private final ExposureStateImpl mExposureStateImpl;

    @NonNull
    @CameraExecutor
    private final Executor mExecutor;

    private boolean mIsActive = false;

    @Nullable
    private CallbackToFutureAdapter.Completer<Integer> mRunningCompleter;
    @Nullable
    private Camera2CameraControlImpl.CaptureResultListener mRunningCaptureResultListener;

    /**
     * Constructs a ExposureControl.
     *
     * <p>All tasks executed by {@code executor}.
     *
     * @param cameraControl         Camera control.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     * @param executor              the camera executor used to run camera task.
     */
    ExposureControl(@NonNull Camera2CameraControlImpl cameraControl,
            @NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @CameraExecutor @NonNull Executor executor) {
        mCameraControl = cameraControl;
        mExposureStateImpl = new ExposureStateImpl(cameraCharacteristics,
                DEFAULT_EXPOSURE_COMPENSATION);
        mExecutor = executor;
    }

    static ExposureState getDefaultExposureState(
            CameraCharacteristicsCompat cameraCharacteristics) {
        return new ExposureStateImpl(cameraCharacteristics, DEFAULT_EXPOSURE_COMPENSATION);
    }

    /**
     * Set current active state. Set active if it is ready to accept operations.
     *
     * <p>Set the active state to false will cancel the in fly
     * {@link #setExposureCompensationIndex(int)} task with
     * {@link CameraControl.OperationCanceledException}.
     */
    @ExecutedBy("mExecutor")
    void setActive(boolean isActive) {
        if (isActive == mIsActive) {
            return;
        }

        mIsActive = isActive;

        if (!mIsActive) {
            mExposureStateImpl.setExposureCompensationIndex(DEFAULT_EXPOSURE_COMPENSATION);
            clearRunningTask();
        }
    }

    /**
     * Called by {@link Camera2CameraControlImpl} to append the CONTROL_AE_EXPOSURE_COMPENSATION
     * option
     * to the shared options. It applies to all repeating requests and single requests.
     */
    @ExecutedBy("mExecutor")
    void setCaptureRequestOption(@NonNull Camera2ImplConfig.Builder configBuilder) {
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                mExposureStateImpl.getExposureCompensationIndex());
    }

    @NonNull
    ExposureState getExposureState() {
        return mExposureStateImpl;
    }

    @NonNull
    ListenableFuture<Integer> setExposureCompensationIndex(int exposure) {
        if (!mExposureStateImpl.isExposureCompensationSupported()) {
            return Futures.immediateFailedFuture(new IllegalArgumentException(
                    "ExposureCompensation is not supported"));
        }

        Range<Integer> range = mExposureStateImpl.getExposureCompensationRange();
        if (!range.contains(exposure)) {
            return Futures.immediateFailedFuture(new IllegalArgumentException(
                    "Requested ExposureCompensation " + exposure + " is not within"
                            + " valid range [" + range.getUpper() + ".." + range.getLower() + "]"));
        }

        // Set the new exposure value to the ExposureState immediately.
        mExposureStateImpl.setExposureCompensationIndex(exposure);

        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                completer -> {
                    mExecutor.execute(() -> {
                        if (!mIsActive) {
                            mExposureStateImpl.setExposureCompensationIndex(
                                    DEFAULT_EXPOSURE_COMPENSATION);
                            completer.setException(new CameraControl.OperationCanceledException(
                                    "Camera is not active."));
                            return;
                        }

                        clearRunningTask();

                        Preconditions.checkState(mRunningCompleter == null, "mRunningCompleter "
                                + "should be null when starting set a new exposure compensation "
                                + "value");
                        Preconditions.checkState(mRunningCaptureResultListener == null,
                                "mRunningCaptureResultListener "
                                        + "should be null when starting set a new exposure "
                                        + "compensation value");

                        mRunningCaptureResultListener =
                                captureResult -> {
                                    Integer state = captureResult.get(
                                            CaptureResult.CONTROL_AE_STATE);
                                    Integer evResult = captureResult.get(
                                            CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);
                                    if (state != null && evResult != null) {
                                        switch (state) {
                                            case CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED:
                                            case CaptureResult.CONTROL_AE_STATE_CONVERGED:
                                            case CaptureResult.CONTROL_AE_STATE_LOCKED:
                                                if (evResult == exposure) {
                                                    completer.set(exposure);
                                                    // Only remove the capture result listener,
                                                    // the mRunningCompleter and
                                                    // mRunningCaptureResultListener will be
                                                    // cleared before the next set exposure task.
                                                    return true;
                                                }
                                                break;
                                            default:
                                                // Ignore other results.
                                        }
                                    } else if (evResult != null && evResult == exposure) {
                                        // If AE state is null, only wait for the exposure result
                                        // to the desired value.
                                        completer.set(exposure);

                                        // Only remove the capture result listener, the
                                        // mRunningCompleter and mRunningCaptureResultListener
                                        // will be cleared before the next set exposure task.
                                        return true;
                                    }
                                    return false;
                                };
                        mRunningCompleter = completer;

                        mCameraControl.addCaptureResultListener(mRunningCaptureResultListener);
                        mCameraControl.updateSessionConfigSynchronous();
                    });

                    return "setExposureCompensationIndex[" + exposure + "]";
                }));
    }

    @ExecutedBy("mExecutor")
    private void clearRunningTask() {
        if (mRunningCompleter != null) {
            mRunningCompleter.setException(
                    new CameraControl.OperationCanceledException(
                            "Cancelled by another setExposureCompensationIndex()"));
            mRunningCompleter = null;
        }

        if (mRunningCaptureResultListener != null) {
            mCameraControl.removeCaptureResultListener(mRunningCaptureResultListener);
            mRunningCaptureResultListener = null;
        }
    }
}
