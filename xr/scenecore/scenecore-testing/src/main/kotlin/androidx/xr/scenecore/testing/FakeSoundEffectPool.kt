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

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPool
import java.util.concurrent.Executor

/** Test-only implementation of [SoundEffectPool]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSoundEffectPool : SoundEffectPool {
    public var loadedResId: Int? = null
    public var loadedAfd: AssetFileDescriptor? = null
    public var unloadedSoundEffect: SoundEffect? = null
    public var released: Boolean = false
    public var loadCompleteListener: SoundEffectPool.LoadCompleteListener? = null

    override fun setOnLoadCompleteListener(
        executor: Executor,
        listener: SoundEffectPool.LoadCompleteListener,
    ) {
        loadCompleteListener = listener
    }

    override fun load(context: Context, resId: Int): SoundEffect {
        loadedResId = resId
        return SoundEffect(resId)
    }

    override fun load(assetFileDescriptor: AssetFileDescriptor): SoundEffect {
        loadedAfd = assetFileDescriptor
        return SoundEffect(0)
    }

    override fun unload(soundEffect: SoundEffect): Boolean {
        unloadedSoundEffect = soundEffect
        return true
    }

    override fun release() {
        released = true
    }

    public fun notifyLoadComplete(soundEffect: SoundEffect, success: Boolean) {
        loadCompleteListener?.onLoadComplete(soundEffect, success)
    }
}
