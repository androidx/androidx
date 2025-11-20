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

package androidx.test.shell.command

import android.graphics.Point
import android.util.Log
import androidx.annotation.IntRange
import androidx.test.shell.Shell
import androidx.test.shell.internal.TAG
import androidx.test.shell.internal.waitFor
import java.io.File
import kotlin.math.max

/** Allows running the screen record android utility to record the screen. */
public class RecorderCommands internal constructor(private val shell: Shell) {

    private val process by lazy { shell.process() }

    /**
     * Starts the recording.
     *
     * @param outputFile the output file where to write the recording.
     * @param screenSizeInPixel the size of the screen in pixel.
     * @param bitRateMb the bitrate of the recording in Mb.
     * @param timeLimitSeconds the number of seconds to record.
     * @return a running [Recording].
     */
    @JvmOverloads
    @SuppressWarnings("StreamFiles")
    public fun start(
        outputFile: File,
        screenSizeInPixel: Point? = null,
        @IntRange(from = 0) bitRateMb: Int = 0,
        @IntRange(from = 0) timeLimitSeconds: Long = 0,
    ): Recording {
        val cmd =
            listOfNotNull(
                    screenSizeInPixel?.let { "--size ${it.x}x${it.y}." },
                    if (bitRateMb > 0) "--bit-rate ${bitRateMb}M" else null,
                    if (timeLimitSeconds > 0) "--time-limit $timeLimitSeconds" else null,
                )
                .let { "screenrecord ${it.joinToString(" ")} ${outputFile.absolutePath}" }

        with(shell.command("echo pid:$$ ; exec $cmd")) {
            val processPid =
                stdOutStream
                    .bufferedReader()
                    .lineSequence()
                    .first { it.startsWith("pid:") }
                    .split("pid:")[1]
                    .toInt()

            // Ensure recording has started
            waitFor(onError = { throwWithCommandOutput() }) { process.isProcessAlive(processPid) }

            return Recording(
                pid = processPid,
                timeLimitSeconds = timeLimitSeconds,
                process = process,
                commandOutput = this,
            )
        }
    }
}

/**
 * A recording started with [RecorderCommands.start]. This interface allows awaits for completion,
 * if a time limit was specified or force stop the recording.
 */
public class Recording(
    private val process: ProcessCommands,
    private val commandOutput: Shell.CommandOutput,
    private val timeLimitSeconds: Long,
    /** The process id of the recording. */
    public val pid: Int,
) : AutoCloseable {

    /** Stops the current recording. */
    public override fun close(): Unit = process.killPid(pid, "TERM")

    /**
     * Blocks until the recording is complete or up to the [timeoutSeconds]. If a time limit was not
     * given this method throws an [IllegalStateException].
     *
     * @param timeoutSeconds the timeout in number of seconds.
     * @return whether the recording completed in the given [timeoutSeconds].
     */
    @JvmOverloads
    public fun await(timeoutSeconds: Long = max(timeLimitSeconds * 2, 10)): Boolean {
        if (timeLimitSeconds == 0L) {
            throw IllegalArgumentException(
                "Cannot await for screen record when no time limit is given."
            )
        }
        var complete = true
        waitFor(
            onError = {
                val cmdOutput = commandOutput.stdOutStdErrCommandOutput()
                cmdOutput.lines().forEach { Log.e(TAG, it) }
                Log.e(
                    TAG,
                    "Recorder did not end in the given timeout of $timeoutSeconds seconds." +
                        "\n$cmdOutput",
                )
                complete = false
            },
            timeoutMs = timeoutSeconds * 1_000L,
        ) {
            !isRunning()
        }
        if (commandOutput.stdErr.isNotBlank()) {
            commandOutput.throwWithCommandOutput()
        }
        return complete
    }

    /** Returns whether the recording is still running. */
    public fun isRunning(): Boolean = process.isProcessAlive(pid = pid)
}
