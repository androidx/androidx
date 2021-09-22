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
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to build a camera2 {@link CaptureRequest} from a {@link CaptureConfig}
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class Camera2CaptureRequestBuilder {
    private Camera2CaptureRequestBuilder() {
    }

    private static final String TAG = "CaptureRequestBuilder";

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


    /**
     * Builds a {@link CaptureRequest} from a {@link CaptureConfig} and a {@link CameraDevice}.
     *
     * <p>It uses configuredSurfaceMap to get the target surfaces from a {@link DeferrableSurface}.
     *
     * @param captureConfig        which {@link CaptureConfig} to build {@link CaptureRequest}
     * @param device               {@link CameraDevice} to create the {@link CaptureRequest}
     * @param configuredSurfaceMap A map of {@link DeferrableSurface} to {@link Surface}
     */
    @Nullable
    public static CaptureRequest build(@NonNull CaptureConfig captureConfig,
            @Nullable CameraDevice device,
            @NonNull Map<DeferrableSurface, Surface> configuredSurfaceMap)
            throws CameraAccessException {
        if (device == null) {
            return null;
        }

        List<Surface> surfaceList = getConfiguredSurfaces(captureConfig.getSurfaces(),
                configuredSurfaceMap);
        if (surfaceList.isEmpty()) {
            return null;
        }

        CaptureRequest.Builder builder = device.createCaptureRequest(
                captureConfig.getTemplateType());

        applyImplementationOptionToCaptureBuilder(builder,
                captureConfig.getImplementationOptions());

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
            @Nullable CameraDevice device)
            throws CameraAccessException {
        if (device == null) {
            return null;
        }
        CaptureRequest.Builder builder = device.createCaptureRequest(
                captureConfig.getTemplateType());

        applyImplementationOptionToCaptureBuilder(builder,
                captureConfig.getImplementationOptions());

        return builder.build();
    }
}
