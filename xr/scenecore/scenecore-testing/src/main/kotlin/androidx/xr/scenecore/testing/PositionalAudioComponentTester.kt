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

import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.PositionalAudioComponent
import androidx.xr.scenecore.testing.internal.FakePositionalAudioComponent as InternalFakePositionalAudioComponent

/**
 * A test-only accessor for [PositionalAudioComponent] that enables direct manipulation and
 * inspection of its internal state.
 */
public class PositionalAudioComponentTester
internal constructor(
    private val rtPositionalAudioComponent: InternalFakePositionalAudioComponent,
    internal val positionalAudioComponent: PositionalAudioComponent,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [PositionalAudioComponent].
         *
         * This function provides a [PositionalAudioComponentTester] instance, which can be used to
         * inspect and manipulate its underlying data in the test environment.
         *
         * @param positionalAudioComponent The component for which to retrieve the test data
         *   accessor.
         * @return A [PositionalAudioComponentTester] instance for the given component.
         */
        internal fun create(
            positionalAudioComponent: PositionalAudioComponent
        ): PositionalAudioComponentTester {
            return PositionalAudioComponentTester(
                @Suppress("DEPRECATION")
                (positionalAudioComponent.rtComponent as FakePositionalAudioComponent).fakeInternal,
                positionalAudioComponent,
            )
        }
    }

    /**
     * The [PointSourceParams] that are currently set for this spatial audio source.
     *
     * This is useful for verifying if the component has been updated with the intended parameters
     * via [PositionalAudioComponent.pointSourceParams].
     */
    public val pointSourceParams: PointSourceParams
        get() = rtPositionalAudioComponent.params.toPointSourceParams()

    /**
     * The [AudioOutputProvider] used by the [PositionalAudioComponent.audioOutputProvider].
     *
     * Setting this property simulates the configuration of an [AudioOutputProvider], e.g. a
     * [androidx.media3.exoplayer.audio.ForwardingAudioOutputProvider], that can be used to
     * configure an [androidx.media3.exoplayer.ExoPlayer.Builder] for positional audio playback.
     */
    public var audioOutputProvider: AudioOutputProvider
        get() = rtPositionalAudioComponent.getAudioOutputProvider()
        set(value) {
            rtPositionalAudioComponent.setAudioOutputProvider(value)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PositionalAudioComponentTester

        if (rtPositionalAudioComponent != other.rtPositionalAudioComponent) return false
        if (positionalAudioComponent != other.positionalAudioComponent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtPositionalAudioComponent.hashCode()
        result = 31 * result + positionalAudioComponent.hashCode()

        return result
    }
}
