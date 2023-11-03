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
import androidx.tracing.perfetto.internal.handshake.protocol.Response
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseResultCodes.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_BINARY_MISSING
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseResultCodes.RESULT_CODE_SUCCESS
import androidx.tracing.perfetto.jni.PerfettoNative
import androidx.tracing.perfetto.security.IncorrectChecksumException
import androidx.tracing.perfetto.security.SafeLibLoader
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/** Allows for emitting trace events using Perfetto SDK. */
object PerfettoSdkTrace {
    /**
     * Checks whether the tracing library has been loaded and the app has been registered with
     * Perfetto SDK tracing server. This is useful to avoid intermediate string creation for trace
     * sections that require formatting. It is not necessary to guard all Trace method calls as they
     * internally already check this. However it is recommended to use this to prevent creating any
     * temporary objects that would then be passed to those methods to reduce runtime cost when
     * tracing isn't enabled.
     *
     * @return true if tracing is currently enabled, false otherwise
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
    private fun enable(descriptor: Pair<File, Context>?): Response {
        enableTracingLock.readLock().withLock {
            if (isEnabled) return Response(RESULT_CODE_ALREADY_ENABLED)
        }

        enableTracingLock.writeLock().withLock {
            return enableImpl(descriptor)
        }
    }

    /** Calling thread must obtain a write lock on [enableTracingLock] before calling this method */
    @RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
    private fun enableImpl(descriptor: Pair<File, Context>?): Response {
        if (!enableTracingLock.isWriteLockedByCurrentThread) throw RuntimeException()

        if (isEnabled) return Response(RESULT_CODE_ALREADY_ENABLED)

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
                    Response(RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR, t)
                is UnsatisfiedLinkError ->
                    Response(RESULT_CODE_ERROR_BINARY_MISSING, t)
                is Exception ->
                    Response(RESULT_CODE_ERROR_OTHER, t)
                else -> throw t
            }
        }

        // Verify binary/java version match
        val nativeVersion = PerfettoNative.nativeVersion()
        val javaVersion = PerfettoNative.Metadata.version
        if (nativeVersion != javaVersion) {
            return Response(
                RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH,
                "Binary and Java version mismatch. Binary: $nativeVersion. Java: $javaVersion"
            )
        }

        // Register as a Perfetto SDK data-source
        try {
            PerfettoNative.nativeRegisterWithPerfetto()
        } catch (e: Exception) {
            return Response(RESULT_CODE_ERROR_OTHER, e)
        }

        isEnabled = true
        return Response(RESULT_CODE_SUCCESS)
    }

    /**
     * Writes a trace message to indicate that a given section of code has begun. This call must
     * be followed by a corresponding call to [endSection] on the same thread.
     *
     * @param sectionName The name of the code section to appear in the trace.
     */
    fun beginSection(sectionName: String) {
        if (isEnabled) {
            // Note: key is not currently used, so passing 0 for now
            PerfettoNative.nativeTraceEventBegin(key = 0, traceInfo = sectionName)
        }
    }

    /**
     * Writes a trace message to indicate that a given section of code has ended. This call must
     * be preceded by a corresponding call to [beginSection]. Calling this method
     * will mark the end of the most recently begun section of code, so care must be taken to
     * ensure that [beginSection] / [endSection] pairs are properly nested and called from the same
     * thread.
     */
    fun endSection() {
        if (isEnabled) PerfettoNative.nativeTraceEventEnd()
    }

    private fun errorMessage(t: Throwable): String = t.run {
        javaClass.name + if (message != null) ": $message" else ""
    }

    internal fun Response(resultCode: Int, message: String? = null) =
        Response(resultCode, PerfettoNative.Metadata.version, message)

    internal fun Response(resultCode: Int, exception: Throwable) =
        Response(resultCode, errorMessage(exception))
}
