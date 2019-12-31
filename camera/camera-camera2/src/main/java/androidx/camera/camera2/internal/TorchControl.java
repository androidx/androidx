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

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraControl.OperationCanceledException;
import androidx.camera.core.TorchState;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Implementation of torch control used within {@link Camera2CameraControl}.
 *
 * It is used to control the flash torch of camera device that {@link Camera2CameraControl}
 * operates. The torch control must be activated via {@link #setActive(boolean)} when the
 * camera device is ready to do torch operations and be deactivated when the camera device is
 * closing or closed.
 */
final class TorchControl {
    private static final String TAG = "TorchControl";

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mEnableTorchLock = new Object();
    private final Object mActiveLock = new Object();
    private final Camera2CameraControl mCamera2CameraControl;
    private final MutableLiveData<Integer> mTorchState;
    private final boolean mHasFlashUnit;

    @GuardedBy("mActiveLock")
    private boolean mIsActive;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mEnableTorchLock")
    CallbackToFutureAdapter.Completer<Void> mEnableTorchCompleter;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mEnableTorchLock")
    boolean mTargetTorchEnabled;

    /**
     * Constructs a TorchControl.
     *
     * @param camera2CameraControl the camera control this TorchControl belongs.
     * @param cameraCharacteristics the characteristics for the camera being controlled.
     */
    TorchControl(@NonNull Camera2CameraControl camera2CameraControl,
            @NonNull CameraCharacteristics cameraCharacteristics) {
        mCamera2CameraControl = camera2CameraControl;
        Boolean hasFlashUnit =
                cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mHasFlashUnit = hasFlashUnit != null && hasFlashUnit.booleanValue();
        mTorchState = new MutableLiveData<>(TorchState.OFF);
        mCamera2CameraControl.addCaptureResultListener(mCaptureResultListener);
    }

    /**
     * Set current active state. Set active if it is ready to do torch operations.
     *
     * @param isActive true to activate or false otherwise.
     */
    void setActive(boolean isActive) {
        synchronized (mActiveLock) {
            if (mIsActive == isActive) {
                return;
            }

            mIsActive = isActive;

            boolean shouldResetDefault = false;
            CallbackToFutureAdapter.Completer<Void> completerToCancel = null;
            synchronized (mEnableTorchLock) {
                if (!isActive) {
                    if (mEnableTorchCompleter != null) {
                        completerToCancel = mEnableTorchCompleter;
                        mEnableTorchCompleter = null;
                    }
                    if (mTargetTorchEnabled) {
                        shouldResetDefault = true;
                        mTargetTorchEnabled = false;
                        mCamera2CameraControl.enableTorchInternal(false);
                    }
                }
            }

            if (shouldResetDefault) {
                setLiveDataValue(mTorchState, TorchState.OFF);
            }
            if (completerToCancel != null) {
                completerToCancel.setException(new OperationCanceledException(
                        "Camera is not active."));
            }
        }
    }

    /**
     * Enable the torch or disable the torch.
     *
     * <p>The returned {@link ListenableFuture} will succeed when the request is sent to camera
     * device. But it may get an {@link OperationCanceledException} result when:
     * <ol>
     * <li>There are multiple {@link #enableTorch(boolean)} requests in the same time, the older
     * and incomplete futures will get cancelled.
     * <li>When the TorchControl is set to inactive.
     * </ol>
     *
     * <p>The returned {@link ListenableFuture} will fail immediately when:
     * <ol>
     * <li>The TorchControl is not in active state.
     * <li>The camera doesn't have a flash unit. (see
     * {@link CameraCharacteristics#FLASH_INFO_AVAILABLE})
     * </ol>
     *
     * @param enabled true to open the torch, false to close it.
     * @return A {@link ListenableFuture} which is successful when the torch was changed to the
     * value specified. It fails when it is unable to change the torch state.
     */
    ListenableFuture<Void> enableTorch(boolean enabled) {
        if (!mHasFlashUnit) {
            Log.d(TAG, "Unable to enableTorch due to there is no flash unit.");
            return Futures.immediateFailedFuture(new IllegalStateException("No flash unit"));
        }

        synchronized (mActiveLock) {
            if (!mIsActive) {
                return Futures.immediateFailedFuture(new OperationCanceledException(
                        "Camera is not active."));
            }

            ListenableFuture<Void> future = CallbackToFutureAdapter.getFuture(
                    completer -> {
                        CallbackToFutureAdapter.Completer<Void> completerToCancel = null;
                        synchronized (mEnableTorchLock) {
                            if (mEnableTorchCompleter != null) {
                                completerToCancel = mEnableTorchCompleter;
                            }
                            mEnableTorchCompleter = completer;
                            mTargetTorchEnabled = enabled;
                            mCamera2CameraControl.enableTorchInternal(enabled);
                        }
                        setLiveDataValue(mTorchState, enabled ? TorchState.ON : TorchState.OFF);
                        if (completerToCancel != null) {
                            completerToCancel.setException(new OperationCanceledException(
                                    "There is a new enableTorch being set"));
                        }
                        return "enableTorch: " + enabled;
                    });
            return future;
        }
    }

    /**
     * Returns a {@link LiveData} of current {@link TorchState}.
     *
     * <p>The torch state can be enabled or disabled via {@link #enableTorch(boolean)} which will
     * trigger the change event to the returned {@link LiveData}.
     *
     * @return a {@link LiveData} containing current torch state.
     */
    @NonNull
    LiveData<Integer> getTorchState() {
        return mTorchState;
    }

    private <T> void setLiveDataValue(@NonNull MutableLiveData<T> liveData, T value) {
        if (Threads.isMainThread()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }

    private final Camera2CameraControl.CaptureResultListener mCaptureResultListener =
            new Camera2CameraControl.CaptureResultListener() {

        @Override
        public boolean onCaptureResult(@NonNull TotalCaptureResult captureResult) {
            CallbackToFutureAdapter.Completer<Void> completerToSet = null;
            synchronized (mEnableTorchLock) {
                if (mEnableTorchCompleter != null) {
                    CaptureRequest captureRequest = captureResult.getRequest();
                    Integer flashMode = captureRequest.get(CaptureRequest.FLASH_MODE);
                    boolean torchEnabled =
                            flashMode != null && flashMode == CaptureRequest.FLASH_MODE_TORCH;

                    if (torchEnabled == mTargetTorchEnabled) {
                        completerToSet = mEnableTorchCompleter;
                        mEnableTorchCompleter = null;
                    }
                }
            }
            if (completerToSet != null) {
                completerToSet.set(null);
            }
            // Return false to keep getting captureResult.
            return false;
        }
    };
}
