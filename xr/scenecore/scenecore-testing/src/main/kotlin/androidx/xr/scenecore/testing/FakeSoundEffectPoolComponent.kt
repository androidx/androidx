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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPoolComponent
import androidx.xr.scenecore.runtime.Stream
import androidx.xr.scenecore.testing.internal.FakeEntity as InternalFakeEntity
import androidx.xr.scenecore.testing.internal.FakeSoundEffectPoolComponent as InternalFakeSoundEffectPoolComponent
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

/** Test-only implementation of [SoundEffectPoolComponent]. */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeSoundEffectPoolComponent
internal constructor(internal val fakeInternal: InternalFakeSoundEffectPoolComponent) :
    FakeComponent(), SoundEffectPoolComponent {

    public constructor() : this(InternalFakeSoundEffectPoolComponent())

    /** Mapping between [InternalFakeEntity] and [FakeEntity] */
    internal val entityMap =
        Collections.synchronizedMap(WeakHashMap<Entity, WeakReference<Entity>>())

    public var lastPlayedEntity: Entity?
        get() = fakeInternal.lastPlayedEntity?.let { entityMap[it]?.get() }
        set(value) {
            val fakeEntity = value as? FakeEntity
            val internalEntity = fakeEntity?.fakeInternal as? InternalFakeEntity

            fakeInternal.lastPlayedEntity = internalEntity
            if (internalEntity != null) {
                entityMap[internalEntity] = WeakReference(fakeEntity)
            }
        }

    public var lastPlayedSoundEffect: SoundEffect?
        get() = fakeInternal.lastPlayedSoundEffect
        set(value) {
            fakeInternal.lastPlayedSoundEffect = value
        }

    public var lastPlayedParams: PointSourceParams?
        get() = fakeInternal.lastPlayedParams
        set(value) {
            fakeInternal.lastPlayedParams = value
        }

    public var lastPlayedVolume: Float?
        get() = fakeInternal.lastPlayedVolume
        set(value) {
            fakeInternal.lastPlayedVolume = value
        }

    public var lastPlayedPriority: Int?
        get() = fakeInternal.lastPlayedPriority
        set(value) {
            fakeInternal.lastPlayedPriority = value
        }

    public var lastPlayedIsLooping: Boolean?
        get() = fakeInternal.lastPlayedIsLooping
        set(value) {
            fakeInternal.lastPlayedIsLooping = value
        }

    public var lastPausedStream: Stream?
        get() = fakeInternal.lastPausedStream
        set(value) {
            fakeInternal.lastPausedStream = value
        }

    public var lastResumedStream: Stream?
        get() = fakeInternal.lastResumedStream
        set(value) {
            fakeInternal.lastResumedStream = value
        }

    public var lastStoppedStream: Stream?
        get() = fakeInternal.lastStoppedStream
        set(value) {
            fakeInternal.lastStoppedStream = value
        }

    public var lastSetVolumeStream: Stream?
        get() = fakeInternal.lastSetVolumeStream
        set(value) {
            fakeInternal.lastSetVolumeStream = value
        }

    public var lastSetVolumeVolume: Float?
        get() = fakeInternal.lastSetVolumeVolume
        set(value) {
            fakeInternal.lastSetVolumeVolume = value
        }

    public var lastSetLoopingStream: Stream?
        get() = fakeInternal.lastSetLoopingStream
        set(value) {
            fakeInternal.lastSetLoopingStream = value
        }

    public var lastSetLoopingIsLooping: Boolean?
        get() = fakeInternal.lastSetLoopingIsLooping
        set(value) {
            fakeInternal.lastSetLoopingIsLooping = value
        }

    override fun play(
        soundEffect: SoundEffect,
        pointSourceParams: PointSourceParams,
        entity: Entity?,
        volume: Float,
        priority: Int,
        isLooping: Boolean,
    ): Stream {
        val internalEntity = (entity as? FakeEntity)?.fakeInternal as? InternalFakeEntity
        lastPlayedEntity = entity
        return fakeInternal.play(
            soundEffect,
            pointSourceParams,
            internalEntity,
            volume,
            priority,
            isLooping,
        )
    }

    override fun pause(stream: Stream) {
        fakeInternal.pause(stream)
    }

    override fun resume(stream: Stream) {
        fakeInternal.resume(stream)
    }

    override fun stop(stream: Stream) {
        fakeInternal.stop(stream)
    }

    override fun setVolume(stream: Stream, volume: Float) {
        fakeInternal.setVolume(stream, volume)
    }

    override fun setLooping(stream: Stream, isLooping: Boolean) {
        fakeInternal.setLooping(stream, isLooping)
    }
}
