/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.Config;
import androidx.camera.core.FlashMode;
import androidx.camera.core.OnFocusListener;
import androidx.camera.core.SessionConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Camera2 implementation for CameraControl interface
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Camera2CameraControl implements CameraControl {
    @VisibleForTesting
    static final long FOCUS_TIMEOUT = 5000;
    private static final String TAG = "Camera2CameraControl";
    private final ControlUpdateListener mControlUpdateListener;
    private final Handler mHandler;
    final CameraControlSessionCallback mSessionCallback = new CameraControlSessionCallback();
    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    // use volatile modifier to make these variables in sync in all threads.
    private volatile boolean mIsTorchOn = false;
    private volatile boolean mIsFocusLocked = false;
    private volatile FlashMode mFlashMode = FlashMode.OFF;
    private volatile Rect mCropRect = null;
    volatile MeteringRectangle mAfRect;
    private volatile MeteringRectangle mAeRect;
    private volatile MeteringRectangle mAwbRect;
    volatile Integer mCurrentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
    volatile OnFocusListener mFocusListener = null;
    private volatile Handler mFocusListenerHandler = null;
    volatile CaptureResultListener mSessionListenerForFocus = null;
    private final Runnable mHandleFocusTimeoutRunnable =
            new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.cancelFocus();

                    mSessionCallback.removeListener(mSessionListenerForFocus);

                    if (mFocusListener != null
                            && mCurrentAfState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
                        Camera2CameraControl.this.runInFocusListenerHandler(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mFocusListener.onFocusTimedOut(mAfRect.getRect());
                                    }
                                });
                    }
                }
            };

    public Camera2CameraControl(ControlUpdateListener controlUpdateListener, Handler handler) {
        mControlUpdateListener = controlUpdateListener;
        mHandler = handler;

        mSessionConfigBuilder.setTemplateType(getDefaultTemplate());
        mSessionConfigBuilder.addCameraCaptureCallback(
                CaptureCallbackContainer.create(mSessionCallback));
        updateSessionConfig();
    }

    /** {@inheritDoc} */
    @Override
    public void setCropRegion(final Rect crop) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.setCropRegion(crop);
                }
            });
            return;
        }

        mCropRect = crop;
        updateSessionConfig();
    }

    /** {@inheritDoc} */
    @Override
    public void focus(
            final Rect focus,
            final Rect metering,
            @Nullable final OnFocusListener listener,
            @Nullable final Handler listenerHandler) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.focus(focus, metering, listener, listenerHandler);
                }
            });
            return;
        }

        mSessionCallback.removeListener(mSessionListenerForFocus);

        mHandler.removeCallbacks(mHandleFocusTimeoutRunnable);

        mAfRect = new MeteringRectangle(focus, MeteringRectangle.METERING_WEIGHT_MAX);
        mAeRect = new MeteringRectangle(metering, MeteringRectangle.METERING_WEIGHT_MAX);
        mAwbRect = new MeteringRectangle(metering, MeteringRectangle.METERING_WEIGHT_MAX);
        Log.d(TAG, "Setting new AF rectangle: " + mAfRect);
        Log.d(TAG, "Setting new AE rectangle: " + mAeRect);
        Log.d(TAG, "Setting new AWB rectangle: " + mAwbRect);

        mFocusListener = listener;
        mFocusListenerHandler =
                (listenerHandler != null ? listenerHandler : new Handler(Looper.getMainLooper()));
        mCurrentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
        mIsFocusLocked = true;

        if (listener != null) {

            mSessionListenerForFocus =
                    new CaptureResultListener() {
                        @Override
                        public boolean onCaptureResult(TotalCaptureResult result) {
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == null) {
                                return false;
                            }

                            if (mCurrentAfState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
                                if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                                    Camera2CameraControl.this.runInFocusListenerHandler(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    mFocusListener.onFocusLocked(mAfRect.getRect());
                                                }
                                            });
                                    return true; // finished
                                } else if (afState
                                        == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                    Camera2CameraControl.this.runInFocusListenerHandler(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    mFocusListener.onFocusUnableToLock(
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

        triggerAf();
        if (FOCUS_TIMEOUT != 0) {
            mHandler.postDelayed(mHandleFocusTimeoutRunnable, FOCUS_TIMEOUT);
        }
    }

    @Override
    public void focus(Rect focus, Rect metering) {
        focus(focus, metering, null, null);
    }

    void runInFocusListenerHandler(Runnable runnable) {
        if (mFocusListenerHandler != null) {
            mFocusListenerHandler.post(runnable);
        }
    }

    /** Cancels the focus operation. */
    @VisibleForTesting
    void cancelFocus() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.cancelFocus();
                }
            });
            return;
        }

        mHandler.removeCallbacks(mHandleFocusTimeoutRunnable);

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

    @Override
    public FlashMode getFlashMode() {
        return mFlashMode;
    }

    /** {@inheritDoc} */
    @Override
    public void setFlashMode(FlashMode flashMode) {
        // update mFlashMode immediately so that following getFlashMode() returns correct value.
        mFlashMode = flashMode;

        updateSessionConfig();
    }

    /** {@inheritDoc} */
    @Override
    public void enableTorch(boolean torch) {
        // update isTorchOn immediately so that following isTorchOn() returns correct value.
        mIsTorchOn = torch;
        enableTorchInternal(torch);
    }

    void enableTorchInternal(final boolean torch) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.enableTorchInternal(torch);
                }
            });
            return;
        }

        if (!torch) {
            CaptureConfig.Builder singleRequestBuilder = createCaptureBuilderWithSharedOptions();
            singleRequestBuilder.setTemplateType(getDefaultTemplate());
            singleRequestBuilder.setUseRepeatingSurface(true);
            Camera2Config.Builder configBuilder = new Camera2Config.Builder();
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
            singleRequestBuilder.addImplementationOptions(configBuilder.build());
            notifyCaptureRequests(Collections.singletonList(singleRequestBuilder.build()));
        }
        updateSessionConfig();
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
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.triggerAf();
                }
            });
            return;
        }

        CaptureConfig.Builder builder = createCaptureBuilderWithSharedOptions();
        builder.setTemplateType(getDefaultTemplate());
        builder.setUseRepeatingSurface(true);
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        builder.addImplementationOptions(configBuilder.build());
        notifyCaptureRequests(Collections.singletonList(builder.build()));
    }

    /**
     * Issues a {@link CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_START} request to start auto
     * exposure scan.
     */
    @Override
    public void triggerAePrecapture() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.triggerAePrecapture();
                }
            });
            return;
        }

        CaptureConfig.Builder builder = createCaptureBuilderWithSharedOptions();
        builder.setTemplateType(getDefaultTemplate());
        builder.setUseRepeatingSurface(true);
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        builder.addImplementationOptions(configBuilder.build());
        notifyCaptureRequests(Collections.singletonList(builder.build()));
    }

    /**
     * Issues {@link CaptureRequest#CONTROL_AF_TRIGGER_CANCEL} or {@link
     * CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL} request to cancel auto focus or auto
     * exposure scan.
     */
    @Override
    public void cancelAfAeTrigger(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.cancelAfAeTrigger(cancelAfTrigger,
                            cancelAePrecaptureTrigger);
                }
            });
            return;
        }
        CaptureConfig.Builder builder = createCaptureBuilderWithSharedOptions();
        builder.setUseRepeatingSurface(true);
        builder.setTemplateType(getDefaultTemplate());

        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        if (cancelAfTrigger) {
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        }
        if (cancelAePrecaptureTrigger) {
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
        }
        builder.addImplementationOptions(configBuilder.build());
        notifyCaptureRequests(Collections.singletonList(builder.build()));
    }

    private int getDefaultTemplate() {
        return CameraDevice.TEMPLATE_PREVIEW;
    }

    void notifyCaptureRequests(final List<CaptureConfig> captureConfigs) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.notifyCaptureRequests(captureConfigs);
                }
            });
            return;
        }
        mControlUpdateListener.onCameraControlCaptureRequests(captureConfigs);
    }

    void updateSessionConfig() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.updateSessionConfig();
                }
            });
            return;
        }
        mSessionConfigBuilder.setImplementationOptions(getSharedOptions());
        mControlUpdateListener.onCameraControlUpdateSessionConfig(mSessionConfigBuilder.build());
    }

    /** {@inheritDoc} */
    @Override
    public void submitCaptureRequests(final List<CaptureConfig> captureConfigs) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera2CameraControl.this.submitCaptureRequests(captureConfigs);
                }
            });
            return;
        }

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
     * Creates a CaptureConfig.Builder contains shared options.
     *
     * @return a {@link CaptureConfig.Builder} contains shared options.
     */
    private CaptureConfig.Builder createCaptureBuilderWithSharedOptions() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.addImplementationOptions(getSharedOptions());
        return builder;
    }

    /**
     * Gets shared options by current status.
     *
     * <p>The shared options are based on the current torch status, flash mode, focus area, crop
     * area, etc... They should be appended to the repeat request and each single capture request.
     */
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
            aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
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

        private final Set<CaptureResultListener> mResultListeners = new HashSet<>();

        public void addListener(CaptureResultListener listener) {
            synchronized (mResultListeners) {
                mResultListeners.add(listener);
            }
        }

        public void removeListener(CaptureResultListener listener) {
            if (listener == null) {
                return;
            }
            synchronized (mResultListeners) {
                mResultListeners.remove(listener);
            }
        }

        @Override
        public void onCaptureCompleted(
                @NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull TotalCaptureResult result) {
            Set<CaptureResultListener> listeners;
            synchronized (mResultListeners) {
                if (mResultListeners.isEmpty()) {
                    return;
                }
                listeners = new HashSet<>(mResultListeners);
            }

            Set<CaptureResultListener> removeSet = new HashSet<>();
            for (CaptureResultListener listener : listeners) {
                boolean isFinished = listener.onCaptureResult(result);
                if (isFinished) {
                    removeSet.add(listener);
                }
            }

            if (!removeSet.isEmpty()) {
                synchronized (mResultListeners) {
                    mResultListeners.removeAll(removeSet);
                }
            }
        }
    }
}
