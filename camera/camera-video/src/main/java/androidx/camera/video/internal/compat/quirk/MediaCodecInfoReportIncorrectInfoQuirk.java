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

import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;
import static android.media.MediaFormat.MIMETYPE_VIDEO_MPEG4;

import android.media.CamcorderProfile;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: 192431846, 199582287, 218841498, 203481899, 216583006, 278843124, 278855948
 *     Description: Quirk which denotes {@link MediaCodecInfo} queried by {@link MediaCodecList}
 *                  returns incorrect info.
 *                  On Nokia 1, {@link CamcorderProfile} indicates it can support resolutions
 *                  1280x720 and 640x480 for video codec type
 *                  {@link android.media.MediaRecorder.VideoEncoder#MPEG_4_SP}, which maps to
 *                  mime type "video/mp4v-es". The {@link MediaCodecInfo} searched by
 *                  {@link MediaCodecList#getCodecInfos()} shows the maximum supported resolution
 *                  of "video/mp4v-es" is 174x174. Therefore,
 *                  {@link MediaCodecList#findEncoderForFormat} cannot find any supported codec
 *                  by the resolution provided by {@code CamcorderProfile} because it internally
 *                  use {@code MediaCodecInfo} to check the supported resolution. By testing,
 *                  "video/mp4v-es" with 1280x720 or 640x480 can be used to record video. So the
 *                  maximum supported resolution 174x174 is probably incorrect for
 *                  "video/mp4v-es" and doesn't make sense. See b/192431846#comment3.
 *                  Motc C, X650 and LG-X230 have the same problem as Nokia 1. See b/199582287
 *                  Positivo Twist 2 Pro have the same problem as Nokia 1. See b/218841498
 *                  On Huawei Mate9, {@link CamcorderProfile} indicates it can support
 *                  resolutions 3840x2160 for video codec type
 *                  {@link android.media.MediaRecorder.VideoEncoder#HEVC}, but the current video
 *                  codec type is default {@link android.media.MediaRecorder.VideoEncoder#H264}.
 *                  Even, change video codec type to
 *                  {@link android.media.MediaRecorder.VideoEncoder#HEVC}, it still meet
 *                  unsupported resolution for 3840x2160, it only support 3840x2112. By
 *                  experimental result, H.264 + 3840x2160 can be used to record video on this
 *                  device. Hence use quirk to workaround this case. See b/203481899#comment2.
 *                  @link MediaCodecInfo} searched by {@link MediaCodecList#getCodecInfos()}
 *                  shows the maximum supported resolution of the AVC encoder is 1920x1072.
 *                  However, the 1920x1080 option can be successfully configured properly.
 *                  See b/216583006, b/278843124, b/278855948.
 *     Device(s): Nokia 1, Motc C, X650, LG-X230, Positivo Twist 2 Pro, Huawei Mate9, Redmi note 4
 *                , LG K10 LTE K430, Samsung Galaxy A03 Core, Vivo Y75, Realme C11 2021
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MediaCodecInfoReportIncorrectInfoQuirk implements Quirk {

    static boolean load() {
        return isNokia1() || isMotoC() || isX650() || isX230() || isHuaweiMate9()
                || isPositivoTwist2Pro() || isFHDProblematicDevice();
    }

    private static boolean isNokia1() {
        return "Nokia".equalsIgnoreCase(Build.BRAND) && "Nokia 1".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isMotoC() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto c".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isX650() {
        return "infinix".equalsIgnoreCase(Build.BRAND)
                && "infinix x650".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isX230() {
        return "LGE".equalsIgnoreCase(Build.BRAND) && "LG-X230".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isHuaweiMate9() {
        return "Huawei".equalsIgnoreCase(Build.BRAND) && "mha-l29".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isPositivoTwist2Pro() {
        return "positivo".equalsIgnoreCase(Build.BRAND) && "twist 2 pro".equalsIgnoreCase(
                Build.MODEL);
    }

    public static final List<String> INCORRECT_FHD_PROFILE_MODEL_LIST = Arrays.asList(
            "lg-k430",
            "redmi note 4",
            "rmx3231",
            "v2117",
            "sm-a032f",
            "moto g(20)",
            "sm-a035m"
    );

    /** Check if problematic MediaFormat info for these candidate devices. */
    public boolean isUnSupportMediaCodecInfo(@NonNull MediaFormat mediaFormat) {
        MediaFormatResolver formatResolver = new MediaFormatResolver(mediaFormat);
        if (isNokia1() || isMotoC() || isX650() || isX230() || isPositivoTwist2Pro()) {
            return formatResolver.isMpeg4();
        } else if (isHuaweiMate9()) {
            return formatResolver.isVideo() && formatResolver.isSize(3840, 2160);
        } else if (isFHDProblematicDevice()) {
            return formatResolver.isAvc() && formatResolver.isSize(1920, 1080);
        }
        return false;
    }

    /**
     * Returns the extra supported sizes for the problematic devices if any, otherwise returns an
     * empty set.
     */
    @NonNull
    public static Set<Size> getExtraSupportedSizes() {
        if (isFHDProblematicDevice()) {
            return Collections.singleton(new Size(1920, 1080));
        }
        return Collections.emptySet();
    }

    private static boolean isFHDProblematicDevice() {
        return INCORRECT_FHD_PROFILE_MODEL_LIST.contains(Build.MODEL.toLowerCase(Locale.US));
    }

    private static class MediaFormatResolver {
        private final MediaFormat mMediaFormat;

        MediaFormatResolver(@NonNull MediaFormat mediaFormat) {
            mMediaFormat = mediaFormat;
        }

        boolean isVideo() {
            String mimeType = getMime();
            return mimeType != null && mimeType.contains("video/");
        }

        boolean isAvc() {
            return MIMETYPE_VIDEO_AVC.equalsIgnoreCase(getMime());
        }

        boolean isMpeg4() {
            return MIMETYPE_VIDEO_MPEG4.equalsIgnoreCase(getMime());
        }

        boolean isSize(int width, int height) {
            int formatWidth = mMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            int formatHeight = mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            return formatWidth == width && formatHeight == height;
        }

        @Nullable
        private String getMime() {
            return mMediaFormat.getString(MediaFormat.KEY_MIME);
        }
    }
}
