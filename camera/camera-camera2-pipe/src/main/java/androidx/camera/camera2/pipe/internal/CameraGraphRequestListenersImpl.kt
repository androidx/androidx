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
        var invokeUpdate = false
        synchronized(lock) {
            val modified = this.listeners.addAll(listeners)
            if (modified && !dirty) {
                dirty = true
                invokeUpdate = true
            }
        }
        if (invokeUpdate) {
            applyUpdate()
        }
    }

    public override fun remove(listener: Request.Listener) {
        removeAll(listOf(listener))
    }

    public override fun removeAll(listeners: List<Request.Listener>) {
        var invokeUpdate = false
        synchronized(lock) {
            val modified = this.listeners.removeAll(listeners)
            if (modified && !dirty) {
                dirty = true
                invokeUpdate = true
            }
        }
        if (invokeUpdate) {
            applyUpdate()
        }
    }

    internal fun fetchUpdatedListeners(): List<Request.Listener>? {
        synchronized(lock) {
            if (!dirty) return null

            dirty = false
            return listeners.toList()
        }
    }

    private fun applyUpdate() {
        val unappliedListeners = fetchUpdatedListeners() ?: return
        sessionLock.withTokenIn(graphScope) {
            graphProcessor.updateRequestListeners(unappliedListeners)
        }
    }
}
