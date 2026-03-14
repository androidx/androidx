/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo
import androidx.media3.exoplayer.audio.AudioOutputProvider

/** A Component that provides positional spatial audio playback for an [Entity]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface PositionalAudioComponent : Component {

    /**
     * Returns an [AudioOutputProvider] that can be used to configure an
     * [androidx.media3.exoplayer.ExoPlayer.Builder] for positional audio playback.
     */
    public fun getAudioOutputProvider(): AudioOutputProvider

    /**
     * Sets the [PointSourceParams] to control params of the spatial audio source.
     *
     * These params will apply to currently playing audio and future playback requests.
     */
    public fun setPointSourceParams(params: PointSourceParams)
}
