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
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.SurfaceConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Quirk that requires specific resolutions as the workaround.
 *
 * <p> This is an allowlist that selects specific resolutions to override the app provided
 * resolutions. The resolution provided in this file have been manually tested by CameraX team.
 */
@RequiresApi(21)
public class SelectResolutionQuirk implements Quirk {

    private static final List<String> SAMSUNG_DISTORTION_MODELS = Arrays.asList(
            "SM-T580", // Samsung Galaxy Tab A (2016)
            "SM-J710MN", // Samsung Galaxy J7 (2016)
            "SM-A320FL", // Samsung Galaxy A3 (2017)
            "SM-G570M", // Samsung Galaxy J5 Prime
            "SM-G610F", // Samsung Galaxy J7 Prime
            "SM-G610M"); // Samsung Galaxy J7 Prime

    static boolean load() {
        return isSamsungDistortion();
    }

    /**
     * Selects a resolution based on {@link SurfaceConfig.ConfigType}.
     *
     * <p> The selected resolution have been manually tested by CameraX team. It is known to
     * work for the given device/stream.
     *
     * @return null if no resolution provided, in which case the calling code should fallback to
     * user provided target resolution.
     */
    @Nullable
    public Size selectResolution(@NonNull SurfaceConfig.ConfigType configType) {
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
     *
     * <p> The symptom of these devices is that the output of one or many streams, including PRIV,
     * JPEG and/or YUV, can have an extra 25% crop, and the cropped image is stretched to
     * fill the Surface, which results in a distorted output. The streams can also have an
     * extra 25% double crop, in which case the stretched image will not be distorted, but the
     * FOV is smaller than it should be.
     *
     * <p> The behavior is inconsistent in a way that the extra cropping depends on the
     * resolution of the streams. The existence of the issue also depends on API level and/or
     * build number. See discussion in go/samsung-camera-distortion.
     */
    private static boolean isSamsungDistortion() {
        return "samsung".equalsIgnoreCase(Build.BRAND)
                && SAMSUNG_DISTORTION_MODELS.contains(Build.MODEL.toUpperCase(Locale.US));
    }

}
