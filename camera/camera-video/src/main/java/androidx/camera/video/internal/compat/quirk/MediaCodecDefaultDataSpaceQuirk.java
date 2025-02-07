/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk;

import static androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT601_625;
import static androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT709;
import static androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED;

import android.os.Build;

import androidx.camera.core.impl.Quirk;
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * <p>QuirkSummary
 *     Bug Id: b/382186115
 *     Description: Quirk denotes that MediaCodec incorrectly infers the data space from the video
 *                  resolution, resulting in a red tint in video recorded on certain Pixel devices.
 *                  This incorrect inference is likely due to the OpenGL pipeline between the camera
 *                  and MediaCodec disrupting the data space information flow from the camera.
 *                  As a result, MediaCodec relies on a resolution-based heuristic, incorrectly
 *                  associating UHD with BT2020 color standard.
 *     Device(s): OnePlus, Oppo, Pixel, and Xiaomi devices
 */
public class MediaCodecDefaultDataSpaceQuirk implements Quirk {

    private static final List<String> BT601_ONE_PLUS_MODELS = Collections.singletonList(
            "cph2449"
    );

    private static final List<String> BT601_OPPO_MODELS = Arrays.asList(
            "cph2437",
            "pht110"
    );

    private static final List<String> BT601_PIXEL_MODELS = Arrays.asList(
            "pixel 3",
            "pixel 3a xl",
            "pixel 4",
            "pixel 4a",
            "pixel 4a (5g)",
            "pixel 4 xl",
            "pixel 5",
            "pixel 5a"
    );

    private static final List<String> BT709_PIXEL_MODELS = Arrays.asList(
            "pixel fold",
            "pixel 6",
            "pixel 6a",
            "pixel 6 pro",
            "pixel 7",
            "pixel 7a",
            "pixel 7 pro",
            "pixel 8",
            "pixel 8a",
            "pixel 8 pro",
            "pixel 9",
            "pixel 9 pro",
            "pixel 9 pro xl",
            "pixel 9 pro fold"
    );

    private static final List<String> BT601_XIAOMI_MODELS = Collections.singletonList(
            "m2101k7ag"
    );

    private static final List<String> BT709_XIAOMI_MODELS = Collections.singletonList(
            "2307pnd5g"
    );


    static boolean load() {
        return isAffectedBT601Devices() || isAffectedBT709Devices();
    }

    private static boolean isAffectedBT601Devices() {
        return isAffectedBT601OnePlusModel() || isAffectedBT601OppoModel()
                || isAffectedBT601PixelModel() || isAffectedBT601XiaomiModel();
    }

    private static boolean isAffectedBT709Devices() {
        return isAffectedBT709PixelModel() || isAffectedBT709XiaomiModel();
    }

    private static boolean isAffectedBT601OnePlusModel() {
        return BT601_ONE_PLUS_MODELS.contains(Build.MODEL.toLowerCase(Locale.ROOT));
    }

    private static boolean isAffectedBT601OppoModel() {
        return BT601_OPPO_MODELS.contains(Build.MODEL.toLowerCase(Locale.ROOT));
    }

    private static boolean isAffectedBT601PixelModel() {
        return BT601_PIXEL_MODELS.contains(Build.MODEL.toLowerCase(Locale.ROOT));
    }

    private static boolean isAffectedBT601XiaomiModel() {
        return BT601_XIAOMI_MODELS.contains(Build.MODEL.toLowerCase(Locale.ROOT));
    }

    private static boolean isAffectedBT709PixelModel() {
        return BT709_PIXEL_MODELS.contains(Build.MODEL.toLowerCase(Locale.ROOT));
    }

    private static boolean isAffectedBT709XiaomiModel() {
        return BT709_XIAOMI_MODELS.contains(Build.MODEL.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the suggested {@link VideoEncoderDataSpace} for the current device.
     */
    @NonNull
    public VideoEncoderDataSpace getSuggestedDataSpace() {
        if (isAffectedBT709Devices()) {
            return ENCODER_DATA_SPACE_BT709;
        } else if (isAffectedBT601Devices()) {
            return ENCODER_DATA_SPACE_BT601_625;
        }

        return ENCODER_DATA_SPACE_UNSPECIFIED;
    }
}
