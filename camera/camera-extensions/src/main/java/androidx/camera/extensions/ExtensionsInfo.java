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


import androidx.annotation.NonNull;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
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

/**
 * A class for querying extensions related information.
 *
 * <p>The typical usages include checking whether or not a camera exists that supports an extension
 * by using {@link #isExtensionAvailable(CameraProvider, CameraSelector, int)}. Then after it has
 * been determined that the extension can be enabled, a
 * {@link #getExtensionCameraSelectorAndInjectCameraConfig(CameraSelector, int)} call can be used to get the
 * specified {@link CameraSelector} to bind use cases and enable the extension mode on the camera.
 */
final class ExtensionsInfo {
    private static final String TAG = "ExtensionsInfo";

    private static final String EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX = ":camera:camera"
            + "-extensions-";

    /**
     * Returns a {@link CameraSelector} for the specified extension mode.
     *
     * <p>The corresponding extension camera config provider will be injected to the
     * {@link ExtendedCameraConfigProviderStore} when the function is called.
     *
     * @param cameraProvider The {@link CameraProvider} which will be used to bind use cases.
     * @param baseCameraSelector The base {@link CameraSelector} to be applied the extension
     *                           related configuration on.
     * @param mode The target extension mode.
     * @return a {@link CameraSelector} for the specified Extensions mode.
     * @throws IllegalArgumentException If no camera can be found to support the specified
     * extension mode, or the base {@link CameraSelector} has contained
     * extension related configuration in it.
     */
    @NonNull
    static CameraSelector getExtensionCameraSelectorAndInjectCameraConfig(
            @NonNull CameraProvider cameraProvider,
            @NonNull CameraSelector baseCameraSelector,
            @ExtensionMode.Mode int mode) {
        if (!isExtensionAvailable(cameraProvider, baseCameraSelector, mode)) {
            throw new IllegalArgumentException("No camera can be found to support the specified "
                    + "extensions mode! isExtensionAvailable should be checked first before "
                    + "calling getExtensionCameraSelector.");
        }

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
     * Returns true if the particular extension mode is available for the specified
     * {@link CameraSelector}.
     *
     * @param cameraProvider The {@link CameraProvider} which will be used to bind use cases.
     * @param baseCameraSelector The base {@link CameraSelector} to find a camera to use.
     * @param mode The target extension mode to support.
     */
    static boolean isExtensionAvailable(
            @NonNull CameraProvider cameraProvider,
            @NonNull CameraSelector baseCameraSelector,
            @ExtensionMode.Mode int mode) {
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

    private static CameraFilter getFilter(@ExtensionMode.Mode int mode) {
        CameraFilter filter;
        String id = getExtendedCameraConfigProviderId(mode);

        try {
            switch (mode) {
                case ExtensionMode.BOKEH:
                    filter = new ExtensionCameraFilter(id, new BokehPreviewExtenderImpl(),
                            new BokehImageCaptureExtenderImpl());
                    break;
                case ExtensionMode.HDR:
                    filter = new ExtensionCameraFilter(id, new HdrPreviewExtenderImpl(),
                            new HdrImageCaptureExtenderImpl());
                    break;
                case ExtensionMode.NIGHT:
                    filter = new ExtensionCameraFilter(id, new NightPreviewExtenderImpl(),
                            new NightImageCaptureExtenderImpl());
                    break;
                case ExtensionMode.BEAUTY:
                    filter = new ExtensionCameraFilter(id, new BeautyPreviewExtenderImpl(),
                            new BeautyImageCaptureExtenderImpl());
                    break;
                case ExtensionMode.AUTO:
                    filter = new ExtensionCameraFilter(id, new AutoPreviewExtenderImpl(),
                            new AutoImageCaptureExtenderImpl());
                    break;
                case ExtensionMode.NONE:
                default:
                    filter = CameraFilters.ANY;
            }
        } catch (NoClassDefFoundError e) {
            filter = CameraFilters.NONE;
        }

        return filter;
    }

    /**
     * Injects {@link CameraConfigProvider} for specified extension mode to the
     * {@link ExtendedCameraConfigProviderStore}.
     */
    private static void injectExtensionCameraConfig(@ExtensionMode.Mode int mode) {
        CameraFilter.Id id = CameraFilter.Id.create(getExtendedCameraConfigProviderId(mode));

        if (ExtendedCameraConfigProviderStore.getConfigProvider(id) == CameraConfigProvider.EMPTY) {
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

    private static String getExtendedCameraConfigProviderId(@ExtensionMode.Mode int mode) {
        String id;

        switch (mode) {
            case ExtensionMode.BOKEH:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_BOKEH";
                break;
            case ExtensionMode.HDR:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_HDR";
                break;
            case ExtensionMode.NIGHT:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_NIGHT";
                break;
            case ExtensionMode.BEAUTY:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_BEAUTY";
                break;
            case ExtensionMode.AUTO:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_AUTO";
                break;
            case ExtensionMode.NONE:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_NONE";
                break;
            default:
                throw new IllegalArgumentException("Invalid extension mode!");
        }
        return id;
    }

    private ExtensionsInfo() {
    }
}
