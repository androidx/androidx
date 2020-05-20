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

package androidx.camera.camera2.pipe.impl

import android.content.Context
import androidx.camera.camera2.pipe.CameraGraph
import javax.inject.Inject

@CameraGraphScope
class CameraGraphImpl @Inject constructor(
    private val context: Context,
    private val config: CameraGraph.Config
) : CameraGraph {
    // Only one session can be active at a time.
    private val sessionLock = TokenLockImpl(1)

    override fun start() {
        Log.debug { "Starting Camera Graph!" }
        TODO("Not Implemented")
    }

    override fun stop() {
        TODO("Not Implemented")
    }

    override suspend fun acquireSession(): CameraGraph.Session {
        val token = sessionLock.acquire(1)
        return CameraGraphSessionImpl(token)
    }

    override fun acquireSessionOrNull(): CameraGraph.Session? {
        val token = sessionLock.acquireOrNull(1) ?: return null
        return CameraGraphSessionImpl(token)
    }
}