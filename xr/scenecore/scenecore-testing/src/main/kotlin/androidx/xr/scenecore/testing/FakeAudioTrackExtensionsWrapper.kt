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

@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package androidx.xr.scenecore.testing

import android.media.AudioTrack
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants
import androidx.xr.scenecore.testing.internal.FakeAudioTrackExtensionsWrapper as InternalFakeAudioTrackExtensionsWrapper
import androidx.xr.scenecore.testing.internal.FakeEntity as InternalFakeEntity

/** Test-only implementation of [androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeAudioTrackExtensionsWrapper
internal constructor(internal var fakeInternal: InternalFakeAudioTrackExtensionsWrapper) :
    AudioTrackExtensionsWrapper {

    public constructor() : this(InternalFakeAudioTrackExtensionsWrapper())

    private val entityWrappers = mutableMapOf<InternalFakeEntity, FakeEntity>()

    /**
     * A proxy implementation of [Map] that translates internal entity representations back to their
     * public wrapper counterparts upon retrieval.
     *
     * This proxy works in conjunction with the [setPointSourceParams] methods. When
     * [setPointSourceParams] is called with an [AudioTrack] (which implicitly updates the
     * underlying [entityMap]) or an [AudioTrack.Builder] (which implicitly updates the underlying
     * [entityBuilderMap]), the mapping between the external [FakeEntity] and its corresponding
     * [InternalFakeEntity] is recorded in the `entityWrappers` map.
     *
     * Later, when external tests inspect [entityMap] or [entityBuilderMap], this proxy uses that
     * recorded mapping to intercept read operations ([get], [values], [entries]) and correctly
     * restore the exact external [FakeEntity] instances that were originally provided.
     *
     * @param K The type of the keys maintained by this map (typically [AudioTrack] or
     *   [AudioTrack.Builder]).
     * @property delegate The underlying internal map containing [InternalFakeEntity] values.
     */
    private inner class EntityMapProxy<K>(private val delegate: Map<K, Entity?>) :
        Map<K, Entity?> by delegate {
        override fun get(key: K): Entity? {
            val internalEntity = delegate[key] as? InternalFakeEntity
            return internalEntity?.let { entityWrappers[it] }
        }

        override val values: Collection<Entity?>
            get() =
                delegate.values.map { internal ->
                    (internal as? InternalFakeEntity)?.let { entityWrappers[it] }
                }

        override val entries: Set<Map.Entry<K, Entity?>>
            get() =
                delegate.entries
                    .map { entry ->
                        java.util.AbstractMap.SimpleEntry(
                            entry.key,
                            (entry.value as? InternalFakeEntity)?.let { entityWrappers[it] },
                        ) as Map.Entry<K, Entity?>
                    }
                    .toSet()
    }

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [PointSourceParams] that were set on a specific
     * [AudioTrack] via the [setPointSourceParams] method. It is also used by the fake
     * [getPointSourceParams] to return a value, allowing tests to control its behavior.
     */
    public val pointSourceParamsMap: Map<AudioTrack, PointSourceParams?>
        get() = fakeInternal.pointSourceParamsMap

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [Entity] that were set on a specific [AudioTrack] via
     * the [setPointSourceParams] method.
     */
    public val entityMap: Map<AudioTrack, Entity?>
        get() = EntityMapProxy(fakeInternal.entityMap)

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [PointSourceParams] that were associated with an
     * [AudioTrack.Builder] via the [setPointSourceParams] builder method. This is useful for
     * verifying that the correct parameters were passed during the audio track configuration
     * process.
     */
    public val pointSourceParamsBuilderMap: Map<AudioTrack.Builder, PointSourceParams?>
        get() = fakeInternal.pointSourceParamsBuilderMap

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [Entity] that were associated with an
     * [AudioTrack.Builder] via the [setPointSourceParams] builder method. This is useful for
     * verifying that the correct parameters were passed during the audio track configuration
     * process.
     */
    public val entityBuilderMap: Map<AudioTrack.Builder, Entity?>
        get() = EntityMapProxy(fakeInternal.entityBuilderMap)

    public val soundFieldAttributesMap: MutableMap<AudioTrack, SoundFieldAttributes?>
        get() = fakeInternal.soundFieldAttributesMap

    /**
     * For test purposes only.
     *
     * This map allows tests to inspect the [SoundFieldAttributes] that were associated with an
     * [AudioTrack.Builder] via the [setSoundFieldAttributes] builder method. This is useful for
     * verifying that the correct attributes were passed during the audio track configuration
     * process.
     */
    public val soundFieldAttributesBuilderMap: Map<AudioTrack.Builder, SoundFieldAttributes?>
        get() = fakeInternal.soundFieldAttributesBuilderMap

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
    public var spatialSourceTypeMap: MutableMap<AudioTrack, Int>
        get() = fakeInternal.spatialSourceTypeMap
        set(value) {
            fakeInternal.spatialSourceTypeMap = value
        }

    /**
     * For test purposes only. If non-null, methods in this class can throw this exception to
     * simulate runtime failures.
     */
    public var fakeExtensionException: Throwable? = null

    override fun getPointSourceParams(track: AudioTrack): PointSourceParams? {
        return fakeInternal.getPointSourceParams(track)
    }

    override fun getSoundFieldAttributes(track: AudioTrack): SoundFieldAttributes? {
        return fakeInternal.getSoundFieldAttributes(track)
    }

    @SpatializerConstants.SourceType
    override fun getSpatialSourceType(track: AudioTrack): Int {
        return fakeInternal.getSpatialSourceType(track)
    }

    override fun setPointSourceParams(
        track: AudioTrack,
        params: PointSourceParams,
        entity: Entity?,
    ) {
        fakeExtensionException?.let { throw it }

        val fakeEntity = entity as? FakeEntity
        val internalEntity = fakeEntity?.fakeInternal as? InternalFakeEntity
        if (fakeEntity != null && internalEntity != null) {
            entityWrappers[internalEntity] = fakeEntity
        }

        fakeInternal.setPointSourceParams(track, params, internalEntity)
    }

    override fun setPointSourceParams(
        builder: AudioTrack.Builder,
        params: PointSourceParams,
        entity: Entity?,
    ): AudioTrack.Builder {

        val fakeEntity = entity as? FakeEntity
        val internalEntity = fakeEntity?.fakeInternal as? InternalFakeEntity
        if (fakeEntity != null && internalEntity != null) {
            entityWrappers[internalEntity] = fakeEntity
        }

        fakeInternal.setPointSourceParams(builder, params, internalEntity)
        return builder
    }

    override fun setSoundFieldAttributes(
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes,
    ): AudioTrack.Builder {
        fakeInternal.setSoundFieldAttributes(builder, attributes)
        return builder
    }

    /**
     * For test purposes only. Manually sets the [SoundFieldAttributes] for a given [AudioTrack].
     */
    public fun setSoundFieldAttributes(track: AudioTrack, attributes: SoundFieldAttributes) {
        fakeInternal.soundFieldAttributesMap[track] = attributes
    }
}
