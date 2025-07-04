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
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.workaround.FlashAvailabilityChecker;
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Implementation of torch control used within {@link Camera2CameraControlImpl}.
 *
 * It is used to control the flash torch of camera device that {@link Camera2CameraControlImpl}
 * operates. The torch control must be activated via {@link #setActive(boolean)} when the
 * camera device is ready to do torch operations and be deactivated when the camera device is
 * closing or closed.
 */
final class TorchControl {
    private static final String TAG = "TorchControl";
    static final int DEFAULT_TORCH_STATE = TorchState.OFF;

    /** Torch is off. */
    static final int OFF = 0;
    /** Torch is turned on explicitly by {@link #enableTorch(boolean)}. */
    static final int ON = 1;
    /** Torch is turned on as flash by the capture pipeline. */
    static final int USED_AS_FLASH = 2;

    /** The internal torch state. */
    @IntDef({OFF, ON, USED_AS_FLASH})
    @Retention(RetentionPolicy.SOURCE)
    @interface TorchStateInternal {
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private final Camera2CameraControlImpl mCamera2CameraControlImpl;
    private final MutableLiveData<Integer> mTorchState;
    private final MutableLiveData<Integer> mTorchStrength;
    private final boolean mHasFlashUnit;
    @CameraExecutor
    private final Executor mExecutor;

    private boolean mIsActive;
    private boolean mIsTorchStrengthSupported;
    private int mDefaultTorchStrength;
    private int mTargetTorchStrength;
    private CallbackToFutureAdapter.Completer<Void> mTorchStrengthCompleter;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            CallbackToFutureAdapter.Completer<Void> mEnableTorchCompleter;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            boolean mTargetTorchEnabled;

    /**
     * Constructs a TorchControl.
     *
     * @param camera2CameraControlImpl the camera control this TorchControl belongs.
     * @param cameraCharacteristics    the characteristics for the camera being controlled.
     * @param executor                 the camera executor used to run camera task.
     */
    TorchControl(@NonNull Camera2CameraControlImpl camera2CameraControlImpl,
            @NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @CameraExecutor @NonNull Executor executor) {
        mCamera2CameraControlImpl = camera2CameraControlImpl;
        mExecutor = executor;

        mHasFlashUnit = FlashAvailabilityChecker.isFlashAvailable(cameraCharacteristics::get);
        mIsTorchStrengthSupported = cameraCharacteristics.isTorchStrengthLevelSupported();
        mDefaultTorchStrength = mHasFlashUnit && mIsTorchStrengthSupported
                ? cameraCharacteristics.getDefaultTorchStrengthLevel()
                : Camera2CameraInfoImpl.TORCH_STRENGTH_LEVEL_UNSUPPORTED;
        mTargetTorchStrength = mDefaultTorchStrength;
        mTorchState = new MutableLiveData<>(DEFAULT_TORCH_STATE);
        mTorchStrength = new MutableLiveData<>(mDefaultTorchStrength);
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
            if (mIsTorchStrengthSupported
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                    && mTorchStrengthCompleter != null) {
                Integer torchStrength = captureResult.get(CaptureResult.FLASH_STRENGTH_LEVEL);

                if (torchStrength != null && torchStrength == mTargetTorchStrength) {
                    mTorchStrengthCompleter.set(null);
                    mTorchStrengthCompleter = null;
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
                mTargetTorchStrength = mDefaultTorchStrength;
                mCamera2CameraControlImpl.enableTorchInternal(OFF);
                setTorchState(OFF);
                setLiveDataValue(mTorchStrength, mDefaultTorchStrength);
            }

            if (mEnableTorchCompleter != null) {
                mEnableTorchCompleter.setException(
                        new OperationCanceledException("Camera is not active."));
                mEnableTorchCompleter = null;
            }

            if (mTorchStrengthCompleter != null) {
                mTorchStrengthCompleter.setException(
                        new OperationCanceledException("Camera is not active."));
                mTorchStrengthCompleter = null;
            }
        }
    }

    /**
     * Enable the torch or disable the torch.
     *
     * <p>The returned {@link ListenableFuture} will succeed when the request is sent to camera
     * device. But it may get an {@link OperationCanceledException} result when:
     * <ol>
     * <li>There are multiple {@code enableTorch(boolean)} requests in the same time, the older
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

        @TorchStateInternal int torchState = enabled ? ON : OFF;
        setTorchState(torchState);

        return CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(() -> enableTorchInternal(completer, torchState));
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
    @NonNull LiveData<Integer> getTorchState() {
        return mTorchState;
    }

    @NonNull LiveData<Integer> getTorchStrengthLevel() {
        return mTorchStrength;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void enableTorchInternal(@Nullable Completer<Void> completer,
            @TorchStateInternal int torchState) {
        if (!mHasFlashUnit) {
            if (completer != null) {
                completer.setException(new IllegalStateException("No flash unit"));
            }
            return;
        }

        if (!mIsActive) {
            setTorchState(OFF);
            if (completer != null) {
                completer.setException(new OperationCanceledException("Camera is not active."));
            }
            return;
        }

        if (mCamera2CameraControlImpl.isLowLightBoostOn()) {
            if (completer != null) {
                completer.setException(new IllegalStateException(
                        "Torch can not be enabled when low-light boost is on!"));
            }
            return;
        }

        mTargetTorchEnabled = torchState != OFF;
        mCamera2CameraControlImpl.enableTorchInternal(torchState);
        setTorchState(torchState);
        if (mEnableTorchCompleter != null) {
            mEnableTorchCompleter.setException(new OperationCanceledException(
                    "There is a new enableTorch being set"));
        }
        mEnableTorchCompleter = completer;
    }

    ListenableFuture<Void> setTorchStrengthLevel(@IntRange(from = 1) int torchStrengthLevel) {
        if (!mIsTorchStrengthSupported) {
            return Futures.immediateFailedFuture(new UnsupportedOperationException(
                    "Setting torch strength is not supported on the device."));
        }

        setLiveDataValue(mTorchStrength, torchStrengthLevel);

        return CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(
                    () -> setTorchStrengthLevelInternal(completer, torchStrengthLevel));
            return "setTorchStrength: " + torchStrengthLevel;
        });
    }

    @ExecutedBy("mExecutor")
    void setTorchStrengthLevelInternal(@Nullable Completer<Void> completer,
            @IntRange(from = 1) int torchStrengthLevel) {
        if (!mIsTorchStrengthSupported) {
            if (completer != null) {
                completer.setException(new UnsupportedOperationException(
                        "Setting torch strength is not supported on the device."));
            }
            return;
        }

        if (!mIsActive) {
            if (completer != null) {
                completer.setException(new OperationCanceledException("Camera is not active."));
            }
            return;
        }

        mTargetTorchStrength = torchStrengthLevel;
        mCamera2CameraControlImpl.setTorchStrengthLevelInternal(torchStrengthLevel);
        if (!mCamera2CameraControlImpl.isTorchOn() && completer != null) {
            // Complete the future if the torch is not on. The new strength will be applied next
            // time it's turned on.
            completer.set(null);
        } else {
            if (mTorchStrengthCompleter != null) {
                mTorchStrengthCompleter.setException(new OperationCanceledException(
                        "There is a new torch strength being set."));
            }
            mTorchStrengthCompleter = completer;
        }
    }

    /**
     * Force update the torch state to OFF.
     *
     * <p>This can be invoked when low-light boost is turned on. The torch state will also be
     * updated as {@link TorchState#OFF}.
     */
    @ExecutedBy("mExecutor")
    void forceUpdateTorchStateToOff() {
        // Directly return if torch is originally off
        if (!mTargetTorchEnabled) {
            return;
        }

        mTargetTorchEnabled = false;
        setTorchState(OFF);
    }

    private void setTorchState(@TorchStateInternal int internalState) {
        @TorchState.State int state;
        switch (internalState) {
            case ON:
                state = TorchState.ON;
                break;
            case USED_AS_FLASH:
                // If torch is turned on as flash, it's considered off because it's not used for
                // torch purpose.
                // Fall-through
            case OFF:
                // Fall-through
            default:
                state = TorchState.OFF;
        }
        setLiveDataValue(mTorchState, state);
    }

    private <T> void setLiveDataValue(@NonNull MutableLiveData<T> liveData, T value) {
        if (Threads.isMainThread()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }
}
