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
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.ProcessingQueue
import androidx.camera.camera2.pipe.core.ProcessingQueue.Companion.processIn
import androidx.camera.camera2.pipe.putAllMetadata
import java.io.Closeable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
    private val listeners: List<Listener>,
    private val shutdownScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : Closeable {
    internal interface Listener {

        fun onStopRepeating()

        fun onGraphStopped()

        fun onGraphShutdown()
    }

    private val graphLoopScope = CoroutineScope(dispatcher.plus(CoroutineName("CXCP-GraphLoop")))
    private val processingQueue =
        ProcessingQueue(onUnprocessedElements = ::finalizeUnprocessedCommands, process = ::process)
            .processIn(graphLoopScope)

    private val lock = Any()

    @Volatile private var closed = false
    @GuardedBy("lock") private var _requestProcessor: GraphRequestProcessor? = null
    @GuardedBy("lock") private var _repeatingRequest: Request? = null
    @GuardedBy("lock") private var _graphParameters: Map<*, Any?> = emptyMap<Any, Any?>()
    @GuardedBy("lock") private var _graph3AParameters: Map<*, Any?> = emptyMap<Any, Any?>()

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
                processingQueue.tryEmit(GraphCommand.RequestProcessor(previous, value))
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
                    processingQueue.tryEmit(GraphCommand.Repeat(value))
                } else {
                    // If the repeating request is set to null, stop repeating.
                    processingQueue.tryEmit(GraphCommand.Stop)
                }
            }
            if (value == null) {
                for (i in listeners.indices) {
                    listeners[i].onStopRepeating()
                }
            }
        }

    var graphParameters: Map<*, Any?>
        get() = synchronized(lock) { _graphParameters }
        set(value) =
            synchronized(lock) {
                _graphParameters = value
                processingQueue.tryEmit(GraphCommand.Parameters(value, _graph3AParameters))
            }

    var graph3AParameters: Map<*, Any?>
        get() = synchronized(lock) { _graph3AParameters }
        set(value) =
            synchronized(lock) {
                _graph3AParameters = value
                processingQueue.tryEmit(GraphCommand.Parameters(_graphParameters, value))
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
        if (!processingQueue.tryEmit(GraphCommand.Capture(requests))) {
            abortRequests(requests)
            return false
        }
        return true
    }

    fun trigger(parameters: Map<*, Any?>): Boolean {
        check(repeatingRequest != null) {
            "Cannot submit parameters without an active repeating request!"
        }
        return processingQueue.tryEmit(GraphCommand.Trigger(parameters))
    }

    fun abort() {
        processingQueue.tryEmit(GraphCommand.Abort)
    }

    fun invalidate() {
        processingQueue.tryEmit(GraphCommand.Invalidate)
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true

            _requestProcessor?.let { processor -> shutdownScope.launch { processor.shutdown() } }
            _requestProcessor = null

            // Shutdown Process - This will occur when the CameraGraph is closed:
            // 1. Clear the _requestProcessor reference. This stops enqueued requests from being
            //    processed, since they use the current requestProcessor instance.
            // 2. Emit a Shutdown call. This will clear or abort any previous requests and will
            //    close the request processor before cancelling the scope.
            processingQueue.tryEmit(GraphCommand.Shutdown)
        }

        // Invoke shutdown listeners. There is a small chance that additional elements will be
        // canceled or released after this point due to unprocessed elements in the queue.
        for (i in listeners.indices) {
            listeners[i].onGraphShutdown()
        }
    }

    private var currentRepeatingRequest: Request? = null
    private var currentGraphParameters: Map<*, Any?> = emptyMap<Any, Any?>()
    private var currentGraph3AParameters: Map<*, Any?> = emptyMap<Any, Any?>()
    private var currentRequiredParameters: Map<*, Any?> = requiredParameters
    private var currentRequestProcessor: GraphRequestProcessor? = null

    private suspend fun process(commands: MutableList<GraphCommand>) {
        // The GraphLoop is responsible for bridging the core interactions with a camera so that
        // ordering (and thus deterministic execution) is preserved, while also optimizing away
        // unnecessary operations in real time.
        //
        // Unlike the consumer of a Flow, these optimizations require access to the full state of
        // the command queue in order to evaluate what operations are redundant and may be safely
        // skipped without altering the guarantees provided by the API surface.
        //
        // In general, this function must execute as fast as possible (But is allowed to suspend).
        // after returning, the function may be re-invoked if:
        //
        // 1. The number of items `commands` is different, and non-zero
        // 2. New items were added to the queue while process was executing.
        //
        // To keep things organized, commands are split into individual functions.

        val idx = selectGraphCommand(commands)

        // Process the selected command
        when (val command = commands[idx]) {
            GraphCommand.Invalidate -> commands.removeAt(idx)
            GraphCommand.Shutdown -> processShutdown(commands)
            GraphCommand.Abort -> processAbort(commands, idx)
            GraphCommand.Stop -> processStop(commands, idx)
            is GraphCommand.RequestProcessor -> processRequestProcessor(commands, idx, command)
            is GraphCommand.Capture -> processCapture(commands, idx, command)
            is GraphCommand.Trigger -> processTrigger(commands, idx, command)
            is GraphCommand.Parameters -> processParameters(commands, idx, command)
            is GraphCommand.Repeat -> processRepeat(commands, idx)
        }
    }

    private fun selectGraphCommand(commands: MutableList<GraphCommand>): Int {
        // This function will never be invoked with an empty command list.
        if (commands.size == 1) return 0

        // First, pick "interrupt commands". These are prioritized because they tend to remove other
        // commands, or are guaranteed to be a NoOp (Invalidate). Because of this, pick the most
        // recent interrupt command in the command list.
        //
        // RequestProcessor commands are special - The most recent one should always be selected,
        // but it should be lower priority than Abort / Stop / Shutdown (and Invalidate). To avoid
        // looping over it twice, track the first instance that is encountered, and return it if one
        // is found and no other interrupt commands have been found.
        var latestRequestProcessorCommand = -1
        for (i in commands.indices.reversed()) {
            when (commands[i]) {
                GraphCommand.Abort,
                GraphCommand.Invalidate,
                GraphCommand.Stop,
                GraphCommand.Shutdown -> {
                    return i
                }
                is GraphCommand.RequestProcessor -> {
                    if (latestRequestProcessorCommand < 0) {
                        latestRequestProcessorCommand = i
                    }
                }
                else -> continue
            }
        }

        if (latestRequestProcessorCommand >= 0) {
            return latestRequestProcessorCommand
        }

        // If there are no interrupt commands, prioritize commands that update parameters.
        //
        // However - we must maintain ordering to avoid skipping over trigger and capture commands.
        // We can skip over StartRepeating calls, but not SubmitCapture or SubmitParameter calls.
        // To do this, we iterate through the commands in order until we hit a non-Parameter or a
        // non-Repeat command. We then return the most-recent parameter command to execute.
        var latestParameterCommand = -1
        for (i in commands.indices) {
            when (commands[i]) {
                is GraphCommand.Parameters -> latestParameterCommand = i
                is GraphCommand.Repeat -> continue
                else -> break
            }
        }
        if (latestParameterCommand >= 0) {
            return latestParameterCommand
        }

        // If the current repeating request is valid, and captureProcessing is enabled, prioritize
        // capture and trigger commands.
        if (currentRepeatingRequest != null && captureProcessingEnabled) {
            // Pick the first Capture or Trigger command
            for (i in commands.indices) {
                when (commands[i]) {
                    is GraphCommand.Capture,
                    is GraphCommand.Trigger -> return i
                    else -> continue
                }
            }
        }

        // Pick the most recent Repeat command without skipping over Capture/Triggers
        var latestRepeatingCommand = -1
        for (i in commands.indices) {
            when (commands[i]) {
                is GraphCommand.Repeat -> latestRepeatingCommand = i
                else -> break
            }
        }
        if (latestRepeatingCommand >= 0) {
            return latestRepeatingCommand
        }

        // Pick the next command in order.
        return 0
    }

    private fun processCapture(
        commands: MutableList<GraphCommand>,
        idx: Int,
        command: GraphCommand.Capture,
        repeatAllowed: Boolean = true,
    ) {
        if (captureProcessingEnabled) {
            if (buildAndSubmit(isRepeating = false, requests = command.requests)) {
                commands.removeAt(idx)
                return
            }
        }

        // If captureProcessing failed, or if we cannot currently issue captures, check to see if
        // there are prior repeating requests that we should attempt.
        if (repeatAllowed && idx > 0) {
            val previousCommand = commands[idx - 1]
            // The previous command, if it exists (idx > 0), must always be a Repeat command as
            // other commands must always be prioritized.
            check(previousCommand is GraphCommand.Repeat)
            processRepeat(commands, idx - 1, captureAllowed = false)
        }
    }

    private fun processTrigger(
        commands: MutableList<GraphCommand>,
        idx: Int,
        command: GraphCommand.Trigger,
    ) {
        // Trigger commands take an existing repeating request, add some one-time parameters to it,
        // and the submit it exactly once.
        val repeatingRequest = currentRepeatingRequest
        if (repeatingRequest == null && idx == 0) {
            commands.removeAt(idx)
            return
        }

        // If capture processing is enabled, and there is a non-null repeating request, attempt to
        // submit the trigger.
        if (captureProcessingEnabled && repeatingRequest != null) {
            if (
                buildAndSubmit(
                    isRepeating = false,
                    requests = listOf(repeatingRequest),
                    oneTimeRequiredParameters = command.triggerParameters,
                )
            ) {
                commands.removeAt(idx)
                return
            }
        }

        // If processTrigger failed, or if we cannot currently issue captures, check to see if
        // there are prior repeating requests that we should attempt.
        if (idx > 0) {
            val previousCommand = commands[idx - 1]
            check(previousCommand is GraphCommand.Repeat)
            processRepeat(commands, idx - 1, captureAllowed = false)
        }
    }

    private fun processRepeat(
        commands: MutableList<GraphCommand>,
        idx: Int,
        captureAllowed: Boolean = true,
    ) {
        // Attempt to issue the repeating request at idx.
        // 1. If that fails - move backwards through the list, attempting each repeating command in
        //    order.
        // 2. If submitting a repeating request from the command queue fails, attempt to submit the
        //    next command, if it is a trigger or a capture.
        for (i in idx downTo 0) {
            val command = commands[i]
            if (
                command is GraphCommand.Repeat &&
                    buildAndSubmit(isRepeating = true, requests = listOf(command.request))
            ) {
                currentRepeatingRequest = command.request
                commands.removeAt(i)
                commands.removeUpTo(i) { it is GraphCommand.Repeat }
                return
            }
        }

        // Repeating request failed, and there is a command in the queue after idx, and we are
        // allowed to attempt capture (Capture can invoke processRepeat, and this avoids loops)
        if (captureAllowed && idx + 1 < commands.size) {
            val nextCommand = commands[idx + 1]
            when (nextCommand) {
                is GraphCommand.Capture ->
                    processCapture(commands, idx + 1, nextCommand, repeatAllowed = false)
                is GraphCommand.Trigger -> processTrigger(commands, idx + 1, nextCommand)
                else -> return
            }
        }
    }

    private fun processParameters(
        commands: MutableList<GraphCommand>,
        idx: Int,
        command: GraphCommand.Parameters,
    ) {
        currentGraphParameters = command.graphParameters
        currentGraph3AParameters = command.graph3AParameters
        currentRequiredParameters =
            if (command.graph3AParameters.isEmpty()) {
                requiredParameters
            } else {
                buildMap {
                    putAllMetadata(command.graph3AParameters)
                    putAllMetadata(requiredParameters)
                }
            }

        commands.removeAt(idx)
        commands.removeUpTo(idx) { it is GraphCommand.Parameters }
        reissueRepeatingRequest()
    }

    private suspend fun processRequestProcessor(
        commands: MutableList<GraphCommand>,
        idx: Int,
        command: GraphCommand.RequestProcessor,
    ) {
        var commandsRemoved = 1
        commands.removeAt(idx)
        commands.removeUpTo(idx) {
            when (it) {
                is GraphCommand.RequestProcessor -> {
                    it.old?.shutdown()
                    it.new?.shutdown()
                    commandsRemoved++
                    true
                }
                else -> false
            }
        }

        command.old?.shutdown()
        currentRequestProcessor = command.new

        // If we have a previously submitted repeating request, attempt to resubmit it. If that
        // fails, add it to the beginning of the queue.
        if (!reissueRepeatingRequest()) {
            currentRepeatingRequest?.let {
                commands.add(0, GraphCommand.Repeat(it))

                // Edge case: The graphProcessor may not re-attempt unless the number of items in
                // `commands` has changed.
                if (commandsRemoved == 1) {
                    commands.add(GraphCommand.Invalidate)
                }
            }
            currentRepeatingRequest = null
        }
    }

    private fun processStop(commands: MutableList<GraphCommand>, idx: Int) {
        // stopRepeating causes the current repeating request to stop, but does not affect capture
        // commands. Invoke stopRepeating on the current RequestProcessor and clear the current
        // repeating request.
        currentRequestProcessor?.stopRepeating()
        currentRepeatingRequest = null

        // Remove all `Stop` and `Repeat` commands prior to the current stop command, since they
        // are no longer relevant.
        commands.removeAt(idx)
        commands.removeUpTo(idx) {
            when (it) {
                GraphCommand.Stop,
                is GraphCommand.Repeat -> true
                else -> false
            }
        }
    }

    private fun processAbort(commands: MutableList<GraphCommand>, idx: Int) {
        // abortCaptures affects both in-flight captures and the current repeating request.
        // Invoke abortCaptures on the current RequestProcessor, and then clear the current
        // repeating
        // request, any pending Stop/Abort commands, and any pending Capture or Trigger commands.
        currentRequestProcessor?.abortCaptures()
        currentRepeatingRequest = null

        commands.removeAt(idx)
        commands.removeUpTo(idx) {
            when (it) {
                GraphCommand.Stop,
                GraphCommand.Abort,
                is GraphCommand.Repeat,
                is GraphCommand.Trigger -> true
                is GraphCommand.Capture -> {
                    // Make sure listeners for capture events are always triggered.
                    abortRequests(it.requests)
                    true
                }
                else -> false
            }
        }
    }

    private suspend fun processShutdown(commands: MutableList<GraphCommand>) {
        currentRepeatingRequest = null
        currentGraphParameters = emptyMap<Any, Any>()
        currentGraph3AParameters = emptyMap<Any, Any>()

        // Abort and remove all commands during shutdown.
        for (idx in commands.indices) {
            val command = commands[idx]
            if (command is GraphCommand.Capture) {
                // Make sure listeners for capture events are always triggered.
                abortRequests(command.requests)
            }
        }

        // Shutdown request processors (Current and pending)
        currentRequestProcessor?.shutdown()
        currentRequestProcessor = null

        for (idx in commands.indices) {
            val command = commands[idx]
            if (command is GraphCommand.RequestProcessor) {
                command.old?.shutdown()
                command.new?.shutdown()
            }
        }
        commands.clear()

        // Cancel the scope. This will trigger the onUnprocessedItems callback after this returns
        // and hits the next suspension point.
        graphLoopScope.cancel()
    }

    /** Attempt to re-issue a previously submitted repeating request, likely with new parameters */
    private fun reissueRepeatingRequest(): Boolean =
        currentRequestProcessor?.let { processor ->
            currentRepeatingRequest?.let { request ->
                processor.submit(
                    isRepeating = true,
                    requests = listOf(request),
                    defaultParameters = defaultParameters,
                    graphParameters = currentGraphParameters,
                    requiredParameters = currentRequiredParameters,
                    listeners = graphListeners,
                )
            }
        } == true

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

    private fun finalizeUnprocessedCommands(unprocessedCommands: List<GraphCommand>) {
        // When the graph loop is shutdown it is possible that additional elements may have been
        // added to the queue. To avoid leaking resources, ensure that capture commands are aborted
        // and that requestProcessor commands shutdown the associated request processor(s).
        for (command in unprocessedCommands) {
            when (command) {
                // Make sure listeners for capture events are always triggered.
                is GraphCommand.Capture -> abortRequests(command.requests)
                is GraphCommand.RequestProcessor -> {
                    shutdownScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        command.old?.shutdown()
                        command.new?.shutdown()
                    }
                }
                else -> continue
            }
        }
    }

    private fun buildAndSubmit(
        isRepeating: Boolean,
        requests: List<Request>,
        oneTimeRequiredParameters: Map<*, Any?> = emptyMap<Any, Any>(),
    ): Boolean {
        val processor = currentRequestProcessor
        if (processor == null) return false

        val success =
            processor.submit(
                isRepeating = isRepeating,
                requests = requests,
                defaultParameters = defaultParameters,
                graphParameters = currentGraphParameters,
                requiredParameters =
                    if (oneTimeRequiredParameters.isEmpty()) {
                        currentRequiredParameters
                    } else {
                        buildMap<Any, Any?> {
                            this.putAllMetadata(currentGraph3AParameters)
                            this.putAllMetadata(oneTimeRequiredParameters)
                            this.putAllMetadata(requiredParameters)
                        }
                    },
                listeners = graphListeners,
            )

        if (!success) {
            if (isRepeating) {
                Log.warn { "Failed to repeat with ${requests.single()}" }
            } else {
                if (oneTimeRequiredParameters.isEmpty()) {
                    Log.warn { "Failed to submit capture with $requests" }
                } else {
                    Log.warn {
                        "Failed to trigger with ${requests.single()} and $oneTimeRequiredParameters"
                    }
                }
            }
        }

        return success
    }

    override fun toString(): String = "GraphLoop($cameraGraphId)"

    companion object {
        /**
         * Utility function to remove items by index from a `MutableList` up-to (but not including)
         * the provided index without creating an iterator.
         *
         * For example, in a list of [1, 2, 3, 4, 5], calling removeUpTo(3) { it % 2 = 1 } will test
         * [1, 2, 3], and remove "1" and "3", modifying the list to have [2, 4, 5]
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
    }
}

internal sealed interface GraphCommand {
    object Invalidate : GraphCommand

    object Shutdown : GraphCommand

    object Stop : GraphCommand

    object Abort : GraphCommand

    class RequestProcessor(val old: GraphRequestProcessor?, val new: GraphRequestProcessor?) :
        GraphCommand

    class Parameters(val graphParameters: Map<*, Any?>, val graph3AParameters: Map<*, Any?>) :
        GraphCommand

    class Repeat(val request: Request) : GraphCommand

    class Capture(val requests: List<Request>) : GraphCommand

    class Trigger(val triggerParameters: Map<*, Any?>) : GraphCommand
}
