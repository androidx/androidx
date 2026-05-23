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

package androidx.xr.scenecore

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.Stream as RtStream

/**
 * Represents the handle of a [SoundEffect] that was returned by [SoundEffectPlayer.play]. The
 * Stream can be used to control playback with that [SoundEffectPlayer].
 */
public class Stream
internal constructor(@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val streamId: Int) {
    internal fun toRtStream(): RtStream {
        return RtStream(streamId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Stream

        return streamId == other.streamId
    }

    override fun hashCode(): Int {
        return streamId
    }

    override fun toString(): String {
        return "Stream(streamId=$streamId)"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RtStream.toStream(): Stream {
    return Stream(this.streamId)
}

/** Provides playback control for instances of [SoundEffect]. */
@Suppress("NotCloseable")
public interface SoundEffectPlayer {
    /**
     * Plays a loaded sound effect.
     *
     * The [SoundEffect]s can play concurrently up to the max stream count of the [SoundEffectPool]
     * from which they were loaded. If the max stream count is exceeded, streams with lower priority
     * are stopped first.
     *
     * @param soundEffect the handle to the loaded [SoundEffect] to play
     * @param volume volume in the range [0.0 to 1.0]
     * @param priority playback priority (0 = lowest)
     * @param isLooping true to loop indefinitely, false to play once
     * @return a [Stream] for controlling this specific instance of the sound
     * @throws RuntimeException if the sound effect fails to play
     */
    public fun play(
        soundEffect: SoundEffect,
        @FloatRange(from = 0.0, to = 1.0) volume: Float,
        @IntRange(from = 0) priority: Int,
        isLooping: Boolean,
    ): Stream

    /**
     * Pauses a currently playing sound stream.
     *
     * If the [stream] has already been stopped or released due to hitting the max stream count then
     * this method has no effect.
     *
     * @param stream a [Stream] returned by [play]
     */
    public fun pause(stream: Stream)

    /**
     * Resumes a previously paused sound stream.
     *
     * If the [stream] has already been stopped or released due to hitting the max stream count then
     * this method has no effect.
     *
     * @param stream a [Stream] returned by [play]
     */
    public fun resume(stream: Stream)

    /**
     * Stops a currently playing or paused sound stream and releases that stream ID.
     *
     * If the [stream] has already been stopped or released due to hitting the max stream count then
     * this method has no effect.
     *
     * @param stream a [Stream] returned by [play]
     */
    public fun stop(stream: Stream)

    /**
     * Updates the volume of an active stream.
     *
     * If the [stream] has already been stopped or released due to hitting the max stream count then
     * this method has no effect.
     *
     * @param stream a [Stream] returned by [play]
     * @param volume volume in the range [0.0 to 1.0]
     */
    public fun setVolume(stream: Stream, @FloatRange(from = 0.0, to = 1.0) volume: Float)

    /**
     * Updates the looping status of an active stream.
     *
     * If the [stream] has already been stopped or released due to hitting the max stream count then
     * this method has no effect.
     *
     * @param stream a [Stream] returned by [play]
     * @param isLooping true to loop indefinitely, false to stop looping
     */
    public fun setLooping(stream: Stream, isLooping: Boolean)
}
