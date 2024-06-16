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

package androidx.room.gradle

import androidx.kruth.assertThat
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

internal fun runGradle(
    vararg args: String,
    projectDir: File,
    expectFailure: Boolean = false
): BuildResult {
    val runner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withDebug(true)
            // workaround for b/231154556
            .withArguments("-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=512m", *args)
    return if (expectFailure) {
        runner.buildAndFail()
    } else {
        runner.build()
    }
}

internal fun BuildResult.assertTaskOutcome(taskPath: String, outcome: TaskOutcome) {
    assertThat(this.task(taskPath)!!.outcome).isEqualTo(outcome)
}

internal fun searchAndReplace(file: File, search: String, replace: String) {
    file.writeText(file.readText().replace(search, replace))
}
