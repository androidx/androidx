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
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants

/** Test-only implementation of [androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeAudioTrackExtensionsWrapper : AudioTrackExtensionsWrapper {

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [PointSourceParams] that were set on a specific
     * [AudioTrack] via the [setPointSourceParams] method. It is also used by the fake
     * [getPointSourceParams] to return a value, allowing tests to control its behavior.
     */
    public val pointSourceParamsMap: MutableMap<AudioTrack, PointSourceParams?> = mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [Entity] that were set on a specific [AudioTrack] via
     * the [setPointSourceParams] method.
     */
    public val entityMap: MutableMap<AudioTrack, Entity?> = mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [PointSourceParams] that were associated with an
     * [AudioTrack.Builder] via the [setPointSourceParams] builder method. This is useful for
     * verifying that the correct parameters were passed during the audio track configuration
     * process.
     */
    public val pointSourceParamsBuilderMap: MutableMap<AudioTrack.Builder, PointSourceParams?> =
        mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [Entity] that were associated with an
     * [AudioTrack.Builder] via the [setPointSourceParams] builder method. This is useful for
     * verifying that the correct parameters were passed during the audio track configuration
     * process.
     */
    public val entityBuilderMap: MutableMap<AudioTrack.Builder, Entity?> = mutableMapOf()

    override fun getPointSourceParams(track: AudioTrack): PointSourceParams? {
        return pointSourceParamsMap[track]
    }

    private val soundFieldAttributesMap: MutableMap<AudioTrack, SoundFieldAttributes?> =
        mutableMapOf()

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [SoundFieldAttributes] that were associated with an
     * [AudioTrack.Builder] via the [setSoundFieldAttributes] builder method. This is useful for
     * verifying that the correct attributes were passed during the audio track configuration
     * process.
     */
    public val soundFieldAttributesBuilderMap:
        MutableMap<AudioTrack.Builder, SoundFieldAttributes?> =
        mutableMapOf()

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

    @SpatializerConstants.SourceType
    override fun getSpatialSourceType(track: AudioTrack): Int {
        return (spatialSourceTypeMap[track] ?: SpatializerConstants.SOURCE_TYPE_BYPASS)
    }

    /**
     * For test purposes only. If non-null, methods in this class can throw this exception to
     * simulate runtime failures.
     *
     * This allows tests to verify how the client code handles various exceptions thrown by the
     * audio track extension layer. It can be set to any subclass of [Throwable], including specific
     * exceptions like [IllegalStateException] or even [Error]s to test edge cases.
     */
    public var fakeExtensionException: Throwable? = null

    override fun setPointSourceParams(
        track: AudioTrack,
        params: PointSourceParams,
        entity: Entity?,
    ) {
        fakeExtensionException?.let { throw it }

        when (getSpatialSourceType(track)) {
            SpatializerConstants.SOURCE_TYPE_BYPASS,
            SpatializerConstants.SOURCE_TYPE_POINT_SOURCE -> {
                pointSourceParamsMap[track] = params
                entityMap[track] = entity
            }
        }
    }

    override fun setPointSourceParams(
        builder: AudioTrack.Builder,
        params: PointSourceParams,
        entity: Entity?,
    ): AudioTrack.Builder {
        pointSourceParamsBuilderMap[builder] = params
        entityBuilderMap[builder] = entity
        return builder
    }

    override fun setSoundFieldAttributes(
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes,
    ): AudioTrack.Builder {
        soundFieldAttributesBuilderMap[builder] = attributes
        return builder
    }

    /**
     * For test purposes only. Manually sets the [SoundFieldAttributes] for a given [AudioTrack].
     *
     * This function allows tests to directly populate the [soundFieldAttributesMap], controlling
     * the value that will be returned by [getSoundFieldAttributes] for the specified `track`. This
     * is useful for simulating scenarios where an audio track has specific sound field properties
     * without needing to use an `AudioTrack.Builder`.
     *
     * @param track The [AudioTrack] instance whose sound field attributes are to be set.
     * @param attributes The [SoundFieldAttributes] to associate with the `track`.
     */
    public fun setSoundFieldAttributes(track: AudioTrack, attributes: SoundFieldAttributes) {
        soundFieldAttributesMap[track] = attributes
    }
}
