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
 * Helper class that defines certain enum-like methods for {@link FlashMode}
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FlashModeHelper {

    private FlashModeHelper() {
    }

    /**
     * Returns the {@link FlashMode} constant for the specified name
     *
     * @param name The name of the {@link FlashMode} to return
     * @return The {@link FlashMode} constant for the specified name
     */
    @FlashMode
    public static int valueOf(@Nullable final String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        switch (name) {
            case "AUTO":
                return FlashMode.AUTO;
            case "ON":
                return FlashMode.ON;
            case "OFF":
                return FlashMode.OFF;
            default:
                throw new IllegalArgumentException("Unknown flash mode name " + name);
        }
    }

    /**
     * Returns the name of the {@link FlashMode} constant, exactly as it is declared.
     *
     * @param flashMode A {@link FlashMode} constant
     * @return The name of the {@link FlashMode} constant.
     */
    @NonNull
    public static String nameOf(@FlashMode final int flashMode) {
        switch (flashMode) {
            case FlashMode.AUTO:
                return "AUTO";
            case FlashMode.ON:
                return "ON";
            case FlashMode.OFF:
                return "OFF";
            default:
                throw new IllegalArgumentException("Unknown flash mode " + flashMode);
        }
    }
}
