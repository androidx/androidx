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

import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.video.MediaSpec

/**
 * Data class containing information about the media container format.
 *
 * This includes the resolved output format (e.g., MPEG-4, WebM) and the [EncoderProfilesProxy] that
 * provided the device capabilities for this specific container.
 */
public data class ContainerInfo(
    /**
     * Returns the output format of the container.
     *
     * @return The format as defined in [MediaSpec.OutputFormat], such as
     *   [MediaSpec.OUTPUT_FORMAT_MPEG_4].
     */
    @property:MediaSpec.OutputFormat val outputFormat: Int,

    /**
     * Returns the compatible [EncoderProfilesProxy] used to resolve the settings for this
     * container.
     *
     * If no profiles are available or compatible with the requested configuration, returns `null`.
     */
    val compatibleEncoderProfiles: EncoderProfilesProxy?,
)
