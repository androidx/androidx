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

package androidx.xr.scenecore.testing

import android.media.AudioTrack
import android.media.AudioTrack.Builder
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatialAudioTrackBuilder
import androidx.xr.scenecore.testing.internal.FakeAudioTrackExtensionsWrapper as InternalFakeAudioTrackExtensionsWrapper

/**
 * Test utility class for spatial audio extensions.
 *
 * This class provides a mechanism for tests to inspect and verify spatial audio attributes
 * associated with an [AudioTrack.Builder] that are otherwise encapsulated within the SceneCore
 * runtime.
 */
@Suppress("MissingBuildMethod", "TopLevelBuilder")
public class SpatialAudioTrackBuilderTester
internal constructor(private val rtInstance: InternalFakeAudioTrackExtensionsWrapper) {

    /**
     * Retrieves the [PointSourceParams] set on the builder.
     *
     * This reflects values set via [SpatialAudioTrackBuilder.setPointSourceParams].
     *
     * @param builder The [AudioTrack.Builder] from which to get the [PointSourceParams].
     * @return The source params to be set.
     */
    @Suppress("GetterOnBuilder")
    public fun getPointSourceParams(builder: Builder): PointSourceParams? {
        return rtInstance.pointSourceParamsBuilderMap[builder]?.toPointSourceParams()
    }

    /**
     * Retrieves the [SoundFieldAttributes] set on the builder.
     *
     * This reflects values set via [SpatialAudioTrackBuilder.setSoundFieldAttributes].
     *
     * @param builder The [AudioTrack.Builder] from which to get the [SoundFieldAttributes].
     * @return The source attributes to be set.
     */
    @Suppress("GetterOnBuilder")
    public fun getSoundFieldAttributes(builder: Builder): SoundFieldAttributes? {
        return rtInstance.soundFieldAttributesBuilderMap[builder]?.toSoundFieldAttributes()
    }

    /**
     * Checks whether the given [entity] is the current point source for this [AudioTrack.Builder].
     *
     * This returns `true` if the [entity] was passed to
     * [SpatialAudioTrackBuilder.setPointSourceParams] as a point source.
     *
     * @param builder The [AudioTrack.Builder] from [SpatialAudioTrackBuilder.setPointSourceParams].
     * @param entity The target [Entity] to check against.
     * @return `true` if the [entity] is the current point source for this builder, `false`
     *   otherwise.
     */
    @Suppress("GetterOnBuilder")
    public fun isCurrentPointSource(builder: Builder, entity: Entity): Boolean {
        @Suppress("DEPRECATION")
        return rtInstance.entityBuilderMap[builder] == (entity.rtEntity as FakeEntity).fakeInternal
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpatialAudioTrackBuilderTester

        return rtInstance == other.rtInstance
    }

    override fun hashCode(): Int {
        return rtInstance.hashCode()
    }
}
