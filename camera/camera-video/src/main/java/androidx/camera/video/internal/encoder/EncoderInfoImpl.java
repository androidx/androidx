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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Objects;

/**
 * An EncoderInfo base implementation providing encoder related information and capabilities.
 *
 * <p>The implementation wraps and queries {@link MediaCodecInfo} relevant capability classes
 * such as {@link MediaCodecInfo.CodecCapabilities} and
 * {@link MediaCodecInfo.EncoderCapabilities}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class EncoderInfoImpl implements EncoderInfo {
    private final MediaCodecInfo mMediaCodecInfo;
    protected final MediaCodecInfo.CodecCapabilities mCodecCapabilities;

    EncoderInfoImpl(@NonNull MediaCodecInfo codecInfo, @NonNull String mime)
            throws InvalidConfigException {
        mMediaCodecInfo = codecInfo;
        try {
            mCodecCapabilities = Objects.requireNonNull(codecInfo.getCapabilitiesForType(mime));
        } catch (RuntimeException e) {
            // MediaCodecInfo.getCapabilitiesForType(mime) will throw exception if the mime is not
            // supported.
            throw new InvalidConfigException("Unable to get CodecCapabilities for mime: " + mime,
                    e);
        }
    }

    @Override
    @NonNull
    public String getName() {
        return mMediaCodecInfo.getName();
    }
}
