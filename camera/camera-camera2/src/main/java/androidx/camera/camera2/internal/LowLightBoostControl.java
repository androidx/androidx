/*
 * Copyright 2024 The Android Open Source Project
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

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY;
import static android.hardware.camera2.CaptureResult.CONTROL_LOW_LIGHT_BOOST_STATE;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraControl;
import androidx.camera.core.Logger;
import androidx.camera.core.LowLightBoostState;
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

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of low-light boost control used within CameraControl and CameraInfo.
 *
 * It is used to control the low-light boost mode of camera device that
 * {@link Camera2CameraControlImpl} operates. The low-light boost control must be activated via
 * {@link #setActive(boolean)} when the camera device is ready to do low-light boost operations
 * and be deactivated when the camera device is closing or closed.
 */
final class LowLightBoostControl {
    private static final String TAG = "LowLightBoostControl";
    static final int DEFAULT_LLB_STATE = LowLightBoostState.OFF;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private final Camera2CameraControlImpl mCamera2CameraControlImpl;
    private final MutableLiveData<Integer> mLowLightBoostState;
    private final AtomicInteger mLowLightBoostStateAtomic = new AtomicInteger(
            LowLightBoostState.OFF);
    private final boolean mIsLowLightBoostSupported;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private boolean mLowLightBoostDisabledByUseCaseSessionConfig = false;
    @CameraExecutor
    private final Executor mExecutor;

    private boolean mIsActive;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            CallbackToFutureAdapter.Completer<Void> mEnableLlbCompleter;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            boolean mTargetLlbEnabled;

    @VisibleForTesting
    final Camera2CameraControlImpl.CaptureResultListener mCaptureResultListener;

