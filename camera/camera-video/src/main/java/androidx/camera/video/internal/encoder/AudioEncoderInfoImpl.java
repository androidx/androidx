/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.video.internal.encoder;

import static androidx.camera.video.internal.utils.CodecUtil.findCodecAndGetCodecInfo;

import android.media.MediaCodecInfo;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Objects;

/**
 * AudioEncoderInfoImpl provides audio encoder related information and capabilities.
 *
 * <p>The implementation wraps and queries {@link MediaCodecInfo} relevant capability classes
 * such as {@link MediaCodecInfo.CodecCapabilities}, {@link MediaCodecInfo.EncoderCapabilities}
 * and {@link MediaCodecInfo.AudioCapabilities}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AudioEncoderInfoImpl extends EncoderInfoImpl implements AudioEncoderInfo {

    private final MediaCodecInfo.AudioCapabilities mAudioCapabilities;

    /**
     * Returns an AudioEncoderInfoImpl from a AudioEncoderConfig.
     *
     * <p>The input AudioEncoderConfig is used to find the corresponding encoder.
     *
     * @throws InvalidConfigException if the encoder is not found.
     */
    @NonNull
    public static AudioEncoderInfoImpl from(@NonNull AudioEncoderConfig encoderConfig)
            throws InvalidConfigException {
        return new AudioEncoderInfoImpl(findCodecAndGetCodecInfo(encoderConfig),
                encoderConfig.getMimeType());
    }

    AudioEncoderInfoImpl(@NonNull MediaCodecInfo codecInfo, @NonNull String mime)
            throws InvalidConfigException {
        super(codecInfo, mime);
        mAudioCapabilities = Objects.requireNonNull(mCodecCapabilities.getAudioCapabilities());
    }

    @NonNull
    @Override
    public Range<Integer> getBitrateRange() {
        return mAudioCapabilities.getBitrateRange();
    }
}
