/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

import androidx.benchmark.Outputs
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.io.InputStream

internal data class ShellOutput(val stdout: String, val stderr: String)

/**
 * Provides shell scripting functionality, as well as stdin/stderr capabilities (for the first/last
 * command, respectively)
 *
 * A better way to implement stdin/stderr may be to have an additional wrapper script when
 * stdin/stderr is required, so that all stderr can be captured (instead of redirecting the
 * last command), and stdin can be read by other commands in the script (instead of just the 1st).
 */
private fun UiDevice.executeShellScript(
    script: String,
    stdin: String?,
    includeStderr: Boolean
): Pair<String, String?> {
    // dirUsableByAppAndShell is writable, but we can't execute there (as of Q),
    // so we copy to /data/local/tmp
    val externalDir = Outputs.dirUsableByAppAndShell
    val writableScriptFile = File.createTempFile("temporaryScript", ".sh", externalDir)
    val runnableScriptPath = "/data/local/tmp/" + writableScriptFile.name

    // only create/read/delete stdin/stderr files if they are needed
    val stdinFile = stdin?.run {
        File.createTempFile("temporaryStdin", null, externalDir)
    }
    val stderrPath = if (includeStderr) {
        // we use a modified runnableScriptPath (as opposed to externalDir) because some shell
        // commands fail to redirect stderr to externalDir (notably, `am start`).
        // This also means we need to `cat` the file to read it, and `rm` to remove it.
        runnableScriptPath + "_stderr"
    } else {
        null
    }

    try {
        var scriptText: String = script
        if (stdinFile != null) {
            stdinFile.writeText(stdin)
            scriptText = "cat ${stdinFile.absolutePath} | $scriptText"
        }
        if (stderrPath != null) {
            scriptText = "$scriptText 2> $stderrPath"
        }
        writableScriptFile.writeText(scriptText)

        // Note: we don't check for return values from the below, since shell based file
        // permission errors generally crash our process.
        executeShellCommand("cp ${writableScriptFile.absolutePath} $runnableScriptPath")
        executeShellCommand("chmod +x $runnableScriptPath")

        val stdout = executeShellCommand(runnableScriptPath)
        val stderr = stderrPath?.run { executeShellCommand("cat $stderrPath") }

        return Pair(stdout, stderr)
    } finally {
        stdinFile?.delete()
        stderrPath?.run {
            executeShellCommand("rm $stderrPath")
        }
        writableScriptFile.delete()
        executeShellCommand("rm $runnableScriptPath")
    }
}

/**
 * Convenience wrapper around [UiDevice.executeShellCommand()] which enables redirects, piping, and
 * all other shell script functionality, and captures stderr of last command.
 *
 * Unlike [UiDevice.executeShellCommand()], this method supports arbitrary multi-line shell
 * expressions, as it creates and executes a shell script in `/data/local/tmp/`.
 *
 * Note that shell scripting capabilities differ based on device version. To see which utilities
 * are available on which platform versions,see
 * [Android's shell and utilities](https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/README.md#)
 *
 * @param script Script content to run
 * @param stdin String to pass in as stdin to first command in script
 *
 * @return ShellOutput, including stdout of full script, and stderr of last command.
 */
internal fun UiDevice.executeShellScriptWithStderr(
    script: String,
    stdin: String? = null
): ShellOutput {
    return executeShellScript(
        script = script,
        stdin = stdin,
        includeStderr = true
    ).run {
        ShellOutput(first, second!!)
    }
}

/**
 * Convenience wrapper around [UiDevice.executeShellCommand()] which enables redirects, piping, and
 * all other shell script functionality.
 *
 * Unlike [UiDevice.executeShellCommand()], this method supports arbitrary multi-line shell
 * expressions, as it creates and executes a shell script in `/data/local/tmp/`.
 *
 * Note that shell scripting capabilities differ based on device version. To see which utilities
 * are available on which platform versions,see
 * [Android's shell and utilities](https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/README.md#)
 *
 * @param script Script content to run
 * @param stdin String to pass in as stdin to first command in script
 *
 * @return Stdout string
 */
internal fun UiDevice.executeShellScript(script: String, stdin: String? = null): String {
    return executeShellScript(script, stdin, false).first
}

/**
 * Writes the inputStream to an executable file with the given name in `/data/local/tmp`
 */
internal fun UiDevice.createRunnableExecutable(name: String, inputStream: InputStream): String {
    // dirUsableByAppAndShell is writable, but we can't execute there (as of Q),
    // so we copy to /data/local/tmp
    val externalDir = Outputs.dirUsableByAppAndShell
    val writableExecutableFile = File.createTempFile(
        /* prefix */ "temporary_$name",
        /* suffix */ null,
        /* directory */ externalDir
    )
    val runnableExecutablePath = "/data/local/tmp/$name"

    try {
        writableExecutableFile.outputStream().use {
            inputStream.copyTo(it)
        }
        moveToTmpAndMakeExecutable(
            src = writableExecutableFile.absolutePath,
            dst = runnableExecutablePath
        )
    } finally {
        writableExecutableFile.delete()
    }

    return runnableExecutablePath
}

private fun UiDevice.moveToTmpAndMakeExecutable(src: String, dst: String) {
    // Note: we don't check for return values from the below, since shell based file
    // permission errors generally crash our process.
    executeShellCommand("cp $src $dst")
    executeShellCommand("chmod +x $dst")
}
