/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.tracing.perfetto

import android.content.Context
import android.os.Build
import android.util.JsonWriter
import androidx.annotation.RequiresApi
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_ERROR_MESSAGE
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_EXIT_CODE
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_REQUIRED_VERSION
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_BINARY_MISSING
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_SUCCESS
import androidx.tracing.perfetto.TracingReceiver.EnableTracingResultCode
import androidx.tracing.perfetto.jni.PerfettoNative
import androidx.tracing.perfetto.security.IncorrectChecksumException
import androidx.tracing.perfetto.security.SafeLibLoader
import androidx.tracing.perfetto.security.UnapprovedLocationException
import java.io.File
import java.io.StringWriter
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

object Tracing {
    /**
     * Indicates whether the tracing library has been loaded and the app registered with
     * Perfetto SDK.
     */
    // Note: some of class' code relies on the field never changing from true -> false,
    // which is realistic (at the time of writing this, we are unable to unload the library and
    // unregister the app with Perfetto).
    var isEnabled: Boolean = false
        private set

    /**
     * Ensures that we enable tracing (load the tracing library and register with Perfetto) only
     * once.
     *
     * Note: not intended for synchronization during tracing as not to impact performance.
     */
    private val enableTracingLock = ReentrantReadWriteLock()

    @RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
    internal fun enable() = enable(null)

    @RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
    internal fun enable(file: File, context: Context) = enable(file to context)

    @RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
    private fun enable(descriptor: Pair<File, Context>?): EnableTracingResponse {
        enableTracingLock.readLock().withLock {
            if (isEnabled) return EnableTracingResponse(RESULT_CODE_ALREADY_ENABLED)
        }

        enableTracingLock.writeLock().withLock {
            return enableImpl(descriptor)
        }
    }

    /** Calling thread must obtain a write lock on [enableTracingLock] before calling this method */
    @RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
    private fun enableImpl(descriptor: Pair<File, Context>?): EnableTracingResponse {
        if (!enableTracingLock.isWriteLockedByCurrentThread) throw RuntimeException()

        if (isEnabled) return EnableTracingResponse(RESULT_CODE_ALREADY_ENABLED)

        // Load library
        try {
            when (descriptor) {
                null -> PerfettoNative.loadLib()
                else -> descriptor.let { (file, context) ->
                    PerfettoNative.loadLib(file, SafeLibLoader(context))
                }
            }
        } catch (t: Throwable) {
            return when (t) {
                is IncorrectChecksumException, is UnapprovedLocationException ->
                    EnableTracingResponse(
                        exitCode = RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR,
                        errorMessage = errorMessage(t)
                    )
                is UnsatisfiedLinkError ->
                    EnableTracingResponse(
                        exitCode = RESULT_CODE_ERROR_BINARY_MISSING,
                        errorMessage = errorMessage(t)
                    )
                is Exception ->
                    EnableTracingResponse(
                        exitCode = RESULT_CODE_ERROR_OTHER,
                        errorMessage = errorMessage(t)
                    )
                else -> throw t
            }
        }

        // Verify binary/java version match
        val nativeVersion = PerfettoNative.nativeVersion()
        val javaVersion = PerfettoNative.Metadata.version
        if (nativeVersion != javaVersion) {
            return EnableTracingResponse(
                exitCode = RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH,
                errorMessage =
                "Binary and Java version mismatch. " +
                    "Binary: $nativeVersion. " +
                    "Java: $javaVersion",
            )
        }

        // Register as a Perfetto SDK data-source
        try {
            PerfettoNative.nativeRegisterWithPerfetto()
        } catch (e: Exception) {
            return EnableTracingResponse(RESULT_CODE_ERROR_OTHER, errorMessage = errorMessage(e))
        }

        isEnabled = true
        return EnableTracingResponse(RESULT_CODE_SUCCESS)
    }

    // TODO: remove and replace with an observer wired into Perfetto
    internal fun flushEvents() {
        if (isEnabled) {
            PerfettoNative.nativeFlushEvents()
        }
    }

    /** Writes a trace message to indicate that a given section of code has begun. */
    fun traceEventStart(key: Int, traceInfo: String) {
        if (isEnabled) {
            PerfettoNative.nativeTraceEventBegin(key, traceInfo)
        }
    }

    /** Writes a trace message to indicate that a given section of code has ended. */
    fun traceEventEnd() {
        if (isEnabled) PerfettoNative.nativeTraceEventEnd()
    }

    internal data class EnableTracingResponse(
        @EnableTracingResultCode val exitCode: Int,
        val errorMessage: String? = null
    ) {
        internal fun toJsonString(): String {
            val output = StringWriter()

            JsonWriter(output).use {
                it.beginObject()

                it.name(KEY_EXIT_CODE)
                it.value(exitCode)

                it.name(KEY_REQUIRED_VERSION)
                it.value(PerfettoNative.Metadata.version)

                errorMessage?.let { msg ->
                    it.name(KEY_ERROR_MESSAGE)
                    it.value(msg)
                }

                it.endObject()
            }

            return output.toString()
        }
    }

    internal fun errorMessage(t: Throwable): String =
        t.javaClass.name + if (t.message != null) ": ${t.message}" else ""
}
