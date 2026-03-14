/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.intellij

import androidx.build.ProjectLayoutType
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "the purpose of this task is to launch IntelliJ")
abstract class IntelliJTask : DefaultTask() {
    @TaskAction
    fun intellijw() {
        println("ran intellij task")
    }

    companion object {
        private const val INTELLIJ_TASK = "intellij"

        fun Project.registerIntelliJTask() {
            val studioTask =
                when (ProjectLayoutType.from(this)) {
                    ProjectLayoutType.ANDROIDX -> RootIntelliJTask::class.java
                    else ->
                        error(
                            "Launching IntelliJ in any other environment than AndroidX project layout type is not supported"
                        )
                }
            tasks.register(INTELLIJ_TASK, studioTask)
        }
    }
}

/** Task for launching intellij in the frameworks/support project */
@DisableCachingByDefault(because = "the purpose of this task is to launch IntelliJ")
abstract class RootIntelliJTask : IntelliJTask() {}
