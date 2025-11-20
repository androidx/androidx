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

@FrameGraphScope
internal class FrameGraphSessionImpl(
    private val cameraGraphSession: CameraGraph.Session,
    private val frameGraphBuffers: FrameGraphBuffers,
) : FrameGraph.Session, CameraGraph.Session by cameraGraphSession {
    /**
     * Closes and invalidates the session, reverting it to the state it was before the session was
     * acquired.
     */
    override fun close() {
        cameraGraphSession.close()
        frameGraphBuffers.invalidate()
    }
}
