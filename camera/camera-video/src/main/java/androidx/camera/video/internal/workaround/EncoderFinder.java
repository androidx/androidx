/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video.internal.workaround;

import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.ExcludeKeyFrameRateInFindEncoderQuirk;

/**
 * Workaround to fix the selection of video encoder by MediaFormat on API 21.
 *
 * @see ExcludeKeyFrameRateInFindEncoderQuirk
 */
public class EncoderFinder {
    private final boolean mShouldRemoveKeyFrameRate;

    public EncoderFinder() {
        final ExcludeKeyFrameRateInFindEncoderQuirk quirk =
                DeviceQuirks.get(ExcludeKeyFrameRateInFindEncoderQuirk.class);

        mShouldRemoveKeyFrameRate = (quirk != null);
    }

    /**
     * Selects an encoder by a given MediaFormat.
     *
     * <p>The encoder finder might temporarily alter the media format for better compatibility
     * based on OS version. It is not thread safe to use the same media format instance.
     */
    @Nullable
    public String findEncoderForFormat(@NonNull MediaFormat mediaFormat,
            @NonNull MediaCodecList mediaCodecList) {
        Integer tempFrameRate = null;
        Integer tempAacProfile = null;
        try {
            // If the frame rate value is assigned, keep it and restore it later.
            if (mShouldRemoveKeyFrameRate && mediaFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                tempFrameRate = Integer.valueOf(mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE));
                // Reset frame rate value in API 21.
                mediaFormat.setString(MediaFormat.KEY_FRAME_RATE, null);
            }

            // TODO(b/192129356): Remove KEY_AAC_PROFILE when API <= 23 in order to find an encoder
            //  name or it will get null. This is currently needed for not blocking e2e/MH test.
            //  After the bug has been clarified, the workaround should be removed or a quirk should
            //  be added.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M && mediaFormat.containsKey(
                    MediaFormat.KEY_AAC_PROFILE)) {
                tempAacProfile = Integer.valueOf(
                        mediaFormat.getInteger(MediaFormat.KEY_AAC_PROFILE));
                mediaFormat.setString(MediaFormat.KEY_AAC_PROFILE, null);
            }

            return mediaCodecList.findEncoderForFormat(mediaFormat);
        } finally {
            // Restore the frame rate value.
            if (tempFrameRate != null) {
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, tempFrameRate.intValue());
            }

            // Restore the aac profile value.
            if (tempAacProfile != null) {
                mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, tempAacProfile.intValue());
            }
        }
    }
}
