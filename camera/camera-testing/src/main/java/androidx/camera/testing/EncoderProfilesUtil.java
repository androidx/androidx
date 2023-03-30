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

package androidx.camera.testing;

import android.media.EncoderProfiles;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;

import java.util.Collections;

/**
 * Utility methods for testing {@link EncoderProfiles} related classes, including predefined
 * resolutions, attributes and {@link EncoderProfilesProxy}, which can be used directly on the
 * unit tests.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class EncoderProfilesUtil {

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
    /** Default media type. */
    public static final String DEFAULT_VIDEO_MEDIA_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    /** Default video bitrate. */
    public static final int DEFAULT_VIDEO_BITRATE = 8 * 1024 * 1024;
    /** Default video frame rate. */
    public static final int DEFAULT_VIDEO_FRAME_RATE = 30;
    /** Default video code profile. */
    public static final int DEFAULT_VIDEO_PROFILE = EncoderProfilesProxy.CODEC_PROFILE_NONE;
    /** Default bit depth. */
    public static final int DEFAULT_VIDEO_BIT_DEPTH = VideoProfileProxy.BIT_DEPTH_8;
    /** Default chroma subsampling. */
    public static final int DEFAULT_VIDEO_CHROMA_SUBSAMPLING = EncoderProfiles.VideoProfile.YUV_420;
    /** Default hdr format. */
    public static final int DEFAULT_VIDEO_HDR_FORMAT = EncoderProfiles.VideoProfile.HDR_NONE;
    /** Default audio codec. */
    public static final int DEFAULT_AUDIO_CODEC = MediaRecorder.AudioEncoder.AAC;
    /** Default media type. */
    public static final String DEFAULT_AUDIO_MEDIA_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    /** Default audio bitrate. */
    public static final int DEFAULT_AUDIO_BITRATE = 192_000;
    /** Default audio sample rate. */
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 48_000;
    /** Default channel count. */
    public static final int DEFAULT_AUDIO_CHANNELS = 1;
    /** Default audio code profile. */
    public static final int DEFAULT_AUDIO_PROFILE = EncoderProfilesProxy.CODEC_PROFILE_NONE;

    public static final EncoderProfilesProxy PROFILES_QCIF = createFakeEncoderProfilesProxy(
            RESOLUTION_QCIF.getWidth(),
            RESOLUTION_QCIF.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_QVGA = createFakeEncoderProfilesProxy(
            RESOLUTION_QVGA.getWidth(),
            RESOLUTION_QVGA.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_CIF = createFakeEncoderProfilesProxy(
            RESOLUTION_CIF.getWidth(),
            RESOLUTION_CIF.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_VGA = createFakeEncoderProfilesProxy(
            RESOLUTION_VGA.getWidth(),
            RESOLUTION_VGA.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_480P = createFakeEncoderProfilesProxy(
            RESOLUTION_480P.getWidth(),
            RESOLUTION_480P.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_720P = createFakeEncoderProfilesProxy(
            RESOLUTION_720P.getWidth(),
            RESOLUTION_720P.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_1080P = createFakeEncoderProfilesProxy(
            RESOLUTION_1080P.getWidth(),
            RESOLUTION_1080P.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_2K = createFakeEncoderProfilesProxy(
            RESOLUTION_2K.getWidth(),
            RESOLUTION_2K.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_QHD = createFakeEncoderProfilesProxy(
            RESOLUTION_QHD.getWidth(),
            RESOLUTION_QHD.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_2160P = createFakeEncoderProfilesProxy(
            RESOLUTION_2160P.getWidth(),
            RESOLUTION_2160P.getHeight()
    );

    public static final EncoderProfilesProxy PROFILES_4KDCI = createFakeEncoderProfilesProxy(
            RESOLUTION_4KDCI.getWidth(),
            RESOLUTION_4KDCI.getHeight()
    );

    /** A utility method to create an EncoderProfilesProxy with some default values. */
    @NonNull
    public static EncoderProfilesProxy createFakeEncoderProfilesProxy(
            int videoFrameWidth,
            int videoFrameHeight
    ) {
        VideoProfileProxy videoProfile = VideoProfileProxy.create(
                DEFAULT_VIDEO_CODEC,
                DEFAULT_VIDEO_MEDIA_TYPE,
                DEFAULT_VIDEO_BITRATE,
                DEFAULT_VIDEO_FRAME_RATE,
                videoFrameWidth,
                videoFrameHeight,
                DEFAULT_VIDEO_PROFILE,
                DEFAULT_VIDEO_BIT_DEPTH,
                DEFAULT_VIDEO_CHROMA_SUBSAMPLING,
                DEFAULT_VIDEO_HDR_FORMAT
        );
        AudioProfileProxy audioProfile = AudioProfileProxy.create(
                DEFAULT_AUDIO_CODEC,
                DEFAULT_AUDIO_MEDIA_TYPE,
                DEFAULT_AUDIO_BITRATE,
                DEFAULT_AUDIO_SAMPLE_RATE,
                DEFAULT_AUDIO_CHANNELS,
                DEFAULT_AUDIO_PROFILE
        );

        return ImmutableEncoderProfilesProxy.create(
                DEFAULT_DURATION,
                DEFAULT_OUTPUT_FORMAT,
                Collections.singletonList(audioProfile),
                Collections.singletonList(videoProfile)
        );
    }

    // This class is not instantiable.
    private EncoderProfilesUtil() {
    }
}
