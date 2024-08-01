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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.workaround.TemplateParamsOverride;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.stabilization.StabilizationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to build a camera2 {@link CaptureRequest} from a {@link CaptureConfig}
 */
class Camera2CaptureRequestBuilder {
    private Camera2CaptureRequestBuilder() {
    }

    private static final String TAG = "Camera2CaptureRequestBuilder";

    /**
     * Get the configured Surface from DeferrableSurface list using the Surface map which should be
     * created when creating capture session.
     *
     * @param configuredSurfaceMap surface mapping which was created when creating capture session.
     * @return a list of Surface confirmed to be configured.
     * @throws IllegalArgumentException if the DeferrableSurface is not the one in SessionConfig.
     */
    @NonNull
    private static List<Surface> getConfiguredSurfaces(List<DeferrableSurface> deferrableSurfaces,
            Map<DeferrableSurface, Surface> configuredSurfaceMap) {
        List<Surface> surfaceList = new ArrayList<>();
        for (DeferrableSurface deferrableSurface : deferrableSurfaces) {
            Surface surface = configuredSurfaceMap.get(deferrableSurface);

            if (surface == null) {
                throw new IllegalArgumentException("DeferrableSurface not in configuredSurfaceMap");
            }

            surfaceList.add(surface);
        }

        return surfaceList;
    }

