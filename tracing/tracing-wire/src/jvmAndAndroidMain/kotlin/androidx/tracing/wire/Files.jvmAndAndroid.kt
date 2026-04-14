/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.tracing.wire

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.annotation.VisibleForTesting
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// The Perfetto file name prefix.
@VisibleForTesting internal const val PREFIX = "perfetto"

/**
 * @return the trace [File], for a given parent directory, that can be used by the
 *   [androidx.tracing.AbstractTraceSink].
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public fun File.perfettoTraceFile(): File {
    val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getDefault()
    val fileName = "$PREFIX-${formatter.format(Date())}"
    return perfettoTraceFile(fileName = fileName, sequenceId = 0)
}

/**
 * @param fileName The name of the [File], typically derived by using a timestamp.
 * @param sequenceId The [File] name suffix to be used to disambiguate between trace sessions
 *   already running, or multiple processes emitting traces simultaneously in one directory.
 * @return the trace [File], for a given parent directory, that can be used by the
 *   [androidx.tracing.AbstractTraceSink].
 */
@VisibleForTesting
@RestrictTo(Scope.LIBRARY_GROUP)
public tailrec fun File.perfettoTraceFile(fileName: String, sequenceId: Int = 0): File {
    val traceFile = File(this, "$fileName-$sequenceId.perfetto-trace")
    return if (traceFile.createNewFile()) {
        traceFile
    } else {
        perfettoTraceFile(fileName = fileName, sequenceId = sequenceId + 1)
    }
}
