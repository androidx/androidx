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

import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.inject.Inject

open class LockClocksTask @Inject constructor(sdkPath: String) : ClockTask(sdkPath) {
    init {
        description = "locks clocks of connected, supported, rooted device"
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        // Skip "adb root" if already rooted as it will fail.
        if (execAdbSync(arrayOf("shell", "su exit"), false).exitValue() != 0) {
            execAdbSync(arrayOf("root"))
        }

        val dest = "/data/local/tmp/lockClocks.sh"
        val source = javaClass.classLoader.getResource("scripts/lockClocks.sh")
        val tmpSource = Files.createTempFile("lockClocks.sh", null).toString()
        Files.copy(
            source.openStream(),
            Paths.get(tmpSource),
            StandardCopyOption.REPLACE_EXISTING
        )
        execAdbSync(arrayOf("push", tmpSource, dest))

        // Files pushed by adb push don't always preserve file permissions.
        execAdbSync(arrayOf("shell", "chmod", "700", dest))
        execAdbSync(arrayOf("shell", dest))
        execAdbSync(arrayOf("shell", "rm", dest))
    }
}
