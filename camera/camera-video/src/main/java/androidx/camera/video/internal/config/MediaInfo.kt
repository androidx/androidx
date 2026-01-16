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

/**
 * A high-level aggregate class containing the resolved configuration for a recording session.
 *
 * MediaInfo serves as the final configuration blueprint, combining the container format with the
 * resolved video settings and optional audio settings.
 */
public data class MediaInfo(
    /** Returns the resolved container-level information, including the output format. */
    public val containerInfo: ContainerInfo,

    /**
     * Returns the resolved video configuration.
     *
     * This is a mandatory component of the MediaInfo.
     */
    public val videoMimeInfo: VideoMimeInfo,

    /**
     * Returns the resolved audio configuration, or `null` if audio is not enabled or no compatible
     * audio profile could be resolved.
     */
    public val audioMimeInfo: AudioMimeInfo?,
)
