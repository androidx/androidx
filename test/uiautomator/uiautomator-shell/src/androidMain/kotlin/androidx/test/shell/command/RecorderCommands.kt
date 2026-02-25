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

import android.util.Log
import androidx.annotation.IntRange
import androidx.test.shell.Shell
import androidx.test.shell.internal.TAG
import androidx.test.shell.internal.waitFor
import java.io.File

/** Allows running the screen record android utility to record the screen. */
public class RecorderCommands internal constructor(private val shell: Shell) {

    private val process by lazy { shell.process }

    /**
     * Starts the recording.
     *
     * @param outputFile the output file where to write the recording.
     * @param width the width of the screen in pixels.
     * @param height the height of the screen in pixels.
     * @param bitRateMb the bitrate of the recording in Mb/s.
     * @return a running [Recording].
     *
     * When [width], and [height] are unspecified, the recorder uses the display's native resolution
     * (if supported) and 1280x720` when not. For more information, you can read the documentation
     * [here](https://d.android.com/tools/adb#screenrecord).
     */
    @SuppressWarnings("StreamFiles")
    // We are suppressing this because, when specifying a stream size the developer has to specify
    // both the width and the height otherwise, the parameter ends up getting ignored entirely.
    @Suppress("MissingJvmstatic")
    public fun start(
        outputFile: File,
        width: Int = 0,
        height: Int = 0,
        @IntRange(from = 0) bitRateMb: Int = 0,
    ): Recording {
        val size =
            if (width > 0 && height > 0) {
                "--size ${width}x$height"
            } else {
                null
            }
        val cmd =
            listOfNotNull(size, if (bitRateMb > 0) "--bit-rate ${bitRateMb}M" else null).let {
                "screenrecord ${it.joinToString(" ")} ${outputFile.absolutePath}"
            }

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

            return Recording(pid = processPid, process = process, commandOutput = this)
        }
    }
}

/**
 * A recording started with [RecorderCommands.start]. This interface allows awaits for completion,
 * if a time limit was specified or force stop the recording.
 */
public class Recording
internal constructor(
    private val process: ProcessCommands,
    private val commandOutput: Shell.CommandOutput,
    /** The process id of the recording. */
    private val pid: Int,
) : AutoCloseable {

    /**
     * Stops the current recording.
     *
     * Waits for up to `10 seconds` for the `recorder` process to stop.
     */
    public override fun close() {
        if (!isRunning()) {
            process.killPid(pid, "TERM")
            await()
        }
    }

    /** Blocks until the process that does the [androidx.test.shell.command.Recording] is dead. */
    private fun await() {
        waitFor(
            onError = {
                val cmdOutput = commandOutput.stdOutStdErrCommandOutput()
                cmdOutput.lines().forEach { Log.e(TAG, it) }
                Log.e(TAG, "Recorder did not end: \n$commandOutput")
            }
        ) {
            !isRunning()
        }
        if (commandOutput.stdErr.isNotBlank()) {
            commandOutput.throwWithCommandOutput()
        }
    }

    /** Returns whether the recording is still running. */
    public fun isRunning(): Boolean = process.isProcessAlive(pid = pid)
}
