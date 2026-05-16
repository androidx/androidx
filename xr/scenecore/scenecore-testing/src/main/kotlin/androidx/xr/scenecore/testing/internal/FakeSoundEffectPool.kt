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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore.testing.internal

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPool
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/** Test-only implementation of [SoundEffectPool]. */
internal class FakeSoundEffectPool : SoundEffectPool {
    private val loadedResourcesMap = ConcurrentHashMap<SoundEffect, Int>()
    private val loadedAssetsMap = ConcurrentHashMap<SoundEffect, AssetFileDescriptor>()

    /** The set of all resource IDs currently loaded in the pool. */
    val loadedResourceIds: Set<Int>
        get() = loadedResourcesMap.values.toSet()

    /** The set of all [AssetFileDescriptor]s currently loaded in the pool. */
    val loadedAssetDescriptors: Set<AssetFileDescriptor>
        get() = loadedAssetsMap.values.toSet()

    var released: Boolean = false
    var loadCompleteListener: SoundEffectPool.LoadCompleteListener? = null

    override fun setOnLoadCompleteListener(
        executor: Executor,
        listener: SoundEffectPool.LoadCompleteListener,
    ) {
        loadCompleteListener = listener
    }

    override fun clearOnLoadCompleteListener() {
        loadCompleteListener = null
    }

    override fun load(context: Context, resId: Int): SoundEffect {
        val soundEffect = SoundEffect(resId)
        loadedResourcesMap[soundEffect] = resId
        return soundEffect
    }

    override fun load(assetFileDescriptor: AssetFileDescriptor): SoundEffect {
        // Use identity hash code to ensure a unique handle for each asset loaded.
        val soundEffect = SoundEffect(System.identityHashCode(assetFileDescriptor))
        loadedAssetsMap[soundEffect] = assetFileDescriptor
        return soundEffect
    }

    override fun unload(soundEffect: SoundEffect): Boolean {
        // Use ID-based removal because SoundEffect might be a different instance with the same ID.
        val targetId = soundEffect.id
        loadedResourcesMap.entries.removeIf { it.value == targetId || it.key.id == targetId }
        loadedAssetsMap.keys.removeIf { it.id == targetId }
        return true
    }

    override fun release() {
        released = true
        loadedResourcesMap.clear()
        loadedAssetsMap.clear()
    }

    fun notifyLoadComplete(soundEffect: SoundEffect, success: Boolean) {
        loadCompleteListener?.onLoadComplete(soundEffect, success)
    }
}
