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

package androidx.test.shell

import android.os.ParcelFileDescriptor
import androidx.test.shell.internal.ShellInstaller
import androidx.test.shell.internal.uiAutomation
import androidx.test.shell.internal.waitFor
import java.net.ConnectException
import java.net.Socket
import kotlin.random.Random

/**
 * A [ShellServer] starts a new long running process on device that is capable of establishing and
 * executing multiple concurrent [ShellProcess] with "/system/bin/sh". Each [ShellProcess]
 * represents an executing `sh` process. To interact with it, the 3 standard streams are exposed
 * (stdin, stdout, stderr) via [java.io.OutputStream]s and [java.io.InputStream]. At a high-level,
 * you can think of a [ShellServer] as a "factory" for [ShellProcess], which is useful to re-use for
 * performance, or to configure which ports [ShellServer] opens on-device.
 *
 * @see Shell.setShellServer
 */
public class ShellServer
private constructor(
    private val stdInSocketPort: Int,
    private val stdOutSocketPort: Int,
    private val stdErrSocketPort: Int,
    private val nativeLogs: Boolean,
    private val connectTimeoutMs: Long,
) : AutoCloseable {

    private var processPid: Int? = null

    public companion object {

        private const val LOCALHOST = "localhost"
        private const val PORT_START = 1025
        private const val PORT_END = 65532

        /**
         * Starts a new [ShellServer] with the given configuration.
         *
         * @param stdInSocketPort the port utilized for stdin. If not specified, this is chosen
         *   randomly in the interval between [PORT_START] and [PORT_END].
         * @param stdOutSocketPort the port utilized for stdout. If not specified, this is chosen
         *   randomly in the interval between [PORT_START] and [PORT_END].
         * @param stdErrSocketPort the port utilized for stderr. If not specified, this is chosen
         *   randomly in the interval between [PORT_START] and [PORT_END].
         * @param nativeLogs controls whether the native shell server should print logs.
         * @param connectTimeoutMs a timeout in millis to connect to the local shell server.
         * @return a started [ShellServer]
         */
        @JvmOverloads
        @JvmStatic
        public fun start(
            stdInSocketPort: Int = Random.nextInt(from = PORT_START, until = PORT_END),
            stdOutSocketPort: Int = Random.nextInt(from = PORT_START, until = PORT_END),
            stdErrSocketPort: Int = Random.nextInt(from = PORT_START, until = PORT_END),
            nativeLogs: Boolean = false,
            connectTimeoutMs: Long = 1000L,
        ): ShellServer =
            ShellServer(
                    stdInSocketPort = stdInSocketPort,
                    stdOutSocketPort = stdOutSocketPort,
                    stdErrSocketPort = stdErrSocketPort,
                    nativeLogs = nativeLogs,
                    connectTimeoutMs = connectTimeoutMs,
                )
                .also { it.start() }
    }

    private fun start() {
        if (processPid != null) return

        val cmd =
            listOf(
                    if (nativeLogs) "1" else "0",
                    stdInSocketPort,
                    stdOutSocketPort,
                    stdErrSocketPort,
                )
                .joinToString(" ")
                .let { "${ShellInstaller.shellExecutableFile.absolutePath} $it" }

        processPid =
            try {
                ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(cmd))
                    .use { iStream -> iStream.bufferedReader().readLine() }
                    .trim()
                    .toInt()
            } catch (e: NumberFormatException) {
                throw IllegalStateException(
                    "The shell server did not start correctly. " +
                        "Check logcat with tag `NativeShellServer` for details.",
                    e,
                )
            }
    }

    /**
     * Turns off the [ShellServer]. After the closing, no more [ShellProcess] can be created.
     *
     * @throws IllegalStateException if the shell server did not start correctly.
     */
    override fun close() {
        processPid?.let { pid ->

            // Immediately resets the pid so further calls to close do nothing.
            processPid = null

            val out =
                ParcelFileDescriptor.AutoCloseInputStream(
                        uiAutomation.executeShellCommand("kill -TERM $pid")
                    )
                    .bufferedReader()
                    .use { it.readText() }
            if (out.isNotBlank()) {
                throw IllegalStateException("Failed to stop ShellServer.")
            }
        }
    }

    /** Creates a new [ShellProcess]. */
    public fun newProcess(): ShellProcess {
        if (processPid == null) {
            throw IllegalStateException(
                "Cannot create a new ShellProcess if ShellServer is not running."
            )
        }
        var stdInSocket: Socket? = null
        waitFor(timeoutMs = connectTimeoutMs, onError = { /* Do nothing */ }) {
            try {
                stdInSocket = Socket(LOCALHOST, stdInSocketPort)
            } catch (_: ConnectException) {
                // When trying to connect to the server, it's possible the server is not yet
                // listening, in which case it'll throw a ConnectException.
            }
            stdInSocket != null
        }
        stdInSocket
            ?: throw IllegalStateException(
                "Can't connect to the ShellServer. " +
                    "[" +
                    " port stdin:$stdInSocketPort," +
                    " port stdout:$stdOutSocketPort," +
                    " stderr:$stdErrSocketPort " +
                    "]"
            )
        return ShellProcess(
            stdInSocket = stdInSocket,
            stdOutSocket = Socket(LOCALHOST, stdOutSocketPort),
            stdErrSocket = Socket(LOCALHOST, stdErrSocketPort),
        )
    }
}
