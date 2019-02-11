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
import android.os.Handler;

import androidx.annotation.Nullable;

/**
 * The CameraControl Interface.
 *
 * <p>CameraControl is used for global camera operations like zoom, focus, flash and triggering
 * AF/AE. To control the global camera status across different UseCases,
 * getSingleRequestImplOptions() is used to attach the common request parameter to all SINGLE
 * CaptureRequests and getControlSessionConfiguration() is used to hook a {@link
 * SessionConfiguration} alongside with other use cases to determine the final sessionConfiguration.
 * A CameraControl implementation can use getControlSessionConfiguration to modify parameter for
 * repeating request or add a listener to check the capture result.
 *
 * @hide
 */
public interface CameraControl {
    static CameraControl defaultEmptyInstance() {
        return new CameraControl() {
            @Override
            public void setCropRegion(Rect crop) {
            }

            @Override
            public void focus(
                    Rect focus,
                    Rect metering,
                    @Nullable OnFocusCompletedListener listener,
                    @Nullable Handler handler) {
            }

            @Override
            public FlashMode getFlashMode() {
                return null;
            }

            @Override
            public void setFlashMode(FlashMode flashMode) {
            }

            @Override
            public void enableTorch(boolean torch) {
            }

            @Override
            public boolean isTorchOn() {
                return false;
            }

            @Override
            public boolean isFocusLocked() {
                return false;
            }

            @Override
            public void triggerAf() {
            }

            @Override
            public void triggerAePrecapture() {
            }

            @Override
            public void cancelAfAeTrigger(
                    boolean cancelAfTrigger, boolean cancelAePrecaptureTrigger) {
            }

            @Override
            public SessionConfiguration getControlSessionConfiguration() {
                return SessionConfiguration.defaultEmptySessionConfiguration();
            }

            @Override
            public Configuration getSingleRequestImplOptions() {
                return OptionsBundle.emptyBundle();
            }
        };
    }

    /**
     * Set the desired crop region of the sensor to read out for all capture requests.
     *
     * <p>This crop region can be used to implement digital zoom. It is applied to every single and
     * re peating requests.
     *
     * @param crop rectangle with dimensions in sensor pixel coordinate.
     */
    void setCropRegion(Rect crop);

    /**
     * Adjusts the camera output according to the properties in some local regions with a callback
     * called once focus scan has completed.
     *
     * <p>The auto-focus (AF), auto-exposure (AE) and auto-whitebalance (AWB) properties will be
     * recalculated from the local regions.
     *
     * @param focus    rectangle with dimensions in sensor coordinate frame for focus
     * @param metering rectangle with dimensions in sensor coordinate frame for metering
     * @param listener listener for when focus has completed
     * @param handler  the handler where the listener will execute.
     */
    void focus(
            Rect focus,
            Rect metering,
            @Nullable OnFocusCompletedListener listener,
            @Nullable Handler handler);

    default void focus(Rect focus, Rect metering) {
        focus(focus, metering, null, null);
    }

    FlashMode getFlashMode();

    /**
     * Sets current flash mode
     *
     * @param flashMode the {@link FlashMode}.
     */
    void setFlashMode(FlashMode flashMode);

    /**
     * Enable the torch or disable the torch
     *
     * @param torch true to open the torch, false to close it.
     */
    void enableTorch(boolean torch);

    /** Returns if current torch is enabled or not. */
    boolean isTorchOn();

    boolean isFocusLocked();

    /** Performs a AF trigger. */
    void triggerAf();

    /** Performs a AE Precapture trigger. */
    void triggerAePrecapture();

    /** Cancel AF trigger AND/OR AE Precapture trigger.* */
    void cancelAfAeTrigger(boolean cancelAfTrigger, boolean cancelAePrecaptureTrigger);

    /**
     * Hooks a SessionConfiguration into the final SessionConfiguration ValidatingBuilder.
     * CameraControl can modify SessionConfiguration to add implementation options or add a listener
     * to check the capture result.
     */
    SessionConfiguration getControlSessionConfiguration();

    /** Attaches the common request implementation options to every SINGLE requests. */
    Configuration getSingleRequestImplOptions();
}
