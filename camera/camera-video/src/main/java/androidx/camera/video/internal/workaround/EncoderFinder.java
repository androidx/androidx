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
     * Select an encoder by a given MediaFormat.
     * There is one particular case when get a video encoder on API 21.
     */
    @Nullable
    public String findEncoderForFormat(@NonNull MediaFormat mediaFormat,
            @NonNull MediaCodecList mediaCodecList) {
        // If the frame rate value is assigned, keep it and restore it later.
        String encoderName;

        if (mShouldRemoveKeyFrameRate && mediaFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            int tempFrameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            // Reset frame rate value in API 21.
            mediaFormat.setString(MediaFormat.KEY_FRAME_RATE, null);
            encoderName = mediaCodecList.findEncoderForFormat(mediaFormat);
            // Restore the frame rate value.
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, tempFrameRate);
        } else {
            encoderName = mediaCodecList.findEncoderForFormat(mediaFormat);
        }

        return encoderName;
    }

}
