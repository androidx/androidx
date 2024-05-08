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

package androidx.camera.testing.impl;

import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Surface format related utility functions.
 */
public class SurfaceUtil {
    private SurfaceUtil() {
    }

    /**
     * Returns the surface pixel format.
     */
    public static int getSurfaceFormat(@Nullable Surface surface) {
        return nativeGetSurfaceFormat(surface);
    }

    /**
     * Sets the buffers transform for the surface that will be applied to future buffers posted
     * to the surface.
     */
    @RequiresApi(26)
    public static void setBuffersTransform(@NonNull Surface surface,
            @Transformation int transformation) {
        if (nativeSetBuffersTransform(surface, transformation) != 0) {
            throw new IllegalStateException(
                    "Attempted to set buffer transform on Surface in invalid state");
        }
    }

    static {
        System.loadLibrary("testing_surface_jni");
    }

    @IntDef({
            TRANSFORM_IDENTITY,
            TRANSFORM_MIRROR_HORIZONTAL,
            TRANSFORM_MIRROR_VERTICAL,
            TRANSFORM_ROTATE_90,
            TRANSFORM_ROTATE_180,
            TRANSFORM_ROTATE_270,
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface Transformation {
    }

    public static final int TRANSFORM_IDENTITY = 0x00;
    public static final int TRANSFORM_MIRROR_HORIZONTAL = 0x01;
    public static final int TRANSFORM_MIRROR_VERTICAL = 0x02;
    public static final int TRANSFORM_ROTATE_90 = 0x04;
    public static final int TRANSFORM_ROTATE_180 =
            TRANSFORM_MIRROR_HORIZONTAL | TRANSFORM_MIRROR_VERTICAL;
    public static final int TRANSFORM_ROTATE_270 = TRANSFORM_ROTATE_180 | TRANSFORM_ROTATE_90;
    public static final int TRANSFORM_INVERSE_DISPLAY = 0x08;

    private static native int nativeGetSurfaceFormat(@Nullable Surface surface);

    private static native int nativeSetBuffersTransform(@NonNull Surface surface,
            @Transformation int transformation);
}
