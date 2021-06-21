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

import androidx.annotation.NonNull;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake implementation for the CameraControlInternal interface which is capable of notifying
 * submitted requests onCaptureCancelled/onCaptureCompleted/onCaptureFailed.
 */
public final class FakeCameraControl implements CameraControlInternal {
    private static final String TAG = "FakeCameraControl";
    private final ControlUpdateCallback mControlUpdateCallback;
    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    @ImageCapture.FlashMode
    private int mFlashMode = FLASH_MODE_OFF;
    private ArrayList<CaptureConfig> mSubmittedCaptureRequests = new ArrayList<>();
    private OnNewCaptureRequestListener mOnNewCaptureRequestListener;
    private MutableOptionsBundle mInteropConfig = MutableOptionsBundle.create();

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
        mSubmittedCaptureRequests.clear();
    }

    /** Notifies all submitted requests onCaptureCompleted */
    public void notifyAllRequestsOnCaptureCompleted(CameraCaptureResult result) {
        for (CaptureConfig captureConfig : mSubmittedCaptureRequests) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    captureConfig.getCameraCaptureCallbacks()) {
                cameraCaptureCallback.onCaptureCompleted(result);
            }
        }
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
    @NonNull
    public ListenableFuture<Void> enableTorch(boolean torch) {
        Logger.d(TAG, "enableTorch(" + torch + ")");
        return Futures.immediateFuture(null);
    }

    @Override
    @NonNull
    public ListenableFuture<CameraCaptureResult> triggerAf() {
        Logger.d(TAG, "triggerAf()");
        return Futures.immediateFuture(CameraCaptureResult.EmptyCameraCaptureResult.create());
    }

    @Override
    @NonNull
    public ListenableFuture<CameraCaptureResult> triggerAePrecapture() {
        Logger.d(TAG, "triggerAePrecapture()");
        return Futures.immediateFuture(CameraCaptureResult.EmptyCameraCaptureResult.create());
    }

    @Override
    public void cancelAfAeTrigger(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        Logger.d(TAG, "cancelAfAeTrigger(" + cancelAfTrigger + ", "
                + cancelAePrecaptureTrigger + ")");
    }

    @NonNull
    @Override
    public ListenableFuture<Integer> setExposureCompensationIndex(int exposure) {
        return Futures.immediateFuture(null);
    }

    @Override
    public void submitCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        mSubmittedCaptureRequests.addAll(captureConfigs);
        mControlUpdateCallback.onCameraControlCaptureRequests(captureConfigs);
        if (mOnNewCaptureRequestListener != null) {
            mOnNewCaptureRequestListener.onNewCaptureRequests(captureConfigs);
        }
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
        return Futures.immediateFuture(null);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        return Futures.immediateFuture(null);
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
        return mInteropConfig;
    }

    /** A listener which are used to notify when there are new submitted capture requests */
    public interface OnNewCaptureRequestListener {
        /** Called when there are new submitted capture request */
        void onNewCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);
    }
}
