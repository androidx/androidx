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
import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * A {@link CameraControlInternal} whose capabilities can be restricted via
 * {@link #enableRestrictedOperations(boolean, Set)}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class RestrictedCameraControl {
    /**
     * Defines the list of supported camera operations.
     */
    public static final int ZOOM = 0;
    public static final int AUTO_FOCUS = 1;
    public static final int AF_REGION = 2;
    public static final int AE_REGION = 3;
    public static final int AWB_REGION = 4;
    public static final int FLASH = 5;
    public static final int TORCH = 6;
    public static final int EXPOSURE_COMPENSATION = 7;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ZOOM, AUTO_FOCUS, AF_REGION, AE_REGION,
            AWB_REGION, FLASH, TORCH, EXPOSURE_COMPENSATION})
    public @interface CameraOperation {}
}
