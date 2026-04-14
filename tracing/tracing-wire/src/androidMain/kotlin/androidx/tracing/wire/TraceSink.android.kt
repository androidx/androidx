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

@file:JvmName("TraceSinkUtils") // Provide a reasonable name for Java users.

package androidx.tracing.wire

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import java.io.File
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext
import okio.BufferedSink
import okio.appendingSink
import okio.buffer
import okio.sink

public fun TraceSink(
    context: Context,
    sequenceId: Int,
    coroutineContext: CoroutineContext,
    outputStream: OutputStream,
): TraceSink =
    TraceSink(
        context = context,
        sequenceId = sequenceId,
        coroutineContext = coroutineContext,
        bufferedSink = outputStream.sink().buffer(),
    )

@JvmOverloads
public fun TraceSink(
    context: Context,
    sequenceId: Int,
    coroutineContext: CoroutineContext,
    traceFile: File = context.filesDir.createPerfettoFile(),
): TraceSink {
    val sink =
        TraceSink(
            context = context,
            sequenceId = sequenceId,
            bufferedSink = traceFile.appendingSink().buffer(),
            coroutineContext = coroutineContext,
        )
    return sink
}

@JvmInline
private value class FlushCallback(private val sink: TraceSink) : ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        sink.flush()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Nothing to do.
    }

    override fun onLowMemory() {
        sink.flush()
    }
}

private fun TraceSink(
    context: Context,
    sequenceId: Int,
    coroutineContext: CoroutineContext,
    bufferedSink: BufferedSink,
): TraceSink {
    val sink =
        TraceSink(
            sequenceId = sequenceId,
            bufferedSink = bufferedSink,
            coroutineContext = coroutineContext,
        )
    val callback = FlushCallback(sink)
    context.applicationContext.registerComponentCallbacks(callback)
    return sink
}
