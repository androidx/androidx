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
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalCameraFilter;
import androidx.camera.core.impl.CameraConfigProvider;
import androidx.camera.core.impl.CameraFilters;
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.extensions.internal.ExtensionsUseCaseConfigFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class for querying extensions related information.
 *
 * <p>The typical usages include checking whether or not a camera exists that supports an extension
 * by using {@link #isExtensionAvailable(CameraProvider, CameraSelector, int)}. Then after it has
 * been determined that the extension can be enabled, a
 * {@link #getExtensionCameraSelector(CameraSelector, int)} call can be used to get the
 * specified {@link CameraSelector} to bind use cases and enable the extension mode on the camera.
 *
 * <p>When the Camera has been set to a particular extension it might require the camera to
 * restart which can cause the preview to momentarily stop. Once the extension has been enabled
 * for a Camera instance then it will stay in that extension mode until the extension has been
 * disabled.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Extensions {
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

    private static final String EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX = ":camera:camera"
            + "-extensions-";
    /**
     * The different extension modes that a {@link Camera} can be configured for.
     *
     * <p>Not all devices and cameras support the different extension modes. To query whether or
     * not a specific Camera supports an extension mode use
     * {@link Extensions#isExtensionAvailable(CameraProvider, CameraSelector, int)}.
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
     * Returns a {@link CameraSelector} for the specific extension mode.
     *
     * @param baseCameraSelector The base {@link CameraSelector} to be applied the extension
     *                           related configuration on.
     *                         {@link #isExtensionAvailable(CameraProvider, CameraSelector, int)}
     *                          can be used to check whether any camera can support the specified
     *                          extension mode for the base camera selector.
     * @param mode The target extension mode.
     * @return a {@link CameraSelector} for the specific Extensions mode.
     * @throws IllegalArgumentException if the base {@link CameraSelector} has contained
     * extension related configuration in it.
     */
    @OptIn(markerClass = ExperimentalCameraFilter.class)
    @NonNull
    public CameraSelector getExtensionCameraSelector(@NonNull CameraSelector baseCameraSelector,
            @ExtensionMode int mode) {
        // Checks whether there has been Extensions related CameraConfig set in the base
        // CameraSelector.
        for (CameraFilter cameraFilter : baseCameraSelector.getCameraFilterSet()) {
            if (cameraFilter instanceof ExtensionCameraFilter) {
                throw new IllegalArgumentException(
                        "An extension is already applied to the base CameraSelector.");
            }
        }

        // Injects CameraConfigProvider for the extension mode to the
        // ExtendedCameraConfigProviderStore.
        injectExtensionCameraConfig(mode);

        CameraSelector.Builder builder = CameraSelector.Builder.fromSelector(baseCameraSelector);

        // Adds the CameraFilter that determines which cameras can support the Extensions mode
        // to the CameraSelector.
        builder.addCameraFilter(getFilter(mode));

        return builder.build();
    }

    /**
     * Returns the extension mode that is currently set on the camera.
     */
    @ExtensionMode
    public int getExtension(@NonNull Camera camera) {
        Object extensionsConfigObject = camera.getExtendedConfig();

        if (extensionsConfigObject instanceof ExtensionsConfig) {
            ExtensionsConfig extensionsConfig = (ExtensionsConfig) extensionsConfigObject;
            return extensionsConfig.getExtensionMode();
        }
        return EXTENSION_MODE_NONE;
    }

    /**
     * Returns true if the particular extension mode is available for the specified
     * {@link CameraSelector}.
     *
     * @param cameraProvider The {@link CameraProvider} which will be used to bind use cases.
     * @param baseCameraSelector The base {@link CameraSelector} to find a camera to use.
     * @param mode The target extension mode to support.
     */
    @OptIn(markerClass = ExperimentalCameraFilter.class)
    public boolean isExtensionAvailable(
            @NonNull CameraProvider cameraProvider,
            @NonNull CameraSelector baseCameraSelector,
            @ExtensionMode int mode) {
        try {
            CameraSelector.Builder builder = CameraSelector.Builder.fromSelector(
                    baseCameraSelector);
            builder.addCameraFilter(getFilter(mode));

            builder.build().filter(cameraProvider.getAvailableCameraInfos());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    @OptIn(markerClass = ExperimentalCameraFilter.class)
    private CameraFilter getFilter(@ExtensionMode int mode) {
        CameraFilter filter;
        String id = getExtendedCameraConfigProviderId(mode);

        try {
            switch (mode) {
                case EXTENSION_MODE_BOKEH:
                    filter = new ExtensionCameraFilter(id, new BokehPreviewExtenderImpl(),
                            new BokehImageCaptureExtenderImpl());
                    break;
                case EXTENSION_MODE_HDR:
                    filter = new ExtensionCameraFilter(id, new HdrPreviewExtenderImpl(),
                            new HdrImageCaptureExtenderImpl());
                    break;
                case EXTENSION_MODE_NIGHT:
                    filter = new ExtensionCameraFilter(id, new NightPreviewExtenderImpl(),
                            new NightImageCaptureExtenderImpl());
                    break;
                case EXTENSION_MODE_BEAUTY:
                    filter = new ExtensionCameraFilter(id, new BeautyPreviewExtenderImpl(),
                            new BeautyImageCaptureExtenderImpl());
                    break;
                case EXTENSION_MODE_AUTO:
                    filter = new ExtensionCameraFilter(id, new AutoPreviewExtenderImpl(),
                            new AutoImageCaptureExtenderImpl());
                    break;
                case EXTENSION_MODE_NONE:
                default:
                    filter = CameraFilters.ANY;
            }
        } catch (NoClassDefFoundError e) {
            filter = CameraFilters.NONE;
        }

        return filter;
    }

    /**
     * Injects {@link CameraConfigProvider} for specific extension mode to the
     * {@link ExtendedCameraConfigProviderStore}.
     */
    private void injectExtensionCameraConfig(@ExtensionMode int mode) {
        CameraFilter.Id id = CameraFilter.Id.create(getExtendedCameraConfigProviderId(mode));

        if (ExtendedCameraConfigProviderStore.getConfig(id) == CameraConfigProvider.EMPTY) {
            ExtendedCameraConfigProviderStore.addConfig(id, (cameraInfo, context) -> {
                ExtensionsUseCaseConfigFactory factory = new
                        ExtensionsUseCaseConfigFactory(mode, cameraInfo, context);
                return new ExtensionsConfig.Builder()
                        .setExtensionMode(mode)
                        .setUseCaseConfigFactory(factory)
                        .build();
            });
        }
    }

    private String getExtendedCameraConfigProviderId(@ExtensionMode int mode) {
        String id;

        switch (mode) {
            case EXTENSION_MODE_BOKEH:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_BOKEH";
                break;
            case EXTENSION_MODE_HDR:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_HDR";
                break;
            case EXTENSION_MODE_NIGHT:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_NIGHT";
                break;
            case EXTENSION_MODE_BEAUTY:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_BEAUTY";
                break;
            case EXTENSION_MODE_AUTO:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_AUTO";
                break;
            case EXTENSION_MODE_NONE:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_NONE";
                break;
            default:
                throw new IllegalArgumentException("Invalid extension mode!");
        }
        return id;
    }
}
