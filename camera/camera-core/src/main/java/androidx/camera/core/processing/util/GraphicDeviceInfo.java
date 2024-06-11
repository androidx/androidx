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

package androidx.camera.core.processing.util;

import static androidx.camera.core.processing.util.GLUtils.VERSION_UNKNOWN;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

/**
 * Information about an initialized graphics device.
 *
 * <p>This information can be used to determine which version or extensions of OpenGL and EGL
 * are supported on the device to ensure the attached output surface will have expected
 * characteristics.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AutoValue
public abstract class GraphicDeviceInfo {
    /**
     * Returns the OpenGL version this graphics device has been initialized to.
     *
     * <p>The version is in the form &lt;major&gt;.&lt;minor&gt;.
     *
     * <p>Returns {@link GLUtils#VERSION_UNKNOWN} if version information can't be
     * retrieved.
     */
    @NonNull
    public abstract String getGlVersion();

    /**
     * Returns the EGL version this graphics device has been initialized to.
     *
     * <p>The version is in the form &lt;major&gt;.&lt;minor&gt;.
     *
     * <p>Returns {@link GLUtils#VERSION_UNKNOWN} if version information can't be
     * retrieved.
     */
    @NonNull
    public abstract String getEglVersion();

    /**
     * Returns a space separated list of OpenGL extensions or an empty string if extensions
     * could not be retrieved.
     */
    @NonNull
    public abstract String getGlExtensions();

    /**
     * Returns a space separated list of EGL extensions or an empty string if extensions
     * could not be retrieved.
     */
    @NonNull
    public abstract String getEglExtensions();

    /**
     * Returns the Builder.
     */
    @NonNull
    public static Builder builder() {
        return new AutoValue_GraphicDeviceInfo.Builder()
                .setGlVersion(VERSION_UNKNOWN)
                .setEglVersion(VERSION_UNKNOWN)
                .setGlExtensions("")
                .setEglExtensions("");
    }

    // Should not be instantiated directly
    GraphicDeviceInfo() {
    }

    /**
     * Builder for {@link GraphicDeviceInfo}.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Sets the gl version.
         */
        @NonNull
        public abstract Builder setGlVersion(@NonNull String version);

        /**
         * Sets the egl version.
         */
        @NonNull
        public abstract Builder setEglVersion(@NonNull String version);

        /**
         * Sets the gl extensions.
         */
        @NonNull
        public abstract Builder setGlExtensions(@NonNull String extensions);

        /**
         * Sets the egl extensions.
         */
        @NonNull
        public abstract Builder setEglExtensions(@NonNull String extensions);

        /**
         * Builds the {@link GraphicDeviceInfo}.
         */
        @NonNull
        public abstract GraphicDeviceInfo build();
    }
}
