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

package androidx.camera.camera2.internal.compat.quirk;

import android.os.Build;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.SurfaceConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Quirk that requires specific resolutions as the workaround.
 *
 * <p>QuirkSummary
 *     Bug Id: 190203334
 *     Description: The symptom of these devices is that the output of one or many streams,
 *                  including PRIV, JPEG and/or YUV, can have an unintended 25% crop, and the
 *                  cropped image is stretched to fill the Surface, which results in a distorted
 *                  output. The streams can also have an unintended 25% double crop, in which
 *                  case the stretched image will not be distorted, but the FOV is smaller than
 *                  it should be. The behavior is inconsistent in a way that the extra cropping
 *                  depends on the resolution of the streams. The existence of the issue also
 *                  depends on API level and/or build number. See discussion in
 *                  go/samsung-camera-distortion.
 *     Device(s): Samsung Galaxy Tab A (2016) SM-T580, Samsung Galaxy J7 (2016) SM-J710MN,
 *                Samsung Galaxy A3 (2017) SM-A320FL, Samsung Galaxy J5 Prime SM-G570M,
 *                Samsung Galaxy J7 Prime SM-G610F, Samsung Galaxy J7 Prime SM-G610M
 */
public class ExtraCroppingQuirk implements Quirk {

    private static final Map<String, Range<Integer>> SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP =
            new HashMap<>();

    static {
        SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP.put("SM-T580", null);
        SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP.put("SM-J710MN", new Range<>(21, 26));
        SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP.put("SM-A320FL", null);
        SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP.put("SM-G570M", null);
        SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP.put("SM-G610F", null);
        SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP.put("SM-G610M", new Range<>(21, 26));
    }

    static boolean load() {
        return isSamsungDistortion();
    }

    /**
     * Get a verified resolution that is guaranteed to work.
     *
     * <p> The selected resolution have been manually tested by CameraX team. It is known to
     * work for the given device/stream.
     *
     * @return null if no resolution provided, in which case the calling code should fallback to
     * user provided target resolution.
     */
    @Nullable
    public Size getVerifiedResolution(@NonNull SurfaceConfig.ConfigType configType) {
        if (isSamsungDistortion()) {
            // The following resolutions are needed for both the front and the back camera.
            switch (configType) {
                case PRIV:
                    return new Size(1920, 1080);
                case YUV:
                    return new Size(1280, 720);
                case JPEG:
                    return new Size(3264, 1836);
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * Checks for device model with Samsung output distortion bug (b/190203334).
     */
    private static boolean isSamsungDistortion() {
        boolean isDeviceModelContained = "samsung".equalsIgnoreCase(Build.BRAND)
                && SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP.containsKey(
                Build.MODEL.toUpperCase(Locale.US));

        if (!isDeviceModelContained) {
            return false;
        }

        Range<Integer> apiLevelRange =
                SAMSUNG_DISTORTION_MODELS_TO_API_LEVEL_MAP.get(Build.MODEL.toUpperCase(Locale.US));

        return apiLevelRange == null ? true : apiLevelRange.contains(Build.VERSION.SDK_INT);
    }

}
