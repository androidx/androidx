/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The available modes for the extensions.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ExtensionMode {
    /** Normal mode without any specific effect applied. */
    public static final int NONE = 0;
    /** Makes foreground people sharper when photos are taken in portrait mode. */
    public static final int BOKEH = 1;
    /** Takes photos with different AE settings to generate the best result. */
    public static final int HDR = 2;
    /** Gets the best still images under low-light situations, typically at night time. */
    public static final int NIGHT = 3;
    /** Retouches face skin tone, geometry and so on when taking still images. */
    public static final int FACE_RETOUCH = 4;
    /** Automatically adjusts the final image with the surrounding scenery. */
    public static final int AUTO = 5;

    /**
     * The different extension modes that a {@link Camera} can be configured for.
     *
     * <p>Not all devices and cameras support the different extension modes. To query whether or
     * not a specific Camera supports an extension mode use
     * {@link ExtensionsInfo#isExtensionAvailable(CameraProvider, CameraSelector, int)}.
     *
     * @hide
     */
    @IntDef({NONE, BOKEH, HDR, NIGHT, FACE_RETOUCH, AUTO})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface Mode {
    }

    private ExtensionMode() {
    }
}
