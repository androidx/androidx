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

package androidx.benchmark.perfetto

import android.os.Build
import android.util.JsonReader
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.benchmark.Shell
import androidx.tracing.perfetto.TracingReceiver
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_ERROR_MESSAGE
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_EXIT_CODE
import androidx.tracing.perfetto.TracingReceiver.Companion.KEY_REQUIRED_VERSION
import java.io.StringReader

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PerfettoHandshake(private val targetPackage: String) {
    private companion object {
        val receiverName: String = TracingReceiver::class.java.name
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun requestEnable(providedBinaryPath: String? = null): Response {
        val action = TracingReceiver.ACTION_ENABLE_TRACING
        val pathExtra = providedBinaryPath?.let { """--es ${TracingReceiver.KEY_PATH} $it""" } ?: ""
        val command = "am broadcast -a $action $pathExtra $targetPackage/$receiverName"
        return Response.parse(Shell.executeCommand(command))
    }

    data class Response constructor(
        val exitCode: Int?,
        val requiredVersion: String?,
        val errorMessage: String?
    ) {
        companion object {
            @DoNotInline // System.lineSeparator() is API level 19
            fun parse(rawResponse: String): Response {
                val line = rawResponse
                    .split(Regex("\r?\n"))
                    .firstOrNull { it.contains("Broadcast completed: result=") }
                    ?: throw IllegalArgumentException("Cannot parse: $rawResponse")

                val matchResult =
                    Regex("Broadcast completed: (result=.*?)(, data=\".*?\")?(, extras: .*)?")
                        .matchEntire(line)
                        ?: throw IllegalArgumentException("Cannot parse: $rawResponse")

                val broadcastResponseCode = matchResult
                    .groups[1]
                    ?.value
                    ?.substringAfter("result=")
                    ?.toIntOrNull()

                val dataString = matchResult
                    .groups
                    .firstOrNull { it?.value?.startsWith(", data=") ?: false }
                    ?.value
                    ?.substringAfter(", data=\"")
                    ?.dropLast(1)
                    ?: throw IllegalArgumentException("Cannot parse: $rawResponse. " +
                        "Unable to detect 'data=' section."
                    )

                JsonReader(StringReader(dataString)).use { reader ->
                    reader.beginObject()

                    var requiredVersion: String? = null
                    var errorMessage: String? = null
                    var exitCode: Int? = null

                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            KEY_EXIT_CODE -> exitCode = reader.nextInt()
                            KEY_REQUIRED_VERSION -> requiredVersion = reader.nextString()
                            KEY_ERROR_MESSAGE -> errorMessage = reader.nextString()
                            else -> {}
                        }
                    }

                    if (broadcastResponseCode != exitCode) {
                        throw IllegalStateException("Cannot parse: $rawResponse. Exit code " +
                            "not matching broadcast exit code."
                        )
                    }

                    reader.endObject()
                    return Response(exitCode, requiredVersion, errorMessage)
                }
            }
        }
    }
}