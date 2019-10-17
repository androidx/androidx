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

package androidx.camera.core;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.List;

/**
 * The CameraControlInternal Interface.
 *
 * <p>CameraControlInternal is used for global camera operations like zoom, focus, flash and
 * triggering
 * AF/AE.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
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
    @NonNull
    FlashMode getFlashMode();

    /**
     * Sets current flash mode
     *
     * @param flashMode the {@link FlashMode}.
     */
    void setFlashMode(@NonNull FlashMode flashMode);

    /**
     * Enable the torch or disable the torch
     *
     * @param torch true to open the torch, false to close it.
     */
    void enableTorch(boolean torch);

    /** Returns if current torch is enabled or not. */
    boolean isTorchOn();

    /** Performs a AF trigger. */
    void triggerAf();

    /** Performs a AE Precapture trigger. */
    void triggerAePrecapture();

    /** Cancel AF trigger AND/OR AE Precapture trigger.* */
    void cancelAfAeTrigger(boolean cancelAfTrigger, boolean cancelAePrecaptureTrigger);

    /**
     * Performs capture requests.
     */
    void submitCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);

    CameraControlInternal DEFAULT_EMPTY_INSTANCE = new CameraControlInternal() {
        @Override
        public void setCropRegion(@Nullable Rect crop) {
        }

        @NonNull
        @Override
        public FlashMode getFlashMode() {
            return FlashMode.OFF;
        }

        @Override
        public void setFlashMode(@NonNull FlashMode flashMode) {
        }

        @Override
        public void enableTorch(boolean torch) {
        }

        @Override
        public boolean isTorchOn() {
            return false;
        }

        @Override
        public void triggerAf() {
        }

        @Override
        public void triggerAePrecapture() {
        }

        @Override
        public void cancelAfAeTrigger(boolean cancelAfTrigger, boolean cancelAePrecaptureTrigger) {

        }

        @Override
        public void submitCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        }

        @Override
        public void startFocusAndMetering(@NonNull FocusMeteringAction action) {

        }

        @Override
        public void cancelFocusAndMetering() {

        }
    };

    /** Listener called when CameraControlInternal need to notify event. */
    interface ControlUpdateCallback {

        /** Called when CameraControlInternal has updated session configuration. */
        void onCameraControlUpdateSessionConfig(@NonNull SessionConfig sessionConfig);

        /** Called when CameraControlInternal need to send capture requests. */
        void onCameraControlCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);
    }
}
