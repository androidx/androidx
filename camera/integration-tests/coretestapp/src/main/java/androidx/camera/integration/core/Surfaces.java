/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.core;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.view.Surface;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;

/**
 * Utilities for working with {@link Surface Surfaces}.
 */
public final class Surfaces {

    public static final int ROTATION_0_DEG = 0;
    public static final int ROTATION_90_DEG = 90;
    public static final int ROTATION_180_DEG = 180;
    public static final int ROTATION_270_DEG = 270;

    @Retention(SOURCE)
    @IntDef({ROTATION_0_DEG, ROTATION_90_DEG, ROTATION_180_DEG, ROTATION_270_DEG})
    public @interface RotationDegrees {}

    @Retention(SOURCE)
    @IntDef({Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270})
    public @interface RotationEnum {}

    /** @return One of 0, 90, 180, 270. */
    @RotationDegrees
    public static int toSurfaceRotationDegrees(@RotationEnum int rotationEnum) {
        @RotationDegrees int rotationDegrees;
        switch (rotationEnum) {
            case Surface.ROTATION_0:
                rotationDegrees = ROTATION_0_DEG;
                break;
            case Surface.ROTATION_90:
                rotationDegrees = ROTATION_90_DEG;
                break;
            case Surface.ROTATION_180:
                rotationDegrees = ROTATION_180_DEG;
                break;
            case Surface.ROTATION_270:
                rotationDegrees = ROTATION_270_DEG;
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported rotation enum: " + rotationEnum);
        }
        return rotationDegrees;
    }

    // Should not be instantiated.
    private Surfaces() {}
}
