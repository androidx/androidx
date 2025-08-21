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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastNone

/**
 * A diagnostic exception with a composition stack trace. This exception is usually appended to the
 * suppressed exceptions when [androidx.compose.runtime.Composer.setDiagnosticStackTraceEnabled]
 * flag is set to true.
 */
internal expect class DiagnosticComposeException(trace: List<ComposeStackTraceFrame>) :
    RuntimeException

@OptIn(ComposeToolingApi::class)
internal data class ComposeStackTraceFrame(
    val sourceInfo: SourceInformation,
    val groupOffset: Int?,
)

internal fun Throwable.tryAttachComposeStackTrace(
    trace: () -> List<ComposeStackTraceFrame>
): Boolean {
    var result = false
    if (suppressedExceptions.fastNone { it is DiagnosticComposeException }) {
        val traceException =
            try {
                val frames = trace()
                result = frames.isNotEmpty()
                if (result) DiagnosticComposeException(frames) else null
            } catch (e: Throwable) {
                // Attach the exception thrown while collecting trace.
                // Usually this means that the slot table is malformed.
                e
            }
        if (traceException != null) {
            addSuppressed(traceException)
        }
    }
    return result
}

internal fun Throwable.attachComposeStackTrace(
    trace: () -> List<ComposeStackTraceFrame>
): Throwable = apply { tryAttachComposeStackTrace(trace) }

@OptIn(ComposeToolingApi::class)
internal fun StringBuilder.appendStackTrace(trace: List<ComposeStackTraceFrame>) {
    var currentFunction: String? = null
    var currentFile: String? = null
    val lines = buildList {
        trace.asReversed().fastForEach { frame ->
            val sourceInfo = frame.sourceInfo
            val functionName =
                sourceInfo.functionName
                    ?: "<lambda>".takeIf { sourceInfo.isCall }
                    ?: currentFunction
                    ?: "<unknown function>"

            val fileName = sourceInfo.sourceFile ?: currentFile ?: "<unknown file>"
            val lineNumbers = sourceInfo.locations
            val resolvedLine =
                if (frame.groupOffset != null && frame.groupOffset < lineNumbers.size) {
                    lineNumbers[frame.groupOffset].lineNumber.toString()
                } else {
                    if (IncludeDebugInfo) {
                        "<no offset ${frame.groupOffset} in $lineNumbers>"
                    } else {
                        "<unknown line>"
                    }
                }

            val traceLine = buildString {
                append(functionName)
                append('(')
                append(fileName)
                append(':')
                append(resolvedLine)
                append(')')

                if (IncludeDebugInfo) {
                    append(", parsed from ")
                    append(sourceInfo.rawData)
                    append(", group offset: ")
                    append(frame.groupOffset)
                }
            }

            if (!sourceInfo.isCall) {
                // replace previous line for source info, since this line will provide more
                // precise info for line numbers from previous entry
                val line = removeLastOrNull()
                if (IncludeDebugInfo) {
                    add("$line (collapsed)")
                }
            }

            // Filter first subcomposition frames that point to rememberCompositionContext.
            if (
                sourceInfo.functionName == "rememberCompositionContext" &&
                    sourceInfo.packageHash == RuntimePackageHash
            ) {
                if (IncludeDebugInfo) {
                    add("$traceLine (ignored)")
                }
            } else {
                add(traceLine)
            }

            currentFunction = functionName
            currentFile = fileName
        }
    }
    lines.asReversed().fastForEach { appendLine("\tat $it") }
}

private const val RuntimePackageHash = "9igjgp"

private const val IncludeDebugInfo = false
