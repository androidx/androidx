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

package androidx.camera.video.internal.utils

import android.media.MediaFormat

/** A collection of extended keys and helper functions for [MediaFormat]. */
public object MediaFormatExt {

    /** The key for the first codec-specific data buffer. */
    public const val KEY_CSD_0: String = "csd-0"

    /** The key for the second codec-specific data buffer. */
    public const val KEY_CSD_1: String = "csd-1"

    /** The key for the third codec-specific data buffer. */
    public const val KEY_CSD_2: String = "csd-2"

    /** A key to indicate whether time-lapse/slow-motion is enabled. */
    public const val KEY_TIMELAPSE_ENABLED: String = "time-lapse-enable"

    /** A key to indicate the frame rate for time-lapse/slow-motion recording. */
    public const val KEY_TIMELAPSE_FPS: String = "time-lapse-fps"

    /**
     * Checks if the [MediaFormat] is for a video track.
     *
     * @return `true` if the format's MIME type starts with "video/", `false` otherwise.
     */
    public fun MediaFormat.isVideo(): Boolean =
        getString(MediaFormat.KEY_MIME)?.startsWith("video/") ?: false
}
