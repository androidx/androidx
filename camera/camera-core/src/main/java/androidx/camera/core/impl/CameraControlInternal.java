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

package androidx.camera.core.impl;

import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraControl;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture.FlashMode;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * The CameraControlInternal Interface.
 *
 * <p>CameraControlInternal is used for global camera operations like zoom, focus, flash and
 * triggering
 * AF/AE.
 */
public interface CameraControlInternal extends CameraControl {
    /**
     * Set the desired crop region of the sensor to read out for all capture requests.
     *
     * <p>This crop region can be used to implement digital zoom. It is applied to every single and
     * re peating requests.
     *
     * @param crop rectangle with dimensions in sensor pixel coordinate.
     */
    void setCropRegion(@Nullable Rect crop);

    /** Returns the current flash mode. */
    @FlashMode
    int getFlashMode();

    /**
     * Sets current flash mode
     *
     * @param flashMode the {@link FlashMode}.
     */
    void setFlashMode(@FlashMode int flashMode);

    /**
     * Performs a AF trigger.
     *
     * @return a {@link ListenableFuture} which completes when the request is completed.
     * Cancelling the ListenableFuture is a no-op.
     */
    @NonNull
    ListenableFuture<CameraCaptureResult> triggerAf();

    /**
     * Performs a AE Precapture trigger.
     *
     * @return a {@link ListenableFuture} which completes when the request is completed.
     * Cancelling the ListenableFuture is a no-op.
     */
    @NonNull
    ListenableFuture<CameraCaptureResult> triggerAePrecapture();

    /** Cancel AF trigger AND/OR AE Precapture trigger.* */
    void cancelAfAeTrigger(boolean cancelAfTrigger, boolean cancelAePrecaptureTrigger);

    /**
     * Performs capture requests.
     */
    void submitCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);

    /**
     * Gets the full sensor rect.
     */
    @NonNull
    Rect getSensorRect();

    CameraControlInternal DEFAULT_EMPTY_INSTANCE = new CameraControlInternal() {
        @Override
        public void setCropRegion(@Nullable Rect crop) {
        }

        @FlashMode
        @Override
        public int getFlashMode() {
            return FLASH_MODE_OFF;
        }

        @Override
        public void setFlashMode(@FlashMode int flashMode) {
        }

        @NonNull
        @Override
        public ListenableFuture<Void> enableTorch(boolean torch) {
            return Futures.immediateFuture(null);
        }

        @Override
        @NonNull
        public ListenableFuture<CameraCaptureResult> triggerAf() {
            return Futures.immediateFuture(CameraCaptureResult.EmptyCameraCaptureResult.create());
        }

        @Override
        @NonNull
        public ListenableFuture<CameraCaptureResult> triggerAePrecapture() {
            return Futures.immediateFuture(CameraCaptureResult.EmptyCameraCaptureResult.create());
        }

        @Override
        public void cancelAfAeTrigger(boolean cancelAfTrigger, boolean cancelAePrecaptureTrigger) {

        }

        @Override
        public void submitCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        }

        @NonNull
        @Override
        public Rect getSensorRect() {
            return new Rect();
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
    };

    /** Listener called when CameraControlInternal need to notify event. */
    interface ControlUpdateCallback {

        /** Called when CameraControlInternal has updated session configuration. */
        void onCameraControlUpdateSessionConfig(@NonNull SessionConfig sessionConfig);

        /** Called when CameraControlInternal need to send capture requests. */
        void onCameraControlCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);
    }

    /**
     * An exception thrown when the camera control is failed to execute the request.
     */
    final class CameraControlException extends Exception {
        @NonNull
        private CameraCaptureFailure mCameraCaptureFailure;
        public CameraControlException(@NonNull CameraCaptureFailure failure) {
            super();
            mCameraCaptureFailure = failure;
        }

        public CameraControlException(@NonNull CameraCaptureFailure failure,
                @NonNull Throwable cause) {
            super(cause);
            mCameraCaptureFailure = failure;
        }

        @NonNull
        public CameraCaptureFailure getCameraCaptureFailure() {
            return mCameraCaptureFailure;
        }
    }
}
