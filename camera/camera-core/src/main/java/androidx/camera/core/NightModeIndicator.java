/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines the valid states for Night Mode Indicator, as returned by
 * {@link CameraInfo#getNightModeIndicator}.
 *
 * <p>These states indicate whether the current environment conditions meet the criteria for Night
 * Mode.
 */
public final class NightModeIndicator {

    /**
     * Night mode indicator is unknown.
     *
     * <p>Scene lighting cannot yet be accurately assessed. Apps should generally hide any
     * suggestion UI in this state.
     */
    public static final int UNKNOWN = 0;

    /**
     * Night mode is not recommended.
     *
     * <p>Lighting conditions are sufficiently bright. The environment conditions do not meet the
     * criteria for Night Mode.
     */
    public static final int NOT_RECOMMENDED = 1;

    /**
     * Night mode is recommended.
     *
     * <p>Low-light conditions detected. The environment conditions meet the criteria for Night
     * Mode.
     */
    public static final int RECOMMENDED = 2;

    private NightModeIndicator() {
    }

    /**
     * Night Mode Indicator State.
     */
    @IntDef({UNKNOWN, NOT_RECOMMENDED, RECOMMENDED})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface State {
    }
}
