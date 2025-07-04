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

import android.media.MediaCodecInfo;
import android.util.Range;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * AudioEncoderInfoImpl provides audio encoder related information and capabilities.
 *
 * <p>The implementation wraps and queries {@link MediaCodecInfo} relevant capability classes
 * such as {@link MediaCodecInfo.CodecCapabilities}, {@link MediaCodecInfo.EncoderCapabilities}
 * and {@link MediaCodecInfo.AudioCapabilities}.
 */
public class AudioEncoderInfoImpl extends EncoderInfoImpl implements AudioEncoderInfo {

    private final MediaCodecInfo.AudioCapabilities mAudioCapabilities;

    AudioEncoderInfoImpl(@NonNull MediaCodecInfo codecInfo, @NonNull String mime)
            throws InvalidConfigException {
        super(codecInfo, mime);
        mAudioCapabilities = Objects.requireNonNull(mCodecCapabilities.getAudioCapabilities());
    }

    @Override
    public @NonNull Range<Integer> getBitrateRange() {
        return mAudioCapabilities.getBitrateRange();
    }
}
