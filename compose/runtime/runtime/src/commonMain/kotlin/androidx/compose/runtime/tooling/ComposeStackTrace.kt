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

import androidx.compose.runtime.compositionLocalMapKey
import androidx.compose.runtime.defaultsKey
import androidx.compose.runtime.invocationKey
import androidx.compose.runtime.movableContentKey
import androidx.compose.runtime.nodeKey
import androidx.compose.runtime.providerKey
import androidx.compose.runtime.providerMapsKey
import androidx.compose.runtime.recomposerKey
import androidx.compose.runtime.referenceKey
import androidx.compose.runtime.reuseKey
import androidx.compose.runtime.rootKey
import androidx.compose.runtime.snapshots.fastAny
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastNone
import kotlin.jvm.JvmInline

/**
 * Defines how Compose runtime collects stack traces after a crash. The stack trace mode is
 * configured globally for all composition instances through
 * [androidx.compose.runtime.Composer.setDiagnosticStackTraceMode].
 *
 * @see CompositionErrorContext
 */
@JvmInline
public value class ComposeStackTraceMode private constructor(private val value: Int) {
    public companion object {
        /** No stack trace information will be collected. */
        public val None: ComposeStackTraceMode = ComposeStackTraceMode(0)

        /**
         * Collects a stack trace with group keys. This stack trace can be deobfuscated with the
         * proguard mapping that ships with the app (starting with Kotlin 2.3.0).
         *
         * The group key stack traces are less precise than source information, but they do not add
         * any runtime overhead until crash occurs and work with minified builds.
         *
         * Example stack trace:
         * ```
         * java.lang.IllegalStateException: <message>
         *     at <original trace>
         * Suppressed: androidx.compose.runtime.ComposeTraceException:
         * Composition stack when thrown:
         *     at $$compose.m$123(SourceFile:1)
         *     at $$compose.m$234(SourceFile:1)
         *     ...
         * ```
         */
        public val GroupKeys: ComposeStackTraceMode = ComposeStackTraceMode(1)

        /**
         * Collects source information for stack trace purposes. When this flag is enabled,
         * composition will record source information at runtime. When crash occurs, Compose will
         * append a suppressed exception that contains a stack trace pointing to the place in
         * composition closest to the crash.
         *
         * Note that:
         * - Recording source information introduces additional performance overhead, so this option
         *   should NOT be enabled in release builds that are not optimized with R8 (or equivalent).
         * - Compose ships with a minifier config that removes source information from the release
         *   builds. Enabling this flag in minified builds will fallback in [GroupKeys]
         *
         * Example stack trace:
         * ```
         * java.lang.IllegalStateException: <message>
         *     at <original trace>
         * Suppressed: androidx.compose.runtime.ComposeTraceException:
         * Composition stack when thrown:
         *     at ReusableComposeNode(Composables.kt:359)
         *     at Layout(Layout.kt:79)
         *     at <lambda>(App.kt:164)
         *     ...
         * ```
         */
        public val SourceInformation: ComposeStackTraceMode = ComposeStackTraceMode(2)

        /** [GroupKeys] when app is minified, or [None] otherwise. */
        public val Auto: ComposeStackTraceMode
            get() = if (isMinified) GroupKeys else None

        /* This field should always return true when app is minified by R8. */
        private var isMinified: Boolean = false
    }
}

/**
 * A diagnostic exception with a composition stack trace. This exception is usually appended to the
 * suppressed exceptions when [androidx.compose.runtime.Composer.setDiagnosticStackTraceMode] flag
 * is set to true.
 */
internal expect class DiagnosticComposeException(trace: ComposeStackTrace) : RuntimeException

internal class ComposeStackTrace(val frames: List<ComposeStackTraceFrame>) {
    @OptIn(ComposeToolingApi::class)
    val hasSourceInformation
        get() = frames.fastAny { it.sourceInfo != null }
}

@OptIn(ComposeToolingApi::class)
internal data class ComposeStackTraceFrame(
    val groupKey: Int,
    val sourceInfo: SourceInformation?,
    val groupOffset: Int?,
)

internal fun Throwable.tryAttachComposeStackTrace(trace: () -> ComposeStackTrace?): Boolean {
    var result = false
    if (suppressedExceptions.fastNone { it is DiagnosticComposeException }) {
        val traceException =
            try {
                val stackTrace = trace()
                result = stackTrace != null && stackTrace.frames.isNotEmpty()
                if (result) DiagnosticComposeException(stackTrace!!) else null
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

internal fun Throwable.attachComposeStackTrace(trace: () -> ComposeStackTrace?): Throwable = apply {
    tryAttachComposeStackTrace(trace)
}

internal fun StringBuilder.appendStackTrace(trace: ComposeStackTrace) {
    if (trace.hasSourceInformation) {
        appendSourceInformationStackTrace(trace)
    } else {
        appendGroupKeyStackTrace(trace)
    }
}

@OptIn(ComposeToolingApi::class)
internal fun StringBuilder.appendSourceInformationStackTrace(trace: ComposeStackTrace) {
    var currentFunction: String? = null
    var currentFile: String? = null
    val lines = buildList {
        trace.frames.asReversed().fastForEach { frame ->
            val sourceInfo = frame.sourceInfo
            if (sourceInfo == null) return@fastForEach

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
                        @Suppress("ListIterator")
                        "<no offset ${frame.groupOffset} in ${lineNumbers.map { it.lineNumber }}>"
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
    lines.asReversed().fastForEach {
        append("\tat ")
        appendLine(it)
    }
}

internal fun ComposeStackTrace.filterInternalFramesByGroupKey(): List<ComposeStackTraceFrame> {
    // This is allocated every time we are creating a stack trace, but in most apps
    // it happens at most once, so it is better than allocating during <clinit>
    val knownKeys =
        intArrayOf(
            providerKey,
            compositionLocalMapKey,
            providerMapsKey,
            referenceKey,
            reuseKey,
            nodeKey,
            defaultsKey,
            movableContentKey,
            invocationKey,
        )

    var i = 0
    val fCount = frames.size
    val filteredFrames = mutableListOf<ComposeStackTraceFrame>()
    while (i < fCount) {
        val frame = frames[i++]
        if (frame.groupKey in knownKeys) continue
        if (frame.groupKey == rootKey) {
            if (i + 1 < fCount && frames[i + 1].groupKey == recomposerKey) {
                // We reached the root group
                break
            } else {
                // Remove the previous frame, it is a hash of parent context and has no reference
                // in the source code.
                filteredFrames.removeLastOrNull()
                continue
            }
        }
        filteredFrames += frame
    }
    return filteredFrames
}

internal fun StringBuilder.appendGroupKeyStackTrace(trace: ComposeStackTrace) {
    trace.filterInternalFramesByGroupKey().fastForEach {
        // at $$compose.m$<group-key>(SourceFile:1)
        append("\tat $\$compose.m$")
        append(it.groupKey)
        append("(SourceFile:1)")
        appendLine()
    }
}

private const val RuntimePackageHash = "9igjgp"

private const val IncludeDebugInfo = false
