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

package androidx.camera.video.internal.compat.quirk;

import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaRecorder.VideoEncoder;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CamcorderProfileProvider;
import androidx.camera.video.Quality;

/**
 * Quirk where qualities reported as available by {@link CamcorderProfileProvider#hasProfile(int)}
 * does not work on the device, and should not be used.
 *
 * <p>QuirkSummary
 *      Bug Id:      202080832
 *      Description: On devices exhibiting this quirk, {@link CamcorderProfile} indicates it
 *                   can support resolutions for a specific video encoder (e.g., 3840x2160 for
 *                   {@link VideoEncoder#H264} on Huawei Mate 20), and it can create the video
 *                   encoder by the corresponding format. However, the camera is unable to produce
 *                   video frames when configured with a {@link MediaCodec} surface at the
 *                   specified resolution. On these devices, the capture session is opened and
 *                   configured, but an error occurs in the HAL. See b/202080832#comment8
 *                   for details of this error.
 *      Device(s):   Huawei Mate 20, Huawei Mate 20 Pro
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ReportedVideoQualityNotSupportedQuirk implements VideoQualityQuirk {
    static boolean load() {
        return isHuaweiMate20() || isHuaweiMate20Pro();
    }

    private static boolean isHuaweiMate20() {
        return "Huawei".equalsIgnoreCase(Build.BRAND) && "HMA-L29".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isHuaweiMate20Pro() {
        return "Huawei".equalsIgnoreCase(Build.BRAND) && "LYA-AL00".equalsIgnoreCase(Build.MODEL);
    }

    /** Checks if the given mime type is a problematic quality. */
    @Override
    public boolean isProblematicVideoQuality(@NonNull Quality quality) {
        return quality == Quality.UHD;
    }
}
