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

package androidx.camera.extensions;

import android.content.Context;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.Camera;
import androidx.camera.core.UseCase;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A class for querying and controlling the extensions that are enable for individual
 * {@link Camera} instances.
 *
 * <p> The typical usages is to check whether or not a Camera and/or the {@link UseCase}
 * combinations support the extension by using {@link #isExtensionAvailable(Camera, int)} and
 * {@link #checkUseCases(Camera, List, int)}. Then after it has been determined that the
 * extension can be enable then a {@link #setExtension(Camera, int)} call can be used to set the
 * specified extension on the camera.
 *
 * <p> When the Camera has been set to a particular extension it might require the camera to
 * restart which can cause the preview to temporarily stop. Once the extension has been enable
 * for a Camera instance then it will stay in that extension mode until the extension has been
 * disabled. Setting extension modes is separate for each camera instance.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Extensions {
    private static final String TAG = "Extensions";

    /** Normal mode without any specific effect applied. */
    public static final int EXTENSION_MODE_NONE = 0;
    /** Bokeh mode that is often applied as portrait mode for people pictures. */
    public static final int EXTENSION_MODE_BOKEH = 1;
    /**
     * HDR mode that may get source pictures with different AE settings to generate a best
     * result.
     */
    public static final int EXTENSION_MODE_HDR = 2;
    /**
     * Night mode is used for taking better still capture images under low-light situations,
     * typically at night time.
     */
    public static final int EXTENSION_MODE_NIGHT = 3;
    /**
     * Beauty mode is used for taking still capture images that incorporate facial changes
     * like skin tone, geometry, or retouching.
     */
    public static final int EXTENSION_MODE_BEAUTY = 4;
    /**
     * Auto mode is used for taking still capture images that automatically adjust to the
     * surrounding scenery.
     */
    public static final int EXTENSION_MODE_AUTO = 5;


    /**
     * The different extension modes that a {@link Camera} can be configured for.
     *
     * <p>Not all devices and camera support the different extension modes. To query whether or
     * not a specific Camera supports an extension mode use
     * {@link Extensions#isExtensionAvailable(Camera, int)}.
     *
     * @hide
     */
    @IntDef({EXTENSION_MODE_NONE, EXTENSION_MODE_BOKEH, EXTENSION_MODE_HDR, EXTENSION_MODE_NIGHT,
            EXTENSION_MODE_BEAUTY, EXTENSION_MODE_AUTO})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface ExtensionMode {
    }

    Extensions(@NonNull Context context) {
    }

    /**
     * Sets the specified extension mode for the Camera.
     *
     * To return to a non-extensions mode Camera set the extension mode as NONE.
     * For full list of extensions see Extension Modes
     *
     * @param camera The camera that the UseCases are attached to
     * @param mode The extension mode to set. Setting this to EXTENSION_NONE will
     *             remove the current extension and will always succeed.
     *
     * @throws IllegalArgumentException if unable to change to the specified extension
     *                              mode
     */
    public void setExtension(@NonNull Camera camera, @ExtensionMode int mode) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Returns the extension mode that is currently set on the camera.
     */
    public @ExtensionMode int getExtension(@NonNull Camera camera) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Returns true if the particular extension mode is available for the specified
     * Camera.
     *
     * <p> This check is independent of the {@link UseCase} which are currently attached to the
     * {@link Camera}. To check whether the Camera can support the attached UseCases use {@link
     * #checkUseCases(Camera, List, int)}.
     *
     * @param camera The Camera to check if it supports the extension.
     * @param mode The extension mode to check
     */
    public boolean isExtensionAvailable(@NonNull Camera camera, @ExtensionMode int mode) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Checks if the list of UseCases attached to the Camera can support the
     * extension.
     *
     * If the list of UseCases exceeds the capacity of Surfaces for the Camera then
     * it returns a list of UseCase lists that can be removed in order to allow for
     * the extension to be enabled. Any of the individual lists can be removed.
     *
     * @return null if the Camera supports the extension using the list of UseCases, otherwise a
     * list of UseCase list to remove.
     */
    @NonNull
    public List<List<UseCase>> checkUseCases(@NonNull Camera camera,
            @NonNull List<UseCase> useCases,
            @ExtensionMode int mode) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
