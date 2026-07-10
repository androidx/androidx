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

import android.content.res.AssetFileDescriptor
import androidx.xr.scenecore.SoundEffect
import androidx.xr.scenecore.SoundEffectPool
import androidx.xr.scenecore.runtime.SoundEffect as RtSoundEffect
import androidx.xr.scenecore.testing.internal.FakeSoundEffectPool as InternalFakeSoundEffectPool

/** A data container for inspecting the properties of a [SoundEffectPool] resource. */
public class SoundEffectPoolTester
internal constructor(
    private val rtSoundEffectPool: InternalFakeSoundEffectPool,
    internal val soundEffectPool: SoundEffectPool,
) {

    internal companion object {
        /**
         * Creates a test data accessor for the given [SoundEffectPool].
         *
         * This function provides a [SoundEffectPoolTester] instance, which can be used to inspect
         * and manipulate its underlying data in the test environment.
         *
         * @param soundEffectPool The [SoundEffectPool] for which to retrieve the test data
         *   accessor.
         * @return A [SoundEffectPoolTester] instance for the given resource.
         */
        internal fun create(soundEffectPool: SoundEffectPool): SoundEffectPoolTester {
            return SoundEffectPoolTester(
                @Suppress("DEPRECATION")
                (soundEffectPool.rtSoundEffectPool as FakeSoundEffectPool).fakeInternal,
                soundEffectPool,
            )
        }
    }

    /**
     * Checks if a sound effect with the given resource ID is currently loaded in the pool.
     *
     * @param resourceId The resource ID to check.
     * @return `true` if the resource is currently loaded, `false` otherwise.
     */
    public fun isResourceLoaded(resourceId: Int): Boolean {
        return !rtSoundEffectPool.released &&
            rtSoundEffectPool.loadedResourceIds.contains(resourceId)
    }

    /**
     * Checks if a sound effect with the given [AssetFileDescriptor] is currently loaded in the
     * pool.
     *
     * @param assetFileDescriptor The file descriptor to check.
     * @return `true` if the descriptor is currently loaded, `false` otherwise.
     */
    public fun isAssetLoaded(assetFileDescriptor: AssetFileDescriptor): Boolean {
        return !rtSoundEffectPool.released &&
            rtSoundEffectPool.loadedAssetDescriptors.contains(assetFileDescriptor)
    }

    /**
     * Simulates a load complete event from the runtime, notifying all registered listeners.
     *
     * This triggers [SoundEffectPool.LoadCompleteListener] registered via
     * [SoundEffectPool.addLoadCompleteListener].
     *
     * @param soundEffect The [SoundEffect] that was loaded.
     * @param success Whether the load was successful.
     */
    public fun triggerLoadCompleteListener(soundEffect: SoundEffect, success: Boolean) {
        rtSoundEffectPool.notifyLoadComplete(RtSoundEffect(soundEffect.id), success)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SoundEffectPoolTester

        if (rtSoundEffectPool != other.rtSoundEffectPool) return false
        if (soundEffectPool != other.soundEffectPool) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtSoundEffectPool.hashCode()
        result = 31 * result + soundEffectPool.hashCode()
        return result
    }
}
