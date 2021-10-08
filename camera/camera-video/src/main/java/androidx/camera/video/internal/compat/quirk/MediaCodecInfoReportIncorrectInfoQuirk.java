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
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MediaCodecInfoReportIncorrectInfoQuirk implements Quirk {

    private static final String BUILD_BRAND = "Nokia";

    private static final String BUILD_MODEL = "Nokia 1";

    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_MPEG4;

    static boolean load() {
        return BUILD_BRAND.equalsIgnoreCase(Build.BRAND)
                && BUILD_MODEL.equalsIgnoreCase(Build.MODEL);
    }

    /** Checks if the given mime type is a problematic mime type. */
    public static boolean isProblematicMimeType(@NonNull String mimeType) {
        return MIME_TYPE.equals(mimeType);
    }
}
