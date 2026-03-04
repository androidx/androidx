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

package androidx.camera.camera2.pipe.framegraph

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.config.FrameGraphScope
import androidx.camera.camera2.pipe.graph.Controller3A
import kotlinx.atomicfu.atomic

@FrameGraphScope
internal class FrameGraphSessionImpl(
    private val cameraGraphSession: CameraGraph.Session,
    private val frameGraphBuffers: FrameGraphBuffers,
    private val controller3A: Controller3A,
) : FrameGraph.Session, CameraGraph.Session by cameraGraphSession {
    private val state3ASnapshot = controller3A.state3ASnapshot()
    private val closed = atomic(false)

    /**
     * Closes and invalidates the session, reverting it to the state it was before the session was
     * acquired.
     */
    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            controller3A.reset3A(state3ASnapshot)
            frameGraphBuffers.flush(cameraGraphSession)
            cameraGraphSession.close()
        }
    }
}
