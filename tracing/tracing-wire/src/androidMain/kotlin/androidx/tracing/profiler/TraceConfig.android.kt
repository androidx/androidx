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

package androidx.tracing.profiler

import android.content.Context
import android.util.Log
import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.tracing.Trace.TAG
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.buffer
import okio.source
import okio.utf8Size

// The trace configuration version.
private const val TRACE_CONFIGURATION_VERSION = 1

// The name of the file that contains the configuration.
private const val TRACE_CONFIGURATION = "androidx.tracing_wire.config"

internal fun File.saveConfig(categories: Array<String>) {
    val config = this
    runBlocking(context = Dispatchers.IO) {
        try {
            if (!config.exists()) config.createNewFile()
            val buffer = Buffer()
            buffer.use {
                buffer.writeInt(TRACE_CONFIGURATION_VERSION)
                buffer.writeInt(categories.size)
                categories.forEach { category ->
                    buffer.writeLong(category.utf8Size())
                    buffer.writeUtf8(category)
                }
                config.outputStream().buffered().use {
                    buffer.writeTo(it)
                }
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "Unable to save configuration", throwable)
            runCatching { config.delete() }
        }
    }
}

internal fun File.loadConfig(): MutableScatterSet<String> {
    val config = this
    return runBlocking(context = Dispatchers.IO) {
        if (!config.exists() || !config.canRead()) {
            mutableScatterSetOf()
        } else {
            val result = runCatching {
                val buffer = config.source().buffer()
                buffer.use {
                    /* version */
                    buffer.readInt()
                    val size = buffer.readInt()
                    val categories = mutableScatterSetOf<String>()
                    repeat(times = size) {
                        val byteCount = buffer.readLong()
                        categories += buffer.readUtf8(byteCount)
                    }
                    categories
                }
            }
            result.getOrElse {
                Log.e(TAG, "Unable to load configuration", it)
                runCatching { config.delete() }
                mutableScatterSetOf()
            }
        }
    }
}

internal fun Context.traceConfiguration(): File {
    return File(noBackupFilesDir, TRACE_CONFIGURATION)
}
