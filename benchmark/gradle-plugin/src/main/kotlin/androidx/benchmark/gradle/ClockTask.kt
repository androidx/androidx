/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import java.util.concurrent.TimeUnit

open class ClockTask(private val sdkPath: String) : DefaultTask() {
    init {
        group = "Android"
    }

    fun execAdbSync(adbCmd: Array<String>, shouldThrow: Boolean = true): Process {
        val cmd = arrayOf("$sdkPath/platform-tools/adb", *adbCmd)

        logger.log(LogLevel.QUIET, cmd.joinToString(" "))
        val process = Runtime.getRuntime().exec(cmd)

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            throw GradleException("Timeout waiting for ${cmd.joinToString(" ")}")
        }

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        logger.log(LogLevel.QUIET, stdout)
        logger.log(LogLevel.WARN, stderr)

        if (shouldThrow && process.exitValue() != 0) {
            throw GradleException(stderr)
        }

        return process
    }
}
