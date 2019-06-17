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

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;
import android.util.Log;

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
import androidx.camera.core.OnFocusListener;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A Camera2 implementation for CameraControlInternal interface
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Camera2CameraControl implements CameraControlInternal {
    private static final long DEFAULT_FOCUS_TIMEOUT_MS = 5000;
    private static final String TAG = "Camera2CameraControl";
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final CameraControlSessionCallback mSessionCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mExecutor;
    private final ControlUpdateListener mControlUpdateListener;
    private final ScheduledExecutorService mScheduler;
    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();

    // use volatile modifier to make these variables in sync in all threads.
    private volatile boolean mIsTorchOn = false;
    private volatile boolean mIsFocusLocked = false;
    private volatile FlashMode mFlashMode = FlashMode.OFF;

    //******************** Should only be accessed by executor *****************************//
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CaptureResultListener mSessionListenerForFocus = null;
    private Rect mCropRect = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    MeteringRectangle mAfRect;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    MeteringRectangle mAeRect;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    MeteringRectangle mAwbRect;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Integer mCurrentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mFocusTimeoutCounter = 0;
    private long mFocusTimeoutMs;
    private ScheduledFuture<?> mFocusTimeoutHandle;
    //**************************************************************************************//


    public Camera2CameraControl(@NonNull ControlUpdateListener controlUpdateListener,
            @NonNull ScheduledExecutorService scheduler, @NonNull Executor executor) {
        this(controlUpdateListener, DEFAULT_FOCUS_TIMEOUT_MS, scheduler, executor);
    }

    public Camera2CameraControl(
            @NonNull ControlUpdateListener controlUpdateListener,
            long focusTimeoutMs,
            @NonNull ScheduledExecutorService scheduler,
            @NonNull Executor executor) {
        mControlUpdateListener = controlUpdateListener;
        if (CameraXExecutors.isSequentialExecutor(executor)) {
            mExecutor = executor;
        } else {
            mExecutor = CameraXExecutors.newSequentialExecutor(executor);
        }
        mScheduler = scheduler;
        mFocusTimeoutMs = focusTimeoutMs;

        mSessionCallback = new CameraControlSessionCallback(mExecutor);

        mSessionConfigBuilder.setTemplateType(getDefaultTemplate());
        mSessionConfigBuilder.addRepeatingCameraCaptureCallback(
                CaptureCallbackContainer.create(mSessionCallback));

        // Initialize the session config
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                updateSessionConfig();
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

    /** {@inheritDoc} */
    @SuppressLint("LambdaLast") // Remove after https://issuetracker.google.com/135275901
    @Override
    public void focus(
            @NonNull final Rect focus,
            @NonNull final Rect metering,
            @NonNull final Executor userListenerExecutor,
            @NonNull final OnFocusListener listener) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                focusInternal(focus, metering, userListenerExecutor, listener);
            }
        });
    }

    @Override
    public void focus(@NonNull Rect focus, @NonNull Rect metering) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Listener executor won't be called, so its ok use pass direct executor here.
                focusInternal(focus, metering, CameraXExecutors.directExecutor(), null);
            }
        });
    }

    /** Cancels the focus operation. */
    @VisibleForTesting
    void cancelFocus() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                cancelFocusInternal();
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

    @Override
    public boolean isFocusLocked() {
        return mIsFocusLocked;
    }

    /**
     * Issues a {@link CaptureRequest#CONTROL_AF_TRIGGER_START} request to start auto focus scan.
     */
    @Override
    public void triggerAf() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                triggerAfInternal();
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
                triggerAePrecaptureInternal();
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
                cancelAfAeTriggerInternal(cancelAfTrigger, cancelAePrecaptureTrigger);
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

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void focusInternal(
            final Rect focus,
            final Rect metering,
            @NonNull final Executor listenerExecutor,
            @Nullable final OnFocusListener listener) {
        mSessionCallback.removeListener(mSessionListenerForFocus);

        cancelFocusTimeout();

        mAfRect = new MeteringRectangle(focus, MeteringRectangle.METERING_WEIGHT_MAX);
        mAeRect = new MeteringRectangle(metering, MeteringRectangle.METERING_WEIGHT_MAX);
        mAwbRect = new MeteringRectangle(metering, MeteringRectangle.METERING_WEIGHT_MAX);
        Log.d(TAG, "Setting new AF rectangle: " + mAfRect);
        Log.d(TAG, "Setting new AE rectangle: " + mAeRect);
        Log.d(TAG, "Setting new AWB rectangle: " + mAwbRect);

        mCurrentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
        mIsFocusLocked = true;

        if (listener != null) {

            mSessionListenerForFocus =
                    new CaptureResultListener() {
                        // Will be called on mExecutor since mSessionCallback was created with
                        // mExecutor
                        @WorkerThread
                        @Override
                        public boolean onCaptureResult(TotalCaptureResult result) {
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == null) {
                                return false;
                            }

                            if (mCurrentAfState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
                                if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                                    listenerExecutor.execute(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    listener.onFocusLocked(mAfRect.getRect());
                                                }
                                            });
                                    return true; // finished
                                } else if (afState
                                        == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                    listenerExecutor.execute(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    listener.onFocusUnableToLock(
                                                            mAfRect.getRect());
                                                }
                                            });
                                    return true; // finished
                                }
                            }
                            if (!mCurrentAfState.equals(afState)) {
                                mCurrentAfState = afState;
                            }
                            return false; // continue checking
                        }
                    };

            mSessionCallback.addListener(mSessionListenerForFocus);
        }
        updateSessionConfig();

        triggerAfInternal();
        if (mFocusTimeoutMs > 0) {
            final long timeoutId = ++mFocusTimeoutCounter;
            final Runnable timeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    mExecutor.execute(new Runnable() {
                        @WorkerThread
                        @Override
                        public void run() {
                            if (timeoutId == mFocusTimeoutCounter) {
                                cancelFocusInternal();

                                mSessionCallback.removeListener(mSessionListenerForFocus);

                                if (listener != null
                                        && mCurrentAfState
                                        == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
                                    listenerExecutor.execute(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    listener.onFocusTimedOut(mAfRect.getRect());
                                                }
                                            });
                                }
                            }
                        }
                    });
                }
            };

            mFocusTimeoutHandle = mScheduler.schedule(timeoutRunnable, mFocusTimeoutMs,
                    TimeUnit.MILLISECONDS);
        }
    }

    @WorkerThread
    private void cancelFocusTimeout() {
        if (mFocusTimeoutHandle != null) {
            mFocusTimeoutHandle.cancel(/*mayInterruptIfRunning=*/true);
            mFocusTimeoutHandle = null;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void cancelFocusInternal() {
        cancelFocusTimeout();

        MeteringRectangle zeroRegion =
                new MeteringRectangle(new Rect(), MeteringRectangle.METERING_WEIGHT_DONT_CARE);
        mAfRect = zeroRegion;
        mAeRect = zeroRegion;
        mAwbRect = zeroRegion;

        // Send a single request to cancel af process
        CaptureConfig.Builder singleRequestBuilder = createCaptureBuilderWithSharedOptions();
        singleRequestBuilder.setTemplateType(getDefaultTemplate());
        singleRequestBuilder.setUseRepeatingSurface(true);
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        singleRequestBuilder.addImplementationOptions(configBuilder.build());
        notifyCaptureRequests(Collections.singletonList(singleRequestBuilder.build()));

        mIsFocusLocked = false;
        updateSessionConfig();
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
                    CaptureRequest.CONTROL_AE_MODE_ON);
            configBuilder.setCaptureRequestOption(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF);
            singleRequestBuilder.addImplementationOptions(configBuilder.build());
            notifyCaptureRequests(Collections.singletonList(singleRequestBuilder.build()));
        }
        updateSessionConfig();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void triggerAfInternal() {
        CaptureConfig.Builder builder = createCaptureBuilderWithSharedOptions();
        builder.setTemplateType(getDefaultTemplate());
        builder.setUseRepeatingSurface(true);
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        builder.addImplementationOptions(configBuilder.build());
        notifyCaptureRequests(Collections.singletonList(builder.build()));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void triggerAePrecaptureInternal() {
        CaptureConfig.Builder builder = createCaptureBuilderWithSharedOptions();
        builder.setTemplateType(getDefaultTemplate());
        builder.setUseRepeatingSurface(true);
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        builder.addImplementationOptions(configBuilder.build());
        notifyCaptureRequests(Collections.singletonList(builder.build()));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void cancelAfAeTriggerInternal(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        CaptureConfig.Builder builder = createCaptureBuilderWithSharedOptions();
        builder.setUseRepeatingSurface(true);
        builder.setTemplateType(getDefaultTemplate());

        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        if (cancelAfTrigger) {
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        }
        if (Build.VERSION.SDK_INT >= 23 && cancelAePrecaptureTrigger) {
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
        }
        builder.addImplementationOptions(configBuilder.build());
        notifyCaptureRequests(Collections.singletonList(builder.build()));
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

        builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                isFocusLocked()
                        ? CaptureRequest.CONTROL_AF_MODE_AUTO
                        : CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

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
        builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, aeMode);

        builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

        if (mAfRect != null) {
            builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{mAfRect});
        }
        if (mAeRect != null) {
            builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{mAeRect});
        }
        if (mAwbRect != null) {
            builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_REGIONS, new MeteringRectangle[]{mAwbRect});
        }

        if (mCropRect != null) {
            builder.setCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, mCropRect);
        }

        return builder.build();
    }

    /** An interface to listen to camera capture results. */
    private interface CaptureResultListener {
        /**
         * Callback to handle camera capture results.
         *
         * @param captureResult camera capture result.
         * @return true to finish listening, false to continue listening.
         */
        boolean onCaptureResult(TotalCaptureResult captureResult);
    }

    static final class CameraControlSessionCallback extends CaptureCallback {

        /* synthetic accessor */final Set<CaptureResultListener> mResultListeners = new HashSet<>();
        private final Executor mExecutor;

        CameraControlSessionCallback(@NonNull Executor executor) {
            mExecutor = executor;
        }

        @WorkerThread
        void addListener(CaptureResultListener listener) {
            mResultListeners.add(listener);
        }

        @WorkerThread
        void removeListener(CaptureResultListener listener) {
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
