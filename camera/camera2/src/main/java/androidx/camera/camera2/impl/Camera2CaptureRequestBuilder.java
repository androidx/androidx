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

package androidx.camera.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.Config;
import androidx.camera.core.DeferrableSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to build a camera2 {@link CaptureRequest} from a {@link CaptureConfig}
 */
public class Camera2CaptureRequestBuilder {
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

    private static void applyImplementationOptionToCaptureBuilder(
            CaptureRequest.Builder builder, Config config) {
        Camera2Config camera2Config = new Camera2Config(config);
        for (Config.Option<?> option : camera2Config.getCaptureRequestOptions()) {
            /* Although type is erased below, it is safe to pass it to CaptureRequest.Builder
            because these option are created via Camera2Config.Extender.setCaptureRequestOption
            (CaptureRequest.Key<ValueT> key, ValueT value) and hence the type compatibility of key
            and value are ensured by the compiler. */
            @SuppressWarnings("unchecked")
            Config.Option<Object> typeErasedOption = (Config.Option<Object>) option;
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) option.getToken();

            // TODO(b/129997028): Error of setting unavailable CaptureRequest.Key may need to
            //  send back out to the developer
            try {
                // Ignores keys that don't exist
                builder.set(key, camera2Config.retrieveOption(typeErasedOption));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "CaptureRequest.Key is not supported: " + key);
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

        for (Surface surface : surfaceList) {
            builder.addTarget(surface);
        }

        builder.setTag(captureConfig.getTag());

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
