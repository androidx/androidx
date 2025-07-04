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

package androidx.camera.core;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureResult;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines the valid states for Low Light Boost, as returned by
 * {@link CameraInfo#getLowLightBoostState()}.
 *
 * <p>These states indicate whether the camera device supports low light boost and if it is
 * currently active.
 *
 * @see CaptureResult#CONTROL_LOW_LIGHT_BOOST_STATE
 */
public class LowLightBoostState {
    /**
     * Low-light boost is off.
     *
     * <p>This is the default state if low-light boost is unavailable. This may be because your
     * camera doesn't support it or there's a settings conflict.
     */
    public static final int OFF = -1;
    /**
     * Low-light boost is on but inactive.
     *
     * <p>This state indicates that the camera device supports low-light boost but it is not
     * currently active. This can happen if the camera device is not in low-light conditions.
     *
     * @see CameraMetadata#CONTROL_LOW_LIGHT_BOOST_STATE_INACTIVE
     */
    public static final int INACTIVE = 0;
    /**
     * Low-light boost is on and active.
     *
     * <p>This state indicates that the camera device is currently applying low-light boost to
     * the image stream.
     *
     * @see CameraMetadata#CONTROL_LOW_LIGHT_BOOST_STATE_ACTIVE
     */
    public static final int ACTIVE = 1;

    private LowLightBoostState() {
    }

    /**
     */
    @IntDef({OFF, INACTIVE, ACTIVE})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface State {
    }
}
