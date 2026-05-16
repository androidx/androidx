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

package androidx.xr.scenecore.testing.internal

import android.media.AudioTrack
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants

/** Test-only implementation of [androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper] */
internal class FakeAudioTrackExtensionsWrapper : AudioTrackExtensionsWrapper {

    private val _pointSourceParamsMap: MutableMap<AudioTrack, PointSourceParams?> = mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [PointSourceParams] that were set on a specific
     * [AudioTrack] via the [setPointSourceParams] method. It is also used by the fake
     * [getPointSourceParams] to return a value, allowing tests to control its behavior.
     */
    val pointSourceParamsMap: Map<AudioTrack, PointSourceParams?>
        get() = _pointSourceParamsMap

    private val _entityMap: MutableMap<AudioTrack, Entity?> = mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [Entity] that were set on a specific [AudioTrack] via
     * the [setPointSourceParams] method.
     */
    val entityMap: Map<AudioTrack, Entity?>
        get() = _entityMap

    private val _pointSourceParamsBuilderMap: MutableMap<AudioTrack.Builder, PointSourceParams?> =
        mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [PointSourceParams] that were associated with an
     * [AudioTrack.Builder] via the [setPointSourceParams] builder method. This is useful for
     * verifying that the correct parameters were passed during the audio track configuration
     * process.
     */
    val pointSourceParamsBuilderMap: Map<AudioTrack.Builder, PointSourceParams?>
        get() = _pointSourceParamsBuilderMap

    private val _entityBuilderMap: MutableMap<AudioTrack.Builder, Entity?> = mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [Entity] that were associated with an
     * [AudioTrack.Builder] via the [setPointSourceParams] builder method. This is useful for
     * verifying that the correct parameters were passed during the audio track configuration
     * process.
     */
    val entityBuilderMap: Map<AudioTrack.Builder, Entity?>
        get() = _entityBuilderMap

    override fun getPointSourceParams(track: AudioTrack): PointSourceParams? {
        return pointSourceParamsMap[track]
    }

    val soundFieldAttributesMap: MutableMap<AudioTrack, SoundFieldAttributes?> = mutableMapOf()

    private val _soundFieldAttributesBuilderMap:
        MutableMap<AudioTrack.Builder, SoundFieldAttributes?> =
        mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [SoundFieldAttributes] that were associated with an
     * [AudioTrack.Builder] via the [setSoundFieldAttributes] builder method. This is useful for
     * verifying that the correct attributes were passed during the audio track configuration
     * process.
     */
    val soundFieldAttributesBuilderMap: Map<AudioTrack.Builder, SoundFieldAttributes?>
        get() = _soundFieldAttributesBuilderMap

    override fun getSoundFieldAttributes(track: AudioTrack): SoundFieldAttributes? {
        return soundFieldAttributesMap[track]
    }

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
     * If an [AudioTrack] is not found as a key in this map, [getSpatialSourceType] will default to
     * returning [androidx.xr.scenecore.runtime.SpatializerConstants.Companion.SOURCE_TYPE_BYPASS].
     */
    var spatialSourceTypeMap: MutableMap<AudioTrack, Int> = mutableMapOf()

    @SpatializerConstants.SourceType
    override fun getSpatialSourceType(track: AudioTrack): Int {
        return (spatialSourceTypeMap[track] ?: SpatializerConstants.SOURCE_TYPE_BYPASS)
    }

    override fun setPointSourceParams(
        track: AudioTrack,
        params: PointSourceParams,
        entity: Entity?,
    ) {
        when (getSpatialSourceType(track)) {
            SpatializerConstants.SOURCE_TYPE_BYPASS,
            SpatializerConstants.SOURCE_TYPE_POINT_SOURCE -> {
                _pointSourceParamsMap[track] = params
                _entityMap[track] = entity
            }
            SpatializerConstants.SOURCE_TYPE_SOUND_FIELD -> {
                throw IllegalStateException("Cannot set PointSourceParams to a POINT_SOURCE track.")
            }
        }
    }

    override fun setPointSourceParams(
        builder: AudioTrack.Builder,
        params: PointSourceParams,
        entity: Entity?,
    ): AudioTrack.Builder {
        _pointSourceParamsBuilderMap[builder] = params
        _entityBuilderMap[builder] = entity
        return builder
    }

    override fun setSoundFieldAttributes(
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes,
    ): AudioTrack.Builder {
        _soundFieldAttributesBuilderMap[builder] = attributes
        return builder
    }
}