    /**
     * Constructs a LowLightBoostControl.
     *
     * @param camera2CameraControlImpl the camera control this LowLightBoostControl belongs.
     * @param cameraCharacteristics    the characteristics for the camera being controlled.
     * @param executor                 the camera executor used to run camera task.
     */
    LowLightBoostControl(@NonNull Camera2CameraControlImpl camera2CameraControlImpl,
            @NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @CameraExecutor @NonNull Executor executor) {
        mCamera2CameraControlImpl = camera2CameraControlImpl;
        mExecutor = executor;

        mIsLowLightBoostSupported = checkLowLightBoostAvailability(cameraCharacteristics);
        mLowLightBoostState = new MutableLiveData<>(DEFAULT_LLB_STATE);

        mCaptureResultListener = captureResult -> {
            if (mEnableLlbCompleter != null) {
                CaptureRequest captureRequest = captureResult.getRequest();
                Integer aeMode = captureRequest.get(CaptureRequest.CONTROL_AE_MODE);

                // Skips the check if capture result doesn't contain AE mode related info.
                if (aeMode == null) {
                    return false;
                }

                // mTargetLlbEnabled might be either true or false.
                // - When mTargetLlbEnabled is true: complete the completer when
                // AE mode becomes CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY.
                // - When mTargetLlbEnabled is false: complete the completer when
                // AE mode becomes non-CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY.
                boolean llbEnabled =
                        aeMode == CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY;
                if (llbEnabled == mTargetLlbEnabled) {
                    mEnableLlbCompleter.set(null);
                    mEnableLlbCompleter = null;
                } else {
                    return false;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                    && mTargetLlbEnabled) {
                Integer currentState = captureResult.get(CONTROL_LOW_LIGHT_BOOST_STATE);
                if (currentState != null) {
                    setLiveDataValue(mLowLightBoostState, currentState);
                }
            }

            // Return false to keep getting captureResult.
            return false;
        };

        if (mIsLowLightBoostSupported) {
            mCamera2CameraControlImpl.addCaptureResultListener(mCaptureResultListener);
        }
    }

    /**
     * Set current active state. Set active if it is ready to do low-light boost operations.
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
            if (mTargetLlbEnabled) {
                mTargetLlbEnabled = false;
                mCamera2CameraControlImpl.enableLowLightBoostInternal(false);
                setLiveDataValue(mLowLightBoostState, LowLightBoostState.OFF);
            }

            if (mEnableLlbCompleter != null) {
                mEnableLlbCompleter.setException(
                        new CameraControl.OperationCanceledException("Camera is not active."));
                mEnableLlbCompleter = null;
            }
        }
    }

    @ExecutedBy("mExecutor")
    void setLowLightBoostDisabledByUseCaseSessionConfig(boolean disabled) {
        synchronized (mLock) {
            mLowLightBoostDisabledByUseCaseSessionConfig = disabled;

            if (!mLowLightBoostDisabledByUseCaseSessionConfig) {
                return;
            }
        }

        if (mTargetLlbEnabled) {
            mTargetLlbEnabled = false;
            mCamera2CameraControlImpl.enableLowLightBoostInternal(false);
            setLiveDataValue(mLowLightBoostState, LowLightBoostState.OFF);

            if (mEnableLlbCompleter != null) {
                mEnableLlbCompleter.setException(new IllegalStateException(
                        "Low-light boost is disabled when expected frame rate range exceeds 30 or"
                                + " HDR 10-bit is on."));
                mEnableLlbCompleter = null;
            }
        }
    }

    boolean isLowLightBoostDisabledByUseCaseSessionConfig() {
        synchronized (mLock) {
            return mLowLightBoostDisabledByUseCaseSessionConfig;
        }
    }

    /**
     * Enable or disable the low-light boost.
     *
     * <p>The returned {@link ListenableFuture} will succeed when the request is sent to camera
     * device. But it may get an {@link CameraControl.OperationCanceledException} result when:
     * <ol>
     * <li>There are multiple {@code enableLowLightBoost(boolean)} requests in the same time, the
     * older and incomplete futures will get cancelled.
     * <li>When the LowLightBoostControl is set to inactive.
     * </ol>
     *
     * <p>The returned {@link ListenableFuture} will fail immediately when:
     * <ol>
     * <li>The LowLightBoostControl is not in active state.
     * <li>The camera doesn't support low-light boost mode.
     * </ol>
     *
     * @param enabled true to open the low-light boost, false to close it.
     * @return A {@link ListenableFuture} which is successful when the low-light boost was changed
     * to the value specified. It fails when it is unable to change the low-light boost state.
     */
    ListenableFuture<Void> enableLowLightBoost(boolean enabled) {
        if (!mIsLowLightBoostSupported) {
            Logger.d(TAG, "Unable to enable low-light boost due to it is not supported.");
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Low-light boost is not supported"));
        }

        setLiveDataValue(mLowLightBoostState,
                enabled ? LowLightBoostState.INACTIVE : LowLightBoostState.OFF);

        return CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(() -> enableLowLightBoostInternal(completer, enabled));
            return "enableLowLightBoost: " + enabled;
        });
    }

    /**
     * Returns a {@link LiveData} of current {@link LowLightBoostState}.
     *
     * <p>The low-light boost state can be enabled or disabled via
     * {@link #enableLowLightBoost(boolean)} which will trigger the change event to the returned
     * {@link LiveData}.
     *
     * @return a {@link LiveData} containing current low-light boost state.
     */
    @NonNull
    LiveData<Integer> getLowLightBoostState() {
        return mLowLightBoostState;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void enableLowLightBoostInternal(@Nullable Completer<Void> completer, boolean enabled) {
        synchronized (mLock) {
            if (mLowLightBoostDisabledByUseCaseSessionConfig) {
                setLiveDataValue(mLowLightBoostState, LowLightBoostState.OFF);
                if (completer != null) {
                    completer.setException(new IllegalStateException(
                            "Low-light boost is disabled when expected frame rate range exceeds "
                                    + "30 or HDR 10-bit is on."));
                }
                return;
            }
        }

        if (!mIsActive) {
            setLiveDataValue(mLowLightBoostState, LowLightBoostState.OFF);
            if (completer != null) {
                completer.setException(
                        new CameraControl.OperationCanceledException("Camera is not active."));
            }
            return;
        }

        mTargetLlbEnabled = enabled;
        mCamera2CameraControlImpl.enableLowLightBoostInternal(enabled);
        setLiveDataValue(mLowLightBoostState,
                enabled ? LowLightBoostState.INACTIVE : LowLightBoostState.OFF);
        if (mEnableLlbCompleter != null) {
            mEnableLlbCompleter.setException(new CameraControl.OperationCanceledException(
                    "There is a new enableLowLightBoost being set"));
        }
        mEnableLlbCompleter = completer;
    }

    boolean isLowLightBoostSupported() {
        return mIsLowLightBoostSupported;
    }

    static boolean checkLowLightBoostAvailability(
            @NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return false;
        }

        int[] availableAeModes = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        if (availableAeModes != null) {
            for (int availableAeMode : availableAeModes) {
                if (availableAeMode
                        == CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY) {
                    return true;
                }
            }
        }

        return false;
    }

    private void setLiveDataValue(@NonNull MutableLiveData<Integer> liveData, int value) {
        if (mLowLightBoostStateAtomic.getAndSet(value) != value) {
            if (Threads.isMainThread()) {
                liveData.setValue(value);
            } else {
                liveData.postValue(value);
            }
        }
    }
}
