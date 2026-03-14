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

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SoundEffect as RtSoundEffect
import java.util.concurrent.Executor

/**
 * Represents a handle to a single sound effect loaded into memory.
 *
 * Can be used only with the [SoundEffectPool] that loaded this SoundEffect or any
 * [SoundEffectPlayer] that was created with that [SoundEffectPool]. The SoundEffect can be released
 * with [SoundEffectPool.unload] when no longer needed.
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public class SoundEffect internal constructor(internal val id: Int) {
    internal fun toRtSoundEffect(): RtSoundEffect {
        return RtSoundEffect(id)
    }
}

@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
internal fun RtSoundEffect.toSoundEffect(): SoundEffect {
    return SoundEffect(this.id)
}

/**
 * Manages the loading and unloading of sound assets into memory.
 *
 * If the value of the [maxStreams] parameter exceeds the capabilities for the platform then the
 * value will be clamped to the platform's max number of supported streams.
 *
 * @param maxStreams The maximum number of simultaneous streams that can be played by this pool.
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public class SoundEffectPool private constructor(sceneRuntime: SceneRuntime, maxStreams: Int) :
    AutoCloseable {

    internal val rtSoundEffectPool = sceneRuntime.createSoundEffectPool(maxStreams)

    /** Callback interface for receiving notification when a sound effect has finished loading. */
    public fun interface LoadCompleteListener {
        /**
         * Called when a sound effect has finished loading.
         *
         * @param soundEffect The handle to the loaded sound effect.
         * @param success True if the load operation was successful, false otherwise.
         */
        public fun onLoadComplete(soundEffect: SoundEffect, success: Boolean)
    }

    /**
     * Loads a sound from an application resource and returns a [SoundEffect] handle.
     *
     * Note: Loading is asynchronous. The sound may not be ready to play immediately after this
     * method returns. Use [LoadCompleteListener] to be notified when it is ready.
     *
     * @param context The application context.
     * @param resId The resource ID of the sound (e.g., R.raw.sound).
     * @return A [SoundEffect] handle for the loaded sound.
     */
    public fun load(context: Context, resId: Int): SoundEffect {
        return rtSoundEffectPool.load(context, resId).toSoundEffect()
    }

    /**
     * Loads a sound from an [AssetFileDescriptor] and returns a [SoundEffect] handle.
     *
     * Note: Loading is asynchronous. The sound may not be ready to play immediately after this
     * method returns. Use [LoadCompleteListener] to be notified when it is ready.
     *
     * @param assetFileDescriptor The asset file descriptor for the sound file.
     * @return A [SoundEffect] handle for the loaded sound.
     */
    public fun load(assetFileDescriptor: AssetFileDescriptor): SoundEffect {
        return rtSoundEffectPool.load(assetFileDescriptor).toSoundEffect()
    }

    /**
     * Unloads a sound from memory to release resources.
     *
     * @param soundEffect The [SoundEffect] to unload.
     * @return True if the sound was successfully unloaded.
     */
    public fun unload(soundEffect: SoundEffect): Boolean {
        return rtSoundEffectPool.unload(soundEffect.toRtSoundEffect())
    }

    /**
     * Sets the [listener] to be notified when sounds finish loading. The listener will be called on
     * the main thread.
     */
    public fun setOnLoadCompleteListener(listener: LoadCompleteListener) {
        setOnLoadCompleteListener(HandlerExecutor.mainThreadExecutor, listener)
    }

    /**
     * Sets the [listener] to be notified when sounds finish loading. The listener will be called on
     * the provided [executor].
     */
    public fun setOnLoadCompleteListener(executor: Executor, listener: LoadCompleteListener) {
        rtSoundEffectPool.setOnLoadCompleteListener(executor) { soundEffect: RtSoundEffect, success
            ->
            listener.onLoadComplete(soundEffect.toSoundEffect(), success)
        }
    }

    /** Releases all native resources associated with this pool. */
    public fun release() {
        return rtSoundEffectPool.release()
    }

    override fun close() {
        release()
    }

    public companion object {

        internal fun create(sceneRuntime: SceneRuntime, maxStreams: Int): SoundEffectPool {
            return SoundEffectPool(sceneRuntime, maxStreams)
        }

        /** Creates a [SoundEffectPool] with the given [session] and [maxStreams]. */
        @JvmStatic
        public fun create(session: Session, maxStreams: Int): SoundEffectPool {
            return create(session.sceneRuntime, maxStreams)
        }
    }
}
