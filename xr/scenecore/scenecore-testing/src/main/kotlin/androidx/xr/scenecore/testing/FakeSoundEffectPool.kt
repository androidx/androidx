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

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPool
import androidx.xr.scenecore.testing.internal.FakeSoundEffectPool as InternalFakeSoundEffectPool
import java.util.concurrent.Executor

/** Test-only implementation of [SoundEffectPool]. */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeSoundEffectPool
internal constructor(internal var fakeInternal: InternalFakeSoundEffectPool) : SoundEffectPool {

    public constructor() : this(InternalFakeSoundEffectPool())

    /** The set of all resource IDs currently loaded in the pool. */
    public val loadedResourceIds: Set<Int>
        get() = fakeInternal.loadedResourceIds

    /** The set of all [AssetFileDescriptor]s currently loaded in the pool. */
    public val loadedAssetDescriptors: Set<AssetFileDescriptor>
        get() = fakeInternal.loadedAssetDescriptors

    public var released: Boolean
        get() = fakeInternal.released
        set(value) {
            fakeInternal.released = value
        }

    public var loadCompleteListener: SoundEffectPool.LoadCompleteListener?
        get() = fakeInternal.loadCompleteListener
        set(value) {
            fakeInternal.loadCompleteListener = value
        }

    override fun setOnLoadCompleteListener(
        executor: Executor,
        listener: SoundEffectPool.LoadCompleteListener,
    ) {
        fakeInternal.setOnLoadCompleteListener(executor, listener)
    }

    override fun clearOnLoadCompleteListener() {
        fakeInternal.clearOnLoadCompleteListener()
    }

    override fun load(context: Context, resId: Int): SoundEffect {
        return fakeInternal.load(context, resId)
    }

    override fun load(assetFileDescriptor: AssetFileDescriptor): SoundEffect {
        return fakeInternal.load(assetFileDescriptor)
    }

    override fun unload(soundEffect: SoundEffect): Boolean {
        return fakeInternal.unload(soundEffect)
    }

    override fun release() {
        fakeInternal.release()
    }

    public fun notifyLoadComplete(soundEffect: SoundEffect, success: Boolean) {
        fakeInternal.notifyLoadComplete(soundEffect, success)
    }
}
