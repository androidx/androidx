/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes;

import static android.graphics.ImageFormat.JPEG;
import static android.graphics.ImageFormat.YUV_420_888;

import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;

import static com.google.common.primitives.Ints.asList;

import android.util.Pair;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.streamsharing.StreamSharingConfig;
import androidx.camera.video.impl.VideoCaptureConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A CameraDeviceSurfaceManager which has no supported SurfaceConfigs. */
public final class FakeCameraDeviceSurfaceManager implements CameraDeviceSurfaceManager {

    public static final Size MAX_OUTPUT_SIZE = new Size(4032, 3024); // 12.2 MP

    private final Map<String, Map<Class<? extends UseCaseConfig<?>>, StreamSpec>>
            mDefinedStreamSpecs = new HashMap<>();

    private Set<List<Integer>> mValidSurfaceCombos = createDefaultValidSurfaceCombos();

    /**
     * Sets the given suggested stream specs for the specified camera Id and use case type.
     */
    public void setSuggestedStreamSpec(@NonNull String cameraId,
            @NonNull Class<? extends UseCaseConfig<?>> type,
            @NonNull StreamSpec streamSpec) {
        Map<Class<? extends UseCaseConfig<?>>, StreamSpec> useCaseConfigTypeToStreamSpecMap =
                mDefinedStreamSpecs.get(cameraId);
        if (useCaseConfigTypeToStreamSpecMap == null) {
            useCaseConfigTypeToStreamSpecMap = new HashMap<>();
            mDefinedStreamSpecs.put(cameraId, useCaseConfigTypeToStreamSpecMap);
        }

        useCaseConfigTypeToStreamSpecMap.put(type, streamSpec);
    }

    @Override
    @Nullable
    public SurfaceConfig transformSurfaceConfig(
            @CameraMode.Mode int cameraMode,
            @NonNull String cameraId,
            int imageFormat,
            @NonNull Size size) {

        //returns a placeholder SurfaceConfig
        return SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW);
    }

    @NonNull
    @Override
    public Pair<Map<UseCaseConfig<?>, StreamSpec>, Map<AttachedSurfaceInfo, StreamSpec>>
            getSuggestedStreamSpecs(
            @CameraMode.Mode int cameraMode,
            @NonNull String cameraId,
            @NonNull List<AttachedSurfaceInfo> existingSurfaces,
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap,
            boolean isPreviewStabilizationOn,
            boolean hasVideoCapture) {
        List<UseCaseConfig<?>> newUseCaseConfigs =
                new ArrayList<>(newUseCaseConfigsSupportedSizeMap.keySet());
        checkSurfaceCombo(existingSurfaces, newUseCaseConfigs);

        // Populate the suggested stream specs for new use cases.
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecs = new HashMap<>();
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            suggestedStreamSpecs.put(useCaseConfig,
                    getStreamSpec(cameraId, useCaseConfig.getClass(), hasVideoCapture));
        }

        // Populate the stream specs for existing use cases.
        Map<AttachedSurfaceInfo, StreamSpec> existingStreamSpecs = new HashMap<>();
        for (AttachedSurfaceInfo attachedSurfaceInfo : existingSurfaces) {
            existingStreamSpecs.put(attachedSurfaceInfo, getStreamSpec(cameraId,
                    captureTypeToUseCaseConfigType(attachedSurfaceInfo.getCaptureTypes().get(0)),
                    hasVideoCapture));
        }

        return new Pair<>(suggestedStreamSpecs, existingStreamSpecs);
    }

    @NonNull
    private StreamSpec getStreamSpec(@NonNull String cameraId, @NonNull Class<?> classType,
            boolean hasVideoCapture) {
        StreamSpec streamSpec = StreamSpec.builder(MAX_OUTPUT_SIZE)
                .setZslDisabled(hasVideoCapture)
                .build();
        Map<Class<? extends UseCaseConfig<?>>, StreamSpec> definedStreamSpecs =
                mDefinedStreamSpecs.get(cameraId);
        if (definedStreamSpecs != null) {
            StreamSpec definedStreamSpec = definedStreamSpecs.get(classType);
            if (definedStreamSpec != null) {
                streamSpec = definedStreamSpec;
            }
        }
        return streamSpec;
    }

    /**
     * Returns the {@link UseCaseConfig} type from a
     * {@link androidx.camera.core.impl.UseCaseConfigFactory.CaptureType}.
     */
    private Class<?> captureTypeToUseCaseConfigType(
            @NonNull UseCaseConfigFactory.CaptureType captureType) {
        switch (captureType) {
            case METERING_REPEATING:
                // Fall-through
            case PREVIEW:
                return PreviewConfig.class;
            case IMAGE_CAPTURE:
                return ImageCaptureConfig.class;
            case IMAGE_ANALYSIS:
                return ImageAnalysisConfig.class;
            case VIDEO_CAPTURE:
                return VideoCaptureConfig.class;
            case STREAM_SHARING:
                return StreamSharingConfig.class;
            default:
                throw new IllegalArgumentException("Invalid capture type.");
        }
    }

    /**
     * Checks if the surface combinations is supported.
     *
     * <p> Throws {@link IllegalArgumentException} if not supported.
     */
    private void checkSurfaceCombo(List<AttachedSurfaceInfo> existingSurfaceInfos,
            @NonNull List<UseCaseConfig<?>> newSurfaceConfigs) {
        // Combine existing Surface with new Surface
        List<Integer> currentCombo = new ArrayList<>();
        for (UseCaseConfig<?> useCaseConfig : newSurfaceConfigs) {
            currentCombo.add(useCaseConfig.getInputFormat());
        }
        for (AttachedSurfaceInfo surfaceInfo : existingSurfaceInfos) {
            currentCombo.add(surfaceInfo.getImageFormat());
        }
        // Loop through valid combinations and return early if the combo is supported.
        for (List<Integer> validCombo : mValidSurfaceCombos) {
            if (isComboSupported(currentCombo, validCombo)) {
                return;
            }
        }
        // Throw IAE if none of the valid combos supports the current combo.
        throw new IllegalArgumentException("Surface combo not supported");
    }

    /**
     * Checks if the app combination in covered by the given valid combination.
     */
    private boolean isComboSupported(@NonNull List<Integer> appCombo,
            @NonNull List<Integer> validCombo) {
        List<Integer> combo = new ArrayList<>(validCombo);
        for (Integer format : appCombo) {
            if (!combo.remove(format)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The default combination is similar to LEGACY level devices.
     */
    private static Set<List<Integer>> createDefaultValidSurfaceCombos() {
        Set<List<Integer>> validCombos = new HashSet<>();
        validCombos.add(asList(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, YUV_420_888, JPEG));
        validCombos.add(asList(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE));
        return validCombos;
    }

    public void setValidSurfaceCombos(@NonNull Set<List<Integer>> validSurfaceCombos) {
        mValidSurfaceCombos = validSurfaceCombos;
    }
}
