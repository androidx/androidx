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
 */
internal fun UiDevice.executeShellScript(script: String, stdin: String? = null): String {
    // dirUsableByAppAndShell is writable, but we can't execute there (as of Q),
    // so we copy to /data/local/tmp
    val externalDir = Outputs.dirUsableByAppAndShell
    val stdinFile = File.createTempFile("temporaryStdin", null, externalDir)
    val writableScriptFile = File.createTempFile("temporaryScript", ".sh", externalDir)
    val runnableScriptPath = "/data/local/tmp/" + writableScriptFile.name

    try {
        if (stdin != null) {
            stdinFile.writeText(stdin)
            writableScriptFile.writeText("cat ${stdinFile.absolutePath} | $script")
        } else {
            writableScriptFile.writeText(script)
        }

        // Note: we don't check for return values from the below, since shell based file
        // permission errors generally crash our process.
        executeShellCommand("cp ${writableScriptFile.absolutePath} $runnableScriptPath")
        executeShellCommand("chmod +x $runnableScriptPath")

        return executeShellCommand(runnableScriptPath)
    } finally {
        stdinFile.delete()
        writableScriptFile.delete()
        executeShellCommand("rm $runnableScriptPath")
    }
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
