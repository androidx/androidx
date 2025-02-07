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

package androidx.tracing.driver

import android.content.Context
import com.squareup.wire.ProtoWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import okio.BufferedSink
import okio.appendingSink
import okio.buffer

/** The trace sink that writes to a new file per trace session. */
@SuppressWarnings("StreamFiles") // Accepts a BufferedSink instead.
public class AndroidTraceSink(
    context: Context,
    private val bufferedSink: BufferedSink,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : TraceSink() {

    public constructor(
        context: Context
    ) : this(context = context, traceFile = context.noBackupFilesDir.perfettoTraceFile())

    @SuppressWarnings("StreamFiles") // Accepting a BufferedSink instead.
    public constructor(
        context: Context,
        traceFile: File,
    ) : this(context = context, bufferedSink = traceFile.appendingSink().buffer())

    // Hold on to the application context.
    @Suppress("UNUSED") private val context = context.applicationContext
    private val protoWriter: ProtoWriter = ProtoWriter(bufferedSink)

    // There are 2 distinct mechanisms for thread safety here, and they are not necessarily in sync.
    // The Queue by itself is thread-safe, but after we drain the queue we mark drainRequested
    // to false (not an atomic operation). So a writer can come along and add a pooled array of
    // trace packets. That is still okay given, those packets will get picked during the next
    // drain request; or on flush() prior to the close() of the Sink.
    // No packets are lost or dropped; and therefore we are still okay with this small
    // compromise with thread safety.
    private val queue = Queue<PooledTracePacketArray>()
    private val drainRequested = AtomicBoolean(false)

    @Volatile private var resumeDrain: Continuation<Unit>

    init {
        resumeDrain =
            suspend {
                    coroutineContext[Job]?.invokeOnCompletion { makeDrainRequest() }
                    while (true) {
                        drainQueue() // Sets drainRequested to false on completion
                        suspendCoroutine<Unit> { continuation ->
                            resumeDrain = continuation
                            COROUTINE_SUSPENDED // Suspend
                        }
                    }
                }
                .createCoroutineUnintercepted(Continuation(context = coroutineContext) {})

        // Kick things off and suspend
        makeDrainRequest()
    }

    override fun emit(pooledPacketArray: PooledTracePacketArray) {
        queue.addFirst(pooledPacketArray)
        makeDrainRequest()
    }

    override fun flush() {
        makeDrainRequest()
        while (drainRequested.get()) {
            // Await completion of the drain.
        }
        bufferedSink.flush()
    }

    private fun makeDrainRequest() {
        // Only make a request if one is not already ongoing
        if (drainRequested.compareAndSet(false, true)) {
            resumeDrain.resume(Unit)
        }
    }

    private fun drainQueue() {
        while (queue.isNotEmpty()) {
            val pooledPacketArray = queue.removeFirstOrNull()
            if (pooledPacketArray != null) {
                for (pooledTracePacket in pooledPacketArray.pooledTracePacketArray) {
                    if (pooledTracePacket != null) {
                        pooledTracePacket.encodeTracePacket(protoWriter)
                        pooledTracePacket.recycle()
                    }
                }
                pooledPacketArray.recycle()
            }
        }
        drainRequested.set(false)
    }

    override fun close() {
        flush()
        bufferedSink.close()
    }
}

private fun File.perfettoTraceFile(): File {
    val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    val traceFile = File(this, "perfetto-${formatter.format(Date())}.perfetto-trace")
    return traceFile
}
