/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.control

import android.annotation.SuppressLint
import androidx.annotation.UiThread

/** Keeps track of [InteractiveWatchFaceImpl]s. */
internal class InteractiveInstanceManager {
    private constructor()

    private class RefCountedInteractiveWatchFaceInstance(
        val impl: InteractiveWatchFaceImpl,
        var refcount: Int
    )

    companion object {
        private val instances = HashMap<String, RefCountedInteractiveWatchFaceInstance>()

        @SuppressLint("SyntheticAccessor")
        @UiThread
        fun addInstance(impl: InteractiveWatchFaceImpl) {
            require(!instances.containsKey(impl.instanceId))
            instances[impl.instanceId] = RefCountedInteractiveWatchFaceInstance(impl, 1)
        }

        @SuppressLint("SyntheticAccessor")
        @UiThread
        fun getAndRetainInstance(instanceId: String): InteractiveWatchFaceImpl ? {
            val refCountedInstance = instances[instanceId] ?: return null
            refCountedInstance.refcount++
            return refCountedInstance.impl
        }

        @SuppressLint("SyntheticAccessor")
        @UiThread
        fun releaseInstance(instanceId: String) {
            val instance = instances[instanceId]!!
            if (--instance.refcount == 0) {
                instance.impl.engine.onDestroy()
                instances.remove(instanceId)
            }
        }
    }
}