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

package androidx.core.os

import android.content.Context
import android.os.CancellationSignal
import android.os.ProfilingResult
import androidx.annotation.Sampled
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.flowOn

/** Sample showing how to request a java heap dump with various optional parameters. */
@Sampled
fun requestJavaHeapDump(context: Context) {
    val listener =
        Consumer<ProfilingResult> { profilingResult ->
            if (profilingResult.errorCode == ProfilingResult.ERROR_NONE) {
                doSomethingWithMyFile(profilingResult.resultFilePath)
            } else {
                doSomethingWithFailure(profilingResult.errorCode, profilingResult.errorMessage)
            }
        }

    requestProfiling(
        context,
        JavaHeapDumpRequestBuilder()
            .setBufferSizeKb(123 /* Requested buffer size in KB */)
            .setTag("tag" /* Caller supplied tag for identification */)
            .build(),
        Dispatchers.IO.asExecutor(), // Your choice of executor for the callback to occur on.
        listener
    )
}

/**
 * Sample showing how to request a heap profile with various optional parameters and optional
 * cancellation after the event of interest was captured.
 */
@Sampled
fun requestHeapProfile(context: Context) {
    val listener =
        Consumer<ProfilingResult> { profilingResult ->
            if (profilingResult.errorCode == ProfilingResult.ERROR_NONE) {
                doSomethingWithMyFile(profilingResult.resultFilePath)
            } else {
                doSomethingWithFailure(profilingResult.errorCode, profilingResult.errorMessage)
            }
        }

    val cancellationSignal = CancellationSignal()

    requestProfiling(
        context,
        HeapProfileRequestBuilder()
            .setBufferSizeKb(1000 /* Requested buffer size in KB */)
            .setDurationMs(5 * 1000 /* Requested profiling duration in milliseconds */)
            .setTrackJavaAllocations(true)
            .setSamplingIntervalBytes(100 /* Requested sampling interval in bytes */)
            .setTag("tag" /* Caller supplied tag for identification */)
            .setCancellationSignal(cancellationSignal)
            .build(),
        Dispatchers.IO.asExecutor(), // Your choice of executor for the callback to occur on.
        listener
    )

    // Optionally, wait for something interesting to happen and then stop the profiling to receive
    // the result as is.
    cancellationSignal.cancel()
}

/**
 * Sample showing how to request a stack sample with various optional parameters and optional
 * cancellation after the event of interest was captured.
 */
@Sampled
fun requestStackSampling(context: Context) {
    val listener =
        Consumer<ProfilingResult> { profilingResult ->
            if (profilingResult.errorCode == ProfilingResult.ERROR_NONE) {
                doSomethingWithMyFile(profilingResult.resultFilePath)
            } else {
                doSomethingWithFailure(profilingResult.errorCode, profilingResult.errorMessage)
            }
        }

    val cancellationSignal = CancellationSignal()

    requestProfiling(
        context,
        StackSamplingRequestBuilder()
            .setBufferSizeKb(1000 /* Requested buffer size in KB */)
            .setDurationMs(10 * 1000 /* Requested profiling duration in millisconds */)
            .setSamplingFrequencyHz(100 /* Requested sampling frequency */)
            .setTag("tag" /* Caller supplied tag for identification */)
            .setCancellationSignal(cancellationSignal)
            .build(),
        Dispatchers.IO.asExecutor(), // Your choice of executor for the callback to occur on.
        listener
    )

    // Optionally, wait for something interesting to happen and then stop the profiling to receive
    // the result as is.
    cancellationSignal.cancel()
}

/**
 * Sample showing how to request a system trace with various optional parameters and optional
 * cancellation after the event of interest was captured.
 */
@Sampled
fun requestSystemTrace(context: Context) {
    val listener =
        Consumer<ProfilingResult> { profilingResult ->
            if (profilingResult.errorCode == ProfilingResult.ERROR_NONE) {
                doSomethingWithMyFile(profilingResult.resultFilePath)
            } else {
                doSomethingWithFailure(profilingResult.errorCode, profilingResult.errorMessage)
            }
        }

    val cancellationSignal = CancellationSignal()

    requestProfiling(
        context,
        SystemTraceRequestBuilder()
            .setBufferSizeKb(1000 /* Requested buffer size in KB */)
            .setDurationMs(60 * 1000 /* Requested profiling duration in millisconds */)
            .setBufferFillPolicy(BufferFillPolicy.RING_BUFFER /* Buffer fill policy */)
            .setTag("tag" /* Caller supplied tag for identification */)
            .setCancellationSignal(cancellationSignal)
            .build(),
        Dispatchers.IO.asExecutor(), // Your choice of executor for the callback to occur on.
        listener
    )

    // Optionally, wait for something interesting to happen and then stop the profiling to receive
    // the result as is.
    cancellationSignal.cancel()
}

/** Sample showing how to register a listener for all profiling results from your app. */
@Sampled
fun registerForAllProfilingResultsSample(context: Context) {
    val listener =
        Consumer<ProfilingResult> { profilingResult ->
            if (profilingResult.errorCode == ProfilingResult.ERROR_NONE) {
                doSomethingWithMyFile(profilingResult.resultFilePath)
            } else {
                doSomethingWithFailure(profilingResult.errorCode, profilingResult.errorMessage)
            }
        }

    registerForAllProfilingResults(
        context,
        Dispatchers.IO.asExecutor(), // Your choice of executor for the callback to occur on.
        listener
    )
}

/** Sample showing how to register a flow for all profiling results from your app. */
@Sampled
suspend fun registerForAllProfilingResultsFlowSample(context: Context) {
    val flow = registerForAllProfilingResults(context)

    flow
        .flowOn(Dispatchers.IO) // Consume files on a background thread
        .collect { profilingResult ->
            if (profilingResult.errorCode == ProfilingResult.ERROR_NONE) {
                doSomethingWithMyFile(profilingResult.resultFilePath)
            } else {
                doSomethingWithFailure(profilingResult.errorCode, profilingResult.errorMessage)
            }
        }
}

@Suppress("UNUSED_PARAMETER") fun doSomethingWithMyFile(filePath: String?) {}

@Suppress("UNUSED_PARAMETER") fun doSomethingWithFailure(errorCode: Int, errorMessage: String?) {}
