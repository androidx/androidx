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

package androidx.tracing.driver.wire

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import okio.appendingSink
import okio.buffer

private fun File.perfettoTraceFile(): File {
    val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    val traceFile = File(this, "perfetto-${formatter.format(Date())}.perfetto-trace")
    return traceFile
}

@JvmOverloads
public fun TraceSink(
    directory: File,
    sequenceId: Int,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): TraceSink =
    TraceSink(
        sequenceId = sequenceId,
        bufferedSink = directory.perfettoTraceFile().appendingSink().buffer(),
        coroutineContext = coroutineContext,
    )
