/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics

import android.hardware.HardwareBuffer
import androidx.annotation.WorkerThread
import androidx.graphics.utils.HandlerThreadExecutor
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper class to handle processing of event queues between the provided [HandlerThreadExecutor]
 * and the [FrameProducer] which may be executing on different threads. This provides helper
 * facilities to guarantee cancellation of requests and proper queueing of pending requests
 * while the [FrameProducer] is in the middle of generating a frame.
 */
internal class RenderQueue(
    private val handlerThread: HandlerThreadExecutor,
    private val frameProducer: FrameProducer,
    private val frameCallback: FrameCallback
) {

    /**
     * Callbacks invoked when new frames are produced or if a frame is generated for a request
     * that has been cancelled.
     */
    interface FrameCallback {
        fun onFrameComplete(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?)

        fun onFrameCancelled(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat?)
    }

    /**
     * Interface to represent a [FrameProducer] this can either be backed by a
     * [android.graphics.HardwareRenderer] or [android.graphics.HardwareBufferRenderer] depending
     * on the API level.
     */
    interface FrameProducer {
        fun renderFrame(
            executor: Executor,
            requestComplete: (HardwareBuffer, SyncFenceCompat?) -> Unit
        )
    }

    /**
     * Request to be executed by the [RenderQueue] this provides callbacks that are invoked
     * when the request is initially queued as well as when to be executed before a frame is
     * generated. This supports batching operations if the [FrameProducer] is busy.
     */
    interface Request {

        /**
         * Callback invoked when the request is enqueued but before a frame is generated
         */
        @WorkerThread
        fun onEnqueued() {}

        /**
         * Callback invoked when the request is about to be processed as part of a the next
         * frame
         */
        @WorkerThread
        fun execute()

        /**
         * Identifier for a request type to determine if the request can be batched
         */
        val id: Int
    }

    /**
     * Flag to determine if all pending requests should be cancelled
     */
    private val mIsCancelling = AtomicBoolean(false)

    /**
     * Queue of pending requests that are executed whenever the [FrameProducer] is idle
     */
    private val mRequests = ArrayDeque<Request>()

    /**
     * Determines if the [FrameProducer] is in the middle of rendering a frame.
     * This is accessed on the underlying HandlerThread only
     */
    private var mRequestPending = false

    /**
     * Callback invoked when the [RenderQueue] is to be released. This will be invoked when
     * there are no more pending requests to process
     */
    private var mReleaseCallback: (() -> Unit)? = null

    /**
     * Flag to determine if we are in the middle of releasing the [RenderQueue]
     */
    private val mIsReleasing = AtomicBoolean(false)

    /**
     * Enqueues a request to be executed by the provided [FrameProducer]
     */
    fun enqueue(request: Request) {
        if (!mIsReleasing.get()) {
            handlerThread.post(this) {
                request.onEnqueued()
                executeRequest(request)
            }
        }
    }

    /**
     * Cancels all pending requests. If a frame is the in the middle of being rendered,
     * [FrameCallback.onFrameCancelled] will be invoked upon completion
     */
    fun cancelPending() {
        if (!mIsReleasing.get()) {
            mIsCancelling.set(true)
            handlerThread.removeCallbacksAndMessages(this)
            handlerThread.post(cancelRequestsRunnable)
        }
    }

    /**
     * Configures a release callback to be invoked. If there are no pending requests, this
     * will get invoked immediately on the [HandlerThreadExecutor]. Otherwise the callback is
     * preserved and invoked after there are no more pending requests.
     * After this method is invoked, no subsequent requests will be processed and this [RenderQueue]
     * instance can no longer be used.
     */
    fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)?) {
        if (!mIsReleasing.get()) {
            if (cancelPending) {
                cancelPending()
            }
            handlerThread.post {
                mReleaseCallback = onReleaseComplete
                val pendingRequest = isPendingRequest()
                if (!pendingRequest) {
                    executeReleaseCallback()
                }
            }
            mIsReleasing.set(true)
        }
    }

    /**
     * Determines if there are any pending requests or a frame is waiting to be produced
     */
    private fun isPendingRequest() = mRequestPending || mRequests.isNotEmpty()

    /**
     * Helper method that will execute a request on the [FrameProducer] if there is not a previous
     * request pending. If there is a pending request, this will add it to an internal queue
     * that will be dequeued when the request is completed.
     */
    @WorkerThread
    private fun executeRequest(request: Request) {
        if (!mRequestPending) {
            mRequestPending = true
            request.execute()
            frameProducer.renderFrame(handlerThread) { hardwareBuffer, syncFenceCompat ->
                mRequestPending = false
                if (!mIsCancelling.getAndSet(false)) {
                    frameCallback.onFrameComplete(hardwareBuffer, syncFenceCompat)
                } else {
                    frameCallback.onFrameCancelled(hardwareBuffer, syncFenceCompat)
                }

                if (mRequests.isNotEmpty()) {
                    // Execute any pending requests that were queued while waiting for the
                    // previous frame to render
                    executeRequest(mRequests.removeFirst())
                } else if (mIsReleasing.get()) {
                    executeReleaseCallback()
                }
            }
        } else {
            // If the last request matches the type that we are adding, then batch the request
            // i.e. don't add it to the queue as the previous request will handle batching.
            val pendingRequest = mRequests.lastOrNull()
            if (pendingRequest == null || pendingRequest.id != request.id) {
                mRequests.add(request)
            }
        }
    }

    /**
     * Returns true if [release] has been invoked
     */
    fun isReleased(): Boolean = mIsReleasing.get()

    /**
     * Invokes the release callback if one is previously configured and discards it
     */
    private fun executeReleaseCallback() {
        mReleaseCallback?.invoke()
        mReleaseCallback = null
    }

    /**
     * Runnable executed when requests are to be cancelled
     */
    private val cancelRequestsRunnable = Runnable {
        mRequests.clear()
        // Only reset the cancel flag if there is no current frame render request pending
        // Otherwise when the frame is completed we will update the flag in the corresponding
        // callback
        if (!mRequestPending) {
            mIsCancelling.set(false)
        }
    }
}
