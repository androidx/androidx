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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraControl.OperationCanceledException;
import androidx.camera.core.Logger;
import androidx.camera.core.TorchState;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Implementation of torch control used within {@link Camera2CameraControlImpl}.
 *
 * It is used to control the flash torch of camera device that {@link Camera2CameraControlImpl}
 * operates. The torch control must be activated via {@link #setActive(boolean)} when the
 * camera device is ready to do torch operations and be deactivated when the camera device is
 * closing or closed.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class TorchControl {
    private static final String TAG = "TorchControl";
    static final int DEFAULT_TORCH_STATE = TorchState.OFF;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private final Camera2CameraControlImpl mCamera2CameraControlImpl;
    private final MutableLiveData<Integer> mTorchState;
    private final boolean mHasFlashUnit;
    @CameraExecutor
    private final Executor mExecutor;

    private boolean mIsActive;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CallbackToFutureAdapter.Completer<Void> mEnableTorchCompleter;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean mTargetTorchEnabled;

    /**
     * Constructs a TorchControl.
     *
     * @param camera2CameraControlImpl the camera control this TorchControl belongs.
     * @param cameraCharacteristics the characteristics for the camera being controlled.
     * @param executor the camera executor used to run camera task.
     */
    TorchControl(@NonNull Camera2CameraControlImpl camera2CameraControlImpl,
            @NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @CameraExecutor @NonNull Executor executor) {
        mCamera2CameraControlImpl = camera2CameraControlImpl;
        mExecutor = executor;
        Boolean hasFlashUnit =
                cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mHasFlashUnit = hasFlashUnit != null && hasFlashUnit;
        mTorchState = new MutableLiveData<>(DEFAULT_TORCH_STATE);
        Camera2CameraControlImpl.CaptureResultListener captureResultListener = captureResult -> {
            if (mEnableTorchCompleter != null) {
                CaptureRequest captureRequest = captureResult.getRequest();
                Integer flashMode = captureRequest.get(CaptureRequest.FLASH_MODE);
                boolean torchEnabled =
                        flashMode != null && flashMode == CaptureRequest.FLASH_MODE_TORCH;

                if (torchEnabled == mTargetTorchEnabled) {
                    mEnableTorchCompleter.set(null);
                    mEnableTorchCompleter = null;
                }
            }
            // Return false to keep getting captureResult.
            return false;
        };
        mCamera2CameraControlImpl.addCaptureResultListener(captureResultListener);
    }

    /**
     * Set current active state. Set active if it is ready to do torch operations.
     *
     * @param isActive true to activate or false otherwise.
     */
    @ExecutedBy("mExecutor")
    void setActive(boolean isActive) {
        if (mIsActive == isActive) {
            return;
        }

        mIsActive = isActive;

        if (!isActive) {
            if (mTargetTorchEnabled) {
                mTargetTorchEnabled = false;
                mCamera2CameraControlImpl.enableTorchInternal(false);
                setLiveDataValue(mTorchState, TorchState.OFF);
            }

            if (mEnableTorchCompleter != null) {
                mEnableTorchCompleter.setException(
                        new OperationCanceledException("Camera is not active."));
                mEnableTorchCompleter = null;
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
            Logger.d(TAG, "Unable to enableTorch due to there is no flash unit.");
            return Futures.immediateFailedFuture(new IllegalStateException("No flash unit"));
        }

        setLiveDataValue(mTorchState, enabled ? TorchState.ON : TorchState.OFF);

        return CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(
                    () -> enableTorchInternal(completer, enabled));
            return "enableTorch: " + enabled;
        });
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

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void enableTorchInternal(@Nullable Completer<Void> completer, boolean enabled) {
        if (!mHasFlashUnit) {
            if (completer != null) {
                completer.setException(new IllegalStateException("No flash unit"));
            }
            return;
        }

        if (!mIsActive) {
            setLiveDataValue(mTorchState, TorchState.OFF);
            if (completer != null) {
                completer.setException(new OperationCanceledException("Camera is not active."));
            }
            return;
        }

        mTargetTorchEnabled = enabled;
        mCamera2CameraControlImpl.enableTorchInternal(enabled);
        setLiveDataValue(mTorchState, enabled ? TorchState.ON : TorchState.OFF);
        if (mEnableTorchCompleter != null) {
            mEnableTorchCompleter.setException(new OperationCanceledException(
                    "There is a new enableTorch being set"));
        }
        mEnableTorchCompleter = completer;
    }

    private <T> void setLiveDataValue(@NonNull MutableLiveData<T> liveData, T value) {
        if (Threads.isMainThread()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }
}
