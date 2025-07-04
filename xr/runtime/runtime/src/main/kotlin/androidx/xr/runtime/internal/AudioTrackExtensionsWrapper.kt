/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.internal

import android.media.AudioTrack
import androidx.annotation.RestrictTo

/** Interface for a XR Runtime AudioTrackExtensionsWrapper */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface AudioTrackExtensionsWrapper {
    /**
     * Returns the [PointSourceParams] of the AudioTrack.
     *
     * @param track The AudioTrack to get the PointSourceParams from.
     * @return The PointSourceParams of the AudioTrack.
     */
    public fun getPointSourceParams(track: AudioTrack): PointSourceParams?

    /**
     * Returns the SoundFieldAttributes of the AudioTrack.
     *
     * @param track The AudioTrack to get the SoundFieldAttributes from.
     * @return The SoundFieldAttributes of the AudioTrack.
     */
    public fun getSoundFieldAttributes(track: AudioTrack): SoundFieldAttributes?

    /**
     * Returns the spatial source type of the AudioTrack.
     *
     * @param track The AudioTrack to get the spatial source type from.
     * @return The spatial source type of the AudioTrack.
     */
    @SpatializerConstants.SourceType public fun getSpatialSourceType(track: AudioTrack): Int

    /**
     * Sets the PointSourceParams of the AudioTrack.
     *
     * <p>The new PointSourceParams will be applied if the [SpatializerConstants.SourceType] of the
     * AudioTrack was either [SpatializerConstants.DEFAULT]0 or [SpatializerConstants.POINT_SOURCE].
     * If the [SpatializerConstants.SourceType] was [SpatializerConstants.SOUND_FIELD], then this
     * method will have no effect.
     *
     * @param track The AudioTrack to set the PointSourceParams on.
     * @param params The PointSourceParams to set.
     */
    public fun setPointSourceParams(track: AudioTrack, params: PointSourceParams)

    /**
     * Sets the PointSourceParams of the AudioTrack.
     *
     * @param builder The AudioTrack.Builder to set the PointSourceParams on.
     * @param params The PointSourceParams to set.
     * @return The AudioTrack.Builder with the PointSourceAttributes set.
     */
    public fun setPointSourceParams(
        builder: AudioTrack.Builder,
        params: PointSourceParams,
    ): AudioTrack.Builder

    /**
     * Sets the SoundFieldAttributes of the AudioTrack.
     *
     * @param builder The AudioTrack.Builder to set the SoundFieldAttributes on.
     * @param attributes The SoundFieldAttributes to set.
     * @return The AudioTrack.Builder with the SoundFieldAttributes set.
     */
    public fun setSoundFieldAttributes(
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes,
    ): AudioTrack.Builder
}
