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
package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.RecordingXMessager
import androidx.room.compiler.processing.util.XTestInvocation

/**
 * Common interface for SyntheticProcessors that we create for testing.
 */
@ExperimentalProcessingApi
internal interface SyntheticProcessor {
    /**
     * List of invocations that was sent to the test code.
     *
     * The test code can register assertions on the compilation result, which is why we need this
     * list (to run assertions after compilation).
     */
    val invocationInstances: List<XTestInvocation>

    /**
     * The recorder for messages where we'll grab the diagnostics.
     */
    val messageWatcher: RecordingXMessager

    /**
     * Should return any assertion error that happened during processing.
     *
     * When assertions fail, we don't fail the compilation to keep the stack trace, instead,
     * dispatch them afterwards.
     */
    fun getProcessingException(): Throwable?

    /**
     * Returns true if the processor expected to run another round.
     */
    fun expectsAnotherRound(): Boolean
}

/**
 * Helper class to implement [SyntheticProcessor] processor that handles the communication with
 * the testing infrastructure.
 */
@ExperimentalProcessingApi
internal class SyntheticProcessorImpl(
    handlers: List<(XTestInvocation) -> Unit>
) : SyntheticProcessor {
    private var result: Result<Unit>? = null
    override val invocationInstances = mutableListOf<XTestInvocation>()
    private val nextRunHandlers = handlers.toMutableList()
    override val messageWatcher = RecordingXMessager()

    override fun expectsAnotherRound(): Boolean {
        return nextRunHandlers.isNotEmpty()
    }

    /**
     * Returns true if this can run another round, which means previous round didn't throw an
     * exception and there is another handler in the queue.
     */
    fun canRunAnotherRound(): Boolean {
        if (result?.exceptionOrNull() != null) {
            // if there is an existing failure from a previous run, stop
            return false
        }
        return expectsAnotherRound()
    }

    override fun getProcessingException(): Throwable? {
        val result = this.result ?: return AssertionError("processor didn't run")
        result.exceptionOrNull()?.let {
            return it
        }
        if (result.isFailure) {
            return AssertionError("processor failed but no exception is reported")
        }
        return null
    }

    /**
     * Runs the next handler with the given test invocation.
     */
    fun runNextRound(
        invocation: XTestInvocation
    ) {
        check(nextRunHandlers.isNotEmpty()) {
            "Called run next round w/o a runner to handle it. Looks like a testing infra bug"
        }
        val handler = nextRunHandlers.removeAt(0)
        invocationInstances.add(invocation)
        invocation.processingEnv.messager.addMessageWatcher(messageWatcher)
        result = kotlin.runCatching {
            handler(invocation)
            invocation.dispose()
        }
    }
}
