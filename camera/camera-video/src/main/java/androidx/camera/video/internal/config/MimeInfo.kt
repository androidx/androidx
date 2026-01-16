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
package androidx.camera.video.internal.config

import androidx.camera.video.internal.encoder.EncoderConfig

/**
 * An interface for data classes containing information about a media mime.
 *
 * The information included can include the mime type, profile and any compatible configuration
 * types that can be used to resolve settings.
 */
public interface MimeInfo {
    /** Returns the mime type. */
    public val mimeType: String

    /**
     * Returns the profile for the given mime.
     *
     * The returned integer will generally come from
     * [android.media.MediaCodecInfo.CodecProfileLevel], or if no profile is required,
     * [EncoderConfig.CODEC_PROFILE_NONE].
     */
    public val profile: Int
}
