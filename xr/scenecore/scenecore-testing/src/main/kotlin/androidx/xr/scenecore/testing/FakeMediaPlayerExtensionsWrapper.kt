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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import android.media.MediaPlayer
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.testing.internal.FakeEntity as InternalFakeEntity
import androidx.xr.scenecore.testing.internal.FakeMediaPlayerExtensionsWrapper as InternalFakeMediaPlayerExtensionsWrapper
import java.util.Collections
import java.util.WeakHashMap

/** Test-only implementation of [androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeMediaPlayerExtensionsWrapper
internal constructor(internal var fakeInternal: InternalFakeMediaPlayerExtensionsWrapper) :
    MediaPlayerExtensionsWrapper {
    public constructor() : this(InternalFakeMediaPlayerExtensionsWrapper())

    /** Mapping between [InternalFakeEntity] and [FakeEntity] */
    internal val entityMap = Collections.synchronizedMap(WeakHashMap<Entity, Entity>())

    /**
     * For test purposes only.
     *
     * This read-only map stores the [androidx.xr.scenecore.runtime.PointSourceParams] that were
     * last set for each [MediaPlayer] instance via the [setPointSourceParams] method.
     *
     * Tests can inspect this map to verify that the code under test correctly applies the intended
     * `PointSourceParams` to the `MediaPlayer`.
     */
    public val paramsWithEntity: Map<MediaPlayer, Pair<PointSourceParams, Entity>>
        get() =
            fakeInternal.paramsWithEntity.mapValues { (_, pair) ->
                val (params, rtEntity) = pair
                params to requireNotNull(entityMap[rtEntity])
            }

    /**
     * Sets the PointSourceParams of the MediaPlayer.
     *
     * @param mediaPlayer The MediaPlayer to set the PointSourceParams on.
     * @param params The PointSourceParams to set.
     */
    override fun setPointSourceParams(
        mediaPlayer: MediaPlayer,
        params: PointSourceParams,
        entity: Entity,
    ) {
        val internalFakeEntity = (entity as FakeEntity).fakeInternal as InternalFakeEntity
        entityMap[internalFakeEntity] = entity
        fakeInternal.setPointSourceParams(mediaPlayer, params, internalFakeEntity)
    }

    /**
     * For test purposes only.
     *
     * This read-only map stores the [androidx.xr.scenecore.runtime.SoundFieldAttributes] that were
     * last set for each [MediaPlayer] instance via the [setSoundFieldAttributes] method.
     *
     * Tests can inspect this map to verify that the code under test correctly applies the intended
     * `SoundFieldAttributes` to the `MediaPlayer`.
     */
    public val soundFieldAttributes: Map<MediaPlayer, SoundFieldAttributes>
        get() = fakeInternal.soundFieldAttributes

    /**
     * Sets the SoundFieldAttributes of the MediaPlayer.
     *
     * @param mediaPlayer The MediaPlayer to set the SoundFieldAttributes on.
     * @param attributes The SoundFieldAttributes to set.
     */
    override fun setSoundFieldAttributes(
        mediaPlayer: MediaPlayer,
        attributes: SoundFieldAttributes,
    ) {
        fakeInternal.setSoundFieldAttributes(mediaPlayer, attributes)
    }
}
