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

package androidx.test.shell.internal

import android.annotation.SuppressLint
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedReader
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeoutException

internal const val TAG = "Shell"

internal val instrumentation = InstrumentationRegistry.getInstrumentation()
internal val uiAutomation = instrumentation.uiAutomation
/**
 * Deprecation is suppressed because this is the folder Android Studio uses to read instrumentation
 * test results. Note that [android.content.Context.externalMediaDirs] is deprecated because in an
 * app you should use [android.provider.MediaStore]. Since we want to access the files directly we
 * instead use [android.content.Context.externalMediaDirs].
 */
@Suppress("DEPRECATION")
internal val instrumentationPackageMediaDir =
    instrumentation.targetContext.externalMediaDirs.firstOrNull {
        Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED
    } ?: throw IllegalStateException("Cannot get external storage because it's not mounted")

internal fun command(
    cmd: String,
    isFailureBlock: (String) -> (Boolean) = { it.isNotBlank() },
    block: (BufferedReader) -> (String) = { it.readText() },
): String {
    Log.d(TAG, "Executing: $cmd")
    return uiAutomation
        .executeShellCommand(cmd)
        .let { ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use(block) }
        .also { if (isFailureBlock(it)) throw IllegalStateException(it) }
}

internal fun randomHexString(len: Int) =
    ByteBuffer.allocate(Long.SIZE_BYTES)
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(System.nanoTime())
        .array()
        .take(len)
        .joinToString("") { "%02x".format(it) }

internal fun waitFor(
    timeoutMs: Long = 10000L,
    poolIntervalMs: Long = 1000L,
    onError: () -> (Unit) = { throw TimeoutException("Condition was not met.") },
    condition: () -> (Boolean),
) {
    val start = System.nanoTime()
    while (true) {
        if ((System.nanoTime() - start) / 1_000_000 >= timeoutMs) {
            onError()
            return
        }
        if (condition()) {
            break
        }
        @SuppressLint("BanThreadSleep") sleep(poolIntervalMs)
    }
}
