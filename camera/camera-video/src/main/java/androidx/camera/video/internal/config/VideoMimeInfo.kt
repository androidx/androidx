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
package androidx.camera.video.internal.config

import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.video.internal.encoder.EncoderConfig

/**
 * Data class containing information about a video mime.
 *
 * The information includes all information from [MimeInfo] as well as compatible configuration
 * types that can be used to resolve settings, such as [EncoderProfilesProxy.VideoProfileProxy].
 */
public data class VideoMimeInfo(
    override val mimeType: String,
    override val profile: Int = EncoderConfig.CODEC_PROFILE_NONE,
    /**
     * Returns compatible [EncoderProfilesProxy.VideoProfileProxy] that can be used to resolve
     * settings.
     *
     * If no VideoProfileProxy is provided, returns `null`.
     */
    public val compatibleVideoProfile: EncoderProfilesProxy.VideoProfileProxy? = null,
) : MimeInfo
