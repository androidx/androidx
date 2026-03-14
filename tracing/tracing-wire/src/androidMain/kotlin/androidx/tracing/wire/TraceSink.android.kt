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
import androidx.tracing.AbstractTraceSink
import androidx.tracing.PooledTracePacketArray
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.CoroutineContext
import okio.BufferedSink
import okio.appendingSink
import okio.buffer
import okio.sink

private fun File.perfettoTraceFile(): File {
    val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    val traceFile = File(this, "perfetto-${formatter.format(Date())}.perfetto-trace")
    return traceFile
}

public fun TraceSink(
    context: Context,
    sequenceId: Int,
    coroutineContext: CoroutineContext,
    outputStream: OutputStream,
): AbstractTraceSink =
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
    traceFile: File = context.filesDir.perfettoTraceFile(),
): AbstractTraceSink {
    val sink =
        TraceSink(
            context = context,
            sequenceId = sequenceId,
            bufferedSink = traceFile.appendingSink().buffer(),
            coroutineContext = coroutineContext,
        )
    return sink
}

private class TraceSinkDelegate(private val context: Context, private val sink: TraceSink) :
    AbstractTraceSink() {
    private val callback: FlushCallback = FlushCallback(sink)

    init {
        context.applicationContext.registerComponentCallbacks(callback)
    }

    override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
        sink.enqueue(pooledPacketArray)
    }

    override fun onDroppedTraceEvent() {
        sink.onDroppedTraceEvent()
    }

    override fun flush() {
        sink.flush()
    }

    override fun close() {
        sink.close()
        context.applicationContext.unregisterComponentCallbacks(callback)
    }
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
): AbstractTraceSink {
    val sink =
        TraceSink(
            sequenceId = sequenceId,
            bufferedSink = bufferedSink,
            coroutineContext = coroutineContext,
        )
    return TraceSinkDelegate(context = context, sink = sink)
}
