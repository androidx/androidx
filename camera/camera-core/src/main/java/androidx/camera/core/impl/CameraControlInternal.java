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
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraControl;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraControlInternal extends CameraControl {

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
     * Starts a flash sequence.
     *
     * @param flashType Uses one shot flash or use torch as flash when taking a picture.
     * @return a {@link ListenableFuture} which completes when the request is completed.
     * Cancelling the ListenableFuture is a no-op.
     */
    @NonNull
    ListenableFuture<Void> startFlashSequence(@ImageCapture.FlashType int flashType);

    /** Cancels AF trigger AND/OR finishes flash sequence.* */
    void cancelAfAndFinishFlashSequence(boolean cancelAfTrigger, boolean finishFlashSequence);

    /**
     * Set a exposure compensation to the camera
     *
     * @param exposure the exposure compensation value to set
     * @return a ListenableFuture which is completed when the new exposure compensation reach the
     * target.
     */
    @NonNull
    @Override
    ListenableFuture<Integer> setExposureCompensationIndex(int exposure);

    /**
     * Performs still capture requests.
     */
    void submitStillCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);

    /**
     * Gets the current SessionConfig.
     *
     * <p>When the SessionConfig is changed,
     * {@link ControlUpdateCallback#onCameraControlUpdateSessionConfig()} will be called to
     * notify the change.
     */
    @NonNull
    SessionConfig getSessionConfig();

    /**
     * Gets the full sensor rect.
     */
    @NonNull
    Rect getSensorRect();

    /**
     * Adds the Interop configuration.
     */
    void addInteropConfig(@NonNull Config config);

    /**
     * Clears the Interop configuration set previously.
     */
    void clearInteropConfig();

    /**
     * Gets the Interop configuration.
     */
    @NonNull
    Config getInteropConfig();

    CameraControlInternal DEFAULT_EMPTY_INSTANCE = new CameraControlInternal() {
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
        public ListenableFuture<Void> startFlashSequence(@ImageCapture.FlashType int flashType) {
            return Futures.immediateFuture(null);
        }

        @Override
        public void cancelAfAndFinishFlashSequence(boolean cancelAfTrigger,
                boolean finishFlashSequence) {
        }

        @NonNull
        @Override
        public ListenableFuture<Integer> setExposureCompensationIndex(int exposure) {
            return Futures.immediateFuture(0);
        }

        @Override
        public void submitStillCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        }

        @NonNull
        @Override
        public SessionConfig getSessionConfig() {
            return SessionConfig.defaultEmptySessionConfig();
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

        @Override
        public void addInteropConfig(@NonNull Config config) {
        }

        @Override
        public void clearInteropConfig() {
        }

        @NonNull
        @Override
        public Config getInteropConfig() {
            return null;
        }
    };

    /** Listener called when CameraControlInternal need to notify event. */
    interface ControlUpdateCallback {

        /**
         * Called when CameraControlInternal has updated session configuration.
         *
         * <p>The latest SessionConfig can be obtained by calling {@link #getSessionConfig()}.
         */
        void onCameraControlUpdateSessionConfig();

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
