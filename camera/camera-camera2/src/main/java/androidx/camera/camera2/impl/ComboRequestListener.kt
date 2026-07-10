/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.impl

import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * A ComboRequestListener which contains a set of [Request.Listener]s. The primary purpose of this
 * class is to receive the capture result from the currently configured [UseCaseCamera] and
 * propagate to the registered [Request.Listener]s.
 */
@CameraScope
public class ComboRequestListener @Inject constructor() : Request.Listener {
    private val lock = Any()

    @Volatile private var _listenerHolders = emptyArray<ListenerHolder>()

    @get:VisibleForTesting
    internal val listenerHolders: Array<ListenerHolder>
        get() = _listenerHolders

    @VisibleForTesting
    internal class ListenerHolder(
        @JvmField val listener: Request.Listener,
        @JvmField val executor: Executor,
        @JvmField val isDirect: Boolean,
    )

    public fun addListener(listener: Request.Listener, executor: Executor) {
        synchronized(lock) {
            if (_listenerHolders.any { it.listener === listener }) return

            val isDirect = (executor == CameraXExecutors.directExecutor())
            val newHolder = ListenerHolder(listener, executor, isDirect)
            _listenerHolders += newHolder
        }
    }

    public fun removeListener(listener: Request.Listener) {
        synchronized(lock) {
            _listenerHolders = _listenerHolders.filter { it.listener !== listener }.toTypedArray()
        }
    }

    private inline fun dispatch(crossinline action: (Request.Listener) -> Unit) {
        val holders = _listenerHolders
        val size = holders.size
        if (size == 0) return

        // Manual for-loop is the fastest way to iterate on API 23+
        // as it avoids any Iterator or Stream overhead.
        for (i in 0 until size) {
            val holder = holders[i]
            if (holder.isDirect) {
                action(holder.listener)
            } else {
                holder.executor.execute { action(holder.listener) }
            }
        }
    }

    override fun onAborted(request: Request): Unit = dispatch { it.onAborted(request) }

    override fun onBufferLost(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        streamId: StreamId,
        outputId: OutputId,
    ): Unit = dispatch { it.onBufferLost(requestMetadata, frameNumber, streamId, outputId) }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo,
    ): Unit = dispatch { it.onComplete(requestMetadata, frameNumber, result) }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        requestFailure: RequestFailure,
    ): Unit = dispatch { it.onFailed(requestMetadata, frameNumber, requestFailure) }

    override fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata,
    ): Unit = dispatch { it.onPartialCaptureResult(requestMetadata, frameNumber, captureResult) }

    override fun onRequestSequenceAborted(requestMetadata: RequestMetadata): Unit = dispatch {
        it.onRequestSequenceAborted(requestMetadata)
    }

    override fun onRequestSequenceCompleted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
    ): Unit = dispatch { it.onRequestSequenceCompleted(requestMetadata, frameNumber) }

    override fun onRequestSequenceCreated(requestMetadata: RequestMetadata): Unit = dispatch {
        it.onRequestSequenceCreated(requestMetadata)
    }

    override fun onRequestSequenceSubmitted(requestMetadata: RequestMetadata): Unit = dispatch {
        it.onRequestSequenceSubmitted(requestMetadata)
    }

    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp,
    ): Unit = dispatch { it.onStarted(requestMetadata, frameNumber, timestamp) }

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo,
    ): Unit = dispatch { it.onTotalCaptureResult(requestMetadata, frameNumber, totalCaptureResult) }
}

public fun RequestMetadata.containsTag(tagKey: String, tagValue: Any): Boolean =
    getOrDefault(CAMERAX_TAG_BUNDLE, TagBundle.emptyBundle()).getTag(tagKey) == tagValue
