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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.core.CameraControl;
import androidx.camera.core.ExposureState;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Implementation of Exposure compensation control.
 *
 * <p>It is intended to be used within {@link Camera2CameraControl} to implement the
 * functionality of {@link Camera2CameraControl#setExposureCompensationIndex(int)}.
 *
 * <p>To wait for the exposure setting reach to the new requested target, it calls
 * {@link Camera2CameraControl#addCaptureResultListener(Camera2CameraControl.CaptureResultListener)}
 * to monitor the capture result.
 *
 * <p>The {@link Camera2CameraControl#setExposureCompensationIndex(int)} can only allow to run one
 * task at the same time, it will cancel the incomplete task if a new task is requested. The
 * task will fails with {@link CameraControl.OperationCanceledException} if the camera is closed.
 */
@UseExperimental(markerClass = androidx.camera.core.ExperimentalExposureCompensation.class)
public class ExposureControl {

    private static final int DEFAULT_EXPOSURE_COMPENSATION = 0;

    private final Object mLock = new Object();

    @NonNull
    private final Camera2CameraControl mCameraControl;

    @NonNull
    private final ExposureStateImpl mExposureStateImpl;

    @GuardedBy("mLock")
    private boolean mIsActive = false;

    @Nullable
    @GuardedBy("mLock")
    private CallbackToFutureAdapter.Completer<Integer> mRunningCompleter;
    @Nullable
    @GuardedBy("mLock")
    private Camera2CameraControl.CaptureResultListener mRunningCaptureResultListener;

    /**
     * Constructs a ExposureControl.
     *
     * <p>All tasks executed by {@code executor}.
     *
     * @param cameraControl         Camera control.
     * @param cameraCharacteristics The {@link CameraCharacteristics} of the camera.
     */
    ExposureControl(@NonNull Camera2CameraControl cameraControl,
            @NonNull CameraCharacteristics cameraCharacteristics) {
        mCameraControl = cameraControl;
        mExposureStateImpl = new ExposureStateImpl(cameraCharacteristics,
                DEFAULT_EXPOSURE_COMPENSATION);
    }

    /**
     * Set current active state. Set active if it is ready to accept operations.
     *
     * <p>Set the active state to false will cancel the in fly
     * {@link #setExposureCompensationIndex(int)} task with
     * {@link CameraControl.OperationCanceledException}.
     */
    void setActive(boolean isActive) {
        synchronized (mLock) {
            if (isActive == mIsActive) {
                return;
            }

            mIsActive = isActive;

            if (!mIsActive) {
                mExposureStateImpl.setExposureCompensationIndex(DEFAULT_EXPOSURE_COMPENSATION);
                clearRunningTask();
            }
        }
    }

    /**
     * Called by {@link Camera2CameraControl} to append the CONTROL_AE_EXPOSURE_COMPENSATION option
     * to the shared options. It applies to all repeating requests and single requests.
     */
    @WorkerThread
    void setCaptureRequestOption(@NonNull Camera2ImplConfig.Builder configBuilder) {
        synchronized (mLock) {
            configBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    mExposureStateImpl.getExposureCompensationIndex());
        }
    }

    @NonNull
    ExposureState getExposureState() {
        return mExposureStateImpl;
    }

    @NonNull
    @WorkerThread
    ListenableFuture<Integer> setExposureCompensationIndex(int exposure) {
        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                completer -> {
                    synchronized (mLock) {
                        if (!mIsActive) {
                            completer.setException(new CameraControl.OperationCanceledException(
                                    "Camera is not active."));
                            return "setExposureCompensation[" + exposure + "]";
                        }

                        if (!mExposureStateImpl.isExposureCompensationSupported()) {
                            completer.setException(new IllegalArgumentException(
                                    "ExposureCompensation is not supported"));
                            return "setExposureCompensation[" + exposure + "]";

                        }

                        Range<Integer> range = mExposureStateImpl.getExposureCompensationRange();
                        if (!range.contains(exposure)) {
                            completer.setException(new IllegalArgumentException(
                                    "Requested ExposureCompensation " + exposure + " is not within"
                                            + " valid range [" + range.getUpper() + ".."
                                            + range.getLower() + "]"));
                            return "setExposureCompensation[" + exposure + "]";
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
                        mExposureStateImpl.setExposureCompensationIndex(exposure);
                        mCameraControl.updateSessionConfig();

                        return "setExposureCompensationIndex[" + exposure + "]";
                    }
                }));
    }

    private void clearRunningTask() {
        synchronized (mLock) {
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
}
