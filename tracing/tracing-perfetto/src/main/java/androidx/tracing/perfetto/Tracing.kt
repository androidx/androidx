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

import android.util.JsonWriter
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_ERROR_MESSAGE
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_EXIT_CODE
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_REQUIRED_VERSION
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_BINARY_MISSING
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_SUCCESS
import androidx.tracing.perfetto.TracingReceiver.EnableTracingResultCode
import androidx.tracing.perfetto.jni.PerfettoNative
import java.io.StringWriter
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

object Tracing {
    /**
     * Indicates that the tracing library has been loaded and that the app was registered with
     * Perfetto as a data source.
     *
     * Note: some of class' code relies on the field ever changing from true -> false and not in
     * the other direction, which is realistic (we cannot unload the library or unregister with
     * Perfetto at the time of writing).
     */
    var isEnabled: Boolean = false
        private set

    /**
     * Ensures that we enable tracing (load the tracing library and register with Perfetto) only
     * once.
     *
     * Note: not intended for synchronization during tracing as not to impact performance.
     */
    private val enableTracingLock = ReentrantReadWriteLock()

    fun enable(path: String? = null): EnableTracingResponse {
        enableTracingLock.readLock().withLock {
            if (isEnabled) return EnableTracingResponse(RESULT_CODE_ALREADY_ENABLED)
        }

        enableTracingLock.writeLock().withLock {
            return enableImpl(path)
        }
    }

    /** Calling thread must obtain a write lock on [enableTracingLock] before calling this method */
    private fun enableImpl(path: String?): EnableTracingResponse {
        if (!enableTracingLock.isWriteLockedByCurrentThread) throw RuntimeException()

        if (isEnabled) return EnableTracingResponse(RESULT_CODE_ALREADY_ENABLED)

        // Load library
        try {
            PerfettoNative.loadLib(path)
        } catch (t: Throwable) {
            when (t) {
                is UnsatisfiedLinkError, is Exception -> return EnableTracingResponse(
                    exitCode = RESULT_CODE_ERROR_BINARY_MISSING,
                    errorMessage = t.toErrorMessage()
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
            return EnableTracingResponse(RESULT_CODE_ERROR_OTHER, errorMessage = e.toErrorMessage())
        }

        isEnabled = true
        return EnableTracingResponse(RESULT_CODE_SUCCESS)
    }

    // TODO: remove and replace with an observer wired into Perfetto
    fun flushEvents() {
        if (isEnabled) {
            PerfettoNative.nativeFlushEvents()
        }
    }

    fun traceEventStart(key: Int, traceInfo: String) {
        if (isEnabled) {
            PerfettoNative.nativeTraceEventBegin(key, traceInfo)
        }
    }

    fun traceEventEnd() {
        if (isEnabled) PerfettoNative.nativeTraceEventEnd()
    }

    data class EnableTracingResponse(
        @EnableTracingResultCode val exitCode: Int,
        val errorMessage: String? = null
    ) {
        fun toJsonString(): String {
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
}

internal fun Throwable.toErrorMessage(): String =
    javaClass.name + if (message != null) ": $message" else ""
