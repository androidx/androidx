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

import android.hardware.camera2.CaptureRequest
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameBuffer
import androidx.camera.camera2.pipe.FrameReference
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.FrameGraphCoroutineScope
import androidx.camera.camera2.pipe.config.FrameGraphScope
import androidx.camera.camera2.pipe.filterToCaptureRequestParameters
import androidx.camera.camera2.pipe.filterToMetadataParameters
import androidx.camera.camera2.pipe.internal.FrameDistributor
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@FrameGraphScope
internal class FrameGraphBuffers
@Inject
internal constructor(
    private val cameraGraph: CameraGraph,
    @FrameGraphCoroutineScope private val frameGraphCoroutineScope: CoroutineScope,
) : FrameDistributor.FrameStartedListener {
    private val lock = Any()
    @GuardedBy("lock") private val buffers = mutableListOf<FrameBufferImpl>()
    @GuardedBy("lock") private var streams = mutableSetOf<StreamId>()
    @GuardedBy("lock") private var parameters = mutableMapOf<Any, Any>()

    internal fun attach(
        streams: Set<StreamId>,
        parameters: Map<Any, Any?>,
        capacity: Int,
    ): FrameBuffer {
        val frameBuffer = FrameBufferImpl(this, streams, parameters, capacity)
        val modified =
            synchronized(lock) {
                buffers.add(frameBuffer)
                updateStreamsAndParameters()
            }
        if (modified) {
            invalidate()
        }
        return frameBuffer
    }

    internal fun detach(frameBuffer: FrameBufferImpl) {
        val modified =
            synchronized(lock) {
                buffers.remove(frameBuffer)
                updateStreamsAndParameters()
            }
        if (modified) {
            invalidate()
        }
    }

    @GuardedBy("lock")
    private fun updateStreamsAndParameters(): Boolean {
        val newStreams = mutableSetOf<StreamId>()
        val newParameters = mutableMapOf<Any, Any>()
        for (buffer in buffers) {
            newStreams.addAll(buffer.streams)

            for (parameter in buffer.parameters) {
                val key = parameter.key
                val value = parameter.value
                check(key is CaptureRequest.Key<*> || key is Metadata.Key<*>) {
                    "Invalid type for ${parameter.key}"
                }

                // If the key is present the values shouldn't conflict.
                check(!newParameters.containsKey(key) || newParameters[key] == value) {
                    "Conflicting parameter values: $key has different values (${newParameters[key]} and $value)."
                }

                if (value != null) {
                    newParameters[key] = value
                }
            }
        }
        val modified: Boolean = newStreams != streams || newParameters != parameters
        streams = newStreams
        parameters = newParameters
        return modified
    }

    fun flush(session: CameraGraph.Session) {
        synchronized(lock) {
            if (buffers.isEmpty()) {
                session.stopRepeating()
                return
            }
            session.startRepeating(
                Request(
                    streams = streams.toList(),
                    parameters = parameters.filterToCaptureRequestParameters(),
                    extras = parameters.filterToMetadataParameters(),
                )
            )
        }
    }

    fun invalidate() {
        cameraGraph.useSessionIn(frameGraphCoroutineScope) { session -> flush(session) }
    }

    override fun onFrameStarted(frameReference: FrameReference) {
        synchronized(lock) {
            for (buffer in buffers) {
                buffer.onFrameStarted(frameReference)
            }
        }
    }
}