    private static void applyTemplateParamsOverrideWorkaround(
            @NonNull CaptureRequest.Builder builder, int template,
            @NonNull TemplateParamsOverride templateParamsOverride) {
        for (Map.Entry<CaptureRequest.Key<?>, Object> entry :
                templateParamsOverride.getOverrideParams(template).entrySet()) {
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) entry.getKey();
            builder.set(key, entry.getValue());
        }
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static void applyImplementationOptionToCaptureBuilder(
            CaptureRequest.Builder builder, Config config) {
        CaptureRequestOptions bundle = CaptureRequestOptions.Builder.from(config).build();
        for (Config.Option<?> option : bundle.listOptions()) {
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) option.getToken();

            // TODO(b/129997028): Error of setting unavailable CaptureRequest.Key may need to
            //  send back out to the developer
            try {
                // Ignores keys that don't exist
                builder.set(key, bundle.retrieveOption(option));
            } catch (IllegalArgumentException e) {
                Logger.e(TAG, "CaptureRequest.Key is not supported: " + key);
            }
        }
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static void applyAeFpsRange(@NonNull CaptureConfig captureConfig,
            @NonNull CaptureRequest.Builder builder) {
        if (!captureConfig.getExpectedFrameRateRange().equals(
                StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)) {
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    captureConfig.getExpectedFrameRateRange());
        }

    }

    @VisibleForTesting
    static void applyVideoStabilization(@NonNull CaptureConfig captureConfig,
            @NonNull CaptureRequest.Builder builder) {
        if (captureConfig.getPreviewStabilizationMode() == StabilizationMode.OFF
                || captureConfig.getVideoStabilizationMode() == StabilizationMode.OFF) {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        } else if (captureConfig.getPreviewStabilizationMode() == StabilizationMode.ON) {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION);
        } else if (captureConfig.getVideoStabilizationMode() == StabilizationMode.ON) {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
        }
    }

    /**
     * Builds a {@link CaptureRequest} from a {@link CaptureConfig} and a {@link CameraDevice}.
     *
     * <p>It uses configuredSurfaceMap to get the target surfaces from a {@link DeferrableSurface}.
     *
     * @param captureConfig        which {@link CaptureConfig} to build {@link CaptureRequest}
     * @param device               {@link CameraDevice} to create the {@link CaptureRequest}
     * @param configuredSurfaceMap A map of {@link DeferrableSurface} to {@link Surface}
     * @param isRepeatingRequest   whether it is building a repeating request or not
     */
    @Nullable
    public static CaptureRequest build(@NonNull CaptureConfig captureConfig,
            @Nullable CameraDevice device,
            @NonNull Map<DeferrableSurface, Surface> configuredSurfaceMap,
            boolean isRepeatingRequest, @NonNull TemplateParamsOverride mTemplateParamsOverride)
            throws CameraAccessException {
        if (device == null) {
            return null;
        }

        List<Surface> surfaceList = getConfiguredSurfaces(captureConfig.getSurfaces(),
                configuredSurfaceMap);
        if (surfaceList.isEmpty()) {
            return null;
        }

        CaptureRequest.Builder builder;
        CameraCaptureResult cameraCaptureResult = captureConfig.getCameraCaptureResult();
        if (Build.VERSION.SDK_INT >= 23
                && captureConfig.getTemplateType() == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                && cameraCaptureResult != null
                && cameraCaptureResult.getCaptureResult() instanceof TotalCaptureResult) {
            Logger.d(TAG, "createReprocessCaptureRequest");
            builder = Api23Impl.createReprocessCaptureRequest(
                    device, (TotalCaptureResult) cameraCaptureResult.getCaptureResult());
        } else {
            Logger.d(TAG, "createCaptureRequest");
            if (captureConfig.getTemplateType() == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG) {
                // Fallback template type to the same as regular capture mode when ZSL is disabled
                int templateType = isRepeatingRequest ? CameraDevice.TEMPLATE_PREVIEW :
                        CameraDevice.TEMPLATE_STILL_CAPTURE;
                builder = device.createCaptureRequest(templateType);
            } else {
                builder = device.createCaptureRequest(captureConfig.getTemplateType());
            }
        }

        applyTemplateParamsOverrideWorkaround(builder, captureConfig.getTemplateType(),
                mTemplateParamsOverride);

        applyAeFpsRange(captureConfig, builder);

        applyVideoStabilization(captureConfig, builder);

        if (captureConfig.getImplementationOptions().containsOption(
                CaptureConfig.OPTION_ROTATION)) {
            builder.set(CaptureRequest.JPEG_ORIENTATION,
                    captureConfig.getImplementationOptions().retrieveOption(
                            CaptureConfig.OPTION_ROTATION));
        }

        if (captureConfig.getImplementationOptions().containsOption(
                CaptureConfig.OPTION_JPEG_QUALITY)) {
            builder.set(CaptureRequest.JPEG_QUALITY,
                    captureConfig.getImplementationOptions().retrieveOption(
                            CaptureConfig.OPTION_JPEG_QUALITY).byteValue());
        }

        // This should be the last to be applied due to Camera2Interop values with higher priority
        // TODO: Properly use option priorities and tokens to ensure priorities are respected, but
        //  doesn't seem to have any issue due to this right now (still a bit error-prone).
        applyImplementationOptionToCaptureBuilder(builder,
                captureConfig.getImplementationOptions());

        for (Surface surface : surfaceList) {
            builder.addTarget(surface);
        }

        builder.setTag(captureConfig.getTagBundle());

        return builder.build();
    }

    /**
     * Return a {@link CaptureRequest} which include capture request parameters and
     * desired template type, but no target surfaces and tag.
     *
     * <p>Returns {@code null} if a valid {@link CaptureRequest} can not be constructed.
     */
    @Nullable
    public static CaptureRequest buildWithoutTarget(@NonNull CaptureConfig captureConfig,
            @Nullable CameraDevice device, @NonNull TemplateParamsOverride templateParamsOverride)
            throws CameraAccessException {
        if (device == null) {
            return null;
        }
        Logger.d(TAG, "template type = " + captureConfig.getTemplateType());
        CaptureRequest.Builder builder = device.createCaptureRequest(
                captureConfig.getTemplateType());

        applyTemplateParamsOverrideWorkaround(builder, captureConfig.getTemplateType(),
                templateParamsOverride);

        applyAeFpsRange(captureConfig, builder);

        applyImplementationOptionToCaptureBuilder(builder,
                captureConfig.getImplementationOptions());

        return builder.build();
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        static CaptureRequest.Builder createReprocessCaptureRequest(
                @NonNull CameraDevice cameraDevice,
                @NonNull TotalCaptureResult totalCaptureResult)
                throws CameraAccessException {
            return cameraDevice.createReprocessCaptureRequest(totalCaptureResult);
        }

    }
}
