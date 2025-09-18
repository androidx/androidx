/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Services that are needed to be provided by the platform during encoding. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Platform {

    /**
     * Converts a platform-specific image object into a platform-independent byte buffer
     *
     * @param image
     * @return
     */
    byte @Nullable [] imageToByteArray(@NonNull Object image);

    /**
     * Returns the width of a platform-specific image object
     *
     * @param image platform-specific image object
     * @return the width of the image in pixels
     */
    int getImageWidth(@NonNull Object image);

    /**
     * Returns the height of a platform-specific image object
     *
     * @param image platform-specific image object
     * @return the height of the image in pixels
     */
    int getImageHeight(@NonNull Object image);

    /**
     * Returns true if the platform-specific image object has format ALPHA_8
     *
     * @param image platform-specific image object
     * @return whether or not the platform-specific image object has format ALPHA_8
     */
    boolean isAlpha8Image(@NonNull Object image);

    /**
     * Converts a platform-specific path object into a platform-independent float buffer
     *
     * @param path path object
     * @return float array of the path
     */
    float @Nullable [] pathToFloatArray(@NonNull Object path);

    /**
     * Parse a path represented as a string and returns a Path object
     *
     * @param pathData path data
     * @return platform path
     */
    @NonNull Object parsePath(@NonNull String pathData);

    enum LogCategory {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        TODO,
    }

    /**
     * Log a message
     *
     * @param category
     * @param message
     */
    void log(@NonNull LogCategory category, @NonNull String message);

    /**
     * Represents a precomputed text layout, for complex text painting / measuring / layout. Allows
     * the implementation to return a cached / engine after a text measure to be used int the paint
     * pass.
     */
    interface ComputedTextLayout {
        /**
         * Horizontal dimension of this text layout
         *
         * @return
         */
        float getWidth();

        /**
         * Vertical dimension of this text layout
         *
         * @return
         */
        float getHeight();
    }

    Platform None =
            new Platform() {
                @Override
                public byte[] imageToByteArray(@NonNull Object image) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int getImageWidth(@NonNull Object image) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int getImageHeight(@NonNull Object image) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean isAlpha8Image(@NonNull Object image) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public float[] pathToFloatArray(@NonNull Object path) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public @NonNull Object parsePath(@NonNull String pathData) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void log(@NonNull LogCategory category, @NonNull String message) {}
            };
}
