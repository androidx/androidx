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
import androidx.annotation.RestrictTo;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The available modes for the extensions.
 */
public final class ExtensionMode {
    /** Normal mode without any specific effect applied. */
    public static final int NONE = 0;
    /**
     * Bokeh mode blurs the background of a photo. It is generally intended for taking portrait
     * photos of people like what would be produced by a camera with a large lens.
     */
    public static final int BOKEH = 1;
    /**
     * HDR mode takes photos that keep a larger range of scene illumination levels visible in the
     * final image. For example, when taking a picture of an object in front of a bright window,
     * both the object and the scene through the window may be visible when using HDR mode, while
     * in normal mode, one or the other may be poorly exposed. As a tradeoff, HDR mode generally
     * takes much longer to capture a single image, has no user control, and may have other
     * artifacts depending on the HDR method used.
     */
    public static final int HDR = 2;
    /** Gets the best still images under low-light situations, typically at night time. */
    public static final int NIGHT = 3;
    /** Retouches face skin tone, geometry and so on when taking still images. */
    public static final int FACE_RETOUCH = 4;
    /** Automatically adjusts the final image with the surrounding scenery. For example, the
     * vendor library implementation might do the low light detection and can switch to low light
     * mode or HDR to take the picture. Or the face retouch mode can be automatically applied
     * when taking a portrait image. This delegates modes to the vendor library implementation to
     * decide.
     */
    public static final int AUTO = 5;

    /**
     * The different extension modes that a {@link Camera} can be configured for.
     *
     * <p>Not all devices and cameras support the different extension modes. To query whether or
     * not a specific Camera supports an extension mode use
     * {@link ExtensionsManager#isExtensionAvailable(CameraSelector, int)}.
     *
     */
    @IntDef({NONE, BOKEH, HDR, NIGHT, FACE_RETOUCH, AUTO})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface Mode {
    }

    private ExtensionMode() {
    }
}
