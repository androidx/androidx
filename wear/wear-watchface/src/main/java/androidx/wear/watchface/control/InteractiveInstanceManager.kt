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
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.IndentingPrintWriter
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** Keeps track of [InteractiveWatchFaceImpl]s. */
internal class InteractiveInstanceManager {
    private constructor()

    private class RefCountedInteractiveWatchFaceInstance(
        val impl: InteractiveWatchFaceImpl,
        var refcount: Int
    ) {
        @UiThread
        fun dump(writer: IndentingPrintWriter) {
            writer.println("InteractiveInstanceManager:")
            writer.increaseIndent()
            writer.println("impl.instanceId=${impl.instanceId}")
            writer.println("refcount=$refcount")
            impl.engine.dump(writer)
            writer.decreaseIndent()
        }
    }

    class PendingWallpaperInteractiveWatchFaceInstance(
        val params: WallpaperInteractiveWatchFaceInstanceParams,
        val callback: IPendingInteractiveWatchFace
    )

    companion object {
        private val instances = HashMap<String, RefCountedInteractiveWatchFaceInstance>()
        private val pendingWallpaperInteractiveWatchFaceInstanceLock = Any()
        private var pendingWallpaperInteractiveWatchFaceInstance:
            PendingWallpaperInteractiveWatchFaceInstance? = null

        @SuppressLint("SyntheticAccessor")
        fun addInstance(impl: InteractiveWatchFaceImpl) {
            synchronized(pendingWallpaperInteractiveWatchFaceInstanceLock) {
                require(!instances.containsKey(impl.instanceId)) {
                    "Already have an InteractiveWatchFaceImpl with id ${impl.instanceId}"
                }
                instances[impl.instanceId] = RefCountedInteractiveWatchFaceInstance(impl, 1)
            }
        }

        @SuppressLint("SyntheticAccessor")
        fun getAndRetainInstance(instanceId: String): InteractiveWatchFaceImpl? {
            synchronized(pendingWallpaperInteractiveWatchFaceInstanceLock) {
                val refCountedInstance = instances[instanceId] ?: return null
                refCountedInstance.refcount++
                return refCountedInstance.impl
            }
        }

        @SuppressLint("SyntheticAccessor")
        fun releaseInstance(instanceId: String) {
            synchronized(pendingWallpaperInteractiveWatchFaceInstanceLock) {
                instances[instanceId]?.let {
                    if (--it.refcount == 0) {
                        instances.remove(instanceId)
                    }
                }
            }
        }

        @SuppressLint("SyntheticAccessor")
        fun deleteInstance(instanceId: String) {
            synchronized(pendingWallpaperInteractiveWatchFaceInstanceLock) {
                instances.remove(instanceId)
            }
        }

        @SuppressLint("SyntheticAccessor")
        fun renameInstance(oldInstanceId: String, newInstanceId: String) {
            synchronized(pendingWallpaperInteractiveWatchFaceInstanceLock) {
                val instance = instances.remove(oldInstanceId)
                require(instance != null) {
                    "Expected an InteractiveWatchFaceImpl with id $oldInstanceId"
                }
                require(!instances.containsKey(newInstanceId)) {
                    "Already have an InteractiveWatchFaceImpl with id $newInstanceId"
                }
                instances.put(newInstanceId, instance)
            }
        }

        /** Can be called on any thread. */
        @SuppressLint("SyntheticAccessor")
        @RequiresApi(27)
        fun getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
            value: PendingWallpaperInteractiveWatchFaceInstance
        ): IInteractiveWatchFace? {
            synchronized(pendingWallpaperInteractiveWatchFaceInstanceLock) {
                val instance = instances[value.params.instanceId]
                return if (instance != null) {
                    // The system on reboot will use this to connect to an existing watch face, we
                    // need to ensure there isn't a skew between the style the watch face actually
                    // has and what the system thinks we should have. Note runBlocking is safe here
                    // because we never await.
                    val engine = instance.impl.engine
                    runBlocking {
                        withContext(engine.uiThreadCoroutineScope.coroutineContext) {
                            if (engine.deferredWatchFaceImpl.isCompleted) {
                                // setUserStyle awaits deferredWatchFaceImpl but it's completed.
                                engine.setUserStyle(value.params.userStyle)
                            } else {
                                // Defer the UI update until deferredWatchFaceImpl is about to
                                // complete.
                                engine.pendingUserStyle = value.params.userStyle
                            }
                        }
                    }
                    instance.impl
                } else {
                    TraceEvent("Set pendingWallpaperInteractiveWatchFaceInstance").use {
                        pendingWallpaperInteractiveWatchFaceInstance = value
                    }
                    null
                }
            }
        }

        /** Can be called on any thread. */
        @SuppressLint("SyntheticAccessor")
        fun takePendingWallpaperInteractiveWatchFaceInstance():
            PendingWallpaperInteractiveWatchFaceInstance? {
                synchronized(pendingWallpaperInteractiveWatchFaceInstanceLock) {
                    val returnValue = pendingWallpaperInteractiveWatchFaceInstance
                    pendingWallpaperInteractiveWatchFaceInstance = null
                    return returnValue
                }
            }

        @UiThread
        fun dump(writer: IndentingPrintWriter) {
            writer.println("InteractiveInstanceManager instances:")
            writer.increaseIndent()
            pendingWallpaperInteractiveWatchFaceInstance?.let {
                writer.println(
                    "Pending WallpaperInteractiveWatchFaceInstance id ${it.params.instanceId}"
                )
            }
            synchronized(pendingWallpaperInteractiveWatchFaceInstanceLock) {
                for ((_, value) in instances) {
                    value.dump(writer)
                }
            }
            writer.decreaseIndent()
        }
    }
}
