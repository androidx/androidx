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
import androidx.annotation.RequiresApi
import androidx.tracing.perfetto.internal.handshake.protocol.EnableTracingResponse
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseExitCodes.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseExitCodes.RESULT_CODE_ERROR_BINARY_MISSING
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseExitCodes.RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseExitCodes.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseExitCodes.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseExitCodes.RESULT_CODE_SUCCESS
import androidx.tracing.perfetto.jni.PerfettoNative
import androidx.tracing.perfetto.security.IncorrectChecksumException
import androidx.tracing.perfetto.security.SafeLibLoader
import java.io.File
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
                is IncorrectChecksumException ->
                    EnableTracingResponse(RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR, t)
                is UnsatisfiedLinkError ->
                    EnableTracingResponse(RESULT_CODE_ERROR_BINARY_MISSING, t)
                is Exception ->
                    EnableTracingResponse(RESULT_CODE_ERROR_OTHER, t)
                else -> throw t
            }
        }

        // Verify binary/java version match
        val nativeVersion = PerfettoNative.nativeVersion()
        val javaVersion = PerfettoNative.Metadata.version
        if (nativeVersion != javaVersion) {
            return EnableTracingResponse(
                RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH,
                "Binary and Java version mismatch. Binary: $nativeVersion. Java: $javaVersion"
            )
        }

        // Register as a Perfetto SDK data-source
        try {
            PerfettoNative.nativeRegisterWithPerfetto()
        } catch (e: Exception) {
            return EnableTracingResponse(RESULT_CODE_ERROR_OTHER, e)
        }

        isEnabled = true
        return EnableTracingResponse(RESULT_CODE_SUCCESS)
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

    private fun errorMessage(t: Throwable): String = t.run {
        javaClass.name + if (message != null) ": $message" else ""
    }

    internal fun EnableTracingResponse(exitCode: Int, message: String? = null) =
        EnableTracingResponse(exitCode, PerfettoNative.Metadata.version, message)

    internal fun EnableTracingResponse(exitCode: Int, exception: Throwable) =
        EnableTracingResponse(exitCode, errorMessage(exception))
}
