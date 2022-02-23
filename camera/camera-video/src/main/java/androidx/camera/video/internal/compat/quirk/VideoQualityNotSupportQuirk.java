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
import android.media.MediaRecorder.VideoEncoder;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CamcorderProfileProvider;
import androidx.camera.video.Quality;
import androidx.camera.video.VideoCapabilities;

/**
 * Quirk denotes that quality {@link VideoCapabilities} queried by {@link CamcorderProfileProvider}
 * does not work on video recording on device, and need to exclude it.
 *
 * <p>On Huawei Mate20 and Mate20 Pro, {@link CamcorderProfile} indicates it can support
 * resolutions 3840x2160 for {@link VideoEncoder#H264}, and it can create the video
 * encoder by the corresponding format. However, there is not any video frame output from Camera
 * . The CaptureSession is opened and configured, but something error in the HAL of these devices
 * . Hence use a quirk to exclude the problematic resolution quality. See b/202080832#comment8.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoQualityNotSupportQuirk implements VideoQualityQuirk {
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
