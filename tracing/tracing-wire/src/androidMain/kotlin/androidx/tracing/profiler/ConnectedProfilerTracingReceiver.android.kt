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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.system.Os
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.startup.AppInitializer
import androidx.tracing.Trace.TAG
import androidx.tracing.profiler.ConnectedProfilerTracing.disableTracing
import androidx.tracing.profiler.ConnectedProfilerTracing.enableTracing
import androidx.tracing.wire.getOrCreateTracesDirectory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Actions
internal const val ACTION_START = "androidx.tracing.profiler.action.START"
internal const val ACTION_FLUSH_TRACES_GET_PATH =
    "androidx.tracing.profiler.action.FLUSH_TRACES_GET_PATH"
internal const val ACTION_STOP = "androidx.tracing.profiler.action.STOP"

// Result codes
internal const val RESULT_CODE_DELAYED_TRACE_DRIVER = -2
internal const val RESULT_CODE_FAILED = -1
internal const val RESULT_CODE_SUCCESS = 1
internal const val RESULT_CODE_FLUSH_COMPLETED = 2

/**
 * The [BroadcastReceiver] that can be used flush in-process traces. Tracing 2.0 and TracingDriver
 * record in-process traces. To get these off of the device, or make them available to testing
 * harnesses such as `androidx.benchmark`, this receiver offers start/stop/flush to file actions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConnectedProfilerTracingReceiver : BroadcastReceiver() {

    // The scope used for all work done by the receiver.
    // Dispatchers.Default is okay, given the actual flush happens on Dispatchers.IO
    private val scope = CoroutineScope(context = Dispatchers.Default)

    override fun onReceive(context: Context?, intent: Intent?) {
        val context = context ?: return
        val action = intent?.action ?: return

        when (action) {
            ACTION_START -> start(context)
            ACTION_STOP -> stop(context)
            ACTION_FLUSH_TRACES_GET_PATH -> flushTraces(context)
            else -> {
                Log.w(TAG, "Unknown intent action: $action. Ignoring.")
            }
        }
    }

    internal fun start(context: Context) {
        Log.w(TAG, "Starting an in-process tracing session.")
        val result = goAsync()
        scope.launch {
            try {
                clearInProcessTraces(context)
                enableTracing(context)
                result.resultCode = RESULT_CODE_SUCCESS
            } finally {
                result.finish()
            }
        }
    }

    internal fun clearInProcessTraces(context: Context) {
        val packageName = context.packageName
        val tracesDirectory = context.getOrCreateTracesDirectory()
        val traceFiles = tracesDirectory.listFiles() ?: return
        val paths = packageRelativeOpenPaths(packageName)
        traceFiles.forEach { file ->
            val path = relativePath(file.absolutePath, packageName)
            if (path !in paths) {
                file.delete()
            }
        }
    }

    internal fun stop(context: Context) {
        Log.w(TAG, "Stopping an in-process tracing session.")
        disableTracing(context)
        resultCode = RESULT_CODE_SUCCESS
    }

    internal fun flushTraces(context: Context) {
        // Flushing traces should not take too long. By using goAsync(), we effectively have
        // 10 seconds to respond on the main thread before we risk a background ANR.
        val result = goAsync()
        scope.launch {
            try {
                val initializer = AppInitializer.getInstance(context)
                val klass = ConnectedProfilerTracingInitializer::class.java
                val isEager = initializer.isEagerlyInitialized(klass)
                val driver = initializer.initializeComponent(klass)
                driver.flush()
                // Copy files from the traces directory to a directory accessible by the
                // app and shell so we can pull and merge traces.
                val tracesDirectory = copyTraceFiles(context)
                if (tracesDirectory == null) {
                    result.resultCode = RESULT_CODE_FAILED
                } else {
                    result.resultCode =
                        if (isEager) RESULT_CODE_FLUSH_COMPLETED
                        else RESULT_CODE_DELAYED_TRACE_DRIVER
                    // The path to find traces in.
                    result.resultData = tracesDirectory.absolutePath
                }
                Log.d(TAG, "Flushed traces.")
            } finally {
                result.finish()
            }
        }
    }

    internal fun copyTraceFiles(context: Context): File? {
        val tracesDirectory = context.getOrCreateTracesDirectory()
        val parent = context.dirUsableByAppAndShell() ?: return null
        // The path we copy trace files to.
        val outputDirectory = File(parent, "perfetto_traces")
        // Create the directory if it does not already exist.
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        // Delete existing trace files.
        outputDirectory.listFiles { file -> file.isFile }?.forEach { file -> file.delete() }
        val files = tracesDirectory.listFiles { file -> file.isFile }
        if (files != null && files.isNotEmpty()) {
            files.forEach { file ->
                file.copyTo(File(outputDirectory, file.name), overwrite = true)
            }
        }
        return outputDirectory
    }

    // This is effectively a copy of `Outputs.dirUsableByAppAndShell`.
    internal fun Context.dirUsableByAppAndShell(): File? {
        // On Android Q+ we are using the media directory because that is
        // the directory that the shell has access to. Context: b/181601156
        // Additionally, Benchmarks append user space traces to the ones produced
        // by the Macro Benchmark run; and that is a lot simpler to do if we use the
        // Media directory. (b/216588251)
        @Suppress("DEPRECATION")
        return externalMediaDirs.firstOrNull {
            Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED
        }
    }

    internal companion object {
        internal fun packageRelativeOpenPaths(packageName: String): Set<String> {
            // This mechanism of looking for open files does not work all that well when referring
            // to files using the SAF. They show up as anonymous file handles.
            // However, our use case is pretty safe, given the app is writing a file in
            // its internal data directory.
            val open = File("/proc/self/fd")
            val files = open.listFiles() ?: return emptySet()
            val paths = mutableSetOf<String>()
            for (file in files) {
                val originalPath = file.absolutePath
                val link = runCatching { Os.readlink(originalPath) }
                val path = link.getOrElse { originalPath }
                val packageRelativePath = relativePath(path, packageName)
                paths += packageRelativePath
            }
            return paths
        }

        internal fun relativePath(path: String, packageName: String): String {
            // If /data/data/../packageName/<path>
            // We only are about the <path> part, given we are only looking to clear
            // files that are not open and **owned** by the package.

            // This step is important because you will end up comparing things like:
            // /data/data/0/<packageName> and /data/data/packageName which effectively refer
            // to the same package.
            return path.substringAfter(delimiter = packageName)
        }
    }
}
