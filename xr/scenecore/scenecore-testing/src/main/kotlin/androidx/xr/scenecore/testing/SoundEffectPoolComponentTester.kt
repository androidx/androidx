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

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundEffect
import androidx.xr.scenecore.SoundEffectPoolComponent
import androidx.xr.scenecore.Stream
import androidx.xr.scenecore.testing.internal.FakeSoundEffectPoolComponent as InternalFakeSoundEffectPoolComponent
import androidx.xr.scenecore.toSoundEffect
import androidx.xr.scenecore.toStream

/**
 * A test-only accessor for [SoundEffectPoolComponent] that enables direct manipulation and
 * inspection of its internal state.
 */
public class SoundEffectPoolComponentTester
internal constructor(
    private val rtSoundEffectPoolComponent: InternalFakeSoundEffectPoolComponent,
    internal val soundEffectPoolComponent: SoundEffectPoolComponent,
) {

    internal companion object {
        /** Retrieves a test data accessor for the given [SoundEffectPoolComponent]. */
        internal fun create(
            soundEffectPoolComponent: SoundEffectPoolComponent
        ): SoundEffectPoolComponentTester {
            return SoundEffectPoolComponentTester(
                @Suppress("DEPRECATION")
                (soundEffectPoolComponent.rtComponent as FakeSoundEffectPoolComponent).fakeInternal,
                soundEffectPoolComponent,
            )
        }
    }

    // --- Played ---

    /**
     * Retrieves the [PointSourceParams] used in the most recent call to
     * [SoundEffectPoolComponent.play], or null if play has not been called yet.
     */
    public val lastPlayedPointSourceParams: PointSourceParams?
        get() = rtSoundEffectPoolComponent.lastPlayedParams?.toPointSourceParams()

    /** The [Stream] of the most recently played sound, or null if no stream has been played yet. */
    public val lastPlayedStream: Stream?
        get() = rtSoundEffectPoolComponent.lastPlayedStream?.toStream()

    /**
     * Retrieves the playback priority of the specified [stream].
     *
     * Priority is represented by a non-negative integer, where 0 is the lowest priority and greater
     * values indicate higher priorities.
     *
     * @param stream a [Stream] returned by [SoundEffectPoolComponent.play]
     * @return the playback priority of the stream
     * @throws IllegalArgumentException if the stream is no longer active or is invalid
     */
    @IntRange(from = 0)
    public fun getPriority(stream: Stream): Int {
        return requireNotNull(rtSoundEffectPoolComponent.priorityMap[stream.streamId]) {
            "Priority for stream ID ${stream.streamId} not found. The stream may be inactive or invalid."
        }
    }

    /**
     * Retrieves the [SoundEffect] associated with the specified [stream].
     *
     * @param stream a [Stream] returned by [SoundEffectPoolComponent.play]
     * @return the [SoundEffect] being played on the stream
     * @throws IllegalArgumentException if the stream is no longer active or is invalid
     */
    public fun getSoundEffect(stream: Stream): SoundEffect {
        return requireNotNull(rtSoundEffectPoolComponent.soundEffectMap[stream.streamId]) {
                "SoundEffect for stream ID ${stream.streamId} not found. The stream may be inactive or invalid."
            }
            .toSoundEffect()
    }

    /** The [Stream] of the most recently paused sound, or null if no stream has been paused yet. */
    public val lastPausedStream: Stream?
        get() = rtSoundEffectPoolComponent.lastPausedStream?.toStream()

    /**
     * The [Stream] of the most recently resumed sound, or null if no stream has been resumed yet.
     */
    public val lastResumedStream: Stream?
        get() = rtSoundEffectPoolComponent.lastResumedStream?.toStream()

    /**
     * The [Stream] of the most recently stopped sound, or null if no stream has been stopped yet.
     */
    public val lastStoppedStream: Stream?
        get() = rtSoundEffectPoolComponent.lastStoppedStream?.toStream()

    /**
     * Returns whether the specified [stream] is configured to loop indefinitely.
     *
     * @param stream a [Stream] returned by [SoundEffectPoolComponent.play]
     * @return true if the stream is looping, false otherwise
     * @throws IllegalArgumentException if the stream is no longer active or is invalid
     */
    public fun isLooping(stream: Stream): Boolean {
        return requireNotNull(rtSoundEffectPoolComponent.isLoopingMap[stream.streamId]) {
            "Looping state for stream ID ${stream.streamId} not found. The stream may be inactive or invalid."
        }
    }

    /**
     * Retrieves the current volume of the specified [stream].
     *
     * @param stream a [Stream] returned by [SoundEffectPoolComponent.play]
     * @return the volume of the stream in the range [0.0 to 1.0]
     * @throws IllegalArgumentException if the stream is no longer active or is invalid
     */
    @FloatRange(from = 0.0, to = 1.0)
    public fun getVolume(stream: Stream): Float {
        return requireNotNull(rtSoundEffectPoolComponent.volumeMap[stream.streamId]) {
            "Volume for stream ID ${stream.streamId} not found. The stream may be inactive or invalid."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SoundEffectPoolComponentTester

        if (rtSoundEffectPoolComponent != other.rtSoundEffectPoolComponent) return false
        if (soundEffectPoolComponent != other.soundEffectPoolComponent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtSoundEffectPoolComponent.hashCode()
        result = 31 * result + soundEffectPoolComponent.hashCode()

        return result
    }
}
