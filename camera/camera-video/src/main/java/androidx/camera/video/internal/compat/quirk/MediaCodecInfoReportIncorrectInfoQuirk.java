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
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * Quirk which denotes {@link MediaCodecInfo} queried by {@link MediaCodecList} returns incorrect
 * info.
 *
 * <p>On Nokia 1, {@link CamcorderProfile} indicates it can support resolutions 1280x720 and
 * 640x480 for video codec type {@link android.media.MediaRecorder.VideoEncoder#MPEG_4_SP}, which
 * maps to mime type "video/mp4v-es".
 * The {@link MediaCodecInfo} searched by {@link MediaCodecList#getCodecInfos()} shows the
 * maximum supported resolution of "video/mp4v-es" is 174x174. Therefore,
 * {@link MediaCodecList#findEncoderForFormat} cannot find any supported codec by
 * the resolution provided by {@code CamcorderProfile} because it internally use
 * {@code MediaCodecInfo} to check the supported resolution. By testing,
 * "video/mp4v-es" with 1280x720 or 640x480 can be used to record video. So the maximum supported
 * resolution 174x174 is probably incorrect for "video/mp4v-es" and doesn't make sense.
 * See b/192431846#comment3.
 *
 * <p>On Huawei Mate9, {@link CamcorderProfile} indicates it can support resolutions 3840x2160 for
 *  video codec type {@link android.media.MediaRecorder.VideoEncoder#HEVC}, but the current video
 *  codec type is default {@link android.media.MediaRecorder.VideoEncoder#H264}.
 *  Even, change video codec type to {@link android.media.MediaRecorder.VideoEncoder#HEVC}, it
 *  still meet unsupported resolution for 3840x2160, it only support 3840x2112. By experimental
 *  result, H.264 + 3840x2160 can be used to record video on this device. Hence use quirk to
 *  workaround this case. See b/203481899#comment2.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MediaCodecInfoReportIncorrectInfoQuirk implements Quirk {

    static boolean load() {
        return isNokia1() || isHuaweiMate9();
    }

    private static boolean isNokia1() {
        return "Nokia".equalsIgnoreCase(Build.BRAND) && "Nokia 1".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isHuaweiMate9() {
        return "Huawei".equalsIgnoreCase(Build.BRAND) && "mha-l29".equalsIgnoreCase(Build.MODEL);
    }

    /** Check if problematic MediaFormat info for these candidate devices. */
    public boolean isUnSupportMediaCodecInfo(@NonNull MediaFormat mediaFormat) {
        if (isNokia1()) {
            /** Checks if the given mime type is a problematic mime type. */
            String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
            return MediaFormat.MIMETYPE_VIDEO_MPEG4.equals(mimeType);
        } else if (isHuaweiMate9()) {
            /** Checks if this is an unsupported resolution for avc. */
            int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            return (width == 3840 && height == 2160);
        }
        return false;
    }

}
