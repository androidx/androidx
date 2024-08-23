/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.ProcessingQueue
import androidx.camera.camera2.pipe.core.ProcessingQueue.Companion.processIn
import androidx.camera.camera2.pipe.putAllMetadata
import java.io.Closeable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * GraphLoop is a thread-safe class that handles incoming state changes and requests and executes
 * them, in order, on a dispatcher. In addition, this implementation handles several optimizations
 * that enable requests to be deterministically skipped or aborted, and is responsible for the
 * cleanup of pending requests during shutdown.
 */
internal class GraphLoop(
    private val cameraGraphId: CameraGraphId,
    private val defaultParameters: Map<*, Any?>,
    private val requiredParameters: Map<*, Any?>,
    private val graphListeners: List<Request.Listener>,
    private val graphState3A: GraphState3A?,
    private val listeners: List<GraphLoop.Listener>,
    private val shutdownScope: CoroutineScope,
    dispatcher: CoroutineDispatcher
) : Closeable {
    internal interface Listener {
        fun onStopRepeating()

        fun onGraphStopped()

        fun onGraphShutdown()
    }

    private val lock = Any()
    private val graphProcessorScope =
        CoroutineScope(dispatcher.plus(CoroutineName("CXCP-GraphLoop")))
    private val processingQueue =
        ProcessingQueue(onUnprocessedElements = ::onShutdown, process = ::commandLoop)
            .processIn(graphProcessorScope)

    @Volatile private var closed = false

    @GuardedBy("lock") private var _requestProcessor: GraphRequestProcessor? = null

    @GuardedBy("lock") private var _repeatingRequest: Request? = null

    var requestProcessor: GraphRequestProcessor?
        get() = synchronized(lock) { _requestProcessor }
        set(value) {
            synchronized(lock) {
                val previous = _requestProcessor
                _requestProcessor = value

                if (closed) {
                    _requestProcessor = null
                    if (value != null) {
                        shutdownScope.launch { value.shutdown() }
                    }
                    return
                }

                // Ignore duplicate calls to set with the same value.
                if (previous === value) {
                    return@synchronized
                }

                if (previous != null) {
                    // Closing the request processor can (sometimes) block the calling thread.
                    // Make sure this is invoked in the background.
                    processingQueue.tryEmit(CloseRequestProcessor(previous))
                }

                if (value != null) {
                    val repeatingRequest = _repeatingRequest
                    if (repeatingRequest == null) {
                        // This handles the case where a single request has been issued before
                        // the GraphRequestProcessor was configured when there is not repeating
                        // request. Invalidate will cause the commandLoop to re-evaluate, which
                        // may succeed now that a valid request processor has been configured.
                        processingQueue.tryEmit(Invalidate)
                    } else {
                        // If there is an active repeating request, make sure the request is
                        // issued to the new request processor. This serves the same purpose as
                        // Invalidate which re-triggers the commandLoop.
                        processingQueue.tryEmit(StartRepeating(repeatingRequest))
                    }
                }
            }

            if (value == null) {
                for (i in listeners.indices) {
                    listeners[i].onGraphStopped()
                }
            }
        }

    var repeatingRequest: Request?
        get() = synchronized(lock) { _repeatingRequest }
        set(value) {
            synchronized(lock) {
                val previous = _repeatingRequest
                _repeatingRequest = value

                // Ignore duplicate calls to set null, this avoids multiple stopRepeating calls from
                // being invoked.
                if (previous == null && value == null) {
                    return@synchronized
                }

                if (value != null) {
                    processingQueue.tryEmit(StartRepeating(value))
                } else {
                    // If the repeating request is set to null, stop repeating (using the current
                    // request processor instance). This is allowed because stop and abort can be
                    // called on a requestProcessor that has, or is in the process, of being
                    // released.
                    processingQueue.tryEmit(StopRepeating(_requestProcessor))
                }
            }

            if (value == null) {
                for (i in listeners.indices) {
                    listeners[i].onStopRepeating()
                }
            }
        }

    private val _captureProcessingEnabled = atomic(true)
    var captureProcessingEnabled: Boolean
        get() = _captureProcessingEnabled.value
        set(value) {
            _captureProcessingEnabled.value = value
            if (value) {
                invalidate()
            }
        }

    fun submit(request: Request): Boolean = submit(listOf(request))

    fun submit(requests: List<Request>): Boolean {
        if (!processingQueue.tryEmit(SubmitCapture(requests))) {
            abortRequests(requests)
            return false
        }
        return true
    }

    fun submit(parameters: Map<*, Any?>): Boolean {
        synchronized(lock) {
            val currentRepeatingRequest = _repeatingRequest
            check(currentRepeatingRequest != null) {
                "Cannot submit parameters without an active repeating request!"
            }
            return processingQueue.tryEmit(SubmitParameters(currentRepeatingRequest, parameters))
        }
    }

    fun abort() {
        processingQueue.tryEmit(AbortCaptures(requestProcessor))
    }

    fun invalidate() {
        synchronized(lock) {
            val currentRepeatingRequest = _repeatingRequest
            if (currentRepeatingRequest != null) {
                processingQueue.tryEmit(StartRepeating(currentRepeatingRequest))
            } else {
                processingQueue.tryEmit(Invalidate)
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true

            val previousRequestProcessor = _requestProcessor
            _requestProcessor = null

            // Shutdown Process - This will occur when the CameraGraph is closed:
            // 1. Clear the _requestProcessor reference. This stops enqueued requests from being
            //    processed, since they use the current requestProcessor instance.
            // 2. Emit a Shutdown call. This will clear or abort any previous requests and will
            //    close the request processor before cancelling the scope.
            processingQueue.tryEmit(Shutdown(previousRequestProcessor))
        }

        for (i in listeners.indices) {
            listeners[i].onGraphShutdown()
        }
    }

    /**
     * Invoke the onAborted listener for each request, prioritizing internal listeners over the
     * request-specific listeners.
     */
    private fun abortRequests(requests: List<Request>) {
        // Internal listeners
        for (rIdx in requests.indices) {
            val request = requests[rIdx]
            for (listenerIdx in graphListeners.indices) {
                graphListeners[listenerIdx].onAborted(request)
            }
        }

        // Request-specific listeners
        for (rIdx in requests.indices) {
            val request = requests[rIdx]
            for (listenerIdx in request.listeners.indices) {
                request.listeners[listenerIdx].onAborted(request)
            }
        }
    }

    private fun onShutdown(unprocessedCommands: List<GraphCommand>) {
        // Cleanup unprocessed state and commands.
        for (command in unprocessedCommands) {
            when (command) {
                is SubmitCapture -> abortRequests(command.requests)
                else -> continue
            }
        }
    }

    private var lastRepeatingRequest: Request? = null

    private suspend fun commandLoop(commands: MutableList<GraphCommand>) {
        // Command Loop Design:
        //
        // 1. Iterate through commands, newest first.
        // 2. If any of the commands match in this first phase, execute the command, remove it (as
        //    well as any other commands that are no longer valid), and then return. This will cause
        //    processCommands to be check for new commands and re-invoke.
        // 3. If none of the phase 1 commands match, process the remaining commands in the order
        //    they were submitted, returning after each submission.

        // ### Phase 1: LIFO High Priority Command Selection ###

        var idx = -1
        if (commands.size > 1) {
            for (i in commands.indices.reversed()) {
                when (commands[i]) {
                    is Invalidate,
                    is Shutdown,
                    is AbortCaptures,
                    is CloseRequestProcessor,
                    is StopRepeating -> {
                        idx = i
                        break
                    }
                    is StartRepeating,
                    is SubmitCapture,
                    is SubmitParameters -> continue
                }
            }

            if (idx < 0) {
                // ### Phase 2: LIFO Secondary Command Selection ###
                //
                // This primarily exists so that [StartRepeating, StopRepeating, StartRepeating]
                // will execute the StopRepeating before the StartRepeating command. SubmitCapture
                // and SubmitParameters are not affected because they are not skip-able.
                for (i in commands.indices.reversed()) {
                    if (commands[i] is StartRepeating) {
                        idx = i
                        break
                    }
                }
            }
        }

        if (idx < 0) {
            // Default: Pick the first command in the queue.
            idx = 0
        }

        // Process and optionally remove the selected command.
        when (val command = commands[idx]) {
            is Invalidate -> commands.removeAt(idx)
            is Shutdown -> {
                commands.removeAt(idx)

                // Remove all commands leading up to Shutdown and abort requests.
                commands.removeUpTo(idx) {
                    if (it is SubmitCapture) {
                        abortRequests(it.requests)
                    }
                    true
                }

                // If the request processor is not null, shut it down. Consider making this a
                // suspending call instead of just blocking to allow suspend-with-timeout.
                command.requestProcessor?.shutdown()

                // Cancel the scope. This will trigger the onUnprocessedItems callback after after
                // this hits the next suspension point.
                graphProcessorScope.cancel()
            }
            is CloseRequestProcessor -> {
                commands.removeAt(idx)
                command.requestProcessor.shutdown()
            }
            is AbortCaptures -> {
                commands.removeAt(idx)

                // Attempt to abort captures in the approximate order they were submitted:
                // 1. Abort captures submitted to the camera
                // 2. Invoke abort on captures that have not yet been submitted to the camera.
                if (command.requestProcessor != null) {
                    command.requestProcessor.abortCaptures()
                }
                commands.removeUpTo(idx) {
                    when (it) {
                        is AbortCaptures -> it.requestProcessor === command.requestProcessor
                        is SubmitCapture -> {
                            abortRequests(it.requests)
                            true
                        }
                        is SubmitParameters -> {
                            // Silently remove parameter requests. These are normally associated
                            // with a repeating request, which will not expect abort commands to
                            // fire.
                            true
                        }
                        else -> false
                    }
                }
            }
            is StopRepeating -> {
                commands.removeAt(idx)
                if (command.requestProcessor != null) {
                    command.requestProcessor.stopRepeating()
                }
                // Always remove prior SubmitRepeating and StopRepeating commands, but only if the
                // StopRepeating commands are associated with the same requestProcessor instance.
                commands.removeUpTo(idx) {
                    it is StartRepeating ||
                        (it is StopRepeating && it.requestProcessor === command.requestProcessor)
                }
            }
            is StartRepeating -> {
                val success =
                    requestProcessor?.buildAndSubmit(
                        isRepeating = true,
                        requests = listOf(command.request)
                    ) == true
                if (success) {
                    lastRepeatingRequest = command.request
                    commands.removeAt(idx)
                }
                commands.removeUpTo(idx) { it is StartRepeating }
            }
            is SubmitCapture -> {
                if (!_captureProcessingEnabled.value) {
                    Log.warn {
                        "Skipping SubmitCapture because capture processing is paused: " +
                            "${command.requests}"
                    }
                    return
                }
                val success =
                    requestProcessor?.buildAndSubmit(
                        isRepeating = false,
                        requests = command.requests
                    ) == true
                if (success) {
                    commands.removeAt(idx)
                } else {
                    Log.warn {
                        "SubmitCapture failed to submit requests to $requestProcessor: " +
                            "${command.requests}, may be retried."
                    }
                }
            }
            is SubmitParameters -> {
                if (!_captureProcessingEnabled.value) {
                    Log.warn {
                        "Skipping SubmitParameters because capture processing is paused: " +
                            "${command.parameters}"
                    }
                    return
                }

                val success =
                    requestProcessor?.buildAndSubmit(
                        isRepeating = false,
                        requests = listOf(command.request),
                        parameters = command.parameters
                    ) == true
                if (success) {
                    commands.removeAt(idx)
                } else {
                    Log.warn {
                        "SubmitParameters failed to submit to $requestProcessor: " +
                            Debug.formatParameterMap(command.parameters)
                    }
                }
            }
        }
    }

    /**
     * Utility function to remove items by index from a mutable list up-to a given index that match
     * the provided function.
     */
    private inline fun <T> MutableList<T>.removeUpTo(idx: Int, predicate: (T) -> Boolean) {
        var a = 0
        var b = idx
        while (a < b) {
            if (predicate(this[a])) {
                this.removeAt(a)
                b-- // Reduce upper bound
            } else {
                a++ // Advance lower bound
            }
        }
    }

    private fun GraphRequestProcessor.buildAndSubmit(
        isRepeating: Boolean,
        requests: List<Request>,
        parameters: Map<*, Any?> = emptyMap<Any, Any?>()
    ): Boolean {
        val graphRequiredParameters = buildMap {
            // Build the required parameter map:
            // 1. graphState3A parameters override provided parameters.
            // 2. requiredParameters override graphState and parameters.
            this.putAllMetadata(parameters)
            graphState3A?.writeTo(this)
            this.putAllMetadata(requiredParameters)
        }

        return this.submit(
            isRepeating = isRepeating,
            requests = requests,
            defaultParameters = defaultParameters,
            requiredParameters = graphRequiredParameters,
            listeners = graphListeners
        )
    }

    override fun toString(): String = "GraphLoop($cameraGraphId)"

    private sealed class GraphCommand

    private object Invalidate : GraphCommand()

    private class Shutdown(val requestProcessor: GraphRequestProcessor?) : GraphCommand()

    private class CloseRequestProcessor(val requestProcessor: GraphRequestProcessor) :
        GraphCommand()

    private class StopRepeating(val requestProcessor: GraphRequestProcessor?) : GraphCommand()

    private class AbortCaptures(val requestProcessor: GraphRequestProcessor?) : GraphCommand()

    private class StartRepeating(val request: Request) : GraphCommand()

    private class SubmitCapture(val requests: List<Request>) : GraphCommand()

    private class SubmitParameters(val request: Request, val parameters: Map<*, Any?>) :
        GraphCommand()
}
