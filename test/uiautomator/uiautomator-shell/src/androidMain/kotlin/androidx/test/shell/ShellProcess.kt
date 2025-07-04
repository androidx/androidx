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

import android.annotation.SuppressLint
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

/**
 * Represents a process that is running sh as a shell user.
 *
 * Opposite to [Shell] this class doesn't handle the concept of command, rather it allows to read
 * and write in the process streams, i.e. stdin, stdout, stderr. For example, using [ShellProcess],
 * it's possible to write a file in shell space like `/data/local/tmp` launching `cp /dev/stdin
 * /data/local/tmp/myfile`:
 * ```kotlin
 * ShellProcess.create().use {
 *
 *   // Writing a file
 *   it.write("cp /dev/stdin /data/local/tmp/myfile")
 *   it.stdin.write(myByteByffer)
 *
 *   // Reading a file
 *   it.write("cat /data/local/tmp/somefile")
 *   assert(it.stdout.bufferedReader().readLine() == "This is a test")
 * }
 * ```
 *
 * For the vast majority of commands used for testing, [Shell] process is a better candidate and
 * automatically captures the full command output, as opposed to [ShellProcess] that doesn't
 * distinguish where a command ends and another starts.
 *
 * Note that this class is not thread safe due to internal state changes, although streams can be
 * read or written by another thread, as long as it's always the same. For example it's possible to
 * read [ShellProcess.stdOut] from another thread, as long as this doesn't change later.
 *
 * [ShellProcess] should be used only when access to streams is required, like in the above example.
 */
public class ShellProcess
internal constructor(
    private val stdInSocket: Socket,
    private val stdOutSocket: Socket,
    private val stdErrSocket: Socket,
) : AutoCloseable {

    // A data output stream to write in the process standard input.
    private val stdInOutputStream: DataOutputStream = DataOutputStream(stdInSocket.outputStream)

    // A wrapper around the stdout socket buffer to catch [SocketException] that may arise from the
    // process unexpected termination.
    private val stdOutSocketBuffer = SocketBuffer(socket = stdOutSocket)

    // A wrapper around the stderr socket buffer to catch [SocketException] that may arise from the
    // process unexpected termination.
    private val stdErrSocketBuffer = SocketBuffer(socket = stdErrSocket)

    /**
     * An [InputStream] for the process standard output. Note that this is backed by a tcp
     * connection to the native cli utility. If a large output is expected but it's not important to
     * capture it, a shell command may be launched piping in /dev/null.
     *
     * For example:
     * ```
     * cat large_file > /dev/null
     * ```
     */
    public val stdOut: InputStream
        get() = stdOutSocketBuffer.inputStream

    /**
     * An [InputStream] for the process standard error. Note that this is backed by a tcp connection
     * to the native cli utility. If a large output is expected but it's not important to capture
     * it, a shell command may be launched piping in /dev/null.
     *
     * For example:
     * ```
     * cat large_file 2> /dev/null
     * ```
     */
    public val stdErr: InputStream
        get() = stdErrSocketBuffer.inputStream

    /** An [OutputStream] for the process standard output. */
    public val stdIn: OutputStream
        get() = stdInOutputStream

    /** Returns whether the process is closed. */
    public fun isClosed(): Boolean = stdOutSocketBuffer.isClosed()

    /**
     * Writes a string in the process standard input.
     *
     * @param string a utf-8 string to write in the process stdin.
     */
    public fun writeLine(string: String) {
        stdInOutputStream.write("$string${System.lineSeparator()}".toByteArray(Charsets.UTF_8))
    }

    /**
     * Closes the current process. Internally this simply sends the shell process the exit command.
     * This means that the process is not immediately terminated, but gracefully shutdown finishing
     * prior commands execution. Once a shell process is closed, further calls to close don't have
     * any effect, as the [stdIn] will be closed after the first one.
     */
    override fun close() {
        if (!isClosed()) {
            try {
                writeLine("exit")

                // After closing this ShellProcess, the stdin socket is immediately closed as well.
                stdInSocket.close()

                // Spin up a clean up thread that periodically checks when the shell process
                // connections are complete. This is necessary because if the native process
                // terminates unexpectedly, the sockets can only be closed either calling socket
                // or if an I/O fails. SocketBuffer#isClosed performs a health check that, when
                // it fails, triggers the socket clean up.
                thread {
                    while (!stdOutSocketBuffer.isClosed() || !stdErrSocketBuffer.isClosed()) {
                        @SuppressLint("BanThreadSleep") sleep(200)
                    }
                }
            } catch (_: IOException) {
                // This may throw when there are multiple rapid calls to close() so that
                // `isClosed` returns false but right before writing in the stream, the socket
                // actually closes.
                // Nothing do do here anyway, the socket was already closing and the fact the
                // IOException is throws means the stream is no more accessible.
            }
        }
    }
}

/**
 * Wrapper around [InputStream] of a [Socket]. This is needed because if the underlying process
 * doesn't terminate correctly (for example if killed), the server side of the socket doesn't send
 * the FIN package to indicate the end of the stream. When that happens, if the client attends to
 * read but the connection doesn't exist anymore, a [SocketException] is thrown. An unexpected
 * termination of the child process is completely normal in this scenario, so we can just ignore the
 * [SocketException].
 */
private class SocketBuffer(private val socket: Socket) {

    private val socketInputStream by lazy { socket.inputStream }
    private var closed: Boolean = false

    fun markClosed() {
        if (closed) return
        socket.close()
        closed = true
    }

    fun isClosed(): Boolean {
        if (closed) return true
        return try {
            socket.sendUrgentData(0x00)
            false
        } catch (_: IOException) {
            markClosed()
            true
        }
    }

    val inputStream =
        object : InputStream() {

            override fun close() = markClosed()

            override fun read(): Int =
                try {
                    socketInputStream.read()
                } catch (_: SocketException) {
                    // Can happen if the native process ends unexpectedly.
                    markClosed()
                    -1
                }

            override fun read(b: ByteArray, offset: Int, len: Int): Int =
                try {
                    socketInputStream.read(b, offset, len)
                } catch (_: SocketException) {
                    // Can happen if the native process ends unexpectedly.
                    markClosed()
                    -1
                }

            override fun available(): Int =
                try {
                    socketInputStream.available()
                } catch (_: SocketException) {
                    // Can happen if the native process ends unexpectedly.
                    markClosed()
                    -1
                }
        }
}
