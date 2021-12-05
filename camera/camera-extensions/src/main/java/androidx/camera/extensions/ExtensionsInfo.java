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
import android.hardware.camera2.CameraCharacteristics;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraConfigProvider;
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore;
import androidx.camera.core.impl.Identifier;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.extensions.internal.AdvancedVendorExtender;
import androidx.camera.extensions.internal.BasicVendorExtender;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.ExtensionsUseCaseConfigFactory;
import androidx.camera.extensions.internal.VendorExtender;
import androidx.camera.extensions.internal.Version;
import androidx.camera.extensions.internal.compat.workaround.ExtensionDisabledValidator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A class for querying extensions related information.
 *
 * <p>The typical usages include checking whether or not a camera exists that supports an extension
 * by using {@link #isExtensionAvailable(CameraSelector, int)}. Then after it has been determined
 * that the extension can be enabled, a
 * {@link #getExtensionCameraSelectorAndInjectCameraConfig(CameraSelector, int)} call can be used
 * to get the specified {@link CameraSelector} to bind use cases and enable the extension mode on
 * the camera.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ExtensionsInfo {
    private static final String EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX = ":camera:camera"
            + "-extensions-";
    private static final ExtensionDisabledValidator sExtensionDisabledValidator =
            new ExtensionDisabledValidator();
    private final CameraProvider mCameraProvider;

    ExtensionsInfo(@NonNull CameraProvider cameraProvider) {
        mCameraProvider = cameraProvider;
    }

    /**
     * Returns a {@link CameraSelector} for the specified extension mode.
     *
     * <p>The corresponding extension camera config provider will be injected to the
     * {@link ExtendedCameraConfigProviderStore} when the function is called.
     *
     * @param baseCameraSelector The base {@link CameraSelector} to be applied the extension
     *                           related configuration on.
     * @param mode               The target extension mode.
     * @return a {@link CameraSelector} for the specified Extensions mode.
     * @throws IllegalArgumentException If no camera can be found to support the specified
     *                                  extension mode, or the base {@link CameraSelector} has
     *                                  contained
     *                                  extension related configuration in it.
     */
    @NonNull
    CameraSelector getExtensionCameraSelectorAndInjectCameraConfig(
            @NonNull CameraSelector baseCameraSelector,
            @ExtensionMode.Mode int mode) {
        if (!isExtensionAvailable(baseCameraSelector, mode)) {
            throw new IllegalArgumentException("No camera can be found to support the specified "
                    + "extensions mode! isExtensionAvailable should be checked first before "
                    + "calling getExtensionEnabledCameraSelector.");
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
     * @param baseCameraSelector The base {@link CameraSelector} to find a camera to use.
     * @param mode               The target extension mode to support.
     */
    boolean isExtensionAvailable(
            @NonNull CameraSelector baseCameraSelector,
            @ExtensionMode.Mode int mode) {
        CameraSelector.Builder builder = CameraSelector.Builder.fromSelector(
                baseCameraSelector);
        builder.addCameraFilter(getFilter(mode));

        List<CameraInfo> cameraInfos = builder.build().filter(
                mCameraProvider.getAvailableCameraInfos());
        return !cameraInfos.isEmpty();
    }

    /**
     * Returns the estimated capture latency range in milliseconds for the target capture
     * resolution.
     *
     * @param cameraSelector    The {@link CameraSelector} to find a camera which supports the
     *                          specified extension mode.
     * @param mode              The extension mode to check.
     * @param resolution        The resolution of the {@link ImageCapture} which will be used to
     *                          take a picture. If the input value of this parameter is null or
     *                          it is not included in the supported output sizes, the maximum
     *                          capture output size is used to get the estimated range information.
     * @return the range of estimated minimal and maximal capture latency in milliseconds.
     * Returns null if no capture latency info can be provided.
     * @throws IllegalArgumentException If no camera can be found to support the specified
     *                                  extension mode.
     */
    @Nullable
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    Range<Long> getEstimatedCaptureLatencyRange(
            @NonNull CameraSelector cameraSelector,
            @ExtensionMode.Mode int mode, @Nullable Size resolution) {
        // Adds the filter to find a CameraInfo of the Camera which supports the specified
        // extension mode. Checks this first so that the API behavior will be the same no matter
        // the vendor library is above version 1.2 or not.
        CameraSelector newCameraSelector = CameraSelector.Builder.fromSelector(
                cameraSelector).addCameraFilter(getFilter(mode)).build();

        CameraInfo extensionsCameraInfo;
        List<CameraInfo> cameraInfos =
                newCameraSelector.filter(mCameraProvider.getAvailableCameraInfos());

        if (cameraInfos.isEmpty()) {
            throw new IllegalArgumentException("No cameras found for given CameraSelector");
        }

        extensionsCameraInfo = cameraInfos.get(0);

        // This API is only supported since version 1.2
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_2) < 0) {
            return null;
        }

        try {
            VendorExtender vendorExtender = getVendorExtender(mode);
            vendorExtender.init(extensionsCameraInfo);

            return vendorExtender.getEstimatedCaptureLatencyRange(resolution);
        } catch (NoSuchMethodError e) {
            return null;
        }
    }

    private static CameraFilter getFilter(@ExtensionMode.Mode int mode) {
        CameraFilter filter;
        String id = getExtendedCameraConfigProviderId(mode);

        VendorExtender vendorExtender = getVendorExtender(mode);
        filter = new ExtensionCameraFilter(id, vendorExtender);
        return filter;
    }

    /**
     * Injects {@link CameraConfigProvider} for specified extension mode to the
     * {@link ExtendedCameraConfigProviderStore}.
     */
    private static void injectExtensionCameraConfig(@ExtensionMode.Mode int mode) {
        Identifier id = Identifier.create(getExtendedCameraConfigProviderId(mode));

        if (ExtendedCameraConfigProviderStore.getConfigProvider(id) == CameraConfigProvider.EMPTY) {
            ExtendedCameraConfigProviderStore.addConfig(id, (cameraInfo, context) -> {
                VendorExtender vendorExtender = getVendorExtender(mode);
                vendorExtender.init(cameraInfo);

                ExtensionsUseCaseConfigFactory factory = new
                        ExtensionsUseCaseConfigFactory(mode, vendorExtender, context);

                ExtensionsConfig.Builder builder = new ExtensionsConfig.Builder()
                        .setExtensionMode(mode)
                        .setUseCaseConfigFactory(factory)
                        .setCompatibilityId(id)
                        .setUseCaseCombinationRequiredRule(
                                CameraConfig.REQUIRED_RULE_COEXISTING_PREVIEW_AND_IMAGE_CAPTURE);

                SessionProcessor sessionProcessor = vendorExtender.createSessionProcessor(context);
                if (sessionProcessor != null) {
                    builder.setSessionProcessor(sessionProcessor);
                }

                return builder.build();
            });
        }
    }

    @NonNull
    private static VendorExtender getVendorExtender(int mode) {
        boolean isAdvancedExtenderSupported = isAdvancedExtenderSupported();

        // Disable Advanced Extender until it is well tested.
        if (isAdvancedExtenderSupported) {
            return new DisabledVendorExtender();
        }

        // Force disable extension for some devices by quirk.
        if (sExtensionDisabledValidator.shouldDisableExtension(isAdvancedExtenderSupported)) {
            return new DisabledVendorExtender();
        }

        VendorExtender vendorExtender;
        if (isAdvancedExtenderSupported) {
            vendorExtender = new AdvancedVendorExtender(mode);
        } else {
            vendorExtender = new BasicVendorExtender(mode);
        }
        return vendorExtender;
    }

    private static boolean isAdvancedExtenderSupported() {
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_2) < 0) {
            return false;
        }
        return ExtensionVersion.isAdvancedExtenderSupported();
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
            case ExtensionMode.FACE_RETOUCH:
                id = EXTENDED_CAMERA_CONFIG_PROVIDER_ID_PREFIX + "EXTENSION_MODE_FACE_RETOUCH";
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

    static class DisabledVendorExtender implements VendorExtender {
        @Override
        public boolean isExtensionAvailable(@NonNull String cameraId,
                @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
            return false;
        }

        @Override
        public void init(@NonNull CameraInfo cameraInfo) {

        }

        @Nullable
        @Override
        public Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size size) {
            return null;
        }

        @NonNull
        @Override
        public List<Pair<Integer, Size[]>> getSupportedPreviewOutputResolutions() {
            return Collections.emptyList();
        }

        @NonNull
        @Override
        public List<Pair<Integer, Size[]>> getSupportedCaptureOutputResolutions() {
            return Collections.emptyList();
        }

        @NonNull
        @Override
        public Size[] getSupportedYuvAnalysisResolutions() {
            return new Size[0];
        }

        @Nullable
        @Override
        public SessionProcessor createSessionProcessor(@NonNull Context context) {
            return null;
        }
    }
}
