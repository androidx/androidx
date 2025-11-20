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

package androidx.xr.scenecore.testing

import android.media.AudioTrack
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants

/** Test-only implementation of [androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeAudioTrackExtensionsWrapper : AudioTrackExtensionsWrapper {

    private var pointSourceParamsMap: MutableMap<AudioTrack, PointSourceParams?> = mutableMapOf()

    /**
     * Returns the [androidx.xr.scenecore.runtime.PointSourceParams] of the AudioTrack.
     *
     * @param track The AudioTrack to get the PointSourceParams from.
     * @return The PointSourceParams of the AudioTrack.
     */
    override fun getPointSourceParams(track: AudioTrack): PointSourceParams? {
        return pointSourceParamsMap[track]
    }

    /**
     * For test purposes only.
     *
     * This map allows tests to control the [androidx.xr.scenecore.runtime.SoundFieldAttributes]
     * returned by [getSoundFieldAttributes] for specific [AudioTrack] instances. By pre-configuring
     * entries in this map, tests can simulate various sound field configurations for different
     * audio tracks.
     *
     * If an [AudioTrack] is not found as a key in this map, [getSoundFieldAttributes] for that
     * track will default to returning `null`.
     */
    public var soundFieldAttributesMap: MutableMap<AudioTrack, SoundFieldAttributes?> =
        mutableMapOf()

    /**
     * Returns the SoundFieldAttributes of the AudioTrack.
     *
     * @param track The AudioTrack to get the SoundFieldAttributes from.
     * @return The SoundFieldAttributes of the AudioTrack.
     */
    override fun getSoundFieldAttributes(track: AudioTrack): SoundFieldAttributes? {
        return soundFieldAttributesMap[track]
    }

    private var _spatialSourceTypeMap: MutableMap<AudioTrack, Int> = mutableMapOf()

    /**
     * For test purposes only.
     *
     * A map used to control the spatial source type returned by [getSpatialSourceType] for specific
     * [AudioTrack] instances. This allows tests to simulate different spatialization states by
     * pre-configuring the behavior of [getSpatialSourceType].
     *
     * Populate this map with [AudioTrack] instances as keys and their desired spatial source type
     * (an `Int` constant) as values. Valid source types include:
     * - [androidx.xr.scenecore.runtime.SpatializerConstants.Companion.SOURCE_TYPE_BYPASS]
     * - [androidx.xr.scenecore.runtime.SpatializerConstants.Companion.SOURCE_TYPE_POINT_SOURCE]
     * - [androidx.xr.scenecore.runtime.SpatializerConstants.Companion.SOURCE_TYPE_SOUND_FIELD]
     *
     * The custom setter for this property validates that all values in an assigned map are one of
     * the valid source types. If the validation fails, the assignment is ignored, and the map
     * remains unchanged.
     *
     * If an [AudioTrack] is not found as a key in this map, [getSpatialSourceType] will default to
     * returning [androidx.xr.scenecore.runtime.SpatializerConstants.Companion.SOURCE_TYPE_BYPASS].
     */
    public var spatialSourceTypeMap: MutableMap<AudioTrack, Int>
        get() = _spatialSourceTypeMap
        set(newMap) {
            if (
                newMap.values.all {
                    it in
                        listOf(
                            SpatializerConstants.SOURCE_TYPE_BYPASS,
                            SpatializerConstants.SOURCE_TYPE_POINT_SOURCE,
                            SpatializerConstants.SOURCE_TYPE_SOUND_FIELD,
                        )
                }
            ) {
                _spatialSourceTypeMap = newMap
            }
        }

    /**
     * Returns the spatial source type of the AudioTrack.
     *
     * @param track The AudioTrack to get the spatial source type from.
     * @return The spatial source type of the AudioTrack.
     */
    @SpatializerConstants.SourceType
    override fun getSpatialSourceType(track: AudioTrack): Int {
        return (spatialSourceTypeMap[track] ?: SpatializerConstants.SOURCE_TYPE_BYPASS)
    }

    /**
     * Sets the PointSourceParams of the AudioTrack.
     *
     * The new PointSourceParams will be applied if the
     * [androidx.xr.scenecore.runtime.SpatializerConstants.SourceType] of the AudioTrack was either
     * [androidx.xr.scenecore.runtime.SpatializerConstants.Companion.SOURCE_TYPE_BYPASS] or
     * [androidx.xr.scenecore.runtime.SpatializerConstants.Companion.SOURCE_TYPE_POINT_SOURCE]. If
     * the [androidx.xr.scenecore.runtime.SpatializerConstants.SourceType] was
     * [androidx.xr.scenecore.runtime.SpatializerConstants.Companion.SOURCE_TYPE_SOUND_FIELD], then
     * this method will have no effect.
     *
     * @param track The AudioTrack to set the PointSourceParams on.
     * @param params The PointSourceParams to set.
     */
    override fun setPointSourceParams(track: AudioTrack, params: PointSourceParams) {
        when (getSpatialSourceType(track)) {
            SpatializerConstants.SOURCE_TYPE_BYPASS,
            SpatializerConstants.SOURCE_TYPE_POINT_SOURCE -> {
                pointSourceParamsMap[track] = params
            }
        }
    }

    /**
     * Sets the PointSourceParams of the AudioTrack.
     *
     * @param builder The AudioTrack.Builder to set the PointSourceParams on.
     * @param params The PointSourceParams to set.
     * @return The AudioTrack.Builder with the PointSourceAttributes set.
     */
    override fun setPointSourceParams(
        builder: AudioTrack.Builder,
        params: PointSourceParams,
    ): AudioTrack.Builder = builder

    /**
     * Sets the SoundFieldAttributes of the AudioTrack.
     *
     * @param builder The AudioTrack.Builder to set the SoundFieldAttributes on.
     * @param attributes The SoundFieldAttributes to set.
     * @return The AudioTrack.Builder with the SoundFieldAttributes set.
     */
    override fun setSoundFieldAttributes(
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes,
    ): AudioTrack.Builder = builder
}
