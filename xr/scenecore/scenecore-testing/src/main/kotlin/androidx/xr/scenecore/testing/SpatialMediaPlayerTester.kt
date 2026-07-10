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

import android.media.MediaPlayer
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatialMediaPlayer
import androidx.xr.scenecore.testing.internal.FakeMediaPlayerExtensionsWrapper as InternalFakeMediaPlayerExtensionsWrapper

/**
 * A test utility for accessing and inspecting the spatial data associated with the
 * [SpatialMediaPlayer].
 */
public class SpatialMediaPlayerTester
internal constructor(
    private val rtInstance: InternalFakeMediaPlayerExtensionsWrapper,
    private val mediaPlayer: MediaPlayer,
) {

    /**
     * The [PointSourceParams] that are currently set for this [MediaPlayer].
     *
     * This is useful for verifying if the [MediaPlayer] has been updated with the intended
     * parameters via [SpatialMediaPlayer.setPointSourceParams].
     */
    public val pointSourceParams: PointSourceParams?
        get() = rtInstance.paramsWithEntity[mediaPlayer]?.first?.toPointSourceParams()

    /**
     * Checks whether the given [entity] is currently configured as the point source for the
     * associated [MediaPlayer].
     *
     * This returns `true` if the [entity] was passed to [SpatialMediaPlayer.setPointSourceParams]
     * for this [MediaPlayer].
     *
     * @param entity The target [Entity] to check against.
     * @return `true` if the [entity] is the point source for the [mediaPlayer], `false` otherwise.
     */
    public fun isCurrentPointSource(entity: Entity): Boolean {
        @Suppress("DEPRECATION")
        val expectedInternalEntity = (entity.rtEntity as FakeEntity).fakeInternal

        // Retrieve the internal entity that is actually associated with the MediaPlayer.
        val actualInternalEntity = rtInstance.paramsWithEntity[mediaPlayer]?.second

        return expectedInternalEntity == actualInternalEntity
    }

    /**
     * The [SoundFieldAttributes] that are currently set for this [MediaPlayer].
     *
     * This is useful for verifying if the [MediaPlayer] has been updated with the intended
     * attributes via [SpatialMediaPlayer.setSoundFieldAttributes].
     */
    public val soundFieldAttributes: SoundFieldAttributes?
        get() = rtInstance.soundFieldAttributes[mediaPlayer]?.toSoundFieldAttributes()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpatialMediaPlayerTester

        if (rtInstance != other.rtInstance) return false
        if (mediaPlayer != other.mediaPlayer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtInstance.hashCode()
        result = 31 * result + mediaPlayer.hashCode()
        return result
    }
}
