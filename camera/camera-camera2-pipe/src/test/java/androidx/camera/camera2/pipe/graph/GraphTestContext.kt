/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor

internal class GraphTestContext : AutoCloseable {
    val streamId = StreamId(0)
    val surfaceMap = mapOf(streamId to Surface(SurfaceTexture(1)))
    val captureSequenceProcessor = FakeCaptureSequenceProcessor()
    val graphRequestProcessor = GraphRequestProcessor.from(captureSequenceProcessor)
    val graphProcessor = FakeGraphProcessor()

    init {
        captureSequenceProcessor.surfaceMap = surfaceMap
        graphProcessor.onGraphStarted(graphRequestProcessor)
        graphProcessor.repeatingRequest = Request(streams = listOf(streamId))
    }

    override fun close() {
        surfaceMap[streamId]?.release()
    }
}
