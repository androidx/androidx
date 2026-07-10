/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.pipe.internal

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestListeners
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.graph.GraphProcessor
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@CameraGraphScope
public class CameraGraphRequestListenersImpl
@Inject
internal constructor(
    private val sessionLock: GraphSessionLock,
    private val graphProcessor: GraphProcessor,
    @ForCameraGraph private val graphScope: CoroutineScope,
) : RequestListeners {
    private val lock = Any()
    @GuardedBy("lock") private val listeners = mutableSetOf<Request.Listener>()
    @GuardedBy("lock") private var dirty = false

    public override fun add(listener: Request.Listener) {
        addAll(listOf(listener))
    }

    public override fun addAll(listeners: List<Request.Listener>) {
        val invokeUpdate =
            synchronized(lock) {
                val modified = this.listeners.addAll(listeners)
                shouldApplyUpdate(modified)
            }
        if (invokeUpdate) {
            applyUpdate()
        }
    }

    public override fun remove(listener: Request.Listener) {
        removeAll(listOf(listener))
    }

    public override fun removeAll(listeners: List<Request.Listener>) {
        val invokeUpdate =
            synchronized(lock) {
                val modified = this.listeners.removeAll(listeners.toSet())
                shouldApplyUpdate(modified)
            }
        if (invokeUpdate) {
            applyUpdate()
        }
    }

    // We should apply the update only if the listener set is modified, and we are the one setting
    // "dirty" to true. If the "dirty" flag was already true then someone else should "flush" the
    // listeners as part of their update call.
    @GuardedBy("lock")
    private fun shouldApplyUpdate(modified: Boolean): Boolean {
        if (!modified) {
            return false
        }
        if (!dirty) {
            dirty = true
            return true
        }
        return false
    }

    private fun applyUpdate() {
        sessionLock.withTokenIn(graphScope) { flush() }
    }

    // Note: this must be called only when caller has an active sessionLock token.
    public fun flush() {
        val snapshot =
            synchronized(lock) {
                if (!dirty) {
                    return
                }
                dirty = false
                listeners.toList()
            }
        graphProcessor.updateRequestListeners(snapshot)
    }
}
