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

package androidx.build.lint

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "simple file listing task")
abstract class ValidateLintChecks : DefaultTask() {
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val sourceDirectories: ConfigurableFileCollection

    @TaskAction
    fun validateRegistryTestExists() {
        val projectFiles = sourceDirectories.asFileTree.files
        // if the project doesn't define a registry it doesn't make sense to test versions
        if (projectFiles.none { it.name.contains("Registry") }) {
            return
        }
        projectFiles.find { it.name == "ApiLintVersionsTest.kt" }
            ?: throw GradleException("Lint projects should include ApiLintVersionsTest.kt")
    }
}
