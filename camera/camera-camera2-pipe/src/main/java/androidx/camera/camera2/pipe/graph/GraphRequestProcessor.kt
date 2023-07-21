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

import android.hardware.camera2.CameraAccessException
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.CaptureSequences.invokeOnRequests
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.compat.ObjectUnavailableException
import androidx.camera.camera2.pipe.core.Log
import kotlinx.atomicfu.atomic

internal val graphRequestProcessorIds = atomic(0)

/**
 * The GraphRequestProcessor wraps and tracks the internal state for requests that are submitted to
 * a [CaptureSequenceProcessor] instance.
 *
 * GraphRequestProcessors are intended to be in conjunction with a [GraphListener].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Suppress("NOTHING_TO_INLINE")
class GraphRequestProcessor
private constructor(
    private val captureSequenceProcessor: CaptureSequenceProcessor<Any, CaptureSequence<Any>>
) {
    companion object {
        /** Create a [GraphRequestProcessor] from a [CaptureSequenceProcessor] instance. */
        fun from(captureSequenceProcessor: CaptureSequenceProcessor<*, *>): GraphRequestProcessor {
            @Suppress("UNCHECKED_CAST")
            return GraphRequestProcessor(
                captureSequenceProcessor as CaptureSequenceProcessor<Any, CaptureSequence<Any>>
            )
        }
    }

    private val debugId = graphRequestProcessorIds.incrementAndGet()
    private val closed = atomic(false)

    @GuardedBy("activeCaptureSequences")
    private val activeCaptureSequences = mutableListOf<CaptureSequence<*>>()
    private val activeBurstListener =
        object : CaptureSequence.CaptureSequenceListener {
            override fun onCaptureSequenceComplete(captureSequence: CaptureSequence<*>) {
                // Listen to the completion of active capture sequences and remove them from the
                // list
                // of currently active capture sequences. Since repeating requests are not required
                // to
                // execute, only non-repeating capture sequences are tracked.
                if (!captureSequence.repeating) {
                    synchronized(activeCaptureSequences) {
                        activeCaptureSequences.remove(captureSequence)
                    }
                }
            }
        }

    internal fun abortCaptures() {
        // Note: abortCaptures is not affected by active state.

        // TODO: Consider adding a synchronization lock of some kind to prevent requests from being
        //   submitted while an abort is actively occurring. This could increase the risk of
        //   deadlock. Not locking could increase the risk that additional capture sequences are
        //   submitted in-between reading the list of activeCaptureSequences and calling the
        //   abortCaptures method on the captureSequenceProcessor.

        // Create a copy of the list of non-repeating capture sequences (thread safe), clear the
        // list, then invoke the onAborted listeners for all capture sequences that were in progress
        // at the time abort was invoked.
        val requestsToAbort =
            synchronized(activeCaptureSequences) {
                val copy = activeCaptureSequences.toList()
                activeCaptureSequences.clear()
                copy
            }

        // Invoke onAbort to indicate that the actual abort is about to happen.
        for (sequence in requestsToAbort) {
            sequence.invokeOnAborted()
        }

        // Finally, invoke abortCaptures on the underlying captureSequenceProcessor instance.
        captureSequenceProcessor.abortCaptures()
    }

    internal fun stopRepeating() {
        // Note: stopRepeating is not affected by active state.
        captureSequenceProcessor.stopRepeating()
    }

    internal fun close() {
        Log.debug { "Closing $this" }
        if (closed.compareAndSet(expect = false, update = true)) {
            captureSequenceProcessor.close()
        }
    }

    internal fun submit(
        isRepeating: Boolean,
        requests: List<Request>,
        defaultParameters: Map<*, Any?>,
        requiredParameters: Map<*, Any?>,
        listeners: List<Request.Listener>,
    ): Boolean {
        // Reject incoming requests if this instance has been stopped or closed.
        if (closed.value) {
            Log.warn { "Rejecting requests $requests: Request processor is closed." }
            return false
        }

        // This can fail for various reasons and may throw exceptions.
        val captureSequence =
            captureSequenceProcessor.build(
                isRepeating,
                requests,
                defaultParameters,
                requiredParameters,
                listeners,
                activeBurstListener
            )

        // Reject incoming requests if this instance has been stopped or closed.
        if (captureSequence == null) {
            Log.warn { "Rejecting requests $requests: Could not create the capture sequence." }

            // We do not need to invoke the sequenceCompleteListener since it has not been added to
            // the list of activeCaptureSequences yet.
            return false
        }

        // Re-check again and reject requests if this instance has been closed or stopped.
        // This is an optimization since building the captureSequence can take non-zero time.
        if (closed.value) {
            Log.warn { "Rejecting requests $requests: Request processor is closed." }
            return false
        }

        // Reject incorrectly structured capture sequences:
        check(captureSequence.captureRequestList.size == captureSequence.captureMetadataList.size) {
            "CaptureSequence ($captureSequence) has mismatched request and metadata lists!"
        }

        // Non-repeating requests must always be aware of abort calls.
        if (!captureSequence.repeating) {
            synchronized(activeCaptureSequences) { activeCaptureSequences.add(captureSequence) }
        }

        var captured = false
        return try {
            Log.debug { "Submitting $captureSequence" }
            captureSequence.invokeOnRequestSequenceCreated()

            // NOTE: This is an unusual synchronization call. The purpose is to avoid a rare but
            // possible situation where calling submit causes one of the callback methods to be
            // invoked before this method call returns and sequenceNumber has been set on the
            // callback. Both this call and the synchronized behavior on the captureSequence have
            // been designed to minimize the number of synchronized calls.
            val result =
                synchronized(lock = captureSequence) {
                    // Check closed state right before submitting.
                    if (closed.value) {
                        Log.warn { "Did not submit $captureSequence, $this was closed!" }
                        return false
                    }
                    val sequenceNumber = captureSequenceProcessor.submit(captureSequence) ?: -1
                    captureSequence.sequenceNumber = sequenceNumber
                    sequenceNumber
                }

            if (result != -1) {
                captureSequence.invokeOnRequestSequenceSubmitted()
                captured = true
                Log.debug { "Submitted $captureSequence" }
                true
            } else {
                Log.warn { "Did not submit $captureSequence, SequenceNumber was -1" }
                false
            }
        } catch (closedException: ObjectUnavailableException) {
            false
        } catch (accessException: CameraAccessException) {
            false
        } finally {
            // If ANY unhandled exception occurs, don't throw, but make sure we remove it from the
            // list of in-flight requests.
            if (!captured && !captureSequence.repeating) {
                synchronized(activeCaptureSequences) {
                    activeCaptureSequences.remove(captureSequence)
                }
                captureSequence.invokeOnAborted()
            }
        }
    }

    override fun toString(): String = "GraphRequestProcessor-$debugId"

    /**
     * Custom implementation that informs all listeners that the request had not completed when
     * abort was called.
     */
    private inline fun <T> CaptureSequence<T>.invokeOnAborted() {
        invokeOnRequests { request, _, listener -> listener.onAborted(request.request) }
    }

    private inline fun <T> CaptureSequence<T>.invokeOnRequestSequenceCreated() {
        invokeOnRequests { request, _, listener -> listener.onRequestSequenceCreated(request) }
    }

    private inline fun <T> CaptureSequence<T>.invokeOnRequestSequenceSubmitted() {
        invokeOnRequests { request, _, listener -> listener.onRequestSequenceSubmitted(request) }
    }
}
