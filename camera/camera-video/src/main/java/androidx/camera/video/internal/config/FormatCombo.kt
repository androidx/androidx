/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.camera.video.MediaSpec

/**
 * A canonical representation of a valid media format combination for recording.
 *
 * @property container The output format (e.g., [MediaSpec.OUTPUT_FORMAT_MPEG_4]).
 * @property videoMime The MIME type of the video track. A `null` value represents an audio-only
 *   recording.
 * @property audioMime The MIME type of the audio track. A `null` value represents a video-only
 *   recording.
 */
public data class FormatCombo(
    @property:MediaSpec.OutputFormat public val container: Int,
    public val videoMime: String?,
    public val audioMime: String?,
) {
    init {
        require(videoMime != null || audioMime != null) {
            "FormatCombo must have at least one valid track. Both videoMime and audioMime cannot be null."
        }
    }
}
