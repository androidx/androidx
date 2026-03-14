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

package androidx.xr.scenecore.runtime

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import java.util.concurrent.Executor

@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public class SoundEffect(@get:RestrictTo(Scope.LIBRARY_GROUP_PREFIX) public val id: Int)

@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public class Stream(@get:RestrictTo(Scope.LIBRARY_GROUP_PREFIX) public val streamId: Int)

@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public interface SoundEffectPool {
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
     * Sets the [listener] to be notified when sounds finish loading. The listener will be called on
     * the provided [executor].
     */
    public fun setOnLoadCompleteListener(executor: Executor, listener: LoadCompleteListener)

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
    public fun load(context: Context, resId: Int): SoundEffect

    /**
     * Loads a sound from an [AssetFileDescriptor] and returns a [SoundEffect] handle.
     *
     * Note: Loading is asynchronous. The sound may not be ready to play immediately after this
     * method returns. Use [LoadCompleteListener] to be notified when it is ready.
     *
     * @param assetFileDescriptor The asset file descriptor for the sound file.
     * @return A [SoundEffect] handle for the loaded sound.
     */
    public fun load(assetFileDescriptor: AssetFileDescriptor): SoundEffect

    /**
     * Unloads a sound from memory to release resources.
     *
     * @param soundEffect The [SoundEffect] to unload.
     * @return True if the sound was successfully unloaded.
     */
    public fun unload(soundEffect: SoundEffect): Boolean

    /** Releases all native resources associated with this pool. */
    public fun release()
}
