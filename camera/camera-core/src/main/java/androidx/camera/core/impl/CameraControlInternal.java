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
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.ImageCapture.FlashMode;
import androidx.camera.core.ImageCapture.FlashType;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;

/**
 * The CameraControlInternal Interface.
 *
 *
 * <p>CameraControlInternal is used for global camera operations like zoom, focus, flash and
 * triggering AF/AE as well as some internal operations.
 *
 * <p>{@link #getImplementation()} returns a {@link CameraControlInternal} instance
 * that contains the actual implementation and can be cast to an implementation specific class.
 * If the instance itself is the implementation instance, then it should return <code>this</code>.
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
     * Adds zero-shutter lag config to {@link SessionConfig}.
     * @param sessionConfigBuilder session config builder.
     */
    void addZslConfig(@NonNull SessionConfig.Builder sessionConfigBuilder);

    /**
     * Sets the flag if zero-shutter lag needs to be disabled by user case config.
     *
     * <p> Zero-shutter lag will be disabled when any of the following conditions:
     * <ul>
     *     <li> Extension is ON
     *     <li> VideoCapture is ON
     * </ul>
     *
     * @param disabled True if zero-shutter lag should be disabled. Otherwise returns false.
     *                 However, enabling zero-shutter lag needs other conditions e.g. flash mode
     *                 OFF, so setting to false doesn't guarantee zero-shutter lag to be always ON.
     */
    void setZslDisabledByUserCaseConfig(boolean disabled);

    /**
     * Checks if zero-shutter lag is disabled by user case config.
     *
     * @return True if zero-shutter lag should be disabled. Otherwise returns false.
     */
    boolean isZslDisabledByByUserCaseConfig();

    /**
     * Performs still capture requests with the desired capture mode.
     *
     * @param captureConfigs capture configuration used for creating CaptureRequest
     * @param captureMode the mode to capture the image, possible value is
     * {@link ImageCapture#CAPTURE_MODE_MINIMIZE_LATENCY} or
     * {@link ImageCapture#CAPTURE_MODE_MAXIMIZE_QUALITY}
     * @param flashType the options when flash is required for taking a picture.
     * @return ListenableFuture that would be completed while all the captures are completed. It
     * would fail with a {@link androidx.camera.core.ImageCapture#ERROR_CAMERA_CLOSED} when the
     * capture was canceled, or a {@link androidx.camera.core.ImageCapture#ERROR_CAPTURE_FAILED}
     * when the capture was failed.
     */
    @NonNull
    ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            @CaptureMode int captureMode,
            @FlashType int flashType);

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

    /**
     * Gets the underlying implementation instance which could be cast into an implementation
     * specific class for further use in implementation module. Returns <code>this</code> if this
     * instance is the implementation instance.
     */
    @NonNull
    default CameraControlInternal getImplementation() {
        return this;
    }

    CameraControlInternal DEFAULT_EMPTY_INSTANCE = new CameraControlInternal() {
        @FlashMode
        @Override
        public int getFlashMode() {
            return FLASH_MODE_OFF;
        }

        @Override
        public void setFlashMode(@FlashMode int flashMode) {
        }

        @Override
        public void setZslDisabledByUserCaseConfig(boolean disabled) {
        }

        @Override
        public boolean isZslDisabledByByUserCaseConfig() {
            return false;
        }

        @Override
        public void addZslConfig(@NonNull SessionConfig.Builder sessionConfigBuilder) {
        }

        @NonNull
        @Override
        public ListenableFuture<Void> enableTorch(boolean torch) {
            return Futures.immediateFuture(null);
        }

        @NonNull
        @Override
        public ListenableFuture<Integer> setExposureCompensationIndex(int exposure) {
            return Futures.immediateFuture(0);
        }

        @NonNull
        @Override
        public ListenableFuture<List<Void>> submitStillCaptureRequests(
                @NonNull List<CaptureConfig> captureConfigs,
                @CaptureMode int captureMode,
                @FlashType int flashType) {
            return Futures.immediateFuture(Collections.emptyList());
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
