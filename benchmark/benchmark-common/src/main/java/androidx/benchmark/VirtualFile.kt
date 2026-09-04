/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.benchmark

import android.app.UiAutomation
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.trace
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Virtual file is a wrapper class around the concept of file. Depending on where the file is stored
 * it can be accessed either by the shell user or the app user. When the file is managed by the app
 * user this is managed by the subclass [UserFile], when the file is managed by shell user this is
 * managed by the subclass [ShellFile]. Note that shell user runs as user 0.
 *
 * When the selected android user is the primary user, that is also 0. In this case shell can access
 * both user storage and shell storage. When running in multiuser mode or on headless devices (where
 * the primary user is by default 10), shell cannot access the user storage and this class offers a
 * layer of abstraction to primarily copy and move files across the 2 different storages.
 *
 * Note that [Shell] is not used in this implementation for multiple reasons. [ShellFile] uses
 * [UiAutomation.executeShellCommandRw] that is only available from api 31. Additionally, since it
 * needs to interact with the instrumentation process to copy any amount of data, it uses streaming
 * api to read and write from files and allow large file size IO.
 *
 * Internally, [Shell] utilizes [ShellFile] to create scripts, including the ones for
 * [Shell.executeScriptSilent] for android multiuser.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class VirtualFile {

    public companion object {
        public fun fromPath(path: String): VirtualFile {
            val file = File(path)
            // It's only a `UserFile` iff we have the ability to write to the path,
            // or it's parentFile.
            return if (file.isAccessible()) {
                UserFile(path)
            } else {
                ShellFile(path)
            }
        }

        private tailrec fun File?.isAccessible(): Boolean {
            if (this == null) return false
            return if (this.exists()) {
                when {
                    this.canRead() && this.canWrite() -> true
                    else -> false
                }
            } else {
                this.parentFile.isAccessible()
            }
        }
    }

    public abstract val absolutePath: String
    public abstract val fileType: String

    public fun writeText(content: String): Unit = copyFrom(content.byteInputStream())

    public fun writeBytes(bytes: ByteArray): Unit = copyFrom(bytes.inputStream())

    public fun readText(): String = useInputStream { it.bufferedReader().readText() }

    public fun readBytes(): ByteArray = useInputStream { it.readBytes() }

    public abstract fun delete(): Boolean

    protected abstract fun <T> useInputStream(block: (InputStream) -> (T)): T

    protected abstract fun useOutputStream(block: (OutputStream) -> (Unit))

    public fun copyFrom(otherInputStream: InputStream): Unit = useOutputStream { o ->
        otherInputStream.copyTo(o)
    }

    public fun copyTo(otherOutputStream: OutputStream): Long = useInputStream { i ->
        i.copyTo(otherOutputStream)
    }

    public fun copyFrom(otherVirtualFile: VirtualFile) {
        if (this is ShellFile && otherVirtualFile is ShellFile) {
            // Optimization: reading and writing a shell file require 2 processes to run.
            // We don't need to do that if the file is copied in shell storage.
            executeCommand { "cp ${otherVirtualFile.absolutePath} $absolutePath" }
            return
        }
        otherVirtualFile.useInputStream { i -> useOutputStream { o -> i.copyTo(o) } }
    }

    public fun copyTo(otherVirtualFile: VirtualFile) {
        if (this is ShellFile && otherVirtualFile is ShellFile) {
            // Optimization: reading and writing a shell file require 2 processes to run.
            // We don't need to do that if the file is copied in shell storage.
            executeCommand { "cp $absolutePath ${otherVirtualFile.absolutePath}" }
            return
        }
        useInputStream { i -> otherVirtualFile.useOutputStream { o -> i.copyTo(o) } }
    }

    public fun moveTo(otherVirtualFile: VirtualFile) {
        if (this is ShellFile && otherVirtualFile is ShellFile) {
            // Optimization: reading and writing a shell file require 2 processes to run.
            // We don't need to do that if the file is moved in shell storage.
            executeCommand { "mv $absolutePath ${otherVirtualFile.absolutePath}" }
            return
        }
        copyTo(otherVirtualFile).also { this.delete() }
    }

    public abstract fun executeCommand(block: (String) -> String): String

    public fun md5sum(): String = executeCommand { "md5sum $it" }.substringBefore(" ")

    public fun chmod(args: String): String = executeCommand { "chmod $args $it" }

    public fun ls(): List<String> = executeCommand {
        "ls -1 $it"
    }
        .lines()
        .filter { it.isNotBlank() }

    public fun listFiles(): List<String> = executeCommand {
        "ls -1tp $it"
    }
        .lines()
        .filter { it.isNotBlank() && !it.endsWith("/") }

    public abstract fun mkdir()

    public abstract fun exists(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UserFile(private val file: File) : VirtualFile() {

    public companion object {
        public fun inOutputsDir(name: String): UserFile {
            val file = File(Outputs.dirUsableByAppAndShell, name)
            return UserFile(file)
        }
    }

    public constructor(path: String) : this(File(path))

    public override val absolutePath: String
        get() = file.absolutePath

    public override val fileType: String
        get() = "UserFile"

    public override fun <T> useInputStream(block: (InputStream) -> T): T =
        file.inputStream().use { block(it) }

    public override fun useOutputStream(block: (OutputStream) -> Unit): Unit =
        file.outputStream().use { block(it) }

    public override fun delete(): Boolean = file.deleteRecursively()

    public override fun executeCommand(block: (String) -> String): String {
        val cmd = block(absolutePath)
        return trace("UserFile#executeCommand $cmd".take(127)) {
            DataInputStream(Runtime.getRuntime().exec(cmd).inputStream)
                .bufferedReader()
                .use { it.readText() }
                .trim()
        }
    }

    public override fun mkdir() {
        file.mkdirs()
    }

    public override fun exists(): Boolean = file.exists()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ShellFile(public override val absolutePath: String) : VirtualFile() {

    override val fileType: String
        get() = "ShellFile"

    public companion object {
        private const val TAG = "ShellFile"
        private val uiAutomation: UiAutomation =
            InstrumentationRegistry.getInstrumentation().uiAutomation
        private val rootState by lazy { RootState.check() }

        public fun inTempDir(name: String): ShellFile = ShellFile("/data/local/tmp/", name)
    }

    public constructor(
        directory: String,
        filename: String,
    ) : this("${if (directory.endsWith("/")) directory else "$directory/"}$filename")

    override fun <T> useInputStream(block: (InputStream) -> T): T {
        // The following command prints the size of the file in bytes followed by the name.
        // If the file is a folder or the user doesn't have permission it throws an error.
        // We use the following command to ensure this is a valid read operation because
        // `UiAutomation#executeShellCommand` does not give any information if the cat process
        // fails.
        size()

        val cmd = rootState.maybeRootify("cat $absolutePath")
        val value =
            trace("ShellFile#useInputStream $cmd".take(127)) {
                val outDescriptor = uiAutomation.executeShellCommand(cmd)
                ParcelFileDescriptor.AutoCloseInputStream(outDescriptor).use(block)
            }
        return value
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun useOutputStream(block: (OutputStream) -> Unit) {
        val cmd = rootState.maybeRootify("cp /dev/stdin $absolutePath")
        var counterOs: CounterOutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            trace("ShellFile#useOutputStream $cmd".take(127)) {
                val (_, inDescriptor, errDescriptor) = uiAutomation.executeShellCommandRwe(cmd)
                ParcelFileDescriptor.AutoCloseOutputStream(inDescriptor).use {
                    counterOs = CounterOutputStream(it)
                    block(counterOs)
                }
                checkErr(errDescriptor)
            }
        } else {
            trace("ShellFile#useOutputStream $cmd".take(127)) {
                val (_, inDescriptor) = uiAutomation.executeShellCommandRw(cmd)
                ParcelFileDescriptor.AutoCloseOutputStream(inDescriptor).use {
                    counterOs = CounterOutputStream(it)
                    block(counterOs)
                }
            }
        }

        // The file might still be in cache. Sync will force the writing.
        // Without this, trying to read the file just written may result in file not found.
        sync()

        // Verify that the actual file size is the same that has been written
        val actualSize = size()
        val expectedSize = counterOs!!.writtenBytes
        if (actualSize != expectedSize) {
            throw IllegalStateException(
                "Expected $absolutePath size to be $actualSize but was $expectedSize"
            )
        }
    }

    override fun delete(): Boolean {
        val output = executeCommand { "rm -Rf $it" }
        sync()
        if (output.isBlank()) {
            return true
        } else {
            Log.d(TAG, "Error deleting `$absolutePath`: $output")
            return false
        }
    }

    override fun executeCommand(block: (String) -> String): String {
        val cmd = rootState.maybeRootify(block(absolutePath))
        val output =
            trace("ShellFile#executeCommand $cmd".take(127)) {
                uiAutomation.executeShellCommand(cmd).fullyReadInputStream()
            }
        return output.trim()
    }

    override fun mkdir() {
        executeCommand { "mkdir -p $it" }
    }

    override fun exists(): Boolean {
        return ls().isNotEmpty()
    }

    private fun sync() {
        executeCommand { "sync" }
    }

    private fun size() =
        try {
            executeCommand { "wc -c $it" }.split(" ").first().toLong()
        } catch (e: Throwable) {
            throw IllegalStateException("Cannot check size of $absolutePath.", e)
        }

    private fun checkErr(errDescriptor: ParcelFileDescriptor) {
        val err =
            ParcelFileDescriptor.AutoCloseInputStream(errDescriptor)
                .bufferedReader()
                .readText()
                .trim()
        if (err.isNotBlank()) {
            throw IllegalStateException("Error writing in $absolutePath: `$err`")
        }
    }
}

private class CounterOutputStream(private val output: OutputStream) : OutputStream() {
    private var _writtenBytes = 0L
    val writtenBytes: Long
        get() = _writtenBytes

    override fun write(b: Int) {
        _writtenBytes++
        output.write(b)
    }

    override fun close() {
        output.close()
    }

    override fun flush() {
        output.flush()
    }
}

private class RootState(val sessionRooted: Boolean, val suAvailable: Boolean) {
    companion object {
        private val uiAutomation: UiAutomation =
            InstrumentationRegistry.getInstrumentation().uiAutomation

        fun check() =
            RootState(
                sessionRooted =
                    DeviceInfo.isRooted &&
                        uiAutomation
                            .executeShellCommand("id")
                            .fullyReadInputStream()
                            .contains("uid=0(root)"),
                suAvailable =
                    DeviceInfo.isRooted &&
                        uiAutomation
                            .executeShellCommand("su root id")
                            .fullyReadInputStream()
                            .contains("uid=0(root)"),
            )
    }

    fun maybeRootify(cmd: String) =
        trace("buildCommand $cmd".take(127)) {
            if (!sessionRooted && suAvailable) {
                "su root $cmd"
            } else {
                cmd
            }
        }
}
