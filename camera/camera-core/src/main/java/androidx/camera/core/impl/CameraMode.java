/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The available camera modes.
 *
 * <p>Camera devices might work in different camera modes:
 * <ul>
 *   <li> Default mode
 *   <li> Concurrent mode
 *   <li> Maximum resolution sensor pixel mode
 * </ul>
 *
 * <p>The surface combination that is used depends on the camera mode. The defined constants are
 * used to identify which supported surface combination list should be used.
 */
public final class CameraMode {
    /**
     * The camera is in the default mode.
     */
    public static final int DEFAULT = 0;
    /**
     * The camera is running in the concurrent camera mode.
     */
    public static final int CONCURRENT_CAMERA = 1;
    /**
     * The camera is running in the ultra high resolution camera mode.
     */
    public static final int ULTRA_HIGH_RESOLUTION_CAMERA = 2;

    @IntDef({DEFAULT, CONCURRENT_CAMERA, ULTRA_HIGH_RESOLUTION_CAMERA})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    /**
     * Returns a string representation of the CameraMode integer enum.
     */
    @NonNull
    public static String toLabelString(@Mode int mode) {
        switch (mode) {
            case CONCURRENT_CAMERA: return "CONCURRENT_CAMERA";
            case ULTRA_HIGH_RESOLUTION_CAMERA: return "ULTRA_HIGH_RESOLUTION_CAMERA";
            default: return "DEFAULT";
        }
    }

    private CameraMode() {
    }
}
