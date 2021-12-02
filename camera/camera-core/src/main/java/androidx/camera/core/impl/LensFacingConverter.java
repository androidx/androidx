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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;

/**
 * Helper class that defines certain enum-like methods for {@link CameraSelector.LensFacing}
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class LensFacingConverter {

    private LensFacingConverter() {
    }

    /**
     * @return an array containing the constants of {@link CameraSelector.LensFacing} in the
     * order they're declared.
     */
    @NonNull
    public static Integer[] values() {
        return new Integer[]{CameraSelector.LENS_FACING_FRONT, CameraSelector.LENS_FACING_BACK};
    }

    /**
     * Returns the {@link CameraSelector.LensFacing} constant for the specified name
     *
     * @param name The name of the {@link CameraSelector.LensFacing} to return
     * @return The {@link CameraSelector.LensFacing} constant for the specified name
     */
    @CameraSelector.LensFacing
    public static int valueOf(@Nullable final String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        switch (name) {
            case "FRONT":
                return CameraSelector.LENS_FACING_FRONT;
            case "BACK":
                return CameraSelector.LENS_FACING_BACK;
            default:
                throw new IllegalArgumentException("Unknown len facing name " + name);
        }
    }

    /**
     * Returns the name of the {@link CameraSelector.LensFacing} constant, exactly as it is
     * declared.
     *
     * @param lensFacing A {@link CameraSelector.LensFacing} constant
     * @return The name of the {@link CameraSelector.LensFacing} constant.
     */
    @NonNull
    public static String nameOf(@CameraSelector.LensFacing final int lensFacing) {
        switch (lensFacing) {
            case CameraSelector.LENS_FACING_FRONT:
                return "FRONT";
            case CameraSelector.LENS_FACING_BACK:
                return "BACK";
            default:
                throw new IllegalArgumentException("Unknown lens facing " + lensFacing);
        }
    }
}
