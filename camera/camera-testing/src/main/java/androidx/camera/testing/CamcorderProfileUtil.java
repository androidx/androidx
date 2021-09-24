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

package androidx.camera.testing;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CamcorderProfileProxy;

/**
 * Utility methods for testing {@link CamcorderProfile} related classes, including predefined
 * resolutions, attributes and {@link CamcorderProfileProxy}, which can be used directly on the
 * unit tests.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CamcorderProfileUtil {

    private CamcorderProfileUtil() {
    }

    /** Resolution for QCIF. */
    public static final Size RESOLUTION_QCIF = new Size(176, 144);
    /** Resolution for QVGA. */
    public static final Size RESOLUTION_QVGA = new Size(320, 240);
    /** Resolution for CIF. */
    public static final Size RESOLUTION_CIF = new Size(352, 288);
    /** Resolution for VGA. */
    public static final Size RESOLUTION_VGA = new Size(640, 480);
    /** Resolution for 480P. */
    public static final Size RESOLUTION_480P = new Size(720, 480); /* 640, 704 or 720 x 480 */
    /** Resolution for 720P. */
    public static final Size RESOLUTION_720P = new Size(1280, 720);
    /** Resolution for 1080P. */
    public static final Size RESOLUTION_1080P = new Size(1920, 1080); /* 1920 x 1080 or 1088 */
    /** Resolution for 2K. */
    public static final Size RESOLUTION_2K = new Size(2048, 1080);
    /** Resolution for QHD. */
    public static final Size RESOLUTION_QHD = new Size(2560, 1440);
    /** Resolution for 2160P. */
    public static final Size RESOLUTION_2160P = new Size(3840, 2160);
    /** Resolution for 4KDCI. */
    public static final Size RESOLUTION_4KDCI = new Size(4096, 2160);

    /** Default duration. */
    public static final int DEFAULT_DURATION = 30;
    /** Default output format. */
    public static final int DEFAULT_OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4;
    /** Default video codec. */
    public static final int DEFAULT_VIDEO_CODEC = MediaRecorder.VideoEncoder.H264;
    /** Default video bitrate. */
    public static final int DEFAULT_VIDEO_BITRATE = 8 * 1024 * 1024;
    /** Default video frame rate. */
    public static final int DEFAULT_VIDEO_FRAME_RATE = 30;
    /** Default audio codec. */
    public static final int DEFAULT_AUDIO_CODEC = MediaRecorder.AudioEncoder.AAC;
    /** Default audio bitrate. */
    public static final int DEFAULT_AUDIO_BITRATE = 192_000;
    /** Default audio sample rate. */
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 48_000;
    /** Default channel count. */
    public static final int DEFAULT_AUDIO_CHANNELS = 1;

    public static final CamcorderProfileProxy PROFILE_QCIF = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_QCIF,
            RESOLUTION_QCIF.getWidth(),
            RESOLUTION_QCIF.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_QVGA = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_QVGA,
            RESOLUTION_QVGA.getWidth(),
            RESOLUTION_QVGA.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_CIF = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_CIF,
            RESOLUTION_CIF.getWidth(),
            RESOLUTION_CIF.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_VGA = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_VGA,
            RESOLUTION_VGA.getWidth(),
            RESOLUTION_VGA.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_480P = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_480P,
            RESOLUTION_480P.getWidth(),
            RESOLUTION_480P.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_720P = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_720P,
            RESOLUTION_720P.getWidth(),
            RESOLUTION_720P.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_1080P = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_1080P,
            RESOLUTION_1080P.getWidth(),
            RESOLUTION_1080P.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_2K = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_2K,
            RESOLUTION_2K.getWidth(),
            RESOLUTION_2K.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_QHD = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_QHD,
            RESOLUTION_QHD.getWidth(),
            RESOLUTION_QHD.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_2160P = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_2160P,
            RESOLUTION_2160P.getWidth(),
            RESOLUTION_2160P.getHeight()
    );

    public static final CamcorderProfileProxy PROFILE_4KDCI = createCamcorderProfileProxy(
            CamcorderProfile.QUALITY_4KDCI,
            RESOLUTION_4KDCI.getWidth(),
            RESOLUTION_4KDCI.getHeight()
    );

    /** A utility method to create a CamcorderProfileProxy with some default values. */
    @NonNull
    public static CamcorderProfileProxy createCamcorderProfileProxy(
            int quality,
            int videoFrameWidth,
            int videoFrameHeight
    ) {
        return CamcorderProfileProxy.create(
                DEFAULT_DURATION,
                quality,
                DEFAULT_OUTPUT_FORMAT,
                DEFAULT_VIDEO_CODEC,
                DEFAULT_VIDEO_BITRATE,
                DEFAULT_VIDEO_FRAME_RATE,
                videoFrameWidth,
                videoFrameHeight,
                DEFAULT_AUDIO_CODEC,
                DEFAULT_AUDIO_BITRATE,
                DEFAULT_AUDIO_SAMPLE_RATE,
                DEFAULT_AUDIO_CHANNELS
        );
    }

    /**
     * Copies a CamcorderProfileProxy and sets the quality to
     * {@link CamcorderProfile#QUALITY_HIGH}.
     */
    @NonNull
    public static CamcorderProfileProxy asHighQuality(@NonNull CamcorderProfileProxy profile) {
        return asQuality(profile, CamcorderProfile.QUALITY_HIGH);
    }

    /**
     * Copies a CamcorderProfileProxy and sets the quality to
     * {@link CamcorderProfile#QUALITY_LOW}.
     */
    @NonNull
    public static CamcorderProfileProxy asLowQuality(@NonNull CamcorderProfileProxy profile) {
        return asQuality(profile, CamcorderProfile.QUALITY_LOW);
    }

    private static CamcorderProfileProxy asQuality(@NonNull CamcorderProfileProxy profile,
            int quality) {
        return CamcorderProfileProxy.create(
                profile.getDuration(),
                quality,
                profile.getFileFormat(),
                profile.getVideoCodec(),
                profile.getVideoBitRate(),
                profile.getVideoFrameRate(),
                profile.getVideoFrameWidth(),
                profile.getVideoFrameHeight(),
                profile.getAudioCodec(),
                profile.getAudioBitRate(),
                profile.getAudioSampleRate(),
                profile.getAudioChannels()
        );
    }
}
