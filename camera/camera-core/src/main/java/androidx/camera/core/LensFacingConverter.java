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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Helper class that defines certain enum-like methods for {@link LensFacing}
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LensFacingConverter {

    private LensFacingConverter() {
    }

    /**
     * @return an array containing the constants of {@link LensFacing} in the order they're
     * declared.
     */
    @NonNull
    public static Integer[] values() {
        return new Integer[]{LensFacing.FRONT, LensFacing.BACK};
    }

    /**
     * Returns the {@link LensFacing} constant for the specified name
     *
     * @param name The name of the {@link LensFacing} to return
     * @return The {@link LensFacing} constant for the specified name
     */
    @LensFacing
    public static int valueOf(@Nullable final String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        switch (name) {
            case "FRONT":
                return LensFacing.FRONT;
            case "BACK":
                return LensFacing.BACK;
            default:
                throw new IllegalArgumentException("Unknown len facing name " + name);
        }
    }

    /**
     * Returns the name of the {@link LensFacing} constant, exactly as it is declared.
     *
     * @param lensFacing A {@link LensFacing} constant
     * @return The name of the {@link LensFacing} constant.
     */
    @NonNull
    public static String nameOf(@LensFacing final int lensFacing) {
        switch (lensFacing) {
            case LensFacing.FRONT:
                return "FRONT";
            case LensFacing.BACK:
                return "BACK";
            default:
                throw new IllegalArgumentException("Unknown lens facing " + lensFacing);
        }
    }
}
