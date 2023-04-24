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

package androidx.camera.testing.fakes;

import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE;

import android.graphics.Rect;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation for the CameraControlInternal interface which is capable of notifying
 * submitted requests onCaptureCancelled/onCaptureCompleted/onCaptureFailed.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class FakeCameraControl implements CameraControlInternal {
    private static final String TAG = "FakeCameraControl";
    private static final ControlUpdateCallback NO_OP_CALLBACK = new ControlUpdateCallback() {
        @Override
        public void onCameraControlUpdateSessionConfig() {
            // No-op
        }

        @Override
        public void onCameraControlCaptureRequests(
                @NonNull List<CaptureConfig> captureConfigs) {
            // No-op
        }
    };

    private final ControlUpdateCallback mControlUpdateCallback;
    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    @ImageCapture.FlashMode
    private int mFlashMode = FLASH_MODE_OFF;
    private ArrayList<CaptureConfig> mSubmittedCaptureRequests = new ArrayList<>();
    private OnNewCaptureRequestListener mOnNewCaptureRequestListener;
    private MutableOptionsBundle mInteropConfig = MutableOptionsBundle.create();
    private final ArrayList<CallbackToFutureAdapter.Completer<Void>> mSubmittedCompleterList =
            new ArrayList<>();

    private boolean mIsZslDisabledByUseCaseConfig = false;
    private boolean mIsZslConfigAdded = false;
    private float mZoomRatio = -1;
    private float mLinearZoom = -1;
    private boolean mTorchEnabled = false;
    private int mExposureCompensation = -1;

    @Nullable
    private FocusMeteringAction mLastSubmittedFocusMeteringAction = null;

    public FakeCameraControl() {
        this(NO_OP_CALLBACK);
    }

    public FakeCameraControl(@NonNull ControlUpdateCallback controlUpdateCallback) {
        mControlUpdateCallback = controlUpdateCallback;
    }

    /** Notifies all submitted requests onCaptureCancelled */
    public void notifyAllRequestOnCaptureCancelled() {
        for (CaptureConfig captureConfig : mSubmittedCaptureRequests) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    captureConfig.getCameraCaptureCallbacks()) {
                cameraCaptureCallback.onCaptureCancelled();
            }
        }
        for (CallbackToFutureAdapter.Completer<Void> completer : mSubmittedCompleterList) {
            completer.setException(
                    new ImageCaptureException(ImageCapture.ERROR_CAMERA_CLOSED, "Simulate "
                            + "capture cancelled", null));
        }
        mSubmittedCompleterList.clear();
        mSubmittedCaptureRequests.clear();
    }

    /** Notifies all submitted requests onCaptureFailed */
    public void notifyAllRequestsOnCaptureFailed() {
        for (CaptureConfig captureConfig : mSubmittedCaptureRequests) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    captureConfig.getCameraCaptureCallbacks()) {
                cameraCaptureCallback.onCaptureFailed(new CameraCaptureFailure(
                        CameraCaptureFailure.Reason.ERROR));
            }
        }
        for (CallbackToFutureAdapter.Completer<Void> completer : mSubmittedCompleterList) {
            completer.setException(new ImageCaptureException(ImageCapture.ERROR_CAPTURE_FAILED,
                    "Simulate capture fail", null));
        }
        mSubmittedCompleterList.clear();
        mSubmittedCaptureRequests.clear();
    }

    /** Notifies all submitted requests onCaptureCompleted */
    public void notifyAllRequestsOnCaptureCompleted(@NonNull CameraCaptureResult result) {
        for (CaptureConfig captureConfig : mSubmittedCaptureRequests) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    captureConfig.getCameraCaptureCallbacks()) {
                cameraCaptureCallback.onCaptureCompleted(result);
            }
        }
        for (CallbackToFutureAdapter.Completer<Void> completer : mSubmittedCompleterList) {
            completer.set(null);
        }
        mSubmittedCompleterList.clear();
        mSubmittedCaptureRequests.clear();
    }

    @ImageCapture.FlashMode
    @Override
    public int getFlashMode() {
        return mFlashMode;
    }

    @Override
    public void setFlashMode(@ImageCapture.FlashMode int flashMode) {
        mFlashMode = flashMode;
        Logger.d(TAG, "setFlashMode(" + mFlashMode + ")");
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
        mIsZslDisabledByUseCaseConfig = disabled;
    }

    @Override
    public boolean isZslDisabledByByUserCaseConfig() {
        return mIsZslDisabledByUseCaseConfig;
    }

    @Override
    public void addZslConfig(@NonNull SessionConfig.Builder sessionConfigBuilder) {
        // Override if Zero-Shutter Lag needs to add config to session config.
        mIsZslConfigAdded = true;
    }

    /**
     * Checks if {@link FakeCameraControl#addZslConfig(Size, SessionConfig.Builder)} is
     * triggered. Only for testing purpose.
     */
    public boolean isZslConfigAdded() {
        return mIsZslConfigAdded;
    }

    @Override
    @NonNull
    public ListenableFuture<Void> enableTorch(boolean torch) {
        Logger.d(TAG, "enableTorch(" + torch + ")");
        mTorchEnabled = torch;
        return Futures.immediateFuture(null);
    }

    public boolean getTorchEnabled() {
        return mTorchEnabled;
    }

    @NonNull
    @Override
    public ListenableFuture<Integer> setExposureCompensationIndex(int exposure) {
        mExposureCompensation = exposure;
        return Futures.immediateFuture(null);
    }

    public int getExposureCompensationIndex() {
        return mExposureCompensation;
    }

    @NonNull
    @Override
    public ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            int captureMode, int flashType) {
        mSubmittedCaptureRequests.addAll(captureConfigs);
        mControlUpdateCallback.onCameraControlCaptureRequests(captureConfigs);
        List<ListenableFuture<Void>> fakeFutures = new ArrayList<>();
        for (int i = 0; i < captureConfigs.size(); i++) {
            fakeFutures.add(CallbackToFutureAdapter.getFuture(completer -> {
                mSubmittedCompleterList.add(completer);
                return "fakeFuture";
            }));
        }

        if (mOnNewCaptureRequestListener != null) {
            mOnNewCaptureRequestListener.onNewCaptureRequests(captureConfigs);
        }
        return Futures.allAsList(fakeFutures);
    }

    @NonNull
    @Override
    public SessionConfig getSessionConfig() {
        return mSessionConfigBuilder.build();
    }

    @NonNull
    @Override
    public Rect getSensorRect() {
        return new Rect(0, 0, MAX_OUTPUT_SIZE.getWidth(), MAX_OUTPUT_SIZE.getHeight());
    }

    @NonNull
    @Override
    public ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        mLastSubmittedFocusMeteringAction = action;
        return Futures.immediateFuture(FocusMeteringResult.emptyInstance());
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelFocusAndMetering() {
        return Futures.immediateFuture(null);
    }

    /** Sets a listener to be notified when there are new capture request submitted */
    public void setOnNewCaptureRequestListener(@NonNull OnNewCaptureRequestListener listener) {
        mOnNewCaptureRequestListener = listener;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setZoomRatio(float ratio) {
        mZoomRatio = ratio;
        return Futures.immediateFuture(null);
    }

    public float getZoomRatio() {
        return mZoomRatio;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        mLinearZoom = linearZoom;
        return Futures.immediateFuture(null);
    }

    public float getLinearZoom() {
        return mLinearZoom;
    }

    @Nullable
    public FocusMeteringAction getLastSubmittedFocusMeteringAction() {
        return mLastSubmittedFocusMeteringAction;
    }

    @Override
    public void addInteropConfig(@NonNull Config config) {
        for (Config.Option<?> option : config.listOptions()) {
            @SuppressWarnings("unchecked")
            Config.Option<Object> objectOpt = (Config.Option<Object>) option;
            mInteropConfig.insertOption(objectOpt, config.retrieveOption(objectOpt));
        }
    }

    @Override
    public void clearInteropConfig() {
        mInteropConfig = MutableOptionsBundle.create();
    }

    @NonNull
    @Override
    public Config getInteropConfig() {
        return MutableOptionsBundle.from(mInteropConfig);
    }

    /** A listener which are used to notify when there are new submitted capture requests */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public interface OnNewCaptureRequestListener {
        /** Called when there are new submitted capture request */
        void onNewCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);
    }
}
