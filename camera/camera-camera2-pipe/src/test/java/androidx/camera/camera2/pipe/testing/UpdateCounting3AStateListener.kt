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

package androidx.camera.camera2.pipe.testing

import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.graph.Result3AStateListener

/**
 * Wrapper on Result3AStateListenerImpl to keep track of the number of times the update method is
 * called.
 */
internal class UpdateCounting3AStateListener(private val listener: Result3AStateListener) :
    Result3AStateListener {
    var updateCount = 0

    override fun onRequestSequenceCreated(requestNumber: RequestNumber) {
        listener.onRequestSequenceCreated(requestNumber)
    }

    override fun update(requestNumber: RequestNumber, frameMetadata: FrameMetadata): Boolean {
        updateCount++
        return listener.update(requestNumber, frameMetadata)
    }

    override fun onStopRepeating() {}

    override fun onGraphStopped() {}

    override fun onGraphShutdown() {}
}
